package com.example.racing.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class ProceduralAudioEngine {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    private var audioThread: Thread? = null

    // Real-time audio parameters
    @Volatile var engineRpm: Float = 1000f
    @Volatile var throttleLoad: Float = 0f
    @Volatile var tireSlip: Float = 0f
    @Volatile var turboBoost: Float = 0f
    @Volatile var cylinderCount: Int = 8
    @Volatile var isWetRoad: Boolean = false
    @Volatile var isMuted: Boolean = false
    @Volatile var masterVolume: Float = 0.75f
    @Volatile var triggerPop: Boolean = false

    private val sampleRate = 22050
    private val bufferSize = 2048

    fun start() {
        if (isRunning) return
        isRunning = true

        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackBufSize = maxOf(minBufSize, bufferSize * 2)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(trackBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()

            audioThread = Thread({ audioLoop() }, "ApexAudioSynthesizer").apply {
                priority = Thread.NORM_PRIORITY + 1
                start()
            }
        } catch (e: Exception) {
            Log.e("ProceduralAudioEngine", "Error initializing AudioTrack: ${e.message}")
        }
    }

    private fun audioLoop() {
        val audioBuffer = ShortArray(bufferSize)
        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var turboPhase = 0.0
        var tirePhase = 0.0
        var popDecay = 0.0
        var smoothedRpm = 1000.0

        while (isRunning) {
            val track = audioTrack ?: break

            if (isMuted || masterVolume <= 0f) {
                audioBuffer.fill(0)
                try {
                    track.write(audioBuffer, 0, bufferSize)
                } catch (_: Exception) {
                    break
                }
                continue
            }

            if (triggerPop) {
                triggerPop = false
                popDecay = 1.0
            }

            // Smooth RPM to prevent sudden pops on gear shifts
            val targetRpm = engineRpm.coerceIn(800f, 9500f).toDouble()
            smoothedRpm += (targetRpm - smoothedRpm) * 0.15

            val cylinders = cylinderCount.coerceIn(6, 10)
            val f0 = (smoothedRpm / 60.0) * (cylinders / 2.0)
            val load = throttleLoad.coerceIn(0f, 1f)
            val slip = tireSlip.coerceIn(0f, 1f)
            val boost = turboBoost.coerceIn(0f, 1.6f)

            val dt = 1.0 / sampleRate
            val twoPi = 2.0 * PI

            val step1 = twoPi * f0 * dt
            val step2 = twoPi * (f0 * 2.0) * dt
            val step3 = twoPi * (f0 * (if (cylinders == 10) 3.2 else 3.5)) * dt

            // Turbo spool whistle
            val turboFreq = 2200.0 + (boost * 1100.0)
            val turboStep = twoPi * turboFreq * dt

            // Tire squeal frequency
            val tireStep = twoPi * 920.0 * dt

            val vol = masterVolume

            for (i in 0 until bufferSize) {
                val s1 = sin(phase1)
                val s2 = sin(phase2) * 0.52
                val s3 = sin(phase3) * 0.32

                var rawEngine = (s1 + s2 + s3) / 1.84
                if (load > 0.25f) {
                    val drive = 1.0 + (load - 0.25f) * 1.45
                    rawEngine = (rawEngine * drive).coerceIn(-1.0, 1.0)
                }

                val engineLevel = (0.28 + load * 0.42) * vol

                var turboLevel = 0.0
                if (boost > 0.1f) {
                    val whistle = sin(turboPhase) * (boost / 1.6) * 0.18 * vol
                    turboLevel = whistle
                }

                var tireLevel = 0.0
                if (slip > 0.28f) {
                    val noise = (Random.nextFloat() * 2f - 1f) * 0.45
                    val squeal = sin(tirePhase) * 0.45
                    val tireSlipFactor = ((slip - 0.28f) / 0.72f).coerceIn(0f, 1f)
                    tireLevel = (noise + squeal) * tireSlipFactor * 0.42 * vol
                } else if (isWetRoad && load > 0.4f) {
                    tireLevel = (Random.nextFloat() * 2f - 1f) * 0.08 * vol
                }

                var popLevel = 0.0
                if (popDecay > 0.01) {
                    val popNoise = (Random.nextFloat() * 2f - 1f) * popDecay * 0.65 * vol
                    popLevel = popNoise
                    popDecay *= 0.9985
                }

                val mixed = (rawEngine * engineLevel + turboLevel + tireLevel + popLevel).coerceIn(-1.0, 1.0)
                audioBuffer[i] = (mixed * 32767.0).toInt().toShort()

                phase1 = (phase1 + step1) % twoPi
                phase2 = (phase2 + step2) % twoPi
                phase3 = (phase3 + step3) % twoPi
                turboPhase = (turboPhase + turboStep) % twoPi
                tirePhase = (tirePhase + tireStep) % twoPi
            }

            try {
                track.write(audioBuffer, 0, bufferSize)
            } catch (_: Exception) {
                break
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            audioThread?.interrupt()
            audioThread?.join(250)
            audioThread = null
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("ProceduralAudioEngine", "Error stopping audio: ${e.message}")
        }
    }
}
