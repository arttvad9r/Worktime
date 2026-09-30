package com.worktime.app.data.backup

import com.worktime.app.domain.model.WorkEntry
import com.worktime.app.domain.preferences.ThemeMode
import com.worktime.app.domain.preferences.UserPreferences
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BackupCodecTest {
    private val entries = listOf(
        WorkEntry(LocalDate.of(2026, 8, 9), 480, 10_000_000),
        WorkEntry(
            date = LocalDate.of(2026, 8, 10),
            workedMinutes = 420,
            hourlyRateMicros = 11_000_000,
            bonusMicros = 2_000_000,
            penaltyMicros = 500_000,
            note = "double shift",
        ),
    )
    private val preferences = UserPreferences(
        defaultHourlyRateMicros = 12_500_000,
        themeMode = ThemeMode.DARK,
    )

    @Test
    fun `encode decode round trip preserves entries preferences and initialization`() {
        val text = BackupCodec.encode(entries, preferences, defaultRateInitialized = true)

        val restored = BackupCodec.decode(text)

        assertEquals(entries, restored.entries)
        assertEquals(preferences, restored.preferences)
        assertTrue(restored.defaultRateInitialized)
    }

    @Test
    fun `round trip preserves cross year boundary entries and fractional rates`() {
        val boundaryEntries = listOf(
            WorkEntry(
                date = LocalDate.of(2025, 12, 31),
                workedMinutes = 1,
                hourlyRateMicros = 123_450_000L,
                bonusMicros = 5_250_000L,
            ),
            WorkEntry(
                date = LocalDate.of(2026, 1, 1),
                workedMinutes = 24 * 60,
                hourlyRateMicros = 370_125_000L,
                penaltyMicros = 750_000L,
            ),
        )

        val restored = BackupCodec.decode(
            BackupCodec.encode(boundaryEntries, preferences, defaultRateInitialized = true),
        )

        assertEquals(boundaryEntries, restored.entries)
        assertEquals(preferences, restored.preferences)
        assertTrue(restored.defaultRateInitialized)
    }

    @Test
    fun `round trip preserves explicitly initialized zero default rate`() {
        val text = BackupCodec.encode(
            entries = emptyList(),
            preferences = UserPreferences(),
            defaultRateInitialized = true,
        )

        val restored = BackupCodec.decode(text)

        assertEquals(0L, restored.preferences.defaultHourlyRateMicros)
        assertTrue(restored.defaultRateInitialized)
    }

    @Test
    fun `decode accepts an empty entry list`() {
        val text = BackupCodec.encode(emptyList(), preferences)

        val restored = BackupCodec.decode(text)

        assertEquals(emptyList<WorkEntry>(), restored.entries)
        assertEquals(preferences, restored.preferences)
    }

    @Test
    fun `legacy clean backup stays uninitialized`() {
        val text = """{"version":1,"preferences":{"defaultHourlyRateMicros":0,"themeMode":"SYSTEM"},"entries":[]}"""

        val restored = BackupCodec.decode(text)

        assertFalse(restored.defaultRateInitialized)
    }

    @Test
    fun `legacy backup with worked entries is treated as initialized`() {
        val text = """{"version":1,"preferences":{"defaultHourlyRateMicros":0,"themeMode":"SYSTEM"},"entries":[{"date":"2026-08-09","workedMinutes":480,"hourlyRateMicros":10000000,"bonusMicros":0,"penaltyMicros":0}]}"""

        val restored = BackupCodec.decode(text)

        assertTrue(restored.defaultRateInitialized)
    }

    @Test
    fun `decode rejects malformed json`() {
        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode("not json at all")
        }
    }

    @Test
    fun `decode rejects unsupported version`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"version\":2", "\"version\":99")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects an unparsable entry date`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"date\":\"2026-08-09\"", "\"date\":\"09.08.2026\"")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects entry values violating domain limits`() {
        val text = BackupCodec.encode(entries, preferences)
            .replaceFirst("\"workedMinutes\":480", "\"workedMinutes\":1500")

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects duplicate dates`() {
        val text = BackupCodec.encode(entries + entries.first(), preferences)

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }

    @Test
    fun `decode rejects oversized payload`() {
        val text = "{" + "x".repeat(BackupCodec.MAX_BACKUP_SIZE_BYTES) + "}"

        assertThrows(IllegalArgumentException::class.java) {
            BackupCodec.decode(text)
        }
    }
}
