package com.worktime.app.modern.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.model.WorkDay
import com.worktime.app.modern.model.WorkTimeMath

@Composable
fun DayEditorSheet(
    state: ModernViewModel.DayEditorState,
    currencyCode: String,
    onDismiss: () -> Unit,
    onSave: (Int, Long, Long, Long, String) -> Unit,
    onDelete: () -> Unit,
) {
    var hours by remember(state.date, state.workedMinutes) {
        mutableStateOf((state.workedMinutes / 60).toString())
    }
    var minutes by remember(state.date, state.workedMinutes) {
        mutableStateOf((state.workedMinutes % 60).toString())
    }
    var rate by remember(state.date, state.rateMinor) { mutableStateOf(moneyInput(state.rateMinor)) }
    var bonus by remember(state.date, state.bonusMinor) { mutableStateOf(moneyInput(state.bonusMinor)) }
    var penalty by remember(state.date, state.penaltyMinor) { mutableStateOf(moneyInput(state.penaltyMinor)) }
    var note by remember(state.date, state.note) { mutableStateOf(state.note) }
    val haptics = LocalHapticFeedback.current

    val hourValue = hours.toIntOrNull()
    val minuteValue = minutes.toIntOrNull()
    val rateValue = parseMoneyMinor(rate)
    val bonusValue = parseMoneyMinor(bonus)
    val penaltyValue = parseMoneyMinor(penalty)
    val workedMinutes = if (hourValue != null && minuteValue != null) hourValue * 60 + minuteValue else -1
    val valid = hourValue != null && hourValue in 0..24 &&
        minuteValue != null && minuteValue in 0..59 && workedMinutes in 0..1440 &&
        rateValue != null && rateValue >= 0L &&
        bonusValue != null && bonusValue >= 0L &&
        penaltyValue != null && penaltyValue >= 0L

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "%02d.%02d.%04d".format(state.date.dayOfMonth, state.date.monthValue, state.date.year),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            Text("Отработано", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter(Char::isDigit).take(2) },
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = hourValue == null || hourValue !in 0..24,
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter(Char::isDigit).take(2) },
                    label = { Text("Минуты") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = minuteValue == null || minuteValue !in 0..59,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(8, 10, 12).forEach { quickHours ->
                    TextButton(
                        onClick = {
                            hours = quickHours.toString()
                            minutes = "0"
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("$quickHours ч") }
                }
            }

            MoneyField("Ставка / час", rate, { rate = it }, currencyCode)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyField("Премия", bonus, { bonus = it }, currencyCode, Modifier.weight(1f))
                MoneyField("Штраф", penalty, { penalty = it }, currencyCode, Modifier.weight(1f))
            }
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 500) note = it },
                label = { Text("Заметка") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
            )

            if (valid) {
                val preview = WorkDay(
                    date = state.date,
                    workedMinutes = workedMinutes,
                    hourlyRateMinor = rateValue!!,
                    bonusMinor = bonusValue!!,
                    penaltyMinor = penaltyValue!!,
                )
                val pay = WorkTimeMath.payForDay(preview)
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(formatMinutes(workedMinutes))
                        Text(formatMoney(pay.totalMinor, currencyCode), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.exists) {
                    OutlinedButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete()
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Удалить", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Button(
                    onClick = { onSave(workedMinutes, rateValue!!, bonusValue!!, penaltyValue!!, note) },
                    enabled = valid,
                    modifier = Modifier.weight(1f),
                ) { Text("Сохранить") }
            }
        }
    }
}

@Composable
private fun MoneyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val parsed = parseMoneyMinor(value)
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (next.length <= 14 && next.all { it.isDigit() || it == ',' || it == '.' }) onValueChange(next)
        },
        label = { Text(label) },
        suffix = { Text(currencyCode) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        isError = parsed == null || parsed < 0L,
    )
}
