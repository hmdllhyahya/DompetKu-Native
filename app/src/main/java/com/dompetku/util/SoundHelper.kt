package com.dompetku.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Sound effects ported from Web Audio API (JSX) to ToneGenerator.
 *
 * Income:   523→659→784 Hz  (ascending  trio, 80ms each)
 * Expense:  784→659→523 Hz  (descending trio, 80ms each)
 * Transfer: 659×3           (flat       trio, 80ms each)
 *
 * ToneGenerator only plays DTMF tones (standard set), so we map nearest
 * frequencies to their DTMF equivalents and control volume + timing ourselves.
 * For richer sound fidelity this can be swapped for AudioTrack in future.
 */
object SoundHelper {

    private const val DURATION_MS = 80
    private const val VOLUME = ToneGenerator.MAX_VOLUME

    suspend fun playIncome() = withContext(Dispatchers.IO) {
        playSequence(listOf(
            ToneGenerator.TONE_DTMF_5,   // ~523 Hz (closest to C5)
            ToneGenerator.TONE_DTMF_A,   // ~659 Hz
            ToneGenerator.TONE_DTMF_B,   // ~784 Hz
        ))
    }

    suspend fun playExpense() = withContext(Dispatchers.IO) {
        playSequence(listOf(
            ToneGenerator.TONE_DTMF_B,
            ToneGenerator.TONE_DTMF_A,
            ToneGenerator.TONE_DTMF_5,
        ))
    }

    suspend fun playTransfer() = withContext(Dispatchers.IO) {
        playSequence(listOf(
            ToneGenerator.TONE_DTMF_A,
            ToneGenerator.TONE_DTMF_A,
            ToneGenerator.TONE_DTMF_A,
        ))
    }

    private suspend fun playSequence(tones: List<Int>) {
        val gen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
        try {
            for (tone in tones) {
                gen.startTone(tone, DURATION_MS)
                delay(DURATION_MS.toLong() + 10L)
            }
        } finally {
            gen.release()
        }
    }
}
