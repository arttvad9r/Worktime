package com.worktime.app.modern.model

import java.time.LocalDate
import java.time.YearMonth

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object MoneyRules {
    const val MAX_MINOR: Long = 100_000_000_000L

    fun isValid(value: Long): Boolean = value in 0..MAX_MINOR
}

data class AppSettings(
    val defaultRateMinor: Long = 0L,
    val currencyCode: String = "RUB",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

data class WorkDay(
    val date: LocalDate,
    val workedMinutes: Int,
    val hourlyRateMinor: Long,
    val bonusMinor: Long = 0L,
    val penaltyMinor: Long = 0L,
    val note: String = "",
)

data class DayPay(
    val baseMinor: Long,
    val totalMinor: Long,
)

data class PeriodSummary(
    val workedMinutes: Int = 0,
    val shiftCount: Int = 0,
    val baseMinor: Long = 0L,
    val bonusMinor: Long = 0L,
    val penaltyMinor: Long = 0L,
    val totalMinor: Long = 0L,
)

data class MonthTotal(
    val month: YearMonth,
    val summary: PeriodSummary,
)

object WorkTimeMath {
    fun payForDay(day: WorkDay): DayPay {
        require(day.workedMinutes in 0..24 * 60)
        require(MoneyRules.isValid(day.hourlyRateMinor))
        require(MoneyRules.isValid(day.bonusMinor))
        require(MoneyRules.isValid(day.penaltyMinor))
        val base = divideRoundedHalfUp(
            Math.multiplyExact(day.hourlyRateMinor, day.workedMinutes.toLong()),
            60L,
        )
        return DayPay(
            baseMinor = base,
            totalMinor = Math.subtractExact(Math.addExact(base, day.bonusMinor), day.penaltyMinor),
        )
    }

    fun summarize(days: Collection<WorkDay>): PeriodSummary {
        var workedMinutes = 0
        var shifts = 0
        var base = 0L
        var bonus = 0L
        var penalty = 0L
        var total = 0L
        days.forEach { day ->
            val pay = payForDay(day)
            workedMinutes = Math.addExact(workedMinutes, day.workedMinutes)
            if (day.workedMinutes > 0) shifts++
            base = Math.addExact(base, pay.baseMinor)
            bonus = Math.addExact(bonus, day.bonusMinor)
            penalty = Math.addExact(penalty, day.penaltyMinor)
            total = Math.addExact(total, pay.totalMinor)
        }
        return PeriodSummary(workedMinutes, shifts, base, bonus, penalty, total)
    }

    fun sixWeekGrid(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1)
        val mondayOffset = first.dayOfWeek.value - 1L
        val start = first.minusDays(mondayOffset)
        return List(42) { index -> start.plusDays(index.toLong()) }
    }

    fun monthTotals(days: Collection<WorkDay>, year: Int): List<MonthTotal> =
        (1..12).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            MonthTotal(
                month = month,
                summary = summarize(days.filter { YearMonth.from(it.date) == month }),
            )
        }

    private fun divideRoundedHalfUp(numerator: Long, denominator: Long): Long {
        require(denominator > 0L)
        val quotient = numerator / denominator
        val remainder = numerator % denominator
        if (remainder == 0L) return quotient
        return if (kotlin.math.abs(remainder) * 2 >= denominator) {
            quotient + if (numerator >= 0L) 1 else -1
        } else {
            quotient
        }
    }
}
