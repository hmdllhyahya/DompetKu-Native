package com.dompetku.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object DeepLinkHelper {

    /**
     * Attempt to open the banking / e-wallet app via deep link.
     * Falls back to Play Store if app is not installed.
     */
    fun openApp(context: Context, brandKey: String) {
        val info = BrandDetector.byKey(brandKey) ?: return
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(info.deepLinkUri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (deepLinkIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(deepLinkIntent)
        } else {
            // Fallback: open Play Store
            val playStore = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${info.fallbackPkg}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(playStore)
        }
    }
}
