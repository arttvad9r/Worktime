package com.worktime.app.ui.yearsummary

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.theme.semanticColors
import java.time.Month
import java.util.Locale
import kotlin.math.abs
import java.time.format.TextStyle as JavaTextStyle

private val MonthLabelWidth = 44.dp
private val AmountColumnWidth = 96.dp
private val BarHeight = 8.dp

@Composable
fun YearSummaryScreen(
    selectedYear: Int,
    summaries: Map<Int, YearSummary>,
    onDismiss: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AppTopBar(
                title = stringResource(R.string.year_summary),
                onBack = onDismiss,
            )
            YearSwitcher(
                year = selectedYear,
                onPrevious = { onSelectYear(selectedYear - 1) },
                onNext = { onSelectYear(selectedYear + 1) },
            )
            Crossfade(
                targetState = summaries[selectedYear],
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(AppMotion.StandardMillis),
                label = "year-summary",
            ) { summary ->
                if (summary != null) {
                    YearSummaryContent(summary = summary, locale = locale)
                }
            }
        }
    }
}

@Composable
private fun YearSwitcher(
    year: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimens.rowMinHeight),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous_year),
            )
        }
        Text(
            text = year.toString(),
            modifier = Modifier
                .width(96.dp)
                .testTag("year-summary-year"),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_year),
            )
        }
    }
}

@Composable
private fun YearSummaryContent(
    summary: YearSummary,
    locale: Locale,
) {
    val colors = MaterialTheme.colorScheme
    val semantic = MaterialTheme.semanticColors
    val total = summary.total
    val maxAbs = summary.months.maxOfOrNull { abs(it.totalPayMicros) } ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenHorizontalPadding)
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
            .testTag("year-summary-content"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(total.totalPayMicros, locale),
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (total.totalPayMicros < 0L) colors.error else colors.onSurface,
                maxLines = 1,
            )
            Text(
                text = "${pluralStringResource(R.plurals.shifts_short, total.shiftCount, total.shiftCount)} · " +
                    stringResource(R.string.hours_short, formatDurationCompact(total.workedMinutes)),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (summary.monthsWithData > 1) {
                Text(
                    text = stringResource(
                        R.string.year_average_month,
                        formatAmountMicros(total.totalPayMicros / summary.monthsWithData, locale),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (total.bonusMicros > 0L || total.penaltyMicros > 0L) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (total.bonusMicros > 0L) {
                        Text(
                            text = "+${formatAmountMicros(total.bonusMicros, locale)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = semantic.positive,
                        )
                    }
                    if (total.penaltyMicros > 0L) {
                        Text(
                            text = "−${formatAmountMicros(total.penaltyMicros, locale)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = semantic.negative,
                        )
                    }
                }
            }
        }

        val emptyMonths = Month.entries.filterIndexed { index, _ ->
            !summary.monthHasData.getOrElse(index) { false }
        }
        Column(modifier = Modifier.testTag("year-summary-months")) {
            Month.entries.forEachIndexed { index, month ->
                if (!summary.monthHasData.getOrElse(index) { false }) return@forEachIndexed
                val monthTotal = summary.months[index]
                MonthBarRow(
                    modifier = Modifier.testTag("year-summary-month-${month.value}"),
                    label = monthLabel(month, locale),
                    fraction = if (maxAbs == 0L) 0f else abs(monthTotal.totalPayMicros).toFloat() / maxAbs,
                    amount = formatAmountMicros(monthTotal.totalPayMicros, locale),
                    barColor = if (monthTotal.totalPayMicros < 0L) colors.error else colors.primary,
                )
            }
        }
        if (summary.monthsWithData == 0) {
            Text(
                text = stringResource(R.string.year_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else if (emptyMonths.isNotEmpty()) {
            Text(
                text = stringResource(
                    R.string.year_empty_months,
                    emptyMonths.joinToString(", ") { monthLabel(it, locale) },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthBarRow(
    label: String,
    fraction: Float,
    amount: String,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(MonthLabelWidth),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(BarHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(BarHeight)
                    .background(barColor, RoundedCornerShape(4.dp)),
            )
        }
        Text(
            text = amount,
            modifier = Modifier.width(AmountColumnWidth),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun monthLabel(month: Month, locale: Locale): String =
    month.getDisplayName(JavaTextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }
