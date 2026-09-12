package com.worktime.app.modern.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.worktime.app.R
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

@Composable
fun formatDuration(minutes: Int): String {
    require(minutes >= 0)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        minutes == 0 -> stringResource(R.string.modern_duration_zero)
        rest == 0 -> stringResource(R.string.modern_duration_hours, hours)
        hours == 0 -> stringResource(R.string.modern_duration_minutes, rest)
        else -> stringResource(R.string.modern_duration_hours_minutes, hours, rest)
    }
}

fun formatMoney(minor: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val formatter = NumberFormat.getCurrencyInstance(locale)
    formatter.currency = runCatching { Currency.getInstance(currencyCode) }.getOrDefault(Currency.getInstance("RUB"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
    return formatter.format(BigDecimal.valueOf(minor, 2))
}

fun moneyInput(minor: Long): String = BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()

fun parseMoneyMinor(value: String): Long? = runCatching {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isEmpty()) return@runCatching 0L
    BigDecimal(normalized)
        .setScale(2, RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
}.getOrNull()

fun monthTitle(month: YearMonth, locale: Locale = Locale.getDefault()): String {
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
    return monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() } + " ${month.year}"
}
