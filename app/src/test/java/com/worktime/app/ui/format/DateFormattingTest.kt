package com.worktime.app.ui.format

import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DateFormattingTest {
    private val locale = Locale.US

    @Test
    fun `month title uses full month and year`() {
        assertEquals(
            "September 2026",
            formatMonthTitle(YearMonth.of(2026, 9), locale),
        )
    }

    @Test
    fun `day title keeps weekday day and full month`() {
        assertEquals(
            "Wednesday, 30 September",
            formatDateTitle(LocalDate.of(2026, 9, 30), locale),
        )
    }

    @Test
    fun `compact date keeps day short month and year`() {
        assertEquals(
            "30 Sep 2026",
            formatDateCompact(LocalDate.of(2026, 9, 30), locale),
        )
    }

    @Test
    fun `month abbreviation is title cased`() {
        assertEquals("Sep", formatMonthAbbreviation(Month.SEPTEMBER, locale))
    }
}
