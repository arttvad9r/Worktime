package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.dayeditor.DayEditorSheetContent
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayEditorFocusUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun keyboardStaysClosedOnOpenAndImeNextMovesToRate() {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
                    DayEditorSheetContent(
                        date = LocalDate.of(2026, 8, 21),
                        existing = null,
                        recentEntries = emptyList(),
                        defaultHourlyRateMicros = 370_000_000L,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onSave = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("day-editor-duration").assertIsNotFocused()

        composeRule.onNodeWithTag("day-editor-duration").performClick()
        composeRule.onNodeWithTag("day-editor-duration").assertIsFocused()
        composeRule.onNodeWithTag("day-editor-duration").performImeAction()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("day-editor-rate").assertIsFocused()
    }

    @Test
    fun tappingDurationHintFillsDurationField() {
        val history = listOf(
            WorkEntry(LocalDate.of(2026, 8, 14), 13 * 60, 370_000_000L),
            WorkEntry(LocalDate.of(2026, 8, 15), 15 * 60, 370_000_000L),
        )
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(360.dp, 800.dp)) {
                    DayEditorSheetContent(
                        date = LocalDate.of(2026, 8, 21),
                        existing = null,
                        recentEntries = history,
                        defaultHourlyRateMicros = 370_000_000L,
                        operationErrorMessage = null,
                        onDismiss = {},
                        onSave = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("day-editor-hint-900").performClick()
        composeRule.onNodeWithTag("day-editor-duration").assertTextContains("15")
        composeRule.onNodeWithTag("day-editor-hint-780").performClick()
        composeRule.onNodeWithTag("day-editor-duration").assertTextContains("13")
    }
}
