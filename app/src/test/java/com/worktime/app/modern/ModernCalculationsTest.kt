package com.worktime.app.modern

import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath
import java.time.LocalDate
import java.time.YearMonth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ModernCalculationsTest {
    @Test
    fun `six week grid always has 42 dates starting on monday`() {
        val grid = WorkTimeMath.sixWeekGrid(YearMonth.of(2026, 2))
        assertEquals(42, grid.size)
        assertEquals(1, grid.first().dayOfWeek.value)
        assertEquals(LocalDate.of(2026, 2, 1), grid[6])
    }

    @Test
    fun `pay uses exact minutes and half up rounding`() {
        val day = WorkDay(
            date = LocalDate.of(2026, 9, 12),
            workedMinutes = 90,
            hourlyRateMinor = 25_001,
            bonusMinor = 1_000,
            penaltyMinor = 500,
        )
        val pay = WorkTimeMath.payForDay(day)
        assertEquals(37_502, pay.baseMinor)
        assertEquals(38_002, pay.totalMinor)
    }

    @Test
    fun `summary counts only days with worked time as shifts`() {
        val days = listOf(
            WorkDay(LocalDate.of(2026, 9, 1), 480, 20_000, bonusMinor = 1_000),
            WorkDay(LocalDate.of(2026, 9, 2), 0, 0, penaltyMinor = 500),
        )
        val summary = WorkTimeMath.summarize(days)
        assertEquals(1, summary.shiftCount)
        assertEquals(480, summary.workedMinutes)
        assertEquals(160_000, summary.baseMinor)
        assertEquals(1_000, summary.bonusMinor)
        assertEquals(500, summary.penaltyMinor)
        assertEquals(160_500, summary.totalMinor)
    }
}
