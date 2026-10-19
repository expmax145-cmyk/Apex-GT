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

    /**
     * Kamm's friction circle / ellipse combined slip computation.
     * Given longitudinal demand force and normal load, determines:
     * 1) actual longitudinal force (limited by available tire adhesion)
     * 2) remaining lateral adhesion capacity factor in range [0.18f, 1.0f]
     * 3) tire slip ratio [0.0f, 1.0f] for telemetry, smoke, and audio.
     */
    fun calculateCombinedAdhesion(
        longitudinalDemandN: Float,
        normalLoadN: Float,
        surfaceGrip: Float = 1.0f
    ): CombinedSlipResult {
        val maxAdhesionN = (normalLoadN * dPeakFriction * surfaceGrip).coerceAtLeast(10f)
        val absDemand = kotlin.math.abs(longitudinalDemandN)
        val demandRatio = absDemand / maxAdhesionN

        val actualLongitudinalForce: Float
        val slipRatio: Float
        val lateralCapacity: Float

        if (demandRatio <= 1.0f) {
            // Static / Linear adhesion zone
            actualLongitudinalForce = longitudinalDemandN
            slipRatio = (demandRatio * 0.20f) // Small micro-slip under grip
            val normalizedDemand = demandRatio.coerceIn(0f, 1f)
            lateralCapacity = kotlin.math.sqrt(1.0f - normalizedDemand * normalizedDemand).coerceIn(0.20f, 1.0f)
        } else {
            // Kinetic / Sliding friction zone (wheelspin or lockup)
            // Tire slides with slightly reduced dynamic friction (~90% of peak)
            val slidingForceN = maxAdhesionN * 0.90f
            actualLongitudinalForce = kotlin.math.sign(longitudinalDemandN) * slidingForceN
            slipRatio = ((demandRatio - 1.0f) * 1.5f + 0.35f).coerceIn(0.35f, 1.0f)
            lateralCapacity = 0.20f // Low residual lateral grip during heavy wheelspin/lockup
        }

        return CombinedSlipResult(
            actualFx = actualLongitudinalForce,
            lateralCapacityFactor = lateralCapacity,
            slipRatio = slipRatio
        )
    }

    data class CombinedSlipResult(
        val actualFx: Float,
        val lateralCapacityFactor: Float,
        val slipRatio: Float
    )
}
