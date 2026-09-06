package com.worktime.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.domain.model.MonthSummary
import com.worktime.app.ui.yearsummary.YearSummary
import com.worktime.app.ui.yearsummary.YearSummaryScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YearSummaryPickerUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingDisplayedYearOpensPickerAndRequestsSelectedYear() {
        var requestedYear: Int? = null

        composeRule.setContent {
            YearSummaryScreen(
                selectedYear = 2026,
                summaries = mapOf(2026 to emptySummary(2026)),
                onDismiss = {},
                onSelectYear = { requestedYear = it },
            )
        }

        composeRule.onNodeWithTag("year-summary-year")
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("2027")
            .assertIsDisplayed()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(2027, requestedYear)
        }
    }

    private fun emptySummary(year: Int): YearSummary = YearSummary(
        year = year,
        total = MonthSummary(0, 0, 0L, 0L, 0L, 0L),
        months = List(12) { MonthSummary(0, 0, 0L, 0L, 0L, 0L) },
        monthHasData = List(12) { false },
    )
}
