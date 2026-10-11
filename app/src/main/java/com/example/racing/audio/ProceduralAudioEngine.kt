package com.example.racing.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class ProceduralAudioEngine {

    private var audioTrack: AudioTrack? = null
    @Volatile private var isRunning = false
    private var audioThread: Thread? = null

    // Real-time audio parameters
    @Volatile var engineRpm: Float = 1000f
    @Volatile var redlineRpm: Float = 8500f
    @Volatile var throttleLoad: Float = 0f
    @Volatile var tireSlip: Float = 0f
    @Volatile var turboBoost: Float = 0f
    @Volatile var cylinderCount: Int = 8
    @Volatile var isWetRoad: Boolean = false
    @Volatile var isOnKerb: Boolean = false
    @Volatile var isMuted: Boolean = false
    @Volatile var masterVolume: Float = 0.75f
    @Volatile var triggerPop: Boolean = false
    @Volatile var collisionImpactIntensity: Float = 0f

    // Opponent spatial audio
    @Volatile var opponentDistance: Float = 100f // meters
    @Volatile var opponentRpm: Float = 3000f
    @Volatile var opponentPan: Float = 0f       // -1.0 (left) to +1.0 (right)

    // BOV tracking
    private var lastThrottle: Float = 0f
    private var lastBoost: Float = 0f
    private var bovDecay: Float = 0f
    private var bovTimer: Float = 0f

    private val sampleRate = 22050
    private val bufferSize = 2048

    fun start() {
        if (isRunning) return
        isRunning = true

        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackBufSize = maxOf(minBufSize, bufferSize * 4)

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
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

    fun triggerCollision(intensity: Float = 1.0f) {
        collisionImpactIntensity = intensity.coerceIn(0.2f, 1.5f)
    }

    private fun audioLoop() {
        val stereoBufferSize = bufferSize * 2
        val audioBuffer = ShortArray(stereoBufferSize)

        var phase1 = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var turboPhase = 0.0
        var tirePhase = 0.0
        var kerbPhase = 0.0
        var opponentPhase = 0.0
        var popDecay = 0.0
        var impactDecay = 0.0
        var revLimiterTimer = 0.0
        var smoothedRpm = 1000.0

        val twoPi = 2.0 * PI
        val dt = 1.0 / sampleRate

        while (isRunning) {
            val track = audioTrack ?: break

            if (isMuted || masterVolume <= 0f) {
                audioBuffer.fill(0)
                try {
                    track.write(audioBuffer, 0, stereoBufferSize)
                } catch (_: Exception) {
                    break
                }
                continue
            }

            // Detect blow-off valve (BOV) trigger on throttle lift off after boost
            val load = throttleLoad.coerceIn(0f, 1f)
            val boost = turboBoost.coerceIn(0f, 1.8f)
            if (lastBoost > 0.35f && load < 0.15f && lastThrottle > 0.4f && bovDecay <= 0.01f) {
                bovDecay = 1.0f
                bovTimer = 0.45f
            }
            lastThrottle = load
            lastBoost = boost

            if (triggerPop) {
                triggerPop = false
                popDecay = 1.0
            }

            if (collisionImpactIntensity > 0f) {
                impactDecay = collisionImpactIntensity.toDouble()
                collisionImpactIntensity = 0f
            }

            // Smooth RPM to prevent harsh discontinuities
            val targetRpm = engineRpm.coerceIn(800f, 9800f).toDouble()
            smoothedRpm += (targetRpm - smoothedRpm) * 0.16

            val cylinders = cylinderCount.coerceIn(6, 10)
            val redline = redlineRpm.toDouble()
            val isRevLimiting = smoothedRpm >= (redline - 120.0)

            // Primary engine firing frequency
            val f0 = (smoothedRpm / 60.0) * (cylinders / 2.0)
            val slip = tireSlip.coerceIn(0f, 1f)
            val vol = masterVolume

            // Harmonic steps based on cylinder architecture
            val step1 = twoPi * f0 * dt
            val step2 = twoPi * (f0 * 2.0) * dt
            val harmonicMultiplier = when (cylinders) {
                10 -> 3.2 // Monza V10: singing high harmonics
                6 -> 2.5  // Spirit 86: raspy boxer harmonics
                else -> 3.5 // Valkyrie V8: deep crossplane bark
            }
            val step3 = twoPi * (f0 * harmonicMultiplier) * dt

            // Turbo whistle frequency (spools 1800 - 3400 Hz)
            val turboFreq = 1800.0 + (boost * 950.0)
            val turboStep = twoPi * turboFreq * dt

            // Tire squeal frequency (~880 Hz)
            val tireStep = twoPi * 880.0 * dt

            // Kerb rumble frequency (~90 Hz)
            val kerbStep = twoPi * 92.0 * dt

            // Opponent engine base step
            val oppDist = opponentDistance
            val hasAudibleOpponent = oppDist < 35f
            val oppF0 = (opponentRpm.coerceIn(800f, 9500f) / 60.0) * 4.0
            val oppStep = twoPi * oppF0 * dt
            val oppAtten = if (hasAudibleOpponent) {
                val dNorm = (1.0 - (oppDist / 35.0)).coerceIn(0.0, 1.0)
                (dNorm * dNorm * 0.42 * vol)
            } else 0.0

            val pan = opponentPan.coerceIn(-1f, 1f).toDouble()
            val oppLeftGain = ((1.0 - pan) * 0.5) * oppAtten
            val oppRightGain = ((1.0 + pan) * 0.5) * oppAtten

            for (i in 0 until bufferSize) {
                // 1. Player Engine Synthesis
                val s1 = sin(phase1)
                val s2 = sin(phase2) * 0.48
                val s3 = sin(phase3) * (if (cylinders == 10) 0.40 else 0.28)

                var rawEngine = (s1 + s2 + s3) / 1.76

                // High load exhaust saturation & growl
                if (load > 0.20f) {
                    val drive = 1.0 + (load - 0.20f) * 1.55
                    rawEngine = (rawEngine * drive).coerceIn(-1.0, 1.0)
                }

                // Rev limiter stutter (rat-tat-tat ignition cut)
                if (isRevLimiting) {
                    revLimiterTimer += dt
                    val cutCycle = (revLimiterTimer * 22.0) % 1.0
                    if (cutCycle < 0.45) {
                        rawEngine *= 0.15 // Ignition cut
                    }
                    if (popDecay < 0.2 && Random.nextFloat() < 0.04f) {
                        popDecay = 0.85
                    }
                }

                val engineLevel = (0.26 + load * 0.44) * vol

                // 2. Turbo Spool Whistle
                var turboLevel = 0.0
                if (boost > 0.08f) {
                    turboLevel = sin(turboPhase) * (boost / 1.6) * 0.16 * vol
                }

                // 3. Blow-Off Valve (BOV) Flutter
                var bovLevel = 0.0
                if (bovDecay > 0.02f) {
                    val flutterMod = sin(bovTimer * 12.0 * twoPi) * 0.5 + 0.5
                    val chirpFreq = 1600.0 + sin(bovTimer * 28.0) * 400.0
                    val chirp = sin(twoPi * chirpFreq * dt * i)
                    bovLevel = chirp * flutterMod * bovDecay * 0.35 * vol
                    bovDecay *= 0.9995f
                }

                // 4. Tire Slip & Squeal
                var tireLevel = 0.0
                if (slip > 0.26f) {
                    val noise = (Random.nextFloat() * 2f - 1f) * 0.42
                    val squeal = sin(tirePhase) * 0.50
                    val tireSlipFactor = ((slip - 0.26f) / 0.74f).coerceIn(0f, 1f)
                    tireLevel = (noise + squeal) * tireSlipFactor * 0.45 * vol
                } else if (isWetRoad && load > 0.35f) {
                    // Wet road spray hiss
                    tireLevel = (Random.nextFloat() * 2f - 1f) * 0.09 * vol
                }

                // 5. Kerb / Rumble Strip Buzz
                var kerbLevel = 0.0
                if (isOnKerb) {
                    val squareBuzz = if (sin(kerbPhase) > 0.0) 0.35 else -0.35
                    kerbLevel = squareBuzz * 0.28 * vol
                }

                // 6. Exhaust Backfire Pops
                var popLevel = 0.0
                if (popDecay > 0.01) {
                    popLevel = (Random.nextFloat() * 2f - 1f) * popDecay * 0.70 * vol
                    popDecay *= 0.9986
                }

                // 7. Collision Impact Crunch (thump + metallic burst)
                var impactLevel = 0.0
                if (impactDecay > 0.02) {
                    val impactThump = sin(twoPi * 65.0 * dt * i) * 0.75
                    val impactCrunch = (Random.nextFloat() * 2f - 1f) * 0.65
                    impactLevel = (impactThump + impactCrunch) * impactDecay * 0.85 * vol
                    impactDecay *= 0.9982
                }

                // Combine mono center components
                val centerMono = (rawEngine * engineLevel + turboLevel + bovLevel + tireLevel + kerbLevel + popLevel + impactLevel)

                // 8. Opponent Engine Synthesis (Spatial stereo)
                var oppSignal = 0.0
                if (hasAudibleOpponent) {
                    val op1 = sin(opponentPhase)
                    val op2 = sin(opponentPhase * 2.0) * 0.45
                    oppSignal = (op1 + op2) / 1.45
                }

                val leftMix = (centerMono + oppSignal * oppLeftGain).coerceIn(-1.0, 1.0)
                val rightMix = (centerMono + oppSignal * oppRightGain).coerceIn(-1.0, 1.0)

                audioBuffer[i * 2] = (leftMix * 32767.0).toInt().toShort()
                audioBuffer[i * 2 + 1] = (rightMix * 32767.0).toInt().toShort()

                // Phase increments
                phase1 = (phase1 + step1) % twoPi
                phase2 = (phase2 + step2) % twoPi
                phase3 = (phase3 + step3) % twoPi
                turboPhase = (turboPhase + turboStep) % twoPi
                tirePhase = (tirePhase + tireStep) % twoPi
                kerbPhase = (kerbPhase + kerbStep) % twoPi
                opponentPhase = (opponentPhase + oppStep) % twoPi
            }

            try {
                track.write(audioBuffer, 0, stereoBufferSize)
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
