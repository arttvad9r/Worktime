package com.worktime.app.modern.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

fun formatMinutes(minutes: Int, locale: Locale = Locale.getDefault()): String {
    val hours = minutes / 60
    val rest = minutes % 60
    val hourUnit = if (locale.language == "ru") "ч" else "h"
    val minuteUnit = if (locale.language == "ru") "мин" else "min"
    return when {
        minutes == 0 -> "0 $hourUnit"
        rest == 0 -> "$hours $hourUnit"
        hours == 0 -> "$rest $minuteUnit"
        else -> "$hours $hourUnit $rest $minuteUnit"
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
