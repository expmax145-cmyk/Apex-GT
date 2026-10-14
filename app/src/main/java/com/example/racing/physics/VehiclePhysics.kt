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
    val driveFrontRatio: Float = 0.0f,
    val tireModel: TireModel = TireModel(),
    val powertrain: EnginePowertrain = EnginePowertrain()
) {
    // World space position & orientations
    val position = Vector3f(0f, 0.35f, 0f)
    var headingAngleRad: Float = 0f
    var pitchAngleRad: Float = 0f
    var rollAngleRad: Float = 0f
    var pitchVelocity: Float = 0f
    var rollVelocity: Float = 0f

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
    var reverseHoldTimer: Float = 0f
    var trackLateralDistance: Float = 0f

    fun update(dt: Float, waypoints: List<Vector3f> = emptyList(), roadHalfWidth: Float = 7.0f) {
        val clampedDt = dt.coerceIn(0.001f, 0.035f)

        // 1. Track Proximity, Elevation Following & Barrier Collisions
        hasCollidedBarrier = false
        var surfaceGrip = 1.0f

        if (waypoints.isNotEmpty()) {
            val (nearestDist, normalX, normalZ, trackElevY, nearestIdx) = calculateTrackMetrics(waypoints)
            lastNearestWaypointIdx = nearestIdx
            trackLateralDistance = nearestDist

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
                forwardVelocity *= 0.82f // Friction dissipation from barrier contact
                lateralVelocity *= 0.40f // Dampen lateral velocity without elastic snap oscillation
                val penetration = (nearestDist - barrierDistance).coerceAtMost(2.0f)
                // normalX and normalZ point from track to vehicle; subtracting moves vehicle back onto track
                position.x -= normalX * (penetration * 0.45f)
                position.z -= normalZ * (penetration * 0.45f)
            }
        }

        // 2. Speed-sensitive steering angle with dynamic self-centering and counter-steer assist
        val speedMagnitude = abs(forwardVelocity)
        speedKmh = speedMagnitude * 3.6f

        val speedSteerFactor = (1.0f / (1.0f + (speedMagnitude / 38f))).coerceIn(0.32f, 1.0f)
        var targetSteer = steerInput * maxSteerAngleRad * speedSteerFactor

        // Counter-steer drift assist: when the tail kicks out into a slide, allow extra steering lock in opposite direction
        val isOversteering = (abs(driftAngleDeg) > 8f && forwardVelocity > 5f)
        if (isOversteering) {
            val counterSteerSign = if (driftAngleDeg > 0f) -1f else 1f
            if (steerInput * counterSteerSign > 0.1f) {
                // Driver is steering into the slide (counter-steering): extend steering authority
                targetSteer *= 1.35f
            }
        }

        // Asymmetric steer rate: faster self-centering when returning to neutral
        val steerRate = if (abs(targetSteer) < abs(currentSteerAngleRad)) 14.0f else 8.8f
        currentSteerAngleRad += (targetSteer - currentSteerAngleRad) * (clampedDt * steerRate).coerceAtMost(1.0f)

        // Automatic Reverse Shifting:
        // Hold brake while stationary for > 0.5s to engage Reverse; in Reverse, throttle shifts back to 1st
        var effectiveThrottle = throttle
        var effectiveBrake = brake

        if (powertrain.currentGear == 0) { // Reverse gear active
            if (throttle > 0.1f) {
                // Throttle shifts back to 1st gear and drives forward
                powertrain.currentGear = 2
                powertrain.shiftTimer = 0.15f
                reverseHoldTimer = 0f
                effectiveThrottle = throttle
                effectiveBrake = 0f
            } else if (brake > 0.05f) {
                // In reverse gear, brake pedal applies reverse throttle
                effectiveThrottle = brake
                effectiveBrake = 0f
            }
        } else { // Forward gears
            if (abs(forwardVelocity) < 0.20f && brake > 0.4f && throttle == 0f) {
                // When stopped and applying brake, engage Reverse gear
                powertrain.currentGear = 0
                powertrain.shiftTimer = 0.15f
                effectiveThrottle = brake
                effectiveBrake = 0f
            }
        }

        // 3. Dynamic normal loads & 4-Corner Weight Transfer
        val totalStaticLoad = massKg * gravity
        val frontStaticLoad = totalStaticLoad * (distRear / wheelbaseM)
        val rearStaticLoad = totalStaticLoad * (distFront / wheelbaseM)

        val longTransfer = (massKg * gForceLongitudinal * cgHeightM) / wheelbaseM
        val latTransfer = (massKg * gForceLateral * cgHeightM) / trackWidthM

        // Aerodynamic downforce F_down = 0.5 * rho * Cl * A * v^2
        val aeroDownforce = 0.5f * 1.225f * downforceCoeff * 2.15f * (speedMagnitude * speedMagnitude)
        val frontDownforce = aeroDownforce * 0.45f
        val rearDownforce = aeroDownforce * 0.55f

        val totalFzFront = max(400f, frontStaticLoad - longTransfer + frontDownforce)
        val totalFzRear = max(400f, rearStaticLoad + longTransfer + rearDownforce)

        // Lateral weight transfer per axle
        val latTransferFront = latTransfer * 0.52f
        val latTransferRear = latTransfer * 0.48f

        val fzFL = max(150f, totalFzFront * 0.5f - latTransferFront)
        val fzFR = max(150f, totalFzFront * 0.5f + latTransferFront)
        val fzRL = max(150f, totalFzRear * 0.5f - latTransferRear)
        val fzRR = max(150f, totalFzRear * 0.5f + latTransferRear)

        // 4. Drive Traction from Powertrain with Drivetrain Distribution
        val boostMultiplier = if (boostActive) 1.45f else 1.0f
        val engineTractionN = powertrain.update(
            effectiveThrottle * boostMultiplier,
            forwardVelocity,
            clampedDt
        )

        val driveFrontN = engineTractionN * driveFrontRatio
        val driveRearN = if (handbrake) 0f else (engineTractionN * (1.0f - driveFrontRatio))

        val aeroDragN = 0.5f * 1.225f * dragCoeff * 2.15f * (forwardVelocity * abs(forwardVelocity))
        val rollingCoeff = if (isOffRoad) 0.08f else 0.015f
        val rollingResistanceN = rollingCoeff * massKg * gravity * (if (forwardVelocity >= 0) 1f else -1f)

        val maxBrakeN = massKg * gravity * 1.35f
        val brakeForceN = effectiveBrake * maxBrakeN

        val netLongitudinalForce = (driveFrontN + driveRearN) - 
            (brakeForceN * (if (forwardVelocity >= 0) 1f else -1f)) - 
            aeroDragN - rollingResistanceN

        // 5. Lateral Tire Slip & Pacejka Magic Formula Forces
        val safeSpeed = max(1.2f, abs(forwardVelocity))
        val alphaFront = (atan2(lateralVelocity + distFront * yawRate, safeSpeed) - currentSteerAngleRad)
        val alphaRear = atan2(lateralVelocity - distRear * yawRate, safeSpeed)

        // Cornering forces per wheel
        val fyFL = -tireModel.calculateLateralForce(alphaFront, fzFL, surfaceGrip)
        val fyFR = -tireModel.calculateLateralForce(alphaFront, fzFR, surfaceGrip)
        val fyFront = fyFL + fyFR

        var fyRL = -tireModel.calculateLateralForce(alphaRear, fzRL, surfaceGrip)
        var fyRR = -tireModel.calculateLateralForce(alphaRear, fzRR, surfaceGrip)

        // Friction circle coupling on rear driven wheels (power oversteer)
        if (abs(driveRearN) > 100f && !handbrake) {
            val maxRearGripN = (fzRL + fzRR) * tireModel.dPeakFriction * surfaceGrip
            val driveRatio = (abs(driveRearN) / maxRearGripN.coerceAtLeast(100f)).coerceIn(0f, 0.85f)
            val lateralReduction = sqrt(1.0f - driveRatio * driveRatio)
            fyRL *= lateralReduction
            fyRR *= lateralReduction
        }

        if (handbrake) {
            fyRL *= 0.18f
            fyRR *= 0.18f
            tireSlipRl = 0.98f
            tireSlipRr = 0.98f
        } else {
            val rearTractionSlip = (abs(driveRearN) / (totalFzRear * 1.25f)).coerceIn(0f, 0.45f)
            tireSlipRl = ((abs(alphaRear) * 2.2f) + rearTractionSlip).coerceIn(0f, 1f)
            tireSlipRr = ((abs(alphaRear) * 2.2f) + rearTractionSlip).coerceIn(0f, 1f)
        }
        tireSlipFl = (abs(alphaFront) * 2.2f).coerceIn(0f, 1f)
        tireSlipFr = (abs(alphaFront) * 2.2f).coerceIn(0f, 1f)

        val fyRear = fyRL + fyRR

        // 6. Equations of Motion with stabilizing yaw damping during counter-steer
        val accelLong = (netLongitudinalForce / massKg) + (lateralVelocity * yawRate)
        val accelLat = ((fyFront * cos(currentSteerAngleRad) + fyRear) / massKg) - (forwardVelocity * yawRate)
        var torqueYaw = (distFront * fyFront * cos(currentSteerAngleRad)) - (distRear * fyRear)

        // Counter-steer yaw stabilization (prevents snap spins on counter-steering)
        if (isOversteering && (steerInput * yawRate < 0f)) {
            val yawDampingNm = yawRate * (massKg * 4.2f)
            torqueYaw -= yawDampingNm
        }

        val angularAccelYaw = torqueYaw / momentOfInertiaZ

        forwardVelocity += accelLong * clampedDt
        lateralVelocity += accelLat * clampedDt
        yawRate += angularAccelYaw * clampedDt

        // Damping when stationary
        if (abs(forwardVelocity) < 0.15f && effectiveThrottle == 0f && effectiveBrake > 0.1f) {
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

        // 2nd-Order Spring-Damper Suspended Mass Dynamics for Chassis Pitch & Roll
        val targetPitch = (-gForceLongitudinal * 0.045f).coerceIn(-0.075f, 0.075f)
        val targetRoll = (-gForceLateral * 0.055f).coerceIn(-0.095f, 0.095f)

        val pitchSpringRate = 34.0f
        val pitchDampingRate = 12.0f
        pitchVelocity += ((targetPitch - pitchAngleRad) * pitchSpringRate - pitchVelocity * pitchDampingRate) * clampedDt
        pitchAngleRad += pitchVelocity * clampedDt

        val rollSpringRate = 38.0f
        val rollDampingRate = 12.5f
        rollVelocity += ((targetRoll - rollAngleRad) * rollSpringRate - rollVelocity * rollDampingRate) * clampedDt
        rollAngleRad += rollVelocity * clampedDt

        // 4-corner visual suspension compression
        suspensionCompression[0] = (-pitchAngleRad * 0.45f - rollAngleRad * 0.45f).coerceIn(-0.06f, 0.06f)
        suspensionCompression[1] = (-pitchAngleRad * 0.45f + rollAngleRad * 0.45f).coerceIn(-0.06f, 0.06f)
        suspensionCompression[2] = (pitchAngleRad * 0.45f - rollAngleRad * 0.45f).coerceIn(-0.06f, 0.06f)
        suspensionCompression[3] = (pitchAngleRad * 0.45f + rollAngleRad * 0.45f).coerceIn(-0.06f, 0.06f)

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

    data class TrackMetric(val lateralDist: Float, val normalX: Float, val normalZ: Float, val elevY: Float, val index: Int)

    private fun calculateTrackMetrics(waypoints: List<Vector3f>): TrackMetric {
        val count = waypoints.size
        if (count == 0) return TrackMetric(0f, 0f, 0f, position.y, 0)

        var minDistanceSq = Float.MAX_VALUE
        var bestIdx = lastNearestWaypointIdx
        var bestProjX = position.x
        var bestProjZ = position.z
        var bestProjY = position.y

        // 1. Adaptive window search around last nearest waypoint index
        val window = if (lastNearestWaypointIdx in 0 until count) -12..24 else 0 until count
        for (offset in window) {
            val i = (lastNearestWaypointIdx + offset + count) % count
            val p1 = waypoints[i]
            val p2 = waypoints[(i + 1) % count]

            val segDx = p2.x - p1.x
            val segDz = p2.z - p1.z
            val segLenSq = segDx * segDx + segDz * segDz
            val t = if (segLenSq > 0.001f) {
                (((position.x - p1.x) * segDx + (position.z - p1.z) * segDz) / segLenSq).coerceIn(0f, 1f)
            } else 0f

            val projX = p1.x + t * segDx
            val projZ = p1.z + t * segDz
            val dx = position.x - projX
            val dz = position.z - projZ
            val dSq = dx * dx + dz * dz

            if (dSq < minDistanceSq) {
                minDistanceSq = dSq
                bestIdx = i
                bestProjX = projX
                bestProjZ = projZ
                bestProjY = p1.y + t * (p2.y - p1.y)
            }
        }

        // 2. Fallback full search across all segments if vehicle moves far into the infield/center
        // This ensures the vehicle never gets stuck on a stale segment when crossing the track interior
        if (minDistanceSq > 625f) {
            for (i in 0 until count) {
                val p1 = waypoints[i]
                val p2 = waypoints[(i + 1) % count]
                val segDx = p2.x - p1.x
                val segDz = p2.z - p1.z
                val segLenSq = segDx * segDx + segDz * segDz
                val t = if (segLenSq > 0.001f) {
                    (((position.x - p1.x) * segDx + (position.z - p1.z) * segDz) / segLenSq).coerceIn(0f, 1f)
                } else 0f

                val projX = p1.x + t * segDx
                val projZ = p1.z + t * segDz
                val dx = position.x - projX
                val dz = position.z - projZ
                val dSq = dx * dx + dz * dz

                if (dSq < minDistanceSq) {
                    minDistanceSq = dSq
                    bestIdx = i
                    bestProjX = projX
                    bestProjZ = projZ
                    bestProjY = p1.y + t * (p2.y - p1.y)
                }
            }
        }

        val trueDist = sqrt(minDistanceSq)
        val normalX: Float
        val normalZ: Float
        if (trueDist > 0.01f) {
            // Normal vector pointing from track centerline directly towards vehicle position
            normalX = (position.x - bestProjX) / trueDist
            normalZ = (position.z - bestProjZ) / trueDist
        } else {
            // Default perpendicular to segment if right on centerline
            val p1 = waypoints[bestIdx]
            val p2 = waypoints[(bestIdx + 1) % count]
            val segDx = p2.x - p1.x
            val segDz = p2.z - p1.z
            val sLen = sqrt(segDx * segDx + segDz * segDz).coerceAtLeast(0.01f)
            normalX = -segDz / sLen
            normalZ = segDx / sLen
        }

        return TrackMetric(trueDist, normalX, normalZ, bestProjY, bestIdx)
    }

    fun resetAt(pos: Vector3f, headingRad: Float) {
        position.set(pos)
        headingAngleRad = headingRad
        forwardVelocity = 0f
        lateralVelocity = 0f
        yawRate = 0f
        pitchAngleRad = 0f
        rollAngleRad = 0f
        pitchVelocity = 0f
        rollVelocity = 0f
        throttle = 0f
        brake = 0f
        steerInput = 0f
        handbrake = false
        boostActive = false
        speedKmh = 0f
        isOffRoad = false
        hasCollidedBarrier = false
        reverseHoldTimer = 0f
        trackLateralDistance = 0f
        lastNearestWaypointIdx = 0
        powertrain.currentGear = 2
        powertrain.currentRpm = powertrain.idleRpm
    }
}
