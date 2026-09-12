package com.worktime.app.modern

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.worktime.app.modern.backup.ModernBackupCodec
import com.worktime.app.modern.data.ModernDatabase
import com.worktime.app.modern.data.ModernRepository
import com.worktime.app.modern.model.WorkDay
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModernRepositoryInstrumentedTest {
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
    fun rateRangeUpdatesExistingDaysAndFutureEffectiveRate() = runBlocking {
        val first = LocalDate.of(2026, 9, 1)
        val second = LocalDate.of(2026, 9, 2)
        repository.saveDay(WorkDay(first, 480, 20_000))
        repository.saveDay(WorkDay(second, 600, 20_000))

        val changed = repository.applyRate(first, second, 25_000)

        assertEquals(2, changed)
        assertEquals(25_000L, repository.getDay(first)?.hourlyRateMinor)
        assertEquals(25_000L, repository.getDay(second)?.hourlyRateMinor)
        assertEquals(25_000L, repository.effectiveRate(LocalDate.of(2026, 9, 2)))
    }

    @Test
    fun latestOverlappingRatePeriodWinsForNewDays() = runBlocking {
        val september = LocalDate.of(2026, 9, 1)
        repository.applyRate(september, LocalDate.of(2026, 9, 30), 25_000)
        repository.applyRate(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 20), 30_000)

        assertEquals(25_000L, repository.effectiveRate(LocalDate.of(2026, 9, 10)))
        assertEquals(30_000L, repository.effectiveRate(LocalDate.of(2026, 9, 16)))
        assertEquals(25_000L, repository.effectiveRate(LocalDate.of(2026, 9, 25)))
    }

    @Test
    fun changingDefaultRateDoesNotRewriteStoredShiftSnapshot() = runBlocking {
        val storedDate = LocalDate.of(2026, 10, 5)
        val futureDate = LocalDate.of(2026, 10, 6)
        repository.saveDay(WorkDay(storedDate, 480, 25_000))

        repository.updateSettings { it.copy(defaultRateMinor = 50_000) }

        assertEquals(25_000L, repository.getDay(storedDate)?.hourlyRateMinor)
        assertEquals(25_000L, repository.effectiveRate(storedDate))
        assertEquals(50_000L, repository.effectiveRate(futureDate))
    }

    @Test
    fun explicitBulkRateChangeRewritesStoredSnapshotsOnlyInsideRange() = runBlocking {
        val inside = LocalDate.of(2026, 11, 10)
        val outside = LocalDate.of(2026, 11, 21)
        repository.saveDay(WorkDay(inside, 480, 25_000))
        repository.saveDay(WorkDay(outside, 480, 25_000))

        repository.applyRate(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), 35_000)

        assertEquals(35_000L, repository.getDay(inside)?.hourlyRateMinor)
        assertEquals(25_000L, repository.getDay(outside)?.hourlyRateMinor)
    }

    @Test
    fun backupRestoreRoundTripReplacesDatabaseAtomically() = runBlocking {
        val date = LocalDate.of(2026, 9, 12)
        repository.saveDay(WorkDay(date, 510, 30_000, bonusMinor = 5_000, penaltyMinor = 1_000, note = "смена"))
        val encoded = ModernBackupCodec.encode(repository.createBackup())
        val payload = ModernBackupCodec.decode(encoded)

        repository.deleteDay(date)
        assertNull(repository.getDay(date))

        repository.restoreBackup(payload)
        val restored = repository.getDay(date)
        assertEquals(510, restored?.workedMinutes)
        assertEquals(30_000L, restored?.hourlyRateMinor)
        assertEquals(5_000L, restored?.bonusMinor)
        assertEquals(1_000L, restored?.penaltyMinor)
        assertEquals("смена", restored?.note)
    }
}
