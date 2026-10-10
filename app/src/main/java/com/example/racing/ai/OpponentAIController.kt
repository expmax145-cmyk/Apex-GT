package com.example.racing.ai

import com.example.racing.engine.Vector3f
import com.example.racing.physics.VehiclePhysics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

enum class AIDifficulty(val targetSpeedMultiplier: Float, val brakeMargin: Float, val steerSharpness: Float) {
    EASY(0.78f, 1.35f, 0.70f),
    MEDIUM(0.90f, 1.15f, 0.85f),
    EXPERT(1.00f, 1.00f, 1.00f)
}

data class AIDriverProfile(
    val name: String,
    val carColor: FloatArray,
    val aggression: Float, // 0.0 to 1.0
    val difficulty: AIDifficulty,
    val carId: String = "apex_valkyrie"
)

class OpponentAIController(
    val vehicle: VehiclePhysics,
    val profile: AIDriverProfile,
    val waypoints: List<Vector3f>
) {
    var currentWaypointIndex: Int = 0
    var lateralOffset: Float = 0f // Offset from centerline for overtaking/drafting

    fun update(dt: Float, playerPos: Vector3f, otherAIs: List<Vector3f>) {
        if (waypoints.isEmpty()) return

        val count = waypoints.size
        val myPos = vehicle.position

        // 1. Find nearest waypoint & look-ahead point
        var bestDistSq = Float.MAX_VALUE
        var nearestIdx = currentWaypointIndex
        for (i in -3..6) {
            val idx = (currentWaypointIndex + i + count) % count
            val wp = waypoints[idx]
            val dSq = (wp.x - myPos.x) * (wp.x - myPos.x) + (wp.z - myPos.z) * (wp.z - myPos.z)
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                nearestIdx = idx
            }
        }
        currentWaypointIndex = nearestIdx

        // Look-ahead distance scales with speed (e.g. 15m at low speed, up to 45m at 200 km/h)
        val lookaheadDist = (14f + (vehicle.speedKmh * 0.16f)).coerceIn(14f, 48f)
        var accumulatedDist = 0f
        var lookaheadIdx = currentWaypointIndex

        while (accumulatedDist < lookaheadDist) {
            val nextIdx = (lookaheadIdx + 1) % count
            accumulatedDist += waypoints[lookaheadIdx].distanceTo(waypoints[nextIdx])
            lookaheadIdx = nextIdx
        }

        val targetWp = waypoints[lookaheadIdx]

        // 2. Dynamic obstacle avoidance (Player & Other AIs)
        val distToPlayer = myPos.distanceTo(playerPos)
        var avoidanceOffset = lateralOffset

        if (distToPlayer < 25f) {
            // Check if player is directly ahead
            val toPlayerX = playerPos.x - myPos.x
            val toPlayerZ = playerPos.z - myPos.z
            val forwardDot = toPlayerX * kotlin.math.sin(vehicle.headingAngleRad) + toPlayerZ * kotlin.math.cos(vehicle.headingAngleRad)

            if (forwardDot > 2f) { // Player is in front
                // Attempt overtake on the side with more space
                avoidanceOffset = if (toPlayerX > 0) -2.8f else 2.8f
            }
        }

        // Apply avoidance perpendicular to track direction
        val nextWp = waypoints[(lookaheadIdx + 1) % count]
        val trackDx = nextWp.x - targetWp.x
        val trackDz = nextWp.z - targetWp.z
        val trackLen = sqrt(trackDx * trackDx + trackDz * trackDz).coerceAtLeast(0.01f)
        val normalX = -(trackDz / trackLen)
        val normalZ = (trackDx / trackLen)

        val targetX = targetWp.x + normalX * avoidanceOffset
        val targetZ = targetWp.z + normalZ * avoidanceOffset

        // 3. Compute Steering Angle
        val dx = targetX - myPos.x
        val dz = targetZ - myPos.z
        val targetAngle = atan2(dx, dz)

        var angleDiff = targetAngle - vehicle.headingAngleRad
        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
        while (angleDiff < -PI) angleDiff += (2 * PI).toFloat()

        val steerTarget = (angleDiff * 2.2f * profile.difficulty.steerSharpness).coerceIn(-1f, 1f)
        vehicle.steerInput = steerTarget

        // 4. Compute Speed & Corner Braking
        // Corner curvature estimation: angle between next few waypoints
        val wpA = waypoints[(currentWaypointIndex + 2) % count]
        val wpB = waypoints[(currentWaypointIndex + 5) % count]
        val wpC = waypoints[(currentWaypointIndex + 8) % count]

        val angleA = atan2(wpB.x - wpA.x, wpB.z - wpA.z)
        val angleB = atan2(wpC.x - wpB.x, wpC.z - wpB.z)
        var cornerTurn = abs(angleB - angleA)
        while (cornerTurn > PI) cornerTurn -= (2 * PI).toFloat()
        cornerTurn = abs(cornerTurn)

        // Target speed based on corner tightness
        var targetSpeedKmh = when {
            cornerTurn > 0.65f -> 65f   // Hairpin
            cornerTurn > 0.35f -> 105f  // Medium curve
            cornerTurn > 0.15f -> 160f  // Fast sweeper
            else -> 265f               // Full throttle straight
        }

        targetSpeedKmh *= profile.difficulty.targetSpeedMultiplier

        // Throttle / Brake controls
        if (vehicle.speedKmh < targetSpeedKmh - 5f) {
            vehicle.throttle = 1.0f
            vehicle.brake = 0f
        } else if (vehicle.speedKmh > targetSpeedKmh + (5f * profile.difficulty.brakeMargin)) {
            vehicle.throttle = 0f
            vehicle.brake = ((vehicle.speedKmh - targetSpeedKmh) / 25f).coerceIn(0.4f, 1.0f)
        } else {
            vehicle.throttle = 0.45f
            vehicle.brake = 0f
        }

        // Advance physics with road boundaries
        vehicle.update(dt, waypoints, roadHalfWidth = 7.0f)
    }
}
