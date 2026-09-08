package com.worktime.app.ui.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val DECIMAL_INPUT_PATTERN = Regex("^(?:\\d+(?:\\.\\d*)?|\\.\\d+)$")

fun sanitizeMoneyInput(value: String): String = value
    .replace(',', '.')
    .filter { it.isDigit() || it == '.' }
    .let { filtered ->
        val firstDot = filtered.indexOf('.')
        if (firstDot < 0) filtered else {
            filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "").take(2)
        }
    }
    .take(18)

fun parseDecimalMicros(text: String): Long {
    if (text.isBlank()) return 0L
    val normalized = text.replace(',', '.')
    require(DECIMAL_INPUT_PATTERN.matches(normalized)) { "Invalid decimal amount" }
    return BigDecimal(normalized)
        .movePointRight(6)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
        .also { require(it >= 0L) }
}

fun formatDecimalMicros(micros: Long): String = BigDecimal.valueOf(micros, 6)
    .stripTrailingZeros()
    .toPlainString()

fun formatAmountMicros(
    micros: Long,
    locale: Locale = Locale.getDefault(),
): String = amountFormatter(locale, grouping = true, maximumFractionDigits = 2)
    .format(BigDecimal.valueOf(micros, 6))

fun formatCompactAmountMicros(
    micros: Long,
    locale: Locale = Locale.getDefault(),
): String = amountFormatter(locale, grouping = false, maximumFractionDigits = 2)
    .format(BigDecimal.valueOf(micros, 6))

/** Whole-unit display for dense calendar surfaces; 4_810_000_000 micros -> "4810". */
fun formatWholeAmountMicros(
    micros: Long,
    locale: Locale = Locale.getDefault(),
): String = amountFormatter(locale, grouping = false, maximumFractionDigits = 0)
    .format(BigDecimal.valueOf(micros, 6))

private fun amountFormatter(
    locale: Locale,
    grouping: Boolean,
    maximumFractionDigits: Int,
): NumberFormat = NumberFormat.getNumberInstance(locale).apply {
    isGroupingUsed = grouping
    minimumFractionDigits = 0
    this.maximumFractionDigits = maximumFractionDigits
    roundingMode = RoundingMode.HALF_UP
}
