package com.worktime.app.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.worktime.app.ui.components.AppDimens
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

private val ShortScreenHeight = 560.dp
private val ShortScreenGridHeight = 56.dp * 6 + 28.dp

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonth: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    onOpenYearSummary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val scope = rememberCoroutineScope()
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberCalendarPagerState(state.visibleMonth)
    CalendarPagerEffects(
        pagerState = pagerState,
        visibleMonth = state.visibleMonth,
        onSelectMonth = onSelectMonth,
    )
    val today = LocalDate.now()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isShort = maxHeight < ShortScreenHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .then(if (isShort) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            ) {
                CalendarHeader(
                    visibleMonth = state.visibleMonth,
                    isReady = state.isReady,
                    locale = locale,
                    onPreviousMonth = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    onNextMonth = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    onSelectMonth = { monthPickerOpen = true },
                    onSettingsClick = onSettingsClick,
                )
                val pagerModifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .testTag("calendar-pager")
                if (!state.isReady) {
                    Box(
                        modifier = pagerModifier.then(
                            if (isShort) Modifier.height(ShortScreenGridHeight) else Modifier.weight(1f),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = pagerModifier.then(
                            if (isShort) Modifier.height(ShortScreenGridHeight) else Modifier.weight(1f),
                        ),
                        beyondViewportPageCount = 1,
                        key = { page -> page },
                    ) { page ->
                        val month = monthForPage(page)
                        val entries = state.monthEntries[month]
                            ?: if (month == state.visibleMonth) state.entries else emptyMap()
                        val monthUi = remember(month, entries, today, locale) {
                            buildCalendarMonthUi(month, entries, today, locale)
                        }
                        CalendarGrid(
                            month = monthUi,
                            selectedDate = state.selectedDate,
                            onDayClick = onDayClick,
                            locale = locale,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    MonthlySummaryCard(
                        summary = state.summary,
                        locale = locale,
                        onOpenYearSummary = onOpenYearSummary,
                        modifier = Modifier.padding(
                            horizontal = AppDimens.screenHorizontalPadding,
                            vertical = 8.dp,
                        ),
                    )
                }
            }
        }
    }

    if (monthPickerOpen) {
        MonthPickerDialog(
            visibleMonth = state.visibleMonth,
            locale = locale,
            onSelect = { month ->
                monthPickerOpen = false
                onSelectMonth(month)
            },
            onDismiss = { monthPickerOpen = false },
        )
    }
}
