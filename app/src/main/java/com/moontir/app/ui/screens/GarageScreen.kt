package com.moontir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Vehicle
import com.moontir.app.ui.components.MoontirOutlineButton
import com.moontir.app.ui.components.MoontirVehicleCard
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

@Composable
fun GarageScreen(
    vehicles: List<Vehicle>,
    onAddVehicle: () -> Unit,
    onEditVehicle: (Vehicle) -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = strings.garage.replaceFirstChar { it.uppercase() },
                    color = colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.myVehicles,
                    color = colors.muted,
                    fontSize = 13.sp
                )
            }
        }

        item {
            MoontirOutlineButton(
                text = strings.addVehicle,
                icon = Icons.Outlined.Add,
                onClick = onAddVehicle
            )
        }

        if (vehicles.isEmpty()) {
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
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (strings.language == "ID") "Your garage is empty" else "Garasi masih kosong",
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (strings.language == "ID") "Add your first car to get started." else "Tambahkan mobil pertamamu.",
                            color = colors.muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(vehicles) { vehicle ->
                MoontirVehicleCard(
                    vehicle = vehicle,
                    onEdit = { onEditVehicle(vehicle) },
                    onDelete = { onDeleteVehicle(vehicle) }
                )
            }
        }
    }
}
