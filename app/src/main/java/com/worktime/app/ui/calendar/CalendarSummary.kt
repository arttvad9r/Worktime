package com.worktime.app.ui.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.formatWholeAmountMicros
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun SummaryStrip(
    state: CalendarUiState,
    locale: Locale,
    expanded: Boolean,
    onClick: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val detailText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        locale = locale,
    )
    val amountText = stringResource(
        R.string.amount_with_currency,
        formatWholeAmountMicros(summary.totalPayMicros, locale),
    )
    val summaryText = "$detailText · $amountText"
    val haptics = LocalHapticFeedback.current
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    val stripHeight = if (largeFont) 72.dp else 56.dp
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.StandardMillis,
            easing = AppMotion.StandardEasing,
        ),
        label = "summary chevron",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(stripHeight),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(haptics, onClick, onSwipeUp) {
                    val threshold = 40.dp.toPx()
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var thresholdActive = false
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                        },
                        onDrag = { change, dragAmount ->
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y
                            val isEligible = totalDragY <= -threshold
                            if (isEligible && !thresholdActive) {
                                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                thresholdActive = true
                            } else if (!isEligible) {
                                thresholdActive = false
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            val dragDistanceSquared =
                                totalDragX * totalDragX + totalDragY * totalDragY
                            when {
                                totalDragY <= -threshold -> onSwipeUp()
                                dragDistanceSquared < threshold * threshold -> onClick()
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                        },
                        onDragCancel = {
                            totalDragX = 0f
                            totalDragY = 0f
                            thresholdActive = false
                        },
                    )
                }
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    shape = MaterialTheme.shapes.medium,
                )
                .clickable(
                    onClickLabel = stringResource(R.string.monthly_summary),
                    onClick = onClick,
                )
                .testTag("monthly-summary-strip")
                .padding(horizontal = if (largeFont) 12.dp else AppDimens.screenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (largeFont) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    text = summaryText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Filled.KeyboardArrowUp,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun summaryLine(
    shiftCount: Int,
    workedMinutes: Int,
    locale: Locale,
    totalPayMicros: Long? = null,
): String {
    val shifts = pluralStringResource(R.plurals.shifts_short, shiftCount, shiftCount)
    val hours = stringResource(R.string.hours_short, formatDurationCompact(workedMinutes))
    val line = "$shifts · $hours"
    if (totalPayMicros == null) return line
    val money = stringResource(
        R.string.amount_with_currency,
        formatWholeAmountMicros(totalPayMicros, locale),
    )
    return "$line · $money"
}

@Composable
internal fun MonthlySummaryPanel(
    state: CalendarUiState,
    onOpenYearSummary: () -> Unit,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val summary = state.summary
    val totalText = stringResource(
        R.string.amount_with_currency,
        formatAmountMicros(summary.totalPayMicros, locale),
    )
    val detailText = summaryLine(
        shiftCount = summary.shiftCount,
        workedMinutes = summary.workedMinutes,
        locale = locale,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly-report-panel"),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = AppDimens.screenHorizontalPadding,
                vertical = 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.visibleMonth.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = totalText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (shouldUseErrorColorForTotal(summary.totalPayMicros)) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                maxLines = 1,
            )
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            LabelValueRow(
                label = stringResource(R.string.calculation_base),
                value = formatAmountMicros(summary.basePayMicros, locale),
            )
            if (summary.bonusMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_bonus),
                    value = "+${formatAmountMicros(summary.bonusMicros, locale)}",
                )
            }
            if (summary.penaltyMicros > 0L) {
                LabelValueRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−${formatAmountMicros(summary.penaltyMicros, locale)}",
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f))

            if (summary.shiftCount > 0) {
                LabelValueRow(
                    label = stringResource(R.string.average_shift),
                    value = formatDurationCompact(summary.workedMinutes / summary.shiftCount),
                )
                LabelValueRow(
                    label = stringResource(R.string.average_shift_income),
                    value = formatAmountMicros(summary.totalPayMicros / summary.shiftCount, locale),
                )
            }

            AppNavigationRow(
                label = stringResource(R.string.year_stats_title),
                onClick = onOpenYearSummary,
            )
        }
    }
}

internal fun shouldUseErrorColorForTotal(totalPayMicros: Long): Boolean = totalPayMicros < 0L
