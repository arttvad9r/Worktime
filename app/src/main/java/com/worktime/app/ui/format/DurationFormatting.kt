package com.worktime.app.ui.format

import java.text.NumberFormat
import java.util.Locale

internal data class DurationSummaryParts(
    val hours: String,
    val minutes: Int,
)

/**
 * Formats a non-negative duration without redundant zero minutes.
 *
 * Examples: 0 -> "0", 15h -> "15", 15h 30m -> "15:30".
 */
fun formatDurationCompact(minutes: Int): String {
    require(minutes >= 0) { "minutes must be non-negative" }
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) {
        hours.toString()
    } else {
        "$hours:${remainder.toString().padStart(2, '0')}"
    }
}

/** Summary-friendly duration: grouped hours plus a separate minute remainder. */
internal fun formatDurationSummaryParts(
    minutes: Int,
    locale: Locale = Locale.getDefault(),
): DurationSummaryParts {
    require(minutes >= 0) { "minutes must be non-negative" }
    return DurationSummaryParts(
        hours = NumberFormat.getIntegerInstance(locale).format(minutes / 60),
        minutes = minutes % 60,
    )
}
