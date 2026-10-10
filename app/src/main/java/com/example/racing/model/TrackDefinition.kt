package com.example.racing.model

enum class TrackId(
    val title: String,
    val subtitle: String,
    val lengthKm: Float,
    val difficulty: String,
    val turns: Int
) {
    GRAND_PRIX(
        title = "Apex GP Circuit",
        subtitle = "Grand Prix Circuit • High Speed & Banked Curves",
        lengthKm = 1.8f,
        difficulty = "Medium",
        turns = 6
    ),
    ALPINE_PASS(
        title = "Alpine Mountain Pass",
        subtitle = "Mountain Ascent • Steep Elevation & Switchbacks",
        lengthKm = 2.2f,
        difficulty = "Hard",
        turns = 10
    ),
    NEON_METROPOLIS(
        title = "Night Neon Metropolis",
        subtitle = "Night City Circuit • Illuminated Skyline & Wet Asphalt",
        lengthKm = 1.9f,
        difficulty = "Extreme",
        turns = 8
    )
}

data class TrackLightingConfig(
    val sunDirection: FloatArray,
    val sunColor: FloatArray,
    val skyAmbient: FloatArray,
    val groundAmbient: FloatArray,
    val fogColor: FloatArray,
    val fogNear: Float,
    val fogFar: Float,
    val isWetRoad: Boolean
) {
    companion object {
        val GRAND_PRIX = TrackLightingConfig(
            sunDirection = floatArrayOf(0.45f, 1.25f, 0.75f),
            sunColor = floatArrayOf(1.08f, 1.04f, 0.98f),
            skyAmbient = floatArrayOf(0.38f, 0.46f, 0.60f),
            groundAmbient = floatArrayOf(0.18f, 0.22f, 0.16f),
            fogColor = floatArrayOf(0.45f, 0.65f, 0.88f),
            fogNear = 140f,
            fogFar = 700f,
            isWetRoad = false
        )
        val ALPINE_PASS = TrackLightingConfig(
            sunDirection = floatArrayOf(-0.35f, 0.95f, 0.60f),
            sunColor = floatArrayOf(0.95f, 0.98f, 1.05f),
            skyAmbient = floatArrayOf(0.28f, 0.35f, 0.50f),
            groundAmbient = floatArrayOf(0.20f, 0.22f, 0.25f),
            fogColor = floatArrayOf(0.55f, 0.62f, 0.72f),
            fogNear = 80f,
            fogFar = 550f,
            isWetRoad = false
        )
        val NEON_METROPOLIS = TrackLightingConfig(
            sunDirection = floatArrayOf(0.15f, 0.85f, -0.30f),
            sunColor = floatArrayOf(0.25f, 0.35f, 0.65f), // Dim nighttime moonlight
            skyAmbient = floatArrayOf(0.08f, 0.10f, 0.18f),
            groundAmbient = floatArrayOf(0.05f, 0.06f, 0.08f),
            fogColor = floatArrayOf(0.05f, 0.07f, 0.14f),
            fogNear = 60f,
            fogFar = 450f,
            isWetRoad = true
        )
    }
}
