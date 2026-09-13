package com.worktime.app.modern

import com.worktime.app.modern.backup.BackupRatePeriod
import com.worktime.app.modern.backup.BackupSettings
import com.worktime.app.modern.backup.BackupWorkDay
import com.worktime.app.modern.backup.ModernBackupCodec
import com.worktime.app.modern.backup.ModernBackupPayload
import java.io.ByteArrayInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ModernBackupCodecTest {
    @Test
    fun `backup round trip preserves signed other adjustment`() {
        val payload = ModernBackupPayload(
            settings = BackupSettings(25_000, "RUB", "DARK"),
            workDays = listOf(
                BackupWorkDay(
                    epochDay = 20_000,
                    workedMinutes = 480,
                    hourlyRateMinor = 25_000,
                    bonusMinor = 1_000,
                    penaltyMinor = 0,
                    otherMinor = -750,
                    note = "test",
                ),
            ),
            ratePeriods = listOf(BackupRatePeriod(1, 20_000, 20_030, 25_000)),
        )
        assertEquals(payload, ModernBackupCodec.decode(ModernBackupCodec.encode(payload)))
    }

    @Test
    fun `legacy schema one backup without other adjustment stays compatible`() {
        val json = """
            {
              "schemaVersion":1,
              "settings":{"defaultRateMinor":25000,"currencyCode":"RUB","themeMode":"SYSTEM"},
              "workDays":[{
                "epochDay":20000,
                "workedMinutes":480,
                "hourlyRateMinor":25000,
                "bonusMinor":1000,
                "penaltyMinor":0,
                "note":"legacy"
              }],
              "ratePeriods":[]
            }
        """.trimIndent()

        val decoded = ModernBackupCodec.decode(json)
        assertEquals(0L, decoded.workDays.single().otherMinor)
        assertEquals("legacy", decoded.workDays.single().note)
    }

    @Test
    fun `unsupported schema is rejected`() {
        val json = """{"schemaVersion":99,"settings":{"defaultRateMinor":0,"currencyCode":"RUB","themeMode":"SYSTEM"},"workDays":[],"ratePeriods":[]}"""
        assertThrows(IllegalArgumentException::class.java) { ModernBackupCodec.decode(json) }
    }

    @Test
    fun `unknown currency code is rejected`() {
        val json = """{"schemaVersion":1,"settings":{"defaultRateMinor":0,"currencyCode":"ZZZ","themeMode":"SYSTEM"},"workDays":[],"ratePeriods":[]}"""
        assertThrows(IllegalArgumentException::class.java) { ModernBackupCodec.decode(json) }
    }

    @Test
    fun `oversized backup stream is rejected before decode`() {
        val oversized = ByteArray(ModernBackupCodec.MAX_BACKUP_SIZE_BYTES + 1) { 'x'.code.toByte() }

        assertThrows(IllegalArgumentException::class.java) {
            ByteArrayInputStream(oversized).use(ModernBackupCodec::readUtf8Limited)
        }
    }

    @Test
    fun `bounded backup stream preserves utf8 text`() {
        val expected = "{\"message\":\"смена\"}"
        val input = ByteArrayInputStream(expected.toByteArray(Charsets.UTF_8))

        assertEquals(expected, input.use(ModernBackupCodec::readUtf8Limited))
    }
}
