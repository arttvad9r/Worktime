package com.worktime.app.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationSummaryParts
import com.worktime.app.ui.theme.semanticColors
import java.util.Locale

@Composable
internal fun MonthlySummaryCard(
    summary: MonthSummary,
    locale: Locale,
    onOpenYearSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = stringResource(
        R.string.amount_with_currency,
        formatAmountMicros(summary.totalPayMicros, locale),
    )
    val shifts = pluralStringResource(R.plurals.shifts_short, summary.shiftCount, summary.shiftCount)
    val duration = formatDurationSummaryParts(summary.workedMinutes, locale)
    val hours = if (duration.minutes == 0) {
        stringResource(R.string.duration_hours, duration.hours)
    } else {
        stringResource(R.string.duration_hours_minutes, duration.hours, duration.minutes)
    }
    val base = stringResource(R.string.calculation_base)
    val bonus = stringResource(R.string.calculation_bonus)
    val penalty = stringResource(R.string.calculation_penalty)
    val baseAmount = stringResource(R.string.amount_with_currency, formatAmountMicros(summary.basePayMicros, locale))
    val bonusAmount = stringResource(R.string.amount_with_currency, formatAmountMicros(summary.bonusMicros, locale))
    val penaltyAmount = stringResource(R.string.amount_with_currency, formatAmountMicros(summary.penaltyMicros, locale))
    val hasAdjustments = summary.bonusMicros > 0L || summary.penaltyMicros > 0L
    val semantic = MaterialTheme.semanticColors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly-report-panel"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    onClickLabel = stringResource(R.string.year_stats_title),
                    onClick = onOpenYearSummary,
                )
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = total,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
                Text(
                    text = "$shifts · $hours",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (hasAdjustments) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "$base $baseAmount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        if (summary.bonusMicros > 0L) {
                            Text(
                                text = "$bonus +$bonusAmount",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.positive,
                                maxLines = 1,
                            )
                        }
                        if (summary.penaltyMicros > 0L) {
                            Text(
                                text = "$penalty −$penaltyAmount",
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.negative,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun shouldUseErrorColorForTotal(totalPayMicros: Long): Boolean = totalPayMicros < 0L
