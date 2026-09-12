package com.worktime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.data.MIGRATION_1_2
import com.worktime.app.modern.data.ModernDatabase
import com.worktime.app.modern.data.ModernRepository
import com.worktime.app.modern.ui.ModernWorkTimeApp
import com.worktime.app.modern.ui.ModernWorkTimeTheme

class MainActivity : ComponentActivity() {
    private val modernDatabase: ModernDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ModernDatabase::class.java,
            "worktime-modern.db",
        ).addMigrations(MIGRATION_1_2)
            .build()
    }

    private val modernRepository: ModernRepository by lazy {
        ModernRepository(modernDatabase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val factory = remember { ModernViewModel.Factory(modernRepository) }
            val viewModel: ModernViewModel = viewModel(factory = factory)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            ModernWorkTimeTheme(themeMode = settings.themeMode) {
                ModernWorkTimeApp(viewModel)
            }
        }
    }
}
