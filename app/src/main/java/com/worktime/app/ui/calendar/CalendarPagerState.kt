package com.worktime.app.ui.calendar

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val FirstMonth: YearMonth = YearMonth.of(1970, 1)
private val LastMonth: YearMonth = YearMonth.of(2100, 12)
internal val CalendarPageCount: Int = ChronoUnit.MONTHS.between(FirstMonth, LastMonth).toInt() + 1

internal fun pageForMonth(month: YearMonth): Int =
    ChronoUnit.MONTHS.between(FirstMonth, month).toInt().coerceIn(0, CalendarPageCount - 1)

internal fun monthForPage(page: Int): YearMonth = FirstMonth.plusMonths(page.toLong())

@Composable
internal fun rememberCalendarPagerState(initialMonth: YearMonth): PagerState =
    rememberPagerState(
        initialPage = pageForMonth(initialMonth),
        pageCount = { CalendarPageCount },
    )

/**
 * The view model's visible month is the single source of truth. A user swipe reports the settled
 * page to the view model; any other month change (arrows, picker, restore) moves the pager.
 * Neighbouring months animate, distant ones jump so no unloaded pages flash by.
 */
@Composable
internal fun CalendarPagerEffects(
    pagerState: PagerState,
    visibleMonth: YearMonth,
    onSelectMonth: (YearMonth) -> Unit,
) {
    LaunchedEffect(pagerState, visibleMonth) {
        val target = pageForMonth(visibleMonth)
        val distance = abs(target - pagerState.currentPage)
        when {
            target == pagerState.currentPage -> Unit
            distance == 1 -> pagerState.animateScrollToPage(target)
            else -> pagerState.scrollToPage(target)
        }
    }
    val currentVisibleMonth by rememberUpdatedState(visibleMonth)
    val currentOnSelectMonth by rememberUpdatedState(onSelectMonth)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val month = monthForPage(page)
            if (month != currentVisibleMonth) currentOnSelectMonth(month)
        }
    }
}
