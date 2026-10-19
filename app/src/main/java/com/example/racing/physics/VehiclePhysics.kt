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
    var isOnKerb: Boolean = false
    var currentSurfaceGrip: Float = 1.0f
    var hasCollidedBarrier: Boolean = false
    var tireSlipFl: Float = 0f
    var tireSlipFr: Float = 0f
    var tireSlipRl: Float = 0f
    var tireSlipRr: Float = 0f
    var wheelSpinFl: Float = 0f
    var wheelSpinFr: Float = 0f
    var wheelSpinRl: Float = 0f
    var wheelSpinRr: Float = 0f
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

    fun update(
        dt: Float,
        waypoints: List<Vector3f> = emptyList(),
        roadHalfWidth: Float = 7.0f,
        baseSurfaceGrip: Float = 1.0f
    ) {
        val clampedDt = dt.coerceIn(0.001f, 0.50f)

        // 1. Track Proximity, Elevation Following & Road Surface Zones
        hasCollidedBarrier = false
        val effectiveBaseGrip = baseSurfaceGrip.coerceIn(0.35f, 1.30f)

        if (waypoints.isNotEmpty()) {
            val (nearestDist, normalX, normalZ, trackElevY, nearestIdx) = calculateTrackMetrics(waypoints)
            lastNearestWaypointIdx = nearestIdx
            trackLateralDistance = nearestDist

            // Smooth elevation following (tracks climbing and descending)
            val targetY = trackElevY + 0.35f
            position.y += (targetY - position.y) * (clampedDt * 20f).coerceAtMost(1.0f)

            // Dynamic Road Surface Zones: Infield (Grass/Gravel), Rumble Strip (Kerb), Main Track
            val targetGrip: Float
            if (nearestDist > roadHalfWidth) {
                isOffRoad = true
                isOnKerb = false
                targetGrip = 0.52f * effectiveBaseGrip // Significant off-road grip penalty
            } else if (nearestDist in (roadHalfWidth - 1.5f)..roadHalfWidth) {
                isOffRoad = false
                isOnKerb = true
                targetGrip = 0.88f * effectiveBaseGrip // Slight grip reduction on painted kerbs
            } else {
                isOffRoad = false
                isOnKerb = false
                targetGrip = 1.0f * effectiveBaseGrip // Optimal track asphalt grip
            }

            // Smooth physical grip transition rate (eliminates jarring grip discontinuities)
            currentSurfaceGrip += (targetGrip - currentSurfaceGrip) * (clampedDt * 12f).coerceAtMost(1.0f)

            // Outer Armco barrier collision check
            val barrierDistance = roadHalfWidth + 3.1f
            if (nearestDist > barrierDistance) {
                hasCollidedBarrier = true
                val penetration = nearestDist - barrierDistance
                // Fully separate vehicle from barrier: move vehicle back onto allowed track boundary
                position.x -= normalX * (penetration * 0.98f)
                position.z -= normalZ * (penetration * 0.98f)

                // Velocity reflection and contact friction along barrier normal
                val sinYaw = sin(headingAngleRad)
                val cosYaw = cos(headingAngleRad)
                val worldVx = forwardVelocity * sinYaw + lateralVelocity * cosYaw
                val worldVz = forwardVelocity * cosYaw - lateralVelocity * sinYaw

                // Velocity component pointing outward into barrier
                val velDotNormal = worldVx * normalX + worldVz * normalZ
                if (velDotNormal > 0f) {
                    val bounce = 0.22f
                    val newWorldVx = worldVx - (1f + bounce) * velDotNormal * normalX
                    val newWorldVz = worldVz - (1f + bounce) * velDotNormal * normalZ

                    val frictionFactor = 0.85f
                    val tangentialVx = (newWorldVx - (-velDotNormal * bounce * normalX)) * frictionFactor
                    val tangentialVz = (newWorldVz - (-velDotNormal * bounce * normalZ)) * frictionFactor

                    val finalVx = tangentialVx + (-velDotNormal * bounce * normalX)
                    val finalVz = tangentialVz + (-velDotNormal * bounce * normalZ)

                    forwardVelocity = (finalVx * sinYaw + finalVz * cosYaw) * 0.92f
                    lateralVelocity = (finalVx * cosYaw - finalVz * sinYaw) * 0.80f
                } else {
                    forwardVelocity *= 0.88f
                    lateralVelocity *= 0.60f
                }
            }
        } else {
            currentSurfaceGrip = effectiveBaseGrip
            isOffRoad = false
            isOnKerb = false
        }

        // 2. Speed-Sensitive Progressive Steering Model & Dynamic Self-Centering
        val speedMagnitude = abs(forwardVelocity)
        speedKmh = speedMagnitude * 3.6f

        // Progressive speed attenuation (full steering lock at low speed, tight high-speed tracking)
        val speedSteerFactor = (1.0f / (1.0f + (speedMagnitude / 34f))).coerceIn(0.28f, 1.0f)
        var targetSteer = steerInput * maxSteerAngleRad * speedSteerFactor

        // Counter-steer drift assist: allow extra opposite steering authority when sliding
        val isOversteering = (abs(driftAngleDeg) > 7f && forwardVelocity > 4f)
        if (isOversteering) {
            val isCounterSteering = (steerInput * driftAngleDeg > 0.05f)
            if (isCounterSteering) {
                targetSteer *= 1.35f
            }
        }

        // Asymmetric steer rate: faster caster self-centering when returning to neutral
        val steerRate = if (abs(targetSteer) < abs(currentSteerAngleRad)) 14.5f else 9.2f
        currentSteerAngleRad += (targetSteer - currentSteerAngleRad) * (clampedDt * steerRate).coerceAtMost(1.0f)

        // Automatic Reverse Shifting:
        // Hold brake while stationary for at least 0.75s to engage Reverse; in Reverse, throttle shifts back to 1st
        var effectiveThrottle = throttle
        var effectiveBrake = brake

        if (powertrain.currentGear == 0) { // Reverse gear active
            if (throttle > 0.15f) {
                // Throttle shifts back to 1st gear and drives forward
                powertrain.currentGear = 2
                powertrain.shiftTimer = 0.18f
                reverseHoldTimer = 0f
                effectiveThrottle = throttle
                effectiveBrake = 0f
            } else if (brake > 0.05f) {
                // In reverse gear, brake pedal applies reverse throttle
                effectiveThrottle = brake
                effectiveBrake = 0f
            } else {
                // No pedal in reverse: coast with light engine braking
                effectiveThrottle = 0f
                effectiveBrake = 0.1f
            }
        } else { // Forward gears
            if (abs(forwardVelocity) < 0.25f && brake > 0.5f && throttle == 0f) {
                // Stationary with brake applied: accumulate hold timer
                reverseHoldTimer += clampedDt
                if (reverseHoldTimer >= 0.75f) {
                    // Firm brake held while stopped for >= 0.75s: engage Reverse gear
                    powertrain.currentGear = 0
                    powertrain.shiftTimer = 0.18f
                    effectiveThrottle = brake
                    effectiveBrake = 0f
                } else {
                    // Still holding stationary: apply mechanical brakes
                    effectiveThrottle = 0f
                    effectiveBrake = brake
                }
            } else {
                reverseHoldTimer = 0f
            }
        }

        // 3. Dynamic Normal Loads & 4-Corner Weight Transfer
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

        // 4. Drive Traction, Engine Braking & Mechanical Braking per Wheel
        val boostMultiplier = if (boostActive) 1.45f else 1.0f
        val engineTractionN = powertrain.update(
            effectiveThrottle * boostMultiplier,
            forwardVelocity,
            clampedDt
        )

        // Realistic Engine Braking (vacuum drag on throttle release in gear)
        val engineBrakingN: Float
        if (effectiveThrottle < 0.05f && abs(forwardVelocity) > 0.4f && powertrain.currentGear > 1) {
            val gearRatioNorm = abs(powertrain.gearRatios[powertrain.currentGear]) / 3.82f
            val rpmFactor = (powertrain.currentRpm / powertrain.redlineRpm).coerceIn(0.2f, 1.0f)
            val dir = if (forwardVelocity > 0f) -1f else 1f
            engineBrakingN = dir * massKg * 1.4f * gearRatioNorm * rpmFactor
        } else {
            engineBrakingN = 0f
        }

        // Drive distribution per wheel
        val driveFrontWheelN = (engineTractionN * driveFrontRatio) * 0.5f
        val driveRearWheelN = (if (handbrake) 0f else (engineTractionN * (1.0f - driveFrontRatio))) * 0.5f

        // Mechanical Brakes per wheel with realistic 62/38 front brake bias
        val maxBrakeN = massKg * gravity * 1.30f
        val totalBrakeDemandN = effectiveBrake * maxBrakeN
        val brakeFrontWheelDemandN = totalBrakeDemandN * 0.62f * 0.5f
        val brakeRearWheelDemandN = if (handbrake) (fzRL + fzRR) * 0.5f * 1.25f else totalBrakeDemandN * 0.38f * 0.5f

        // Travel direction for friction opposition
        val travelSign = if (forwardVelocity > 0.08f) 1f else if (forwardVelocity < -0.08f) -1f else 0f

        val demandFL = driveFrontWheelN - brakeFrontWheelDemandN * travelSign
        val demandFR = driveFrontWheelN - brakeFrontWheelDemandN * travelSign
        val demandRL = driveRearWheelN - brakeRearWheelDemandN * travelSign
        val demandRR = driveRearWheelN - brakeRearWheelDemandN * travelSign

        // 5. Kamm Friction Circle / Ellipse Combined Adhesion per Wheel
        val slipFL = tireModel.calculateCombinedAdhesion(demandFL, fzFL, currentSurfaceGrip)
        val slipFR = tireModel.calculateCombinedAdhesion(demandFR, fzFR, currentSurfaceGrip)
        val slipRL = tireModel.calculateCombinedAdhesion(demandRL, fzRL, currentSurfaceGrip)
        val slipRR = tireModel.calculateCombinedAdhesion(demandRR, fzRR, currentSurfaceGrip)

        wheelSpinFl = slipFL.slipRatio
        wheelSpinFr = slipFR.slipRatio
        wheelSpinRl = slipRL.slipRatio
        wheelSpinRr = slipRR.slipRatio

        val netWheelDriveBrakeForce = slipFL.actualFx + slipFR.actualFx + slipRL.actualFx + slipRR.actualFx + engineBrakingN
        val rollingCoeff = if (isOffRoad) 0.08f else 0.015f
        val rollingResistanceMagnitude = rollingCoeff * massKg * gravity
        val aeroDragN = 0.5f * 1.225f * dragCoeff * 2.15f * (forwardVelocity * abs(forwardVelocity))

        // Static Hold / Passive Resistance:
        // These forces oppose motion and cannot create reverse movement when stationary
        val resistiveForceN: Float
        if (abs(forwardVelocity) > 0.08f) {
            val travelDir = if (forwardVelocity > 0f) 1f else -1f
            resistiveForceN = rollingResistanceMagnitude * travelDir
        } else {
            // Near-zero velocity (holding on grid or stopped at standstill):
            // Static resistance balances drive force up to available friction limit
            val staticHoldLimitN = (effectiveBrake * maxBrakeN) + rollingResistanceMagnitude
            if (abs(netWheelDriveBrakeForce) <= staticHoldLimitN) {
                // Static hold: perfectly balances drive force; zero net force prevents rolling backwards
                resistiveForceN = netWheelDriveBrakeForce
                forwardVelocity = 0f
            } else {
                val driveDir = if (netWheelDriveBrakeForce > 0f) 1f else -1f
                resistiveForceN = staticHoldLimitN * driveDir
            }
        }

        val netLongitudinalForce = netWheelDriveBrakeForce - resistiveForceN - aeroDragN

        // 6. Lateral Slip & Cornering Forces with Combined Friction Ellipse
        val safeSpeed = max(1.2f, abs(forwardVelocity))
        val alphaFront = (atan2(lateralVelocity + distFront * yawRate, safeSpeed) - currentSteerAngleRad)
        val alphaRear = atan2(lateralVelocity - distRear * yawRate, safeSpeed)

        // Cornering forces per wheel coupled with remaining lateral friction capacity from Kamm circle
        val rawFyFL = -tireModel.calculateLateralForce(alphaFront, fzFL, currentSurfaceGrip) * slipFL.lateralCapacityFactor
        val rawFyFR = -tireModel.calculateLateralForce(alphaFront, fzFR, currentSurfaceGrip) * slipFR.lateralCapacityFactor
        var rawFyRL = -tireModel.calculateLateralForce(alphaRear, fzRL, currentSurfaceGrip) * slipRL.lateralCapacityFactor
        var rawFyRR = -tireModel.calculateLateralForce(alphaRear, fzRR, currentSurfaceGrip) * slipRR.lateralCapacityFactor

        if (handbrake) {
            rawFyRL *= 0.18f
            rawFyRR *= 0.18f
        }

        // Low-speed kinematic blend to prevent twitching/singularity when stopping or creeping
        val lowSpeedBlend = (safeSpeed / 2.2f).coerceIn(0.20f, 1.0f)
        val fyFront = (rawFyFL + rawFyFR) * lowSpeedBlend
        val fyRear = (rawFyRL + rawFyRR) * lowSpeedBlend

        // Comprehensive slip telemetry (lateral slip angle + longitudinal wheelspin/lockup)
        tireSlipFl = (abs(alphaFront) * 2.2f + slipFL.slipRatio).coerceIn(0f, 1f)
        tireSlipFr = (abs(alphaFront) * 2.2f + slipFR.slipRatio).coerceIn(0f, 1f)
        tireSlipRl = (abs(alphaRear) * 2.2f + slipRL.slipRatio + (if (handbrake) 0.85f else 0f)).coerceIn(0f, 1f)
        tireSlipRr = (abs(alphaRear) * 2.2f + slipRR.slipRatio + (if (handbrake) 0.85f else 0f)).coerceIn(0f, 1f)

        // 7. Equations of Motion with Stabilizing Yaw Damping during Counter-Steer
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

        // Damping when stationary: prevent creep, jitter, or unintended drift at standstill
        if (abs(forwardVelocity) < 0.12f && (effectiveBrake > 0.05f || (effectiveThrottle == 0f && abs(netWheelDriveBrakeForce) < 120f))) {
            forwardVelocity = 0f
            lateralVelocity = 0f
            yawRate = 0f
        }

        // 8. World Position & Orientation Integration
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

        // Micro-suspension rumble excitation when riding painted rumble strips / kerbs
        if (isOnKerb && speedMagnitude > 4f) {
            val kerbExcitation = sin(wheelRotationAngle * 14f) * 0.015f
            suspensionCompression[0] += kerbExcitation
            suspensionCompression[1] -= kerbExcitation
            suspensionCompression[2] += kerbExcitation
            suspensionCompression[3] -= kerbExcitation
        }

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
        val window = if (count > 25 && lastNearestWaypointIdx in 0 until count) -12..24 else 0 until count
        for (offset in window) {
            val i = ((lastNearestWaypointIdx + offset) % count + count) % count
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
        isOnKerb = false
        currentSurfaceGrip = 1.0f
        hasCollidedBarrier = false
        reverseHoldTimer = 0f
        trackLateralDistance = 0f
        lastNearestWaypointIdx = 0
        wheelSpinFl = 0f
        wheelSpinFr = 0f
        wheelSpinRl = 0f
        wheelSpinRr = 0f
        tireSlipFl = 0f
        tireSlipFr = 0f
        tireSlipRl = 0f
        tireSlipRr = 0f
        powertrain.currentGear = 2
        powertrain.currentRpm = powertrain.idleRpm
    }
}
