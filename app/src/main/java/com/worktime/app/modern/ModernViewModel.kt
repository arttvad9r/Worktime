package com.worktime.app.modern

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.worktime.app.modern.backup.BackupPreview
import com.worktime.app.modern.backup.ModernBackupCodec
import com.worktime.app.modern.backup.ModernBackupPayload
import com.worktime.app.modern.data.ModernRepository
import com.worktime.app.modern.model.AppSettings
import com.worktime.app.modern.model.ThemeMode
import com.worktime.app.modern.model.WorkDay
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ModernViewModel(private val repository: ModernRepository) : ViewModel() {
    data class DayEditorState(
        val date: LocalDate,
        val workedMinutes: Int,
        val rateMinor: Long,
        val bonusMinor: Long,
        val penaltyMinor: Long,
        val otherMinor: Long,
        val note: String,
        val exists: Boolean,
    )

    data class ImportState(
        val payload: ModernBackupPayload,
        val preview: BackupPreview,
    )

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Year.now().value)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val monthDays: StateFlow<List<WorkDay>> = _selectedMonth.flatMapLatest { month ->
        repository.observeRange(month.atDay(1), month.atEndOfMonth())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val yearDays: StateFlow<List<WorkDay>> = _selectedYear.flatMapLatest { year ->
        repository.observeRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _editor = MutableStateFlow<DayEditorState?>(null)
    val editor: StateFlow<DayEditorState?> = _editor.asStateFlow()

    private val _pendingImport = MutableStateFlow<ImportState?>(null)
    val pendingImport: StateFlow<ImportState?> = _pendingImport.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun previousMonth() = selectMonth(_selectedMonth.value.minusMonths(1))
    fun nextMonth() = selectMonth(_selectedMonth.value.plusMonths(1))
    fun currentMonth() = selectMonth(YearMonth.now())

    fun selectMonth(month: YearMonth) {
        _selectedMonth.value = month
        _selectedYear.value = month.year
    }

    fun previousYear() { _selectedYear.value -= 1 }
    fun nextYear() { _selectedYear.value += 1 }

    fun openDay(date: LocalDate) {
        viewModelScope.launch {
            runCatching {
                val existing = repository.getDay(date)
                val rate = existing?.hourlyRateMinor ?: repository.effectiveRate(date)
                DayEditorState(
                    date = date,
                    workedMinutes = existing?.workedMinutes ?: 0,
                    rateMinor = rate,
                    bonusMinor = existing?.bonusMinor ?: 0L,
                    penaltyMinor = existing?.penaltyMinor ?: 0L,
                    otherMinor = existing?.otherMinor ?: 0L,
                    note = existing?.note.orEmpty(),
                    exists = existing != null,
                )
            }.onSuccess { _editor.value = it }
                .onFailure { reportError(it.message ?: "Не удалось открыть день") }
        }
    }

    fun closeEditor() { _editor.value = null }

    fun saveDay(
        workedMinutes: Int,
        rateMinor: Long,
        bonusMinor: Long,
        penaltyMinor: Long,
        otherMinor: Long,
        note: String,
    ) {
        val current = _editor.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.saveDay(
                    WorkDay(
                        date = current.date,
                        workedMinutes = workedMinutes,
                        hourlyRateMinor = rateMinor,
                        bonusMinor = bonusMinor,
                        penaltyMinor = penaltyMinor,
                        otherMinor = otherMinor,
                        note = note.trim(),
                    ),
                )
            }.onSuccess { _editor.value = null }
                .onFailure { reportError(it.message ?: "Не удалось сохранить день") }
        }
    }

    fun deleteCurrentDay() {
        val current = _editor.value ?: return
        viewModelScope.launch {
            runCatching { repository.deleteDay(current.date) }
                .onSuccess { _editor.value = null }
                .onFailure { reportError(it.message ?: "Не удалось удалить день") }
        }
    }

    fun setDefaultRate(rateMinor: Long) {
        viewModelScope.launch {
            runCatching { repository.updateSettings { it.copy(defaultRateMinor = rateMinor) } }
                .onFailure { reportError(it.message ?: "Не удалось сохранить ставку") }
        }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch {
            runCatching { repository.updateSettings { it.copy(currencyCode = code) } }
                .onFailure { reportError(it.message ?: "Не удалось изменить валюту") }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            runCatching { repository.updateSettings { it.copy(themeMode = mode) } }
                .onFailure { reportError(it.message ?: "Не удалось изменить тему") }
        }
    }

    fun applyRateToMonth(rateMinor: Long) {
        val month = _selectedMonth.value
        applyRate(month.atDay(1), month.atEndOfMonth(), rateMinor)
    }

    fun applyRate(start: LocalDate, endInclusive: LocalDate?, rateMinor: Long) {
        viewModelScope.launch {
            runCatching { repository.applyRate(start, endInclusive, rateMinor) }
                .onFailure { reportError(it.message ?: "Не удалось изменить ставку") }
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { ModernBackupCodec.encode(repository.createBackup()) }
                .onSuccess(onReady)
                .onFailure { reportError(it.message ?: "Не удалось создать резервную копию") }
        }
    }

    fun stageImport(text: String) {
        runCatching { ModernBackupCodec.decode(text) }
            .onSuccess { payload ->
                _pendingImport.value = ImportState(payload, ModernBackupCodec.preview(payload))
            }
            .onFailure { reportError(it.message ?: "Некорректный файл резервной копии") }
    }

    fun cancelImport() { _pendingImport.value = null }

    fun confirmImport() {
        val staged = _pendingImport.value ?: return
        viewModelScope.launch {
            runCatching { repository.restoreBackup(staged.payload) }
                .onSuccess { _pendingImport.value = null }
                .onFailure { reportError(it.message ?: "Не удалось восстановить данные") }
        }
    }

    fun reportError(message: String) { _lastError.value = message }
    fun consumeError() { _lastError.value = null }

    class Factory(private val repository: ModernRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ModernViewModel::class.java))
            return ModernViewModel(repository) as T
        }
    }
}
