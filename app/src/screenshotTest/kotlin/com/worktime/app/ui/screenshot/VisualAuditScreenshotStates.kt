package com.worktime.app.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.worktime.app.R
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.MonthPickerDialog
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Extra screenshot states for geometry review. These are not marketing compositions: they exist
 * so visual changes are checked as rendered pixels across interactive screens/states that can
 * expose spacing, clipping or nested-control regressions.
 */

@PreviewTest
@Preview(name = "Focused numeric editor", widthDp = 360, heightDp = 180, locale = "ru")
@Composable
fun FocusedNumericEditorScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AppSectionSurface(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppDimens.rowMinHeight),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.hourly_rate),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    CompactMoneyField(
                        text = "500",
                        onTextChange = {},
                        isError = false,
                        contentDescription = stringResource(R.string.hourly_rate),
                        autoFocus = true,
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "Change rate custom period", widthDp = 360, heightDp = 480, locale = "ru")
@Composable
fun ChangeRateCustomPeriodScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.rate_for_period),
                    style = MaterialTheme.typography.titleLarge,
                )
                AppSegmentedControl(
                    options = listOf(
                        stringResource(R.string.current_month),
                        stringResource(R.string.custom_period),
                    ),
                    selectedIndex = 1,
                    onSelect = {},
                )
                AppSectionSurface {
                    AppNavigationRow(
                        label = stringResource(R.string.start_date),
                        value = "10 фев. 2025",
                        onClick = {},
                    )
                    AppRowDivider()
                    AppNavigationRow(
                        label = stringResource(R.string.end_date),
                        value = "24 фев. 2025",
                        onClick = {},
                    )
                    AppRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AppDimens.rowMinHeight),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.hourly_rate),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        CompactMoneyField(
                            text = "500",
                            onTextChange = {},
                            isError = false,
                            contentDescription = stringResource(R.string.hourly_rate),
                        )
                    }
                }
                AppPrimaryButton(
                    text = stringResource(R.string.change_rate),
                    onClick = {},
                    enabled = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewTest
@Preview(name = "Date picker content", widthDp = 360, heightDp = 520, locale = "ru")
@Composable
fun DatePickerContentScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        val colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            navigationContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            dayContentColor = MaterialTheme.colorScheme.onSurface,
            selectedDayContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedDayContainerColor = MaterialTheme.colorScheme.primaryContainer,
            todayContentColor = MaterialTheme.colorScheme.primary,
            todayDateBorderColor = MaterialTheme.colorScheme.primary,
            dividerColor = MaterialTheme.colorScheme.outlineVariant,
        )
        val state = rememberDatePickerState(initialSelectedDateMillis = 1_739_491_200_000L)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = colors,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewTest
@Preview(name = "Month picker dialog", widthDp = 360, heightDp = 600, locale = "ru")
@Composable
fun MonthPickerDialogScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        MonthPickerDialog(
            visibleMonth = YearMonth.of(2025, 2),
            locale = Locale.forLanguageTag("ru"),
            onSelect = {},
            onDismiss = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Calendar large font",
    widthDp = 360,
    heightDp = 800,
    locale = "ru",
    fontScale = 1.5f,
)
@Composable
fun CalendarLargeFontScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        CalendarScreen(
            state = visualAuditCalendarState(),
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectMonth = {},
            onDayClick = {},
            onSettingsClick = {},
            onOpenYearSummary = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Settings large font",
    widthDp = 360,
    heightDp = 800,
    locale = "ru",
    fontScale = 1.5f,
)
@Composable
fun SettingsLargeFontScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        SettingsScreen(
            defaultHourlyRateMicros = 500_000_000L,
            themeMode = ThemeMode.SYSTEM,
            operationErrorMessage = null,
            onDismiss = {},
            onThemeChange = {},
            onRateChange = {},
            onOpenChangeRate = {},
            onExportData = {},
            onExportCsv = {},
            onImportData = {},
        )
    }
}

private fun visualAuditCalendarState(): CalendarUiState {
    val month = YearMonth.of(2025, 2)
    val entries = listOf(
        WorkEntry(
            date = LocalDate.of(2025, 2, 3),
            workedMinutes = 8 * 60,
            hourlyRateMicros = 450_000_000L,
        ),
        WorkEntry(
            date = LocalDate.of(2025, 2, 14),
            workedMinutes = 8 * 60,
            hourlyRateMicros = 500_000_000L,
            penaltyMicros = 250_000_000L,
        ),
        WorkEntry(
            date = LocalDate.of(2025, 2, 20),
            workedMinutes = 6 * 60 + 45,
            hourlyRateMicros = 500_000_000L,
        ),
    ).associateBy(WorkEntry::date)
    return CalendarUiState(
        visibleMonth = month,
        entries = entries,
        monthEntries = mapOf(month to entries),
        selectedDate = LocalDate.of(2025, 2, 14),
        isReady = true,
    )
}
