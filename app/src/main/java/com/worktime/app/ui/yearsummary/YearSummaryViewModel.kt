package com.worktime.app.ui.yearsummary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worktime.app.domain.repository.WorkEntryRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal data class YearSummaryUiState(
    val selectedYear: Int,
    val summaries: Map<Int, YearSummary> = emptyMap(),
) {
    val summary: YearSummary?
        get() = summaries[selectedYear]
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class YearSummaryViewModel(
    private val workEntryRepository: WorkEntryRepository,
    private val savedStateHandle: SavedStateHandle,
    initialYear: Int,
) : ViewModel() {
    private val year = savedStateHandle.getStateFlow(KEY_YEAR, initialYear)

    private val summaries = year
        .flatMapLatest { selectedYear ->
            workEntryRepository.observeDateRange(
                LocalDate.of(selectedYear, 1, 1),
                LocalDate.of(selectedYear, 12, 31),
            ).map { entries ->
                mapOf(selectedYear to buildYearSummary(selectedYear, entries))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyMap(),
        )

    val state: StateFlow<YearSummaryUiState> = combine(
        year,
        summaries,
    ) { selectedYear, loadedSummaries ->
        YearSummaryUiState(
            selectedYear = selectedYear,
            summaries = loadedSummaries,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = YearSummaryUiState(selectedYear = year.value),
    )

    fun showYear(selectedYear: Int) {
        savedStateHandle[KEY_YEAR] = selectedYear
    }

    fun showPreviousYear() {
        showYear(year.value - 1)
    }

    fun showNextYear() {
        showYear(year.value + 1)
    }

    companion object {
        private const val KEY_YEAR = "year"

        fun factory(
            workEntryRepository: WorkEntryRepository,
            initialYear: Int,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                YearSummaryViewModel(
                    workEntryRepository = workEntryRepository,
                    savedStateHandle = createSavedStateHandle(),
                    initialYear = initialYear,
                )
            }
        }
    }
}
