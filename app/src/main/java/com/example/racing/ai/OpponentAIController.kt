package com.example.racing.ai

import com.example.racing.engine.Vector3f
import com.example.racing.physics.VehiclePhysics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * AI Competitor Driving Difficulty presets.
 */
enum class AIDifficulty(
    val targetSpeedMultiplier: Float,
    val brakeMargin: Float,
    val steerSharpness: Float,
    val mistakeChance: Float,
    val reactionTimeSec: Float
) {
    EASY(0.82f, 1.32f, 0.74f, 0.07f, 0.18f),
    MEDIUM(0.92f, 1.10f, 0.88f, 0.025f, 0.10f),
    EXPERT(1.00f, 0.96f, 1.00f, 0.005f, 0.04f)
}

/**
 * Distinguishable driving personalities for AI opponents.
 */
enum class AIPersonality {
    AGGRESSIVE, // Viper: Dives inside into corners, late-braker, exploits slipstream, defends line
    BALANCED,   // Shadow: Textbook racing lines, patient overtakes, measured braking and throttle
    DEFENSIVE   // Ghost: Cautious side-by-side spacing, earlier braking, yields under pressure
}

/**
 * Driver configuration profile.
 */
data class AIDriverProfile(
    val name: String,
    val carColor: FloatArray,
    val aggression: Float, // 0.0 to 1.0
    val difficulty: AIDifficulty,
    val carId: String = "apex_valkyrie",
    val preferredLaneOffset: Float = 0f, // Baseline lane bias to fan out competitors
    val personality: AIPersonality = when {
        aggression >= 0.80f -> AIPersonality.AGGRESSIVE
        aggression <= 0.60f -> AIPersonality.DEFENSIVE
        else -> AIPersonality.BALANCED
    }
)

/**
 * Multi-stage recovery state machine for spinning or wall-stuck vehicles.
 */
enum class AIRecoveryState {
    NORMAL,
    REVERSING,
    REALIGNING,
    RESUMING
}

/**
 * Phase 13 — Intelligent Racing AI Controller.
 *
 * Implements:
 * - Dynamic Waypoint Navigation with curvature-aware lookahead
 * - Multi-horizon Speed Profiling & Threshold Braking Distance calculations
 * - Geometric Racing Lines (outside-inside-outside with apex clipping)
 * - Multi-Car Spatial Awareness (Player + AI pack)
 * - Slipstream / Drafting detection and slingshot overtaking
 * - Defensive line blocking against close pursuers
 * - Side-by-side racing etiquette with lateral room allocation
 * - Obstacle / slow vehicle avoidance
 * - Distinguishable opponent personalities (Viper, Shadow, Ghost)
 * - Human-like recoverable driver mistakes
 * - Three-stage recovery state machine (reversing, realigning, resuming)
 * - Anti-oscillation steering rate limiter for buttery vehicle stability
 * - Zero per-frame memory allocation design
 */
class OpponentAIController(
    val vehicle: VehiclePhysics,
    val profile: AIDriverProfile,
    val waypoints: List<Vector3f>
) {
    var currentWaypointIndex: Int = 0
    var lateralOffset: Float = 0f
    var currentLap: Int = 1
    var totalLapsCompleted: Int = 0
    var currentLapTimeSec: Float = 0f
    var bestLapTimeSec: Float = 0f
    var lastLapTimeSec: Float = 0f
    var raceFinished: Boolean = false
    var finishTimeSec: Float = 0f
    var cachedMaterial: com.example.racing.engine.Material? = null

    // State machine & behavior
    var recoveryState: AIRecoveryState = AIRecoveryState.NORMAL
    private var recoveryTimer: Float = 0f
    private var mistakeTimer: Float = 0f
    private var mistakeActive: Boolean = false
    private var mistakeOffset: Float = 0f
    private var draftBonusActive: Boolean = false
    private var lastPassedWaypointIndex: Int = 0

    // Deterministic random generator per driver name
    private val rng = Random(profile.name.hashCode())

    private fun safeIndex(idx: Int, count: Int): Int {
        if (count <= 0) return 0
        return ((idx % count) + count) % count
    }

    /**
     * Standard update loop using competitor positions.
     */
    fun update(dt: Float, playerPos: Vector3f, otherAIs: List<Vector3f>) {
        if (waypoints.isEmpty()) return

        val count = waypoints.size
        val myPos = vehicle.position

        currentLapTimeSec += dt

        // 1. Locate nearest waypoint and track lap progression
        var bestDistSq = Float.MAX_VALUE
        var nearestIdx = currentWaypointIndex
        val scanRange = min(8, count / 2)
        for (i in -scanRange..scanRange) {
            val idx = safeIndex(currentWaypointIndex + i, count)
            val wp = waypoints[idx]
            val dSq = (wp.x - myPos.x) * (wp.x - myPos.x) + (wp.z - myPos.z) * (wp.z - myPos.z)
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                nearestIdx = idx
            }
        }

        // Lap crossing detection
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

        // 2. State Machine: Barrier collision, spin-out & reverse recovery
        if (updateRecoveryStateMachine(dt, nearestIdx, count)) {
            vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
            return
        }

        // 3. Dynamic Look-ahead distance scaled with speed (from 16m up to 52m)
        val lookaheadDist = (16f + (vehicle.speedKmh * 0.17f)).coerceIn(16f, 52f)
        var accumulatedDist = 0f
        var lookaheadIdx = currentWaypointIndex

        while (accumulatedDist < lookaheadDist) {
            val nextIdx = safeIndex(lookaheadIdx + 1, count)
            accumulatedDist += waypoints[lookaheadIdx].distanceTo(waypoints[nextIdx])
            lookaheadIdx = nextIdx
            if (lookaheadIdx == currentWaypointIndex) break
        }

        val targetWp = waypoints[lookaheadIdx]

        // 4. Calculate Geometric Racing Line (outside-inside-outside apex trajectory)
        val racingLineOffset = computeRacingLineOffset(currentWaypointIndex, lookaheadIdx, count)

        // 5. Multi-Car Spatial Awareness & Avoidance
        val dynamicAvoidanceOffset = computeMultiCarAvoidance(
            myPos = myPos,
            playerPos = playerPos,
            otherAIs = otherAIs,
            baseOffset = racingLineOffset + profile.preferredLaneOffset,
            currIdx = currentWaypointIndex,
            count = count
        )

        // 6. Humanized Driver Mistakes
        updateDriverMistakes(dt)
        val finalTargetOffset = (dynamicAvoidanceOffset + mistakeOffset).coerceIn(-5.5f, 5.5f)

        // Smooth lateral steering target transition
        val lateralTransitionRate = when (profile.personality) {
            AIPersonality.AGGRESSIVE -> 4.2f
            AIPersonality.BALANCED -> 3.2f
            AIPersonality.DEFENSIVE -> 2.6f
        }
        lateralOffset += (finalTargetOffset - lateralOffset) * (dt * lateralTransitionRate).coerceIn(0.04f, 0.40f)

        // Project lateral offset perpendicular to track direction
        val nextWp = waypoints[safeIndex(lookaheadIdx + 1, count)]
        val trackDx = nextWp.x - targetWp.x
        val trackDz = nextWp.z - targetWp.z
        val trackLen = sqrt(trackDx * trackDx + trackDz * trackDz).coerceAtLeast(0.01f)
        val normalX = -(trackDz / trackLen)
        val normalZ = (trackDx / trackLen)

        val targetX = targetWp.x + normalX * lateralOffset
        val targetZ = targetWp.z + normalZ * lateralOffset

        // 7. Steering Calculation with Rate-Limiting & Anti-Oscillation
        val dx = targetX - myPos.x
        val dz = targetZ - myPos.z
        val targetAngle = atan2(dx, dz)

        var angleDiff = targetAngle - vehicle.headingAngleRad
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

        val steerSensitivity = profile.difficulty.steerSharpness * (if (profile.carId == "apex_spirit_86") 1.15f else 1.0f)
        val rawSteerTarget = (angleDiff * 2.35f * steerSensitivity).coerceIn(-1f, 1f)

        // Smooth steering rate limiter to eliminate high-frequency twitching
        val maxSteerRate = dt * (6.5f * profile.difficulty.steerSharpness)
        vehicle.steerInput += (rawSteerTarget - vehicle.steerInput).coerceIn(-maxSteerRate, maxSteerRate)

        // 8. Advanced Speed Profiling & Multi-Horizon Braking Distance
        val targetSpeedKmh = computeTargetSpeed(currentWaypointIndex, count)

        // 9. Progressive Throttle & Threshold Braking
        applyThrottleAndBrakes(targetSpeedKmh)

        // 10. Step Vehicle Physics with Road Boundaries
        vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
    }

    /**
     * Enhanced update loop accepting full VehiclePhysics objects for deeper physical telemetry.
     */
    fun updateWithVehicles(dt: Float, playerVehicle: VehiclePhysics, allCompetitors: List<OpponentAIController>) {
        if (waypoints.isEmpty()) return

        val count = waypoints.size
        val myPos = vehicle.position

        currentLapTimeSec += dt

        // 1. Locate nearest waypoint
        var bestDistSq = Float.MAX_VALUE
        var nearestIdx = currentWaypointIndex
        val scanRange = min(8, count / 2)
        for (i in -scanRange..scanRange) {
            val idx = safeIndex(currentWaypointIndex + i, count)
            val wp = waypoints[idx]
            val dSq = (wp.x - myPos.x) * (wp.x - myPos.x) + (wp.z - myPos.z) * (wp.z - myPos.z)
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                nearestIdx = idx
            }
        }

        // Lap crossing
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

        // 2. Recovery state machine
        if (updateRecoveryStateMachine(dt, nearestIdx, count)) {
            vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
            return
        }

        // 3. Dynamic look-ahead distance
        val lookaheadDist = (16f + (vehicle.speedKmh * 0.17f)).coerceIn(16f, 52f)
        var accumulatedDist = 0f
        var lookaheadIdx = currentWaypointIndex

        while (accumulatedDist < lookaheadDist) {
            val nextIdx = safeIndex(lookaheadIdx + 1, count)
            accumulatedDist += waypoints[lookaheadIdx].distanceTo(waypoints[nextIdx])
            lookaheadIdx = nextIdx
            if (lookaheadIdx == currentWaypointIndex) break
        }

        val targetWp = waypoints[lookaheadIdx]

        // 4. Calculate Geometric Racing Line
        val racingLineOffset = computeRacingLineOffset(currentWaypointIndex, lookaheadIdx, count)

        // 5. Multi-Car Spatial Awareness & Avoidance
        val dynamicAvoidanceOffset = computeMultiVehicleAvoidance(
            myVehicle = vehicle,
            playerVehicle = playerVehicle,
            otherCompetitors = allCompetitors,
            baseOffset = racingLineOffset + profile.preferredLaneOffset,
            currIdx = currentWaypointIndex,
            count = count
        )

        // 6. Humanized Driver Mistakes
        updateDriverMistakes(dt)
        val finalTargetOffset = (dynamicAvoidanceOffset + mistakeOffset).coerceIn(-5.5f, 5.5f)

        // Smooth lateral transition
        val lateralTransitionRate = when (profile.personality) {
            AIPersonality.AGGRESSIVE -> 4.2f
            AIPersonality.BALANCED -> 3.2f
            AIPersonality.DEFENSIVE -> 2.6f
        }
        lateralOffset += (finalTargetOffset - lateralOffset) * (dt * lateralTransitionRate).coerceIn(0.04f, 0.40f)

        val nextWp = waypoints[safeIndex(lookaheadIdx + 1, count)]
        val trackDx = nextWp.x - targetWp.x
        val trackDz = nextWp.z - targetWp.z
        val trackLen = sqrt(trackDx * trackDx + trackDz * trackDz).coerceAtLeast(0.01f)
        val normalX = -(trackDz / trackLen)
        val normalZ = (trackDx / trackLen)

        val targetX = targetWp.x + normalX * lateralOffset
        val targetZ = targetWp.z + normalZ * lateralOffset

        // 7. Steering Calculation with Rate-Limiting & Anti-Oscillation
        val dx = targetX - myPos.x
        val dz = targetZ - myPos.z
        val targetAngle = atan2(dx, dz)

        var angleDiff = targetAngle - vehicle.headingAngleRad
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

        val steerSensitivity = profile.difficulty.steerSharpness * (if (profile.carId == "apex_spirit_86") 1.15f else 1.0f)
        val rawSteerTarget = (angleDiff * 2.35f * steerSensitivity).coerceIn(-1f, 1f)

        val maxSteerRate = dt * (6.5f * profile.difficulty.steerSharpness)
        vehicle.steerInput += (rawSteerTarget - vehicle.steerInput).coerceIn(-maxSteerRate, maxSteerRate)

        // 8. Advanced Speed Profiling & Multi-Horizon Braking Distance
        val targetSpeedKmh = computeTargetSpeed(currentWaypointIndex, count)

        // 9. Progressive Throttle & Threshold Braking
        applyThrottleAndBrakes(targetSpeedKmh)

        // 10. Step Vehicle Physics
        vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
    }

    private fun updateRecoveryStateMachine(dt: Float, nearestIdx: Int, count: Int): Boolean {
        if (count < 2) return false
        val forwardX = sin(vehicle.headingAngleRad)
        val forwardZ = cos(vehicle.headingAngleRad)
        val nextWp = waypoints[safeIndex(nearestIdx + 1, count)]
        val currWp = waypoints[safeIndex(nearestIdx, count)]
        val trackDirX = nextWp.x - currWp.x
        val trackDirZ = nextWp.z - currWp.z
        val trackLen = sqrt(trackDirX * trackDirX + trackDirZ * trackDirZ).coerceAtLeast(0.01f)
        val forwardDot = (forwardX * (trackDirX / trackLen)) + (forwardZ * (trackDirZ / trackLen))

        when (recoveryState) {
            AIRecoveryState.NORMAL -> {
                val isStuck = vehicle.hasCollidedBarrier && (vehicle.speedKmh < 9f || abs(vehicle.forwardVelocity) < 2.5f)
                val isFacingWrongWay = forwardDot < -0.3f && (vehicle.speedKmh < 24f || abs(vehicle.forwardVelocity) < 6.0f)

                if (isStuck || isFacingWrongWay) {
                    recoveryState = AIRecoveryState.REVERSING
                    recoveryTimer = 1.15f + (rng.nextFloat() * 0.4f)
                    vehicle.throttle = 0f
                    vehicle.brake = 0.95f // Acts as reverse throttle in the dual-direction powertrain
                    val sideToTurn = if (forwardDot < -0.2f) 1.0f else -1.0f
                    vehicle.steerInput = sideToTurn
                    return true
                }
            }
            AIRecoveryState.REVERSING -> {
                recoveryTimer -= dt
                vehicle.throttle = 0f
                vehicle.brake = 0.95f

                val sideToTurn = if (forwardDot < -0.2f) 1.0f else -1.0f
                vehicle.steerInput = sideToTurn

                if (recoveryTimer <= 0f || (!vehicle.hasCollidedBarrier && (vehicle.speedKmh > 14f || abs(vehicle.forwardVelocity) > 3.8f))) {
                    recoveryState = AIRecoveryState.REALIGNING
                    recoveryTimer = 0.85f
                }
                return true
            }
            AIRecoveryState.REALIGNING -> {
                recoveryTimer -= dt
                vehicle.brake = 0f
                vehicle.throttle = 0.70f

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

    /**
     * Computes the geometric outside-inside-outside racing line offset.
     */
    private fun computeRacingLineOffset(currIdx: Int, lookaheadIdx: Int, count: Int): Float {
        if (count < 8) return 0f

        val step1 = max(1, count / 16)
        val step2 = max(2, count / 8)
        val step3 = max(3, count * 3 / 16)

        val wp1 = waypoints[safeIndex(currIdx + step1, count)]
        val wp2 = waypoints[safeIndex(currIdx + step2, count)]
        val wp3 = waypoints[safeIndex(currIdx + step3, count)]

        val angle1 = atan2(wp2.x - wp1.x, wp2.z - wp1.z)
        val angle2 = atan2(wp3.x - wp2.x, wp3.z - wp2.z)

        var deltaAngle = angle2 - angle1
        while (deltaAngle > PI) deltaAngle -= (2 * PI).toFloat()
        while (deltaAngle < -PI) deltaAngle += (2 * PI).toFloat()

        val apexDist = safeIndex(lookaheadIdx - currIdx, count)

        return when {
            deltaAngle > 0.18f -> {
                // Right-hand corner: sweep wide left entering (-3.2m), clip apex inside right (+3.4m), exit wide left (-2.6m)
                when {
                    apexDist > 9 -> -3.2f
                    apexDist in 3..9 -> 3.4f
                    else -> -2.6f
                }
            }
            deltaAngle < -0.18f -> {
                // Left-hand corner: sweep wide right entering (+3.2m), clip apex inside left (-3.4m), exit wide right (+2.6m)
                when {
                    apexDist > 9 -> 3.2f
                    apexDist in 3..9 -> -3.4f
                    else -> 2.6f
                }
            }
            else -> 0f // Straightaway
        }
    }

    /**
     * Multi-car traffic awareness, drafting, overtaking, and collision avoidance using Vector3f coordinates.
     */
    private fun computeMultiCarAvoidance(
        myPos: Vector3f,
        playerPos: Vector3f,
        otherAIs: List<Vector3f>,
        baseOffset: Float,
        currIdx: Int,
        count: Int
    ): Float {
        var offset = baseOffset
        draftBonusActive = false

        var closestAheadDist = Float.MAX_VALUE

        val forwardX = sin(vehicle.headingAngleRad)
        val forwardZ = cos(vehicle.headingAngleRad)
        val rightX = forwardZ
        val rightZ = -forwardX

        val checkCompetitor: (Vector3f) -> Unit = { comp ->
            if (comp !== myPos && comp.distanceTo(myPos) > 0.05f) {
                val toCompX = comp.x - myPos.x
                val toCompZ = comp.z - myPos.z
                val longDist = toCompX * forwardX + toCompZ * forwardZ
                val latDist = toCompX * rightX + toCompZ * rightZ

                // 1. Slipstream / Drafting detection
                if (longDist in 4.0f..26.0f && abs(latDist) < 2.2f) {
                    if (longDist < closestAheadDist) {
                        closestAheadDist = longDist
                    }
                }

                // 2. Side-by-side etiquette ("Leave space")
                if (abs(longDist) < 4.8f && abs(latDist) < 3.4f) {
                    val clearancePush = when (profile.personality) {
                        AIPersonality.DEFENSIVE -> 3.0f
                        AIPersonality.BALANCED -> 2.4f
                        AIPersonality.AGGRESSIVE -> 1.8f
                    }
                    val pushDirection = if (latDist >= 0f) -clearancePush else clearancePush
                    offset = (offset + pushDirection).coerceIn(-5.5f, 5.5f)
                }

                // 3. Defensive blocking against close pursuers (Viper / Aggressive)
                if (profile.personality == AIPersonality.AGGRESSIVE && longDist in -16.0f..-3.0f && abs(latDist) < 3.0f) {
                    offset = if (baseOffset >= 0f) 2.2f else -2.2f
                }
            }
        }

        checkCompetitor(playerPos)
        for (aiPos in otherAIs) {
            checkCompetitor(aiPos)
        }

        // 4. Overtaking or Drafting Decision
        if (closestAheadDist < 30f) {
            val passThreshold = when (profile.personality) {
                AIPersonality.AGGRESSIVE -> 14.0f
                AIPersonality.BALANCED -> 10.0f
                AIPersonality.DEFENSIVE -> 8.0f
            }
            if (closestAheadDist < passThreshold) {
                val passRight = baseOffset < 0.8f
                val passWidth = if (profile.personality == AIPersonality.AGGRESSIVE) 3.5f else 3.0f
                offset = if (passRight) baseOffset + passWidth else baseOffset - passWidth
            } else {
                draftBonusActive = true
            }
        }

        return offset.coerceIn(-5.5f, 5.5f)
    }

    /**
     * Multi-car traffic awareness using full VehiclePhysics instances for speed & velocity differentials.
     */
    private fun computeMultiVehicleAvoidance(
        myVehicle: VehiclePhysics,
        playerVehicle: VehiclePhysics,
        otherCompetitors: List<OpponentAIController>,
        baseOffset: Float,
        currIdx: Int,
        count: Int
    ): Float {
        var offset = baseOffset
        draftBonusActive = false

        var closestAheadDist = Float.MAX_VALUE

        val myPos = myVehicle.position
        val forwardX = sin(myVehicle.headingAngleRad)
        val forwardZ = cos(myVehicle.headingAngleRad)
        val rightX = forwardZ
        val rightZ = -forwardX

        val checkVehicle: (VehiclePhysics) -> Unit = { comp ->
            if (comp !== myVehicle) {
                val toCompX = comp.position.x - myPos.x
                val toCompZ = comp.position.z - myPos.z
                val longDist = toCompX * forwardX + toCompZ * forwardZ
                val latDist = toCompX * rightX + toCompZ * rightZ

                // Hazard detection: Car ahead is spun out, stopped, or significantly slower
                if (longDist in 2.0f..35.0f && abs(latDist) < 3.2f && (comp.speedKmh < 22f || abs(comp.forwardVelocity) < 6f)) {
                    val avoidRight = latDist < 0f || baseOffset > 0f
                    val avoidDelta = if (avoidRight) 3.6f else -3.6f
                    offset = (offset + avoidDelta).coerceIn(-5.5f, 5.5f)
                }

                // Drafting detection
                if (longDist in 4.0f..28.0f && abs(latDist) < 2.2f) {
                    if (longDist < closestAheadDist) {
                        closestAheadDist = longDist
                    }
                }

                // Side-by-side room
                if (abs(longDist) < 4.8f && abs(latDist) < 3.4f) {
                    val clearance = when (profile.personality) {
                        AIPersonality.DEFENSIVE -> 3.0f
                        AIPersonality.BALANCED -> 2.4f
                        AIPersonality.AGGRESSIVE -> 1.8f
                    }
                    val push = if (latDist >= 0f) -clearance else clearance
                    offset = (offset + push).coerceIn(-5.5f, 5.5f)
                }

                // Defensive blocking (Viper / Aggressive)
                if (profile.personality == AIPersonality.AGGRESSIVE && longDist in -16.0f..-3.0f && abs(latDist) < 3.0f) {
                    offset = if (baseOffset >= 0f) 2.2f else -2.2f
                }
            }
        }

        checkVehicle(playerVehicle)
        for (opp in otherCompetitors) {
            checkVehicle(opp.vehicle)
        }

        if (closestAheadDist < 30f) {
            val passThreshold = when (profile.personality) {
                AIPersonality.AGGRESSIVE -> 14.0f
                AIPersonality.BALANCED -> 10.0f
                AIPersonality.DEFENSIVE -> 8.0f
            }
            if (closestAheadDist < passThreshold) {
                val passRight = baseOffset < 0.8f
                val passWidth = if (profile.personality == AIPersonality.AGGRESSIVE) 3.5f else 3.0f
                offset = if (passRight) baseOffset + passWidth else baseOffset - passWidth
            } else {
                draftBonusActive = true
            }
        }

        return offset.coerceIn(-5.5f, 5.5f)
    }

    private fun updateDriverMistakes(dt: Float) {
        mistakeTimer -= dt
        if (mistakeTimer <= 0f) {
            mistakeTimer = 5f + rng.nextFloat() * 7f
            if (rng.nextFloat() < profile.difficulty.mistakeChance) {
                mistakeActive = true
                mistakeOffset = if (rng.nextBoolean()) 2.2f else -2.2f
            } else {
                mistakeActive = false
                mistakeOffset = 0f
            }
        } else if (mistakeActive && mistakeTimer < 4.0f) {
            mistakeOffset *= 0.94f
        }
    }

    /**
     * Advanced Speed Profiling with Multi-Horizon Braking Distance Calculation.
     * Computes the maximum safe cornering velocity across upcoming bends and calculates
     * required braking distances so opponents decelerate smoothly before corners rather than overshooting.
     */
    private fun computeTargetSpeed(currIdx: Int, count: Int): Float {
        if (count < 3) return 200f
        var minSafeSpeedKmh = 290f
        var accumulatedTrackDist = 0f

        val decelerationCoeff = 9.8f * profile.difficulty.brakeMargin * (if (profile.personality == AIPersonality.AGGRESSIVE) 1.12f else 1.0f)

        // Sample upcoming waypoints across multiple horizons up to 110m ahead
        val maxSteps = min(18, count - 2)
        for (step in 1..maxSteps) {
            val idxA = safeIndex(currIdx + step, count)
            val idxB = safeIndex(currIdx + step + 1, count)
            val idxC = safeIndex(currIdx + step + 2, count)

            accumulatedTrackDist += waypoints[idxA].distanceTo(waypoints[idxB])
            if (accumulatedTrackDist > 120f) break

            val wpA = waypoints[idxA]
            val wpB = waypoints[idxB]
            val wpC = waypoints[idxC]

            val angleA = atan2(wpB.x - wpA.x, wpB.z - wpA.z)
            val angleB = atan2(wpC.x - wpB.x, wpC.z - wpB.z)
            var cornerDelta = abs(angleB - angleA)
            while (cornerDelta > PI) cornerDelta -= (2 * PI).toFloat()
            cornerDelta = abs(cornerDelta)

            // Safe corner apex speed based on bend curvature
            val cornerApexSpeedKmh = when {
                cornerDelta > 0.65f -> 68f   // Hairpin
                cornerDelta > 0.38f -> 108f  // Medium bend
                cornerDelta > 0.16f -> 175f  // Fast sweeper
                else -> 290f                // Flat out
            }

            // Braking distance equation: v_max_now = sqrt(v_target^2 + 2 * a * d)
            val apexSpeedMs = cornerApexSpeedKmh / 3.6f
            val maxAllowableSpeedMs = sqrt(apexSpeedMs * apexSpeedMs + 2.0f * decelerationCoeff * accumulatedTrackDist)
            val maxAllowableSpeedKmh = maxAllowableSpeedMs * 3.6f

            if (maxAllowableSpeedKmh < minSafeSpeedKmh) {
                minSafeSpeedKmh = maxAllowableSpeedKmh
            }
        }

        // Vehicle powertrain & characteristics adjustments
        if (profile.carId == "apex_monza_v10") {
            minSafeSpeedKmh += 14f // Higher top-end
        } else if (profile.carId == "apex_spirit_86") {
            minSafeSpeedKmh -= 4f  // Agile lower-displacement
        }

        var speedMultiplier = profile.difficulty.targetSpeedMultiplier
        if (draftBonusActive) {
            speedMultiplier += 0.07f // Aerodynamic draft pull bonus
        }

        return minSafeSpeedKmh * speedMultiplier
    }

    /**
     * Progressive throttle and threshold braking control.
     */
    private fun applyThrottleAndBrakes(targetSpeedKmh: Float) {
        val speedDiff = vehicle.speedKmh - targetSpeedKmh

        if (speedDiff < -4f) {
            // Accelerate progressively
            val accelDemand = ((-speedDiff) / 22f).coerceIn(0.65f, 1.0f)
            vehicle.throttle = accelDemand
            vehicle.brake = 0f
        } else if (speedDiff > (3.5f * profile.difficulty.brakeMargin)) {
            // Decelerate with threshold braking
            vehicle.throttle = 0f
            val brakeDemand = (speedDiff / 26f).coerceIn(0.45f, 1.0f)
            vehicle.brake = brakeDemand
        } else {
            // Maintain balance speed through the apex
            vehicle.throttle = 0.50f
            vehicle.brake = 0f
        }
    }

    /**
     * Returns continuous normalized track progress including fractional segment advancement.
     */
    fun getTrackProgress(waypoints: List<Vector3f>): Float {
        if (waypoints.isEmpty()) return 0f
        val count = waypoints.size
        val currIdx = safeIndex(currentWaypointIndex, count)
        val nextIdx = safeIndex(currIdx + 1, count)
        val currWp = waypoints[currIdx]
        val nextWp = waypoints[nextIdx]

        val segDx = nextWp.x - currWp.x
        val segDz = nextWp.z - currWp.z
        val segLenSq = (segDx * segDx + segDz * segDz).coerceAtLeast(0.01f)

        val toCarX = vehicle.position.x - currWp.x
        val toCarZ = vehicle.position.z - currWp.z
        val segmentFraction = ((toCarX * segDx + toCarZ * segDz) / segLenSq).coerceIn(0f, 1f)

        return (totalLapsCompleted * count + currIdx + segmentFraction)
    }
}
