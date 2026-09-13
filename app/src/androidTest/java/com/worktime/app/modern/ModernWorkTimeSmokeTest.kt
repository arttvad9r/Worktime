package com.worktime.app.modern

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class ModernWorkTimeSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun calendarStartsExposesLocalizedSemanticsAndReturnsFromSettingsWithSystemBack() {
        val activity = composeRule.activity
        val settings = activity.getString(R.string.settings)
        val locale = activity.resources.configuration.locales[0]
        val localizedToday = LocalDate.now().format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale),
        )
        val spokenToday = activity.getString(R.string.modern_today_date, localizedToday)
        val todayDescription = activity.getString(R.string.modern_day_a11y_empty, spokenToday)

        composeRule.onNodeWithContentDescription(todayDescription).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(settings).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(settings).performClick()
        composeRule.onNodeWithText(settings).assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(todayDescription).assertIsDisplayed()
    }
}
