package com.example.racing.engine

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
        val CAR_PAINT_METALLIC = Material(
            color = floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f),
            specular = floatArrayOf(1f, 0.95f, 0.95f, 1.0f),
            shininess = 90f,
            metallic = 0.88f,
            roughness = 0.12f,
            clearcoat = 0.95f
        )
        val CARBON_FIBER = Material(
            color = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f),
            specular = floatArrayOf(0.45f, 0.45f, 0.45f, 1.0f),
            shininess = 28f,
            metallic = 0.25f,
            roughness = 0.45f,
            clearcoat = 0.4f
        )
        val RUBBER_TIRE = Material(
            color = floatArrayOf(0.15f, 0.15f, 0.16f, 1.0f),
            specular = floatArrayOf(0.18f, 0.18f, 0.18f, 1.0f),
            shininess = 8f,
            metallic = 0.02f,
            roughness = 0.88f
        )
        val ALLOY_RIM = Material(
            color = floatArrayOf(0.88f, 0.88f, 0.92f, 1.0f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 110f,
            metallic = 0.96f,
            roughness = 0.08f,
            clearcoat = 0.8f
        )
        val GLASS_TINT = Material(
            color = floatArrayOf(0.12f, 0.16f, 0.22f, 0.82f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 120f,
            metallic = 0.15f,
            roughness = 0.03f,
            clearcoat = 1.0f
        )
        val HEADLIGHT_LED = Material(
            color = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f),
            specular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
            shininess = 90f,
            emission = floatArrayOf(1.2f, 1.3f, 1.5f, 1.0f)
        )
        val TAILLIGHT_OFF = Material(
            color = floatArrayOf(0.65f, 0.08f, 0.08f, 1.0f),
            specular = floatArrayOf(0.8f, 0.3f, 0.3f, 1.0f),
            shininess = 70f,
            emission = floatArrayOf(0.25f, 0.03f, 0.03f, 1.0f)
        )
        val TAILLIGHT_BRAKE = Material(
            color = floatArrayOf(1.0f, 0.12f, 0.12f, 1.0f),
            specular = floatArrayOf(1.0f, 0.6f, 0.6f, 1.0f),
            shininess = 90f,
            emission = floatArrayOf(2.2f, 0.25f, 0.25f, 1.0f)
        )
        val ASPHALT_DRY = Material(
            color = floatArrayOf(0.24f, 0.25f, 0.27f, 1.0f),
            specular = floatArrayOf(0.22f, 0.22f, 0.22f, 1.0f),
            shininess = 14f,
            metallic = 0.05f,
            roughness = 0.82f
        )
        val ASPHALT_WET = Material(
            color = floatArrayOf(0.14f, 0.15f, 0.17f, 1.0f),
            specular = floatArrayOf(0.85f, 0.85f, 0.90f, 1.0f),
            shininess = 95f,
            metallic = 0.35f,
            roughness = 0.18f,
            clearcoat = 0.9f
        )
        val GRASS = Material(
            color = floatArrayOf(0.20f, 0.40f, 0.16f, 1.0f),
            specular = floatArrayOf(0.08f, 0.08f, 0.08f, 1.0f),
            shininess = 6f,
            metallic = 0.0f,
            roughness = 0.92f
        )
        val MOUNTAIN_ROCK = Material(
            color = floatArrayOf(0.42f, 0.40f, 0.38f, 1.0f),
            specular = floatArrayOf(0.18f, 0.18f, 0.18f, 1.0f),
            shininess = 12f,
            metallic = 0.1f,
            roughness = 0.85f
        )
        val NEON_CYAN = Material(
            color = floatArrayOf(0.0f, 0.9f, 1.0f, 1.0f),
            emission = floatArrayOf(1.5f, 2.2f, 2.5f, 1.0f)
        )
        val NEON_MAGENTA = Material(
            color = floatArrayOf(1.0f, 0.1f, 0.8f, 1.0f),
            emission = floatArrayOf(2.5f, 0.3f, 2.0f, 1.0f)
        )
        val CONTACT_SHADOW = Material(
            color = floatArrayOf(0.03f, 0.03f, 0.04f, 0.72f),
            specular = floatArrayOf(0f, 0f, 0f, 0f),
            shininess = 0f,
            metallic = 0f,
            roughness = 1f
        )
    }
}
