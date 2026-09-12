package com.worktime.app.modern.backup

import com.worktime.app.modern.model.ThemeMode
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
    private const val SCHEMA_VERSION = 1
    private const val MAX_MONEY_MINOR = 100_000_000_000L
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = false
        explicitNulls = true
    }

    fun encode(payload: ModernBackupPayload): String {
        validate(payload)
        return json.encodeToString(ModernBackupPayload.serializer(), payload)
    }

    fun decode(text: String): ModernBackupPayload {
        val payload = json.decodeFromString(ModernBackupPayload.serializer(), text)
        validate(payload)
        return payload
    }

    fun preview(payload: ModernBackupPayload): BackupPreview = BackupPreview(
        workDayCount = payload.workDays.size,
        ratePeriodCount = payload.ratePeriods.size,
        firstEpochDay = payload.workDays.minOfOrNull { it.epochDay },
        lastEpochDay = payload.workDays.maxOfOrNull { it.epochDay },
    )

    private fun validate(payload: ModernBackupPayload) {
        require(payload.schemaVersion == SCHEMA_VERSION) { "Unsupported schemaVersion=${payload.schemaVersion}" }
        require(payload.settings.defaultRateMinor in 0..MAX_MONEY_MINOR)
        require(payload.settings.currencyCode.matches(Regex("[A-Z]{3}")))
        ThemeMode.valueOf(payload.settings.themeMode)
        require(payload.workDays.map { it.epochDay }.distinct().size == payload.workDays.size) {
            "Duplicate work day"
        }
        payload.workDays.forEach { day ->
            require(day.workedMinutes in 0..1440)
            require(day.hourlyRateMinor in 0..MAX_MONEY_MINOR)
            require(day.bonusMinor in 0..MAX_MONEY_MINOR)
            require(day.penaltyMinor in 0..MAX_MONEY_MINOR)
            require(day.note.length <= 2000)
        }
        payload.ratePeriods.forEach { period ->
            require(period.hourlyRateMinor in 0..MAX_MONEY_MINOR)
            require(period.endEpochDay == null || period.endEpochDay >= period.startEpochDay)
        }
    }
}
