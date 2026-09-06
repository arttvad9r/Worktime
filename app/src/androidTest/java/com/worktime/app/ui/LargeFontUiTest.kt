package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.R
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.calendar.CalendarScreen
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.calendarGridHeight
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.yearsummary.YearSummary
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LargeFontUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yearSummaryKeepsEssentialActionsAndMakesMonthsReachableAtLargeFontInNarrowLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
            ) {
                Box(Modifier.size(240.dp, 800.dp)) {
                    YearSummaryScreen(
                        selectedYear = 2026,
                        summaries = mapOf(2026 to summary()),
                        onDismiss = {},
                        onSelectYear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("2026").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.by_month)).assertIsDisplayed()
        composeRule.onNodeWithText(monthLabel(Month.JANUARY, context)).assertIsDisplayed()
        composeRule.onNodeWithText(monthLabel(Month.DECEMBER, context))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.previous_year),
        ).assertIsDisplayed()
    }

    @Test
    fun emptyYearSummaryShowsJanuaryAndDecemberWithoutScrolling() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            Box(Modifier.size(320.dp, 800.dp)) {
                YearSummaryScreen(
                    selectedYear = 2026,
                    summaries = mapOf(2026 to emptySummary()),
                    onDismiss = {},
                    onSelectYear = {},
                )
            }
        }

        composeRule.onNodeWithText(monthLabel(Month.JANUARY, context)).assertIsDisplayed()
        composeRule.onNodeWithText(monthLabel(Month.DECEMBER, context)).assertIsDisplayed()
    }

    @Test
    fun settingsOpensChangeRateFlowAtLargeFontInNarrowLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        var opened = false
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
            ) {
                Box(Modifier.size(240.dp, 800.dp)) {
                    SettingsScreen(
                        defaultHourlyRateMicros = 500_000_000L,
                        themeMode = com.worktime.app.domain.preferences.ThemeMode.SYSTEM,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onThemeChange = {},
                        onRateChange = {},
                        onOpenChangeRate = { opened = true },
                        onExportData = {},
                        onExportCsv = {},
                        onImportData = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.change_rate_for_period))
            .assertIsDisplayed()
            .performClick()
        check(opened)
    }

    @Test
    fun calendarKeepsNavigationAndPopulatedDayAtLargeFontInNarrowLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
            ) {
                Box(Modifier.size(280.dp, 800.dp)) {
                    CalendarScreen(
                        state = CalendarUiState(
                            visibleMonth = YearMonth.of(2026, 8),
                            entries = mapOf(
                                LocalDate.of(2026, 8, 31) to WorkEntry(
                                    date = LocalDate.of(2026, 8, 31),
                                    workedMinutes = 480,
                                    hourlyRateMicros = 10_000_000L,
                                ),
                            ),
                            isReady = true,
                        ),
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onSelectMonth = {},
                        onDayClick = {},
                        onSettingsClick = {},
                        onOpenYearSummary = {},
                    )
                }
            }
        }

        composeRule.onNodeWithContentDescription(context.getString(R.string.previous_month)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.next_month)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.settings)).assertIsDisplayed()
        composeRule.onNodeWithTag("calendar-month-title").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.has_entry), substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.duration_hours, 8), substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("80", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("monthly-summary-strip").performClick()
        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
    }

    @Test
    fun calendarSummaryOpensReportOnUpwardDrag() {
        composeRule.setContent {
            Box(Modifier.size(320.dp, 800.dp)) {
                CalendarScreen(
                    state = CalendarUiState(
                        visibleMonth = YearMonth.of(2026, 1),
                        isReady = true,
                    ),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectMonth = {},
                    onDayClick = {},
                    onSettingsClick = {},
                    onOpenYearSummary = {},
                )
            }
        }

        composeRule.onNodeWithTag("monthly-summary-strip")
            .performTouchInput { swipeUp() }
        composeRule.onNodeWithTag("monthly-report-panel").assertIsDisplayed()
    }

    @Test
    fun calendarPreservesSixWeekGeometryAtLargeFontScale() {
        var deviceDensity = 1f
        composeRule.setContent {
            deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(
                LocalDensity provides Density(deviceDensity, fontScale = 2f),
            ) {
                Box(Modifier.size(320.dp, 800.dp)) {
                    CalendarScreen(
                        state = CalendarUiState(
                            visibleMonth = YearMonth.of(2026, 8),
                            isReady = true,
                        ),
                        onPreviousMonth = {},
                        onNextMonth = {},
                        onSelectMonth = {},
                        onDayClick = {},
                        onSettingsClick = {},
                        onOpenYearSummary = {},
                    )
                }
            }
        }

        composeRule.waitForIdle()
        val largeHeightPx = composeRule.onNodeWithTag("calendar-pager")
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        val largeHeightDp = largeHeightPx / deviceDensity
        val expectedHeightDp = calendarGridHeight().value

        assert(abs(largeHeightDp - expectedHeightDp) <= 1f) {
            "calendar pager changed the fixed six-week geometry: " +
                "expected ${expectedHeightDp}dp, actual ${largeHeightDp}dp"
        }
        composeRule.onNodeWithTag("monthly-summary-strip").assertIsDisplayed()
    }

    private fun summary() = YearSummary(
        year = 2026,
        total = MonthSummary(480, 1, 8_000_000L, 0L, 0L, 8_000_000L),
        months = List(12) { MonthSummary(480, 1, 8_000_000L, 0L, 0L, 8_000_000L) },
        monthHasData = List(12) { true },
    )

    private fun emptySummary() = YearSummary(
        year = 2026,
        total = MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        months = List(12) { MonthSummary(0, 0, 0L, 0L, 0L, 0L) },
        monthHasData = List(12) { false },
    )

    private fun monthLabel(month: Month, context: android.content.Context): String {
        val locale = context.resources.configuration.locales[0]
        return month.getDisplayName(TextStyle.SHORT, locale)
            .replaceFirstChar { it.uppercase(locale) }
    }
}
