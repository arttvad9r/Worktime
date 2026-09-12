package com.worktime.app.modern.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.model.MonthTotal
import com.worktime.app.modern.model.PeriodSummary
import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath
import java.time.YearMonth

@Composable
fun MonthReportScreen(
    viewModel: ModernViewModel,
    onBack: () -> Unit,
    onOpenYear: () -> Unit,
) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val days by viewModel.monthDays.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summary = remember(days) { WorkTimeMath.summarize(days) }

    Column(modifier = Modifier.fillMaxSize()) {
        ReportTopBar(title = monthTitle(month), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(summary, settings.currencyCode) }
            item {
                Button(onClick = onOpenYear, modifier = Modifier.fillMaxWidth()) {
                    Text("Годовой отчёт")
                }
            }
            if (days.isNotEmpty()) {
                item {
                    Text("Смены", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(days.sortedByDescending(WorkDay::date), key = { it.date.toEpochDay() }) { day ->
                    DayReportRow(day = day, currencyCode = settings.currencyCode)
                }
            }
        }
    }
}

@Composable
fun YearReportScreen(
    viewModel: ModernViewModel,
    onBack: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
) {
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    val days by viewModel.yearDays.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val summary = remember(days) { WorkTimeMath.summarize(days) }
    val months = remember(days, year) { WorkTimeMath.monthTotals(days, year) }
    val maxIncome = months.maxOfOrNull { it.summary.totalMinor.coerceAtLeast(0L) }?.coerceAtLeast(1L) ?: 1L

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            IconButton(onClick = viewModel::previousYear) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий год")
            }
            Text(
                text = year.toString(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = viewModel::nextYear) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Следующий год")
            }
            Spacer(Modifier.width(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SummaryCard(summary, settings.currencyCode) }
            item {
                Text(
                    "Доход по месяцам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(months, key = { it.month.monthValue }) { month ->
                MonthIncomeRow(
                    month = month,
                    currencyCode = settings.currencyCode,
                    maxIncome = maxIncome,
                    onClick = { onOpenMonth(month.month) },
                )
            }
        }
    }
}

@Composable
private fun ReportTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun SummaryCard(summary: PeriodSummary, currencyCode: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                formatMoney(summary.totalMinor, currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${summary.shiftCount} смен · ${formatMinutes(summary.workedMinutes)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            SummaryRow("По сменам", formatMoney(summary.baseMinor, currencyCode))
            SummaryRow("Премии", formatMoney(summary.bonusMinor, currencyCode))
            SummaryRow("Штрафы", "−${formatMoney(summary.penaltyMinor, currencyCode)}")
        }
    }
}

@Composable
private fun DayReportRow(day: WorkDay, currencyCode: String) {
    val pay = remember(day) { WorkTimeMath.payForDay(day) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "%02d.%02d".format(day.date.dayOfMonth, day.date.monthValue),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatMinutes(day.workedMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(formatMoney(pay.totalMinor, currencyCode), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthIncomeRow(
    month: MonthTotal,
    currencyCode: String,
    maxIncome: Long,
    onClick: () -> Unit,
) {
    val fraction = (month.summary.totalMinor.coerceAtLeast(0L).toDouble() / maxIncome.toDouble()).toFloat()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    month.month.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, java.util.Locale.forLanguageTag("ru-RU"))
                        .replaceFirstChar { it.titlecase() },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                Text(formatMoney(month.summary.totalMinor, currencyCode), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
