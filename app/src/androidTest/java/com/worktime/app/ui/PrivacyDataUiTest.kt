package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.R
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyDataUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(res: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    @Test
    fun settingsFooterOpensScrollablePrivacyPageAndReturnsToSettings() {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 720.dp)) {
                    SettingsScreen(
                        defaultHourlyRateMicros = 370_000_000L,
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
        }

        composeRule.onNodeWithText(string(R.string.privacy_and_data))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText(string(R.string.privacy_intro_text))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_stored_data_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.privacy_policy_title))
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.back))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText(string(R.string.settings)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_data))
            .performScrollTo()
            .assertIsDisplayed()
    }
}
