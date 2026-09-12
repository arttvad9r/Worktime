package com.worktime.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import com.worktime.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkTimeSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun modernCalendarStartsAndOpensSettings() {
        val settings = composeRule.activity.getString(R.string.settings)

        composeRule.onNodeWithContentDescription(settings).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(settings).performClick()
        composeRule.onNodeWithText(settings).assertIsDisplayed()
    }
}
