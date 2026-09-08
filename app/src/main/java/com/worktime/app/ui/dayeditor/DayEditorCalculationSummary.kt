package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatDurationCompact

@Composable
internal fun CalculationSummary(
    draft: WorkEntry?,
    totalMicros: Long?,
) {
    if (draft == null || totalMicros == null) return
    val locale = LocalLocale.current.platformLocale
    val entryPay = SalaryCalculator.entryPay(draft)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalculationRow(
                label = stringResource(
                    R.string.calc_expression,
                    formatDurationCompact(draft.workedMinutes),
                    formatDecimalMicros(draft.hourlyRateMicros),
                ),
                value = stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(entryPay.basePayMicros, locale),
                ),
            )
            if (draft.bonusMicros > 0L) {
                CalculationRow(
                    label = stringResource(R.string.calculation_bonus),
                    value = "+" + stringResource(
                        R.string.amount_with_currency,
                        formatAmountMicros(draft.bonusMicros, locale),
                    ),
                )
            }
            if (draft.penaltyMicros > 0L) {
                CalculationRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−" + stringResource(
                        R.string.amount_with_currency,
                        formatAmountMicros(draft.penaltyMicros, locale),
                    ),
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
            )
            CalculationRow(
                label = stringResource(R.string.calculation_total),
                value = stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(totalMicros, locale),
                ),
                emphasized = true,
                valueColor = if (totalMicros < 0L) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun CalculationRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val textStyle = if (emphasized) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val fontWeight = if (emphasized) FontWeight.SemiBold else null
    val labelColor = if (emphasized) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = textStyle,
            fontWeight = fontWeight,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = textStyle,
            fontWeight = fontWeight,
            color = valueColor,
            maxLines = 1,
        )
    }
}
