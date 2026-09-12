package com.worktime.app.modern.data

import androidx.room.withTransaction
import com.worktime.app.modern.backup.BackupRatePeriod
import com.worktime.app.modern.backup.BackupSettings
import com.worktime.app.modern.backup.BackupWorkDay
import com.worktime.app.modern.backup.ModernBackupPayload
import com.worktime.app.modern.model.AppSettings
import com.worktime.app.modern.model.ThemeMode
import com.worktime.app.modern.model.WorkDay
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModernRepository(private val database: ModernDatabase) {
    private val workDays = database.workDayDao()
    private val rates = database.ratePeriodDao()
    private val settingsDao = database.settingsDao()

    fun settings(): Flow<AppSettings> = settingsDao.observe().map { entity ->
        entity?.toModel() ?: AppSettings()
    }

    fun observeRange(start: LocalDate, endInclusive: LocalDate): Flow<List<WorkDay>> =
        workDays.observeBetween(start.toEpochDay(), endInclusive.toEpochDay()).map { list ->
            list.map { it.toModel() }
        }

    suspend fun getDay(date: LocalDate): WorkDay? = workDays.get(date.toEpochDay())?.toModel()

    suspend fun effectiveRate(date: LocalDate): Long {
        val existing = workDays.get(date.toEpochDay())
        if (existing != null) return existing.hourlyRateMinor
        val override = rates.effectiveFor(date.toEpochDay())
        if (override != null) return override.hourlyRateMinor
        return settingsDao.get()?.defaultRateMinor ?: 0L
    }

    suspend fun saveDay(day: WorkDay) {
        require(day.workedMinutes in 0..1440)
        require(day.hourlyRateMinor >= 0L)
        require(day.bonusMinor >= 0L)
        require(day.penaltyMinor >= 0L)
        if (day.workedMinutes == 0 && day.bonusMinor == 0L && day.penaltyMinor == 0L && day.note.isBlank()) {
            workDays.delete(day.date.toEpochDay())
        } else {
            workDays.upsert(day.toEntity())
        }
    }

    suspend fun deleteDay(date: LocalDate) = workDays.delete(date.toEpochDay())

    suspend fun applyRate(start: LocalDate, endInclusive: LocalDate?, rateMinor: Long): Int {
        require(rateMinor >= 0L)
        require(endInclusive == null || !endInclusive.isBefore(start))
        return database.withTransaction {
            rates.insert(
                RatePeriodEntity(
                    startEpochDay = start.toEpochDay(),
                    endEpochDay = endInclusive?.toEpochDay(),
                    hourlyRateMinor = rateMinor,
                ),
            )
            if (endInclusive == null) {
                workDays.updateRateFrom(start.toEpochDay(), rateMinor)
            } else {
                workDays.updateRateInRange(start.toEpochDay(), endInclusive.toEpochDay(), rateMinor)
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = settingsDao.get()?.toModel() ?: AppSettings()
        val updated = transform(current)
        require(updated.defaultRateMinor >= 0L)
        require(updated.currencyCode.matches(Regex("[A-Z]{3}")))
        settingsDao.upsert(updated.toEntity())
    }

    suspend fun createBackup(): ModernBackupPayload = ModernBackupPayload(
        settings = (settingsDao.get()?.toModel() ?: AppSettings()).let {
            BackupSettings(it.defaultRateMinor, it.currencyCode, it.themeMode.name)
        },
        workDays = workDays.all().map {
            BackupWorkDay(
                epochDay = it.epochDay,
                workedMinutes = it.workedMinutes,
                hourlyRateMinor = it.hourlyRateMinor,
                bonusMinor = it.bonusMinor,
                penaltyMinor = it.penaltyMinor,
                note = it.note,
            )
        },
        ratePeriods = rates.all().map {
            BackupRatePeriod(it.id, it.startEpochDay, it.endEpochDay, it.hourlyRateMinor)
        },
    )

    suspend fun restoreBackup(payload: ModernBackupPayload) {
        database.withTransaction {
            workDays.deleteAll()
            rates.deleteAll()
            settingsDao.deleteAll()
            if (payload.workDays.isNotEmpty()) {
                workDays.upsertAll(payload.workDays.map {
                    WorkDayEntity(it.epochDay, it.workedMinutes, it.hourlyRateMinor, it.bonusMinor, it.penaltyMinor, it.note)
                })
            }
            if (payload.ratePeriods.isNotEmpty()) {
                rates.insertAll(payload.ratePeriods.map {
                    RatePeriodEntity(it.id, it.startEpochDay, it.endEpochDay, it.hourlyRateMinor)
                })
            }
            settingsDao.upsert(
                AppSettingsEntity(
                    defaultRateMinor = payload.settings.defaultRateMinor,
                    currencyCode = payload.settings.currencyCode,
                    themeMode = payload.settings.themeMode,
                ),
            )
        }
    }

    private fun WorkDayEntity.toModel() = WorkDay(
        date = LocalDate.ofEpochDay(epochDay),
        workedMinutes = workedMinutes,
        hourlyRateMinor = hourlyRateMinor,
        bonusMinor = bonusMinor,
        penaltyMinor = penaltyMinor,
        note = note,
    )

    private fun WorkDay.toEntity() = WorkDayEntity(
        epochDay = date.toEpochDay(),
        workedMinutes = workedMinutes,
        hourlyRateMinor = hourlyRateMinor,
        bonusMinor = bonusMinor,
        penaltyMinor = penaltyMinor,
        note = note,
    )

    private fun AppSettingsEntity.toModel() = AppSettings(
        defaultRateMinor = defaultRateMinor,
        currencyCode = currencyCode,
        themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.SYSTEM),
    )

    private fun AppSettings.toEntity() = AppSettingsEntity(
        defaultRateMinor = defaultRateMinor,
        currencyCode = currencyCode,
        themeMode = themeMode.name,
    )
}
