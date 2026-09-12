package com.worktime.app.modern.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.R
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.model.PeriodSummary
import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

@Composable
fun CalendarScreen(
    viewModel: ModernViewModel,
    onOpenMonthReport: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val days by viewModel.monthDays.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val summary = remember(days) { WorkTimeMath.summarize(days) }
    val entries = remember(days) { days.associateBy(WorkDay::date) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
    ) {
        MonthHeader(
            month = month,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onToday = viewModel::currentMonth,
            onSettings = onOpenSettings,
        )
        WeekdayHeader()
        AnimatedContent(
            targetState = month,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "month_grid",
        ) { shownMonth ->
            SixWeekCalendar(
                month = shownMonth,
                entries = entries,
                selectedDate = editor?.date,
                currencyCode = settings.currencyCode,
                onDateClick = viewModel::openDay,
            )
        }
        MonthSummaryPanel(
            summary = summary,
            currencyCode = settings.currencyCode,
            onOpenReport = onOpenMonthReport,
        )
    }

    editor?.let { state ->
        DayEditorSheet(
            state = state,
            currencyCode = settings.currencyCode,
            onDismiss = viewModel::closeEditor,
            onSave = viewModel::saveDay,
            onDelete = viewModel::deleteCurrentDay,
        )
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_month),
            )
        }
        Text(
            text = monthTitle(month),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_month),
            )
        }
        IconButton(onClick = onToday) {
            Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.current_month))
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val locale = LocalConfiguration.current.locales[0]
    val weekdayLabels = remember(locale) {
        DayOfWeek.values().map { day -> day.getDisplayName(TextStyle.SHORT_STANDALONE, locale) }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        weekdayLabels.forEachIndexed { index, label ->
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (index >= 5) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SixWeekCalendar(
    month: YearMonth,
    entries: Map<LocalDate, WorkDay>,
    selectedDate: LocalDate?,
    currencyCode: String,
    onDateClick: (LocalDate) -> Unit,
) {
    val dates = remember(month) { WorkTimeMath.sixWeekGrid(month) }
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(6) { rowIndex ->
            Row(modifier = Modifier.weight(1f)) {
                repeat(7) { columnIndex ->
                    val date = dates[rowIndex * 7 + columnIndex]
                    val inMonth = YearMonth.from(date) == month
                    DayCell(
                        date = date,
                        inMonth = inMonth,
                        selected = inMonth && selectedDate == date,
                        entry = if (inMonth) entries[date] else null,
                        currencyCode = currencyCode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onDateClick(date) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    entry: WorkDay?,
    currencyCode: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val today = inMonth && date == LocalDate.now()
    val locale = LocalConfiguration.current.locales[0]
    val localizedDate = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    val spokenDate = if (today) {
        stringResource(R.string.modern_today_date, localizedDate)
    } else {
        localizedDate
    }
    val durationText = entry?.let { formatDuration(it.workedMinutes) }
    val earningsText = entry?.let { formatMoney(WorkTimeMath.payForDay(it).totalMinor, currencyCode) }
    val accessibilityDescription = when {
        !inMonth -> spokenDate
        entry == null -> stringResource(R.string.modern_day_a11y_empty, spokenDate)
        else -> stringResource(
            R.string.modern_day_a11y_entry,
            spokenDate,
            durationText.orEmpty(),
            earningsText.orEmpty(),
        )
    }
    val targetContainer = when {
        today -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
        else -> Color.Transparent
    }
    val container by animateColorAsState(targetValue = targetContainer, label = "day_cell_container")
    val content = when {
        !inMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
        today -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier.padding(2.dp),
        shape = RoundedCornerShape(14.dp),
        color = container,
        contentColor = content,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        tonalElevation = if (entry != null) 1.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = inMonth, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                    this.selected = selected
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (entry != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = durationText.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                )
                Text(
                    text = earningsText.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MonthSummaryPanel(
    summary: PeriodSummary,
    currencyCode: String,
    onOpenReport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val shifts = pluralStringResource(R.plurals.shifts_short, summary.shiftCount, summary.shiftCount)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$shifts · ${formatDuration(summary.workedMinutes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMoney(summary.totalMinor, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) R.string.modern_collapse else R.string.modern_expand,
                    ),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    SummaryRow(stringResource(R.string.modern_base_earnings), formatMoney(summary.baseMinor, currencyCode))
                    SummaryRow(stringResource(R.string.modern_bonuses), formatMoney(summary.bonusMinor, currencyCode))
                    SummaryRow(stringResource(R.string.modern_penalties), "−${formatMoney(summary.penaltyMinor, currencyCode)}")
                    SummaryRow(stringResource(R.string.modern_other), formatMoney(summary.otherMinor, currencyCode))
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = onOpenReport,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.modern_month_report)) }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
