package com.example.racing.model

import com.example.racing.engine.Vector3f
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Time of Day states for realistic dynamic lighting in Apex GT.
 */
enum class TimeOfDay(
    val displayName: String,
    val description: String,
    val baseHour: Float
) {
    DYNAMIC("Dynamic Cycle", "Real-time smooth sun progression", -1f),
    DAY("Midday Sun", "Bright direct sun & clear visibility", 12.0f),
    AFTERNOON("Late Afternoon", "Warm angled golden sunlight", 16.5f),
    SUNSET("Sunset & Twilight", "Vibrant amber & violet horizon", 19.5f),
    NIGHT("Night Racing", "Moonlit circuit with vehicle headlights", 23.5f)
}

/**
 * Weather conditions affecting atmospheric scattering, cloud density, rainfall, and road wetness.
 */
enum class WeatherCondition(
    val displayName: String,
    val description: String
) {
    CLEAR("Clear Sky", "Optimal dry racing conditions"),
    OVERCAST("Overcast", "Heavy cloud cover & diffuse ambient light"),
    LIGHT_RAIN("Light Rain", "Light precipitation & damp road surface"),
    HEAVY_RAIN("Heavy Rain", "Dense rainfall, soaked asphalt & tyre spray")
}

/**
 * Consolidated dynamic environment lighting parameters shared by Sky, Track, Cars, and PBR Shaders.
 */
data class DynamicEnvironmentState(
    val sunDirection: FloatArray = floatArrayOf(0.45f, 1.25f, 0.75f),
    val sunColor: FloatArray = floatArrayOf(1.08f, 1.04f, 0.98f),
    val skyAmbient: FloatArray = floatArrayOf(0.38f, 0.46f, 0.60f),
    val groundAmbient: FloatArray = floatArrayOf(0.18f, 0.22f, 0.16f),
    val fogColor: FloatArray = floatArrayOf(0.45f, 0.65f, 0.88f),
    val fogNear: Float = 140f,
    val fogFar: Float = 700f,
    val skyZenithColor: FloatArray = floatArrayOf(0.14f, 0.42f, 0.86f),
    val skyHorizonColor: FloatArray = floatArrayOf(0.52f, 0.70f, 0.90f),
    val cloudColor: FloatArray = floatArrayOf(0.85f, 0.88f, 0.92f),
    val cloudCoverage: Float = 0.0f,
    val wetRoadActive: Float = 0.0f,
    val rainIntensity: Float = 0.0f,
    val headlightsActive: Boolean = false,
    val headlightIntensity: Float = 0.0f,
    val isNight: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DynamicEnvironmentState
        return sunDirection.contentEquals(other.sunDirection) &&
                sunColor.contentEquals(other.sunColor) &&
                skyAmbient.contentEquals(other.skyAmbient) &&
                groundAmbient.contentEquals(other.groundAmbient) &&
                fogColor.contentEquals(other.fogColor) &&
                fogNear == other.fogNear &&
                fogFar == other.fogFar &&
                skyZenithColor.contentEquals(other.skyZenithColor) &&
                skyHorizonColor.contentEquals(other.skyHorizonColor) &&
                cloudColor.contentEquals(other.cloudColor) &&
                cloudCoverage == other.cloudCoverage &&
                wetRoadActive == other.wetRoadActive &&
                rainIntensity == other.rainIntensity &&
                headlightsActive == other.headlightsActive &&
                headlightIntensity == other.headlightIntensity &&
                isNight == other.isNight
    }

    override fun hashCode(): Int {
        var result = sunDirection.contentHashCode()
        result = 31 * result + sunColor.contentHashCode()
        result = 31 * result + skyAmbient.contentHashCode()
        result = 31 * result + groundAmbient.contentHashCode()
        result = 31 * result + fogColor.contentHashCode()
        result = 31 * result + fogNear.hashCode()
        result = 31 * result + fogFar.hashCode()
        result = 31 * result + skyZenithColor.contentHashCode()
        result = 31 * result + skyHorizonColor.contentHashCode()
        result = 31 * result + cloudColor.contentHashCode()
        result = 31 * result + cloudCoverage.hashCode()
        result = 31 * result + wetRoadActive.hashCode()
        result = 31 * result + rainIntensity.hashCode()
        result = 31 * result + headlightsActive.hashCode()
        result = 31 * result + headlightIntensity.hashCode()
        result = 31 * result + isNight.hashCode()
        return result
    }
}

/**
 * Controller calculating smooth realistic lighting, celestial sun motion, and atmospheric weather.
 */
class EnvironmentController(
    var selectedTimeOfDay: TimeOfDay = TimeOfDay.DYNAMIC,
    var selectedWeather: WeatherCondition = WeatherCondition.CLEAR
) {
    // Dynamic cycle progresses continuously (starts at 14:00 afternoon)
    var dynamicHour: Float = 14.0f
    private var cycleSpeedHoursPerSec: Float = 0.08f // ~5 minutes for full 24hr cycle

    // Reusable output state to avoid per-frame GC allocation
    private val sunDir = FloatArray(3)
    private val sunCol = FloatArray(3)
    private val skyAmb = FloatArray(3)
    private val gndAmb = FloatArray(3)
    private val fogCol = FloatArray(3)
    private val zenithCol = FloatArray(3)
    private val horizonCol = FloatArray(3)
    private val cloudCol = FloatArray(3)

    var currentState: DynamicEnvironmentState = DynamicEnvironmentState()
        private set

    fun update(dt: Float, trackId: TrackId, graphicsQuality: String) {
        val effectiveHour = when (selectedTimeOfDay) {
            TimeOfDay.DYNAMIC -> {
                dynamicHour = (dynamicHour + dt * cycleSpeedHoursPerSec) % 24f
                dynamicHour
            }
            TimeOfDay.DAY -> 12.5f
            TimeOfDay.AFTERNOON -> 16.0f
            TimeOfDay.SUNSET -> 17.8f
            TimeOfDay.NIGHT -> 23.0f
        }

        // Solar azimuth and elevation calculations (0h=midnight, 6h=sunrise, 12h=noon, 18h=sunset)
        // Normalize 24h into radians [0, 2*PI]
        val timeRad = (effectiveHour / 24f) * (2f * PI.toFloat())
        // Sun elevation: peaks at noon (sin > 0), drops below horizon at night (sin < 0)
        val sunElevation = sin(timeRad - PI.toFloat() * 0.5f)
        val sunAzimuth = cos(timeRad - PI.toFloat() * 0.5f)

        val isNightTime = selectedTimeOfDay == TimeOfDay.NIGHT || (selectedTimeOfDay == TimeOfDay.DYNAMIC && sunElevation < -0.05f)

        // 1. Calculate Sun Direction & Colors based on sun elevation
        if (isNightTime) {
            // Moon replaces sun as primary directional light source
            sunDir[0] = 0.25f
            sunDir[1] = 0.92f
            sunDir[2] = -0.30f

            // Cool moonlight
            sunCol[0] = 0.20f
            sunCol[1] = 0.28f
            sunCol[2] = 0.48f

            // Nighttime sky zenith & horizon
            zenithCol[0] = 0.02f; zenithCol[1] = 0.03f; zenithCol[2] = 0.07f
            horizonCol[0] = 0.07f; horizonCol[1] = 0.08f; horizonCol[2] = 0.16f

            skyAmb[0] = 0.08f; skyAmb[1] = 0.10f; skyAmb[2] = 0.18f
            gndAmb[0] = 0.05f; gndAmb[1] = 0.06f; gndAmb[2] = 0.08f
            fogCol[0] = 0.05f; fogCol[1] = 0.07f; fogCol[2] = 0.14f
            cloudCol[0] = 0.10f; cloudCol[1] = 0.12f; cloudCol[2] = 0.18f
        } else if (sunElevation < 0.25f) {
            // Sunset / Twilight / Sunrise (Golden Hour & Pink/Orange/Amber Glow)
            val t = (sunElevation / 0.25f).coerceIn(0f, 1f)
            sunDir[0] = sunAzimuth * 1.1f
            sunDir[1] = (sunElevation * 1.5f).coerceAtLeast(0.12f)
            sunDir[2] = 0.65f

            // Warm golden-amber sun
            sunCol[0] = 1.25f
            sunCol[1] = 0.72f * (0.6f + 0.4f * t)
            sunCol[2] = 0.32f * (0.4f + 0.6f * t)

            // Dramatic sunset gradients: Violet/Indigo zenith, flaming Amber/Salmon horizon
            zenithCol[0] = 0.12f + 0.04f * t
            zenithCol[1] = 0.18f + 0.22f * t
            zenithCol[2] = 0.45f + 0.38f * t

            horizonCol[0] = 0.88f - 0.28f * t
            horizonCol[1] = 0.42f + 0.26f * t
            horizonCol[2] = 0.25f + 0.60f * t

            skyAmb[0] = 0.32f; skyAmb[1] = 0.28f; skyAmb[2] = 0.42f
            gndAmb[0] = 0.18f; gndAmb[1] = 0.15f; gndAmb[2] = 0.14f
            fogCol[0] = 0.62f - 0.15f * t
            fogCol[1] = 0.40f + 0.22f * t
            fogCol[2] = 0.35f + 0.45f * t
            cloudCol[0] = 0.95f; cloudCol[1] = 0.55f; cloudCol[2] = 0.40f
        } else if (sunElevation < 0.65f) {
            // Warm late afternoon
            val t = ((sunElevation - 0.25f) / 0.40f).coerceIn(0f, 1f)
            sunDir[0] = sunAzimuth * 0.85f
            sunDir[1] = sunElevation * 1.25f
            sunDir[2] = 0.70f

            sunCol[0] = 1.15f - 0.05f * t
            sunCol[1] = 1.02f + 0.02f * t
            sunCol[2] = 0.78f + 0.20f * t

            zenithCol[0] = 0.15f; zenithCol[1] = 0.40f; zenithCol[2] = 0.84f
            horizonCol[0] = 0.58f; horizonCol[1] = 0.68f; horizonCol[2] = 0.88f

            skyAmb[0] = 0.35f; skyAmb[1] = 0.42f; skyAmb[2] = 0.56f
            gndAmb[0] = 0.18f; gndAmb[1] = 0.20f; gndAmb[2] = 0.16f
            fogCol[0] = 0.52f; fogCol[1] = 0.62f; fogCol[2] = 0.82f
            cloudCol[0] = 0.92f; cloudCol[1] = 0.88f; cloudCol[2] = 0.82f
        } else {
            // Bright midday
            sunDir[0] = sunAzimuth * 0.45f
            sunDir[1] = sunElevation * 1.35f
            sunDir[2] = 0.75f

            sunCol[0] = 1.08f; sunCol[1] = 1.05f; sunCol[2] = 0.98f

            zenithCol[0] = 0.14f; zenithCol[1] = 0.42f; zenithCol[2] = 0.86f
            horizonCol[0] = 0.52f; horizonCol[1] = 0.70f; horizonCol[2] = 0.90f

            skyAmb[0] = 0.38f; skyAmb[1] = 0.46f; skyAmb[2] = 0.60f
            gndAmb[0] = 0.18f; gndAmb[1] = 0.22f; gndAmb[2] = 0.16f
            fogCol[0] = 0.45f; fogCol[1] = 0.65f; fogCol[2] = 0.88f
            cloudCol[0] = 0.96f; cloudCol[1] = 0.96f; cloudCol[2] = 0.98f
        }

        // 2. Track-specific Atmospheric Nuances
        when (trackId) {
            TrackId.ALPINE_PASS -> {
                // Alpine mountain cool atmosphere with crisper distance haze
                fogCol[0] = (fogCol[0] * 0.92f).coerceIn(0f, 1f)
                fogCol[1] = (fogCol[1] * 0.96f).coerceIn(0f, 1f)
                fogCol[2] = (fogCol[2] * 1.05f).coerceIn(0f, 1f)
                zenithCol[2] = (zenithCol[2] * 1.08f).coerceIn(0f, 1f)
            }
            TrackId.NEON_METROPOLIS -> {
                // Urban night track: enhance violet/cyan ambient neon glow
                if (isNightTime) {
                    skyAmb[0] = 0.10f; skyAmb[1] = 0.12f; skyAmb[2] = 0.22f
                    horizonCol[0] = 0.12f; horizonCol[1] = 0.10f; horizonCol[2] = 0.24f
                    fogCol[0] = 0.07f; fogCol[1] = 0.08f; fogCol[2] = 0.18f
                }
            }
            TrackId.GRAND_PRIX -> {
                // Professional GP circuit: clean high-contrast asphalt and curbs
            }
        }

        // 3. Weather Adjustments (Cloud Coverage, Diffuse Lighting, Fog Range, Wetness)
        var cloudCoverage = 0.0f
        var wetness = if (trackId == TrackId.NEON_METROPOLIS) 0.85f else 0.0f
        var rainIntensity = 0.0f
        var fogNear = 120f
        var fogFar = 680f

        when (selectedWeather) {
            WeatherCondition.CLEAR -> {
                cloudCoverage = 0.10f
                rainIntensity = 0.0f
            }
            WeatherCondition.OVERCAST -> {
                cloudCoverage = 0.82f
                rainIntensity = 0.0f
                // Overcast softens direct sunlight into diffuse ambient
                sunCol[0] *= 0.45f; sunCol[1] *= 0.45f; sunCol[2] *= 0.48f
                skyAmb[0] *= 1.25f; skyAmb[1] *= 1.25f; skyAmb[2] *= 1.20f
                gndAmb[0] *= 1.15f; gndAmb[1] *= 1.15f; gndAmb[2] *= 1.15f
                fogCol[0] = (fogCol[0] * 0.75f + 0.18f)
                fogCol[1] = (fogCol[1] * 0.75f + 0.20f)
                fogCol[2] = (fogCol[2] * 0.75f + 0.22f)
                fogNear = 90f
                fogFar = 520f
            }
            WeatherCondition.LIGHT_RAIN -> {
                cloudCoverage = 0.92f
                rainIntensity = 0.45f
                wetness = 0.75f
                sunCol[0] *= 0.35f; sunCol[1] *= 0.35f; sunCol[2] *= 0.40f
                skyAmb[0] *= 1.10f; skyAmb[1] *= 1.12f; skyAmb[2] *= 1.15f
                fogCol[0] = fogCol[0] * 0.65f + 0.16f
                fogCol[1] = fogCol[1] * 0.65f + 0.18f
                fogCol[2] = fogCol[2] * 0.65f + 0.24f
                fogNear = 75f
                fogFar = 460f
            }
            WeatherCondition.HEAVY_RAIN -> {
                cloudCoverage = 0.98f
                rainIntensity = 1.0f
                wetness = 1.0f
                sunCol[0] *= 0.25f; sunCol[1] *= 0.25f; sunCol[2] *= 0.30f
                skyAmb[0] *= 0.95f; skyAmb[1] *= 0.98f; skyAmb[2] *= 1.05f
                fogCol[0] = fogCol[0] * 0.55f + 0.14f
                fogCol[1] = fogCol[1] * 0.55f + 0.16f
                fogCol[2] = fogCol[2] * 0.55f + 0.22f
                fogNear = 60f
                fogFar = 380f
            }
        }

        // Graphics quality scaling on wet road specular sheen
        val finalWetness = when (graphicsQuality) {
            "LOW" -> if (wetness > 0.4f) 0.35f else 0.0f
            "MEDIUM" -> wetness * 0.75f
            else -> wetness
        }

        // 4. Headlights Activation (Active at Night, Twilight, or in Rain/Overcast)
        val lowVisibility = isNightTime ||
                sunElevation < 0.20f ||
                selectedWeather == WeatherCondition.LIGHT_RAIN ||
                selectedWeather == WeatherCondition.HEAVY_RAIN ||
                selectedWeather == WeatherCondition.OVERCAST ||
                trackId == TrackId.NEON_METROPOLIS

        val headlightsActive = lowVisibility && graphicsQuality != "LOW"
        val headlightIntensity = when {
            !headlightsActive -> 0.0f
            graphicsQuality == "HIGH" -> if (isNightTime || selectedWeather == WeatherCondition.HEAVY_RAIN) 2.0f else 1.4f
            else -> 1.1f
        }

        currentState = DynamicEnvironmentState(
            sunDirection = sunDir.clone(),
            sunColor = sunCol.clone(),
            skyAmbient = skyAmb.clone(),
            groundAmbient = gndAmb.clone(),
            fogColor = fogCol.clone(),
            fogNear = fogNear,
            fogFar = fogFar,
            skyZenithColor = zenithCol.clone(),
            skyHorizonColor = horizonCol.clone(),
            cloudColor = cloudCol.clone(),
            cloudCoverage = cloudCoverage,
            wetRoadActive = finalWetness,
            rainIntensity = rainIntensity,
            headlightsActive = headlightsActive,
            headlightIntensity = headlightIntensity,
            isNight = isNightTime
        )
    }
}
