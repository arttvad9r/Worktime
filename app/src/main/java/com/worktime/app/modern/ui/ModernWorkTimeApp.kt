package com.worktime.app.modern.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.worktime.app.R
import com.worktime.app.modern.ModernViewModel
import java.time.YearMonth

private val ModernContentMaxWidth = 720.dp

@Composable
fun ModernWorkTimeApp(viewModel: ModernViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(ModernDestination.Calendar)

    fun popDestination() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun pushDestination(destination: ModernDestination) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ModernContentMaxWidth)
                .fillMaxWidth(),
            onBack = ::popDestination,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            popTransitionSpec = {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            },
            predictivePopTransitionSpec = { _ ->
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            },
            entryProvider = entryProvider {
                entry<ModernDestination.Calendar> {
                    CalendarScreen(
                        viewModel = viewModel,
                        onOpenMonthReport = {
                            pushDestination(ModernDestination.MonthReport)
                        },
                        onOpenSettings = {
                            pushDestination(ModernDestination.Settings)
                        },
                    )
                }
                entry<ModernDestination.MonthReport> {
                    MonthReportScreen(
                        viewModel = viewModel,
                        onBack = ::popDestination,
                        onOpenYear = {
                            pushDestination(ModernDestination.YearReport)
                        },
                    )
                }
                entry<ModernDestination.YearReport> {
                    YearReportScreen(
                        viewModel = viewModel,
                        onBack = ::popDestination,
                        onOpenMonth = { selected: YearMonth ->
                            viewModel.selectMonth(selected)
                            if (backStack.lastOrNull() == ModernDestination.YearReport) {
                                backStack.removeLastOrNull()
                            }
                            if (backStack.lastOrNull() != ModernDestination.MonthReport) {
                                backStack.add(ModernDestination.MonthReport)
                            }
                        },
                    )
                }
                entry<ModernDestination.Settings> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = ::popDestination,
                        currentMonth = month,
                    )
                }
            },
        )
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
