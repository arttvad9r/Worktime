package com.worktime.app.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.worktime.app.R
import com.worktime.app.ui.backup.BackupImportPreview
import com.worktime.app.ui.backup.BackupUiState
import com.worktime.app.ui.backup.BackupViewModel
import com.worktime.app.ui.calendar.CalendarOperationError
import com.worktime.app.ui.calendar.CalendarUiState
import com.worktime.app.ui.calendar.CalendarViewModel
import com.worktime.app.ui.dayeditor.DayEditorSheet
import com.worktime.app.ui.preferences.PreferencesUiState
import com.worktime.app.ui.settings.ChangeRateSheet
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun BoxScope.AppOverlays(
    calendarState: CalendarUiState,
    preferencesState: PreferencesUiState,
    backupState: BackupUiState,
    calendarViewModel: CalendarViewModel,
    backupViewModel: BackupViewModel,
    snackbarHostState: SnackbarHostState,
) {
    calendarState.selectedDate?.let { date ->
        val operationErrorMessage = when (calendarState.operationError) {
            CalendarOperationError.SAVE_ENTRY -> stringResource(R.string.save_entry_failed)
            CalendarOperationError.DELETE_ENTRY -> stringResource(R.string.delete_entry_failed)
            CalendarOperationError.BULK_RATE -> stringResource(R.string.bulk_rate_failed)
            CalendarOperationError.UNDO -> stringResource(R.string.undo_failed)
            else -> null
        }
        DayEditorSheet(
            date = date,
            existing = calendarState.entries[date],
            recentEntries = calendarState.recentEntries,
            defaultHourlyRateMicros = preferencesState.defaultHourlyRateMicros,
            operationErrorMessage = operationErrorMessage,
            onDismiss = calendarViewModel::dismissEditor,
            onSave = calendarViewModel::saveEntry,
            onDelete = calendarViewModel::deleteEntry,
        )
    }

    if (calendarState.isChangeRateSheetOpen) {
        val operationErrorMessage = when (calendarState.operationError) {
            CalendarOperationError.BULK_RATE -> stringResource(R.string.bulk_rate_failed)
            else -> null
        }
        ChangeRateSheet(
            visibleMonth = calendarState.visibleMonth,
            initialRange = calendarState.changeRateInitialRange,
            operationErrorMessage = operationErrorMessage,
            onDismiss = calendarViewModel::dismissChangeRateSheet,
            onChangeRate = calendarViewModel::changeRateForPeriod,
            countAffectedEntries = calendarViewModel::countEntries,
        )
    }

    backupState.pendingImport?.let { preview ->
        ImportConfirmationDialog(
            preview = preview,
            onConfirm = {
                calendarViewModel.prepareForExternalDataReplacement()
                backupViewModel.confirmImport()
            },
            onDismiss = backupViewModel::cancelImport,
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding(),
    )
}

@Composable
private fun ImportConfirmationDialog(
    preview: BackupImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val details = buildList {
        add(
            pluralStringResource(
                R.plurals.import_confirmation_text,
                preview.importedEntryCount,
                preview.importedEntryCount,
            ),
        )
        if (preview.firstDate != null && preview.lastDate != null) {
            add(
                stringResource(
                    R.string.import_confirmation_period,
                    preview.firstDate.format(formatter),
                    preview.lastDate.format(formatter),
                ),
            )
        }
        add(
            pluralStringResource(
                R.plurals.import_replacement_warning,
                preview.currentEntryCount,
                preview.currentEntryCount,
            ),
        )
    }.joinToString("\n\n")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_confirmation_title)) },
        text = { Text(details) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.replace)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
