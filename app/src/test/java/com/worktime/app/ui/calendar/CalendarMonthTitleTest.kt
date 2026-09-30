package com.worktime.app.ui.calendar

import java.time.YearMonth
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarMonthTitleTest {
    @Test
    fun largeFontUsesShortLocalizedMonthAndKeepsYear() {
        val title = calendarMonthTitle(
            visibleMonth = YearMonth.of(2026, 9),
            locale = Locale.forLanguageTag("ru"),
            largeFont = true,
        )

        assertEquals("Сент. 2026", title)
    }

    @Test
    fun normalFontKeepsFullLocalizedMonthAndYear() {
        val title = calendarMonthTitle(
            visibleMonth = YearMonth.of(2026, 9),
            locale = Locale.forLanguageTag("ru"),
            largeFont = false,
        )

        assertEquals("Сентябрь 2026", title)
    }
}
