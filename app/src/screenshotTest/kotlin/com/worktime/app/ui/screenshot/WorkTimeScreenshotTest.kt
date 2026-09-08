package com.worktime.app.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.worktime.app.R
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.MonthlySummaryPanel
import com.worktime.app.ui.components.PlainDragHandle
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import com.worktime.app.ui.yearsummary.YearSummary
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import java.time.LocalDate
import java.time.YearMonth

private val ScreenshotMonth = YearMonth.of(2025, 2)

@PreviewTest
@Preview(name = "Calendar populated light", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun CalendarPopulatedLightScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        CalendarScreen(
            state = populatedCalendarState(),
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
@Preview(name = "Calendar populated dark", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun CalendarPopulatedDarkScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.DARK) {
        CalendarScreen(
            state = populatedCalendarState(),
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
@Preview(name = "Monthly summary expanded", widthDp = 360, heightDp = 420, locale = "ru")
@Composable
fun MonthlySummaryExpandedScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                PlainDragHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {},
                    accessibilityLabel = "Месячная сводка",
                )
                MonthlySummaryPanel(
                    state = populatedCalendarState(),
                    onOpenYearSummary = {},
                    locale = LocalLocale.current.platformLocale,
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Year summary populated", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun YearSummaryPopulatedScreenshot() {
    val summary = populatedYearSummary()
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        YearSummaryScreen(
            selectedYear = summary.year,
            summaries = mapOf(summary.year to summary),
            onDismiss = {},
            onSelectYear = {},
        )
    }
}

@PreviewTest
@Preview(name = "Year summary empty dark", widthDp = 360, heightDp = 800, locale = "en")
@Composable
fun YearSummaryEmptyDarkScreenshot() {
    val summary = emptyYearSummary(2024)
    WorkTimeTheme(themeMode = ThemeMode.DARK) {
        YearSummaryScreen(
            selectedYear = summary.year,
            summaries = mapOf(summary.year to summary),
            onDismiss = {},
            onSelectYear = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Year summary large font",
    widthDp = 360,
    heightDp = 800,
    locale = "ru",
    fontScale = 1.5f,
)
@Composable
fun YearSummaryLargeFontScreenshot() {
    val summary = populatedYearSummary()
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        YearSummaryScreen(
            selectedYear = summary.year,
            summaries = mapOf(summary.year to summary),
            onDismiss = {},
            onSelectYear = {},
        )
    }
}

@PreviewTest
@Preview(name = "Settings light", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun SettingsLightScreenshot() {
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

private fun populatedCalendarState(): CalendarUiState {
    val entries = listOf(
        WorkEntry(
            date = LocalDate.of(2025, 2, 3),
            workedMinutes = 8 * 60,
            hourlyRateMicros = 450_000_000L,
        ),
        WorkEntry(
            date = LocalDate.of(2025, 2, 4),
            workedMinutes = 7 * 60 + 30,
            hourlyRateMicros = 450_000_000L,
            bonusMicros = 1_000_000_000L,
        ),
        WorkEntry(
            date = LocalDate.of(2025, 2, 10),
            workedMinutes = 9 * 60,
            hourlyRateMicros = 500_000_000L,
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
        WorkEntry(
            date = LocalDate.of(2025, 2, 27),
            workedMinutes = 8 * 60 + 15,
            hourlyRateMicros = 500_000_000L,
        ),
    ).associateBy(WorkEntry::date)

    return CalendarUiState(
        visibleMonth = ScreenshotMonth,
        entries = entries,
        monthEntries = mapOf(ScreenshotMonth to entries),
        selectedDate = LocalDate.of(2025, 2, 14),
        isReady = true,
    )
}

private fun populatedYearSummary(): YearSummary {
    val months = listOf(
        monthSummary(7_800, 16, 58_500L, bonusRubles = 2_000L),
        monthSummary(6_900, 15, 52_000L, penaltyRubles = 500L),
        monthSummary(8_400, 18, 64_250L, bonusRubles = 1_500L),
        MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        monthSummary(7_200, 15, 55_300L),
        MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        monthSummary(8_100, 17, 62_400L, penaltyRubles = 750L),
        monthSummary(7_650, 16, 59_900L, bonusRubles = 900L),
        MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        monthSummary(8_250, 17, 63_500L),
        monthSummary(7_350, 15, 56_800L, bonusRubles = 1_200L),
        MonthSummary(0, 0, 0L, 0L, 0L, 0L),
    )
    return YearSummary(
        year = 2025,
        total = MonthSummary(
            workedMinutes = months.sumOf { it.workedMinutes },
            shiftCount = months.sumOf { it.shiftCount },
            basePayMicros = months.sumOf { it.basePayMicros },
            bonusMicros = months.sumOf { it.bonusMicros },
            penaltyMicros = months.sumOf { it.penaltyMicros },
            totalPayMicros = months.sumOf { it.totalPayMicros },
        ),
        months = months,
        monthHasData = months.map { month ->
            month.shiftCount > 0 || month.bonusMicros > 0L || month.penaltyMicros > 0L
        },
    )
}

private fun emptyYearSummary(year: Int): YearSummary = YearSummary(
    year = year,
    total = MonthSummary(0, 0, 0L, 0L, 0L, 0L),
    months = List(12) { MonthSummary(0, 0, 0L, 0L, 0L, 0L) },
    monthHasData = List(12) { false },
)

private fun monthSummary(
    workedMinutes: Int,
    shifts: Int,
    baseRubles: Long,
    bonusRubles: Long = 0L,
    penaltyRubles: Long = 0L,
): MonthSummary {
    val base = baseRubles * 1_000_000L
    val bonus = bonusRubles * 1_000_000L
    val penalty = penaltyRubles * 1_000_000L
    return MonthSummary(
        workedMinutes = workedMinutes,
        shiftCount = shifts,
        basePayMicros = base,
        bonusMicros = bonus,
        penaltyMicros = penalty,
        totalPayMicros = base + bonus - penalty,
    )
}
