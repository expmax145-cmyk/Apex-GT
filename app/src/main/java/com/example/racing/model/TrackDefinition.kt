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
    COASTAL_SPRINT(
        title = "Coastal Sprint",
        subtitle = "Coastal Highway • Sweeping Ocean Views",
        lengthKm = 2.4f,
        difficulty = "Medium",
        turns = 5
    ),
    ALPINE_PASS(
        title = "Alpine Mountain Pass",
        subtitle = "Mountain Ascent • Steep Elevation & Switchbacks",
        lengthKm = 2.2f,
        difficulty = "Hard",
        turns = 10
    ),
    DESERT_RUN(
        title = "Desert Run",
        subtitle = "Arid Flatlands • Long Straights & Broad Curves",
        lengthKm = 3.0f,
        difficulty = "Easy",
        turns = 4
    ),
    FOREST_DRIVE(
        title = "Forest Drive",
        subtitle = "Wooded Canopy • Flowing Technical Sections",
        lengthKm = 2.1f,
        difficulty = "Medium",
        turns = 7
    ),
    NEON_METROPOLIS(
        title = "Night Neon Metropolis",
        subtitle = "Night City Circuit • Illuminated Skyline & Wet Asphalt",
        lengthKm = 1.9f,
        difficulty = "Extreme",
        turns = 8
    ),
    COUNTRYSIDE_RUN(
        title = "Countryside Run",
        subtitle = "Open Fields • Gentle Rolling Terrain",
        lengthKm = 2.5f,
        difficulty = "Easy",
        turns = 6
    ),
    INDUSTRIAL_ROUTE(
        title = "Industrial Route",
        subtitle = "Industrial Outskirts • Wide Paved Roads",
        lengthKm = 2.0f,
        difficulty = "Medium",
        turns = 5
    ),
    CANYON_CHALLENGE(
        title = "Canyon Challenge",
        subtitle = "Winding Canyon • Extreme Elevation Changes",
        lengthKm = 2.8f,
        difficulty = "Hard",
        turns = 12
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
    val isWetRoad: Boolean,
    val skyZenithColor: FloatArray = floatArrayOf(0.18f, 0.45f, 0.85f),
    val skyHorizonColor: FloatArray = floatArrayOf(0.55f, 0.72f, 0.90f)
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
            isWetRoad = false,
            skyZenithColor = floatArrayOf(0.14f, 0.42f, 0.86f),
            skyHorizonColor = floatArrayOf(0.52f, 0.70f, 0.90f)
        )
        val ALPINE_PASS = TrackLightingConfig(
            sunDirection = floatArrayOf(-0.35f, 0.95f, 0.60f),
            sunColor = floatArrayOf(0.95f, 0.98f, 1.05f),
            skyAmbient = floatArrayOf(0.28f, 0.35f, 0.50f),
            groundAmbient = floatArrayOf(0.20f, 0.22f, 0.25f),
            fogColor = floatArrayOf(0.55f, 0.62f, 0.72f),
            fogNear = 80f,
            fogFar = 550f,
            isWetRoad = false,
            skyZenithColor = floatArrayOf(0.16f, 0.38f, 0.75f),
            skyHorizonColor = floatArrayOf(0.60f, 0.68f, 0.78f)
        )
        val NEON_METROPOLIS = TrackLightingConfig(
            sunDirection = floatArrayOf(0.15f, 0.85f, -0.30f),
            sunColor = floatArrayOf(0.25f, 0.35f, 0.65f), // Dim nighttime moonlight
            skyAmbient = floatArrayOf(0.08f, 0.10f, 0.18f),
            groundAmbient = floatArrayOf(0.05f, 0.06f, 0.08f),
            fogColor = floatArrayOf(0.05f, 0.07f, 0.14f),
            fogNear = 60f,
            fogFar = 450f,
            isWetRoad = true,
            skyZenithColor = floatArrayOf(0.02f, 0.03f, 0.07f),
            skyHorizonColor = floatArrayOf(0.09f, 0.08f, 0.18f)
        )
        val COASTAL_SPRINT = GRAND_PRIX
        val DESERT_RUN = TrackLightingConfig(
            sunDirection = floatArrayOf(0.85f, 1.10f, 0.25f),
            sunColor = floatArrayOf(1.20f, 1.12f, 0.85f),
            skyAmbient = floatArrayOf(0.45f, 0.42f, 0.35f),
            groundAmbient = floatArrayOf(0.30f, 0.25f, 0.18f),
            fogColor = floatArrayOf(0.85f, 0.75f, 0.60f),
            fogNear = 100f,
            fogFar = 600f,
            isWetRoad = false,
            skyZenithColor = floatArrayOf(0.25f, 0.50f, 0.85f),
            skyHorizonColor = floatArrayOf(0.80f, 0.70f, 0.55f)
        )
        val FOREST_DRIVE = ALPINE_PASS
        val COUNTRYSIDE_RUN = GRAND_PRIX
        val INDUSTRIAL_ROUTE = ALPINE_PASS
        val CANYON_CHALLENGE = DESERT_RUN
    }
}
