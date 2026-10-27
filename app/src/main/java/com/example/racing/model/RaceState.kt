package com.example.racing.model

enum class RaceMode {
    FREE_DRIVE,
    CIRCUIT_RACE,
    TIME_ATTACK,
    CHAMPIONSHIP
}

enum class CameraView {
    CHASE_NEAR,
    CHASE_FAR,
    HOOD_BUMPER,
    COCKPIT,
    CINEMATIC_TRACKSIDE,
    FRONT_TRACKING,
    REAR_TRACKING,
    OVERHEAD,
    FREE_ORBIT
}

enum class ControlType {
    TOUCH_BUTTONS,
    VIRTUAL_WHEEL,
    TILT_GYRO
}

data class LeaderboardRacer(
    val position: Int,
    val name: String,
    val carName: String,
    val color: FloatArray,
    val currentLap: Int,
    val bestLapTimeSec: Float,
    val gapToLeaderText: String,
    val isPlayer: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LeaderboardRacer

        if (position != other.position) return false
        if (name != other.name) return false
        if (carName != other.carName) return false
        if (!color.contentEquals(other.color)) return false
        if (currentLap != other.currentLap) return false
        if (bestLapTimeSec != other.bestLapTimeSec) return false
        if (gapToLeaderText != other.gapToLeaderText) return false
        if (isPlayer != other.isPlayer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position
        result = 31 * result + name.hashCode()
        result = 31 * result + carName.hashCode()
        result = 31 * result + color.contentHashCode()
        result = 31 * result + currentLap
        result = 31 * result + bestLapTimeSec.hashCode()
        result = 31 * result + gapToLeaderText.hashCode()
        result = 31 * result + isPlayer.hashCode()
        return result
    }
}

data class RaceTiming(
    var currentLap: Int = 1,
    var totalLaps: Int = 3,
    var currentLapTimeSec: Float = 0f,
    var bestLapTimeSec: Float = 0f,
    var lastLapTimeSec: Float = 0f,
    var raceFinished: Boolean = false,
    var finishPosition: Int = 1,
    var countdownSec: Float = 3.99f // 3.. 2.. 1.. GO
) {
    val isCountdownActive: Boolean get() = countdownSec > 0f

    fun formatTime(timeSec: Float): String {
        if (timeSec <= 0f) return "--:--.---"
        val minutes = (timeSec / 60).toInt()
        val seconds = (timeSec % 60).toInt()
        val millis = ((timeSec * 1000) % 1000).toInt()
        return "%02d:%02d.%03d".format(minutes, seconds, millis)
    }
}
