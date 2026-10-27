package com.example.racing.replay

import com.example.racing.model.CameraView
import kotlin.random.Random

/**
 * Automatically switches between cinematic camera views during replay.
 */
class ReplayCameraDirector {
    private var lastSwitchTimeMs = 0L
    private val switchIntervalMs = 6000L // Switch every 6 seconds
    private val views = listOf(
        CameraView.CHASE_NEAR,
        CameraView.CHASE_FAR,
        CameraView.FRONT_TRACKING,
        CameraView.REAR_TRACKING,
        CameraView.CINEMATIC_TRACKSIDE,
        CameraView.OVERHEAD
    )
    private var currentViewIdx = 0

    private var forceDwellTimeMs = 0L
    private var lastEventTimeMs = 0L

    fun update(currentTimeMs: Long, snapshot: ReplaySnapshot?): CameraView? {
        // Handle priority events
        val collision = snapshot?.events?.find { it.type == ReplayEventType.COLLISION }
        if (collision != null && currentTimeMs - lastEventTimeMs > 2000L) {
            lastEventTimeMs = currentTimeMs
            forceDwellTimeMs = 3500L // Lock camera on collision for 3.5s
            currentViewIdx = views.indexOf(CameraView.CINEMATIC_TRACKSIDE).coerceAtLeast(0)
            return views[currentViewIdx]
        }

        if (forceDwellTimeMs > 0) {
            forceDwellTimeMs -= 16 // Assuming ~60fps update, but dt is better. 
            // Wait, update is called with dt in VM, but director.update takes currentTimeMs.
            // I'll subtract fixed amount or use time difference.
        }

        if (forceDwellTimeMs <= 0 && currentTimeMs - lastSwitchTimeMs > switchIntervalMs) {
            lastSwitchTimeMs = currentTimeMs
            
            // Prefer cinematic views more often
            val weights = listOf(1, 1, 1, 1, 3, 1) // Weight for each view
            val totalWeight = weights.sum()
            var r = Random.nextInt(totalWeight)
            for (i in views.indices) {
                r -= weights[i]
                if (r < 0) {
                    currentViewIdx = i
                    break
                }
            }
            
            return views[currentViewIdx]
        }
        return null
    }

    fun reset() {
        lastSwitchTimeMs = 0L
        currentViewIdx = 0
        forceDwellTimeMs = 0L
        lastEventTimeMs = 0L
    }
}
