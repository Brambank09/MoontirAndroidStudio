package com.moontir.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.BuildConfig
import com.moontir.app.data.model.User
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.MoontirTextField
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun ProfileScreen(
    user: User,
    themeMode: String,
    currentLanguage: String,
    baseUrl: String,
    isMockMode: Boolean,
    busy: Boolean,
    onUpdateName: (String) -> Unit,
    onCycleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onUpdateBaseUrl: (String) -> Unit,
    onToggleMockMode: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    var editName by remember(user.name) { mutableStateOf(user.name) }
    val isDirty = editName.trim() != user.name && editName.isNotBlank()

    var showServerDialog by remember { mutableStateOf(false) }
    var tempUrl by remember(baseUrl) { mutableStateOf(baseUrl) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
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
                    text = if (strings.language == "ID") "Kelola akun dan preferensi Moontir." else "Manage your account and Moontir preferences.",
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
                        "dark" -> strings.themeDark
                        "light" -> strings.themeLight
                        else -> strings.themeSystem
                    }
                    SettingRow(
                        icon = if (themeMode == "dark") Icons.Outlined.DarkMode else if (themeMode == "light") Icons.Outlined.LightMode else Icons.Outlined.Contrast,
                        title = strings.themeSetting,
                        value = themeLabel,
                        showArrow = true,
                        onClick = onCycleTheme
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

                    // Payment
                    SettingRow(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = strings.payment,
                        value = strings.unpaid,
                        showArrow = false,
                        onClick = {}
                    )

                    HorizontalDivider(color = colors.divider)

                    // Support
                    SettingRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = strings.support,
                        value = strings.supportValue,
                        showArrow = false,
                        onClick = {}
                    )

                    HorizontalDivider(color = colors.divider)

                    // App Version
                    SettingRow(
                        icon = Icons.Outlined.Info,
                        title = strings.appVersion,
                        value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        showArrow = false,
                        onClick = {}
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
    onClick: () -> Unit
) {
    val colors = MoontirTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
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
