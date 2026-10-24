package com.example.racing.model

data class CarDefinition(
    val id: String,
    val name: String,
    val category: String, // Supercar, Tuner Coupe, Trackday Hypercar
    val description: String,
    val basePrice: Int,
    val massKg: Float,
    val baseTorqueNm: Float,
    val topSpeedKmh: Float,
    val handlingRating: Float, // 1 to 10
    val accelerationRating: Float, // 1 to 10
    val defaultColor: FloatArray,
    val availableColors: List<Pair<String, FloatArray>>,
    val wheelbaseM: Float = 2.65f,
    val trackWidthM: Float = 1.68f,
    val cgHeightM: Float = 0.36f,
    val maxSteerAngleRad: Float = 0.58f,
    val downforceCoeff: Float = 0.75f,
    val dragCoeff: Float = 0.31f,
    val driveFrontRatio: Float = 0.0f
) {
    companion object {
        val ALL_CARS = listOf(
            CarDefinition(
                id = "apex_valkyrie",
                name = "Apex RS-1 Valkyrie",
                category = "GT Supercar",
                description = "Twin-turbo V8 endurance racer featuring carbon aero and AWD vectoring.",
                basePrice = 0, // Starter car
                massKg = 1380f,
                baseTorqueNm = 720f,
                topSpeedKmh = 335f,
                handlingRating = 9.2f,
                accelerationRating = 9.5f,
                defaultColor = floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f), // Apex Crimson Red
                availableColors = listOf(
                    "Crimson Red" to floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f),
                    "Obsidian Black" to floatArrayOf(0.12f, 0.13f, 0.15f, 1.0f),
                    "Electric Blue" to floatArrayOf(0.10f, 0.50f, 0.98f, 1.0f),
                    "Acid Green" to floatArrayOf(0.35f, 0.92f, 0.15f, 1.0f),
                    "Pearl White" to floatArrayOf(0.95f, 0.96f, 0.98f, 1.0f),
                    "Sunset Orange" to floatArrayOf(0.98f, 0.55f, 0.10f, 1.0f),
                    "Midnight Purple" to floatArrayOf(0.38f, 0.12f, 0.65f, 1.0f),
                    "Nardo Gray" to floatArrayOf(0.55f, 0.56f, 0.58f, 1.0f),
                    "Racing Yellow" to floatArrayOf(0.98f, 0.88f, 0.10f, 1.0f),
                    "British Green" to floatArrayOf(0.10f, 0.35f, 0.18f, 1.0f),
                    "Deep Navy" to floatArrayOf(0.08f, 0.18f, 0.38f, 1.0f),
                    "Silver" to floatArrayOf(0.80f, 0.82f, 0.85f, 1.0f),
                    "Rose Pink" to floatArrayOf(0.95f, 0.40f, 0.65f, 1.0f),
                    "Teal Cyan" to floatArrayOf(0.05f, 0.75f, 0.75f, 1.0f),
                    "Copper Bronze" to floatArrayOf(0.72f, 0.45f, 0.20f, 1.0f),
                    "Gunmetal" to floatArrayOf(0.25f, 0.28f, 0.32f, 1.0f)
                ),
                wheelbaseM = 2.65f,
                trackWidthM = 1.70f,
                cgHeightM = 0.36f,
                maxSteerAngleRad = 0.58f,
                downforceCoeff = 0.85f,
                dragCoeff = 0.31f,
                driveFrontRatio = 0.35f
            ),
            CarDefinition(
                id = "apex_spirit_86",
                name = "Apex Spirit GTS",
                category = "Tuner Coupe",
                description = "High-revving naturally aspirated RWD lightweight chassis engineered for precision drift angles.",
                basePrice = 25000,
                massKg = 1180f,
                baseTorqueNm = 480f,
                topSpeedKmh = 285f,
                handlingRating = 9.6f,
                accelerationRating = 8.1f,
                defaultColor = floatArrayOf(0.98f, 0.55f, 0.10f, 1.0f), // Sunset Orange
                availableColors = listOf(
                    "Crimson Red" to floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f),
                    "Obsidian Black" to floatArrayOf(0.12f, 0.13f, 0.15f, 1.0f),
                    "Electric Blue" to floatArrayOf(0.10f, 0.50f, 0.98f, 1.0f),
                    "Acid Green" to floatArrayOf(0.35f, 0.92f, 0.15f, 1.0f),
                    "Pearl White" to floatArrayOf(0.95f, 0.96f, 0.98f, 1.0f),
                    "Sunset Orange" to floatArrayOf(0.98f, 0.55f, 0.10f, 1.0f),
                    "Midnight Purple" to floatArrayOf(0.38f, 0.12f, 0.65f, 1.0f),
                    "Nardo Gray" to floatArrayOf(0.55f, 0.56f, 0.58f, 1.0f),
                    "Racing Yellow" to floatArrayOf(0.98f, 0.88f, 0.10f, 1.0f),
                    "British Green" to floatArrayOf(0.10f, 0.35f, 0.18f, 1.0f),
                    "Deep Navy" to floatArrayOf(0.08f, 0.18f, 0.38f, 1.0f),
                    "Silver" to floatArrayOf(0.80f, 0.82f, 0.85f, 1.0f),
                    "Rose Pink" to floatArrayOf(0.95f, 0.40f, 0.65f, 1.0f),
                    "Teal Cyan" to floatArrayOf(0.05f, 0.75f, 0.75f, 1.0f),
                    "Copper Bronze" to floatArrayOf(0.72f, 0.45f, 0.20f, 1.0f),
                    "Gunmetal" to floatArrayOf(0.25f, 0.28f, 0.32f, 1.0f)
                ),
                wheelbaseM = 2.50f,
                trackWidthM = 1.62f,
                cgHeightM = 0.38f,
                maxSteerAngleRad = 0.68f, // High angle drift lock
                downforceCoeff = 0.40f,
                dragCoeff = 0.34f,
                driveFrontRatio = 0.0f
            ),
            CarDefinition(
                id = "apex_monza_v10",
                name = "Apex Monza V10",
                category = "Hypercar Prototype",
                description = "900HP naturally aspirated V10 screaming to 9200 RPM with extreme downforce.",
                basePrice = 75000,
                massKg = 1120f,
                baseTorqueNm = 840f,
                topSpeedKmh = 370f,
                handlingRating = 9.8f,
                accelerationRating = 9.9f,
                defaultColor = floatArrayOf(0.12f, 0.13f, 0.15f, 1.0f), // Carbon Stealth
                availableColors = listOf(
                    "Crimson Red" to floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f),
                    "Obsidian Black" to floatArrayOf(0.12f, 0.13f, 0.15f, 1.0f),
                    "Electric Blue" to floatArrayOf(0.10f, 0.50f, 0.98f, 1.0f),
                    "Acid Green" to floatArrayOf(0.35f, 0.92f, 0.15f, 1.0f),
                    "Pearl White" to floatArrayOf(0.95f, 0.96f, 0.98f, 1.0f),
                    "Sunset Orange" to floatArrayOf(0.98f, 0.55f, 0.10f, 1.0f),
                    "Midnight Purple" to floatArrayOf(0.38f, 0.12f, 0.65f, 1.0f),
                    "Nardo Gray" to floatArrayOf(0.55f, 0.56f, 0.58f, 1.0f),
                    "Racing Yellow" to floatArrayOf(0.98f, 0.88f, 0.10f, 1.0f),
                    "British Green" to floatArrayOf(0.10f, 0.35f, 0.18f, 1.0f),
                    "Deep Navy" to floatArrayOf(0.08f, 0.18f, 0.38f, 1.0f),
                    "Silver" to floatArrayOf(0.80f, 0.82f, 0.85f, 1.0f),
                    "Rose Pink" to floatArrayOf(0.95f, 0.40f, 0.65f, 1.0f),
                    "Teal Cyan" to floatArrayOf(0.05f, 0.75f, 0.75f, 1.0f),
                    "Copper Bronze" to floatArrayOf(0.72f, 0.45f, 0.20f, 1.0f),
                    "Gunmetal" to floatArrayOf(0.25f, 0.28f, 0.32f, 1.0f)
                ),
                wheelbaseM = 2.85f,
                trackWidthM = 1.76f,
                cgHeightM = 0.30f,
                maxSteerAngleRad = 0.50f, // Sharp high-speed tracking
                downforceCoeff = 1.45f,
                dragCoeff = 0.28f,
                driveFrontRatio = 0.15f
            )
        )
    }
}

data class CarUpgradeState(
    val engineStage: Int = 0,     // 0..3 (+50Nm per stage)
    val tireStage: Int = 0,       // 0: Street, 1: Sport, 2: Semi-Slick, 3: Racing Slick
    val suspensionStage: Int = 0, // 0..3 (stiffness & downforce)
    val weightReduction: Int = 0  // 0..3 (-60kg per stage)
)
