package com.moontir.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moontir.app.data.model.Service
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.formatRupiah
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun ServiceDetailDialog(
    service: Service?,
    isIndonesian: Boolean,
    onDismiss: () -> Unit,
    onBookClick: (Service) -> Unit
) {
    if (service == null) return

    val colors = MoontirTheme.colors
    val name = if (isIndonesian) service.nameId else service.name
    val category = if (isIndonesian) service.categoryId else service.category
    val desc = if (isIndonesian) service.descriptionId else service.description
    val dur = if (isIndonesian) service.durationId else service.duration
    val isEmergency = service.group == "emergency"
    val isSpecial = service.group == "special"

    val groupIcon = when (service.group) {
        "emergency" -> Icons.Outlined.WarningAmber
        "car_service", "light" -> Icons.Outlined.Build
        "detail" -> Icons.Outlined.AutoAwesome
        else -> Icons.Outlined.DarkMode
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergency) Color(0xFFEF4444) else colors.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Bar with category & close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isEmergency) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.surfaceTertiary,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isEmergency) Color(0xFFEF4444) else colors.border)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = groupIcon,
                                    contentDescription = null,
                                    tint = if (isEmergency) Color(0xFFEF4444) else colors.moonGlow,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.uppercase(),
                                    color = if (isEmergency) Color(0xFFEF4444) else colors.onSurface,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Service Name
                    Text(
                        text = name,
                        color = colors.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price and Duration Badge Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatRupiah(service.price),
                            color = if (isEmergency) Color(0xFFEF4444) else colors.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Text(
                            text = "·",
                            color = colors.muted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = colors.muted,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = dur,
                                color = colors.muted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Detailed Description
                    Text(
                        text = desc,
                        color = colors.onSurfaceSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    // Features Section
                    if (service.features.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = if (isIndonesian) "CAKUPAN LAYANAN" else "WHAT'S INCLUDED",
                            color = colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            service.features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isEmergency) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.surfaceTertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isEmergency) Color(0xFFEF4444) else colors.moonGlow,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = feature,
                                        color = colors.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Button: Book this service
                    MoontirButton(
                        text = if (isIndonesian) "PILIH LAYANAN INI" else "BOOK THIS SERVICE",
                        onClick = {
                            onDismiss()
                            onBookClick(service)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text(
                            text = if (isIndonesian) "Tutup" else "Close",
                            color = colors.onSurfaceSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
