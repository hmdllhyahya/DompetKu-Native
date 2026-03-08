package com.dompetku

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DompetKuApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Transaksi",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Pengingat harian untuk mencatat transaksi" }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_BUDGET_ID,
                    "Analisis Budget Harian",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Notifikasi pintar soal budget dan pengeluaran harianmu" }
            )
        }
    }

    companion object {
        const val CHANNEL_ID        = "dompetku_reminder"
        const val CHANNEL_BUDGET_ID = "dompetku_budget"
    }
}
