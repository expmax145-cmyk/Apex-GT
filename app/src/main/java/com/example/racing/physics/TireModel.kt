package com.example.racing.physics

import kotlin.math.atan
import kotlin.math.sin

/**
 * Pacejka Magic Formula tire friction model.
 * Computes lateral and longitudinal tire adhesion forces based on slip angle, slip ratio, and dynamic normal load.
 */
class TireModel(
    val bStiffness: Float = 10.0f,
    val cShape: Float = 1.3f,
    val dPeakFriction: Float = 1.25f,
    val eCurvature: Float = 0.97f
) {
    /**
     * Calculates lateral grip force Fy = Fz * D * sin(C * atan(B * alpha - E * (B * alpha - atan(B * alpha))))
     */
    fun calculateLateralForce(slipAngleRad: Float, normalLoadN: Float, surfaceGrip: Float = 1.0f): Float {
        val bAlpha = bStiffness * slipAngleRad
        val inner = bAlpha - eCurvature * (bAlpha - atan(bAlpha))
        val peak = dPeakFriction * surfaceGrip
        val normalizedForce = peak * sin(cShape * atan(inner))
        return normalizedForce * normalLoadN
    }

    /**
     * Calculates longitudinal traction/braking force Fx based on slip ratio
     */
    fun calculateLongitudinalForce(slipRatio: Float, normalLoadN: Float, surfaceGrip: Float = 1.0f): Float {
        val clampedRatio = slipRatio.coerceIn(-1.0f, 1.0f)
        val bSlip = 12.0f * clampedRatio
        val inner = bSlip - 0.9f * (bSlip - atan(bSlip))
        val peak = dPeakFriction * surfaceGrip
        val normalizedForce = peak * sin(1.4f * atan(inner))
        return normalizedForce * normalLoadN
    }
}
