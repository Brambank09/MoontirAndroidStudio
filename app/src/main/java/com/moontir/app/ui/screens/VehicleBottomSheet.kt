package com.moontir.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Vehicle
import com.moontir.app.ui.components.MoontirButton
import com.moontir.app.ui.components.MoontirTextField
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme

private val VEHICLE_TYPES = listOf("Sedan", "Hatchback", "MPV", "SUV", "Pickup", "Truck")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleBottomSheet(
    isOpen: Boolean,
    vehicleToEdit: Vehicle?,
    busy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (nickname: String, make: String, model: String, year: String, plate: String, type: String, editId: String?) -> Unit
) {
    if (!isOpen) return

    val colors = MoontirTheme.colors
    val strings = AppI18n.current
    val isEdit = vehicleToEdit != null

    var nickname by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.nickname ?: "") }
    var make by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.make ?: "") }
    var model by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.model ?: "") }
    var year by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.year ?: "") }
    var plate by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.plate ?: "") }
    var type by remember(vehicleToEdit) { mutableStateOf(vehicleToEdit?.type ?: "Sedan") }

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEdit) strings.editVehicle else strings.addVehicle,
                color = colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            MoontirTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = strings.nickname,
                leadingIcon = Icons.Outlined.BookmarkBorder
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoontirTextField(
                    value = make,
                    onValueChange = { make = it },
                    label = strings.make,
                    leadingIcon = Icons.Outlined.DirectionsCar,
                    modifier = Modifier.weight(1f)
                )
                MoontirTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = strings.model,
                    leadingIcon = Icons.Outlined.Build,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoontirTextField(
                    value = year,
                    onValueChange = { year = it },
                    label = strings.year,
                    leadingIcon = Icons.Outlined.CalendarToday,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                MoontirTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = strings.plate,
                    leadingIcon = Icons.Outlined.Pin,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = strings.vehicleType.uppercase(),
                color = colors.onSurfaceSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VEHICLE_TYPES) { itemType ->
                    val isSelected = type == itemType
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) colors.brand else colors.surfaceTertiary,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) colors.brand else colors.border),
                        modifier = Modifier.clickable { type = itemType }
                    ) {
                        Text(
                            text = itemType.uppercase(),
                            color = if (isSelected) colors.onBrand else colors.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(errorMessage, color = colors.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            MoontirButton(
                text = if (isEdit) strings.saveChanges else strings.save,
                onClick = {
                    onSave(nickname, make, model, year, plate, type, vehicleToEdit?.id)
                },
                busy = busy,
                enabled = nickname.isNotBlank() && make.isNotBlank() && model.isNotBlank() && year.isNotBlank() && plate.isNotBlank()
            )
        }
    }
}
