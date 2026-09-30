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

/** Whole amounts have no decimals; anything else always shows two ("2 712.50"). */
fun formatAmountMicros(
    micros: Long,
    locale: Locale = Locale.getDefault(),
): String {
    val amount = BigDecimal.valueOf(micros, 6).setScale(2, RoundingMode.HALF_UP)
    val whole = amount.stripTrailingZeros().scale() <= 0
    return NumberFormat.getNumberInstance(locale).apply {
        isGroupingUsed = true
        minimumFractionDigits = if (whole) 0 else 2
        maximumFractionDigits = 2
        roundingMode = RoundingMode.HALF_UP
    }.format(amount)
}
