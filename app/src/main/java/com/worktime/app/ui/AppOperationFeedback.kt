package com.worktime.app.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.worktime.app.R
import com.worktime.app.ui.backup.BackupOperationError
import com.worktime.app.ui.backup.BackupOperationEvent
import com.worktime.app.ui.backup.BackupViewModel
import com.worktime.app.ui.calendar.CalendarOperationError
import com.worktime.app.ui.calendar.CalendarOperationEvent
import com.worktime.app.ui.calendar.CalendarViewModel

@Composable
internal fun AppOperationFeedback(
    calendarViewModel: CalendarViewModel,
    backupViewModel: BackupViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val haptics = LocalHapticFeedback.current
    val entryDeletedMessage = stringResource(R.string.entry_deleted)
    val rateChangedMessage = stringResource(R.string.rate_changed)
    val noEntriesInPeriodMessage = stringResource(R.string.no_entries_in_period)
    val undoLabel = stringResource(R.string.undo)
    val undoFailedMessage = stringResource(R.string.undo_failed)
    val backupExportedMessage = stringResource(R.string.backup_exported)
    val backupImportedMessage = stringResource(R.string.backup_imported)
    val defaultRateAdoptionFailedMessage = stringResource(R.string.default_rate_adoption_failed)

    LaunchedEffect(
        calendarViewModel,
        haptics,
        entryDeletedMessage,
        rateChangedMessage,
        noEntriesInPeriodMessage,
        undoLabel,
        undoFailedMessage,
        defaultRateAdoptionFailedMessage,
    ) {
        calendarViewModel.operationEvents.collect { event ->
            when (event) {
                CalendarOperationEvent.Success.ENTRY_SAVED ->
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                CalendarOperationEvent.Success.ENTRY_DELETED -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    showUndoSnackbar(
                        snackbarHostState = snackbarHostState,
                        message = entryDeletedMessage,
                        actionLabel = undoLabel,
                        viewModel = calendarViewModel,
                    )
                }
                CalendarOperationEvent.Success.RATE_UPDATED -> {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    showUndoSnackbar(
                        snackbarHostState = snackbarHostState,
                        message = rateChangedMessage,
                        actionLabel = undoLabel,
                        viewModel = calendarViewModel,
                    )
                }
                CalendarOperationEvent.Success.NO_OP ->
                    snackbarHostState.showSnackbar(noEntriesInPeriodMessage)
                CalendarOperationEvent.Success.OPERATION_UNDONE -> Unit
                is CalendarOperationEvent.Error ->
                    when (event.kind) {
                        CalendarOperationError.UNDO ->
                            snackbarHostState.showSnackbar(
                                undoFailedMessage,
                                duration = SnackbarDuration.Long,
                            )
                        CalendarOperationError.DEFAULT_RATE_ADOPTION ->
                            snackbarHostState.showSnackbar(
                                defaultRateAdoptionFailedMessage,
                                duration = SnackbarDuration.Long,
                            )
                        else -> Unit
                    }
            }
        }
    }

    LaunchedEffect(
        backupViewModel,
        calendarViewModel,
        backupExportedMessage,
        backupImportedMessage,
        undoLabel,
        undoFailedMessage,
    ) {
        backupViewModel.events.collect { event ->
            when (event) {
                BackupOperationEvent.Success.EXPORTED ->
                    snackbarHostState.showSnackbar(backupExportedMessage)
                BackupOperationEvent.Success.IMPORTED -> {
                    val result = snackbarHostState.showSnackbar(
                        message = backupImportedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        calendarViewModel.prepareForExternalDataReplacement()
                        backupViewModel.undoImport()
                    }
                }
                BackupOperationEvent.Success.IMPORT_UNDONE -> Unit
                is BackupOperationEvent.Error ->
                    if (event.kind == BackupOperationError.UNDO_IMPORT) {
                        snackbarHostState.showSnackbar(
                            undoFailedMessage,
                            duration = SnackbarDuration.Long,
                        )
                    }
            }
        }
    }
}

private suspend fun showUndoSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
    actionLabel: String,
    viewModel: CalendarViewModel,
) {
    val result = snackbarHostState.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed) {
        viewModel.undoLastOperation()
    }
}
