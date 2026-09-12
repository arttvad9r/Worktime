package com.worktime.app.modern.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.R
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.model.MoneyRules
import com.worktime.app.modern.model.ThemeMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

@Composable
fun SettingsScreen(
    viewModel: ModernViewModel,
    onBack: () -> Unit,
    currentMonth: YearMonth,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val pendingRateChange by viewModel.pendingRateChange.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var defaultRateInput by remember(settings.defaultRateMinor) { mutableStateOf(moneyInput(settings.defaultRateMinor)) }
    var showMonthRate by remember { mutableStateOf(false) }
    var showRangeRate by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }
    val parsedDefaultRate = parseMoneyMinor(defaultRateInput)
    val fileOpenFailed = stringResource(R.string.modern_file_open_failed)
    val backupSaveFailed = stringResource(R.string.modern_backup_save_failed)
    val fileReadFailed = stringResource(R.string.modern_file_read_failed)
    val backupReadFailed = stringResource(R.string.modern_backup_read_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = exportText
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                    ?: kotlin.error(fileOpenFailed)
            }.onFailure { viewModel.reportError(it.message ?: backupSaveFailed) }
        }
        exportText = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: kotlin.error(fileReadFailed)
            }.onSuccess(viewModel::stageImport)
                .onFailure { viewModel.reportError(it.message ?: backupReadFailed) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingsSection(stringResource(R.string.modern_rate_section)) {
                    OutlinedTextField(
                        value = defaultRateInput,
                        onValueChange = { next ->
                            if (next.length <= 14 && next.all { it.isDigit() || it == ',' || it == '.' }) {
                                defaultRateInput = next
                            }
                        },
                        label = { Text(stringResource(R.string.modern_default_rate_per_hour)) },
                        suffix = { Text(settings.currencyCode) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = parsedDefaultRate == null || !MoneyRules.isValid(parsedDefaultRate),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            parsedDefaultRate
                                ?.takeIf(MoneyRules::isValid)
                                ?.let(viewModel::setDefaultRate)
                        },
                        enabled = parsedDefaultRate != null && MoneyRules.isValid(parsedDefaultRate),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.modern_save_default_rate)) }
                    OutlinedButton(onClick = { showMonthRate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.modern_change_rate_month, monthTitle(currentMonth)))
                    }
                    OutlinedButton(onClick = { showRangeRate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.change_rate_for_period))
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.modern_currency)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("RUB", "USD", "EUR").forEach { code ->
                            FilterChip(
                                selected = settings.currencyCode == code,
                                onClick = { viewModel.setCurrency(code) },
                                label = { Text(code) },
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.modern_theme)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            }
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setTheme(mode) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            item {
                SettingsSection(stringResource(R.string.modern_data)) {
                    Button(
                        onClick = {
                            viewModel.exportBackup { payload ->
                                exportText = payload
                                exportLauncher.launch("worktime-backup.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.modern_export_backup)) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.modern_import_backup)) }
                    Text(
                        stringResource(R.string.modern_restore_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showMonthRate) {
        MonthRateDialog(
            month = currentMonth,
            currencyCode = settings.currencyCode,
            onDismiss = { showMonthRate = false },
            onApply = { rate ->
                viewModel.stageRateToMonth(rate)
                showMonthRate = false
            },
        )
    }
    if (showRangeRate) {
        RangeRateDialog(
            currencyCode = settings.currencyCode,
            onDismiss = { showRangeRate = false },
            onApply = { start, end, rate ->
                viewModel.stageRate(start, end, rate)
                showRangeRate = false
            },
        )
    }
    pendingRateChange?.let { staged ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRateChange,
            title = { Text(stringResource(R.string.modern_rate_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val startText = staged.start.format(dateFormatter)
                    val endText = staged.endInclusive?.format(dateFormatter)
                    Text(
                        if (endText == null) {
                            stringResource(R.string.modern_rate_confirm_open_period, startText)
                        } else {
                            stringResource(R.string.modern_rate_confirm_period, startText, endText)
                        },
                    )
                    Text(
                        stringResource(
                            R.string.modern_rate_confirm_value,
                            formatMoney(staged.rateMinor, settings.currencyCode),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.modern_rate_confirm_affected,
                            staged.affectedExistingEntries,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRateChange) {
                    Text(stringResource(R.string.modern_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRateChange) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    pendingImport?.let { staged ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.modern_restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.modern_import_preview,
                        staged.preview.workDayCount,
                        staged.preview.ratePeriodCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.modern_restore)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    lastError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(stringResource(R.string.modern_error)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::consumeError) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
        Spacer(Modifier.height(2.dp))
        HorizontalDivider()
    }
}

@Composable
private fun MonthRateDialog(
    month: YearMonth,
    currencyCode: String,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    val parsed = parseMoneyMinor(value)
    val valid = parsed != null && MoneyRules.isValid(parsed)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.modern_month_rate_title, monthTitle(month))) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.modern_rate_per_hour)) },
                suffix = { Text(currencyCode) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = value.isNotBlank() && !valid,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.takeIf(MoneyRules::isValid)?.let(onApply) },
                enabled = valid,
            ) { Text(stringResource(R.string.modern_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun RangeRateDialog(
    currencyCode: String,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate?, Long) -> Unit,
) {
    var startText by remember { mutableStateOf(LocalDate.now().format(dateFormatter)) }
    var endText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }
    val start = runCatching { LocalDate.parse(startText, dateFormatter) }.getOrNull()
    val end = endText.takeIf(String::isNotBlank)?.let { runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull() }
    val rate = parseMoneyMinor(rateText)
    val endValid = endText.isBlank() || end != null
    val rateValid = rate != null && MoneyRules.isValid(rate)
    val valid = start != null && endValid && (end == null || !end.isBefore(start)) && rateValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rate_for_period)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text(stringResource(R.string.modern_from_date)) },
                    singleLine = true,
                    isError = start == null,
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text(stringResource(R.string.modern_to_date_open_ended)) },
                    singleLine = true,
                    isError = !endValid,
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text(stringResource(R.string.modern_rate_per_hour)) },
                    suffix = { Text(currencyCode) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = rateText.isNotBlank() && !rateValid,
                )
                Text(
                    stringResource(R.string.modern_rate_range_explainer),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validStart = start ?: return@TextButton
                    val validRate = rate?.takeIf(MoneyRules::isValid) ?: return@TextButton
                    onApply(validStart, end, validRate)
                },
                enabled = valid,
            ) { Text(stringResource(R.string.modern_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
