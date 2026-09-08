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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.MoneyLimits
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppFieldValueSlot
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppMotion
import com.worktime.app.ui.components.AppNavigationRow
import com.worktime.app.ui.components.AppRowDivider
import com.worktime.app.ui.components.AppSectionHeader
import com.worktime.app.ui.components.AppSectionSurface
import com.worktime.app.ui.components.AppSegmentedControl
import com.worktime.app.ui.components.AppTopBar
import com.worktime.app.ui.components.CompactMoneyField
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.parseDecimalMicros

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

    if (privacyDataOpen) {
        PrivacyScreen(onDismiss = { privacyDataOpen = false })
        return
    }

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
                AppSectionSurface {
                    RateRow(
                        rateMicros = defaultHourlyRateMicros,
                        editing = rateEditing,
                        onEdit = { rateEditing = true },
                        onDone = { rateEditing = false },
                        onRateChange = onRateChange,
                    )
                    AppRowDivider()
                    AppNavigationRow(
                        label = stringResource(R.string.change_rate_for_period),
                        onClick = {
                            finishRateEditing()
                            onOpenChangeRate()
                        },
                    )
                }

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
                )

                AppSectionHeader(stringResource(R.string.section_data))
                AppSectionSurface {
                    AppNavigationRow(
                        label = stringResource(R.string.export_data),
                        onClick = {
                            finishRateEditing()
                            exportFormatOpen = true
                        },
                    )
                    AppRowDivider()
                    AppNavigationRow(
                        label = stringResource(R.string.import_data),
                        onClick = {
                            finishRateEditing()
                            onImportData()
                        },
                    )
                }

                PrivacyFooterLink(
                    onClick = {
                        finishRateEditing()
                        privacyDataOpen = true
                    },
                )

                Box(modifier = Modifier.navigationBarsPadding().height(16.dp))
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
}

@Composable
private fun PrivacyFooterLink(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.privacy_and_data),
            modifier = Modifier
                .heightIn(min = AppDimens.rowMinHeight)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

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
        horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                fadeIn(animationSpec = tween(AppMotion.MicroMillis)) togetherWith
                    fadeOut(animationSpec = tween(AppMotion.MicroMillis))
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
                        text = formatDecimalMicros(rateMicros),
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
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
        AppSectionSurface {
            AppNavigationRow(
                label = stringResource(R.string.export_json_option),
                subtitle = stringResource(R.string.export_json_hint),
                onClick = onSelectJson,
            )
            AppRowDivider()
            AppNavigationRow(
                label = stringResource(R.string.export_csv_option),
                subtitle = stringResource(R.string.export_csv_hint),
                onClick = onSelectCsv,
            )
        }
    }
}
