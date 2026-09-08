package com.worktime.app.ui.calendar

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CalendarViewModelTest {
    @Test
    fun `serialized mutations leave the newer same-date save last`() = runTest {
        val first = WorkEntry(LocalDate.of(2026, 8, 10), 60, 10_000_000)
        val second = first.copy(workedMinutes = 120)
        val repository = FakeWorkEntryRepository(emptyList()).apply {
            saveStarted = CompletableDeferred()
            releaseFirstSave = CompletableDeferred()
        }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(first)
        repository.saveStarted!!.await()
        viewModel.saveEntry(second)
        runCurrent()
        assertTrue(repository.entries.value.isEmpty())

        repository.releaseFirstSave!!.complete(Unit)
        runCurrent()
        advanceUntilIdle()
        val finalState = withTimeoutOrNull(1_000) {
            while (repository.entries.value != listOf(second)) kotlinx.coroutines.yield()
            repository.entries.value
        }
        assertEquals(listOf(second), finalState)
        stateJob.cancel()
    }

    @Test
    fun `deleting an existing entry stores exact entry and undo restores it`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000, note = "original")
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady && it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)
        advanceUntilIdle()

        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())

        assertEquals(listOf(entry), repository.restoredEntries)
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `external data replacement invalidates pending undo`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)
        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.canUndo }.canUndo)

        viewModel.prepareForExternalDataReplacement()
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
        viewModel.undoLastOperation()
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        stateJob.cancel()
    }

    @Test
    fun `bulk rate update stores all original records and undo restores mixed rates`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 8, 9), 480, 10_000_000),
            WorkEntry(LocalDate.of(2026, 8, 10), 420, 11_000_000, bonusMicros = 2_000_000),
            WorkEntry(LocalDate.of(2026, 8, 12), 360, 12_000_000, penaltyMicros = 1_000_000, note = "late"),
        )
        val repository = FakeWorkEntryRepository(entries)
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(
            LocalDate.of(2026, 8, 10),
            LocalDate.of(2026, 8, 12),
            20_000_000,
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        assertEquals(entries.subList(1, 3), repository.bulkOriginals)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())

        assertEquals(entries.subList(1, 3).toList(), repository.restoredEntries.toList())
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `failed bulk operation does not expose undo`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { bulkError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(
            LocalDate.of(2026, 8, 12),
            LocalDate.of(2026, 8, 10),
            20_000_000,
        )
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(
            CalendarOperationError.BULK_RATE,
            viewModel.state.first { it.operationError == CalendarOperationError.BULK_RATE }.operationError,
        )
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertEquals(null, viewModel.state.value.selectedDate)
        stateJob.cancel()
    }

    @Test
    fun `bulk rate rejects non-positive rates without repository call`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 0L)
        advanceUntilIdle()

        assertEquals(0, repository.bulkCalls)
        assertEquals(
            CalendarOperationError.BULK_RATE,
            viewModel.state.first { it.operationError == CalendarOperationError.BULK_RATE }.operationError,
        )
        stateJob.cancel()
    }

    @Test
    fun `successful delete emits one root event without replay`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)

        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        stateJob.cancel()
    }

    @Test
    fun `failed delete emits root error and leaves undo unavailable`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { deleteError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.deleteEntry(LocalDate.of(2026, 8, 10))

        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.DELETE_ENTRY), viewModel.operationEvents.first())
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `empty bulk update is a no-op without undo`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        assertEquals(CalendarOperationEvent.Success.NO_OP, viewModel.operationEvents.first())
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `new save supersedes undo snapshot`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val replacement = entry.copy(note = "replacement")
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.entries[entry.date] == entry }

        viewModel.deleteEntry(entry.date)
        advanceUntilIdle()
        assertEquals(CalendarOperationEvent.Success.ENTRY_DELETED, viewModel.operationEvents.first())
        viewModel.saveEntry(replacement)
        advanceUntilIdle()
        assertEquals(CalendarOperationEvent.Success.ENTRY_SAVED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        assertFalse(viewModel.state.value.canUndo)
        stateJob.cancel()
    }

    @Test
    fun `saving an entry with empty default rate adopts the entry rate as default`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferencesRepository = FakeUserPreferencesRepository()
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 720, 370_000_000L))

        assertEquals(
            UserPreferences(370_000_000L, ThemeMode.SYSTEM),
            preferencesRepository.preferences.first { it.defaultHourlyRateMicros > 0L },
        )
        assertEquals(
            listOf(UserPreferences(370_000_000L, ThemeMode.SYSTEM)),
            preferencesRepository.updates,
        )
        stateJob.cancel()
    }

    @Test
    fun `theme change before first save does not prevent default rate adoption`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferences = FakeUserPreferencesRepository()
        val viewModel = CalendarViewModel(repository, preferences)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        preferences.updateThemeMode(ThemeMode.DARK)
        preferences.preferences.first { it.themeMode == ThemeMode.DARK }
        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 60, 370_000_000L))

        assertEquals(
            UserPreferences(370_000_000L, ThemeMode.DARK),
            preferences.preferences.first { it.defaultHourlyRateMicros == 370_000_000L },
        )
        assertTrue(preferences.initialized)
        stateJob.cancel()
    }

    @Test
    fun `saving an entry keeps an existing default rate untouched`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferencesRepository = FakeUserPreferencesRepository()
        preferencesRepository.update(
            defaultHourlyRateMicros = 300_000_000L,
            themeMode = ThemeMode.DARK,
            defaultRateInitialized = true,
        )
        val viewModel = CalendarViewModel(repository, preferencesRepository)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 720, 370_000_000L))
        repository.observeMonth(YearMonth.now()).first { it.isNotEmpty() }

        assertEquals(
            listOf(UserPreferences(300_000_000L, ThemeMode.DARK)),
            preferencesRepository.updates,
        )
        assertEquals(300_000_000L, preferencesRepository.preferences.first().defaultHourlyRateMicros)
        stateJob.cancel()
    }

    @Test
    fun `adoption failure leaves saved entry successful and emits one global adoption error`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 720, 370_000_000L)
        val repository = FakeWorkEntryRepository(emptyList())
        val preferences = FakeUserPreferencesRepository().apply { adoptionError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, preferences)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(entry)

        assertEquals(entry, repository.entries.first { it.isNotEmpty() }.single())
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.DEFAULT_RATE_ADOPTION), viewModel.operationEvents.first())
        assertEquals(CalendarOperationEvent.Success.ENTRY_SAVED, viewModel.operationEvents.first())
        assertEquals(null, withTimeoutOrNull(50) { viewModel.operationEvents.first() })
        assertEquals(null, viewModel.state.value.selectedDate)
        assertEquals(CalendarOperationError.DEFAULT_RATE_ADOPTION, viewModel.state.value.operationError)
        stateJob.cancel()
    }

    @Test
    fun `clearing default rate prevents later automatic adoption`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val preferences = FakeUserPreferencesRepository()
        val viewModel = CalendarViewModel(repository, preferences)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 60, 370_000_000L))
        repository.observeMonth(YearMonth.now()).first { it.isNotEmpty() }
        preferences.updateDefaultHourlyRate(0L)
        preferences.preferences.first { it.defaultHourlyRateMicros == 0L && preferences.initialized }
        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 11), 60, 420_000_000L))
        repository.observeMonth(YearMonth.now()).first { it.size == 2 }

        assertEquals(0L, preferences.preferences.first().defaultHourlyRateMicros)
        stateJob.cancel()
    }

    @Test
    fun `two quick eligible saves adopt the first serialized rate`() = runTest {
        val preferences = FakeUserPreferencesRepository()
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, preferences)
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 10), 60, 370_000_000L))
        viewModel.saveEntry(WorkEntry(LocalDate.of(2026, 8, 11), 60, 420_000_000L))

        repository.observeMonth(YearMonth.now()).first { it.size == 2 }
        assertEquals(
            370_000_000L,
            preferences.preferences.first { it.defaultHourlyRateMicros > 0L }.defaultHourlyRateMicros,
        )
        stateJob.cancel()
    }

    @Test
    fun `bulk undo keeps snapshot when atomic restore fails and can retry`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000),
            WorkEntry(LocalDate.of(2026, 8, 11), 480, 11_000_000),
        )
        val repository = FakeWorkEntryRepository(entries)
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.changeRateForPeriod(entries.first().date, entries.last().date, 20_000_000)
        viewModel.operationEvents.first()
        repository.restoreError = IllegalStateException()

        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.UNDO), viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        repository.restoreError = null
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())
        assertFalse(viewModel.state.first { !it.canUndo }.canUndo)
        assertEquals(entries, repository.restoredEntries)
        stateJob.cancel()
    }

    @Test
    fun `bulk and undo errors are exposed when editor is closed`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply {
            bulkError = IllegalStateException()
        }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertEquals(null, viewModel.state.value.selectedDate)

        repository.bulkError = null
        repository.restoreError = IllegalStateException()
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        repository.replaceEntries(listOf(entry))
        viewModel.changeRateForPeriod(entry.date, entry.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.UNDO), viewModel.operationEvents.first())
        assertEquals(
            CalendarOperationError.UNDO,
            viewModel.state.first { it.operationError == CalendarOperationError.UNDO }.operationError,
        )
        assertEquals(null, viewModel.state.value.selectedDate)
        stateJob.cancel()
    }

    @Test
    fun `opening rate period editor exposes the change rate flow`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList())
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.openChangeRate(null)
        advanceUntilIdle()

        assertTrue(viewModel.state.first { it.isChangeRateSheetOpen }.isChangeRateSheetOpen)

        viewModel.dismissChangeRateSheet()
        assertFalse(viewModel.state.first { !it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `successful bulk rate update closes the change rate sheet`() = runTest {
        val entry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val repository = FakeWorkEntryRepository(listOf(entry))
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.openChangeRate(null)
        advanceUntilIdle()

        viewModel.changeRateForPeriod(entry.date, entry.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        assertFalse(viewModel.state.first { !it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `failed bulk rate update keeps the change rate sheet open`() = runTest {
        val repository = FakeWorkEntryRepository(emptyList()).apply { bulkError = IllegalStateException() }
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }
        viewModel.openChangeRate(null)
        advanceUntilIdle()

        viewModel.changeRateForPeriod(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10), 20_000_000)
        advanceUntilIdle()

        assertEquals(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE), viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.isChangeRateSheetOpen }.isChangeRateSheetOpen)
        stateJob.cancel()
    }

    @Test
    fun `in flight old undo cannot clear or replace newer undo snapshot`() = runTest {
        val first = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000)
        val second = WorkEntry(LocalDate.of(2026, 8, 11), 480, 11_000_000)
        val repository = FakeWorkEntryRepository(listOf(first, second))
        val restoreStarted = CompletableDeferred<Unit>()
        val releaseRestore = CompletableDeferred<Unit>()
        repository.restoreStarted = restoreStarted
        repository.releaseRestore = releaseRestore
        val viewModel = CalendarViewModel(repository, FakeUserPreferencesRepository())
        viewModel.showMonth(YearMonth.of(2026, 8))
        val stateJob = launch { viewModel.state.collect() }
        viewModel.state.first { it.isReady }

        viewModel.changeRateForPeriod(first.date, first.date, 20_000_000)
        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        viewModel.undoLastOperation()
        restoreStarted.await()

        viewModel.changeRateForPeriod(second.date, second.date, 30_000_000)
        releaseRestore.complete(Unit)
        advanceUntilIdle()

        assertEquals(CalendarOperationEvent.Success.RATE_UPDATED, viewModel.operationEvents.first())
        assertTrue(viewModel.state.first { it.canUndo }.canUndo)
        viewModel.undoLastOperation()
        assertEquals(CalendarOperationEvent.Success.OPERATION_UNDONE, viewModel.operationEvents.first())
        assertEquals(listOf(second), repository.restoredEntries)
        stateJob.cancel()
    }
}

private class FakeWorkEntryRepository(initialEntries: List<WorkEntry>) : WorkEntryRepository {
    val entries = MutableStateFlow(initialEntries)
    val savedEntries = mutableListOf<WorkEntry>()
    var bulkOriginals = emptyList<WorkEntry>()
    var bulkError: Exception? = null
    var deleteError: Exception? = null
    var restoreError: Exception? = null
    var restoredEntries = emptyList<WorkEntry>()
    var restoreStarted: CompletableDeferred<Unit>? = null
    var releaseRestore: CompletableDeferred<Unit>? = null
    var bulkCalls = 0
    var saveStarted: CompletableDeferred<Unit>? = null
    var releaseFirstSave: CompletableDeferred<Unit>? = null
    var saveCalls = 0

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { it.date in startDate..endDate }.sortedBy(WorkEntry::date)
    }

    fun replaceEntries(updated: List<WorkEntry>) {
        entries.value = updated
    }

    override suspend fun getAll(): List<WorkEntry> = entries.value.sortedBy(WorkEntry::date)

    override suspend fun save(entry: WorkEntry) {
        saveCalls++
        if (saveCalls == 1) {
            saveStarted?.complete(Unit)
            releaseFirstSave?.await()
        }
        savedEntries += entry
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }

    override suspend fun delete(date: LocalDate) {
        deleteError?.let { throw it }
        entries.value = entries.value.filterNot { it.date == date }
    }

    override suspend fun restore(entries: List<WorkEntry>) {
        restoreError?.let { throw it }
        restoreStarted?.complete(Unit)
        releaseRestore?.await()
        restoredEntries = entries
        entries.forEach { entry -> save(entry) }
    }

    override suspend fun replaceAll(entries: List<WorkEntry>) {
        this.entries.value = entries
    }

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> {
        bulkCalls++
        bulkError?.let { throw it }
        val originals = entries.value.filter { it.date in startDate..endDate }
        bulkOriginals = originals
        entries.value = entries.value.map { entry ->
            if (entry.date in startDate..endDate) entry.copy(hourlyRateMicros = hourlyRateMicros) else entry
        }
        return originals
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _preferences = MutableStateFlow(UserPreferences())
    override val preferences: Flow<UserPreferences> = _preferences
    override val defaultRateInitialized: Flow<Boolean>
        get() = flowOf(initialized)
    val updates = mutableListOf<UserPreferences>()
    var initialized = false
    var adoptionError: Exception? = null

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) {
        val value = UserPreferences(defaultHourlyRateMicros, themeMode)
        updates += value
        initialized = defaultRateInitialized
        _preferences.value = value
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        val value = _preferences.value.copy(themeMode = themeMode)
        updates += value
        _preferences.value = value
    }

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) {
        val value = _preferences.value.copy(defaultHourlyRateMicros = defaultHourlyRateMicros)
        updates += value
        initialized = true
        _preferences.value = value
    }

    override suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean {
        adoptionError?.let { throw it }
        if (initialized) return false
        updateDefaultHourlyRate(defaultHourlyRateMicros)
        return true
    }
}
