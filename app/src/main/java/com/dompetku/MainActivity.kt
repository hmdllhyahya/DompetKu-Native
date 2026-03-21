package com.dompetku

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.dompetku.ui.RootViewModel
import com.dompetku.ui.navigation.DompetKuNavHost
import com.dompetku.ui.theme.DompetKuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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
        // Prevent screenshots and app-switcher previews from exposing financial data
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            val prefs = rootViewModel.prefs.collectAsStateWithLifecycle().value
            LaunchedEffect(prefs?.lang) {
                val lang = prefs?.lang ?: "id"
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(lang)
                )
            }
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
