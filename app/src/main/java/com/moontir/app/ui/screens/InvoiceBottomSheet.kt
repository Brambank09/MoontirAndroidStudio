package com.moontir.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.moontir.app.data.model.Order
import com.moontir.app.ui.components.MoontirStarRating
import com.moontir.app.ui.components.formatRupiah
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBottomSheet(
    order: Order?,
    isIndonesian: Boolean,
    onDismiss: () -> Unit,
    onCompleteOrder: (Order) -> Unit,
    onRateOrder: (Order) -> Unit
) {
    if (order == null) return

    val colors = MoontirTheme.colors
    val strings = AppI18n.current
    val isCompleted = order.status == "completed"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Kicker
            Text(
                text = "HOME VEHICLE CARE · ${order.id.take(8).uppercase()}",
                color = colors.onSurfaceSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )

            // Section 1: Service Timeline
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NearMe, contentDescription = null, tint = colors.brand, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.trackingHeading.uppercase(),
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }

            Column(modifier = Modifier.padding(start = 4.dp)) {
                order.statusHistory.forEachIndexed { index, item ->
                    val isLast = index == order.statusHistory.size - 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(16.dp).padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(colors.brand)
                            )
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(30.dp)
                                        .background(colors.divider)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.padding(bottom = if (isLast) 0.dp else 12.dp)) {
                            val label = if (isIndonesian && !item.labelId.isNullOrBlank()) item.labelId else item.label
                            Text(label, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(item.at, color = colors.muted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Rating card if already rated
            if (order.rating != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        MoontirStarRating(stars = order.rating.stars, size = 20.dp)
                        if (order.rating.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "“${order.rating.note}”",
                                color = colors.onSurface,
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.rating.ratedAt ?: "", color = colors.muted, fontSize = 10.sp)
                    }
                }
            } else {
                // Action buttons: Complete / Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isCompleted) {
                        OutlinedButton(
                            onClick = { onCompleteOrder(order) },
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(strings.markComplete, color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = { onRateOrder(order) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.brand,
                            contentColor = colors.onBrand
                        ),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = colors.onBrand, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.rateSpecialist, color = colors.onBrand, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: Invoice Detail
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = colors.brand, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.invoiceHeading.uppercase(),
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.4.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(order.serviceName, color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${order.vehicle.make} ${order.vehicle.model} · ${order.vehicle.plate} · ${order.vehicle.type}", color = colors.onSurfaceSecondary, fontSize = 12.sp)

                    HorizontalDivider(color = colors.divider)

                    Text(strings.address.uppercase(), color = colors.onSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(order.address.label, color = colors.onSurface, fontSize = 12.sp)

                    Text(strings.schedule.uppercase(), color = colors.onSurfaceSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text("${order.scheduleDate} · ${order.scheduleTime}", color = colors.onSurface, fontSize = 12.sp)

                    HorizontalDivider(color = colors.borderStrong)

                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val label = if (isIndonesian && !item.labelId.isNullOrBlank()) item.labelId else item.label
                            Text(label, color = colors.onSurfaceSecondary, fontSize = 12.sp)
                            Text(formatRupiah(item.amount), color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = colors.borderStrong)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.total.uppercase(), color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        Text(formatRupiah(order.total), color = colors.moonGlow, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
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
