package com.worktime.app.ui.dayeditor

import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.parseDecimalMicros
import java.time.LocalDate

internal data class DayEditorInput(
    val duration: String,
    val rate: String,
    val bonus: String,
    val penalty: String,
)

/** [entry] is non-null only when every field is valid and the entry carries data worth saving. */
internal data class DayEditorState(
    val entry: WorkEntry?,
    val totalMicros: Long?,
    val durationError: Boolean,
    val rateError: Boolean,
    val bonusError: Boolean,
    val penaltyError: Boolean,
) {
    val canSave: Boolean get() = entry != null && totalMicros != null
}

internal fun evaluateDayEditor(
    date: LocalDate,
    input: DayEditorInput,
    note: String = "",
): DayEditorState {
    val workedMinutes = parseDurationMinutes(input.duration)
    val rate = parseMoneyOrNull(input.rate)
    val bonus = parseMoneyOrNull(input.bonus)
    val penalty = parseMoneyOrNull(input.penalty)

    val positiveRateRequired = (workedMinutes ?: 0) > 0
    val rateError = rate == null ||
        rate > MoneyLimits.MAX_COMPONENT_MICROS ||
        (positiveRateRequired && rate == 0L)
    val bonusError = bonus == null || bonus > MoneyLimits.MAX_COMPONENT_MICROS
    val penaltyError = penalty == null || penalty > MoneyLimits.MAX_COMPONENT_MICROS
    val durationError = workedMinutes == null

    val hasData = workedMinutes != null &&
        (workedMinutes > 0 || (bonus ?: 0L) > 0L || (penalty ?: 0L) > 0L)
    val entry = if (
        !durationError && !rateError && !bonusError && !penaltyError && hasData
    ) {
        runCatching {
            WorkEntry(
                date = date,
                workedMinutes = workedMinutes!!,
                hourlyRateMicros = rate!!,
                bonusMicros = bonus!!,
                penaltyMicros = penalty!!,
                note = note,
            )
        }.getOrNull()
    } else {
        null
    }
    val total = entry?.let { runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull() }
    return DayEditorState(
        entry = entry,
        totalMicros = total,
        durationError = durationError,
        rateError = rateError,
        bonusError = bonusError,
        penaltyError = penaltyError,
    )
}

/** Blank means zero; valid range is 0:00..24:00 with minutes below 60. */
internal fun parseDurationMinutes(text: String): Int? {
    if (text.isBlank()) return 0
    val parts = text.split(':')
    if (parts.size > 2) return null
    val hours = parts[0].ifBlank { "0" }.toIntOrNull() ?: return null
    val minutes = parts.getOrNull(1)?.ifBlank { "0" }?.toIntOrNull() ?: 0
    if (hours !in 0..24 || minutes !in 0..59) return null
    if (hours == 24 && minutes != 0) return null
    return hours * 60 + minutes
}

private fun parseMoneyOrNull(text: String): Long? = runCatching { parseDecimalMicros(text) }.getOrNull()

internal fun sanitizeDurationInput(value: String): String {
    val filtered = value.filter { it.isDigit() || it == ':' }
    val firstColon = filtered.indexOf(':')
    if (firstColon >= 0) {
        val rawHours = filtered.take(firstColon).filter(Char::isDigit)
        val hours = normalizeLeadingZeroes(rawHours).take(2)
        val minutes = filtered.drop(firstColon + 1).filter(Char::isDigit).take(2)
        return "$hours:$minutes"
    }

    val rawDigits = filtered.filter(Char::isDigit).take(4)
    val digits = if (rawDigits.length > 1) normalizeLeadingZeroes(rawDigits) else rawDigits
    return when (digits.length) {
        0, 1 -> digits
        2 -> {
            if (digits.toInt() <= 24) digits else "${digits.take(1)}:${digits.drop(1)}"
        }
        3 -> {
            val twoDigitHours = digits.take(2).toInt()
            if (twoDigitHours <= 24) {
                "${digits.take(2)}:${digits.drop(2)}"
            } else {
                "${digits.take(1)}:${digits.drop(1)}"
            }
        }
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}

private fun normalizeLeadingZeroes(value: String): String {
    if (value.isEmpty()) return value
    return value.trimStart('0').ifEmpty { "0" }
}

