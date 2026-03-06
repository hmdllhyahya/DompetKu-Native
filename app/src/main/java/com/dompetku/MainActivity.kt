package com.dompetku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dompetku.ui.RootViewModel
import com.dompetku.ui.navigation.DompetKuNavHost
import com.dompetku.ui.theme.DompetKuTheme
import dagger.hilt.android.AndroidEntryPoint
import com.dompetku.R

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    // Timestamp saat app masuk background
    private var backgroundTime = 0L

    companion object {
        // Lock setelah 30 detik di background
        private const val LOCK_THRESHOLD_MS = 30_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            DompetKuTheme {
                DompetKuNavHost(rootViewModel = rootViewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundTime > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundTime
            if (elapsed >= LOCK_THRESHOLD_MS) {
                rootViewModel.triggerLock()
            }
            backgroundTime = 0L
        }
    }
}