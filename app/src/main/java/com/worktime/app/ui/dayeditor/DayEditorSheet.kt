package com.worktime.app.ui.dayeditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.worktime.app.domain.model.WorkEntry
import java.time.LocalDate

@Composable
fun DayEditorSheet(
    date: LocalDate,
    existing: WorkEntry?,
    recentEntries: List<WorkEntry>,
    defaultHourlyRateMicros: Long,
    operationErrorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (WorkEntry) -> Unit,
    onDelete: (LocalDate) -> Unit,
) {
    key(date.toEpochDay(), existing) {
        DayEditorSheetContent(
            date = date,
            existing = existing,
            recentEntries = recentEntries,
            defaultHourlyRateMicros = defaultHourlyRateMicros,
            operationErrorMessage = operationErrorMessage,
            onDismiss = onDismiss,
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}
