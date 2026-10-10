package com.example.racing.physics

import com.example.racing.engine.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class VehiclePhysics(
    val massKg: Float = 1380f,
    val wheelbaseM: Float = 2.65f,
    val trackWidthM: Float = 1.68f,
    val cgHeightM: Float = 0.36f,
    val maxSteerAngleRad: Float = 0.58f,
    val downforceCoeff: Float = 0.75f,
    val dragCoeff: Float = 0.31f,
    val tireModel: TireModel = TireModel(),
    val powertrain: EnginePowertrain = EnginePowertrain()
) {
    // World space position & orientations
    val position = Vector3f(0f, 0.35f, 0f)
    var headingAngleRad: Float = 0f
    var pitchAngleRad: Float = 0f
    var rollAngleRad: Float = 0f

    // Local vehicle frame velocities
    var forwardVelocity: Float = 0f
    var lateralVelocity: Float = 0f
    var yawRate: Float = 0f

    // Driver Inputs
    var throttle: Float = 0f
    var brake: Float = 0f
    var steerInput: Float = 0f
    var handbrake: Boolean = false
    var boostActive: Boolean = false

    // Real-time telemetry
    var speedKmh: Float = 0f
    var gForceLateral: Float = 0f
    var gForceLongitudinal: Float = 0f
    var driftAngleDeg: Float = 0f
    var isOffRoad: Boolean = false
    var hasCollidedBarrier: Boolean = false
    var tireSlipFl: Float = 0f
    var tireSlipFr: Float = 0f
    var tireSlipRl: Float = 0f
    var tireSlipRr: Float = 0f
    var currentSteerAngleRad: Float = 0f
    var wheelRotationAngle: Float = 0f

    val suspensionCompression = FloatArray(4) { 0f }

    private val distFront = wheelbaseM * 0.49f
    private val distRear = wheelbaseM * 0.51f
    private val momentOfInertiaZ = massKg * (wheelbaseM * wheelbaseM * 0.25f)
    private val gravity = 9.81f
    private var lastNearestWaypointIdx: Int = 0

    fun update(dt: Float, waypoints: List<Vector3f> = emptyList(), roadHalfWidth: Float = 7.0f) {
        val clampedDt = dt.coerceIn(0.001f, 0.035f)

        // 1. Track Proximity, Elevation Following & Barrier Collisions
        hasCollidedBarrier = false
        var surfaceGrip = 1.0f

        if (waypoints.isNotEmpty()) {
            val (nearestDist, normalX, normalZ, trackElevY, nearestIdx) = calculateTrackMetrics(waypoints)
            lastNearestWaypointIdx = nearestIdx

            // Smooth elevation following (tracks climbing and descending)
            val targetY = trackElevY + 0.35f
            position.y += (targetY - position.y) * (clampedDt * 20f).coerceAtMost(1.0f)

            if (nearestDist > roadHalfWidth) {
                isOffRoad = true
                surfaceGrip = 0.55f // 45% grip reduction on grass/gravel
            } else {
                isOffRoad = false
            }

            // Outer Armco barrier collision check
            val barrierDistance = roadHalfWidth + 3.1f
            if (nearestDist > barrierDistance) {
                hasCollidedBarrier = true
                forwardVelocity *= 0.70f // Speed drop from collision friction
                lateralVelocity = -lateralVelocity * 0.45f // Elastic rebound
                position.x -= normalX * (nearestDist - barrierDistance) * 0.5f
                position.z -= normalZ * (nearestDist - barrierDistance) * 0.5f
            }
        }

        // 2. Speed-sensitive steering angle (Ackermann base)
        val speedMagnitude = abs(forwardVelocity)
        speedKmh = speedMagnitude * 3.6f

        val speedSteerFactor = (1.0f / (1.0f + (speedMagnitude / 36f))).coerceIn(0.28f, 1.0f)
        val targetSteer = steerInput * maxSteerAngleRad * speedSteerFactor

        val steerRate = 8.5f
        currentSteerAngleRad += (targetSteer - currentSteerAngleRad) * (clampedDt * steerRate)

        // 3. Dynamic normal loads & Weight transfer
        val totalStaticLoad = massKg * gravity
        val frontStaticLoad = totalStaticLoad * (distRear / wheelbaseM)
        val rearStaticLoad = totalStaticLoad * (distFront / wheelbaseM)

        val longTransfer = (massKg * gForceLongitudinal * cgHeightM) / wheelbaseM
        val latTransfer = (massKg * gForceLateral * cgHeightM) / trackWidthM

        // Aerodynamic downforce F_down = 0.5 * rho * Cl * A * v^2
        val aeroDownforce = 0.5f * 1.225f * downforceCoeff * 2.15f * (speedMagnitude * speedMagnitude)
        val frontDownforce = aeroDownforce * 0.45f
        val rearDownforce = aeroDownforce * 0.55f

        val fzFront = max(200f, (frontStaticLoad - longTransfer + frontDownforce) * 0.5f)
        val fzRear = max(200f, (rearStaticLoad + longTransfer + rearDownforce) * 0.5f)

        // 4. Drive Traction from Powertrain
        val boostMultiplier = if (boostActive) 1.45f else 1.0f
        val engineTractionN = powertrain.update(
            throttle * boostMultiplier,
            forwardVelocity,
            clampedDt
        )

        val aeroDragN = 0.5f * 1.225f * dragCoeff * 2.15f * (forwardVelocity * abs(forwardVelocity))
        val rollingCoeff = if (isOffRoad) 0.08f else 0.015f
        val rollingResistanceN = rollingCoeff * massKg * gravity * (if (forwardVelocity >= 0) 1f else -1f)

        val maxBrakeN = massKg * gravity * 1.35f
        val brakeForceN = brake * maxBrakeN

        var driveRearN = engineTractionN
        if (handbrake) {
            driveRearN = 0f
        }

        val netLongitudinalForce = driveRearN - (brakeForceN * (if (forwardVelocity >= 0) 1f else -1f)) - aeroDragN - rollingResistanceN

        // 5. Lateral Tire Slip & Pacejka Magic Formula Forces
        val safeSpeed = max(1.2f, abs(forwardVelocity))
        val alphaFront = (atan2(lateralVelocity + distFront * yawRate, safeSpeed) - currentSteerAngleRad)
        val alphaRear = atan2(lateralVelocity - distRear * yawRate, safeSpeed)

        var fyFront = -tireModel.calculateLateralForce(alphaFront, fzFront * 2f, surfaceGrip)
        var fyRear = -tireModel.calculateLateralForce(alphaRear, fzRear * 2f, surfaceGrip)

        if (handbrake) {
            fyRear *= 0.18f
            tireSlipRl = 0.95f
            tireSlipRr = 0.95f
        } else {
            tireSlipRl = (abs(alphaRear) * 2.2f).coerceIn(0f, 1f)
            tireSlipRr = (abs(alphaRear) * 2.2f).coerceIn(0f, 1f)
        }
        tireSlipFl = (abs(alphaFront) * 2.2f).coerceIn(0f, 1f)
        tireSlipFr = (abs(alphaFront) * 2.2f).coerceIn(0f, 1f)

        // 6. Equations of Motion
        val accelLong = (netLongitudinalForce / massKg) + (lateralVelocity * yawRate)
        val accelLat = ((fyFront * cos(currentSteerAngleRad) + fyRear) / massKg) - (forwardVelocity * yawRate)
        val torqueYaw = (distFront * fyFront * cos(currentSteerAngleRad)) - (distRear * fyRear)
        val angularAccelYaw = torqueYaw / momentOfInertiaZ

        forwardVelocity += accelLong * clampedDt
        lateralVelocity += accelLat * clampedDt
        yawRate += angularAccelYaw * clampedDt

        // Damping when stationary
        if (abs(forwardVelocity) < 0.15f && throttle == 0f && brake > 0.1f) {
            forwardVelocity = 0f
            lateralVelocity = 0f
            yawRate = 0f
        }

        // 7. World Position & Orientation Integration
        headingAngleRad += yawRate * clampedDt

        val sinYaw = sin(headingAngleRad)
        val cosYaw = cos(headingAngleRad)

        val worldVx = forwardVelocity * sinYaw + lateralVelocity * cosYaw
        val worldVz = forwardVelocity * cosYaw - lateralVelocity * sinYaw

        position.x += worldVx * clampedDt
        position.z += worldVz * clampedDt

        rollAngleRad = (-gForceLateral * 0.05f).coerceIn(-0.08f, 0.08f)
        pitchAngleRad = (-gForceLongitudinal * 0.04f).coerceIn(-0.06f, 0.06f)

        suspensionCompression[0] = -pitchAngleRad * 0.5f - rollAngleRad * 0.5f
        suspensionCompression[1] = -pitchAngleRad * 0.5f + rollAngleRad * 0.5f
        suspensionCompression[2] = pitchAngleRad * 0.5f - rollAngleRad * 0.5f
        suspensionCompression[3] = pitchAngleRad * 0.5f + rollAngleRad * 0.5f

        wheelRotationAngle += (forwardVelocity / 0.35f) * clampedDt

        gForceLongitudinal = accelLong / gravity
        gForceLateral = accelLat / gravity

        // Drift Angle
        val velocityAngle = atan2(worldVx, worldVz)
        var angleDiff = (velocityAngle - headingAngleRad)
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()
        driftAngleDeg = (angleDiff * (180f / PI.toFloat()))
    }

    private data class TrackMetric(val lateralDist: Float, val normalX: Float, val normalZ: Float, val elevY: Float, val index: Int)

    private fun calculateTrackMetrics(waypoints: List<Vector3f>): TrackMetric {
        val count = waypoints.size
        var minDistanceSq = Float.MAX_VALUE
        var nearestIdx = lastNearestWaypointIdx

        // Windowed search around last nearest waypoint index
        for (offset in -8..16) {
            val idx = (lastNearestWaypointIdx + offset + count) % count
            val wp = waypoints[idx]
            val dx = position.x - wp.x
            val dz = position.z - wp.z
            val dSq = dx * dx + dz * dz
            if (dSq < minDistanceSq) {
                minDistanceSq = dSq
                nearestIdx = idx
            }
        }

        // Fallback full search if far away
        if (minDistanceSq > 2500f) {
            for (i in waypoints.indices) {
                val wp = waypoints[i]
                val dx = position.x - wp.x
                val dz = position.z - wp.z
                val dSq = dx * dx + dz * dz
                if (dSq < minDistanceSq) {
                    minDistanceSq = dSq
                    nearestIdx = i
                }
            }
        }

        val nearest = waypoints[nearestIdx]
        val next = waypoints[(nearestIdx + 1) % count]

        val tx = next.x - nearest.x
        val tz = next.z - nearest.z
        val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.01f)
        val normalX = -(tz / tLen)
        val normalZ = (tx / tLen)

        val cx = position.x - nearest.x
        val cz = position.z - nearest.z

        val lateralDist = abs(cx * normalX + cz * normalZ)
        return TrackMetric(lateralDist, normalX, normalZ, nearest.y, nearestIdx)
    }

    fun resetAt(pos: Vector3f, headingRad: Float) {
        position.set(pos)
        headingAngleRad = headingRad
        forwardVelocity = 0f
        lateralVelocity = 0f
        yawRate = 0f
        pitchAngleRad = 0f
        rollAngleRad = 0f
        throttle = 0f
        brake = 0f
        steerInput = 0f
        handbrake = false
        boostActive = false
        speedKmh = 0f
        isOffRoad = false
        hasCollidedBarrier = false
        powertrain.currentGear = 2
        powertrain.currentRpm = powertrain.idleRpm
    }
}
