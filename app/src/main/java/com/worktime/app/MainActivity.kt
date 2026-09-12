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
import com.worktime.app.modern.ModernAppGraph
import com.worktime.app.modern.ModernViewModel
import com.worktime.app.modern.ui.ModernWorkTimeApp
import com.worktime.app.modern.ui.ModernWorkTimeTheme

class MainActivity : ComponentActivity() {
    private val appGraph: ModernAppGraph by lazy(LazyThreadSafetyMode.NONE) {
        ModernAppGraph.get(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val factory = remember { ModernViewModel.Factory(appGraph.repository) }
            val viewModel: ModernViewModel = viewModel(factory = factory)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            ModernWorkTimeTheme(themeMode = settings.themeMode) {
                ModernWorkTimeApp(viewModel)
            }
        }
    }
}
