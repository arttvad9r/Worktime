package com.worktime.app.ui.backup

import com.worktime.app.domain.backup.BackupDocumentSerializer
import com.worktime.app.domain.backup.BackupPayload
import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import com.worktime.app.domain.repository.UserPreferencesRepository
import com.worktime.app.domain.repository.WorkEntryRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BackupViewModelTest {
    @Test
    fun `export backup delegates domain payload to serializer and emits success`() = runTest {
        val entries = listOf(
            WorkEntry(LocalDate.of(2026, 7, 30), 300, 9_000_000L),
            WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L),
        )
        val workRepository = FakeWorkEntryRepository(entries)
        val preferences = UserPreferences(12_500_000L, ThemeMode.DARK)
        val preferencesRepository = FakeUserPreferencesRepository(
            initial = preferences,
            initialized = true,
        )
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val output = ByteArrayOutputStream()

        viewModel.exportBackup(output)
        assertEquals(BackupOperationEvent.Success.EXPORTED, viewModel.events.first())

        assertEquals("backup", output.toString("UTF-8"))
        assertEquals(
            BackupPayload(entries, preferences, defaultRateInitialized = true),
            serializer.lastEncodedBackup,
        )
    }

    @Test
    fun `export csv delegates entries to serializer and emits success`() = runTest {
        val entries = listOf(WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L))
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(
            FakeWorkEntryRepository(entries),
            FakeUserPreferencesRepository(),
            serializer,
        )
        val output = ByteArrayOutputStream()

        viewModel.exportCsv(output)
        assertEquals(BackupOperationEvent.Success.EXPORTED, viewModel.events.first())

        assertEquals("csv", output.toString("UTF-8"))
        assertEquals(entries, serializer.lastCsvEntries)
    }

    @Test
    fun `failed export reports export error`() = runTest {
        val viewModel = BackupViewModel(
            FakeWorkEntryRepository(emptyList()),
            FakeUserPreferencesRepository(),
            FakeBackupDocumentSerializer(),
        )
        val failingOutput = object : OutputStream() {
            override fun write(b: Int) = error("io failure")
        }

        viewModel.exportBackup(failingOutput)

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.EXPORT),
            viewModel.events.first(),
        )
        assertEquals(BackupOperationError.EXPORT, viewModel.state.first { it.error != null }.error)
    }

    @Test
    fun `import waits for confirmation then replaces entries and preferences`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository()
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(
            entries = listOf(importedEntry),
            preferences = UserPreferences(7_000_000L, ThemeMode.LIGHT),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        val preview = requireNotNull(viewModel.state.first { it.pendingImport != null }.pendingImport)
        assertEquals(1, preview.importedEntryCount)
        assertEquals(1, preview.currentEntryCount)
        assertEquals(importedEntry.date, preview.firstDate)
        assertEquals(importedEntry.date, preview.lastDate)
        assertEquals(0, workRepository.replaceAllCalls)

        viewModel.confirmImport()
        assertEquals(BackupOperationEvent.Success.IMPORTED, viewModel.events.first())
        assertEquals(listOf(importedEntry), workRepository.entries.value)
        assertEquals(UserPreferences(7_000_000L, ThemeMode.LIGHT), preferencesRepository.preferences.first())
        assertEquals(true, preferencesRepository.initialized)
        assertNull(viewModel.state.first { it.pendingImport?.importedEntryCount == null }.pendingImport?.importedEntryCount)
    }

    @Test
    fun `undo import restores previous entries and preferences`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val oldPreferences = UserPreferences(12_000_000L, ThemeMode.DARK)
        val preferencesRepository = FakeUserPreferencesRepository(initial = oldPreferences, initialized = true)
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(
            entries = listOf(importedEntry),
            preferences = UserPreferences(7_000_000L, ThemeMode.LIGHT),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()
        assertEquals(BackupOperationEvent.Success.IMPORTED, viewModel.events.first())

        viewModel.undoImport()
        assertEquals(BackupOperationEvent.Success.IMPORT_UNDONE, viewModel.events.first())
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(oldPreferences, preferencesRepository.preferences.first())
    }

    @Test
    fun `zero imported default rate keeps initialization flag`() = runTest {
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val preferencesRepository = FakeUserPreferencesRepository()
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(
            FakeWorkEntryRepository(emptyList()),
            preferencesRepository,
            serializer,
        )
        val payload = BackupPayload(
            entries = listOf(importedEntry),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()

        assertEquals(BackupOperationEvent.Success.IMPORTED, viewModel.events.first())
        assertEquals(0L, preferencesRepository.preferences.first().defaultHourlyRateMicros)
        assertEquals(true, preferencesRepository.initialized)
    }

    @Test
    fun `cancel import clears pending without replacing data`() = runTest {
        val workRepository = FakeWorkEntryRepository(emptyList())
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository(), serializer)
        val payload = BackupPayload(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.cancelImport()

        assertNull(viewModel.state.first { it.pendingImport?.importedEntryCount == null }.pendingImport?.importedEntryCount)
        assertEquals(0, workRepository.replaceAllCalls)
    }

    @Test
    fun `failed replace keeps pending import for retry`() = runTest {
        val workRepository = FakeWorkEntryRepository(emptyList()).apply {
            replaceAllFailuresRemaining = 1
        }
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository(), serializer)
        val payload = BackupPayload(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(1, viewModel.state.value.pendingImport?.importedEntryCount)
    }

    @Test
    fun `preference failure rolls entries back and keeps pending import`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            updateFailuresRemaining = 1
        }
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(listOf(importedEntry), UserPreferences(), true)

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImport?.importedEntryCount)
    }

    @Test
    fun `cancellation after replace rolls old state back without error event`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            cancelUpdatesRemaining = 1
        }
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(listOf(importedEntry), UserPreferences(), true)

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()
        assertEquals(
            Unit,
            withTimeoutOrNull(1_000) {
                while (workRepository.entries.value != listOf(oldEntry) || preferencesRepository.updates.isEmpty()) {
                    kotlinx.coroutines.yield()
                }
            },
        )

        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(UserPreferences(), preferencesRepository.preferences.first())
        assertEquals(false, preferencesRepository.initialized)
        assertEquals(1, viewModel.state.value.pendingImport?.importedEntryCount)
        assertNull(viewModel.state.value.error)
        assertNull(withTimeoutOrNull(50) { viewModel.events.first() })
    }

    @Test
    fun `rollback cancellation reports rollback error`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            cancelUpdatesRemaining = 2
        }
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(
            entries = listOf(WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT_ROLLBACK),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImport?.importedEntryCount)
    }

    @Test
    fun `rollback preference failure reports rollback error`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val importedEntry = WorkEntry(LocalDate.of(2026, 7, 1), 240, 8_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val preferencesRepository = FakeUserPreferencesRepository().apply {
            updateFailuresRemaining = 2
        }
        val serializer = FakeBackupDocumentSerializer()
        val viewModel = BackupViewModel(workRepository, preferencesRepository, serializer)
        val payload = BackupPayload(listOf(importedEntry), UserPreferences(), true)

        viewModel.importBackup(serializer.inputFor(payload))
        viewModel.state.first { it.pendingImport?.importedEntryCount != null }
        viewModel.confirmImport()

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT_ROLLBACK),
            viewModel.events.first(),
        )
        assertEquals(listOf(oldEntry), workRepository.entries.value)
        assertEquals(1, viewModel.state.value.pendingImport?.importedEntryCount)
    }

    @Test
    fun `malformed import reports import error without replacing data`() = runTest {
        val oldEntry = WorkEntry(LocalDate.of(2026, 8, 10), 480, 10_000_000L)
        val workRepository = FakeWorkEntryRepository(listOf(oldEntry))
        val serializer = FakeBackupDocumentSerializer().apply {
            decodeFailure = IllegalArgumentException("malformed")
        }
        val viewModel = BackupViewModel(workRepository, FakeUserPreferencesRepository(), serializer)

        viewModel.importBackup(ByteArrayInputStream("garbage".encodeToByteArray()))

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(0, workRepository.replaceAllCalls)
        assertNull(viewModel.state.value.pendingImport?.importedEntryCount)
    }

    @Test
    fun `oversized import is rejected before serializer decode`() = runTest {
        val serializer = FakeBackupDocumentSerializer().apply {
            maxBackupSizeBytes = 4
            decodedPayload = BackupPayload(emptyList(), UserPreferences(), true)
        }
        val viewModel = BackupViewModel(
            FakeWorkEntryRepository(emptyList()),
            FakeUserPreferencesRepository(),
            serializer,
        )

        viewModel.importBackup(ByteArrayInputStream("12345".encodeToByteArray()))

        assertEquals(
            BackupOperationEvent.Error(BackupOperationError.IMPORT),
            viewModel.events.first(),
        )
        assertEquals(0, serializer.decodeCalls)
    }
}

private class FakeBackupDocumentSerializer : BackupDocumentSerializer {
    override var maxBackupSizeBytes: Int = 1024
    var decodedPayload: BackupPayload? = null
    var decodeFailure: RuntimeException? = null
    var decodeCalls = 0
    var lastEncodedBackup: BackupPayload? = null
    var lastCsvEntries: List<WorkEntry>? = null

    override fun encodeBackup(
        entries: List<WorkEntry>,
        preferences: UserPreferences,
        defaultRateInitialized: Boolean,
    ): String {
        lastEncodedBackup = BackupPayload(entries, preferences, defaultRateInitialized)
        return "backup"
    }

    override fun decodeBackup(text: String): BackupPayload {
        decodeCalls += 1
        decodeFailure?.let { throw it }
        return requireNotNull(decodedPayload)
    }

    override fun encodeCsv(entries: List<WorkEntry>): String {
        lastCsvEntries = entries
        return "csv"
    }

    fun inputFor(payload: BackupPayload): ByteArrayInputStream {
        decodedPayload = payload
        return ByteArrayInputStream("backup".encodeToByteArray())
    }
}

private class FakeWorkEntryRepository(initial: List<WorkEntry>) : WorkEntryRepository {
    val entries = MutableStateFlow(initial)
    var replaceAllCalls = 0
    var replaceAllFailuresRemaining = 0

    override fun observeMonth(month: YearMonth): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { YearMonth.from(it.date) == month }
    }

    override fun observeDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<WorkEntry>> = entries.map { list ->
        list.filter { it.date in startDate..endDate }
    }

    override suspend fun getAll(): List<WorkEntry> = entries.value

    override suspend fun save(entry: WorkEntry) {
        entries.value = entries.value.filterNot { it.date == entry.date } + entry
    }

    override suspend fun delete(date: LocalDate) {
        entries.value = entries.value.filterNot { it.date == date }
    }

    override suspend fun restore(entries: List<WorkEntry>) {
        this.entries.value = this.entries.value.filterNot { old -> entries.any { it.date == old.date } } + entries
    }

    override suspend fun replaceAll(entries: List<WorkEntry>) {
        replaceAllCalls += 1
        if (replaceAllFailuresRemaining > 0) {
            replaceAllFailuresRemaining -= 1
            throw IllegalStateException("database")
        }
        this.entries.value = entries
    }

    override suspend fun updateHourlyRate(
        startDate: LocalDate,
        endDate: LocalDate,
        hourlyRateMicros: Long,
    ): List<WorkEntry> = emptyList()
}

private class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
    initialized: Boolean = false,
) : UserPreferencesRepository {
    private val values = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = values
    override val defaultRateInitialized: Flow<Boolean>
        get() = flowOf(this.initialized)
    var initialized = initialized
    var updateFailuresRemaining = 0
    var cancelUpdatesRemaining = 0
    val updates = mutableListOf<UserPreferences>()

    override suspend fun update(
        defaultHourlyRateMicros: Long,
        themeMode: ThemeMode,
        defaultRateInitialized: Boolean,
    ) {
        if (cancelUpdatesRemaining > 0) {
            cancelUpdatesRemaining -= 1
            throw CancellationException("cancelled")
        }
        if (updateFailuresRemaining > 0) {
            updateFailuresRemaining -= 1
            throw IllegalStateException("datastore")
        }
        val updated = UserPreferences(defaultHourlyRateMicros, themeMode)
        values.value = updated
        initialized = defaultRateInitialized
        updates += updated
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        values.value = values.value.copy(themeMode = themeMode)
    }

    override suspend fun updateDefaultHourlyRate(defaultHourlyRateMicros: Long) {
        values.value = values.value.copy(defaultHourlyRateMicros = defaultHourlyRateMicros)
        initialized = true
    }

    override suspend fun adoptDefaultHourlyRateIfUninitialized(defaultHourlyRateMicros: Long): Boolean = false
}
