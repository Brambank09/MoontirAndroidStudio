package com.moontir.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.BuildConfig
import com.moontir.app.data.model.SavedCard
import com.moontir.app.data.model.User
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.MoontirTextField
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.AmbientLightReading
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun ProfileScreen(
    user: User,
    themeMode: String,
    ambientLight: AmbientLightReading = AmbientLightReading(hasSensor = false, lux = null, isDark = null),
    currentLanguage: String,
    baseUrl: String,
    isMockMode: Boolean,
    savedCard: SavedCard? = null,
    busy: Boolean,
    onUpdateName: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onToggleLanguage: () -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onToggleMockMode: (Boolean) -> Unit,
    onSaveCard: (cardNumber: String, cardHolder: String, expiry: String, cvv: String) -> Unit = { _, _, _, _ -> },
    onClearCard: () -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    var editName by remember(user.name) { mutableStateOf(user.name) }
    val isDirty = editName.trim() != user.name && editName.isNotBlank()

    var showServerDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSavedCardDialog by remember { mutableStateOf(false) }
    var tempUrl by remember(baseUrl) { mutableStateOf(baseUrl) }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp + navBarBottom),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = strings.profile.replaceFirstChar { it.uppercase() },
                    color = colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (strings.language == "ID") "Manage your account and Moontir preferences." else "Kelola akun dan preferensi Moontir.",
                    color = colors.muted,
                    fontSize = 13.sp
                )
            }
        }

        // Profile Head Avatar Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(22.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceTertiary)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user.name.firstOrNull() ?: 'M').uppercase(),
                            color = colors.onSurface,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = user.name,
                        color = colors.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.email,
                        color = colors.muted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Edit Name Form
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MoontirTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = strings.editName,
                    leadingIcon = Icons.Outlined.Person
                )

                MoontirButton(
                    text = strings.saveProfile,
                    onClick = { onUpdateName(editName) },
                    enabled = isDirty,
                    busy = busy
                )
            }
        }

        // Settings Block
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceTertiary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Connection Mode: Standalone (Offline) vs Remote Server
                    SettingRow(
                        icon = Icons.Outlined.CloudSync,
                        title = if (strings.language == "ID") "Mode Server" else "Connection Mode",
                        value = if (isMockMode) (if (strings.language == "ID") "Standalone (Offline)" else "Standalone (Offline)") else "Remote Server",
                        showArrow = true,
                        onClick = { onToggleMockMode(!isMockMode) }
                    )

                    HorizontalDivider(color = colors.divider)

                    // Server API URL Setting (Very helpful for Android Studio emulator / device testing!)
                    SettingRow(
                        icon = Icons.Outlined.Dns,
                        title = strings.serverUrl,
                        value = baseUrl,
                        showArrow = true,
                        onClick = {
                            tempUrl = baseUrl
                            showServerDialog = true
                        }
                    )

                    HorizontalDivider(color = colors.divider)

                    // Theme
                    val themeLabel = when (themeMode) {
                        "auto" -> {
                            val lux = ambientLight.lux
                            if (lux != null) "Auto (${lux.toInt()} lx)" else "Auto"
                        }
                        "dark" -> strings.themeDark
                        "light" -> strings.themeLight
                        else -> strings.themeSystem
                    }
                    val themeIcon = when (themeMode) {
                        "auto" -> Icons.Outlined.BrightnessAuto
                        "dark" -> Icons.Outlined.DarkMode
                        "light" -> Icons.Outlined.LightMode
                        else -> Icons.Outlined.Contrast
                    }
                    SettingRow(
                        icon = themeIcon,
                        title = strings.themeSetting,
                        value = themeLabel,
                        showArrow = true,
                        onClick = { showThemeDialog = true }
                    )

                    HorizontalDivider(color = colors.divider)

                    // Language
                    SettingRow(
                        icon = Icons.Outlined.Language,
                        title = strings.languageSetting,
                        value = if (currentLanguage == "en") "English" else "Bahasa Indonesia",
                        showArrow = true,
                        onClick = onToggleLanguage
                    )

                    HorizontalDivider(color = colors.divider)

                    // Saved Debit / Credit Card Identity
                    val savedCardLast4 = savedCard?.cardNumber?.filter { it.isDigit() }?.takeLast(4).orEmpty()
                    val savedCardValue = if (savedCard != null && savedCardLast4.isNotEmpty()) {
                        "**** $savedCardLast4 · ${savedCard.expiry}"
                    } else {
                        strings.savedCardNotSet
                    }
                    SettingRow(
                        icon = Icons.Outlined.CreditCard,
                        title = strings.savedCardSetting,
                        value = savedCardValue,
                        showArrow = true,
                        onClick = { showSavedCardDialog = true }
                    )

                    HorizontalDivider(color = colors.divider)

                    // App Version
                    SettingRow(
                        icon = Icons.Outlined.Info,
                        title = strings.appVersion,
                        value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        showArrow = false,
                        onClick = null
                    )
                }
            }
        }

        // Logout Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable { onLogout() },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.logout,
                    color = colors.error,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // App Version Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Moontir App v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    color = colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Standalone Mobile Engine · Native Jetpack Compose",
                    color = colors.onSurfaceSecondary.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val options = listOf(
            Triple("auto", strings.themeAuto, strings.themeAutoDesc),
            Triple("system", strings.themeSystem, strings.themeSystemDesc),
            Triple("light", strings.themeLight, strings.themeLightDesc),
            Triple("dark", strings.themeDark, strings.themeDarkDesc)
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = strings.chooseAppearance,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (mode, title, desc) ->
                        val isSelected = themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.surfaceTertiary else colors.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.brand else colors.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTheme(mode)
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        color = colors.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        color = colors.onSurfaceSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectTheme(mode)
                                        showThemeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.brand)
                                )
                            }
                        }
                    }

                    if (themeMode == "auto") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceTertiary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sensorInfo = when {
                                !ambientLight.hasSensor -> strings.lightSensorUnavailable
                                ambientLight.lux != null -> {
                                    val condition = if (ambientLight.isDark == true) strings.ambientDim else strings.ambientBright
                                    val activeTheme = if (ambientLight.isDark == true) strings.themeDark else strings.themeLight
                                    "${ambientLight.lux.toInt()} lux · $condition ($activeTheme)"
                                }
                                else -> if (currentLanguage == "id") "Membaca sensor cahaya..." else "Reading ambient light sensor..."
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BrightnessAuto,
                                    contentDescription = null,
                                    tint = colors.brand,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sensorInfo,
                                    color = colors.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(strings.cancel, color = colors.muted)
                }
            },
            containerColor = colors.surfaceSecondary
        )
    }

    // Saved Debit / Credit Card Dialog
    if (showSavedCardDialog) {
        var cardNumberInput by remember(showSavedCardDialog, savedCard) {
            mutableStateOf(savedCard?.cardNumber.orEmpty())
        }
        var cardHolderInput by remember(showSavedCardDialog, savedCard) {
            mutableStateOf(savedCard?.cardHolder.orEmpty())
        }
        var cardExpiryInput by remember(showSavedCardDialog, savedCard) {
            mutableStateOf(savedCard?.expiry.orEmpty())
        }
        var cardCvvInput by remember(showSavedCardDialog, savedCard) {
            mutableStateOf(savedCard?.cvv.orEmpty())
        }
        var cardError by remember(showSavedCardDialog) { mutableStateOf<String?>(null) }
        val isId = currentLanguage == "id"

        AlertDialog(
            onDismissRequest = { showSavedCardDialog = false },
            title = {
                Text(
                    text = strings.savedCardDialogTitle,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.savedCardDialogDesc,
                        color = colors.onSurfaceSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = cardNumberInput,
                        onValueChange = {
                            cardNumberInput = formatCardNumber(it)
                            cardError = null
                        },
                        label = { Text(strings.cardNumber, fontSize = 12.sp) },
                        placeholder = { Text("0000 0000 0000 0000", color = colors.muted) },
                        leadingIcon = { Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = colors.muted) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = cardHolderInput,
                        onValueChange = {
                            cardHolderInput = it
                            cardError = null
                        },
                        label = { Text(strings.cardHolderName, fontSize = 12.sp) },
                        placeholder = { Text(if (isId) "Nama sesuai kartu" else "Cardholder Name", color = colors.muted) },
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = colors.muted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = cardExpiryInput,
                            onValueChange = {
                                cardExpiryInput = formatCardExpiry(it)
                                cardError = null
                            },
                            label = { Text(strings.cardExpiry, fontSize = 12.sp) },
                            placeholder = { Text("MM/YY", color = colors.muted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = cardCvvInput,
                            onValueChange = {
                                cardCvvInput = it.filter { ch -> ch.isDigit() }.take(4)
                                cardError = null
                            },
                            label = { Text(strings.cardCvv, fontSize = 12.sp) },
                            placeholder = { Text("123", color = colors.muted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!cardError.isNullOrBlank()) {
                        Text(
                            text = cardError!!,
                            color = colors.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (savedCard != null) {
                        TextButton(
                            onClick = {
                                onClearCard()
                                showSavedCardDialog = false
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.removeCardAction,
                                color = colors.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val err = validateCardPaymentInput(
                        cardNumber = cardNumberInput,
                        cardHolder = cardHolderInput,
                        expiry = cardExpiryInput,
                        cvv = cardCvvInput,
                        isIndonesian = isId
                    )
                    if (err != null) {
                        cardError = err
                    } else {
                        onSaveCard(cardNumberInput, cardHolderInput, cardExpiryInput, cardCvvInput)
                        showSavedCardDialog = false
                    }
                }) {
                    Text(strings.saveCardAction, color = colors.brand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavedCardDialog = false }) {
                    Text(strings.cancel, color = colors.muted)
                }
            },
            containerColor = colors.surfaceSecondary
        )
    }

    // Server URL Dialog
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text(strings.serverUrl, color = colors.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = strings.serverUrlHint,
                        color = colors.muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateBaseUrl(tempUrl)
                    showServerDialog = false
                }) {
                    Text(strings.save, color = colors.brand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text(strings.cancel, color = colors.muted)
                }
            },
            containerColor = colors.surfaceSecondary
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    showArrow: Boolean,
    onClick: (() -> Unit)?
) {
    val colors = MoontirTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = colors.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = colors.onSurfaceSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        if (showArrow) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
