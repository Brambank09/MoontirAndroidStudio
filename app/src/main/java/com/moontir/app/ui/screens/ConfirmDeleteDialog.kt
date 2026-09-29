package com.moontir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Vehicle
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun ConfirmDeleteDialog(
    vehicle: Vehicle?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Vehicle) -> Unit
) {
    if (vehicle == null) return

    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.deleteVehicle,
                color = colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.confirmDelete,
                    color = colors.muted,
                    fontSize = 13.sp
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceTertiary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(vehicle.nickname, color = colors.onSurface, fontWeight = FontWeight.Bold)
                            Text("${vehicle.make} ${vehicle.model} · ${vehicle.plate}", color = colors.onSurfaceSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(vehicle) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                shape = RoundedCornerShape(18.dp),
                enabled = !busy
            ) {
                Text(
                    text = if (busy) "…" else strings.remove,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel, color = colors.onSurface)
            }
        },
        containerColor = colors.surfaceSecondary,
        shape = RoundedCornerShape(20.dp)
    )
}
