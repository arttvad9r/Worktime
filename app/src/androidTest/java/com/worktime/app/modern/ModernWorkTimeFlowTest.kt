package com.worktime.app.modern

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import com.worktime.app.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernWorkTimeFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addWorkDaySurvivesActivityRecreationAndCanBeDeleted() {
        val initialActivity = composeRule.activity
        val locale = initialActivity.resources.configuration.locales[0]
        val localizedToday = LocalDate.now().format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
        )
        val todayMatcher = hasContentDescription(localizedToday, substring = true)

        composeRule.onNode(todayMatcher).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        var fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextReplacement("8")
        fields[1].performTextReplacement("0")
        fields[2].performTextReplacement("100")
        composeRule.onNodeWithText(initialActivity.getString(R.string.modern_save)).performClick()
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNode(todayMatcher).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].assert(hasText("8"))
        fields[1].assert(hasText("0"))
        fields[2].assert(hasText("100"))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.modern_delete)).performClick()
        composeRule.waitForIdle()

        val currentActivity = composeRule.activity
        val spokenToday = currentActivity.getString(R.string.modern_today_date, localizedToday)
        val emptyDescription = currentActivity.getString(R.string.modern_day_a11y_empty, spokenToday)
        composeRule.onNode(hasContentDescription(emptyDescription)).assertIsDisplayed()
    }
}
