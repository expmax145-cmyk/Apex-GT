package com.example.racing.engine

import com.example.racing.model.TrackId
import com.example.racing.model.TrackLightingConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TrackMeshBuilder {

    data class TrackSplinePoint(
        val x: Float,
        val y: Float,
        val z: Float,
        val bankAngleDeg: Float = 0f,
        val width: Float = 14f,
        val hasLeftKerb: Boolean = false,
        val hasRightKerb: Boolean = false
    )

    class TrackModel(
        val trackId: TrackId,
        val roadMesh: Mesh,
        val kerbMesh: Mesh,
        val barrierMesh: Mesh,
        val environmentMesh: Mesh,
        val gantryMesh: Mesh,
        val waypoints: List<Vector3f>,
        val sectorCheckpoints: List<Vector3f>,
        val lightingConfig: TrackLightingConfig
    ) {
        fun release() {
            roadMesh.release()
            kerbMesh.release()
            barrierMesh.release()
            environmentMesh.release()
            gantryMesh.release()
        }
    }

    fun buildTrack(trackId: TrackId): TrackModel {
        return when (trackId) {
            TrackId.ALPINE_PASS -> buildAlpinePass()
            TrackId.NEON_METROPOLIS -> buildNeonMetropolis()
            TrackId.GRAND_PRIX -> buildGrandPrixCircuit()
        }
    }

    // 1. GRAND PRIX CIRCUIT (Apex GP)
    fun buildGrandPrixCircuit(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -80f, 0f, 14f),
            TrackSplinePoint(0f, 0f, 0f, 0f, 14f),
            TrackSplinePoint(0f, 0f, 120f, 0f, 14f),
            TrackSplinePoint(0f, 0.5f, 220f, 0f, 14f),
            TrackSplinePoint(15f, 1.0f, 300f, 3f, 13f, hasRightKerb = true),
            TrackSplinePoint(45f, 1.5f, 360f, 5f, 13f, hasRightKerb = true),
            TrackSplinePoint(30f, 2.0f, 430f, -4f, 13f, hasLeftKerb = true),
            TrackSplinePoint(10f, 2.5f, 500f, -2f, 13f, hasLeftKerb = true),
            TrackSplinePoint(10f, 3.0f, 580f, 4f, 14f),
            TrackSplinePoint(50f, 4.0f, 660f, 8f, 15f, hasRightKerb = true),
            TrackSplinePoint(120f, 4.5f, 700f, 10f, 15f, hasRightKerb = true),
            TrackSplinePoint(200f, 4.0f, 680f, 8f, 15f, hasRightKerb = true),
            TrackSplinePoint(260f, 3.0f, 600f, 6f, 14f, hasRightKerb = true),
            TrackSplinePoint(280f, 2.0f, 480f, 0f, 14f),
            TrackSplinePoint(280f, 1.0f, 320f, 0f, 14f),
            TrackSplinePoint(280f, 0.5f, 160f, 0f, 14f),
            TrackSplinePoint(280f, 0f, 20f, 0f, 14f),
            TrackSplinePoint(270f, 0f, -70f, 3f, 13f, hasRightKerb = true),
            TrackSplinePoint(240f, 0f, -140f, 5f, 13f, hasRightKerb = true),
            TrackSplinePoint(180f, 0.2f, -190f, -8f, 14f, hasLeftKerb = true),
            TrackSplinePoint(120f, 0.5f, -200f, -10f, 14f, hasLeftKerb = true),
            TrackSplinePoint(60f, 0.3f, -170f, -6f, 14f, hasLeftKerb = true),
            TrackSplinePoint(20f, 0.1f, -130f, -3f, 14f, hasLeftKerb = true),
            TrackSplinePoint(0f, 0f, -100f, 0f, 14f)
        )

        val splinePoints = interpolateCatmullRom(controlPoints, 10)
        val waypoints = splinePoints.map { Vector3f(it.x, it.y + 0.35f, it.z) }
        val sectors = listOf(
            waypoints[0],
            waypoints[(waypoints.size * 0.33).toInt()],
            waypoints[(waypoints.size * 0.66).toInt()]
        )

        val road = generateRoadMesh(splinePoints, isWet = false)
        val kerbs = generateKerbMesh(splinePoints)
        val barriers = generateBarrierMesh(splinePoints)
        val env = generateTerrainMesh(hills = true)
        val gantry = generateStartGantryMesh(splinePoints[0])

        return TrackModel(TrackId.GRAND_PRIX, road, kerbs, barriers, env, gantry, waypoints, sectors, TrackLightingConfig.GRAND_PRIX)
    }

    // 2. ALPINE MOUNTAIN PASS (Winding mountain ascent/descent)
    fun buildAlpinePass(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -60f, 0f, 13f),
            TrackSplinePoint(0f, 0f, 20f, 0f, 13f),
            // Climbing sector
            TrackSplinePoint(30f, 6.0f, 120f, 4f, 12f, hasRightKerb = true),
            TrackSplinePoint(80f, 14.0f, 180f, 6f, 12f, hasRightKerb = true),
            // Hairpin 1 (Right turnaround)
            TrackSplinePoint(140f, 20.0f, 190f, 8f, 13f, hasRightKerb = true),
            TrackSplinePoint(160f, 24.0f, 130f, -6f, 12f, hasLeftKerb = true),
            // High ridge pass
            TrackSplinePoint(140f, 29.0f, 40f, -5f, 12f, hasLeftKerb = true),
            TrackSplinePoint(110f, 32.0f, -40f, 4f, 12f),
            TrackSplinePoint(130f, 30.0f, -130f, 6f, 12f, hasRightKerb = true),
            // Summit hairpin 2
            TrackSplinePoint(90f, 25.0f, -200f, -10f, 13f, hasLeftKerb = true),
            TrackSplinePoint(30f, 18.0f, -210f, -8f, 12f, hasLeftKerb = true),
            // Downhill sweep
            TrackSplinePoint(-30f, 12.0f, -160f, 5f, 12f, hasRightKerb = true),
            TrackSplinePoint(-40f, 6.0f, -100f, 3f, 13f, hasRightKerb = true),
            TrackSplinePoint(-20f, 2.0f, -60f, 0f, 13f)
        )

        val splinePoints = interpolateCatmullRom(controlPoints, 12)
        val waypoints = splinePoints.map { Vector3f(it.x, it.y + 0.35f, it.z) }
        val sectors = listOf(
            waypoints[0],
            waypoints[(waypoints.size * 0.35).toInt()],
            waypoints[(waypoints.size * 0.70).toInt()]
        )

        val road = generateRoadMesh(splinePoints, isWet = false)
        val kerbs = generateKerbMesh(splinePoints)
        val barriers = generateBarrierMesh(splinePoints, guardrailHeight = 1.3f)
        val env = generateAlpineMountainsMesh()
        val gantry = generateStartGantryMesh(splinePoints[0])

        return TrackModel(TrackId.ALPINE_PASS, road, kerbs, barriers, env, gantry, waypoints, sectors, TrackLightingConfig.ALPINE_PASS)
    }

    // 3. NIGHT NEON METROPOLIS (Urban street circuit with wet road & skyscrapers)
    fun buildNeonMetropolis(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -80f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 0f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 150f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 260f, 0f, 15f),
            // 90-degree right turn around skyscraper
            TrackSplinePoint(30f, 0f, 320f, 2f, 15f, hasRightKerb = true),
            TrackSplinePoint(90f, 0f, 340f, 3f, 15f, hasRightKerb = true),
            // Downtown neon boulevard
            TrackSplinePoint(180f, 0f, 340f, 0f, 16f),
            TrackSplinePoint(280f, 0f, 340f, 0f, 16f),
            // Chicane through urban square
            TrackSplinePoint(340f, 0f, 300f, -4f, 14f, hasLeftKerb = true),
            TrackSplinePoint(360f, 0f, 220f, 4f, 14f, hasRightKerb = true),
            // South avenue heading back
            TrackSplinePoint(360f, 0f, 100f, 0f, 15f),
            TrackSplinePoint(360f, 0f, -20f, 0f, 15f),
            TrackSplinePoint(340f, 0f, -120f, 3f, 15f, hasRightKerb = true),
            // Final hairpin before finish
            TrackSplinePoint(260f, 0f, -180f, -8f, 14f, hasLeftKerb = true),
            TrackSplinePoint(150f, 0f, -190f, -8f, 14f, hasLeftKerb = true),
            TrackSplinePoint(50f, 0f, -160f, -4f, 15f, hasLeftKerb = true),
            TrackSplinePoint(10f, 0f, -110f, 0f, 15f)
        )

        val splinePoints = interpolateCatmullRom(controlPoints, 10)
        val waypoints = splinePoints.map { Vector3f(it.x, it.y + 0.35f, it.z) }
        val sectors = listOf(
            waypoints[0],
            waypoints[(waypoints.size * 0.33).toInt()],
            waypoints[(waypoints.size * 0.66).toInt()]
        )

        val road = generateRoadMesh(splinePoints, isWet = true)
        val kerbs = generateKerbMesh(splinePoints)
        val barriers = generateBarrierMesh(splinePoints)
        val env = generateMetropolisSkylineMesh()
        val gantry = generateStartGantryMesh(splinePoints[0])

        return TrackModel(TrackId.NEON_METROPOLIS, road, kerbs, barriers, env, gantry, waypoints, sectors, TrackLightingConfig.NEON_METROPOLIS)
    }

    private fun interpolateCatmullRom(pts: List<TrackSplinePoint>, segmentsPerPoint: Int): List<TrackSplinePoint> {
        val result = ArrayList<TrackSplinePoint>()
        val count = pts.size

        for (i in 0 until count) {
            val p0 = pts[(i - 1 + count) % count]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % count]
            val p3 = pts[(i + 2) % count]

            for (step in 0 until segmentsPerPoint) {
                val t = step.toFloat() / segmentsPerPoint
                val t2 = t * t
                val t3 = t2 * t

                val x = 0.5f * ((2f * p1.x) + (-p0.x + p2.x) * t + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)
                val y = 0.5f * ((2f * p1.y) + (-p0.y + p2.y) * t + (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 + (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)
                val z = 0.5f * ((2f * p1.z) + (-p0.z + p2.z) * t + (2f * p0.z - 5f * p1.z + 4f * p2.z - p3.z) * t2 + (-p0.z + 3f * p1.z - 3f * p2.z + p3.z) * t3)
                val bank = p1.bankAngleDeg + (p2.bankAngleDeg - p1.bankAngleDeg) * t
                val w = p1.width + (p2.width - p1.width) * t

                result.add(
                    TrackSplinePoint(
                        x, y, z, bank, w,
                        hasLeftKerb = if (t < 0.5f) p1.hasLeftKerb else p2.hasLeftKerb,
                        hasRightKerb = if (t < 0.5f) p1.hasRightKerb else p2.hasRightKerb
                    )
                )
            }
        }
        return result
    }

    private fun generateRoadMesh(spline: List<TrackSplinePoint>, isWet: Boolean): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        val count = spline.size
        val asphalt = if (isWet) floatArrayOf(0.14f, 0.15f, 0.17f) else floatArrayOf(0.24f, 0.25f, 0.27f)
        val lineWhite = floatArrayOf(0.92f, 0.92f, 0.94f)
        val racingGroove = if (isWet) floatArrayOf(0.10f, 0.11f, 0.12f) else floatArrayOf(0.18f, 0.19f, 0.20f)

        for (i in 0 until count) {
            val curr = spline[i]
            val next = spline[(i + 1) % count]

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val normalX = -(tz / tLen)
            val normalZ = (tx / tLen)

            val halfW = curr.width * 0.5f

            val lx = curr.x - normalX * halfW
            val lz = curr.z - normalZ * halfW
            val rx = curr.x + normalX * halfW
            val rz = curr.z + normalZ * halfW

            val ly = curr.y
            val ry = curr.y

            // 4 vertices across road cross-section: Left Boundary, Left Groove, Right Groove, Right Boundary
            addVertex(lx, ly, lz, 0f, 1f, 0f, lineWhite[0], lineWhite[1], lineWhite[2])
            addVertex(curr.x - normalX * (halfW * 0.5f), ly, curr.z - normalZ * (halfW * 0.5f), 0f, 1f, 0f, racingGroove[0], racingGroove[1], racingGroove[2])
            addVertex(curr.x + normalX * (halfW * 0.5f), ry, curr.z + normalZ * (halfW * 0.5f), 0f, 1f, 0f, racingGroove[0], racingGroove[1], racingGroove[2])
            addVertex(rx, ry, rz, 0f, 1f, 0f, lineWhite[0], lineWhite[1], lineWhite[2])
        }

        for (i in 0 until count) {
            val nextI = (i + 1) % count
            val base0 = (i * 4).toShort()
            val base1 = (nextI * 4).toShort()

            for (col in 0 until 3) {
                val i0 = (base0 + col).toShort()
                val i1 = (base0 + col + 1).toShort()
                val i2 = (base1 + col + 1).toShort()
                val i3 = (base1 + col).toShort()

                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateKerbMesh(spline: List<TrackSplinePoint>): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        val redKerb = floatArrayOf(0.92f, 0.15f, 0.15f)
        val whiteKerb = floatArrayOf(0.95f, 0.95f, 0.95f)

        val count = spline.size
        for (i in 0 until count) {
            val curr = spline[i]
            if (!curr.hasLeftKerb && !curr.hasRightKerb) continue

            val next = spline[(i + 1) % count]
            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val normalX = -(tz / tLen)
            val normalZ = (tx / tLen)

            val isRed = (i / 3) % 2 == 0
            val color = if (isRed) redKerb else whiteKerb

            val kerbW = 1.3f
            val kerbH = 0.08f

            if (curr.hasRightKerb) {
                val halfW = curr.width * 0.5f
                val innerX = curr.x + normalX * halfW
                val innerZ = curr.z + normalZ * halfW
                val outerX = curr.x + normalX * (halfW + kerbW)
                val outerZ = curr.z + normalZ * (halfW + kerbW)

                val nextInnerX = next.x + normalX * (next.width * 0.5f)
                val nextInnerZ = next.z + normalZ * (next.width * 0.5f)
                val nextOuterX = next.x + normalX * (next.width * 0.5f + kerbW)
                val nextOuterZ = next.z + normalZ * (next.width * 0.5f + kerbW)

                val i0 = addVertex(innerX, curr.y, innerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i1 = addVertex(outerX, curr.y + kerbH, outerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i2 = addVertex(nextOuterX, next.y + kerbH, nextOuterZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i3 = addVertex(nextInnerX, next.y, nextInnerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }

            if (curr.hasLeftKerb) {
                val halfW = curr.width * 0.5f
                val innerX = curr.x - normalX * halfW
                val innerZ = curr.z - normalZ * halfW
                val outerX = curr.x - normalX * (halfW + kerbW)
                val outerZ = curr.z - normalZ * (halfW + kerbW)

                val nextInnerX = next.x - normalX * (next.width * 0.5f)
                val nextInnerZ = next.z - normalZ * (next.width * 0.5f)
                val nextOuterX = next.x - normalX * (next.width * 0.5f + kerbW)
                val nextOuterZ = next.z - normalZ * (next.width * 0.5f + kerbW)

                val i0 = addVertex(outerX, curr.y + kerbH, outerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i1 = addVertex(innerX, curr.y, innerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i2 = addVertex(nextInnerX, next.y, nextInnerZ, 0f, 1f, 0f, color[0], color[1], color[2])
                val i3 = addVertex(nextOuterX, next.y + kerbH, nextOuterZ, 0f, 1f, 0f, color[0], color[1], color[2])
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateBarrierMesh(spline: List<TrackSplinePoint>, guardrailHeight: Float = 1.1f): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        val armcoColor = floatArrayOf(0.78f, 0.80f, 0.84f)
        val count = spline.size

        for (i in 0 until count step 2) {
            val nextI = (i + 2) % count
            val curr = spline[i]
            val next = spline[nextI]

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val normalX = -(tz / tLen)
            val normalZ = (tx / tLen)

            val offset = (curr.width * 0.5f) + 3.2f
            val hLow = curr.y + 0.2f
            val hHigh = curr.y + guardrailHeight
            val nextHLow = next.y + 0.2f
            val nextHHigh = next.y + guardrailHeight

            // Right barrier
            val rx0 = curr.x + normalX * offset
            val rz0 = curr.z + normalZ * offset
            val rx1 = next.x + normalX * offset
            val rz1 = next.z + normalZ * offset

            var i0 = addVertex(rx0, hLow, rz0, -normalX, 0f, -normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i1 = addVertex(rx1, nextHLow, rz1, -normalX, 0f, -normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i2 = addVertex(rx1, nextHHigh, rz1, -normalX, 0f, -normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i3 = addVertex(rx0, hHigh, rz0, -normalX, 0f, -normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            indices.add(i0); indices.add(i1); indices.add(i2)
            indices.add(i0); indices.add(i2); indices.add(i3)

            // Left barrier
            val lx0 = curr.x - normalX * offset
            val lz0 = curr.z - normalZ * offset
            val lx1 = next.x - normalX * offset
            val lz1 = next.z - normalZ * offset

            i0 = addVertex(lx0, hLow, lz0, normalX, 0f, normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i1 = addVertex(lx0, hHigh, lz0, normalX, 0f, normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i2 = addVertex(lx1, nextHHigh, lz1, normalX, 0f, normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i3 = addVertex(lx1, nextHLow, lz1, normalX, 0f, normalZ, armcoColor[0], armcoColor[1], armcoColor[2])
            indices.add(i0); indices.add(i1); indices.add(i2)
            indices.add(i0); indices.add(i2); indices.add(i3)
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateTerrainMesh(hills: Boolean): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        val grass1 = floatArrayOf(0.20f, 0.42f, 0.18f)
        val grass2 = floatArrayOf(0.18f, 0.38f, 0.16f)

        val gridSize = 16
        val extent = 750f
        val step = (extent * 2) / gridSize

        for (gz in 0..gridSize) {
            val z = -extent + gz * step
            for (gx in 0..gridSize) {
                val x = -extent + gx * step
                val dist = sqrt(x * x + z * z)
                val y = if (hills && dist > 300f) (dist - 300f) * 0.12f else -0.1f
                val color = if ((gx + gz) % 2 == 0) grass1 else grass2
                addVertex(x, y, z, 0f, 1f, 0f, color[0], color[1], color[2])
            }
        }

        val cols = gridSize + 1
        for (gz in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                val i0 = (gz * cols + gx).toShort()
                val i1 = (gz * cols + gx + 1).toShort()
                val i2 = ((gz + 1) * cols + gx + 1).toShort()
                val i3 = ((gz + 1) * cols + gx).toShort()
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateAlpineMountainsMesh(): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        val rockColor = floatArrayOf(0.40f, 0.38f, 0.36f)
        val snowColor = floatArrayOf(0.90f, 0.92f, 0.95f)

        val gridSize = 14
        val extent = 650f
        val step = (extent * 2) / gridSize

        for (gz in 0..gridSize) {
            val z = -extent + gz * step
            for (gx in 0..gridSize) {
                val x = -extent + gx * step
                val dist = sqrt(x * x + z * z)
                // Mountain peaks rising steeply in distance
                val height = if (dist > 180f) ((dist - 180f) * 0.32f).coerceAtMost(160f) else 0f
                val color = if (height > 65f) snowColor else rockColor
                addVertex(x, height, z, 0f, 1f, 0f, color[0], color[1], color[2])
            }
        }

        val cols = gridSize + 1
        for (gz in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                val i0 = (gz * cols + gx).toShort()
                val i1 = (gz * cols + gx + 1).toShort()
                val i2 = ((gz + 1) * cols + gx + 1).toShort()
                val i3 = ((gz + 1) * cols + gx).toShort()
                indices.add(i0); indices.add(i1); indices.add(i2)
                indices.add(i0); indices.add(i2); indices.add(i3)
            }
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateMetropolisSkylineMesh(): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        fun addBuilding(x: Float, z: Float, w: Float, d: Float, h: Float, isCyan: Boolean) {
            val bColor = floatArrayOf(0.08f, 0.10f, 0.15f)
            val windowGlow = if (isCyan) floatArrayOf(0.0f, 0.85f, 1.0f) else floatArrayOf(1.0f, 0.15f, 0.80f)

            val minX = x - w * 0.5f
            val maxX = x + w * 0.5f
            val minZ = z - d * 0.5f
            val maxZ = z + d * 0.5f

            // Front (+Z)
            var i0 = addVertex(minX, 0f, maxZ, 0f, 0f, 1f, bColor[0], bColor[1], bColor[2])
            var i1 = addVertex(maxX, 0f, maxZ, 0f, 0f, 1f, bColor[0], bColor[1], bColor[2])
            var i2 = addVertex(maxX, h, maxZ, 0f, 0f, 1f, bColor[0], bColor[1], bColor[2])
            var i3 = addVertex(minX, h, maxZ, 0f, 0f, 1f, bColor[0], bColor[1], bColor[2])
            indices.add(i0); indices.add(i1); indices.add(i2); indices.add(i0); indices.add(i2); indices.add(i3)

            // Top (+Y)
            i0 = addVertex(minX, h, maxZ, 0f, 1f, 0f, windowGlow[0], windowGlow[1], windowGlow[2])
            i1 = addVertex(maxX, h, maxZ, 0f, 1f, 0f, windowGlow[0], windowGlow[1], windowGlow[2])
            i2 = addVertex(maxX, h, minZ, 0f, 1f, 0f, windowGlow[0], windowGlow[1], windowGlow[2])
            i3 = addVertex(minX, h, minZ, 0f, 1f, 0f, windowGlow[0], windowGlow[1], windowGlow[2])
            indices.add(i0); indices.add(i1); indices.add(i2); indices.add(i0); indices.add(i2); indices.add(i3)
        }

        // Add 16 city skyscrapers bordering the track perimeter
        val towerCoords = listOf(
            Triple(-50f, 80f, 75f), Triple(-65f, 200f, 110f),
            Triple(120f, 380f, 95f), Triple(220f, 380f, 130f),
            Triple(320f, 250f, 85f), Triple(400f, 150f, 120f),
            Triple(400f, 0f, 90f), Triple(380f, -150f, 105f),
            Triple(200f, -240f, 115f), Triple(100f, -240f, 80f),
            Triple(-80f, -120f, 90f), Triple(-90f, 0f, 135f)
        )

        for (i in towerCoords.indices) {
            val (tx, tz, th) = towerCoords[i]
            addBuilding(tx, tz, 45f, 45f, th, isCyan = i % 2 == 0)
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }

    private fun generateStartGantryMesh(startPoint: TrackSplinePoint): Mesh {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(1f)
            return idx
        }

        fun addBox(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, r: Float, g: Float, b: Float) {
            val i0 = addVertex(minX, minY, maxZ, 0f, 0f, 1f, r, g, b)
            val i1 = addVertex(maxX, minY, maxZ, 0f, 0f, 1f, r, g, b)
            val i2 = addVertex(maxX, maxY, maxZ, 0f, 0f, 1f, r, g, b)
            val i3 = addVertex(minX, maxY, maxZ, 0f, 0f, 1f, r, g, b)
            indices.add(i0); indices.add(i1); indices.add(i2); indices.add(i0); indices.add(i2); indices.add(i3)

            val i4 = addVertex(maxX, minY, minZ, 0f, 0f, -1f, r, g, b)
            val i5 = addVertex(minX, minY, minZ, 0f, 0f, -1f, r, g, b)
            val i6 = addVertex(minX, maxY, minZ, 0f, 0f, -1f, r, g, b)
            val i7 = addVertex(maxX, maxY, minZ, 0f, 0f, -1f, r, g, b)
            indices.add(i4); indices.add(i5); indices.add(i6); indices.add(i4); indices.add(i6); indices.add(i7)
        }

        val frameColor = floatArrayOf(0.2f, 0.22f, 0.25f)
        val lightRed = floatArrayOf(1.0f, 0.1f, 0.1f)
        val span = startPoint.width * 0.5f + 2.5f
        val height = 6.5f

        // Pillars & Overhead Beam
        addBox(-span - 0.6f, startPoint.y, startPoint.z - 0.4f, -span, startPoint.y + height, startPoint.z + 0.4f, frameColor[0], frameColor[1], frameColor[2])
        addBox(span, startPoint.y, startPoint.z - 0.4f, span + 0.6f, startPoint.y + height, startPoint.z + 0.4f, frameColor[0], frameColor[1], frameColor[2])
        addBox(-span, startPoint.y + height - 1.2f, startPoint.z - 0.5f, span, startPoint.y + height, startPoint.z + 0.5f, frameColor[0], frameColor[1], frameColor[2])

        // F1 Start Lights
        for (k in -2..2) {
            val lx = k * 1.8f
            addBox(lx - 0.5f, startPoint.y + height - 1.0f, startPoint.z + 0.52f, lx + 0.5f, startPoint.y + height - 0.4f, startPoint.z + 0.62f, lightRed[0], lightRed[1], lightRed[2])
        }

        val vData = FloatArray(vertices.size)
        for (k in vertices.indices) vData[k] = vertices[k]
        val iData = ShortArray(indices.size)
        for (k in indices.indices) iData[k] = indices[k]

        return Mesh(vData, iData)
    }
}
