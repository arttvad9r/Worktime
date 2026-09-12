package com.worktime.app.modern.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "modern_work_days")
data class WorkDayEntity(
    @PrimaryKey val epochDay: Long,
    val workedMinutes: Int,
    val hourlyRateMinor: Long,
    val bonusMinor: Long,
    val penaltyMinor: Long,
    @ColumnInfo(defaultValue = "0") val otherMinor: Long = 0L,
    val note: String,
)

@Entity(
    tableName = "modern_rate_periods",
    indices = [Index("startEpochDay"), Index("endEpochDay")],
)
data class RatePeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val hourlyRateMinor: Long,
)

@Entity(tableName = "modern_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val defaultRateMinor: Long = 0L,
    val currencyCode: String = "RUB",
    val themeMode: String = "SYSTEM",
)

@Dao
interface WorkDayDao {
    @Query("SELECT * FROM modern_work_days WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY epochDay")
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkDayEntity>>

    @Query("SELECT * FROM modern_work_days WHERE epochDay = :epochDay LIMIT 1")
    suspend fun get(epochDay: Long): WorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorkDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<WorkDayEntity>)

    @Query("DELETE FROM modern_work_days WHERE epochDay = :epochDay")
    suspend fun delete(epochDay: Long)

    @Query("UPDATE modern_work_days SET hourlyRateMinor = :rateMinor WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun updateRateInRange(startEpochDay: Long, endEpochDay: Long, rateMinor: Long): Int

    @Query("UPDATE modern_work_days SET hourlyRateMinor = :rateMinor WHERE epochDay >= :startEpochDay")
    suspend fun updateRateFrom(startEpochDay: Long, rateMinor: Long): Int

    @Query("SELECT * FROM modern_work_days ORDER BY epochDay")
    suspend fun all(): List<WorkDayEntity>

    @Query("DELETE FROM modern_work_days")
    suspend fun deleteAll()
}

@Dao
interface RatePeriodDao {
    @Query("SELECT * FROM modern_rate_periods WHERE startEpochDay <= :epochDay AND (endEpochDay IS NULL OR endEpochDay >= :epochDay) ORDER BY id DESC LIMIT 1")
    suspend fun effectiveFor(epochDay: Long): RatePeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RatePeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RatePeriodEntity>)

    @Query("SELECT * FROM modern_rate_periods ORDER BY id")
    suspend fun all(): List<RatePeriodEntity>

    @Query("DELETE FROM modern_rate_periods")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM modern_settings WHERE id = 0 LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM modern_settings WHERE id = 0 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingsEntity)

    @Query("DELETE FROM modern_settings")
    suspend fun deleteAll()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE modern_work_days ADD COLUMN otherMinor INTEGER NOT NULL DEFAULT 0",
        )
    }
}

@Database(
    entities = [WorkDayEntity::class, RatePeriodEntity::class, AppSettingsEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ModernDatabase : RoomDatabase() {
    abstract fun workDayDao(): WorkDayDao
    abstract fun ratePeriodDao(): RatePeriodDao
    abstract fun settingsDao(): SettingsDao
}
