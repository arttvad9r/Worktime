package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CalendarUndoLifetimeTest {
    @Test
    fun `new view model after deletion does not inherit in-memory undo`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val repository = RecreatedProcessWorkEntryRepository(listOf(entry))
        val preferences = RecreatedProcessPreferencesRepository()

        val firstViewModel = CalendarViewModel(repository, preferences)
        firstViewModel.showMonth(YearMonth.of(2026, 8))
        val firstStateJob = launch { firstViewModel.state.collect() }
        firstViewModel.state.first { it.isReady && it.entries[entry.date] == entry }

        firstViewModel.deleteEntry(entry.date)
        assertEquals(
            CalendarOperationEvent.Success.ENTRY_DELETED,
            firstViewModel.operationEvents.first(),
        )
        advanceUntilIdle()
        assertTrue(firstViewModel.state.first { it.canUndo }.canUndo)
        firstStateJob.cancel()

        // A fresh ViewModel models the state available after process recreation: repository
        // data survives, but the previous ViewModel's in-memory UndoSnapshot does not.
        val recreatedViewModel = CalendarViewModel(repository, preferences)
        recreatedViewModel.showMonth(YearMonth.of(2026, 8))
        val recreatedStateJob = launch { recreatedViewModel.state.collect() }
        val recreatedState = recreatedViewModel.state.first { it.isReady }
        assertFalse(recreatedState.canUndo)

        recreatedViewModel.undoLastOperation()
        advanceUntilIdle()

        assertEquals(0, repository.restoreCalls)
        assertEquals(
            null,
            withTimeoutOrNull(50) { recreatedViewModel.operationEvents.first() },
        )
        recreatedStateJob.cancel()
    }
}

private class RecreatedProcessWorkEntryRepository(
    initialEntries: List<WorkEntry>,
) : WorkEntryRepository {
    private val entries = MutableStateFlow(initialEntries)
    var restoreCalls = 0
        private set

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = entries.map { rows ->
        rows.filter { it.date in startDate..endDate }.sortedBy(WorkEntry::date)
    }

    override suspend fun getAll(): List<WorkEntry> = entries.value.sortedBy(WorkEntry::date)

    override suspend fun save(entry: WorkEntry) {
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }

    override suspend fun delete(date: LocalDate) {
        entries.value = entries.value.filterNot { it.date == date }
    }

    override suspend fun restore(entries: List<WorkEntry>) {
        restoreCalls += 1
        val restoredDates = entries.mapTo(mutableSetOf(), WorkEntry::date)
        this.entries.value = this.entries.value.filterNot { it.date in restoredDates } + entries
    }

    override suspend fun replaceAll(entries: List<WorkEntry>) {
        this.entries.value = entries
    }

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> = emptyList()
}

private class RecreatedProcessPreferencesRepository : UserPreferencesRepository {
    override val preferences: Flow<UserPreferences> = flowOf(UserPreferences())
    override val defaultRateInitialized: Flow<Boolean> = flowOf(false)

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) = Unit

    override suspend fun updateThemeMode(themeMode: ThemeMode) = Unit

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) = Unit

    override suspend fun adoptDefaultHourlyRateIfUninitialized(
        defaultHourlyRateMicros: Long,
    ): Boolean = false
}
