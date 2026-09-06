package com.worktime.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun CalendarGrid(
    state: CalendarUiState,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val weekdayRowHeight = 28.dp
    val dateAreaHeight = 28.dp
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .testTag("calendar-grid"),
    ) {
        val weekdays = (0 until 7).map { DayOfWeek.MONDAY.plus(it.toLong()) }
        val firstDay = state.visibleMonth.atDay(1)
        val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
        val cells = (0L until 42L).map { offset -> gridStart.plusDays(offset) }
        val today = LocalDate.now()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(weekdayRowHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                weekdays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
            )

            cells.chunked(7).forEachIndexed { weekIndex, week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    week.forEach { date ->
                        val isInVisibleMonth = YearMonth.from(date) == state.visibleMonth
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        ) {
                            DayCell(
                                date = date,
                                entry = state.entries[date],
                                isInVisibleMonth = isInVisibleMonth,
                                isToday = date == today,
                                isSelected = date == state.selectedDate,
                                onClick = { onDayClick(date) },
                                locale = locale,
                                dateAreaHeight = dateAreaHeight,
                                isLastGridRow = weekIndex == CalendarWeekCount - 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val CalendarWeekCount = 6
private val WeekRowHeight = 64.dp

internal fun calendarGridHeight(): Dp =
    WeekRowHeight * CalendarWeekCount + 28.dp + 8.dp

@Composable
private fun DayCell(
    date: LocalDate,
    entry: WorkEntry?,
    isInVisibleMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    locale: Locale,
    dateAreaHeight: Dp,
    isLastGridRow: Boolean,
) {
    val visibleEntry = entry.takeIf { isInVisibleMonth }
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val interactionSource = remember { MutableInteractionSource() }
    val totalMicros = visibleEntry?.let {
        runCatching { SalaryCalculator.entryPay(it).totalPayMicros }.getOrNull()
    }
    val dateLabel = date.format(
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
    )
    val a11yDescription = buildDayCellDescription(
        dateLabel = dateLabel,
        todayLabel = if (isToday) stringResource(R.string.today) else null,
        selectedLabel = if (isSelected) stringResource(R.string.day_selected) else null,
        entryLabel = if (visibleEntry != null) stringResource(R.string.has_entry) else null,
        durationText = if (visibleEntry != null && visibleEntry.workedMinutes > 0) {
            formatDuration(visibleEntry.workedMinutes)
        } else {
            null
        },
        amountText = if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
            formatAmountMicros(totalMicros, locale)
        } else {
            null
        },
        bonusText = if ((visibleEntry?.bonusMicros ?: 0L) > 0L) {
            stringResource(R.string.has_bonus)
        } else {
            null
        },
        penaltyText = if ((visibleEntry?.penaltyMicros ?: 0L) > 0L) {
            stringResource(R.string.has_penalty)
        } else {
            null
        },
    )
    val dateColor = when {
        !isInVisibleMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val amountColor = if ((totalMicros ?: 0L) < 0L) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val borderColor = if (isToday && isInVisibleMonth) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
        visibleEntry != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = if (isToday && isInVisibleMonth) 1.25.dp else 0.5.dp,
                brush = SolidColor(borderColor),
                shape = RectangleShape,
            )
            .background(backgroundColor)
            .semantics(mergeDescendants = true) { contentDescription = a11yDescription }
            .then(
                if (isLastGridRow && isInVisibleMonth) Modifier.testTag("calendar-last-row-day")
                else Modifier,
            )
            .clickable(
                enabled = isInVisibleMonth,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dateAreaHeight)
                .padding(top = 3.dp, end = 4.dp)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.TopEnd,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = dateColor,
                maxLines = 1,
            )
        }

        if (visibleEntry != null) {
            Text(
                text = if (visibleEntry.workedMinutes > 0) {
                    formatDurationCompact(visibleEntry.workedMinutes)
                } else {
                    ""
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 3.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (largeFont) 10.sp else 15.sp,
                    lineHeight = if (largeFont) 11.sp else 18.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (totalMicros != null && shouldShowDayAmount(totalMicros)) {
                    formatWholeAmountMicros(totalMicros, locale)
                } else {
                    ""
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 2.dp, bottom = 3.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (largeFont) 9.sp else 12.sp,
                    lineHeight = if (largeFont) 10.sp else 14.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = amountColor,
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal fun shouldShowDayAmount(totalMicros: Long?): Boolean = totalMicros != null && totalMicros != 0L

internal fun shouldShowTodayEntryPrompt(
    visibleMonth: YearMonth,
    entryDates: Set<LocalDate>,
    today: LocalDate,
): Boolean = visibleMonth == YearMonth.from(today) && today !in entryDates

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

@Composable
private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) {
        stringResource(R.string.duration_hours, hours)
    } else {
        stringResource(R.string.duration_hours_minutes, hours, remainder)
    }
}
