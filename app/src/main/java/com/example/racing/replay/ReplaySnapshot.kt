package com.example.racing.replay

import com.example.racing.engine.Vector3f

/**
 * Snapshot of vehicle state for replay system.
 */
data class ReplaySnapshot(
    val timestampMs: Long,
    val playerPosition: Vector3f,
    val playerHeadingRad: Float,
    val playerSteerAngleRad: Float,
    val playerRpm: Float,
    val playerGear: Int,
    val playerThrottle: Float,
    val playerBrake: Float,
    val aiStates: List<AiVehicleSnapshot>,
    val events: List<ReplayEvent> = emptyList()
)

data class AiVehicleSnapshot(
    val id: String,
    val position: Vector3f,
    val headingRad: Float,
    val steerAngleRad: Float = 0f,
    val rpm: Float = 0f,
    val gear: Int = 1
)

enum class ReplayEventType {
    COLLISION,
    OVERTAKE,
    CHECKPOINT,
    LAP_COMPLETE,
    RACE_START,
    RACE_FINISH
}

data class ReplayEvent(
    val type: ReplayEventType,
    val intensity: Float = 1f,
    val involvedVehicleId: String? = null
)
