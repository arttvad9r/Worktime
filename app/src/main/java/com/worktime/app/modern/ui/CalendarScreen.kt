package com.worktime.app.modern.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.model.PeriodSummary
import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath
import java.time.LocalDate
import java.time.YearMonth

private val weekdayLabels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = monthTitle(month),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
        }
        IconButton(onClick = onToday) {
            Icon(Icons.Default.DateRange, contentDescription = "Текущий месяц")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки")
        }
    }
}

@Composable
private fun WeekdayHeader() {
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
    entry: WorkDay?,
    currencyCode: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val today = inMonth && date == LocalDate.now()
    val container = when {
        today -> MaterialTheme.colorScheme.primaryContainer
        entry != null -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
        else -> Color.Transparent
    }
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
        tonalElevation = if (entry != null) 1.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = inMonth, onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (today) FontWeight.Bold else FontWeight.Medium,
            )
            if (entry != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatMinutes(entry.workedMinutes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                )
                Text(
                    text = formatMoney(WorkTimeMath.payForDay(entry).totalMinor, currencyCode),
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
                        text = "${summary.shiftCount} смен · ${formatMinutes(summary.workedMinutes)}",
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
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(10.dp))
                    SummaryRow("По сменам", formatMoney(summary.baseMinor, currencyCode))
                    SummaryRow("Премии", formatMoney(summary.bonusMinor, currencyCode))
                    SummaryRow("Штрафы", "−${formatMoney(summary.penaltyMinor, currencyCode)}")
                    SummaryRow("Прочее", formatMoney(summary.otherMinor, currencyCode))
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = onOpenReport,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Месячный отчёт") }
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
