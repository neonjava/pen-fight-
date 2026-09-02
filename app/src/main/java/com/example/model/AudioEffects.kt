package com.example.model

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class AudioEffects(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    fun playFlickSound(powerRatio: Float) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = (70 + (powerRatio * 50)).toInt()
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                val startFreq = 200.0 + (powerRatio * 300.0)
                val endFreq = 900.0 + (powerRatio * 600.0)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (endFreq - startFreq) * (progress * progress)
                    val envelope = sin(progress * PI)
                    val value = sin(2.0 * PI * currentFreq * t) * envelope * 0.7
                    samples[i] = (value * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(30, (80 * powerRatio).toInt().coerceIn(30, 255))
    }

    fun playClashSound(intensity: Float) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 90
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                val primaryFreq = 1100.0 + (intensity * 400.0)
                val secondaryFreq = 2400.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = exp(-t * 38.0)
                    val noise = (Math.random() * 2.0 - 1.0) * 0.25 * decay
                    val wave = (sin(2.0 * PI * primaryFreq * t) * 0.6 + sin(2.0 * PI * secondaryFreq * t) * 0.3) * decay
                    val sampleVal = (wave + noise) * 0.9 * intensity.coerceIn(0.3f, 1.0f)
                    samples[i] = (sampleVal * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(45, (140 * intensity).toInt().coerceIn(60, 255))
    }

    fun playDeskThudSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 120
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = exp(-t * 28.0)
                    val freq = 180.0 * exp(-t * 12.0)
                    val wave = sin(2.0 * PI * freq * t) * decay * 0.8
                    samples[i] = (wave * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(25, 70)
    }

    fun playFallSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 350
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples
                    val freq = 800.0 * (1.0 - progress * 0.7)
                    val envelope = (1.0 - progress) * (1.0 - progress)
                    val wave = sin(2.0 * PI * freq * t) * envelope * 0.7
                    samples[i] = (wave * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(100, 200)
    }

    fun playWinFanfare() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
                val noteDurationMs = 100
                val totalSamples = (sampleRate * (noteDurationMs * notes.size / 1000.0)).toInt()
                val samples = ShortArray(totalSamples)

                for ((index, freq) in notes.withIndex()) {
                    val start = (index * noteDurationMs * sampleRate / 1000)
                    val end = start + (noteDurationMs * sampleRate / 1000)
                    for (i in start until end.coerceAtMost(totalSamples)) {
                        val t = (i - start).toDouble() / sampleRate
                        val decay = 1.0 - ((i - start).toDouble() / (noteDurationMs * sampleRate / 1000.0))
                        val wave = (sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)) * decay * 0.6
                        samples[i] = (wave * Short.MAX_VALUE).toInt().toShort()
                    }
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(80, 180)
    }

    fun playClickSound() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 44100
                val numSamples = (sampleRate * 0.03).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = exp(-t * 90.0)
                    val wave = sin(2.0 * PI * 1800.0 * t) * decay * 0.7
                    samples[i] = (wave * Short.MAX_VALUE).toInt().toShort()
                }
                playPcm(samples, sampleRate)
            } catch (_: Exception) {}
        }
        vibrate(15, 60)
    }

    private fun playPcm(samples: ShortArray, sampleRate: Int) {
        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size.coerceAtLeast(minBufSize) * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        scope.launch {
            kotlinx.coroutines.delay(800)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
