package com.example.racing.physics

import kotlin.math.PI
import kotlin.math.max

class EnginePowertrain(
    val maxTorqueNm: Float = 720f,
    val peakTorqueRpm: Float = 5500f,
    val redlineRpm: Float = 8600f,
    val idleRpm: Float = 1050f,
    val cylinderCount: Int = 8,
    val hasTurbo: Boolean = true,
    val gearRatios: FloatArray = floatArrayOf(
        -3.40f, // R
        0.00f,  // N
        3.82f,  // 1st
        2.36f,  // 2nd
        1.69f,  // 3rd
        1.31f,  // 4th
        1.05f,  // 5th
        0.84f   // 6th
    ),
    val finalDriveRatio: Float = 3.65f,
    val wheelRadiusM: Float = 0.35f
) {
    var currentGear: Int = 2 // 0: R, 1: N, 2: 1st, 3: 2nd ...
    var currentRpm: Float = idleRpm
    var shiftTimer: Float = 0f
    var turboBoostBar: Float = 0f // 0.0 to 1.6 bar
    var isRevLimiting: Boolean = false
    var exhaustBackfireTrigger: Boolean = false

    fun update(
        throttleInput: Float,
        vehicleSpeedForwardMs: Float,
        dt: Float,
        isAutomatic: Boolean = true
    ): Float {
        exhaustBackfireTrigger = false

        if (shiftTimer > 0f) {
            shiftTimer -= dt
        }

        // Turbo Spool Dynamics: spools up under high throttle & high RPM
        if (hasTurbo) {
            val targetBoost = if (throttleInput > 0.5f && currentRpm > 3200f) 1.55f * throttleInput else 0.0f
            val spoolRate = if (targetBoost > turboBoostBar) 3.5f else 5.0f
            turboBoostBar += (targetBoost - turboBoostBar) * (dt * spoolRate)
        } else {
            turboBoostBar = 0f
        }

        val isReverse = (currentGear == 0)
        val absSpeed = kotlin.math.abs(vehicleSpeedForwardMs)
        val effectiveSpeed = if (isReverse) absSpeed else max(0f, vehicleSpeedForwardMs)
        val selectedRatio = gearRatios[currentGear]

        if (currentGear == 1) { // Neutral
            currentRpm = (currentRpm + (throttleInput * 5500f - (currentRpm - idleRpm) * 3f) * dt)
                .coerceIn(idleRpm, redlineRpm)
            return 0f
        }

        // Calculate wheel angular velocity
        val wheelAngularVel = effectiveSpeed / wheelRadiusM
        val targetEngineRpm = (wheelAngularVel * kotlin.math.abs(selectedRatio) * finalDriveRatio * 60f / (2f * PI.toFloat()))

        // Smooth RPM interpolation with clutch slip at low speeds
        val baseRpm = max(idleRpm, targetEngineRpm)
        currentRpm = (currentRpm + (baseRpm - currentRpm) * (dt * 16f)).coerceIn(idleRpm, redlineRpm + 350f)

        // Rev Limiter Fuel Cut oscillation
        if (currentRpm >= redlineRpm) {
            isRevLimiting = true
            currentRpm -= 250f
            exhaustBackfireTrigger = true
        } else {
            isRevLimiting = false
        }

        if (isAutomatic && shiftTimer <= 0f && currentGear >= 2) {
            // Automatic Shift Up
            if (currentGear in 2 until gearRatios.size - 1 && currentRpm > redlineRpm * 0.94f) {
                currentGear++
                shiftTimer = 0.16f
                currentRpm *= 0.72f
                exhaustBackfireTrigger = true
            }
            // Automatic Shift Down with rev-matching blip
            else if (currentGear > 2 && currentRpm < 3200f && effectiveSpeed < getMinSpeedForGear(currentGear)) {
                currentGear--
                shiftTimer = 0.14f
                currentRpm = (currentRpm * 1.35f).coerceAtMost(redlineRpm * 0.92f)
            }
        }

        // Torque calculation from RPM curve (parabolic drop around peak)
        val rpmNorm = (currentRpm - peakTorqueRpm) / peakTorqueRpm
        val torqueFactor = (1.0f - 0.35f * (rpmNorm * rpmNorm)).coerceIn(0.2f, 1.0f)
        val turboMultiplier = 1.0f + (turboBoostBar * 0.28f)
        val engineTorque = if (isReverse && absSpeed > 13.0f) {
            0f
        } else {
            maxTorqueNm * torqueFactor * throttleInput * turboMultiplier
        }

        // Wheel drive torque (negative in reverse gear)
        val totalRatio = selectedRatio * finalDriveRatio
        val wheelTorque = engineTorque * totalRatio * 0.92f
        return wheelTorque / wheelRadiusM
    }

    private fun getMinSpeedForGear(gear: Int): Float {
        return when (gear) {
            3 -> 12f // ~43 km/h
            4 -> 22f // ~80 km/h
            5 -> 35f // ~126 km/h
            6 -> 48f // ~172 km/h
            else -> 0f
        }
    }

    fun getGearDisplayName(): String {
        return when (currentGear) {
            0 -> "R"
            1 -> "N"
            else -> (currentGear - 1).toString()
        }
    }
}
