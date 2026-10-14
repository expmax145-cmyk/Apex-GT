package com.example.racing.engine

/**
 * Automotive Paint Finish Type with physical microfacet and clearcoat properties.
 */
enum class PaintFinishType(
    val displayName: String,
    val metallic: Float,
    val roughness: Float,
    val clearcoat: Float,
    val specularStrength: Float
) {
    GLOSS("Gloss", 0.35f, 0.16f, 0.85f, 0.85f),
    METALLIC("Metallic", 0.88f, 0.12f, 0.95f, 1.0f),
    MATTE("Matte Satin", 0.15f, 0.65f, 0.05f, 0.40f),
    PEARLESCENT("Pearlescent", 0.60f, 0.10f, 1.0f, 1.25f);

    fun createMaterial(baseColor: FloatArray): Material {
        val specR = (1.0f * specularStrength).coerceIn(0f, 1.5f)
        val specG = (1.0f * specularStrength).coerceIn(0f, 1.5f)
        val specB = (1.0f * specularStrength).coerceIn(0f, 1.5f)
        return Material(
            color = baseColor,
            specular = floatArrayOf(specR, specG, specB, 1.0f),
            shininess = when (this) {
                MATTE -> 18f
                GLOSS -> 80f
                METALLIC -> 95f
                PEARLESCENT -> 110f
            },
            metallic = metallic,
            roughness = roughness,
            clearcoat = clearcoat,
            emission = floatArrayOf(0f, 0f, 0f, 0f)
        )
    }
}

data class Material(
    val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val specular: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val shininess: Float = 48f,
    val metallic: Float = 0.5f,
    val roughness: Float = 0.3f,
    val clearcoat: Float = 0.0f,
    val emission: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Material
        return color.contentEquals(other.color) &&
                specular.contentEquals(other.specular) &&
                shininess == other.shininess &&
                metallic == other.metallic &&
                roughness == other.roughness &&
                clearcoat == other.clearcoat &&
                emission.contentEquals(other.emission)
    }

    override fun hashCode(): Int {
        var result = color.contentHashCode()
        result = 31 * result + specular.contentHashCode()
        result = 31 * result + shininess.hashCode()
        result = 31 * result + metallic.hashCode()
        result = 31 * result + roughness.hashCode()
        result = 31 * result + clearcoat.hashCode()
        result = 31 * result + emission.contentHashCode()
        return result
    }

    companion object {
        // High-gloss automotive clearcoated metallic paint
        val CAR_PAINT_METALLIC = Material(
            color = floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f),
            specular = floatArrayOf(1.0f, 0.95f, 0.95f, 1.0f),
            shininess = 95f,
            metallic = 0.88f,
            roughness = 0.12f,
            clearcoat = 0.95f
        )
        // High-performance autoclave weave carbon fiber
        val CARBON_FIBER = Material(
            color = floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f),
            specular = floatArrayOf(0.50f, 0.50f, 0.52f, 1.0f),
            shininess = 38f,
            metallic = 0.25f,
            roughness = 0.40f,
            clearcoat = 0.50f
        )
        // Vulcanized synthetic racing tire compound (matte, micro-textured, high roughness)
        val RUBBER_TIRE = Material(
            color = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f),
            specular = floatArrayOf(0.14f, 0.14f, 0.15f, 1.0f),
            shininess = 10f,
            metallic = 0.02f,
            roughness = 0.88f,
            clearcoat = 0.0f
        )
        // Machined forged alloy aluminum rim
        val ALLOY_RIM = Material(
            color = floatArrayOf(0.88f, 0.88f, 0.92f, 1.0f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 110f,
            metallic = 0.96f,
            roughness = 0.08f,
            clearcoat = 0.80f
        )
        // Wheel assembly combining spoke alloys, rubber sidewalls and ventilated rotors
        val WHEEL_ASSEMBLY = Material(
            color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            specular = floatArrayOf(0.96f, 0.96f, 0.98f, 1.0f),
            shininess = 85f,
            metallic = 0.72f,
            roughness = 0.20f,
            clearcoat = 0.65f
        )
        // High-reflection tinted greenhouse safety glass
        val GLASS_TINT = Material(
            color = floatArrayOf(0.10f, 0.14f, 0.20f, 0.85f),
            specular = floatArrayOf(1.1f, 1.1f, 1.15f, 1.0f),
            shininess = 125f,
            metallic = 0.12f,
            roughness = 0.025f,
            clearcoat = 1.0f
        )
        // High-intensity Xenon / LED projector headlights
        val HEADLIGHT_LED = Material(
            color = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 90f,
            metallic = 0.05f,
            roughness = 0.10f,
            emission = floatArrayOf(1.8f, 2.0f, 2.4f, 1.0f)
        )
        // LED taillight cluster running light (idle state)
        val TAILLIGHT_OFF = Material(
            color = floatArrayOf(0.65f, 0.08f, 0.08f, 1.0f),
            specular = floatArrayOf(0.8f, 0.3f, 0.3f, 1.0f),
            shininess = 70f,
            metallic = 0.05f,
            roughness = 0.15f,
            clearcoat = 0.9f,
            emission = floatArrayOf(0.40f, 0.04f, 0.04f, 1.0f)
        )
        // High-intensity dynamic LED brake light illumination
        val TAILLIGHT_BRAKE = Material(
            color = floatArrayOf(1.0f, 0.12f, 0.12f, 1.0f),
            specular = floatArrayOf(1.0f, 0.6f, 0.6f, 1.0f),
            shininess = 90f,
            metallic = 0.05f,
            roughness = 0.10f,
            clearcoat = 0.9f,
            emission = floatArrayOf(2.8f, 0.30f, 0.30f, 1.0f)
        )
        // Bitumen aggregate dry racing asphalt with textured micro-roughness
        val ASPHALT_DRY = Material(
            color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            specular = floatArrayOf(0.25f, 0.25f, 0.26f, 1.0f),
            shininess = 16f,
            metallic = 0.04f,
            roughness = 0.80f,
            clearcoat = 0.0f
        )
        // Rain-soaked wet asphalt with high anisotropic specular sheen and puddle reflections
        val ASPHALT_WET = Material(
            color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            specular = floatArrayOf(0.90f, 0.92f, 0.98f, 1.0f),
            shininess = 100f,
            metallic = 0.38f,
            roughness = 0.15f,
            clearcoat = 0.95f
        )
        // Multi-textured environmental scenery with vibrant vertex-colored surfaces
        val ENVIRONMENT_SCENERY = Material(
            color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            specular = floatArrayOf(0.35f, 0.35f, 0.38f, 1.0f),
            shininess = 20f,
            metallic = 0.05f,
            roughness = 0.70f,
            clearcoat = 0.0f
        )
        // Grassy verge / infield terrain
        val GRASS = Material(
            color = floatArrayOf(0.20f, 0.40f, 0.16f, 1.0f),
            specular = floatArrayOf(0.08f, 0.08f, 0.08f, 1.0f),
            shininess = 6f,
            metallic = 0.0f,
            roughness = 0.92f,
            clearcoat = 0.0f
        )
        // Granitic mountain rock face
        val MOUNTAIN_ROCK = Material(
            color = floatArrayOf(0.42f, 0.40f, 0.38f, 1.0f),
            specular = floatArrayOf(0.18f, 0.18f, 0.18f, 1.0f),
            shininess = 12f,
            metallic = 0.10f,
            roughness = 0.85f,
            clearcoat = 0.0f
        )
        // Circuit FIA apex kerbs (high-visibility painted concrete)
        val KERB_RED_WHITE = Material(
            color = floatArrayOf(0.92f, 0.92f, 0.94f, 1.0f),
            specular = floatArrayOf(0.40f, 0.40f, 0.42f, 1.0f),
            shininess = 32f,
            metallic = 0.08f,
            roughness = 0.50f,
            clearcoat = 0.30f
        )
        // Galvanized Armco steel crash barriers
        val BARRIER_ARMCO = Material(
            color = floatArrayOf(0.72f, 0.74f, 0.78f, 1.0f),
            specular = floatArrayOf(0.88f, 0.90f, 0.95f, 1.0f),
            shininess = 75f,
            metallic = 0.85f,
            roughness = 0.28f,
            clearcoat = 0.40f
        )
        // High-output neon cyan city signage
        val NEON_CYAN = Material(
            color = floatArrayOf(0.0f, 0.9f, 1.0f, 1.0f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 80f,
            emission = floatArrayOf(1.8f, 2.5f, 2.8f, 1.0f)
        )
        // High-output neon magenta skyscraper billboard
        val NEON_MAGENTA = Material(
            color = floatArrayOf(1.0f, 0.1f, 0.8f, 1.0f),
            specular = floatArrayOf(1.0f, 0.8f, 1.0f, 1.0f),
            shininess = 80f,
            emission = floatArrayOf(2.8f, 0.4f, 2.4f, 1.0f)
        )
        // High-output neon gold commercial lighting
        val NEON_GOLD = Material(
            color = floatArrayOf(1.0f, 0.75f, 0.10f, 1.0f),
            specular = floatArrayOf(1.0f, 0.9f, 0.4f, 1.0f),
            shininess = 80f,
            emission = floatArrayOf(2.5f, 1.8f, 0.2f, 1.0f)
        )
        // Architectural precast concrete walls & safety jersey barriers
        val CONCRETE_BARRIER = Material(
            color = floatArrayOf(0.75f, 0.76f, 0.78f, 1.0f),
            specular = floatArrayOf(0.20f, 0.20f, 0.22f, 1.0f),
            shininess = 16f,
            metallic = 0.02f,
            roughness = 0.75f,
            clearcoat = 0.0f
        )
        // High-contrast outdoor sponsor hoarding board
        val SPONSOR_BOARD = Material(
            color = floatArrayOf(0.95f, 0.95f, 0.98f, 1.0f),
            specular = floatArrayOf(0.45f, 0.45f, 0.48f, 1.0f),
            shininess = 40f,
            metallic = 0.10f,
            roughness = 0.35f,
            clearcoat = 0.40f
        )
        // Coastal ocean bay water with high specular reflection
        val WATER_OCEAN = Material(
            color = floatArrayOf(0.06f, 0.38f, 0.62f, 0.95f),
            specular = floatArrayOf(0.95f, 0.95f, 1.0f, 1.0f),
            shininess = 120f,
            metallic = 0.15f,
            roughness = 0.05f,
            clearcoat = 0.95f
        )
        // Panoramic sky dome with full unlit ambient response
        val SKY_DOME = Material(
            color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            specular = floatArrayOf(0f, 0f, 0f, 0f),
            shininess = 0f,
            metallic = 0.0f,
            roughness = 1.0f,
            clearcoat = 0.0f
        )
        // High-friction gravel runoff trap
        val GRAVEL_RUNOFF = Material(
            color = floatArrayOf(0.48f, 0.44f, 0.36f, 1.0f),
            specular = floatArrayOf(0.10f, 0.10f, 0.10f, 1.0f),
            shininess = 8f,
            metallic = 0.0f,
            roughness = 0.92f,
            clearcoat = 0.0f
        )
        // Synthetic astroturf shoulder verge
        val ASTROTURF = Material(
            color = floatArrayOf(0.14f, 0.38f, 0.12f, 1.0f),
            specular = floatArrayOf(0.12f, 0.12f, 0.12f, 1.0f),
            shininess = 12f,
            metallic = 0.0f,
            roughness = 0.85f,
            clearcoat = 0.0f
        )
        // Galvanized steel debris catch fencing
        val CATCH_FENCE = Material(
            color = floatArrayOf(0.65f, 0.68f, 0.72f, 0.75f),
            specular = floatArrayOf(0.70f, 0.72f, 0.75f, 1.0f),
            shininess = 60f,
            metallic = 0.80f,
            roughness = 0.35f,
            clearcoat = 0.20f
        )
        // Impact-absorbing tire bundle wall
        val TIRE_WALL = Material(
            color = floatArrayOf(0.14f, 0.14f, 0.15f, 1.0f),
            specular = floatArrayOf(0.15f, 0.15f, 0.15f, 1.0f),
            shininess = 12f,
            metallic = 0.02f,
            roughness = 0.85f,
            clearcoat = 0.0f
        )
        // Highly reflective architectural curtain wall glass
        val GLASS_CURTAIN_WALL = Material(
            color = floatArrayOf(0.12f, 0.18f, 0.28f, 0.92f),
            specular = floatArrayOf(1.2f, 1.25f, 1.3f, 1.0f),
            shininess = 120f,
            metallic = 0.18f,
            roughness = 0.04f,
            clearcoat = 1.0f
        )
        // Grounding contact shadow quad
        val CONTACT_SHADOW = Material(
            color = floatArrayOf(0.02f, 0.02f, 0.03f, 0.78f),
            specular = floatArrayOf(0f, 0f, 0f, 0f),
            shininess = 0f,
            metallic = 0f,
            roughness = 1f,
            clearcoat = 0f
        )
    }
}
