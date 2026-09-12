package com.worktime.app.modern.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.R
import com.worktime.app.modern.ModernViewModel
import java.time.YearMonth

enum class ModernDestination { CALENDAR, MONTH_REPORT, YEAR_REPORT, SETTINGS }

private val ModernContentMaxWidth = 720.dp

@Composable
fun ModernWorkTimeApp(viewModel: ModernViewModel) {
    var destination by remember { mutableStateOf(ModernDestination.CALENDAR) }
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()

    BackHandler(enabled = destination != ModernDestination.CALENDAR) {
        destination = when (destination) {
            ModernDestination.YEAR_REPORT -> ModernDestination.MONTH_REPORT
            else -> ModernDestination.CALENDAR
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedContent(
            targetState = destination,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ModernContentMaxWidth)
                .fillMaxWidth(),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = if (forward) {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeIn()
                } else {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeIn()
                }
                val exit = if (forward) {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left) + fadeOut()
                } else {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) + fadeOut()
                }
                enter togetherWith exit
            },
            label = "app_navigation",
        ) { target ->
            when (target) {
                ModernDestination.CALENDAR -> CalendarScreen(
                    viewModel = viewModel,
                    onOpenMonthReport = { destination = ModernDestination.MONTH_REPORT },
                    onOpenSettings = { destination = ModernDestination.SETTINGS },
                )
                ModernDestination.MONTH_REPORT -> MonthReportScreen(
                    viewModel = viewModel,
                    onBack = { destination = ModernDestination.CALENDAR },
                    onOpenYear = { destination = ModernDestination.YEAR_REPORT },
                )
                ModernDestination.YEAR_REPORT -> YearReportScreen(
                    viewModel = viewModel,
                    onBack = { destination = ModernDestination.MONTH_REPORT },
                    onOpenMonth = { selected: YearMonth ->
                        viewModel.selectMonth(selected)
                        destination = ModernDestination.MONTH_REPORT
                    },
                )
                ModernDestination.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { destination = ModernDestination.CALENDAR },
                    currentMonth = month,
                )
            }
        }
    }

    lastError?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(stringResource(R.string.modern_error)) },
            text = { Text(errorMessage(error)) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeError) {
                    Text(stringResource(R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun errorMessage(error: ModernViewModel.ErrorState): String = when (error) {
    is ModernViewModel.ErrorState.Message -> error.text
    is ModernViewModel.ErrorState.Known -> stringResource(
        when (error.kind) {
            ModernViewModel.ErrorKind.OPEN_DAY -> R.string.modern_error_open_day
            ModernViewModel.ErrorKind.SAVE_DAY -> R.string.modern_error_save_day
            ModernViewModel.ErrorKind.DELETE_DAY -> R.string.modern_error_delete_day
            ModernViewModel.ErrorKind.SAVE_SETTING -> R.string.modern_error_save_setting
            ModernViewModel.ErrorKind.PREPARE_RATE_CHANGE -> R.string.modern_error_prepare_rate
            ModernViewModel.ErrorKind.APPLY_RATE_CHANGE -> R.string.modern_error_apply_rate
            ModernViewModel.ErrorKind.CREATE_BACKUP -> R.string.modern_error_create_backup
            ModernViewModel.ErrorKind.INVALID_BACKUP -> R.string.modern_error_invalid_backup
            ModernViewModel.ErrorKind.RESTORE_BACKUP -> R.string.modern_error_restore_backup
        },
    )
}
