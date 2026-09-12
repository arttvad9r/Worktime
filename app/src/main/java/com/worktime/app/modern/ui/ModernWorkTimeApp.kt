package com.worktime.app.modern.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.worktime.app.modern.ModernViewModel
import java.time.YearMonth

enum class ModernDestination { CALENDAR, MONTH_REPORT, YEAR_REPORT, SETTINGS }

@Composable
fun ModernWorkTimeApp(viewModel: ModernViewModel) {
    var destination by remember { mutableStateOf(ModernDestination.CALENDAR) }
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()

    BackHandler(enabled = destination != ModernDestination.CALENDAR) {
        destination = when (destination) {
            ModernDestination.YEAR_REPORT -> ModernDestination.MONTH_REPORT
            else -> ModernDestination.CALENDAR
        }
    }

    AnimatedContent(
        targetState = destination,
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
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
