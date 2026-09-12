package com.worktime.app.modern

import com.worktime.app.modern.backup.BackupRatePeriod
import com.worktime.app.modern.backup.BackupSettings
import com.worktime.app.modern.backup.BackupWorkDay
import com.worktime.app.modern.backup.ModernBackupCodec
import com.worktime.app.modern.backup.ModernBackupPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModernBackupCodecTest {
    @Test
    fun `backup round trip preserves data`() {
        val payload = ModernBackupPayload(
            settings = BackupSettings(25_000, "RUB", "DARK"),
            workDays = listOf(BackupWorkDay(20_000, 480, 25_000, 1_000, 0, "test")),
            ratePeriods = listOf(BackupRatePeriod(1, 20_000, 20_030, 25_000)),
        )
        assertEquals(payload, ModernBackupCodec.decode(ModernBackupCodec.encode(payload)))
    }

    @Test
    fun `unsupported schema is rejected`() {
        val json = """{"schemaVersion":99,"settings":{"defaultRateMinor":0,"currencyCode":"RUB","themeMode":"SYSTEM"},"workDays":[],"ratePeriods":[]}"""
        assertThrows(IllegalArgumentException::class.java) { ModernBackupCodec.decode(json) }
    }
}
