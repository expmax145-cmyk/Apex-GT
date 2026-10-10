package com.example.racing.model

enum class RaceMode {
    FREE_DRIVE,
    CIRCUIT_RACE,
    TIME_ATTACK
}

enum class CameraView {
    CHASE_NEAR,
    CHASE_FAR,
    HOOD_BUMPER,
    COCKPIT
}

enum class ControlType {
    TOUCH_BUTTONS,
    VIRTUAL_WHEEL,
    TILT_GYRO
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
