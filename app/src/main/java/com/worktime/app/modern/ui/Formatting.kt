package com.worktime.app.modern.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

private val ruLocale = Locale.forLanguageTag("ru-RU")

fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        minutes == 0 -> "0 ч"
        rest == 0 -> "$hours ч"
        hours == 0 -> "$rest мин"
        else -> "$hours ч $rest мин"
    }
}

fun formatMoney(minor: Long, currencyCode: String): String {
    val formatter = NumberFormat.getCurrencyInstance(ruLocale)
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

fun monthTitle(month: YearMonth): String {
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
    return monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() } + " ${month.year}"
}
