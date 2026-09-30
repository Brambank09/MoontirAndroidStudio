package com.moontir.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.R
import com.moontir.app.data.model.Order
import com.moontir.app.data.model.Service
import com.moontir.app.data.model.Vehicle
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme
import java.text.NumberFormat
import java.util.Locale

fun formatRupiah(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount).replace("Rp", "Rp ")
}

@Composable
fun MoontirLogoView(
    modifier: Modifier = Modifier,
    height: Dp = 32.dp
) {
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val logoRes = if (isLight) R.drawable.moontir_logo_dark else R.drawable.moontir_logo_light
    Image(
        painter = painterResource(id = logoRes),
        contentDescription = "Moontir Logo",
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = modifier.height(height)
    )
}

@Composable
fun MoontirTopBar(
    activeOrderCount: Int,
    currentLanguage: String,
    onToggleLanguage: () -> Unit,
    onOrdersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Language Toggle Chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceTertiary,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.clickable { onToggleLanguage() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Language",
                    tint = colors.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentLanguage.uppercase(),
                    color = colors.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Brand Logo
        MoontirLogoView(height = 26.dp)

        // Right Active Orders Badge Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onOrdersClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingBag,
                contentDescription = "Orders",
                tint = colors.onSurface,
                modifier = Modifier.size(22.dp)
            )
            if (activeOrderCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 2.dp, end = 2.dp)
                        .size(16.dp)
                        .background(colors.brand, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeOrderCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MoontirBottomBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    val items = listOf(
        Triple("home", strings.home, Icons.Outlined.Home),
        Triple("services", strings.services, Icons.Outlined.CleaningServices),
        Triple("garage", strings.garage, Icons.Outlined.DirectionsCar),
        Triple("orders", strings.orders, Icons.AutoMirrored.Outlined.ReceiptLong),
        Triple("profile", strings.profile, Icons.Outlined.Person)
    )

    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        items.forEach { (route, label, icon) ->
            val selected = currentScreen == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) colors.brand else colors.muted
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) colors.onSurface else colors.muted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun MoontirButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward
) {
    val colors = MoontirTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = RoundedCornerShape(25.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.brand,
            contentColor = colors.onBrand,
            disabledContainerColor = colors.brand.copy(alpha = 0.4f),
            disabledContentColor = colors.onBrand.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(
                color = colors.onBrand,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun MoontirOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = MoontirTheme.colors
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surfaceTertiary,
            contentColor = colors.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

@Composable
fun MoontirTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isMultiline: Boolean = false,
    maxLines: Int = if (isMultiline) 4 else 1
) {
    val colors = MoontirTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = colors.onSurfaceSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(label, color = colors.muted, fontSize = 14.sp) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, contentDescription = null, tint = colors.muted, modifier = Modifier.size(18.dp)) }
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surfaceSecondary,
                unfocusedContainerColor = colors.surfaceSecondary,
                focusedBorderColor = colors.brand,
                unfocusedBorderColor = colors.border,
                focusedTextColor = colors.onSurface,
                unfocusedTextColor = colors.onSurface
            ),
            singleLine = !isMultiline,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MoontirCategoryCircle(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary)
                .border(1.dp, colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label.uppercase(),
            color = colors.onSurface,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.1.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun MoontirServiceCard(
    service: Service,
    isIndonesian: Boolean,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onCardClick: (() -> Unit)? = null
) {
    val colors = MoontirTheme.colors
    val name = if (isIndonesian) service.nameId else service.name
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

    val iconTint = if (isEmergency) Color(0xFFEF4444) else colors.brand
    val iconBg = if (isEmergency) Color(0xFFEF4444).copy(alpha = 0.15f) else colors.surfaceSecondary

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceTertiary,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) (if (isEmergency) Color(0xFFEF4444) else colors.brand) else colors.border
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { (onCardClick ?: onBookClick)() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = groupIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (isEmergency) {
                    Text(
                        text = if (isIndonesian) "BANTUAN 24/7 DARURAT" else "24/7 EMERGENCY ASSIST",
                        color = Color(0xFFEF4444),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                } else if (isSpecial) {
                    Text(
                        text = if (isIndonesian) "PAKET ANDALAN LENGKAP" else "ALL-INCLUSIVE SIGNATURE",
                        color = colors.moonGlow,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = name,
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    color = colors.onSurfaceSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$dur · ${formatRupiah(service.price)}",
                    color = if (isEmergency) Color(0xFFF87171) else colors.moonGlow,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) (if (isEmergency) Color(0xFFEF4444) else colors.brand)
                        else colors.surfaceSecondary
                    )
                    .clickable { onBookClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isSelected) (if (isEmergency) Color.White else colors.onBrand) else colors.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MoontirVehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceTertiary,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DirectionsCar,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.nickname,
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${vehicle.make} ${vehicle.model} · ${vehicle.year} · ${vehicle.type}",
                    color = colors.onSurfaceSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vehicle.plate,
                    color = colors.moonGlow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = colors.onSurface, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = colors.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun MoontirOrderCard(
    order: Order,
    strings: com.moontir.app.ui.i18n.Strings,
    onClick: () -> Unit,
    onComplete: (() -> Unit)? = null,
    onRate: (() -> Unit)? = null,
    completeButtonText: String? = null,
    isExceeded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = MoontirTheme.colors
    val isCompleted = order.status == "completed"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceTertiary,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.serviceName,
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${order.vehicle.make} ${order.vehicle.model} · ${order.scheduleDate} · ${order.scheduleTime}",
                        color = colors.onSurfaceSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatRupiah(order.total),
                        color = colors.moonGlow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCompleted) colors.brand else colors.brandTertiary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCompleted) colors.brand else colors.borderStrong)
                ) {
                    Text(
                        text = if (isCompleted) strings.completed.uppercase() else strings.inService.uppercase(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Rating or Action buttons
            if (order.rating != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MoontirStarRating(stars = order.rating.stars, size = 16.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (order.rating.note.isNotBlank()) "“${order.rating.note}”" else "${strings.ratedLabel} ${order.rating.stars} ${strings.stars}",
                        color = colors.onSurfaceSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if ((!isCompleted && onComplete != null) || (isCompleted && onRate != null)) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isCompleted && onComplete != null) {
                        val btnLabel = completeButtonText ?: if (isExceeded) strings.serviceCompleted else strings.completeNow
                        if (isExceeded) {
                            Button(
                                onClick = onComplete,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.brand,
                                    contentColor = colors.onBrand
                                ),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.onBrand)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(btnLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onBrand)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onComplete,
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(btnLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            }
                        }
                    }
                    if (isCompleted && onRate != null) {
                        Button(
                            onClick = onRate,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.brand,
                                contentColor = colors.onBrand
                            ),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.onBrand)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.rateSpecialist, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onBrand)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoontirStarRating(
    stars: Int,
    size: Dp = 20.dp,
    interactive: Boolean = false,
    onStarsChanged: ((Int) -> Unit)? = null
) {
    val colors = MoontirTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val filled = i <= stars
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline
            val tint = if (filled) colors.brand else colors.muted
            Icon(
                imageVector = icon,
                contentDescription = "$i stars",
                tint = tint,
                modifier = Modifier
                    .size(size)
                    .clickable(enabled = interactive) {
                        onStarsChanged?.invoke(i)
                    }
            )
        }
    }
}
