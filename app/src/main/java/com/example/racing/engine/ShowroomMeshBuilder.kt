package com.example.racing.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 11 — High-Fidelity 3D Automotive Showroom Environment Generator.
 * Constructs a premium motorsport showroom atmosphere:
 * - Circular polished turntable pedestal with chamfered brushed aluminium rim
 * - Illuminated perimeter LED accent halo ring
 * - Large-format polished granite studio floor tiles with ambient occlusion falloff
 * - Curved architectural studio cyclorama cove wall
 * - Overhead studio softbox diffuse luminaire panels for realistic roof/hood light sweeps
 * - Soft ground contact shadow quad directly under the chassis
 */
object ShowroomMeshBuilder {

    class ShowroomModel(
        val floorMesh: Mesh,
        val turntableMesh: Mesh,
        val ledRingMesh: Mesh,
        val studioCoveMesh: Mesh,
        val overheadLightsMesh: Mesh,
        val contactShadowMesh: Mesh
    ) {
        fun release() {
            floorMesh.release()
            turntableMesh.release()
            ledRingMesh.release()
            studioCoveMesh.release()
            overheadLightsMesh.release()
            contactShadowMesh.release()
        }
    }

    private class Buffer {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f
        ): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(a)
            return idx
        }

        fun addQuad(i0: Short, i1: Short, i2: Short, i3: Short) {
            indices.add(i0); indices.add(i1); indices.add(i2)
            indices.add(i0); indices.add(i2); indices.add(i3)
        }

        fun build(): Mesh {
            val vertArray = FloatArray(vertices.size) { vertices[it] }
            val indArray = ShortArray(indices.size) { indices[it] }
            return Mesh(vertArray, indArray)
        }
    }

    fun buildShowroomModel(): ShowroomModel {
        return ShowroomModel(
            floorMesh = generateFloorMesh(),
            turntableMesh = generateTurntableMesh(),
            ledRingMesh = generateLedRingMesh(),
            studioCoveMesh = generateStudioCoveMesh(),
            overheadLightsMesh = generateOverheadLightsMesh(),
            contactShadowMesh = generateContactShadowMesh()
        )
    }

    /**
     * 1. Circular Turntable Platform (Pedestal)
     * Radius 3.8m, height 0.08m with beveled brushed rim.
     */
    private fun generateTurntableMesh(): Mesh {
        val buf = Buffer()
        val segments = 32
        val platformRadius = 3.8f
        val bevelRadius = 4.05f
        val bevelHeight = -0.08f

        val topTone = floatArrayOf(0.12f, 0.13f, 0.16f)
        val rimTone = floatArrayOf(0.45f, 0.48f, 0.52f)

        // Center vertex of turntable top
        val centerIdx = buf.addVertex(0f, 0f, 0f, 0f, 1f, 0f, topTone[0], topTone[1], topTone[2])

        // Top ring vertices
        val topRingIndices = ShortArray(segments)
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * (PI.toFloat() * 2f)
            val x = platformRadius * cos(angle)
            val z = platformRadius * sin(angle)
            // Subtle radial highlight
            val shade = 1.0f + 0.08f * cos(angle * 2f)
            topRingIndices[i] = buf.addVertex(
                x, 0f, z,
                0f, 1f, 0f,
                topTone[0] * shade, topTone[1] * shade, topTone[2] * shade
            )
        }

        // Fan triangles from center to top ring
        for (i in 0 until segments) {
            val next = (i + 1) % segments
            buf.indices.add(centerIdx)
            buf.indices.add(topRingIndices[i])
            buf.indices.add(topRingIndices[next])
        }

        // Chamfered Beveled Edge
        val bevelRingIndices = ShortArray(segments)
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * (PI.toFloat() * 2f)
            val bx = bevelRadius * cos(angle)
            val bz = bevelRadius * sin(angle)
            val nx = cos(angle) * 0.7f
            val nz = sin(angle) * 0.7f
            bevelRingIndices[i] = buf.addVertex(
                bx, bevelHeight, bz,
                nx, 0.4f, nz,
                rimTone[0], rimTone[1], rimTone[2]
            )
        }

        for (i in 0 until segments) {
            val next = (i + 1) % segments
            buf.addQuad(topRingIndices[i], bevelRingIndices[i], bevelRingIndices[next], topRingIndices[next])
        }

        return buf.build()
    }

    /**
     * 2. Recessed Perimeter LED Halo Ring
     * Sleek glowing accent line surrounding the vehicle platform.
     */
    private fun generateLedRingMesh(): Mesh {
        val buf = Buffer()
        val segments = 32
        val innerR = 3.65f
        val outerR = 3.73f
        val yOffset = 0.004f // Slightly above platform top to avoid z-fighting

        val cyanGlow = floatArrayOf(0.0f, 1.8f, 2.4f) // Emissive

        val innerIndices = ShortArray(segments)
        val outerIndices = ShortArray(segments)

        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * (PI.toFloat() * 2f)
            val c = cos(angle)
            val s = sin(angle)

            innerIndices[i] = buf.addVertex(
                innerR * c, yOffset, innerR * s,
                0f, 1f, 0f,
                cyanGlow[0], cyanGlow[1], cyanGlow[2]
            )
            outerIndices[i] = buf.addVertex(
                outerR * c, yOffset, outerR * s,
                0f, 1f, 0f,
                cyanGlow[0], cyanGlow[1], cyanGlow[2]
            )
        }

        for (i in 0 until segments) {
            val next = (i + 1) % segments
            buf.addQuad(innerIndices[i], outerIndices[i], outerIndices[next], innerIndices[next])
        }

        return buf.build()
    }

    /**
     * 3. Polished Studio Floor Plane
     * Extending from the turntable edge (r = 4.05m) out to radius 32m.
     */
    private fun generateFloorMesh(): Mesh {
        val buf = Buffer()
        val rings = 4
        val segments = 24
        val innerRadius = 4.05f
        val outerRadius = 32.0f
        val floorY = -0.08f

        val nearTone = floatArrayOf(0.08f, 0.09f, 0.11f)
        val farTone = floatArrayOf(0.04f, 0.045f, 0.055f)

        val grid = Array(rings + 1) { ShortArray(segments) }

        for (r in 0..rings) {
            val t = (r.toFloat() / rings)
            val rad = innerRadius + (outerRadius - innerRadius) * t
            val toneR = nearTone[0] + (farTone[0] - nearTone[0]) * t
            val toneG = nearTone[1] + (farTone[1] - nearTone[1]) * t
            val toneB = nearTone[2] + (farTone[2] - nearTone[2]) * t

            for (s in 0 until segments) {
                val angle = (s.toFloat() / segments) * (PI.toFloat() * 2f)
                val x = rad * cos(angle)
                val z = rad * sin(angle)
                grid[r][s] = buf.addVertex(
                    x, floorY, z,
                    0f, 1f, 0f,
                    toneR, toneG, toneB
                )
            }
        }

        for (r in 0 until rings) {
            for (s in 0 until segments) {
                val next = (s + 1) % segments
                buf.addQuad(grid[r][s], grid[r + 1][s], grid[r + 1][next], grid[r][next])
            }
        }

        return buf.build()
    }

    /**
     * 4. Cylindrical Studio Cyclorama Cove Wall
     * Seamless studio background curve rising up around the stage.
     */
    private fun generateStudioCoveMesh(): Mesh {
        val buf = Buffer()
        val segments = 24
        val radius = 28.0f
        val heightSegments = 3
        val maxHeight = 12.0f
        val baseY = -0.08f

        val wallBottom = floatArrayOf(0.04f, 0.045f, 0.055f)
        val wallTop = floatArrayOf(0.02f, 0.022f, 0.030f)

        val wallGrid = Array(heightSegments + 1) { ShortArray(segments) }

        for (h in 0..heightSegments) {
            val t = (h.toFloat() / heightSegments)
            val y = baseY + maxHeight * t
            val toneR = wallBottom[0] + (wallTop[0] - wallBottom[0]) * t
            val toneG = wallBottom[1] + (wallTop[1] - wallBottom[1]) * t
            val toneB = wallBottom[2] + (wallTop[2] - wallBottom[2]) * t

            for (s in 0 until segments) {
                val angle = (s.toFloat() / segments) * (PI.toFloat() * 2f)
                val x = radius * cos(angle)
                val z = radius * sin(angle)
                // Inward-facing normal
                val nx = -cos(angle)
                val nz = -sin(angle)
                wallGrid[h][s] = buf.addVertex(
                    x, y, z,
                    nx, 0.1f, nz,
                    toneR, toneG, toneB
                )
            }
        }

        for (h in 0 until heightSegments) {
            for (s in 0 until segments) {
                val next = (s + 1) % segments
                buf.addQuad(wallGrid[h][s], wallGrid[h][next], wallGrid[h + 1][next], wallGrid[h + 1][s])
            }
        }

        return buf.build()
    }

    /**
     * 5. Overhead Studio Softbox Light Panels & Suspension Trusses
     * Suspended light fixtures above the vehicle for specular reflections.
     */
    private fun generateOverheadLightsMesh(): Mesh {
        val buf = Buffer()
        val lightWhite = floatArrayOf(2.2f, 2.2f, 2.4f)
        val trussSteel = floatArrayOf(0.25f, 0.28f, 0.32f)

        fun addBox(x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, r: Float, g: Float, b: Float) {
            // Front (+Z)
            val v0 = buf.addVertex(x1, y1, z2, 0f, 0f, 1f, r, g, b)
            val v1 = buf.addVertex(x2, y1, z2, 0f, 0f, 1f, r, g, b)
            val v2 = buf.addVertex(x2, y2, z2, 0f, 0f, 1f, r, g, b)
            val v3 = buf.addVertex(x1, y2, z2, 0f, 0f, 1f, r, g, b)
            buf.addQuad(v0, v1, v2, v3)

            // Bottom (-Y)
            val v4 = buf.addVertex(x1, y1, z1, 0f, -1f, 0f, r, g, b)
            val v5 = buf.addVertex(x2, y1, z1, 0f, -1f, 0f, r, g, b)
            val v6 = buf.addVertex(x2, y1, z2, 0f, -1f, 0f, r, g, b)
            val v7 = buf.addVertex(x1, y1, z2, 0f, -1f, 0f, r, g, b)
            buf.addQuad(v4, v5, v6, v7)

            // Top (+Y)
            val v8 = buf.addVertex(x1, y2, z2, 0f, 1f, 0f, r, g, b)
            val v9 = buf.addVertex(x2, y2, z2, 0f, 1f, 0f, r, g, b)
            val v10 = buf.addVertex(x2, y2, z1, 0f, 1f, 0f, r, g, b)
            val v11 = buf.addVertex(x1, y2, z1, 0f, 1f, 0f, r, g, b)
            buf.addQuad(v8, v9, v10, v11)
        }

        // Main Center Softbox Luminaire directly above car (Z: -2.8m to +2.8m, X: -1.2m to +1.2m, Y: 6.8m)
        addBox(-1.2f, 6.75f, -2.8f, 1.2f, 6.85f, 2.8f, lightWhite[0], lightWhite[1], lightWhite[2])

        // Left Lateral Light Strip (X: -3.2m to -2.8m, Z: -3.5m to +3.5m, Y: 6.2m)
        addBox(-3.2f, 6.15f, -3.5f, -2.8f, 6.25f, 3.5f, lightWhite[0] * 0.8f, lightWhite[1] * 0.8f, lightWhite[2] * 0.8f)

        // Right Lateral Light Strip (X: 2.8m to 3.2m, Z: -3.5m to +3.5m, Y: 6.2m)
        addBox(2.8f, 6.15f, -3.5f, 3.2f, 6.25f, 3.5f, lightWhite[0] * 0.8f, lightWhite[1] * 0.8f, lightWhite[2] * 0.8f)

        // Suspension Trusses
        addBox(-1.4f, 6.85f, -2.9f, 1.4f, 7.0f, -2.7f, trussSteel[0], trussSteel[1], trussSteel[2])
        addBox(-1.4f, 6.85f, 2.7f, 1.4f, 7.0f, 2.9f, trussSteel[0], trussSteel[1], trussSteel[2])

        return buf.build()
    }

    /**
     * 6. High-Fidelity Soft Ground Contact Shadow
     * Centered directly beneath the 3D vehicle chassis.
     */
    private fun generateContactShadowMesh(): Mesh {
        val buf = Buffer()
        val halfW = 1.18f
        val halfL = 2.40f
        val y = 0.006f // Flush above turntable surface

        val coreAlpha = 0.85f
        val edgeAlpha = 0.0f
        val shadowRGB = floatArrayOf(0.015f, 0.018f, 0.025f)

        // Inner core (darkest shadow directly under belly)
        val innerW = halfW * 0.70f
        val innerL = halfL * 0.75f

        val c0 = buf.addVertex(-innerW, y, -innerL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], coreAlpha)
        val c1 = buf.addVertex(innerW, y, -innerL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], coreAlpha)
        val c2 = buf.addVertex(innerW, y, innerL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], coreAlpha)
        val c3 = buf.addVertex(-innerW, y, innerL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], coreAlpha)
        buf.addQuad(c0, c1, c2, c3)

        // Outer soft falloff fringe
        val e0 = buf.addVertex(-halfW, y, -halfL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], edgeAlpha)
        val e1 = buf.addVertex(halfW, y, -halfL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], edgeAlpha)
        val e2 = buf.addVertex(halfW, y, halfL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], edgeAlpha)
        val e3 = buf.addVertex(-halfW, y, halfL, 0f, 1f, 0f, shadowRGB[0], shadowRGB[1], shadowRGB[2], edgeAlpha)

        // 4 fringe quads
        buf.addQuad(e0, e1, c1, c0) // front
        buf.addQuad(c3, c2, e2, e3) // rear
        buf.addQuad(e0, c0, c3, e3) // left
        buf.addQuad(c1, e1, e2, c2) // right

        return buf.build()
    }
}
