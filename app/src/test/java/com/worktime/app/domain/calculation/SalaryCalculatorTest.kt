package com.worktime.app.domain.calculation

import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SalaryCalculatorTest {
    @ParameterizedTest
    @MethodSource("goldenCases")
    fun `salary golden cases`(
        workedMinutes: Int,
        hourlyRateMicros: Long,
        bonusMicros: Long,
        penaltyMicros: Long,
        expectedTotalMicros: Long,
    ) {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = workedMinutes,
            hourlyRateMicros = hourlyRateMicros,
            bonusMicros = bonusMicros,
            penaltyMicros = penaltyMicros,
        )

        assertEquals(expectedTotalMicros, SalaryCalculator.entryPay(entry).totalPayMicros)
    }

    @Test
    fun `half cent rounds up`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = 30,
            hourlyRateMicros = 30_000,
        )
        assertEquals(20_000L, SalaryCalculator.entryPay(entry).basePayMicros)
    }

    @Test
    fun `entry pay is rounded to cents`() {
        val entry = WorkEntry(
            date = LocalDate.of(2026, 8, 20),
            workedMinutes = 25,
            hourlyRateMicros = 100_000_000,
        )
        assertEquals(41_670_000L, SalaryCalculator.entryPay(entry).basePayMicros)
    }

    @Test
    fun `month total equals sum of rounded entries`() {
        val entries = (1..3).map {
            WorkEntry(
                date = LocalDate.of(2026, 8, it),
                workedMinutes = 25,
                hourlyRateMicros = 100_000_000,
            )
        }
        val summary = SalaryCalculator.monthSummary(entries)
        assertEquals(125_010_000L, summary.totalPayMicros)
        assertEquals(entries.sumOf { SalaryCalculator.entryPay(it).totalPayMicros }, summary.totalPayMicros)
    }

    @Test
    fun `month summary separates base adjustments and shift count`() {
        val entries = listOf(
            WorkEntry(
                date = LocalDate.of(2026, 8, 20),
                workedMinutes = 60,
                hourlyRateMicros = 10_000_000,
                bonusMicros = 2_000_000,
            ),
            WorkEntry(
                date = LocalDate.of(2026, 8, 21),
                workedMinutes = 0,
                hourlyRateMicros = 0,
                bonusMicros = 3_000_000,
                penaltyMicros = 1_000_000,
            ),
        )

        val summary = SalaryCalculator.monthSummary(entries)
        assertEquals(60, summary.workedMinutes)
        assertEquals(1, summary.shiftCount)
        assertEquals(10_000_000L, summary.basePayMicros)
        assertEquals(5_000_000L, summary.bonusMicros)
        assertEquals(1_000_000L, summary.penaltyMicros)
        assertEquals(14_000_000L, summary.totalPayMicros)
    }

    companion object {
        @JvmStatic
        fun goldenCases() = listOf(
            Arguments.of(8 * 60, 10_000_000L, 0L, 0L, 80_000_000L),
            Arguments.of(8 * 60 + 30, 10_000_000L, 0L, 0L, 85_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 20_000_000L, 0L, 100_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 0L, 15_000_000L, 65_000_000L),
            Arguments.of(8 * 60, 10_000_000L, 20_000_000L, 15_000_000L, 85_000_000L),
            Arguments.of(0, 10_000_000L, 50_000_000L, 0L, 50_000_000L),
            Arguments.of(8 * 60, 10_000L, 0L, 0L, 80_000L),
            Arguments.of(1, 60_000_000L, 0L, 0L, 1_000_000L),
            Arguments.of(30, 30_000L, 0L, 0L, 20_000L),
        )
    }
}
