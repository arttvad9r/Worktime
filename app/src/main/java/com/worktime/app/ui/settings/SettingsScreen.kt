package com.worktime.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppFieldValueSlot
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.AppSectionHeader
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros

private const val InlineEditorFadeMillis = 75

@Composable
fun SettingsScreen(
    defaultHourlyRateMicros: Long,
    themeMode: ThemeMode,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onRateChange: (Long) -> Unit,
    onOpenChangeRate: () -> Unit,
    onExportData: () -> Unit,
    onExportCsv: () -> Unit,
    onImportData: () -> Unit,
) {
    var rateEditing by rememberSaveable { mutableStateOf(false) }
    var exportFormatOpen by rememberSaveable { mutableStateOf(false) }
    var privacyDataOpen by rememberSaveable { mutableStateOf(false) }
    var presentedThemeMode by remember { mutableStateOf(themeMode) }
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val finishRateEditing: () -> Unit = {
        rateEditing = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val dismissSettings: () -> Unit = {
        finishRateEditing()
        onDismiss()
    }

    // External preference changes (restore/system-driven state) remain authoritative.
    // Direct taps update both the visual selection and global palette immediately; the
    // segmented-control spring is presentation only and never delays the preference write.
    LaunchedEffect(themeMode) {
        if (themeMode != presentedThemeMode) {
            presentedThemeMode = themeMode
        }
    }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) {
            presentedThemeMode = themeMode
            snackbarHostState.showSnackbar(operationErrorMessage)
        }
    }

    // Surface sets LocalContentColor=onSurface so titles/icons follow the theme.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.screenHorizontalPadding)
                    .imePadding(),
            ) {
                AppTopBar(
                    title = stringResource(R.string.settings),
                    onBack = dismissSettings,
                )
                AppSectionHeader(stringResource(R.string.section_calculation))
                RateRow(
                    rateMicros = defaultHourlyRateMicros,
                    editing = rateEditing,
                    onEdit = { rateEditing = true },
                    onDone = { rateEditing = false },
                    onRateChange = onRateChange,
                )
                AppNavigationRow(
                    label = stringResource(R.string.change_rate_for_period),
                    onClick = {
                        finishRateEditing()
                        onOpenChangeRate()
                    },
                )

                SectionDivider()

                AppSectionHeader(stringResource(R.string.section_appearance))
                AppSegmentedControl(
                    options = ThemeMode.entries.map { themeLabel(it) },
                    selectedIndex = ThemeMode.entries.indexOf(presentedThemeMode),
                    onSelect = { index ->
                        val selectedMode = ThemeMode.entries[index]
                        finishRateEditing()
                        presentedThemeMode = selectedMode
                        if (selectedMode != themeMode) {
                            onThemeChange(selectedMode)
                        }
                    },
                    modifier = Modifier.padding(vertical = AppDimens.rowGap),
                )

                SectionDivider()

                AppSectionHeader(stringResource(R.string.section_data))
                AppNavigationRow(
                    label = stringResource(R.string.export_data),
                    onClick = {
                        finishRateEditing()
                        exportFormatOpen = true
                    },
                )
                AppNavigationRow(
                    label = stringResource(R.string.import_data),
                    onClick = {
                        finishRateEditing()
                        onImportData()
                    },
                )
                AppNavigationRow(
                    label = stringResource(R.string.privacy_and_data),
                    subtitle = stringResource(R.string.privacy_local_subtitle),
                    onClick = {
                        finishRateEditing()
                        privacyDataOpen = true
                    },
                )

                Box(modifier = Modifier.navigationBarsPadding().height(24.dp))
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(AppDimens.screenHorizontalPadding),
            )
        }
    }

    if (exportFormatOpen) {
        ExportFormatDialog(
            onSelectJson = {
                exportFormatOpen = false
                onExportData()
            },
            onSelectCsv = {
                exportFormatOpen = false
                onExportCsv()
            },
            onDismiss = { exportFormatOpen = false },
        )
    }

    if (privacyDataOpen) {
        PrivacyDataSheet(onDismiss = { privacyDataOpen = false })
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * The default-rate row: reads as a value row, edits inline through the same compact slot.
 * Only the trailing content fades through; the row and every following row stay fixed.
 */
@Composable
private fun RateRow(
    rateMicros: Long,
    editing: Boolean,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onRateChange: (Long) -> Unit,
) {
    val label = stringResource(R.string.settings_rate)
    var rateInput by rememberSaveable { mutableStateOf(formatDecimalMicros(rateMicros)) }
    LaunchedEffect(rateMicros, editing) {
        if (!editing) rateInput = formatDecimalMicros(rateMicros)
    }
    val parsed = runCatching { parseDecimalMicros(rateInput) }.getOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.rowMinHeight)
            .clickable(enabled = !editing, onClick = onEdit),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                fadeIn(animationSpec = tween(InlineEditorFadeMillis)) togetherWith
                    fadeOut(animationSpec = tween(InlineEditorFadeMillis))
            },
            label = "default rate editor",
        ) { isEditing ->
            if (isEditing) {
                CompactMoneyField(
                    text = rateInput,
                    onTextChange = { text ->
                        rateInput = text
                        val value = runCatching { parseDecimalMicros(text) }.getOrNull()
                        if (value != null && value <= MoneyLimits.MAX_COMPONENT_MICROS) {
                            onRateChange(value)
                        }
                    },
                    isError = parsed == null || parsed > MoneyLimits.MAX_COMPONENT_MICROS,
                    contentDescription = label,
                    autoFocus = true,
                    onLostFocus = onDone,
                )
            } else {
                AppFieldValueSlot {
                    Text(
                        text = if (rateMicros == 0L) {
                            stringResource(R.string.rate_not_set)
                        } else {
                            formatDecimalMicros(rateMicros)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (rateMicros == 0L) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun themeLabel(themeMode: ThemeMode): String = when (themeMode) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
    ThemeMode.DARK -> stringResource(R.string.theme_dark)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatDialog(
    onSelectJson: () -> Unit,
    onSelectCsv: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.export_format_title),
    ) {
        AppNavigationRow(
            label = stringResource(R.string.export_json_option),
            subtitle = stringResource(R.string.export_json_hint),
            onClick = onSelectJson,
        )
        AppNavigationRow(
            label = stringResource(R.string.export_csv_option),
            subtitle = stringResource(R.string.export_csv_hint),
            onClick = onSelectCsv,
        )
    }
}
