package com.moontir.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.data.model.Order
import com.moontir.app.ui.components.MoontirOrderCard
import com.moontir.app.ui.i18n.AppI18n
import com.moontir.app.ui.theme.MoontirTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Parses the estimated service completion timestamp from scheduleDate and scheduleTime.
 * Handles formats like:
 * - "09:30 – 11:15 (1h 45m)"
 * - "09:00 - 11:00"
 * - "Immediate 24/7 (Est. completion ~15:05)"
 * - "Segera Siaga 24/7 (Estimasi selesai ~15:05)"
 */
fun parseCompletionCalendar(scheduleDate: String, scheduleTime: String): Calendar {
    val cal = Calendar.getInstance()
    val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US)
    )
    var parsedDate: Date? = null
    for (df in dateFormats) {
        try {
            parsedDate = df.parse(scheduleDate.trim())
            if (parsedDate != null) break
        } catch (_: Exception) {}
    }
    if (parsedDate != null) {
        cal.time = parsedDate
    }
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val timeRegex = Regex("""(\d{1,2}):(\d{2})""")
    val matches = timeRegex.findAll(scheduleTime).toList()

    if (matches.size >= 2) {
        val startH = matches[0].groupValues[1].toInt()
        val startM = matches[0].groupValues[2].toInt()
        val endH = matches[1].groupValues[1].toInt()
        val endM = matches[1].groupValues[2].toInt()

        cal.set(Calendar.HOUR_OF_DAY, endH)
        cal.set(Calendar.MINUTE, endM)

        if ((endH * 60 + endM) < (startH * 60 + startM)) {
            // Spanned midnight
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else if (matches.size == 1) {
        val endH = matches[0].groupValues[1].toInt()
        val endM = matches[0].groupValues[2].toInt()
        cal.set(Calendar.HOUR_OF_DAY, endH)
        cal.set(Calendar.MINUTE, endM)
    } else {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
    }

    return cal
}

/**
 * Returns true if the current time has reached or passed the estimated service completion time.
 */
fun hasExceededCompletionTime(
    scheduleDate: String,
    scheduleTime: String,
    now: Calendar = Calendar.getInstance()
): Boolean {
    val completionCal = parseCompletionCalendar(scheduleDate, scheduleTime)
    return now.timeInMillis >= completionCal.timeInMillis
}

@Composable
fun OrdersScreen(
    orders: List<Order>,
    onOpenInvoice: (Order) -> Unit,
    onCompleteOrder: (Order) -> Unit,
    onRateOrder: (Order) -> Unit,
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
                    text = strings.orders.replaceFirstChar { it.uppercase() },
                    color = colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (strings.language == "ID") "Every dispatch and invoice in one place." else "Semua dispatch dan invoice kamu.",
                    color = colors.muted,
                    fontSize = 13.sp
                )
            }
        }

        if (orders.isEmpty()) {
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
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = colors.muted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.emptyHistory,
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.emptyHistoryHint,
                            color = colors.muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(orders) { order ->
                val isCompleted = order.status == "completed"
                val hasExceeded = hasExceededCompletionTime(order.scheduleDate, order.scheduleTime)
                val completeLabel = if (hasExceeded) strings.serviceCompleted else strings.completeNow

                MoontirOrderCard(
                    order = order,
                    strings = strings,
                    onClick = { onOpenInvoice(order) },
                    onComplete = if (!isCompleted) { { onCompleteOrder(order) } } else null,
                    onRate = if (isCompleted && order.rating == null) { { onRateOrder(order) } } else null,
                    completeButtonText = completeLabel,
                    isExceeded = hasExceeded
                )
            }
        }
    }
}
