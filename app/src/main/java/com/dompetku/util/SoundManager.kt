package com.dompetku.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Lightweight sound feedback using ToneGenerator — no assets required.
 * All calls are fire-and-forget; exceptions silently swallowed (e.g. on low-end devices).
 */
object SoundManager {

    /** Short pleasant beep — save transaction, confirm transfer */
    fun playSuccess(enabled: Boolean) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, 180)
        } catch (_: Exception) {}
    }

    /** Lower short beep — delete transaction */
    fun playDelete(enabled: Boolean) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55)
            tg.startTone(ToneGenerator.TONE_PROP_NACK, 80)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, 140)
        } catch (_: Exception) {}
    }

    /** Double-beep — transfer confirmed */
    fun playTransfer(enabled: Boolean) {
        if (!enabled) return
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
            Handler(Looper.getMainLooper()).postDelayed({ tg.release() }, 220)
        } catch (_: Exception) {}
    }
}
