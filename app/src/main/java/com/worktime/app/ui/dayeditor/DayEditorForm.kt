package com.worktime.app.ui.dayeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.worktime.app.R
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.ui.components.AppDestructiveAction
import com.worktime.app.ui.components.AppDimens
import com.worktime.app.ui.components.AppModalBottomSheet
import com.worktime.app.ui.components.AppPrimaryButton
import com.worktime.app.ui.format.formatDecimalMicros
import com.worktime.app.ui.format.formatDurationCompact
import com.worktime.app.ui.format.sanitizeMoneyInput
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun DayEditorSheetContent(
    date: LocalDate,
    existing: WorkEntry?,
    recentEntries: List<WorkEntry>,
    defaultHourlyRateMicros: Long,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    val durationState = rememberTextFieldState(
        initialText = if ((existing?.workedMinutes ?: 0) == 0) "" else formatDurationCompact(existing!!.workedMinutes),
    )
    val rateState = rememberTextFieldState(
        initialText = moneyText(existing?.hourlyRateMicros ?: defaultHourlyRateMicros),
    )
    val bonusState = rememberTextFieldState(initialText = moneyText(existing?.bonusMicros ?: 0L))
    val penaltyState = rememberTextFieldState(initialText = moneyText(existing?.penaltyMicros ?: 0L))

    val durationTransformation = remember {
        InputTransformation.byValue { _, proposed -> sanitizeDurationInput(proposed.toString()) }
    }
    val moneyTransformation = remember {
        InputTransformation.byValue { _, proposed -> sanitizeMoneyInput(proposed.toString()) }
    }

    val editor = evaluateDayEditor(
        date = date,
        input = DayEditorInput(
            duration = durationState.text.toString(),
            rate = rateState.text.toString(),
            bonus = bonusState.text.toString(),
            penalty = penaltyState.text.toString(),
        ),
        note = existing?.note.orEmpty(),
    )

    val suggestions = remember(date.dayOfWeek, recentEntries) {
        suggestDurations(date.dayOfWeek, durationSamples(recentEntries))
    }

    val focusManager = LocalFocusManager.current
    val durationFocus = remember { FocusRequester() }
    val rateFocus = remember { FocusRequester() }
    val bonusFocus = remember { FocusRequester() }
    val penaltyFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(operationErrorMessage) {
        if (!operationErrorMessage.isNullOrBlank()) snackbarHostState.showSnackbar(operationErrorMessage)
    }

    val next = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
    val done = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)

    AppModalBottomSheet(
        onDismissRequest = onDismiss,
        title = date.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM", LocalLocale.current.platformLocale),
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(AppDimens.rowGap),
            ) {
                OutlinedTextField(
                    state = durationState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(durationFocus)
                        .testTag("day-editor-duration"),
                    label = { Text(stringResource(R.string.worked)) },
                    placeholder = { Text(stringResource(R.string.duration_placeholder)) },
                    isError = editor.durationError,
                    inputTransformation = durationTransformation,
                    keyboardOptions = next,
                    onKeyboardAction = { rateFocus.requestFocus() },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { minutes ->
                        FilterChip(
                            selected = parseDurationMinutes(durationState.text.toString()) == minutes,
                            onClick = {
                                durationState.setTextAndPlaceCursorAtEnd(formatDurationCompact(minutes))
                            },
                            label = {
                                Text(stringResource(R.string.hours_short, formatDurationCompact(minutes)))
                            },
                            modifier = Modifier.testTag("day-editor-hint-$minutes"),
                        )
                    }
                }
                OutlinedTextField(
                    state = rateState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(rateFocus)
                        .testTag("day-editor-rate"),
                    label = { Text(stringResource(R.string.hourly_rate)) },
                    isError = editor.rateError,
                    inputTransformation = moneyTransformation,
                    keyboardOptions = next,
                    onKeyboardAction = { bonusFocus.requestFocus() },
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.rowGap)) {
                    OutlinedTextField(
                        state = bonusState,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(bonusFocus)
                            .testTag("day-editor-bonus"),
                        label = { Text(stringResource(R.string.bonus)) },
                        isError = editor.bonusError,
                        inputTransformation = moneyTransformation,
                        keyboardOptions = next,
                        onKeyboardAction = { penaltyFocus.requestFocus() },
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                    OutlinedTextField(
                        state = penaltyState,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(penaltyFocus)
                            .testTag("day-editor-penalty"),
                        label = { Text(stringResource(R.string.penalty)) },
                        isError = editor.penaltyError,
                        inputTransformation = moneyTransformation,
                        keyboardOptions = done,
                        onKeyboardAction = {
                            editor.entry?.takeIf { editor.canSave }?.let(onSave) ?: focusManager.clearFocus()
                        },
                        lineLimits = TextFieldLineLimits.SingleLine,
                    )
                }

                CalculationSummary(draft = editor.entry, totalMicros = editor.totalMicros)

                AppPrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = { editor.entry?.let(onSave) },
                    enabled = editor.canSave,
                )
                if (existing != null) {
                    AppDestructiveAction(
                        text = stringResource(R.string.delete_entry),
                        onClick = { confirmDelete = true },
                    )
                }
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

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete(date)
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun moneyText(micros: Long): String = if (micros == 0L) "" else formatDecimalMicros(micros)
