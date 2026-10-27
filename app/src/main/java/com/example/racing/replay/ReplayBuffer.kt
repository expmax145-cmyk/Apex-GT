package com.example.racing.replay

import java.util.LinkedList

/**
 * Memory-bounded ring buffer for replay snapshots.
 */
class ReplayBuffer(val maxSnapshots: Int) {
    private val buffer = LinkedList<ReplaySnapshot>()

    fun add(snapshot: ReplaySnapshot) {
        if (buffer.size >= maxSnapshots) {
            buffer.removeFirst()
        }
        buffer.add(snapshot)
    }

    fun getAll(): List<ReplaySnapshot> = buffer.toList()

    fun clear() {
        buffer.clear()
    }

    fun size(): Int = buffer.size
}
