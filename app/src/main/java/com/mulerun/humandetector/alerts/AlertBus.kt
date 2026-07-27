package com.mulerun.humandetector.alerts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Central place for the "new person" / "person left" cues:
 *   1) vibration
 *   2) short tone
 *   3) TTS voice announcement ("One person detected.")
 */
class AlertBus(private val ctx: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)

    var voiceEnabled: Boolean = true
    var soundEnabled: Boolean = true
    var vibrationEnabled: Boolean = true

    init {
        tts = TextToSpeech(ctx) { s ->
            if (s == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }
    }

    fun onCountChanged(oldCount: Int, newCount: Int) {
        if (newCount > oldCount) fireEnter(newCount)
        else if (newCount < oldCount) fireLeave(newCount)
    }

    private fun fireEnter(count: Int) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 60, 40, 120))
        if (soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        if (voiceEnabled) speak(spellCount(count) + " detected.")
    }
    private fun fireLeave(count: Int) {
        if (vibrationEnabled) vibrate(longArrayOf(0, 40))
        if (soundEnabled) tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
        if (voiceEnabled) {
            val phrase = if (count == 0) "All people have left."
                         else "${spellCount(count)} remaining."
            speak(phrase)
        }
    }
    private fun speak(msg: String) {
        if (!ttsReady) return
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "hd_${System.currentTimeMillis()}")
    }

    private fun spellCount(n: Int): String = when (n) {
        0 -> "Zero persons"
        1 -> "One person"
        2 -> "Two people"
        3 -> "Three people"
        4 -> "Four people"
        5 -> "Five people"
        else -> "$n people"
    }

    private fun vibrate(pattern: LongArray) {
        val vib = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= 26) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build())
        } else {
            @Suppress("DEPRECATION") vib.vibrate(pattern, -1)
        }
    }

    fun shutdown() {
        try { tone.release() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
    }
}
