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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var defaultRateInput by remember(settings.defaultRateMinor) { mutableStateOf(moneyInput(settings.defaultRateMinor)) }
    var showMonthRate by remember { mutableStateOf(false) }
    var showRangeRate by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf<String?>(null) }
    val parsedDefaultRate = parseMoneyMinor(defaultRateInput)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = exportText
        if (uri != null && text != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
                    ?: kotlin.error("Не удалось открыть файл")
            }.onFailure { viewModel.reportError(it.message ?: "Не удалось сохранить резервную копию") }
        }
        exportText = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: kotlin.error("Не удалось прочитать файл")
            }.onSuccess(viewModel::stageImport)
                .onFailure { viewModel.reportError(it.message ?: "Не удалось прочитать резервную копию") }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text(
                "Настройки",
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
                SettingsSection("Ставка") {
                    OutlinedTextField(
                        value = defaultRateInput,
                        onValueChange = { next ->
                            if (next.length <= 14 && next.all { it.isDigit() || it == ',' || it == '.' }) {
                                defaultRateInput = next
                            }
                        },
                        label = { Text("Стандартная ставка / час") },
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
                    ) { Text("Сохранить стандартную ставку") }
                    OutlinedButton(onClick = { showMonthRate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Изменить ставку за ${monthTitle(currentMonth)}")
                    }
                    OutlinedButton(onClick = { showRangeRate = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Изменить ставку за период")
                    }
                }
            }

            item {
                SettingsSection("Валюта") {
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
                SettingsSection("Тема") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "Система"
                                ThemeMode.LIGHT -> "Светлая"
                                ThemeMode.DARK -> "Тёмная"
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
                SettingsSection("Данные") {
                    Button(
                        onClick = {
                            viewModel.exportBackup { payload ->
                                exportText = payload
                                exportLauncher.launch("worktime-backup.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Экспортировать резервную копию") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Импортировать резервную копию") }
                    Text(
                        "Восстановление заменяет текущие данные только после проверки файла и подтверждения.",
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
                viewModel.applyRateToMonth(rate)
                showMonthRate = false
            },
        )
    }
    if (showRangeRate) {
        RangeRateDialog(
            currencyCode = settings.currencyCode,
            onDismiss = { showRangeRate = false },
            onApply = { start, end, rate ->
                viewModel.applyRate(start, end, rate)
                showRangeRate = false
            },
        )
    }
    pendingImport?.let { staged ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text("Восстановить данные?") },
            text = {
                Text(
                    "Записей: ${staged.preview.workDayCount}. Периодов ставок: ${staged.preview.ratePeriodCount}. " +
                        "Текущие данные будут заменены.",
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmImport) { Text("Восстановить") } },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text("Отмена") } },
        )
    }
    lastError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text("Ошибка") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::consumeError) { Text("OK") } },
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
        title = { Text("Ставка за ${monthTitle(month)}") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Ставка / час") },
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
            ) { Text("Применить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
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
        title = { Text("Ставка за период") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("С даты, ДД.ММ.ГГГГ") },
                    singleLine = true,
                    isError = start == null,
                )
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("По дату (пусто = бессрочно)") },
                    singleLine = true,
                    isError = !endValid,
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Ставка / час") },
                    suffix = { Text(currencyCode) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = rateText.isNotBlank() && !rateValid,
                )
                Text(
                    "Существующие записи в диапазоне будут явно пересчитаны на новую ставку. Новые записи получат её автоматически.",
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
            ) { Text("Применить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
