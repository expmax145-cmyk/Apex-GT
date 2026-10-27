package com.example.racing.replay

import com.example.racing.engine.Vector3f
import com.example.racing.ui.GameViewModel

/**
 * Manages recording and playback of race data.
 */
class ReplayManager(private val maxDurationSec: Float = 60f) {
    private val samplingRateHz = 20
    private val maxSnapshots = (maxDurationSec * samplingRateHz).toInt()
    private val buffer = ReplayBuffer(maxSnapshots)
    private var isRecording = false

    fun startRecording() {
        buffer.clear()
        isRecording = true
    }

    fun stopRecording() {
        isRecording = false
    }

    private val pendingEvents = mutableListOf<ReplayEvent>()
    
    fun addEvent(event: ReplayEvent) {
        pendingEvents.add(event)
    }

    fun record(viewModel: GameViewModel, timestampMs: Long) {
        if (!isRecording) return
        
        val player = viewModel.playerVehicle
        val aiSnapshots = viewModel.aiOpponents.map { ai ->
            AiVehicleSnapshot(
                ai.profile.name, 
                ai.vehicle.position.copy(), 
                ai.vehicle.headingAngleRad,
                ai.vehicle.currentSteerAngleRad,
                ai.vehicle.powertrain.currentRpm,
                ai.vehicle.powertrain.currentGear
            )
        }

        val snapshot = ReplaySnapshot(
            timestampMs,
            player.position.copy(),
            player.headingAngleRad,
            player.currentSteerAngleRad,
            player.powertrain.currentRpm,
            player.powertrain.currentGear,
            player.throttle,
            player.brake,
            aiSnapshots,
            ArrayList(pendingEvents)
        )
        buffer.add(snapshot)
        pendingEvents.clear()
    }

    fun getReplayData(): List<ReplaySnapshot> = buffer.getAll()

    fun getInterpolatedSnapshot(timeMs: Long): ReplaySnapshot? {
        val snapshots = buffer.getAll()
        if (snapshots.isEmpty()) return null
        if (timeMs <= snapshots.first().timestampMs) return snapshots.first()
        if (timeMs >= snapshots.last().timestampMs) return snapshots.last()

        // Find the two snapshots to interpolate between
        for (i in 0 until snapshots.size - 1) {
            val s0 = snapshots[i]
            val s1 = snapshots[i + 1]
            if (timeMs >= s0.timestampMs && timeMs <= s1.timestampMs) {
                val t = (timeMs - s0.timestampMs).toFloat() / (s1.timestampMs - s0.timestampMs).toFloat()
                
                return interpolate(s0, s1, t)
            }
        }
        return snapshots.last()
    }

    private fun interpolate(s0: ReplaySnapshot, s1: ReplaySnapshot, t: Float): ReplaySnapshot {
        val playerPos = s0.playerPosition.lerp(s1.playerPosition, t)
        val playerHeading = lerpAngle(s0.playerHeadingRad, s1.playerHeadingRad, t)
        val playerSteer = s0.playerSteerAngleRad + (s1.playerSteerAngleRad - s0.playerSteerAngleRad) * t
        val playerRpm = s0.playerRpm + (s1.playerRpm - s0.playerRpm) * t
        
        val aiSnapshots = mutableListOf<AiVehicleSnapshot>()
        for (i in s0.aiStates.indices) {
            if (i < s1.aiStates.size) {
                val ai0 = s0.aiStates[i]
                val ai1 = s1.aiStates[i]
                aiSnapshots.add(
                    AiVehicleSnapshot(
                        ai0.id,
                        ai0.position.lerp(ai1.position, t),
                        lerpAngle(ai0.headingRad, ai1.headingRad, t),
                        ai0.steerAngleRad + (ai1.steerAngleRad - ai0.steerAngleRad) * t,
                        ai0.rpm + (ai1.rpm - ai0.rpm) * t,
                        if (t < 0.5f) ai0.gear else ai1.gear
                    )
                )
            }
        }

        return ReplaySnapshot(
            (s0.timestampMs + (s1.timestampMs - s0.timestampMs) * t).toLong(),
            playerPos,
            playerHeading,
            playerSteer,
            playerRpm,
            if (t < 0.5f) s0.playerGear else s1.playerGear,
            s0.playerThrottle + (s1.playerThrottle - s0.playerThrottle) * t,
            s0.playerBrake + (s1.playerBrake - s0.playerBrake) * t,
            aiSnapshots,
            if (t < 0.1f) s0.events else if (t > 0.9f) s1.events else emptyList()
        )
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = b - a
        while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
        while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()
        return a + diff * t
    }
}

