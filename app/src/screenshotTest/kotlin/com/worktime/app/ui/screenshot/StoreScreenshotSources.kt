package com.worktime.app.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.worktime.app.R
import com.worktime.app.domain.calculation.SalaryCalculator
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.components.LabelValueRow
import com.worktime.app.ui.dayeditor.CalculationSummary
import com.worktime.app.ui.dayeditor.NumericEditorSection
import com.worktime.app.ui.dayeditor.NumericField
import com.worktime.app.ui.settings.PrivacyScreen
import com.worktime.app.ui.theme.WorkTimeTheme
import java.time.LocalDate

/**
 * Reproducible real-UI source states used only to compose the RuStore gallery and visual audit.
 * Modal windows are intentionally unwrapped here because Compose Preview renders window overlays
 * as empty surfaces; the content itself still uses production WorkTime components, typography,
 * dimensions, colors and localized strings.
 */

@PreviewTest
@Preview(name = "Store day editor populated", widthDp = 360, heightDp = 620, locale = "ru")
@Composable
fun StoreDayEditorPopulatedScreenshot() {
    val date = LocalDate.of(2025, 2, 14)
    val draft = WorkEntry(
        date = date,
        workedMinutes = 8 * 60,
        hourlyRateMicros = 500_000_000L,
        bonusMicros = 1_200_000_000L,
        penaltyMicros = 250_000_000L,
    )
    val durationState = rememberTextFieldState(initialText = "8:00")
    val rateState = rememberTextFieldState(initialText = "500")
    val bonusState = rememberTextFieldState(initialText = "1200")
    val penaltyState = rememberTextFieldState(initialText = "250")
    val editorState = rememberTextFieldState(initialText = "8:00")
    val passthrough = remember { InputTransformation.byValue { _, proposed -> proposed } }

    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "пятница, 14 февраля",
                    style = MaterialTheme.typography.titleLarge,
                )
                NumericEditorSection(
                    durationState = durationState,
                    rateState = rateState,
                    bonusState = bonusState,
                    penaltyState = penaltyState,
                    editorState = editorState,
                    activeField = NumericField.Duration,
                    bonusVisible = true,
                    penaltyVisible = true,
                    durationInputTransformation = passthrough,
                    moneyInputTransformation = passthrough,
                    numericKeyboardOptions = KeyboardOptions(),
                    durationHasError = false,
                    rateHasError = false,
                    bonusHasError = false,
                    penaltyHasError = false,
                    onActivateField = {},
                    onShowBonus = {},
                    onShowPenalty = {},
                    onNext = {},
                    editorFocusRequester = remember { FocusRequester() },
                    onEditorFocusChanged = {},
                )
                CalculationSummary(
                    draft = draft,
                    totalMicros = SalaryCalculator.entryPay(draft).totalPayMicros,
                )
                AppPrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = {},
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Store change rate current month", widthDp = 360, heightDp = 470, locale = "ru")
@Composable
fun StoreChangeRateCurrentMonthScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.rate_for_period),
                    style = MaterialTheme.typography.titleLarge,
                )
                AppSegmentedControl(
                    options = listOf(
                        stringResource(R.string.current_month),
                        stringResource(R.string.custom_period),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                )
                AppSectionSurface {
                    LabelValueRow(
                        label = stringResource(R.string.start_date),
                        value = "1 фев. 2025",
                        modifier = Modifier.heightIn(min = AppDimens.rowMinHeight),
                    )
                    AppRowDivider()
                    LabelValueRow(
                        label = stringResource(R.string.end_date),
                        value = "28 фев. 2025",
                        modifier = Modifier.heightIn(min = AppDimens.rowMinHeight),
                    )
                    AppRowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AppDimens.rowMinHeight),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.hourly_rate),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        CompactMoneyField(
                            text = "500",
                            onTextChange = {},
                            isError = false,
                            contentDescription = stringResource(R.string.hourly_rate),
                        )
                    }
                }
                AppPrimaryButton(
                    text = stringResource(R.string.change_rate),
                    onClick = {},
                    enabled = true,
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Store privacy dark", widthDp = 360, heightDp = 800, locale = "ru")
@Composable
fun StorePrivacyDarkScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.DARK) {
        PrivacyScreen(onDismiss = {})
    }
}

@PreviewTest
@Preview(name = "Store export format", widthDp = 360, heightDp = 360, locale = "ru")
@Composable
fun StoreExportFormatScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.export_format_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                AppSectionSurface {
                    AppNavigationRow(
                        label = stringResource(R.string.export_json_option),
                        subtitle = stringResource(R.string.export_json_hint),
                        onClick = {},
                    )
                    AppRowDivider()
                    AppNavigationRow(
                        label = stringResource(R.string.export_csv_option),
                        subtitle = stringResource(R.string.export_csv_hint),
                        onClick = {},
                    )
                }
            }
        }
    }
}
