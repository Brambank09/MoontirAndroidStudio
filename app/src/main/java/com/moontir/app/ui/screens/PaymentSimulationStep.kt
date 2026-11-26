package com.moontir.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.ui.components.formatRupiah
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirDarkEspresso
import com.moontir.app.ui.theme.MoontirSageGreen
import com.moontir.app.ui.theme.MoontirTheme
import com.moontir.app.ui.theme.MoontirWarmIvory
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID

fun formatCardNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(16)
    return digits.chunked(4).joinToString(" ")
}

fun formatCardExpiry(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(4)
    return if (digits.length > 2) {
        "${digits.substring(0, 2)}/${digits.substring(2)}"
    } else {
        digits
    }
}

fun validateCardPaymentInput(
    cardNumber: String,
    cardHolder: String,
    expiry: String,
    cvv: String,
    isIndonesian: Boolean
): String? {
    val digits = cardNumber.filter { it.isDigit() }
    if (digits.length < 16) {
        return if (isIndonesian) "Nomor kartu harus terdiri dari 16 digit." else "Card number must be 16 digits."
    }
    if (cardHolder.trim().length < 2) {
        return if (isIndonesian) "Masukkan nama pemilik kartu." else "Please enter the cardholder name."
    }
    val expDigits = expiry.filter { it.isDigit() }
    if (expDigits.length != 4) {
        return if (isIndonesian) "Format masa berlaku harus BB/TT." else "Expiry date must use MM/YY format."
    }
    val month = expDigits.substring(0, 2).toIntOrNull() ?: 0
    if (month !in 1..12) {
        return if (isIndonesian) "Bulan masa berlaku tidak valid (01-12)." else "Expiry month must be between 01 and 12."
    }
    val cvvDigits = cvv.filter { it.isDigit() }
    if (cvvDigits.length !in 3..4) {
        return if (isIndonesian) "CVV harus terdiri dari 3 atau 4 digit." else "CVV must be 3 or 4 digits."
    }
    return null
}

fun generateSimulatedPaymentRef(method: String): String {
    val prefix = when (method) {
        "card" -> "CARD"
        "qris" -> "QRIS"
        else -> "SIM"
    }
    val code = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
    return "MTR-$prefix-$code"
}

/**
 * Generates a deterministic 25x25 QR-style matrix with standard finder patterns,
 * separators, timing patterns, and an alignment pattern for the local QRIS prototype.
 */
fun generateDeterministicQrMatrix(payload: String, size: Int = 25): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }
    val reserved = Array(size) { BooleanArray(size) }

    fun placeFinder(startRow: Int, startCol: Int) {
        for (r in -1..7) {
            for (c in -1..7) {
                val nr = startRow + r
                val nc = startCol + c
                if (nr in 0 until size && nc in 0 until size) {
                    reserved[nr][nc] = true
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    val isInside = r in 0..6 && c in 0..6
                    matrix[nr][nc] = isInside && (isBorder || isCenter)
                }
            }
        }
    }

    placeFinder(0, 0)
    placeFinder(0, size - 7)
    placeFinder(size - 7, 0)

    // Alignment pattern near bottom-right (for 25x25 version-2 QR layout)
    val alignCenter = size - 7
    for (r in -2..2) {
        for (c in -2..2) {
            val nr = alignCenter + r
            val nc = alignCenter + c
            if (nr in 0 until size && nc in 0 until size) {
                reserved[nr][nc] = true
                val dist = maxOf(kotlin.math.abs(r), kotlin.math.abs(c))
                matrix[nr][nc] = dist == 0 || dist == 2
            }
        }
    }

    // Timing patterns
    for (i in 8 until size - 8) {
        if (!reserved[6][i]) {
            reserved[6][i] = true
            matrix[6][i] = (i % 2 == 0)
        }
        if (!reserved[i][6]) {
            reserved[i][6] = true
            matrix[i][6] = (i % 2 == 0)
        }
    }

    // Deterministic data modules from payload hash
    var seed = payload.fold(1125899906842597L) { acc, ch -> acc * 31L + ch.code.toLong() }
    for (r in 0 until size) {
        for (c in 0 until size) {
            if (!reserved[r][c]) {
                seed = seed xor (seed shl 13)
                seed = seed xor (seed ushr 7)
                seed = seed xor (seed shl 17)
                matrix[r][c] = (seed and 1L) == 1L
            }
        }
    }

    return matrix
}

@Composable
fun PaymentSimulationContent(
    paymentMethod: String, // "card" or "qris"
    grandTotal: Long,
    serviceSummary: String,
    paymentRef: String,
    isIndonesian: Boolean,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    cardHolder: String,
    onCardHolderChange: (String) -> Unit,
    cardExpiry: String,
    onCardExpiryChange: (String) -> Unit,
    cardCvv: String,
    onCardCvvChange: (String) -> Unit,
    validationError: String?,
    isProcessing: Boolean,
    hasSavedCard: Boolean = false,
    onFillDemoCard: () -> Unit,
    onRefreshQris: () -> Unit
) {
    val colors = MoontirTheme.colors
    val strings = AppI18n.current

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Honest Local Simulation Notice
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceTertiary,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.paymentSimulationNotice,
                    color = colors.onSurfaceSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // Amount & Order Summary Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceSecondary,
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = serviceSummary,
                        color = colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${strings.paymentRef}: $paymentRef",
                        color = colors.muted,
                        fontSize = 11.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.total.uppercase(),
                        color = colors.onSurfaceSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = formatRupiah(grandTotal),
                        color = colors.moonGlow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (isProcessing) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceSecondary,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = colors.brand,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.processingPayment,
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        if (paymentMethod == "card") {
            // Live Card Preview (Moontir Espresso + Sage brand palette)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MoontirDarkEspresso,
                border = androidx.compose.foundation.BorderStroke(1.dp, MoontirSageGreen.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                                tint = MoontirSageGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.cardPayment,
                                color = MoontirWarmIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (isIndonesian) "SIMULASI" else "SIMULATION",
                            color = MoontirSageGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val previewNumber = if (cardNumber.isBlank()) {
                        "0000  0000  0000  0000"
                    } else {
                        formatCardNumber(cardNumber)
                    }
                    Text(
                        text = previewNumber,
                        color = MoontirWarmIvory,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.cardHolderName.uppercase(),
                                color = MoontirWarmIvory.copy(alpha = 0.65f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cardHolder.ifBlank { if (isIndonesian) "NAMA ANDA" else "YOUR NAME" }.uppercase(),
                                color = MoontirWarmIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isIndonesian) "BERLAKU" else "VALID THRU",
                                color = MoontirWarmIvory.copy(alpha = 0.65f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = cardExpiry.ifBlank { "MM/YY" },
                                color = MoontirWarmIvory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Saved card indicator or Quick Fill Demo Card helper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasSavedCard) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.brand,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = strings.cardAutoFilledNote,
                            color = colors.onSurfaceSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(
                    onClick = onFillDemoCard,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.fillDemoCard,
                        color = colors.brand,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Card Number Input
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { onCardNumberChange(formatCardNumber(it)) },
                label = { Text(strings.cardNumber, fontSize = 12.sp) },
                placeholder = { Text("0000 0000 0000 0000", color = colors.muted) },
                leadingIcon = { Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = colors.muted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brand,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceTertiary,
                    unfocusedContainerColor = colors.surfaceTertiary,
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface,
                    focusedLabelColor = colors.brand,
                    unfocusedLabelColor = colors.onSurfaceSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Cardholder Name Input
            OutlinedTextField(
                value = cardHolder,
                onValueChange = onCardHolderChange,
                label = { Text(strings.cardHolderName, fontSize = 12.sp) },
                placeholder = { Text(if (isIndonesian) "Nama sesuai kartu" else "Your Name", color = colors.muted) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = colors.muted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brand,
                    unfocusedBorderColor = colors.border,
                    focusedContainerColor = colors.surfaceTertiary,
                    unfocusedContainerColor = colors.surfaceTertiary,
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface,
                    focusedLabelColor = colors.brand,
                    unfocusedLabelColor = colors.onSurfaceSecondary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Expiry + CVV Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = cardExpiry,
                    onValueChange = { onCardExpiryChange(formatCardExpiry(it)) },
                    label = { Text(strings.cardExpiry, fontSize = 12.sp) },
                    placeholder = { Text("MM/YY", color = colors.muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brand,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceTertiary,
                        unfocusedContainerColor = colors.surfaceTertiary,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedLabelColor = colors.brand,
                        unfocusedLabelColor = colors.onSurfaceSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = cardCvv,
                    onValueChange = { onCardCvvChange(it.filter { ch -> ch.isDigit() }.take(4)) },
                    label = { Text(strings.cardCvv, fontSize = 12.sp) },
                    placeholder = { Text("123", color = colors.muted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.brand,
                        unfocusedBorderColor = colors.border,
                        focusedContainerColor = colors.surfaceTertiary,
                        unfocusedContainerColor = colors.surfaceTertiary,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedLabelColor = colors.brand,
                        unfocusedLabelColor = colors.onSurfaceSecondary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError,
                    color = colors.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // QRIS Simulation View
            var secondsRemaining by remember(paymentRef) { mutableIntStateOf(15 * 60) }
            LaunchedEffect(paymentRef) {
                while (secondsRemaining > 0) {
                    delay(1000L)
                    secondsRemaining--
                }
            }
            val mm = secondsRemaining / 60
            val ss = secondsRemaining % 60
            val timerText = String.format(Locale.US, "%02d:%02d", mm, ss)

            val qrPayload = remember(paymentRef, grandTotal) {
                "MOONTIR-QRIS|$paymentRef|$grandTotal"
            }
            val qrMatrix = remember(qrPayload) {
                generateDeterministicQrMatrix(qrPayload, size = 25)
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QRIS",
                                color = Color(0xFF191715),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Moontir Home Care · ${if (isIndonesian) "Simulasi" else "Simulation"}",
                                color = Color(0xFF5C544D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF5F0E6),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33403A35))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF403A35),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timerText,
                                    color = Color(0xFF403A35),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Crisp Canvas-drawn deterministic QR matrix on high-contrast white quiet zone
                    Box(
                        modifier = Modifier
                            .size(216.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x22403A35), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cellSize = size.width / qrMatrix.size
                            for (r in qrMatrix.indices) {
                                for (c in qrMatrix[r].indices) {
                                    if (qrMatrix[r][c]) {
                                        drawRect(
                                            color = Color(0xFF191715),
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = formatRupiah(grandTotal),
                        color = Color(0xFF191715),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.qrisScanHint,
                        color = Color(0xFF5C544D),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(onClick = onRefreshQris) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF403A35),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.refreshQr,
                            color = Color(0xFF403A35),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
