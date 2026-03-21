package com.dompetku.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object HapticHelper {

    fun tapLight(context: Context) {
        vibrateOneShot(context, 16L, 40)
    }

    fun tapMedium(context: Context) {
        vibrateOneShot(context, 24L, 80)
    }

    fun toggleOn(context: Context) {
        vibrateWaveform(context, longArrayOf(0L, 18L, 36L, 24L), intArrayOf(0, 80, 0, 120))
    }

    fun toggleOff(context: Context) {
        vibrateOneShot(context, 22L, 90)
    }

    @Suppress("DEPRECATION")
    private fun vibrateOneShot(context: Context, durationMs: Long, amplitude: Int) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                vibrator.vibrate(durationMs)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateWaveform(context: Context, timings: LongArray, amplitudes: IntArray) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(timings, -1)
            }
        }
    }
}
