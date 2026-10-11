package com.example.racing.ai

import com.example.racing.engine.Vector3f
import com.example.racing.physics.VehiclePhysics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class AIDifficulty(
    val targetSpeedMultiplier: Float,
    val brakeMargin: Float,
    val steerSharpness: Float,
    val mistakeChance: Float
) {
    EASY(0.80f, 1.30f, 0.72f, 0.08f),
    MEDIUM(0.92f, 1.10f, 0.88f, 0.03f),
    EXPERT(1.00f, 0.98f, 1.00f, 0.005f)
}

data class AIDriverProfile(
    val name: String,
    val carColor: FloatArray,
    val aggression: Float, // 0.0 to 1.0
    val difficulty: AIDifficulty,
    val carId: String = "apex_valkyrie",
    val preferredLaneOffset: Float = 0f // Baseline line bias to fan out competitors
)

enum class AIRecoveryState {
    NORMAL,
    REVERSING,
    REALIGNING,
    RESUMING
}

class OpponentAIController(
    val vehicle: VehiclePhysics,
    val profile: AIDriverProfile,
    val waypoints: List<Vector3f>
) {
    var currentWaypointIndex: Int = 0
    var lateralOffset: Float = 0f // Current target lateral offset from centerline
    var currentLap: Int = 1
    var totalLapsCompleted: Int = 0
    var currentLapTimeSec: Float = 0f
    var bestLapTimeSec: Float = 0f
    var lastLapTimeSec: Float = 0f
    var raceFinished: Boolean = false
    var finishTimeSec: Float = 0f
    var cachedMaterial: com.example.racing.engine.Material? = null

    // State machine & humanized behavior
    var recoveryState: AIRecoveryState = AIRecoveryState.NORMAL
    private var recoveryTimer: Float = 0f
    private var mistakeTimer: Float = 0f
    private var mistakeActive: Boolean = false
    private var mistakeOffset: Float = 0f
    private var draftBonusActive: Boolean = false
    private var lastPassedWaypointIndex: Int = 0

    // Random generator seeded per driver name for deterministic personality
    private val rng = Random(profile.name.hashCode())

    fun update(dt: Float, playerPos: Vector3f, otherAIs: List<Vector3f>) {
        if (waypoints.isEmpty()) return

        val count = waypoints.size
        val myPos = vehicle.position

        currentLapTimeSec += dt

        // 1. Find nearest waypoint and track lap progression
        var bestDistSq = Float.MAX_VALUE
        var nearestIdx = currentWaypointIndex
        for (i in -4..8) {
            val idx = (currentWaypointIndex + i + count) % count
            val wp = waypoints[idx]
            val dSq = (wp.x - myPos.x) * (wp.x - myPos.x) + (wp.z - myPos.z) * (wp.z - myPos.z)
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                nearestIdx = idx
            }
        }

        // Lap crossing detection (when crossing from near end of loop to beginning)
        if (lastPassedWaypointIndex > count - 15 && nearestIdx < 15) {
            totalLapsCompleted++
            currentLap++
            lastLapTimeSec = currentLapTimeSec
            if (bestLapTimeSec == 0f || currentLapTimeSec < bestLapTimeSec) {
                bestLapTimeSec = currentLapTimeSec
            }
            currentLapTimeSec = 0f
        }
        lastPassedWaypointIndex = nearestIdx
        currentWaypointIndex = nearestIdx

        // 2. State Machine: Barrier collision & reverse spin recovery
        if (updateRecoveryStateMachine(dt, nearestIdx, count)) {
            // Under recovery, skip normal racing calculations and step physics
            vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
            return
        }

        // 3. Look-ahead distance scaled with speed (from 15m at low speed up to 48m at 220+ km/h)
        val lookaheadDist = (15f + (vehicle.speedKmh * 0.16f)).coerceIn(15f, 48f)
        var accumulatedDist = 0f
        var lookaheadIdx = currentWaypointIndex

        while (accumulatedDist < lookaheadDist) {
            val nextIdx = (lookaheadIdx + 1) % count
            accumulatedDist += waypoints[lookaheadIdx].distanceTo(waypoints[nextIdx])
            lookaheadIdx = nextIdx
        }

        val targetWp = waypoints[lookaheadIdx]

        // 4. Calculate Geometric Racing Line (Outside-Inside-Outside on corners)
        val racingLineOffset = computeRacingLineOffset(currentWaypointIndex, lookaheadIdx, count)

        // 5. Multi-Car Spatial Awareness (Player + All other AI competitors)
        val allCompetitorPositions = mutableListOf<Vector3f>().apply {
            add(playerPos)
            for (pos in otherAIs) {
                if (pos !== myPos && pos.distanceTo(myPos) > 0.05f) {
                    add(pos)
                }
            }
        }

        val dynamicAvoidanceOffset = computeMultiCarAvoidance(
            myPos = myPos,
            competitors = allCompetitorPositions,
            baseOffset = racingLineOffset + profile.preferredLaneOffset
        )

        // 6. Occasional Human-like Driver Mistakes
        updateDriverMistakes(dt)
        val finalTargetOffset = (dynamicAvoidanceOffset + mistakeOffset).coerceIn(-5.5f, 5.5f)

        // Smooth lateral steering target transition
        lateralOffset += (finalTargetOffset - lateralOffset) * (dt * 3.5f).coerceIn(0.05f, 0.45f)

        // Project lateral offset perpendicular to track segment
        val nextWp = waypoints[(lookaheadIdx + 1) % count]
        val trackDx = nextWp.x - targetWp.x
        val trackDz = nextWp.z - targetWp.z
        val trackLen = sqrt(trackDx * trackDx + trackDz * trackDz).coerceAtLeast(0.01f)
        val normalX = -(trackDz / trackLen)
        val normalZ = (trackDx / trackLen)

        val targetX = targetWp.x + normalX * lateralOffset
        val targetZ = targetWp.z + normalZ * lateralOffset

        // 7. Steering Calculation with Ackermann & Speed-Sensitive Sharpness
        val dx = targetX - myPos.x
        val dz = targetZ - myPos.z
        val targetAngle = atan2(dx, dz)

        var angleDiff = targetAngle - vehicle.headingAngleRad
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

        val steerSensitivity = profile.difficulty.steerSharpness * (if (profile.carId == "apex_spirit_86") 1.15f else 1.0f)
        val steerTarget = (angleDiff * 2.3f * steerSensitivity).coerceIn(-1f, 1f)
        vehicle.steerInput = steerTarget

        // 8. Corner Curvature & Intelligent Braking Points
        val targetSpeedKmh = computeTargetSpeed(currentWaypointIndex, count)

        // 9. Progressive Throttle & Braking Application with Draft Boost
        applyThrottleAndBrakes(targetSpeedKmh)

        // 10. Advance Vehicle Physics with Road Boundaries
        vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
    }

    private fun updateRecoveryStateMachine(dt: Float, nearestIdx: Int, count: Int): Boolean {
        val forwardX = sin(vehicle.headingAngleRad)
        val forwardZ = cos(vehicle.headingAngleRad)
        val nextWp = waypoints[(nearestIdx + 1) % count]
        val currWp = waypoints[nearestIdx]
        val trackDirX = nextWp.x - currWp.x
        val trackDirZ = nextWp.z - currWp.z
        val trackLen = sqrt(trackDirX * trackDirX + trackDirZ * trackDirZ).coerceAtLeast(0.01f)
        val forwardDot = (forwardX * (trackDirX / trackLen)) + (forwardZ * (trackDirZ / trackLen))

        when (recoveryState) {
            AIRecoveryState.NORMAL -> {
                // Check if stuck against barrier or facing backwards
                val isStuck = vehicle.hasCollidedBarrier && vehicle.speedKmh < 9f
                val isFacingWrongWay = forwardDot < -0.3f && vehicle.speedKmh < 22f

                if (isStuck || isFacingWrongWay) {
                    recoveryState = AIRecoveryState.REVERSING
                    recoveryTimer = 1.1f + (rng.nextFloat() * 0.4f)
                    vehicle.throttle = 0f
                    vehicle.brake = 0.90f
                    val sideToTurn = if (forwardDot < -0.2f) 1.0f else -1.0f
                    vehicle.steerInput = sideToTurn
                    return true
                }
            }
            AIRecoveryState.REVERSING -> {
                recoveryTimer -= dt
                // In reverse: brake pedal acts as reverse throttle in our dual-direction powertrain
                vehicle.throttle = 0f
                vehicle.brake = 0.90f

                // Steer in direction to swing the front towards track direction
                val sideToTurn = if (forwardDot < -0.2f) 1.0f else -1.0f
                vehicle.steerInput = sideToTurn

                if (recoveryTimer <= 0f || (!vehicle.hasCollidedBarrier && vehicle.speedKmh > 14f)) {
                    recoveryState = AIRecoveryState.REALIGNING
                    recoveryTimer = 0.75f
                }
                return true
            }
            AIRecoveryState.REALIGNING -> {
                recoveryTimer -= dt
                vehicle.brake = 0f
                vehicle.throttle = 0.65f

                // Aim directly at next waypoint ahead
                val dx = nextWp.x - vehicle.position.x
                val dz = nextWp.z - vehicle.position.z
                val targetAngle = atan2(dx, dz)
                var angleDiff = targetAngle - vehicle.headingAngleRad
                while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
                while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()
                vehicle.steerInput = (angleDiff * 2.5f).coerceIn(-1f, 1f)

                if (recoveryTimer <= 0f || forwardDot > 0.65f) {
                    recoveryState = AIRecoveryState.RESUMING
                    recoveryTimer = 0.5f
                }
                return true
            }
            AIRecoveryState.RESUMING -> {
                recoveryTimer -= dt
                if (recoveryTimer <= 0f) {
                    recoveryState = AIRecoveryState.NORMAL
                }
            }
        }
        return false
    }

    private fun computeRacingLineOffset(currIdx: Int, lookaheadIdx: Int, count: Int): Float {
        // Measure corner curvature ahead: entrance, apex, and exit
        val wp1 = waypoints[(currIdx + 4) % count]
        val wp2 = waypoints[(currIdx + 10) % count]
        val wp3 = waypoints[(currIdx + 16) % count]

        val angle1 = atan2(wp2.x - wp1.x, wp2.z - wp1.z)
        val angle2 = atan2(wp3.x - wp2.x, wp3.z - wp2.z)

        var deltaAngle = angle2 - angle1
        while (deltaAngle > PI) deltaAngle -= (2 * PI).toFloat()
        while (deltaAngle < -PI) deltaAngle += (2 * PI).toFloat()

        // Positive deltaAngle = Right hand curve; Negative = Left hand curve
        return when {
            deltaAngle > 0.25f -> {
                // Right hand turn: sweep outside left entering (-3.0m), apex inside right (+3.2m)
                val distToApex = (lookaheadIdx - currIdx + count) % count
                if (distToApex > 8) -2.8f else 3.2f
            }
            deltaAngle < -0.25f -> {
                // Left hand turn: sweep outside right entering (+3.0m), apex inside left (-3.2m)
                val distToApex = (lookaheadIdx - currIdx + count) % count
                if (distToApex > 8) 2.8f else -3.2f
            }
            else -> 0f // Straightaway: stay centered or line up
        }
    }

    private fun computeMultiCarAvoidance(
        myPos: Vector3f,
        competitors: List<Vector3f>,
        baseOffset: Float
    ): Float {
        var offset = baseOffset
        draftBonusActive = false

        var closestAheadDist = Float.MAX_VALUE
        var closestAheadX = 0f
        var closestAheadZ = 0f

        val forwardX = sin(vehicle.headingAngleRad)
        val forwardZ = cos(vehicle.headingAngleRad)
        val rightX = forwardZ
        val rightZ = -forwardX

        for (comp in competitors) {
            val dist = myPos.distanceTo(comp)
            if (dist > 35f) continue

            val toCompX = comp.x - myPos.x
            val toCompZ = comp.z - myPos.z

            val longDist = toCompX * forwardX + toCompZ * forwardZ // Positive = Ahead
            val latDist = toCompX * rightX + toCompZ * rightZ      // Positive = To my right

            // 1. Slipstream / Drafting detection
            if (longDist in 4.0f..25.0f && abs(latDist) < 2.2f) {
                if (longDist < closestAheadDist) {
                    closestAheadDist = longDist
                    closestAheadX = toCompX
                    closestAheadZ = toCompZ
                }
            }

            // 2. Side-by-side etiquette ("Leave space")
            if (abs(longDist) < 4.8f && abs(latDist) < 3.2f) {
                // Yield lateral buffer room (push away from competitor)
                val pushDirection = if (latDist >= 0f) -2.6f else 2.6f
                offset = (offset + pushDirection).coerceIn(-5.5f, 5.5f)
            }
        }

        // 3. Overtaking or Drafting decision
        if (closestAheadDist < 30f) {
            if (closestAheadDist < 12.0f) {
                // Close enough to overtake: peel out to the side with more room
                val passRight = baseOffset < 1.0f
                offset = if (passRight) baseOffset + 3.2f else baseOffset - 3.2f
            } else {
                // In the draft cone: lock into their wake for aerodynamic pull
                draftBonusActive = true
            }
        }

        return offset.coerceIn(-5.5f, 5.5f)
    }

    private fun updateDriverMistakes(dt: Float) {
        mistakeTimer -= dt
        if (mistakeTimer <= 0f) {
            mistakeTimer = 5f + rng.nextFloat() * 7f
            // Check mistake probability based on difficulty
            if (rng.nextFloat() < profile.difficulty.mistakeChance) {
                mistakeActive = true
                mistakeOffset = (if (rng.nextBoolean()) 2.2f else -2.2f)
            } else {
                mistakeActive = false
                mistakeOffset = 0f
            }
        } else if (mistakeActive && mistakeTimer < 4.0f) {
            // Decay mistake offset back to zero
            mistakeOffset *= 0.95f
        }
    }

    private fun computeTargetSpeed(currIdx: Int, count: Int): Float {
        // Sample corners ahead at 25m, 55m, and 85m arc distance
        val wpA = waypoints[(currIdx + 3) % count]
        val wpB = waypoints[(currIdx + 8) % count]
        val wpC = waypoints[(currIdx + 14) % count]

        val angleA = atan2(wpB.x - wpA.x, wpB.z - wpA.z)
        val angleB = atan2(wpC.x - wpB.x, wpC.z - wpB.z)
        var cornerTurn = abs(angleB - angleA)
        while (cornerTurn > PI) cornerTurn -= (2 * PI).toFloat()
        cornerTurn = abs(cornerTurn)

        var speed = when {
            cornerTurn > 0.68f -> 70f   // Hairpin
            cornerTurn > 0.38f -> 110f  // Medium bend
            cornerTurn > 0.16f -> 175f  // Fast sweeper
            else -> 280f               // Full throttle straightaway
        }

        // Adjust for car characteristics
        if (profile.carId == "apex_monza_v10") {
            speed += 12f // Higher top-end
        } else if (profile.carId == "apex_spirit_86") {
            speed -= 5f  // Agility oriented
        }

        var speedMultiplier = profile.difficulty.targetSpeedMultiplier
        if (draftBonusActive) {
            speedMultiplier += 0.06f // Drafting speed bonus
        }

        return speed * speedMultiplier
    }

    private fun applyThrottleAndBrakes(targetSpeedKmh: Float) {
        val speedDiff = vehicle.speedKmh - targetSpeedKmh

        if (speedDiff < -5f) {
            // Accelerate progressively
            val accelDemand = ((-speedDiff) / 25f).coerceIn(0.6f, 1.0f)
            vehicle.throttle = accelDemand
            vehicle.brake = 0f
        } else if (speedDiff > (4f * profile.difficulty.brakeMargin)) {
            // Decelerate / Threshold braking
            vehicle.throttle = 0f
            val brakeDemand = (speedDiff / 30f).coerceIn(0.4f, 1.0f)
            vehicle.brake = brakeDemand
        } else {
            // Maintain cornering balance speed
            vehicle.throttle = 0.48f
            vehicle.brake = 0f
        }
    }

    fun getTrackProgress(waypoints: List<Vector3f>): Float {
        if (waypoints.isEmpty()) return 0f
        return (totalLapsCompleted * waypoints.size + currentWaypointIndex).toFloat()
    }
}
