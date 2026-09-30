package com.worktime.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.operation.DataMutationCoordinator
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
) : ViewModel() {
    private val visibleMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val changeRateSheetOpen = MutableStateFlow(false)
    private val changeRateInitialRange = MutableStateFlow<ClosedRange<LocalDate>?>(null)
    private val operationError = MutableStateFlow<CalendarOperationError?>(null)
    private val undoSnapshot = MutableStateFlow<UndoSnapshot?>(null)
    private var operationGeneration = 0L
    private val _operationEvents = Channel<CalendarOperationEvent>(Channel.BUFFERED)
    val operationEvents: Flow<CalendarOperationEvent> = _operationEvents.receiveAsFlow()

    /**
     * Keep the visible month and both neighbours under one Room observation. A horizontal
     * pager can therefore reveal either adjacent page immediately while the next window
     * subscription is being established after the page settles.
     */
    private val monthWindow = visibleMonth.flatMapLatest { center ->
        val start = center.minusMonths(1).atDay(1)
        val end = center.plusMonths(1).atEndOfMonth()
        workEntryRepository.observeDateRange(start, end).map { rows ->
            MonthWindow(
                center = center,
                entriesByMonth = rows
                    .groupBy { YearMonth.from(it.date) }
                    .mapValues { (_, entries) -> entries.associateBy(WorkEntry::date) },
            )
        }
    }

    private val visibleMonthEntries = combine(
        visibleMonth,
        monthWindow,
    ) { requestedMonth, window ->
        MonthUi(
            requestedMonth = requestedMonth,
            entries = window.entriesByMonth[requestedMonth].orEmpty(),
            entriesByMonth = window.entriesByMonth,
        )
    }

    private val baseState = combine(
        visibleMonthEntries,
        selectedDate,
        operationError,
    ) { monthUi, selected, error ->
        CalendarUiState(
            visibleMonth = monthUi.requestedMonth,
            entries = monthUi.entries,
            monthEntries = monthUi.entriesByMonth,
            selectedDate = selected,
            isReady = true,
            operationError = error,
        )
    }

    private data class ChangeRateUi(
        val open: Boolean,
        val initialRange: ClosedRange<LocalDate>?,
    )

    private val changeRateUi = combine(
        changeRateSheetOpen,
        changeRateInitialRange,
    ) { open, range -> ChangeRateUi(open, range) }

    private val overlayState = combine(
        changeRateUi,
        undoSnapshot,
    ) { changeRate, snapshot ->
        OverlayState(
            isChangeRateSheetOpen = changeRate.open,
            changeRateInitialRange = changeRate.initialRange,
            canUndo = snapshot != null,
        )
    }

    private data class OverlayState(
        val isChangeRateSheetOpen: Boolean,
        val changeRateInitialRange: ClosedRange<LocalDate>?,
        val canUndo: Boolean,
    )

    private val recentEntries = workEntryRepository
        .observeDateRange(LocalDate.now().minusDays(RECENT_HISTORY_DAYS), LocalDate.now())

    val state: StateFlow<CalendarUiState> = combine(
        baseState,
        overlayState,
        recentEntries,
    ) { base, overlay, recent ->
        base.copy(
            recentEntries = recent,
            isChangeRateSheetOpen = overlay.isChangeRateSheetOpen,
            changeRateInitialRange = overlay.changeRateInitialRange,
            canUndo = overlay.canUndo,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() = visibleMonth.update { it.minusMonths(1) }

    fun nextMonth() = visibleMonth.update { it.plusMonths(1) }

    fun showMonth(month: YearMonth) = visibleMonth.update { month }

    fun selectDate(date: LocalDate) {
        operationError.value = null
        selectedDate.value = date
    }

    fun dismissEditor() {
        operationError.value = null
        selectedDate.value = null
    }

    fun openChangeRate(range: ClosedRange<LocalDate>?) {
        operationError.value = null
        selectedDate.value = null
        changeRateInitialRange.value = range
        changeRateSheetOpen.value = true
    }

    suspend fun countEntries(startDate: LocalDate, endDate: LocalDate): Int =
        workEntryRepository.observeDateRange(startDate, endDate).first().size

    fun dismissChangeRateSheet() {
        operationError.value = null
        changeRateSheetOpen.value = false
        changeRateInitialRange.value = null
    }

    fun saveEntry(entry: WorkEntry) {
        runOperation(CalendarOperationError.SAVE_ENTRY, body = {
            workEntryRepository.save(entry)
            selectedDate.value = null
            try {
                adoptDefaultRateFrom(entry)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                reportOperationError(CalendarOperationError.DEFAULT_RATE_ADOPTION)
            }
        }, onSuccess = {
            _operationEvents.send(CalendarOperationEvent.Success.ENTRY_SAVED)
        })
    }

    // First ever entry sets the default rate; an existing default is never overwritten.
    private suspend fun adoptDefaultRateFrom(entry: WorkEntry) {
        if (entry.hourlyRateMicros <= 0L) return
        userPreferencesRepository.adoptDefaultHourlyRateIfUninitialized(entry.hourlyRateMicros)
    }

    fun deleteEntry(date: LocalDate) {
        val deletedEntry = state.value.entries[date]
        runOperation(CalendarOperationError.DELETE_ENTRY, body = {
            workEntryRepository.delete(date)
            selectedDate.value = null
        }, onSuccess = {
            if (deletedEntry != null) undoSnapshot.value = UndoSnapshot.Deleted(deletedEntry)
            _operationEvents.send(CalendarOperationEvent.Success.ENTRY_DELETED)
        })
    }

    fun changeRateForPeriod(startDate: LocalDate, endDate: LocalDate, newRateMicros: Long) {
        operationError.value = null
        if (startDate > endDate || newRateMicros <= 0L) {
            operationError.value = CalendarOperationError.BULK_RATE
            _operationEvents.trySend(CalendarOperationEvent.Error(CalendarOperationError.BULK_RATE))
            return
        }
        var originals: List<WorkEntry> = emptyList()
        runOperation(CalendarOperationError.BULK_RATE, body = {
            originals = workEntryRepository.updateHourlyRate(startDate, endDate, newRateMicros)
        }, onSuccess = {
            if (originals.isEmpty()) {
                _operationEvents.send(CalendarOperationEvent.Success.NO_OP)
            } else {
                undoSnapshot.value = UndoSnapshot.Bulk(originals)
                _operationEvents.send(CalendarOperationEvent.Success.RATE_UPDATED)
            }
            changeRateSheetOpen.value = false
            changeRateInitialRange.value = null
        })
    }

    fun undoLastOperation() {
        val snapshot = undoSnapshot.value ?: return
        runOperation(CalendarOperationError.UNDO, body = {
            when (snapshot) {
                is UndoSnapshot.Deleted -> workEntryRepository.restore(listOf(snapshot.entry))
                is UndoSnapshot.Bulk -> workEntryRepository.restore(snapshot.entries)
            }
        }, onSuccess = {
            undoSnapshot.value = null
            _operationEvents.send(CalendarOperationEvent.Success.OPERATION_UNDONE)
        }, invalidateUndo = false)
    }

    /** Invalidates stale calendar results and undo before an external full-data replacement. */
    fun prepareForExternalDataReplacement() {
        operationError.value = null
        supersedeOperation()
    }

    /**
     * Runs one repository operation: clears stale errors, invalidates prior undo state,
     * and reports failure as [errorKind] unless a newer operation superseded this one.
     * Undo opts out of invalidating: a failed undo must stay retryable.
     */
    private fun runOperation(
        errorKind: CalendarOperationError,
        body: suspend () -> Unit,
        onSuccess: suspend () -> Unit = {},
        invalidateUndo: Boolean = true,
    ) {
        operationError.value = null
        if (invalidateUndo) supersedeOperation()
        val generation = operationGeneration
        viewModelScope.launch {
            try {
                dataMutationCoordinator.run { body() }
                if (generation == operationGeneration) onSuccess()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (generation == operationGeneration) {
                    operationError.value = errorKind
                    _operationEvents.send(CalendarOperationEvent.Error(errorKind))
                }
            }
        }
    }

    fun reportOperationError(error: CalendarOperationError) {
        operationError.value = error
        _operationEvents.trySend(CalendarOperationEvent.Error(error))
    }

    private fun supersedeOperation() {
        operationGeneration++
        undoSnapshot.value = null
    }

    companion object {
        private const val RECENT_HISTORY_DAYS = 180L

        fun factory(
            workEntryRepository: WorkEntryRepository,
            userPreferencesRepository: UserPreferencesRepository,
            dataMutationCoordinator: DataMutationCoordinator = DataMutationCoordinator(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CalendarViewModel(
                    workEntryRepository = workEntryRepository,
                    userPreferencesRepository = userPreferencesRepository,
                    dataMutationCoordinator = dataMutationCoordinator,
                )
            }
        }
    }

    private sealed interface UndoSnapshot {
        data class Deleted(val entry: WorkEntry) : UndoSnapshot
        data class Bulk(val entries: List<WorkEntry>) : UndoSnapshot
    }
}

private data class MonthWindow(
    val center: YearMonth,
    val entriesByMonth: Map<YearMonth, Map<LocalDate, WorkEntry>>,
)

private data class MonthUi(
    val requestedMonth: YearMonth,
    val entries: Map<LocalDate, WorkEntry>,
    val entriesByMonth: Map<YearMonth, Map<LocalDate, WorkEntry>>,
)
