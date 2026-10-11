package com.example.racing.engine

import android.opengl.Matrix

class Matrix4f {
    val values = FloatArray(16)

    init {
        identity()
    }

    fun identity(): Matrix4f {
        try {
            Matrix.setIdentityM(values, 0)
        } catch (_: RuntimeException) {
            for (i in 0 until 16) {
                values[i] = if (i % 5 == 0) 1.0f else 0.0f
            }
        }
        return this
    }

    fun set(other: Matrix4f): Matrix4f {
        System.arraycopy(other.values, 0, values, 0, 16)
        return this
    }

    fun multiply(other: Matrix4f): Matrix4f {
        val temp = FloatArray(16)
        Matrix.multiplyMM(temp, 0, values, 0, other.values, 0)
        System.arraycopy(temp, 0, values, 0, 16)
        return this
    }

    fun translate(x: Float, y: Float, z: Float): Matrix4f {
        Matrix.translateM(values, 0, x, y, z)
        return this
    }

    fun rotate(angleDegrees: Float, x: Float, y: Float, z: Float): Matrix4f {
        Matrix.rotateM(values, 0, angleDegrees, x, y, z)
        return this
    }

    fun scale(x: Float, y: Float, z: Float): Matrix4f {
        Matrix.scaleM(values, 0, x, y, z)
        return this
    }

    fun perspective(fovyDegrees: Float, aspect: Float, zNear: Float, zFar: Float): Matrix4f {
        Matrix.perspectiveM(values, 0, fovyDegrees, aspect, zNear, zFar)
        return this
    }

    fun lookAt(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float
    ): Matrix4f {
        Matrix.setLookAtM(values, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        return this
    }

    fun copy(): Matrix4f {
        val m = Matrix4f()
        System.arraycopy(values, 0, m.values, 0, 16)
        return m
    }

    companion object {
        fun multiply(a: Matrix4f, b: Matrix4f): Matrix4f {
            val result = Matrix4f()
            Matrix.multiplyMM(result.values, 0, a.values, 0, b.values, 0)
            return result
        }
    }
}
