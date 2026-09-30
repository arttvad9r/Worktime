package com.worktime.app.ui.format

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Shared visible-date formatting so the same date is presented consistently across screens. */
fun formatMonthTitle(
    month: YearMonth,
    locale: Locale = Locale.getDefault(),
    abbreviated: Boolean = false,
): String = month
    .format(DateTimeFormatter.ofPattern(if (abbreviated) "LLL yyyy" else "LLLL yyyy", locale))
    .capitalized(locale)

fun formatDateTitle(
    date: LocalDate,
    locale: Locale = Locale.getDefault(),
): String = date
    .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
    .capitalized(locale)

fun formatDateCompact(
    date: LocalDate,
    locale: Locale = Locale.getDefault(),
): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))

fun formatMonthAbbreviation(
    month: Month,
    locale: Locale = Locale.getDefault(),
): String = month
    .getDisplayName(TextStyle.SHORT_STANDALONE, locale)
    .capitalized(locale)

private fun String.capitalized(locale: Locale): String = replaceFirstChar { first ->
    first.toString().uppercase(locale)
}
