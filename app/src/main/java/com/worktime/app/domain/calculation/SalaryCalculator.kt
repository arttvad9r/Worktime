package com.worktime.app.domain.calculation

import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry

object SalaryCalculator {
    const val MICROS_PER_CENT: Long = 10_000L

    data class EntryPay(
        val basePayMicros: Long,
        val totalPayMicros: Long,
    )

    fun entryPay(entry: WorkEntry): EntryPay {
        val basePay = divideRoundedHalfUp(
            numerator = Math.multiplyExact(entry.hourlyRateMicros, entry.workedMinutes.toLong()),
            denominator = 60L * MICROS_PER_CENT,
        ) * MICROS_PER_CENT
        return EntryPay(
            basePayMicros = basePay,
            totalPayMicros = Math.subtractExact(
                Math.addExact(basePay, roundToCents(entry.bonusMicros)),
                roundToCents(entry.penaltyMicros),
            ),
        )
    }

    fun monthSummary(entries: Collection<WorkEntry>): MonthSummary {
        var workedMinutes = 0
        var shiftCount = 0
        var basePay = 0L
        var bonuses = 0L
        var penalties = 0L
        var totalPay = 0L

        entries.forEach { entry ->
            val pay = entryPay(entry)
            workedMinutes = Math.addExact(workedMinutes, entry.workedMinutes)
            if (entry.workedMinutes > 0) shiftCount++
            basePay = Math.addExact(basePay, pay.basePayMicros)
            bonuses = Math.addExact(bonuses, roundToCents(entry.bonusMicros))
            penalties = Math.addExact(penalties, roundToCents(entry.penaltyMicros))
            totalPay = Math.addExact(totalPay, pay.totalPayMicros)
        }

        return MonthSummary(
            workedMinutes = workedMinutes,
            shiftCount = shiftCount,
            basePayMicros = basePay,
            bonusMicros = bonuses,
            penaltyMicros = penalties,
            totalPayMicros = totalPay,
        )
    }

    fun roundToCents(micros: Long): Long =
        divideRoundedHalfUp(micros, MICROS_PER_CENT) * MICROS_PER_CENT

    private fun divideRoundedHalfUp(numerator: Long, denominator: Long): Long {
        require(denominator > 0)
        val quotient = numerator / denominator
        val remainder = numerator % denominator
        if (remainder == 0L) return quotient

        val shouldRoundAwayFromZero = kotlin.math.abs(remainder) * 2 >= denominator
        if (!shouldRoundAwayFromZero) return quotient
        return quotient + if (numerator >= 0) 1 else -1
    }
}
