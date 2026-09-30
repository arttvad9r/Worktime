package com.worktime.app.ui.calendar

import androidx.compose.runtime.Immutable
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal const val CalendarWeekCount = 6
internal const val CalendarDaysInWeek = 7

@Immutable
internal data class DayCellUi(
    val date: LocalDate,
    val dayText: String,
    val dateLabel: String,
    val inMonth: Boolean,
    val isToday: Boolean,
    val hasEntry: Boolean,
    val durationText: String?,
    val amountText: String?,
    val isNegative: Boolean,
    val hasBonus: Boolean,
    val hasPenalty: Boolean,
)

@Immutable
internal data class CalendarMonthUi(
    val month: YearMonth,
    val weeks: List<List<DayCellUi>>,
)

/** Builds the 6x7 Monday-first grid once per data change so cells only draw ready values. */
internal fun buildCalendarMonthUi(
    month: YearMonth,
    entries: Map<LocalDate, WorkEntry>,
    today: LocalDate,
    locale: Locale,
): CalendarMonthUi {
    val firstDay = month.atDay(1)
    val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val weeks = (0 until CalendarWeekCount).map { week ->
        (0 until CalendarDaysInWeek).map { day ->
            val date = gridStart.plusDays((week * CalendarDaysInWeek + day).toLong())
            val inMonth = YearMonth.from(date) == month
            val entry = entries[date].takeIf { inMonth }
            val totalMicros = entry?.let {
                runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull()
            }
            DayCellUi(
                date = date,
                dayText = date.dayOfMonth.toString(),
                dateLabel = date.format(dateFormatter),
                inMonth = inMonth,
                isToday = date == today,
                hasEntry = entry != null,
                durationText = entry?.workedMinutes
                    ?.takeIf { it > 0 }
                    ?.let(::formatDurationCompact),
                amountText = totalMicros
                    ?.takeIf(::shouldShowDayAmount)
                    ?.let { formatAmountMicros(it, locale) },
                isNegative = (totalMicros ?: 0L) < 0L,
                hasBonus = (entry?.bonusMicros ?: 0L) > 0L,
                hasPenalty = (entry?.penaltyMicros ?: 0L) > 0L,
            )
        }
    }
    return CalendarMonthUi(month = month, weeks = weeks)
}

internal fun shouldShowDayAmount(totalMicros: Long?): Boolean = totalMicros != null && totalMicros != 0L

internal fun buildDayCellDescription(
    dateLabel: String,
    todayLabel: String?,
    selectedLabel: String?,
    entryLabel: String?,
    durationText: String?,
    amountText: String?,
    bonusText: String?,
    penaltyText: String?,
): String = listOfNotNull(
    dateLabel,
    todayLabel,
    selectedLabel,
    entryLabel,
    durationText,
    amountText,
    bonusText,
    penaltyText,
).joinToString(", ")
