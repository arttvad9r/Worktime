package com.worktime.app.modern

import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath
import com.worktime.app.modern.ui.parseMoneyMinor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernBoundaryTest {
    @Test
    fun `six week grid handles leap month and keeps monday to sunday boundaries`() {
        val grid = WorkTimeMath.sixWeekGrid(YearMonth.of(2028, 2))

        assertEquals(42, grid.size)
        assertEquals(42, grid.distinct().size)
        assertEquals(LocalDate.of(2028, 1, 31), grid.first())
        assertEquals(LocalDate.of(2028, 3, 12), grid.last())
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, grid.last().dayOfWeek)
        assertTrue(LocalDate.of(2028, 2, 29) in grid)
    }

    @Test
    fun `six week grid crosses year boundary without changing cell count`() {
        val grid = WorkTimeMath.sixWeekGrid(YearMonth.of(2026, 12))

        assertEquals(42, grid.size)
        assertEquals(LocalDate.of(2026, 11, 30), grid.first())
        assertEquals(LocalDate.of(2027, 1, 10), grid.last())
    }

    @Test
    fun `maximum duration remains exact and adjustment only day is not a shift`() {
        val fullDay = WorkDay(
            date = LocalDate.of(2026, 12, 31),
            workedMinutes = 1_440,
            hourlyRateMinor = 12_345,
        )
        val adjustmentOnly = WorkDay(
            date = LocalDate.of(2027, 1, 1),
            workedMinutes = 0,
            hourlyRateMinor = 0,
            bonusMinor = 500,
            penaltyMinor = 125,
            otherMinor = -25,
        )

        assertEquals(296_280L, WorkTimeMath.payForDay(fullDay).baseMinor)
        val summary = WorkTimeMath.summarize(listOf(fullDay, adjustmentOnly))
        assertEquals(1, summary.shiftCount)
        assertEquals(1_440, summary.workedMinutes)
        assertEquals(296_630L, summary.totalMinor)
    }

    @Test
    fun `money parser accepts comma rounds half up and rejects malformed input`() {
        assertEquals(12_346L, parseMoneyMinor("123,456"))
        assertEquals(-1_234L, parseMoneyMinor("-12.34"))
        assertEquals(0L, parseMoneyMinor(""))
        assertNull(parseMoneyMinor("12.3.4"))
        assertNull(parseMoneyMinor("1e3"))
    }
}
