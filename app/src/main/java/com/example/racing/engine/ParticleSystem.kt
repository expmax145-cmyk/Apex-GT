package com.example.racing.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

/**
 * High-performance GPU particle system for tire smoke, gravel dust, and exhaust flame pops.
 */
class ParticleSystem(val maxParticles: Int = 160) {

    private class Particle {
        var x = 0f
        var y = 0f
        var z = 0f
        var vx = 0f
        var vy = 0f
        var vz = 0f
        var size = 0.4f
        var r = 1f
        var g = 1f
        var b = 1f
        var alpha = 0.6f
        var age = 0f
        var maxAge = 0.6f
        var active = false
    }

    private val particles = Array(maxParticles) { Particle() }
    private var nextIdx = 0

    // Vertex data: 6 vertices per particle (2 triangles quad), each vertex: x, y, z, u, v, r, g, b, a (9 floats)
    private val maxVertices = maxParticles * 6
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(maxVertices * 9 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()

    private var programId = 0
    private var vaoId = 0
    private var vboId = 0

    init {
        initShader()
        initBuffers()
    }

    private fun initShader() {
        val vShaderCode = """#version 300 es
            layout(location = 0) in vec3 a_Pos;
            layout(location = 1) in vec2 a_UV;
            layout(location = 2) in vec4 a_Color;

            uniform mat4 u_VP;

            out vec2 v_UV;
            out vec4 v_Color;

            void main() {
                v_UV = a_UV;
                v_Color = a_Color;
                gl_Position = u_VP * vec4(a_Pos, 1.0);
            }
        """.trimIndent()

        val fShaderCode = """#version 300 es
            precision mediump float;
            in vec2 v_UV;
            in vec4 v_Color;
            out vec4 fragColor;

            void main() {
                // Radial soft circle puff with compliant ascending smoothstep
                float dist = length(v_UV - vec2(0.5));
                if (dist > 0.5) discard;
                float falloff = 1.0 - smoothstep(0.0, 0.5, dist);
                fragColor = vec4(v_Color.rgb, v_Color.a * falloff);
            }
        """.trimIndent()

        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vShaderCode)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fShaderCode)

        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vs)
        GLES30.glAttachShader(programId, fs)
        GLES30.glLinkProgram(programId)

        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
    }

    private fun compileShader(type: Int, code: String): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, code)
        GLES30.glCompileShader(s)
        return s
    }

    private fun initBuffers() {
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vaoId = vaos[0]

        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, maxVertices * 9 * 4, null, GLES30.GL_DYNAMIC_DRAW)

        val stride = 9 * 4
        // a_Pos
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        // a_UV
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 3 * 4)
        // a_Color
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, 5 * 4)

        GLES30.glBindVertexArray(0)
    }

    fun emitTireSmoke(x: Float, y: Float, z: Float, isOffRoad: Boolean) {
        val p = particles[nextIdx]
        nextIdx = (nextIdx + 1) % maxParticles

        p.x = x + (Random.nextFloat() - 0.5f) * 0.15f
        p.y = y + 0.08f
        p.z = z + (Random.nextFloat() - 0.5f) * 0.15f

        p.vx = (Random.nextFloat() - 0.5f) * 0.8f
        p.vy = 0.6f + Random.nextFloat() * 0.8f
        p.vz = (Random.nextFloat() - 0.5f) * 0.8f

        p.size = 0.35f + Random.nextFloat() * 0.2f
        p.age = 0f
        p.maxAge = 0.55f + Random.nextFloat() * 0.25f
        p.active = true

        if (isOffRoad) {
            // Gravel/grass dust (earthy tan)
            p.r = 0.52f + Random.nextFloat() * 0.08f
            p.g = 0.44f + Random.nextFloat() * 0.06f
            p.b = 0.32f + Random.nextFloat() * 0.05f
            p.alpha = 0.55f
        } else {
            // Hot rubber smoke (volumetric light grey/white)
            val grey = 0.82f + Random.nextFloat() * 0.12f
            p.r = grey
            p.g = grey
            p.b = grey
            p.alpha = 0.48f
        }
    }

    fun emitExhaustFlame(x: Float, y: Float, z: Float) {
        val p = particles[nextIdx]
        nextIdx = (nextIdx + 1) % maxParticles

        p.x = x
        p.y = y
        p.z = z
        p.vx = (Random.nextFloat() - 0.5f) * 0.4f
        p.vy = 0.2f + Random.nextFloat() * 0.4f
        p.vz = -1.2f - Random.nextFloat() * 1.5f

        p.size = 0.25f
        p.age = 0f
        p.maxAge = 0.18f // Quick pop flash
        p.active = true

        p.r = 1.0f
        p.g = 0.55f
        p.b = 0.15f
        p.alpha = 0.95f
    }

    fun emitCollisionSparks(x: Float, y: Float, z: Float, intensity: Float = 1.0f) {
        val count = (4 + (intensity * 8f).toInt()).coerceIn(3, 12)
        for (i in 0 until count) {
            val p = particles[nextIdx]
            nextIdx = (nextIdx + 1) % maxParticles

            p.x = x + (Random.nextFloat() - 0.5f) * 0.22f
            p.y = y + 0.1f + Random.nextFloat() * 0.2f
            p.z = z + (Random.nextFloat() - 0.5f) * 0.22f

            p.vx = (Random.nextFloat() - 0.5f) * 3.2f * intensity
            p.vy = 1.0f + Random.nextFloat() * 2.2f * intensity
            p.vz = (Random.nextFloat() - 0.5f) * 3.2f * intensity

            p.size = 0.10f + Random.nextFloat() * 0.08f
            p.age = 0f
            p.maxAge = 0.18f + Random.nextFloat() * 0.14f
            p.active = true

            p.r = 1.0f
            p.g = 0.82f + Random.nextFloat() * 0.18f
            p.b = 0.20f
            p.alpha = 0.95f
        }
    }

    fun update(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.age += dt
            if (p.age >= p.maxAge) {
                p.active = false
                continue
            }

            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt

            // Smoke expands and decelerates
            p.size += dt * 1.2f
            p.vx *= 0.94f
            p.vy *= 0.96f
            p.vz *= 0.94f
        }
    }

    fun draw(vpMatrix: FloatArray, camPos: Vector3f) {
        if (programId == 0) return

        vertexBuffer.clear()
        var activeCount = 0

        for (p in particles) {
            if (!p.active) continue

            val progress = p.age / p.maxAge
            val currentAlpha = p.alpha * (1f - progress)
            val halfS = p.size * 0.5f

            // Vector from camera to particle
            val dx = p.x - camPos.x
            val dz = p.z - camPos.z
            val len = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
            // Camera right vector in XZ plane
            val rX = -dz / len * halfS
            val rZ = dx / len * halfS
            val uY = halfS

            // Quad vertices: 2 triangles (0,1,2 and 0,2,3) -> 6 vertices
            // V0: bottom-left
            putVertex(p.x - rX, p.y - uY, p.z - rZ, 0f, 0f, p.r, p.g, p.b, currentAlpha)
            // V1: bottom-right
            putVertex(p.x + rX, p.y - uY, p.z + rZ, 1f, 0f, p.r, p.g, p.b, currentAlpha)
            // V2: top-right
            putVertex(p.x + rX, p.y + uY, p.z + rZ, 1f, 1f, p.r, p.g, p.b, currentAlpha)

            // V0: bottom-left
            putVertex(p.x - rX, p.y - uY, p.z - rZ, 0f, 0f, p.r, p.g, p.b, currentAlpha)
            // V2: top-right
            putVertex(p.x + rX, p.y + uY, p.z + rZ, 1f, 1f, p.r, p.g, p.b, currentAlpha)
            // V3: top-left
            putVertex(p.x - rX, p.y + uY, p.z - rZ, 0f, 1f, p.r, p.g, p.b, currentAlpha)

            activeCount++
            if (activeCount >= maxParticles) break
        }

        if (activeCount == 0) return

        vertexBuffer.flip()

        GLES30.glUseProgram(programId)
        val loc = GLES30.glGetUniformLocation(programId, "u_VP")
        if (loc >= 0) {
            GLES30.glUniformMatrix4fv(loc, 1, false, vpMatrix, 0)
        }

        // Particle blending: alpha blend without writing depth buffer to prevent haloing
        GLES30.glDepthMask(false)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, activeCount * 6 * 9 * 4, vertexBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, activeCount * 6)

        GLES30.glBindVertexArray(0)
        GLES30.glDepthMask(true)
    }

    private fun putVertex(x: Float, y: Float, z: Float, u: Float, v: Float, r: Float, g: Float, b: Float, a: Float) {
        vertexBuffer.put(x); vertexBuffer.put(y); vertexBuffer.put(z)
        vertexBuffer.put(u); vertexBuffer.put(v)
        vertexBuffer.put(r); vertexBuffer.put(g); vertexBuffer.put(b); vertexBuffer.put(a)
    }

    fun release() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            vaoId = 0
        }
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }
}
