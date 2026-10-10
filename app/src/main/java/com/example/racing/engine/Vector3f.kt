package com.example.racing.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector3f(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    fun set(nx: Float, ny: Float, nz: Float): Vector3f {
        x = nx
        y = ny
        z = nz
        return this
    }

    fun set(other: Vector3f): Vector3f {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    fun copy(): Vector3f = Vector3f(x, y, z)

    operator fun plus(other: Vector3f) = Vector3f(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3f) = Vector3f(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3f(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3f(x / scalar, y / scalar, z / scalar)

    fun add(other: Vector3f): Vector3f {
        x += other.x
        y += other.y
        z += other.z
        return this
    }

    fun add(nx: Float, ny: Float, nz: Float): Vector3f {
        x += nx
        y += ny
        z += nz
        return this
    }

    fun sub(other: Vector3f): Vector3f {
        x -= other.x
        y -= other.y
        z -= other.z
        return this
    }

    fun scale(scalar: Float): Vector3f {
        x *= scalar
        y *= scalar
        z *= scalar
        return this
    }

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalize(): Vector3f {
        val len = length()
        if (len > 1e-6f) {
            x /= len
            y /= len
            z /= len
        }
        return this
    }

    fun normalized(): Vector3f {
        val len = length()
        return if (len > 1e-6f) Vector3f(x / len, y / len, z / len) else Vector3f(0f, 0f, 0f)
    }

    fun dot(other: Vector3f): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3f): Vector3f = Vector3f(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun distanceTo(other: Vector3f): Float = (this - other).length()

    fun lerp(target: Vector3f, t: Float): Vector3f {
        val clampedT = t.coerceIn(0f, 1f)
        return Vector3f(
            x + (target.x - x) * clampedT,
            y + (target.y - y) * clampedT,
            z + (target.z - z) * clampedT
        )
    }

    companion object {
        val ZERO get() = Vector3f(0f, 0f, 0f)
        val UP get() = Vector3f(0f, 1f, 0f)
        val FORWARD get() = Vector3f(0f, 0f, 1f)
        val RIGHT get() = Vector3f(1f, 0f, 0f)
    }
}
