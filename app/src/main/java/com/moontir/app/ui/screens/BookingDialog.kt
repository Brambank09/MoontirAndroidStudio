package com.moontir.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moontir.app.data.model.*
import com.moontir.app.ui.components.*
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDialog(
    isOpen: Boolean,
    initialService: Service?,
    services: List<Service>,
    vehicles: List<Vehicle>,
    isIndonesian: Boolean,
    busy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAddVehicle: () -> Unit,
    onGetQuote: suspend (String, String) -> Quote?,
    onSearchAddress: suspend (String) -> List<GeocodeResult>,
    onReverseGeocode: suspend (Double, Double) -> GeocodeResult?,
    onConfirmOrder: (serviceId: String, vehicleId: String, address: Address, date: String, time: String, notes: String) -> Unit,
    onGetMultiQuote: (suspend (List<String>, String) -> Quote?)? = null
) {
    if (!isOpen) return

    val colors = MoontirTheme.colors
    val strings = AppI18n.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var step by remember(isOpen) { mutableIntStateOf(1) }
    var selectedServices by remember(initialService, isOpen) {
        mutableStateOf(if (initialService != null) setOf(initialService) else emptySet())
    }
    var selectedVehicle by remember(isOpen) { mutableStateOf(vehicles.firstOrNull()) }

    var addressLabel by remember { mutableStateOf("") }
    var addressLat by remember { mutableStateOf<Double?>(null) }
    var addressLon by remember { mutableStateOf<Double?>(null) }
    var addressQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val isEmergencyBooking = selectedServices.any { it.group == "emergency" }
    val isSpecialCareBooking = selectedServices.any { it.group == "special" }

    var step1Category by remember(initialService) {
        mutableStateOf(if (initialService?.group == "emergency") "emergency" else "all")
    }

    fun toggleService(service: Service) {
        when (service.group) {
            "emergency" -> {
                // Emergency services cannot be combined with services outside the emergency category
                val emergencyOnly = selectedServices.filter { it.group == "emergency" }.toMutableSet()
                if (emergencyOnly.any { it.id == service.id }) {
                    emergencyOnly.removeAll { it.id == service.id }
                } else {
                    emergencyOnly.add(service)
                }
                selectedServices = emergencyOnly
            }
            "special" -> {
                // Moontir Special Care is an all-inclusive signature package and cannot be bundled with other services
                selectedServices = if (selectedServices.any { it.id == service.id }) {
                    emptySet()
                } else {
                    setOf(service)
                }
            }
            else -> {
                // Regular services: remove any Emergency or Special Care bundle first
                val regularOnly = selectedServices.filter { it.group != "emergency" && it.group != "special" }.toMutableSet()
                if (regularOnly.any { it.id == service.id }) {
                    regularOnly.removeAll { it.id == service.id }
                } else {
                    regularOnly.add(service)
                }
                selectedServices = regularOnly
            }
        }
    }

    fun parseServiceDurationMinutes(service: Service): Int {
        return when (service.id) {
            "emg_tow" -> 45
            "emg_jump" -> 30
            "emg_tire" -> 35
            "car_check" -> 60
            "oil_change" -> 45
            "fluid_refresh" -> 60
            "full_tune" -> 90
            "battery_change" -> 30
            "ac_refresh" -> 60
            "shine" -> 150
            "interior" -> 120
            "glass" -> 90
            "full" -> 240
            "eclipse" -> 300
            else -> {
                val d = service.duration.lowercase()
                if (d.contains("hour")) {
                    val num = Regex("""(\d+)""").findAll(d).map { it.value.toInt() }.lastOrNull() ?: 1
                    num * 60
                } else if (d.contains("min")) {
                    Regex("""(\d+)""").findAll(d).map { it.value.toInt() }.lastOrNull() ?: 60
                } else {
                    60
                }
            }
        }
    }

    val totalDurationMinutes = remember(selectedServices) {
        val total = selectedServices.sumOf { parseServiceDurationMinutes(it) }
        if (total > 0) total else 60
    }

    fun formatDurationText(totalMins: Int): String {
        val hrs = totalMins / 60
        val mins = totalMins % 60
        return when {
            hrs > 0 && mins > 0 -> if (isIndonesian) "${hrs} jam ${mins} mnt" else "${hrs}h ${mins}m"
            hrs > 0 -> if (isIndonesian) "${hrs} jam" else "${hrs}h"
            else -> if (isIndonesian) "${mins} menit" else "${mins}m"
        }
    }

    // Next 5 dates
    val nextDates = remember(isIndonesian) {
        val cal = java.util.Calendar.getInstance()
        (1..5).map { offset ->
            val c = cal.clone() as java.util.Calendar
            c.add(java.util.Calendar.DAY_OF_YEAR, offset)
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val iso = isoFormat.format(c.time)
            val dayNum = c.get(java.util.Calendar.DAY_OF_MONTH).toString()
            val dayFormat = java.text.SimpleDateFormat("EEE", if (isIndonesian) Locale.forLanguageTag("id-ID") else Locale.US)
            val dayName = dayFormat.format(c.time)
            Triple(iso, dayNum, dayName)
        }
    }

    var selectedDate by remember { mutableStateOf(nextDates.first().first) }

    // Start time: standard slots from 09:00 to 16:00
    val standardStartTimes = listOf(
        "09:00", "09:30", "10:00", "10:30",
        "11:00", "11:30", "12:00", "12:30",
        "13:00", "13:30", "14:00", "14:30",
        "15:00", "15:30", "16:00"
    )
    var selectedStartTime by remember { mutableStateOf(standardStartTimes.first()) }

    // Calculated completion time based on selected start time and total duration
    val completionTimeStr = remember(selectedStartTime, totalDurationMinutes) {
        val parts = selectedStartTime.split(":")
        val startH = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val startTotalMins = startH * 60 + startM
        val endTotalMins = startTotalMins + totalDurationMinutes
        val endH = (endTotalMins / 60) % 24
        val endM = endTotalMins % 60
        String.format(Locale.US, "%02d:%02d", endH, endM)
    }

    // Emergency 24/7 immediate dispatch timestamp & estimated completion
    val emergencyInfo = remember(step, totalDurationMinutes, isIndonesian) {
        val c = java.util.Calendar.getInstance()
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayIso = isoFormat.format(c.time)
        val startH = c.get(java.util.Calendar.HOUR_OF_DAY)
        val startM = c.get(java.util.Calendar.MINUTE)
        val startStr = String.format(Locale.US, "%02d:%02d", startH, startM)

        c.add(java.util.Calendar.MINUTE, totalDurationMinutes)
        val endH = c.get(java.util.Calendar.HOUR_OF_DAY)
        val endM = c.get(java.util.Calendar.MINUTE)
        val finishStr = String.format(Locale.US, "%02d:%02d", endH, endM)
        Triple(todayIso, startStr, finishStr)
    }

    val effectiveDate = if (isEmergencyBooking) {
        emergencyInfo.first
    } else {
        selectedDate
    }

    val effectiveTime = if (isEmergencyBooking) {
        if (isIndonesian) "Segera Siaga 24/7 (Estimasi selesai ~${emergencyInfo.third})"
        else "Immediate 24/7 (Est. completion ~${emergencyInfo.third})"
    } else {
        "$selectedStartTime – $completionTimeStr (${formatDurationText(totalDurationMinutes)})"
    }

    var notes by remember { mutableStateOf("") }

    var quote by remember { mutableStateOf<Quote?>(null) }

    // Fetch quote whenever selected services or vehicle changes
    LaunchedEffect(selectedServices, selectedVehicle) {
        val sList = selectedServices.toList()
        val v = selectedVehicle
        if (sList.isNotEmpty() && v != null) {
            quote = if (onGetMultiQuote != null) {
                onGetMultiQuote(sList.map { it.id }, v.type)
            } else if (sList.size == 1) {
                onGetQuote(sList.first().id, v.type)
            } else {
                null
            }
        } else {
            quote = null
        }
    }

    val isStepValid = when (step) {
        1 -> selectedServices.isNotEmpty()
        2 -> selectedVehicle != null
        3 -> addressLabel.isNotBlank()
        4 -> if (isEmergencyBooking) true else selectedDate.isNotBlank() && selectedStartTime.isNotBlank()
        else -> true
    }

    val listState = rememberLazyListState()
    LaunchedEffect(step) {
        listState.scrollToItem(0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Close", tint = colors.onSurface)
                    }
                    MoontirLogoView(height = 26.dp)
                    Spacer(modifier = Modifier.width(44.dp))
                }

                // Progress Step Dots
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= step) colors.brand else colors.divider)
                        )
                    }
                }

                // Main Scrollable Step Content
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "header") {
                        Column {
                            Text(
                                text = "BOOKING · 0$step/5",
                                color = colors.onSurfaceSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val stepTitle = when (step) {
                                1 -> strings.selectService
                                2 -> strings.selectVehicle
                                3 -> strings.address
                                4 -> strings.schedule
                                else -> strings.review
                            }
                            Text(
                                text = stepTitle,
                                color = colors.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (step == 3) strings.dragPinHint else if (isIndonesian) "Rawat mobilmu dengan tenang." else "Care for your car, mindfully.",
                                color = colors.muted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // STEP 1: SELECT SERVICE (Multi-service supported & Emergency Isolated)
                    if (step == 1) {
                        item(key = "step1_header") {
                            if (isEmergencyBooking) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E1214),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.WarningAmber,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${selectedServices.size} ${if (isIndonesian) "Layanan Darurat Dipilih (24/7)" else "Emergency Service(s) Selected (24/7)"}",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = strings.emergencyExclusive,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Subtotal: ${formatRupiah(selectedServices.sumOf { it.price })}",
                                                    color = Color(0xFFFCA5A5),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        TextButton(
                                            onClick = { selectedServices = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(strings.clearSelection, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (isSpecialCareBooking) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceSecondary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.brand),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isIndonesian) "1 Paket Andalan dipilih (Eksklusif)" else "1 Signature Bundle selected (Exclusive)",
                                                color = colors.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = strings.specialCareExclusive,
                                                color = colors.muted,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Subtotal: ${formatRupiah(selectedServices.sumOf { it.price })}",
                                                color = colors.moonGlow,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        TextButton(
                                            onClick = { selectedServices = emptySet() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(strings.clearSelection, color = colors.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceTertiary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (selectedServices.isEmpty()) {
                                                    if (isIndonesian) "Pilih 1 atau lebih layanan" else "Select 1 or more services"
                                                } else {
                                                    "${selectedServices.size} ${if (selectedServices.size == 1) strings.serviceSelected else strings.servicesSelected}"
                                                },
                                                color = if (selectedServices.isEmpty()) colors.muted else colors.onSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (selectedServices.isNotEmpty()) {
                                                Text(
                                                    text = "Subtotal: ${formatRupiah(selectedServices.sumOf { it.price })}",
                                                    color = colors.moonGlow,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        if (selectedServices.isNotEmpty()) {
                                            TextButton(
                                                onClick = { selectedServices = emptySet() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(strings.clearSelection, color = colors.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Category Filter Chips
                        item(key = "step1_category_chips") {
                            val filterGroups = listOf(
                                "all" to if (isIndonesian) "SEMUA" else "ALL",
                                "emergency" to strings.groupEmergency.uppercase(),
                                "car_service" to strings.groupCarService.uppercase(),
                                "detail" to strings.groupDetail.uppercase(),
                                "special" to strings.groupSpecial.uppercase()
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(filterGroups, key = { it.first }) { (key, label) ->
                                    val isSelected = step1Category == key
                                    val chipColor = if (isSelected) {
                                        if (key == "emergency") Color(0xFFEF4444) else colors.brand
                                    } else {
                                        colors.surfaceTertiary
                                    }
                                    val textColor = if (isSelected) Color.White else colors.onSurface
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = chipColor,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) chipColor else colors.border),
                                        modifier = Modifier.clickable { step1Category = key }
                                    ) {
                                        Text(
                                            text = label,
                                            color = textColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Render Services Grouped by Category
                        val displayCategories = if (step1Category == "all") {
                            listOf("emergency", "car_service", "detail", "special")
                        } else {
                            listOf(step1Category)
                        }

                        displayCategories.forEach { catKey ->
                            val list = when (catKey) {
                                "car_service" -> services.filter { it.group == "car_service" || it.group == "light" }
                                else -> services.filter { it.group == catKey }
                            }
                            if (list.isNotEmpty()) {
                                item(key = "step1_cat_title_$catKey") {
                                    val title = when (catKey) {
                                        "emergency" -> "🚨 " + strings.groupEmergency.uppercase() + (if (isIndonesian) " (SIAGA 24/7)" else " (24/7 ASSIST)")
                                        "car_service" -> strings.groupCarService.uppercase()
                                        "detail" -> strings.groupDetail.uppercase()
                                        else -> strings.groupSpecial.uppercase() + (if (isIndonesian) " (PAKET ANDALAN)" else " (SIGNATURE)")
                                    }
                                    Text(
                                        text = title,
                                        color = if (catKey == "emergency") Color(0xFFEF4444) else colors.onSurfaceSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.4.sp,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }

                                items(list, key = { "step1_service_${it.id}" }) { service ->
                                    val isSelected = selectedServices.any { it.id == service.id }
                                    MoontirServiceCard(
                                        service = service,
                                        isIndonesian = isIndonesian,
                                        isSelected = isSelected,
                                        onBookClick = { toggleService(service) }
                                    )
                                }
                            }
                        }
                    }

                    // STEP 2: SELECT VEHICLE
                    if (step == 2) {
                        items(vehicles, key = { "step2_vehicle_${it.id}" }) { vehicle ->
                            val isSelected = selectedVehicle?.id == vehicle.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceTertiary,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) colors.brand else colors.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVehicle = vehicle }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DirectionsCar,
                                        contentDescription = null,
                                        tint = colors.brand,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(vehicle.nickname, color = colors.onSurface, fontWeight = FontWeight.Bold)
                                        Text("${vehicle.make} ${vehicle.model} · ${vehicle.year} · ${vehicle.type}", color = colors.onSurfaceSecondary, fontSize = 12.sp)
                                        Text(vehicle.plate, color = colors.moonGlow, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedVehicle = vehicle },
                                        colors = RadioButtonDefaults.colors(selectedColor = colors.brand)
                                    )
                                }
                            }
                        }

                        item(key = "step2_add_btn") {
                            MoontirOutlineButton(
                                text = strings.addVehicle,
                                icon = Icons.Outlined.Add,
                                onClick = onAddVehicle
                            )
                        }
                    }

                    // STEP 3: ADDRESS & MAP
                    if (step == 3) {
                        item(key = "step3_address") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MoontirTextField(
                                    value = addressLabel,
                                    onValueChange = { addressLabel = it },
                                    label = strings.address,
                                    leadingIcon = Icons.Outlined.PinDrop,
                                    isMultiline = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            // Mock/default coordinates for central Java / Semarang or device location
                                            addressLat = -6.9932
                                            addressLon = 110.4203
                                            coroutineScope.launch {
                                                try {
                                                    val res = onReverseGeocode(-6.9932, 110.4203)
                                                    if (res != null) addressLabel = res.displayName
                                                } catch (_: Throwable) {}
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Outlined.MyLocation, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.useLocation, color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (addressQuery.length >= 3) {
                                                isSearching = true
                                                coroutineScope.launch {
                                                    try {
                                                        searchResults = onSearchAddress(addressQuery)
                                                    } catch (_: Throwable) {}
                                                    isSearching = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.searchAddress, color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                MoontirTextField(
                                    value = addressQuery,
                                    onValueChange = { addressQuery = it },
                                    label = if (isIndonesian) "Cari jalan atau kota" else "Search a street or city",
                                    leadingIcon = Icons.Outlined.Search
                                )

                                if (isSearching) {
                                    CircularProgressIndicator(color = colors.brand, modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                                }

                                searchResults.forEach { result ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceSecondary,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                addressLabel = result.displayName
                                                addressLat = result.latitude
                                                addressLon = result.longitude
                                                searchResults = emptyList()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.PinDrop, contentDescription = null, tint = colors.brand, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(result.displayName, color = colors.onSurfaceSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Interactive Leaflet Map
                                LeafletMapView(
                                    latitude = addressLat,
                                    longitude = addressLon,
                                    onPickPin = { lat, lon ->
                                        addressLat = lat
                                        addressLon = lon
                                        coroutineScope.launch {
                                            try {
                                                val rev = onReverseGeocode(lat, lon)
                                                if (rev != null) addressLabel = rev.displayName
                                            } catch (_: Throwable) {}
                                        }
                                    },
                                    heightDp = 220
                                )

                                if (addressLat != null && addressLon != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Outlined.PinDrop, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.4f, %.4f", addressLat, addressLon),
                                            color = colors.onSurfaceSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = strings.openMap,
                                            color = colors.moonGlow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                val uri = Uri.parse("https://www.openstreetmap.org/?mlat=$addressLat&mlon=$addressLon#map=17/$addressLat/$addressLon")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                        )
                                    }
                                } else {
                                    Text(
                                        text = strings.mapCredit,
                                        color = colors.muted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }

                    // STEP 4: SCHEDULE & NOTES
                    if (step == 4) {
                        item(key = "step4_schedule") {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (isEmergencyBooking) {
                                    // 24/7 IMMEDIATE EMERGENCY DISPATCH VIEW
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFF1E1214),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.WarningAmber,
                                                        contentDescription = null,
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(14.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "🚨 24/7 IMMEDIATE DISPATCH",
                                                        color = Color(0xFFEF4444),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.2.sp
                                                    )
                                                    Text(
                                                        text = strings.emergencyImmediateTitle,
                                                        color = Color.White,
                                                        fontSize = 17.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = strings.emergencyImmediateSub,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // 3 Metrics Display: Start Time, Duration, Estimated Finish
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.Black.copy(alpha = 0.4f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(strings.startTime.uppercase(), color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = if (isIndonesian) "Sekarang" else "Immediate",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                        Text(
                                                            text = "24/7 Siaga",
                                                            color = Color(0xFFEF4444),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.Black.copy(alpha = 0.4f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(strings.totalDuration.uppercase(), color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = formatDurationText(totalDurationMinutes),
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                        Text(
                                                            text = "${selectedServices.size} Layanan",
                                                            color = Color(0xFFEF4444),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color.Black.copy(alpha = 0.4f),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(strings.estCompletion.uppercase(), color = Color(0xFFFCA5A5), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "~${emergencyInfo.third}",
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                        Text(
                                                            text = "Waktu tiba",
                                                            color = Color(0xFFEF4444),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))
                                            HorizontalDivider(color = Color(0xFFEF4444).copy(alpha = 0.3f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = if (isIndonesian) "Layanan darurat yang dipanggil:" else "Dispatched emergency services:",
                                                color = Color(0xFFFCA5A5),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            selectedServices.forEach { s ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("• ${if (isIndonesian) s.nameId else s.name}", color = Color.White, fontSize = 12.sp)
                                                    Text(if (isIndonesian) s.durationId else s.duration, color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // STANDARD / SCHEDULED WORK VIEW (9:00 AM – 4:00 PM)
                                    Text(
                                        text = if (isIndonesian) "TANGGAL LAYANAN" else "SERVICE DATE",
                                        color = colors.onSurfaceSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(nextDates, key = { it.first }) { (iso, dayNum, dayName) ->
                                            val isSelected = selectedDate == iso
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) colors.brand else colors.surfaceTertiary,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.brand else colors.border),
                                                modifier = Modifier
                                                    .width(64.dp)
                                                    .height(72.dp)
                                                    .clickable { selectedDate = iso }
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Text(dayName.uppercase(), color = if (isSelected) colors.onBrand else colors.onSurfaceSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(dayNum, color = if (isSelected) colors.onBrand else colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                                }
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = strings.chooseStartTime,
                                            color = colors.onSurfaceSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isIndonesian) "Pilih jam mulai kerja teknisi (09:00 s/d 16:00)" else "Choose when work should begin (9:00 AM to 4:00 PM)",
                                            color = colors.muted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Start Time Chips Grid (4 columns per row)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        standardStartTimes.chunked(4).forEach { rowTimes ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowTimes.forEach { timeSlot ->
                                                    val isSelected = selectedStartTime == timeSlot
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isSelected) colors.brand else colors.surfaceTertiary,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.brand else colors.border),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(44.dp)
                                                            .clickable { selectedStartTime = timeSlot }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = timeSlot,
                                                                color = if (isSelected) colors.onBrand else colors.onSurface,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                                // Fill empty cells if last row has less than 4 items
                                                if (rowTimes.size < 4) {
                                                    repeat(4 - rowTimes.size) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Custom minute selector (:00, :15, :30, :45)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.surfaceSecondary,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isIndonesian) "Menit Spesifik:" else "Minute Fine-tune:",
                                                color = colors.onSurfaceSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("00", "15", "30", "45").forEach { m ->
                                                    val currentHour = selectedStartTime.split(":").firstOrNull() ?: "09"
                                                    val isCurrentMinute = selectedStartTime.split(":").getOrNull(1) == m
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = if (isCurrentMinute) colors.brand else colors.surfaceTertiary,
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrentMinute) colors.brand else colors.border),
                                                        modifier = Modifier.clickable {
                                                            selectedStartTime = "$currentHour:$m"
                                                        }
                                                    ) {
                                                        Text(
                                                            text = ":$m",
                                                            color = if (isCurrentMinute) colors.onBrand else colors.onSurface,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Dynamic Completion Time Calculation Card
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colors.surfaceSecondary,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(strings.startTime.uppercase(), color = colors.onSurfaceSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(selectedStartTime, color = colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                                }

                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.brand, modifier = Modifier.size(16.dp))

                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(strings.totalDuration.uppercase(), color = colors.onSurfaceSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("+ ${formatDurationText(totalDurationMinutes)}", color = colors.moonGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.brand, modifier = Modifier.size(16.dp))

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(strings.estCompletion.uppercase(), color = colors.onSurfaceSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(completionTimeStr, color = colors.brand, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                                }
                                            }

                                            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 10.dp))

                                            Text(
                                                text = if (isIndonesian) "Rincian durasi layanan yang dipilih:" else "Selected services duration breakdown:",
                                                color = colors.onSurfaceSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            selectedServices.forEach { s ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("• ${if (isIndonesian) s.nameId else s.name}", color = colors.onSurface, fontSize = 12.sp)
                                                    Text(
                                                        formatDurationText(parseServiceDurationMinutes(s)),
                                                        color = colors.onSurfaceSecondary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                MoontirTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = if (isEmergencyBooking) (if (isIndonesian) "Catatan darurat (posisi mobil, patokan, ciri)" else "Emergency notes (car location, landmark)") else strings.notes,
                                    leadingIcon = Icons.AutoMirrored.Outlined.Notes,
                                    isMultiline = true
                                )
                            }
                        }
                    }

                    // STEP 5: REVIEW & SUMMARY
                    if (step == 5) {
                        item(key = "step5_review") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (selectedServices.size == 1) {
                                    val s = selectedServices.first()
                                    val finalPrice = quote?.total ?: s.price
                                    ReviewRow(icon = Icons.Outlined.AutoAwesome, label = strings.selectService, value = "${if (isIndonesian) s.nameId else s.name} (${formatRupiah(finalPrice)})")
                                } else if (selectedServices.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colors.surfaceTertiary,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = strings.selectService.uppercase(),
                                                    color = colors.onSurfaceSecondary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${selectedServices.size} ${strings.servicesSelected}",
                                                    color = colors.moonGlow,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            selectedServices.forEach { s ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "• ${if (isIndonesian) s.nameId else s.name}",
                                                        color = colors.onSurface,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = formatRupiah(s.price),
                                                        color = colors.onSurfaceSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                selectedVehicle?.let { v ->
                                    ReviewRow(icon = Icons.Outlined.DirectionsCar, label = strings.selectVehicle, value = "${v.make} ${v.model} · ${v.plate} · ${v.type}")
                                }

                                ReviewRow(icon = Icons.Outlined.PinDrop, label = strings.address, value = addressLabel)
                                ReviewRow(icon = Icons.Outlined.CalendarToday, label = strings.schedule, value = "$effectiveDate · $effectiveTime")

                                // Price Breakdown Card
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.surfaceSecondary,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = strings.priceBreakdown.uppercase(),
                                            color = colors.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.4.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))

                                        val items = quote?.items ?: selectedServices.map {
                                            InvoiceItem(label = it.name, labelId = it.nameId, amount = it.price)
                                        }
                                        items.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val isSurchargeItem = item.label.contains("handling surcharge", ignoreCase = true) ||
                                                        item.label.equals("Tax and handling fees", ignoreCase = true) ||
                                                        item.labelId?.contains("Biaya penanganan", ignoreCase = true) == true ||
                                                        item.labelId?.contains("Pajak dan biaya", ignoreCase = true) == true

                                                val label = if (isSurchargeItem) {
                                                    strings.taxAndHandling
                                                } else if (isIndonesian && !item.labelId.isNullOrBlank()) {
                                                    item.labelId
                                                } else {
                                                    item.label
                                                }
                                                Text(label, color = colors.onSurfaceSecondary, fontSize = 12.sp)
                                                Text(formatRupiah(item.amount), color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        HorizontalDivider(color = colors.borderStrong, modifier = Modifier.padding(vertical = 8.dp))

                                        val grandTotal = quote?.total ?: selectedServices.sumOf { it.price }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(strings.total.uppercase(), color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                            Text(formatRupiah(grandTotal), color = colors.moonGlow, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = strings.unpaid,
                                            color = colors.muted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        item(key = "booking_error_msg") {
                            Text(errorMessage, color = colors.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Footer Actions
                Surface(
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            TextButton(onClick = { step-- }) {
                                Text(strings.back, color = colors.onSurfaceSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        if (step < 5) {
                            MoontirButton(
                                text = strings.next,
                                onClick = { if (isStepValid) step++ },
                                enabled = isStepValid,
                                modifier = Modifier.width(140.dp)
                            )
                        } else {
                            MoontirButton(
                                text = strings.confirm,
                                onClick = {
                                    val v = selectedVehicle
                                    if (selectedServices.isNotEmpty() && v != null && addressLabel.isNotBlank()) {
                                        onConfirmOrder(
                                            selectedServices.joinToString(",") { it.id },
                                            v.id,
                                            Address(addressLabel, addressLat, addressLon),
                                            effectiveDate,
                                            effectiveTime,
                                            notes
                                        )
                                    }
                                },
                                busy = busy,
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    val colors = MoontirTheme.colors
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceTertiary,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.brand, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label.uppercase(), color = colors.onSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
