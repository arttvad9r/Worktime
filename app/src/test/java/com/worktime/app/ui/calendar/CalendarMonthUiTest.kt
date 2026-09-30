package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalendarMonthUiTest {
    private val locale = Locale.US
    private val month = YearMonth.of(2026, 8)
    private val today = LocalDate.of(2026, 8, 12)

    private fun build(entries: Map<LocalDate, WorkEntry> = emptyMap()) =
        buildCalendarMonthUi(month, entries, today, locale)

    @Test
    fun `grid is six Monday-first weeks of seven days`() {
        val ui = build()

        assertEquals(6, ui.weeks.size)
        assertTrue(ui.weeks.all { it.size == 7 })
        assertEquals(LocalDate.of(2026, 7, 27), ui.weeks.first().first().date)
        assertEquals(LocalDate.of(2026, 9, 6), ui.weeks.last().last().date)
    }

    @Test
    fun `only days of the month are marked in month and only today is marked today`() {
        val cells = build().weeks.flatten()

        assertEquals(31, cells.count { it.inMonth })
        assertEquals(listOf(today), cells.filter { it.isToday }.map { it.date })
    }

    @Test
    fun `entry cell carries formatted duration and amount`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 5),
            workedMinutes = 8 * 60 + 30,
            hourlyRateMicros = 100_000_000L,
        )
        val cell = build(mapOf(entry.date to entry)).weeks.flatten().first { it.date == entry.date }

        assertTrue(cell.hasEntry)
        assertEquals("8:30", cell.durationText)
        assertEquals("850", cell.amountText)
        assertFalse(cell.isNegative)
    }

    @Test
    fun `bonus and penalty flags are exposed and negative total is flagged`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 6),
            workedMinutes = 60,
            hourlyRateMicros = 100_000_000L,
            penaltyMicros = 300_000_000L,
        )
        val cell = build(mapOf(entry.date to entry)).weeks.flatten().first { it.date == entry.date }

        assertTrue(cell.hasPenalty)
        assertFalse(cell.hasBonus)
        assertTrue(cell.isNegative)
        assertEquals("200", cell.amountText?.removePrefix("-")?.removePrefix("−"))
    }

    @Test
    fun `entries outside the month are ignored`() {
        val outside = WorkEntry(
            date = LocalDate.of(2026, 7, 30),
            workedMinutes = 480,
            hourlyRateMicros = 100_000_000L,
        )
        val cell = build(mapOf(outside.date to outside)).weeks.flatten().first { it.date == outside.date }

        assertFalse(cell.inMonth)
        assertFalse(cell.hasEntry)
        assertNull(cell.durationText)
    }

    @Test
    fun `bonus only entry has no duration text but shows its amount`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 7),
            workedMinutes = 0,
            hourlyRateMicros = 100_000_000L,
            bonusMicros = 500_000_000L,
        )
        val cell = build(mapOf(entry.date to entry)).weeks.flatten().first { it.date == entry.date }

        assertTrue(cell.hasEntry)
        assertTrue(cell.hasBonus)
        assertNull(cell.durationText)
        assertEquals("500", cell.amountText)
    }
}
