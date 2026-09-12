package com.worktime.app.modern

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.modern.backup.BackupWorkDay
import com.worktime.app.modern.data.ModernDatabase
import com.worktime.app.modern.data.ModernRepository
import com.worktime.app.modern.model.WorkDay
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernRateBoundaryInstrumentedTest {
    private lateinit var database: ModernDatabase
    private lateinit var repository: ModernRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ModernDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ModernRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun boundedRateChangeIsInclusiveAcrossMonthAndYearBoundary() = runBlocking {
        val dec31 = LocalDate.of(2026, 12, 31)
        val jan1 = LocalDate.of(2027, 1, 1)
        val jan2 = LocalDate.of(2027, 1, 2)
        repository.saveDay(WorkDay(dec31, 480, 20_000))
        repository.saveDay(WorkDay(jan1, 480, 20_000))
        repository.saveDay(WorkDay(jan2, 480, 20_000))

        val changed = repository.applyRate(dec31, jan1, 35_000)

        assertEquals(2, changed)
        assertEquals(35_000L, repository.getDay(dec31)?.hourlyRateMinor)
        assertEquals(35_000L, repository.getDay(jan1)?.hourlyRateMinor)
        assertEquals(20_000L, repository.getDay(jan2)?.hourlyRateMinor)
    }

    @Test
    fun emptyFutureRangeChangesEffectiveRateWithoutCreatingWorkRows() = runBlocking {
        val start = LocalDate.of(2028, 2, 1)
        val end = LocalDate.of(2028, 2, 29)
        repository.updateSettings { it.copy(defaultRateMinor = 10_000) }

        val changed = repository.applyRate(start, end, 25_000)

        assertEquals(0, changed)
        assertEquals(0, repository.countDays(start, end))
        assertEquals(25_000L, repository.effectiveRate(LocalDate.of(2028, 2, 14)))
        assertEquals(10_000L, repository.effectiveRate(LocalDate.of(2028, 3, 1)))
    }

    @Test
    fun openEndedRateChangeUpdatesExistingFutureRowsAndNewDates() = runBlocking {
        val start = LocalDate.of(2026, 11, 15)
        val before = LocalDate.of(2026, 11, 14)
        val nextYear = LocalDate.of(2027, 4, 2)
        repository.saveDay(WorkDay(before, 480, 20_000))
        repository.saveDay(WorkDay(start, 480, 20_000))
        repository.saveDay(WorkDay(nextYear, 480, 20_000))

        val changed = repository.applyRate(start, null, 30_000)

        assertEquals(2, changed)
        assertEquals(20_000L, repository.getDay(before)?.hourlyRateMinor)
        assertEquals(30_000L, repository.getDay(start)?.hourlyRateMinor)
        assertEquals(30_000L, repository.getDay(nextYear)?.hourlyRateMinor)
        assertEquals(30_000L, repository.effectiveRate(LocalDate.of(2030, 1, 1)))
    }

    @Test
    fun rejectedRestoreLeavesExistingDatabaseUntouched() = runBlocking {
        val date = LocalDate.of(2026, 9, 12)
        repository.saveDay(WorkDay(date, 480, 20_000, note = "keep"))
        val original = repository.createBackup()
        val invalid = original.copy(
            workDays = original.workDays + BackupWorkDay(
                epochDay = LocalDate.of(2026, 9, 13).toEpochDay(),
                workedMinutes = 1_441,
                hourlyRateMinor = 20_000,
                bonusMinor = 0,
                penaltyMinor = 0,
                otherMinor = 0,
                note = "invalid",
            ),
        )

        val result = runCatching { repository.restoreBackup(invalid) }

        assertTrue(result.isFailure)
        val stillThere = repository.getDay(date)
        assertEquals(480, stillThere?.workedMinutes)
        assertEquals(20_000L, stillThere?.hourlyRateMinor)
        assertEquals("keep", stillThere?.note)
        assertEquals(1, repository.countDays(date, date))
    }
}
