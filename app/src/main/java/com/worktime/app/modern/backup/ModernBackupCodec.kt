package com.worktime.app.modern.backup

import com.worktime.app.modern.model.MoneyRules
import com.worktime.app.modern.model.ThemeMode
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.util.Currency
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupSettings(
    val defaultRateMinor: Long,
    val currencyCode: String,
    val themeMode: String,
)

@Serializable
data class BackupWorkDay(
    val epochDay: Long,
    val workedMinutes: Int,
    val hourlyRateMinor: Long,
    val bonusMinor: Long,
    val penaltyMinor: Long,
    val otherMinor: Long = 0L,
    val note: String,
)

@Serializable
data class BackupRatePeriod(
    val id: Long,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val hourlyRateMinor: Long,
)

@Serializable
data class ModernBackupPayload(
    val schemaVersion: Int = 1,
    val settings: BackupSettings,
    val workDays: List<BackupWorkDay>,
    val ratePeriods: List<BackupRatePeriod>,
)

data class BackupPreview(
    val workDayCount: Int,
    val ratePeriodCount: Int,
    val firstEpochDay: Long?,
    val lastEpochDay: Long?,
)

object ModernBackupCodec {
    const val MAX_BACKUP_SIZE_BYTES = 4 * 1024 * 1024

    private const val SCHEMA_VERSION = 1
    private const val MAX_WORK_DAYS = 100_000
    private const val MAX_RATE_PERIODS = 100_000
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun encode(payload: ModernBackupPayload): String {
        validate(payload)
        val encoded = json.encodeToString(ModernBackupPayload.serializer(), payload)
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_SIZE_BYTES) {
            "Backup is too large"
        }
        return encoded
    }

    fun decode(text: String): ModernBackupPayload {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_SIZE_BYTES) {
            "Backup is too large"
        }
        val payload = json.decodeFromString(ModernBackupPayload.serializer(), text)
        validate(payload)
        return payload
    }

    fun readUtf8Limited(input: InputStream): String {
        val output = ByteArrayOutputStream(minOf(MAX_BACKUP_SIZE_BYTES, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_BACKUP_SIZE_BYTES) { "Backup is too large" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray().toString(Charsets.UTF_8)
    }

    fun preview(payload: ModernBackupPayload): BackupPreview = BackupPreview(
        workDayCount = payload.workDays.size,
        ratePeriodCount = payload.ratePeriods.size,
        firstEpochDay = payload.workDays.minOfOrNull { it.epochDay },
        lastEpochDay = payload.workDays.maxOfOrNull { it.epochDay },
    )

    private fun validate(payload: ModernBackupPayload) {
        require(payload.schemaVersion == SCHEMA_VERSION) { "Unsupported schemaVersion=${payload.schemaVersion}" }
        require(MoneyRules.isValid(payload.settings.defaultRateMinor))
        require(isCurrencyCodeValid(payload.settings.currencyCode))
        ThemeMode.valueOf(payload.settings.themeMode)

        require(payload.workDays.size <= MAX_WORK_DAYS) { "Too many work days" }
        require(payload.ratePeriods.size <= MAX_RATE_PERIODS) { "Too many rate periods" }
        require(payload.workDays.map { it.epochDay }.distinct().size == payload.workDays.size) {
            "Duplicate work day"
        }
        require(payload.ratePeriods.map { it.id }.distinct().size == payload.ratePeriods.size) {
            "Duplicate rate period id"
        }

        payload.workDays.forEach { day ->
            validateEpochDay(day.epochDay)
            require(day.workedMinutes in 0..1440)
            require(MoneyRules.isValid(day.hourlyRateMinor))
            require(MoneyRules.isValid(day.bonusMinor))
            require(MoneyRules.isValid(day.penaltyMinor))
            require(MoneyRules.isSignedAdjustmentValid(day.otherMinor))
            require(day.note.length <= 2_000)
        }
        payload.ratePeriods.forEach { period ->
            require(period.id > 0L)
            validateEpochDay(period.startEpochDay)
            period.endEpochDay?.let(::validateEpochDay)
            require(MoneyRules.isValid(period.hourlyRateMinor))
            require(period.endEpochDay == null || period.endEpochDay >= period.startEpochDay)
        }
    }

    private fun isCurrencyCodeValid(code: String): Boolean =
        code.matches(Regex("[A-Z]{3}")) && runCatching { Currency.getInstance(code) }.isSuccess

    private fun validateEpochDay(epochDay: Long) {
        runCatching { LocalDate.ofEpochDay(epochDay) }
            .getOrElse { throw IllegalArgumentException("Invalid epochDay=$epochDay", it) }
    }
}
