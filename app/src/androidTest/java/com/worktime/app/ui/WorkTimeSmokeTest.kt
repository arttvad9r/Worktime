package com.worktime.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkTimeSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun modernCalendarStartsAndOpensSettings() {
        composeRule.onNodeWithContentDescription("Настройки").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Настройки").performClick()
        composeRule.onNodeWithText("Настройки").assertIsDisplayed()
    }
}
