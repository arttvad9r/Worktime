package com.worktime.app.ui.yearsummary

import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.format.formatAmountMicros
import com.worktime.app.ui.format.formatDurationCompact
import java.time.Month
import java.time.format.TextStyle as JavaTextStyle

private const val MonthLabelWeight = 1.2f
private const val MonthDetailWeight = 1.2f
private const val MonthAmountWeight = 0.9f
private val ShortViewportMonthRowMinHeight = 34.dp
private val YearSummarySectionGap = 10.dp
private val YearSummaryColumnGap = 8.dp
private const val YearPickerPageSize = 12

internal enum class YearSummaryLayoutMode {
    FixedViewport,
    CompactShort,
}

internal fun yearSummaryLayoutMode(isHeightAtLeastMedium: Boolean): YearSummaryLayoutMode =
    if (isHeightAtLeastMedium) {
        YearSummaryLayoutMode.FixedViewport
    } else {
        YearSummaryLayoutMode.CompactShort
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun YearSummaryScreen(
    selectedYear: Int,
    summaries: Map<Int, YearSummary>,
    onDismiss: () -> Unit,
    onSelectYear: (Int) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    val adaptiveInfo = currentWindowAdaptiveInfoV2()
    val layoutMode = yearSummaryLayoutMode(
        isHeightAtLeastMedium = adaptiveInfo.windowSizeClass.isHeightAtLeastBreakpoint(
            HEIGHT_DP_MEDIUM_LOWER_BOUND,
        ),
    )
    val pager = rememberYearSummaryPagerState(selectedYear)
    var yearPickerOpen by rememberSaveable { mutableStateOf(false) }
    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pager.pagerState,
        snapAnimationSpec = spring(
            dampingRatio = AppMotion.NoBounceDampingRatio,
            stiffness = YearSummaryPagerStiffness,
        ),
        snapPositionalThreshold = 0.25f,
    )
    YearSummaryPagerEffects(
        pager = pager,
        selectedYear = selectedYear,
        scope = scope,
        onSelectYear = onSelectYear,
    )

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
                modifier = Modifier.height(56.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = AppDimens.screenHorizontalPadding, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.testTag("year-summary-year-selector"),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 0.dp,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f),
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { pager.navigatePrevious(scope) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_year),
                            )
                        }
                        Text(
                            text = pager.displayedYear.toString(),
                            modifier = Modifier
                                .clickable(
                                    onClick = { yearPickerOpen = true },
                                    onClickLabel = stringResource(R.string.select_year),
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("year-summary-year"),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(
                            onClick = { pager.navigateNext(scope) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_year),
                            )
                        }
                    }
                }
            }

            HorizontalPager(
                state = pager.pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("year-summary-pager"),
                beyondViewportPageCount = 1,
                flingBehavior = pagerFlingBehavior,
                key = { page -> pager.yearForPage(page) },
            ) { page ->
                val pageYear = pager.yearForPage(page)
                val summary = summaries[pageYear]
                if (summary == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    YearSummaryContent(
                        summary = summary,
                        locale = locale,
                        layoutMode = layoutMode,
                    )
                }
            }
        }
    }

    if (yearPickerOpen) {
        YearPickerDialog(
            selectedYear = pager.displayedYear,
            onSelect = { year ->
                yearPickerOpen = false
                onSelectYear(year)
            },
            onDismiss = { yearPickerOpen = false },
        )
    }
}

@Composable
private fun YearPickerDialog(
    selectedYear: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var firstYear by rememberSaveable(selectedYear) {
        mutableStateOf(selectedYear - (YearPickerPageSize / 2 - 1))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.select_year),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { firstYear -= YearPickerPageSize },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.previous_year),
                        )
                    }
                    Text(
                        text = "$firstYear–${firstYear + YearPickerPageSize - 1}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = { firstYear += YearPickerPageSize },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.next_year),
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (firstYear until firstYear + YearPickerPageSize)
                    .toList()
                    .chunked(3)
                    .forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { year ->
                                val selected = year == selectedYear
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelect(year) },
                                    label = {
                                        Text(
                                            text = year.toString(),
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center,
                                            fontWeight = if (selected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Medium
                                            },
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                )
                            }
                        }
                    }
            }
        },
        confirmButton = {},
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun YearSummaryContent(
    summary: YearSummary,
    locale: java.util.Locale,
    layoutMode: YearSummaryLayoutMode,
) {
    val compactText = LocalDensity.current.fontScale >= 1.4f
    val scrollState = rememberScrollState()
    val metricStyle = if (compactText) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val monthStyle = if (compactText) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val scrollModifier = if (layoutMode == YearSummaryLayoutMode.CompactShort) {
        Modifier.verticalScroll(scrollState)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.screenHorizontalPadding)
            .padding(top = 6.dp, bottom = 8.dp)
            .navigationBarsPadding()
            .then(scrollModifier)
            .testTag("year-summary-content"),
        verticalArrangement = Arrangement.spacedBy(YearSummarySectionGap),
    ) {
        AppSectionSurface {
            YearMetricRow(
                label = stringResource(R.string.year_income),
                value = stringResource(
                    R.string.amount_with_currency,
                    formatAmountMicros(summary.total.totalPayMicros, locale),
                ),
                style = metricStyle,
                valueColor = if (summary.total.totalPayMicros < 0L) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                emphasized = true,
            )
            YearMetricRow(
                label = stringResource(R.string.shift_count_label),
                value = summary.total.shiftCount.toString(),
                style = metricStyle,
            )
            YearMetricRow(
                label = stringResource(R.string.worked_duration),
                value = formatDurationCompact(summary.total.workedMinutes),
                style = metricStyle,
            )
            if (summary.monthsWithData > 0) {
                YearMetricRow(
                    label = stringResource(R.string.average_working_month),
                    value = formatAmountMicros(
                        summary.total.totalPayMicros / summary.monthsWithData,
                        locale,
                    ),
                    style = metricStyle,
                )
                if (summary.total.shiftCount > 0) {
                    YearMetricRow(
                        label = stringResource(R.string.average_shift),
                        value = formatDurationCompact(
                            summary.total.workedMinutes / summary.total.shiftCount,
                        ),
                        style = metricStyle,
                    )
                }
            }
            if (summary.total.bonusMicros > 0L) {
                YearMetricRow(
                    label = stringResource(R.string.year_bonuses),
                    value = "+${formatAmountMicros(summary.total.bonusMicros, locale)}",
                    style = metricStyle,
                )
            }
            if (summary.total.penaltyMicros > 0L) {
                YearMetricRow(
                    label = stringResource(R.string.calculation_penalty),
                    value = "−${formatAmountMicros(summary.total.penaltyMicros, locale)}",
                    style = metricStyle,
                )
            }
        }

        AppSectionSurface(
            modifier = if (layoutMode == YearSummaryLayoutMode.FixedViewport) {
                Modifier.weight(1f)
            } else {
                Modifier
            },
        ) {
            MonthSectionHeader(compactText = compactText)
            AppRowDivider(modifier = Modifier.padding(vertical = 2.dp))
            Column(
                modifier = if (layoutMode == YearSummaryLayoutMode.FixedViewport) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("year-summary-months")
                } else {
                    Modifier
                        .fillMaxWidth()
                        .testTag("year-summary-months")
                },
            ) {
                Month.entries.forEachIndexed { index, month ->
                    val monthTotal = summary.months[month.value - 1]
                    val empty = !summary.monthHasData.getOrElse(index) { false }
                    MonthLine(
                        modifier = if (layoutMode == YearSummaryLayoutMode.FixedViewport) {
                            Modifier
                                .weight(1f)
                                .testTag("year-summary-month-${month.value}")
                        } else {
                            Modifier
                                .heightIn(min = ShortViewportMonthRowMinHeight)
                                .testTag("year-summary-month-${month.value}")
                        },
                        label = monthDisplayName(month, locale),
                        detail = if (empty) {
                            null
                        } else {
                            "${monthTotal.shiftCount} · ${formatDurationCompact(monthTotal.workedMinutes)}"
                        },
                        amount = formatAmountMicros(monthTotal.totalPayMicros, locale),
                        dimmed = empty,
                        style = monthStyle,
                    )
                }
            }
        }

        if (layoutMode == YearSummaryLayoutMode.CompactShort) {
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun YearMetricRow(
    label: String,
    value: String,
    style: TextStyle,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false,
) {
    val largeFont = LocalDensity.current.fontScale >= 1.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeFont) 34.dp else 30.dp),
        horizontalArrangement = Arrangement.spacedBy(YearSummaryColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (largeFont) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = style,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MonthSectionHeader(compactText: Boolean) {
    val primaryHeaderStyle = if (compactText) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 30.dp),
        horizontalArrangement = Arrangement.spacedBy(YearSummaryColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.by_month),
            modifier = Modifier.weight(MonthLabelWeight),
            style = primaryHeaderStyle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.year_month_detail_header),
            modifier = Modifier.weight(MonthDetailWeight),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.year_month_income_header),
            modifier = Modifier.weight(MonthAmountWeight),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.74f),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthLine(
    modifier: Modifier = Modifier,
    label: String,
    detail: String?,
    amount: String,
    dimmed: Boolean,
    style: TextStyle,
) {
    val alpha = if (dimmed) 0.44f else 1f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(YearSummaryColumnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.alpha(alpha).weight(MonthLabelWeight),
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail ?: "—",
            modifier = Modifier.weight(MonthDetailWeight).alpha(alpha),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = if (detail == null) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (dimmed) "—" else amount,
            modifier = Modifier.weight(MonthAmountWeight).alpha(alpha),
            style = style,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (dimmed) TextAlign.Center else TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun monthDisplayName(month: Month, locale: java.util.Locale): String =
    month.getDisplayName(JavaTextStyle.SHORT, locale).replaceFirstChar { it.uppercase(locale) }
