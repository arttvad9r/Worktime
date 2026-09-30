package com.worktime.app.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.theme.semanticColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val AmountHiddenFontScale = 1.3f

@Composable
internal fun CalendarGrid(
    month: CalendarMonthUi,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val weekdays = remember(locale) {
        (0 until CalendarDaysInWeek).map { DayOfWeek.MONDAY.plus(it.toLong()).getDisplayName(TextStyle.SHORT, locale) }
    }
    val labels = DayCellLabels(
        today = stringResource(R.string.today),
        selected = stringResource(R.string.day_selected),
        entry = stringResource(R.string.has_entry),
        bonus = stringResource(R.string.has_bonus),
        penalty = stringResource(R.string.has_penalty),
        fillToday = stringResource(R.string.fill_today),
    )
    val showAmount = LocalDensity.current.fontScale < AmountHiddenFontScale

    Column(modifier = modifier.testTag("calendar-grid")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            weekdays.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
            }
        }
        month.weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                week.forEach { cell ->
                    DayCell(
                        cell = cell,
                        isSelected = cell.date == selectedDate,
                        showAmount = showAmount,
                        labels = labels,
                        onClick = onDayClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

private class DayCellLabels(
    val today: String,
    val selected: String,
    val entry: String,
    val bonus: String,
    val penalty: String,
    val fillToday: String,
)

@Composable
private fun DayCell(
    cell: DayCellUi,
    isSelected: Boolean,
    showAmount: Boolean,
    labels: DayCellLabels,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val semantic = MaterialTheme.semanticColors
    val description = buildDayCellDescription(
        dateLabel = cell.dateLabel,
        todayLabel = if (cell.isToday) labels.today else null,
        selectedLabel = if (isSelected) labels.selected else null,
        entryLabel = if (cell.hasEntry) labels.entry else null,
        durationText = cell.durationText,
        amountText = cell.amountText,
        bonusText = if (cell.hasBonus) labels.bonus else null,
        penaltyText = if (cell.hasPenalty) labels.penalty else null,
    )
    val showTodayHint = cell.isToday && !cell.hasEntry
    val background = when {
        isSelected -> colors.primaryContainer
        cell.hasEntry -> colors.surfaceContainerHigh
        else -> colors.background
    }
    val animatedBackground by animateColorAsState(
        targetValue = background,
        animationSpec = tween(
            durationMillis = AppMotion.FastMillis,
            easing = AppMotion.StandardEasing,
        ),
        label = "calendar-day-background",
    )

    Box(modifier = modifier.padding(2.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(animatedBackground)
                .then(
                    if (cell.isToday && cell.inMonth) {
                        Modifier.border(1.5.dp, colors.primary, MaterialTheme.shapes.small)
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = if (showTodayHint) "$description, ${labels.fillToday}" else description
                }
                .clickable(enabled = cell.inMonth) { onClick(cell.date) },
        ) {
            Text(
                text = cell.dayText,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !cell.inMonth -> colors.onSurfaceVariant.copy(alpha = 0.3f)
                    cell.isToday -> colors.primary
                    else -> colors.onSurfaceVariant
                },
                maxLines = 1,
            )
            if (cell.inMonth && (cell.hasBonus || cell.hasPenalty)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 6.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (cell.hasBonus) MarkerDot(semantic.positive)
                    if (cell.hasPenalty) MarkerDot(semantic.negative)
                }
            }
            if (cell.inMonth && (cell.durationText != null || cell.amountText != null)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (cell.durationText != null) {
                        Text(
                            text = cell.durationText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (showAmount && cell.amountText != null) {
                        Text(
                            text = cell.amountText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cell.isNegative) colors.error else colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (showTodayHint && cell.inMonth) {
                Text(
                    text = "+",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primary,
                )
            }
        }
    }
}

@Composable
private fun MarkerDot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color, CircleShape),
    )
}
