package com.worktime.app.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.worktime.app.R
import com.worktime.app.ui.components.AppDimens
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun TodayEntryPrompt(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.rowMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .height(AppDimens.rowMinHeight)
                .testTag("today-entry-prompt"),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.fill_today),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

internal fun calendarMonthTitle(
    visibleMonth: YearMonth,
    locale: Locale,
    largeFont: Boolean,
): String = visibleMonth.format(
    DateTimeFormatter.ofPattern(if (largeFont) "LLL yyyy" else "LLLL yyyy", locale),
)

@Composable
internal fun CalendarHeader(
    visibleMonth: YearMonth,
    isReady: Boolean,
    locale: Locale,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val largeFont = LocalDensity.current.fontScale >= 1.5f
    val monthTitle = calendarMonthTitle(visibleMonth, locale, largeFont)
    val titleFontSize = if (largeFont) 18.sp else 22.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.previous_month),
                )
            }
            Box(
                modifier = Modifier
                    .height(AppDimens.rowMinHeight)
                    .clickable(
                        onClick = onSelectMonth,
                        onClickLabel = stringResource(R.string.select_month),
                    )
                    .padding(horizontal = 4.dp)
                    .testTag("calendar-month-title"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = monthTitle,
                    modifier = if (largeFont) Modifier.widthIn(max = 140.dp) else Modifier,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.next_month),
                )
            }
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            IconButton(onClick = onSettingsClick, enabled = isReady) {
                Icon(
                    Icons.Filled.Settings,
                    modifier = Modifier.size(22.dp),
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }
    }
}

@Composable
internal fun MonthPickerDialog(
    visibleMonth: YearMonth,
    locale: Locale,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var shownYear by rememberSaveable(visibleMonth.year) { mutableStateOf(visibleMonth.year) }
    val monthLabels = remember(locale) {
        (1..12).map { month ->
            month to Month.of(month).getDisplayName(TextStyle.SHORT, locale)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { shownYear -= 1 },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_year),
                    )
                }
                Text(
                    text = shownYear.toString(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { shownYear += 1 },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_year),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                monthLabels.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (month, label) ->
                            val selected = shownYear == visibleMonth.year &&
                                month == visibleMonth.monthValue
                            FilterChip(
                                selected = selected,
                                onClick = { onSelect(YearMonth.of(shownYear, month)) },
                                label = {
                                    Text(
                                        text = label.replaceFirstChar { it.uppercase(locale) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        maxLines = 1,
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
