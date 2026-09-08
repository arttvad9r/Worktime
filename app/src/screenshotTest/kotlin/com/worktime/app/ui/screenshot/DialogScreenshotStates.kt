package com.worktime.app.ui.screenshot

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.worktime.app.R
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.ui.theme.WorkTimeTheme

/** Standard confirmation overlays included in the visual geometry audit. */

@PreviewTest
@Preview(name = "Delete entry confirmation", widthDp = 360, heightDp = 420, locale = "ru")
@Composable
fun DeleteEntryConfirmationScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.delete_entry)) },
            text = { Text(stringResource(R.string.delete_entry_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {},
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = {}) { Text(stringResource(R.string.cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        )
    }
}

@PreviewTest
@Preview(name = "Change rate confirmation", widthDp = 360, heightDp = 420, locale = "ru")
@Composable
fun ChangeRateConfirmationScreenshot() {
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.change_rate_confirmation_title)) },
            text = { Text(stringResource(R.string.change_rate_confirmation_text)) },
            confirmButton = {
                TextButton(onClick = {}) { Text(stringResource(R.string.change_rate)) }
            },
            dismissButton = {
                TextButton(onClick = {}) { Text(stringResource(R.string.cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        )
    }
}

@PreviewTest
@Preview(name = "Import confirmation", widthDp = 360, heightDp = 420, locale = "ru")
@Composable
fun ImportConfirmationScreenshot() {
    val pendingCount = 42
    WorkTimeTheme(themeMode = ThemeMode.LIGHT) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.import_confirmation_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.import_confirmation_text,
                        pendingCount,
                        pendingCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {},
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = {
                TextButton(onClick = {}) { Text(stringResource(R.string.cancel)) }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        )
    }
}
