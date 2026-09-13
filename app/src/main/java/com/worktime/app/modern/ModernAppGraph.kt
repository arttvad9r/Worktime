package com.worktime.app.modern

import android.content.Context
import androidx.room.Room
import com.worktime.app.modern.data.MIGRATION_1_2
import com.worktime.app.modern.data.ModernDatabase
import com.worktime.app.modern.data.ModernRepository

class ModernAppGraph private constructor(
    val repository: ModernRepository,
) {
    companion object {
        @Volatile
        private var instance: ModernAppGraph? = null

        fun get(context: Context): ModernAppGraph =
            instance ?: synchronized(this) {
                instance ?: create(context.applicationContext).also { instance = it }
            }

        private fun create(appContext: Context): ModernAppGraph {
            val database = Room.databaseBuilder(
                appContext,
                ModernDatabase::class.java,
                "worktime-modern.db",
            ).addMigrations(MIGRATION_1_2)
                .build()
            return ModernAppGraph(ModernRepository(database))
        }
    }
}
