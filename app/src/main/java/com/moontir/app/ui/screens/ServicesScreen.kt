package com.moontir.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Service
import com.moontir.app.ui.components.MoontirServiceCard
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun ServicesScreen(
    services: List<Service>,
    isIndonesian: Boolean,
    initialFilter: String = "all",
    onBookClick: (Service) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember(initialFilter) { mutableStateOf(initialFilter) }
    var serviceForDetail by remember { mutableStateOf<Service?>(null) }

    val filterGroups = listOf(
        "all" to if (isIndonesian) "SEMUA" else "ALL",
        "emergency" to strings.groupEmergency.uppercase(),
        "car_service" to strings.groupCarService.uppercase(),
        "detail" to strings.groupDetail.uppercase(),
        "special" to strings.groupSpecial.uppercase()
    )

    val q = searchQuery.trim().lowercase()
    val activeFilter = if (q.isNotEmpty()) "all" else selectedFilter

    val filteredServices = services.filter { service ->
        if (activeFilter != "all") {
            val matches = service.group == activeFilter || (activeFilter == "car_service" && service.group == "light")
            if (!matches) return@filter false
        }
        if (q.isEmpty()) return@filter true
        val haystack = "${service.name} ${service.nameId} ${service.description} ${service.descriptionId} ${service.category} ${service.categoryId}".lowercase()
        haystack.contains(q)
    }

    val grouped = filteredServices.groupBy { it.group }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp + navBarBottom),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = strings.services.replaceFirstChar { it.uppercase() },
                    color = colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isIndonesian) "Pilih perawatan yang sesuai kendaraanmu." else "Find the right care package for your car.",
                    color = colors.muted,
                    fontSize = 13.sp
                )
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchServices, color = colors.muted, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.muted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceTertiary,
                    unfocusedContainerColor = colors.surfaceTertiary,
                    focusedBorderColor = colors.brand,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }

        // All Category Filters (non-scrolling FlowRow, with distinct "ALL" reset button)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterGroups.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    val isAllButton = key == "all"

                    val chipShape = if (isAllButton) RoundedCornerShape(10.dp) else RoundedCornerShape(20.dp)
                    val chipBg = when {
                        isAllButton && isSelected -> colors.onSurface
                        isAllButton -> colors.surfaceSecondary
                        isSelected -> colors.brand
                        else -> colors.surfaceTertiary
                    }
                    val chipBorder = when {
                        isAllButton && isSelected -> colors.onSurface
                        isAllButton -> colors.borderStrong
                        isSelected -> colors.brand
                        else -> colors.border
                    }
                    val chipTextColor = when {
                        isAllButton && isSelected -> colors.surface
                        isAllButton -> colors.onSurface
                        isSelected -> colors.onBrand
                        else -> colors.onSurface
                    }

                    Surface(
                        shape = chipShape,
                        color = chipBg,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isAllButton) 1.5.dp else 1.dp,
                            color = chipBorder
                        ),
                        modifier = Modifier.clickable { selectedFilter = key }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                horizontal = if (isAllButton) 16.dp else 12.dp,
                                vertical = if (isAllButton) 9.dp else 8.dp
                            )
                        ) {
                            if (isAllButton) {
                                Icon(
                                    imageVector = Icons.Outlined.GridView,
                                    contentDescription = null,
                                    tint = chipTextColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                color = chipTextColor,
                                fontSize = if (isAllButton) 11.sp else 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // Grouped Services
        if (filteredServices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surfaceTertiary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isIndonesian) "Tidak ada layanan cocok" else "No matching services",
                            color = colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isIndonesian) "Coba kata kunci atau kategori lain." else "Try another keyword or category.",
                            color = colors.muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val categories = listOf("emergency", "car_service", "detail", "special")
            categories.forEach { catKey ->
                val list = if (catKey == "car_service") {
                    (grouped["car_service"] ?: emptyList()) + (grouped["light"] ?: emptyList())
                } else {
                    grouped[catKey] ?: emptyList()
                }
                if (list.isNotEmpty()) {
                    item {
                        val groupTitle = when (catKey) {
                            "emergency" -> strings.groupEmergency
                            "car_service" -> strings.groupCarService
                            "detail" -> strings.groupDetail
                            else -> strings.groupSpecial
                        }
                        Text(
                            text = groupTitle.uppercase(),
                            color = colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                    }

                    items(list) { service ->
                        MoontirServiceCard(
                            service = service,
                            isIndonesian = isIndonesian,
                            onBookClick = { onBookClick(service) },
                            onCardClick = { serviceForDetail = service }
                        )
                    }
                }
            }
        }
    }

    if (serviceForDetail != null) {
        ServiceDetailDialog(
            service = serviceForDetail,
            isIndonesian = isIndonesian,
            onDismiss = { serviceForDetail = null },
            onBookClick = { selected ->
                serviceForDetail = null
                onBookClick(selected)
            }
        )
    }
}
