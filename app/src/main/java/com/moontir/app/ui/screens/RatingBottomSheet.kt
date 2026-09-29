package com.moontir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Order
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.MoontirStarRating
import com.moontir.app.ui.components.MoontirTextField
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingBottomSheet(
    order: Order?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmitRating: (Order, Int, String) -> Unit
) {
    if (order == null) return

    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    var stars by remember(order) { mutableIntStateOf(order.rating?.stars ?: 5) }
    var note by remember(order) { mutableStateOf(order.rating?.note ?: "") }

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
            Text(
                text = strings.rateTitle,
                color = colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.rateSub,
                color = colors.muted,
                fontSize = 13.sp
            )

            // Order Card Summary
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonOutline,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(order.serviceName, color = colors.onSurface, fontWeight = FontWeight.Bold)
                        Text("${order.vehicle.make} ${order.vehicle.model} · ${order.scheduleDate}", color = colors.onSurfaceSecondary, fontSize = 12.sp)
                    }
                }
            }

            // Interactive Stars
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    MoontirStarRating(
                        stars = stars,
                        size = 36.dp,
                        interactive = true,
                        onStarsChanged = { stars = it }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "$stars / 5",
                        color = colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            MoontirTextField(
                value = note,
                onValueChange = { note = it },
                label = strings.rateNotePlaceholder,
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
                isMultiline = true
            )

            MoontirButton(
                text = strings.submitRating,
                onClick = {
                    if (stars > 0) {
                        onSubmitRating(order, stars, note)
                    }
                },
                busy = busy,
                enabled = stars > 0
            )
        }
    }
}
