package com.worktime.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.R
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppDestructiveAction
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.dayeditor.NumericEditorSection
import com.worktime.app.ui.dayeditor.NumericField
import com.worktime.app.ui.settings.SettingsScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Component-contract checks for the UI-consistency pass: shared rows keep their
 * geometry while fields switch, and one primitive answers for one pattern.
 */
@RunWith(AndroidJUnit4::class)
class UiConsistencyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun string(res: Int): String =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    @Test
    fun settingsRateRowShowsValueThenEditsInlineWithoutMovingNextRow() {
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
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

        // Normal state reads as a value row: formatted rate visible next to the label.
        composeRule.onNodeWithText("370").assertIsDisplayed()
        val changeRateBefore = composeRule.onNodeWithText(string(R.string.change_rate_for_period))
            .fetchSemanticsNode().boundsInRoot.top
        val importBefore = composeRule.onNodeWithText(string(R.string.import_data))
            .fetchSemanticsNode().boundsInRoot.top

        composeRule.onNodeWithText(string(R.string.settings_rate)).performClick()
        composeRule.waitForIdle()

        // Editing swaps the value for the compact inline field in the same trailing slot.
        // Row-to-row distance is asserted so a global IME inset shift cannot mask jumps,
        // while an intra-column jump still fails.
        composeRule.onNodeWithContentDescription(string(R.string.settings_rate)).assertIsDisplayed()
        val changeRateAfter = composeRule.onNodeWithText(string(R.string.change_rate_for_period))
            .fetchSemanticsNode().boundsInRoot.top
        val importAfter = composeRule.onNodeWithText(string(R.string.import_data))
            .fetchSemanticsNode().boundsInRoot.top
        val spreadBefore = importBefore - changeRateBefore
        val spreadAfter = importAfter - changeRateAfter
        assert(abs(spreadBefore - spreadAfter) < 2f) {
            "rows jumped when editing started: $spreadBefore -> $spreadAfter"
        }
    }

    @Test
    fun dayEditorKeepsSemanticRowOrderWhenFieldsSwitchToCompactEditors() {
        composeRule.setContent {
            WorkTimeTheme {
                var activeField by remember { mutableStateOf(NumericField.Duration) }
                var bonusVisible by remember { mutableStateOf(false) }
                var penaltyVisible by remember { mutableStateOf(false) }
                val durationState = rememberTextFieldState(initialText = "00:00")
                val rateState = rememberTextFieldState(initialText = "370")
                val bonusState = rememberTextFieldState()
                val penaltyState = rememberTextFieldState()
                val editorState = rememberTextFieldState(initialText = "00:00")
                val passthroughTransformation = remember {
                    InputTransformation.byValue { _, proposed -> proposed }
                }
                val focusRequester = remember { FocusRequester() }

                // Geometry is tested at the component boundary instead of inside the
                // scrollable modal sheet. Clicking a field here changes only logical
                // selection, so a real-device IME transition cannot clip/scroll the
                // semantics bounds and masquerade as a row-layout regression.
                Box(Modifier.size(320.dp, 320.dp)) {
                    NumericEditorSection(
                        durationState = durationState,
                        rateState = rateState,
                        bonusState = bonusState,
                        penaltyState = penaltyState,
                        editorState = editorState,
                        activeField = activeField,
                        bonusVisible = bonusVisible,
                        penaltyVisible = penaltyVisible,
                        durationInputTransformation = passthroughTransformation,
                        moneyInputTransformation = passthroughTransformation,
                        numericKeyboardOptions = KeyboardOptions(),
                        durationHasError = false,
                        rateHasError = false,
                        bonusHasError = false,
                        penaltyHasError = false,
                        editorHasFocus = false,
                        onActivateField = { activeField = it },
                        onShowBonus = {
                            bonusVisible = true
                            activeField = NumericField.Bonus
                        },
                        onShowPenalty = {
                            penaltyVisible = true
                            activeField = NumericField.Penalty
                        },
                        onNext = {},
                        editorFocusRequester = focusRequester,
                        onEditorFocusChanged = {},
                    )
                }
            }
        }

        composeRule.waitForIdle()
        fun topOf(tag: String) =
            composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

        val rateTopBefore = topOf("day-editor-row-rate")
        val bonusTopBefore = topOf("day-editor-row-bonus")
        val penaltyTopBefore = topOf("day-editor-row-penalty")
        // Duration is the initially active field, so the persistent editor owns its slot.
        assert(topOf("day-editor-active-field") < rateTopBefore) {
            "active editor does not start in the duration slot"
        }

        // Activate the bonus row: the persistent editor must land in the same slot.
        composeRule.onNodeWithTag("day-editor-row-bonus").performClick()
        composeRule.waitForIdle()

        assert(topOf("day-editor-row-duration") < topOf("day-editor-row-rate")) {
            "duration/rate order broken"
        }
        assert(topOf("day-editor-row-rate") < topOf("day-editor-row-penalty")) {
            "rate/penalty order broken"
        }
        val rateDrift = abs(topOf("day-editor-row-rate") - rateTopBefore)
        assert(rateDrift <= 2f) { "rate row jumped by ${rateDrift}px" }
        val penaltyDrift = abs(topOf("day-editor-row-penalty") - penaltyTopBefore)
        assert(penaltyDrift <= 2f) { "penalty row jumped by ${penaltyDrift}px" }
        val activeTop = topOf("day-editor-active-field")
        val activeDrift = abs(activeTop - bonusTopBefore)
        assert(activeDrift <= 2f) {
            "active editor did not take over the bonus slot: drift ${activeDrift}px"
        }
    }

    @Test
    fun segmentedControlReportsSelectionForEveryOption() {
        val selected = mutableIntStateOf(0)
        composeRule.setContent {
            WorkTimeTheme {
                AppSegmentedControl(
                    options = listOf(
                        string(R.string.current_month),
                        string(R.string.custom_period),
                    ),
                    selectedIndex = selected.intValue,
                    onSelect = { selected.intValue = it },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.current_month)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.custom_period)).assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle {
            check(selected.intValue == 1) { "custom period not selected: ${selected.intValue}" }
        }
        composeRule.onNodeWithText(string(R.string.current_month)).performClick()
        composeRule.runOnIdle {
            check(selected.intValue == 0) { "current month not selected: ${selected.intValue}" }
        }
    }

    @Test
    fun navigationRowShowsLabelSubtitleAndHandlesClicks() {
        var clicks = 0
        composeRule.setContent {
            WorkTimeTheme {
                AppNavigationRow(
                    label = string(R.string.export_json_option),
                    subtitle = string(R.string.export_json_hint),
                    onClick = { clicks += 1 },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.export_json_option)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_json_hint)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.export_json_option)).performClick()
        composeRule.runOnIdle { check(clicks == 1) }
    }

    @Test
    fun primaryAndDestructiveActionsStayVisibleWhenDisabled() {
        var saved = false
        composeRule.setContent {
            WorkTimeTheme {
                Box(Modifier.size(320.dp, 800.dp)) {
                    AppPrimaryButton(
                        text = string(R.string.save),
                        onClick = { saved = true },
                        enabled = false,
                    )
                    AppDestructiveAction(text = string(R.string.delete_entry), onClick = {})
                }
            }
        }

        composeRule.onNodeWithText(string(R.string.save)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.delete_entry)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        composeRule.waitForIdle()
        check(!saved) { "disabled primary button was clickable" }
    }
}
