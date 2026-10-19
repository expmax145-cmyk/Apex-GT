package com.example.racing.engine

import com.example.racing.model.TrackId
import com.example.racing.model.TrackLightingConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural Race Circuit Mesh & Environmental World Generator for Apex GT — Phase 16.
 *
 * Synthesizes high-fidelity 3D track surfaces, FIA-standard kerbs, Armco barriers,
 * tire walls, rich immersive trackside environments, and panoramic sky domes for:
 * 1. Apex GP Circuit (Professional Coastal Motorsport Venue)
 * 2. Alpine Mountain Pass (High-Altitude Mountain Ascent & Switchbacks)
 * 3. Night Neon Metropolis (Night City Boulevard & Urban Street Circuit)
 */
object TrackMeshBuilder {

    data class TrackSplinePoint(
        val x: Float,
        val y: Float,
        val z: Float,
        val bankAngleDeg: Float = 0f,
        val width: Float = 14f,
        val hasLeftKerb: Boolean = false,
        val hasRightKerb: Boolean = false,
        val hasRunoffLeft: Boolean = false,
        val hasRunoffRight: Boolean = false
    )

    class TrackModel(
        val trackId: TrackId,
        val roadMesh: Mesh,
        val kerbMesh: Mesh,
        val barrierMesh: Mesh,
        val environmentMesh: Mesh,
        val gantryMesh: Mesh,
        val skyMesh: Mesh,
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
            skyMesh.release()
        }
    }

    fun buildTrack(trackId: TrackId): TrackModel {
        return when (trackId) {
            TrackId.ALPINE_PASS -> buildAlpinePass()
            TrackId.NEON_METROPOLIS -> buildNeonMetropolis()
            TrackId.GRAND_PRIX -> buildGrandPrixCircuit()
        }
    }

    // =============================================================================================
    // 1. GRAND PRIX CIRCUIT (Professional Coastal Motorsport Venue)
    // =============================================================================================
    fun buildGrandPrixCircuit(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -80f, 0f, 14f),
            TrackSplinePoint(0f, 0f, 0f, 0f, 14f),
            TrackSplinePoint(0f, 0f, 120f, 0f, 14f),
            TrackSplinePoint(0f, 0.5f, 220f, 0f, 14f),
            // Turn 1 (Castrol Sweeper): High-speed entry with banked camber & outside runoff
            TrackSplinePoint(15f, 1.0f, 300f, 5f, 14f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(45f, 1.8f, 360f, 7f, 14f, hasRightKerb = true, hasRunoffLeft = true),
            // Turns 2-3 (Esses Chicane): Technical alternating curbs
            TrackSplinePoint(30f, 2.2f, 430f, -6f, 13.5f, hasLeftKerb = true, hasRunoffRight = true),
            TrackSplinePoint(10f, 2.8f, 500f, -4f, 13.5f, hasLeftKerb = true),
            // Turn 4 (Marina Carousel): Long sweeping banked curve overlooking the coastal bay
            TrackSplinePoint(10f, 3.2f, 580f, 5f, 14f),
            TrackSplinePoint(50f, 4.2f, 660f, 9f, 15f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(120f, 4.8f, 700f, 11f, 15f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(200f, 4.2f, 680f, 9f, 15f, hasRightKerb = true, hasRunoffLeft = true),
            // Back Straight: High-speed downhill descent
            TrackSplinePoint(260f, 3.2f, 600f, 5f, 14f, hasRightKerb = true),
            TrackSplinePoint(280f, 2.2f, 480f, 0f, 14f),
            TrackSplinePoint(280f, 1.2f, 320f, 0f, 14f),
            TrackSplinePoint(280f, 0.6f, 160f, 0f, 14f),
            TrackSplinePoint(280f, 0f, 20f, 0f, 14f),
            // Turn 5-6 (Stadium Infield & Harbor Hairpin)
            TrackSplinePoint(270f, 0f, -70f, 4f, 13.5f, hasRightKerb = true),
            TrackSplinePoint(240f, 0.1f, -140f, 7f, 13.5f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(180f, 0.3f, -190f, -9f, 14f, hasLeftKerb = true, hasRunoffRight = true),
            TrackSplinePoint(120f, 0.5f, -200f, -11f, 14f, hasLeftKerb = true, hasRunoffRight = true),
            TrackSplinePoint(60f, 0.3f, -170f, -7f, 14f, hasLeftKerb = true),
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

        val road = generateRoadMesh(splinePoints, trackType = TrackType.GRAND_PRIX)
        val kerbs = generateKerbMesh(splinePoints, trackType = TrackType.GRAND_PRIX)
        val barriers = generateBarrierMesh(splinePoints, trackType = TrackType.GRAND_PRIX)
        val env = generateGrandPrixSceneryMesh(splinePoints)
        val gantry = generateStartGantryMesh(splinePoints[0])
        val sky = generateSkyDomeMesh(TrackType.GRAND_PRIX, TrackLightingConfig.GRAND_PRIX)
        
        // Add detailed braking markers at 150m, 100m, 50m before turns
        val envWithMarkers = addBrakingMarkers(env, splinePoints)

        return TrackModel(TrackId.GRAND_PRIX, road, kerbs, barriers, envWithMarkers, gantry, sky, waypoints, sectors, TrackLightingConfig.GRAND_PRIX)
    }

    // =============================================================================================
    // 2. ALPINE MOUNTAIN PASS (High-Altitude Mountain Ascent & Switchbacks)
    // =============================================================================================
    fun buildAlpinePass(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -60f, 0f, 13f),
            TrackSplinePoint(0f, 0f, 20f, 0f, 13f),
            // Climbing Ascent Sector
            TrackSplinePoint(30f, 6.5f, 120f, 4f, 12.5f, hasRightKerb = true),
            TrackSplinePoint(80f, 14.5f, 180f, 6f, 12.5f, hasRightKerb = true),
            // Hairpin 1 (Lower Switchback Turnaround)
            TrackSplinePoint(140f, 21.0f, 190f, 10f, 13.5f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(160f, 25.0f, 130f, -8f, 12.5f, hasLeftKerb = true),
            // High Ridge Pass
            TrackSplinePoint(140f, 30.0f, 40f, -6f, 12.5f, hasLeftKerb = true),
            TrackSplinePoint(110f, 33.5f, -40f, 4f, 12.5f),
            TrackSplinePoint(130f, 31.0f, -130f, 8f, 12.5f, hasRightKerb = true),
            // Summit Hairpin 2 (Alpine Peak Turnaround)
            TrackSplinePoint(90f, 26.0f, -200f, -12f, 13.5f, hasLeftKerb = true, hasRunoffRight = true),
            TrackSplinePoint(30f, 19.0f, -210f, -9f, 12.5f, hasLeftKerb = true),
            // Downhill Valley Descent
            TrackSplinePoint(-30f, 12.5f, -160f, 6f, 12.5f, hasRightKerb = true),
            TrackSplinePoint(-40f, 6.5f, -100f, 3f, 13f, hasRightKerb = true),
            TrackSplinePoint(-20f, 2.0f, -60f, 0f, 13f)
        )

        val splinePoints = interpolateCatmullRom(controlPoints, 12)
        val waypoints = splinePoints.map { Vector3f(it.x, it.y + 0.35f, it.z) }
        val sectors = listOf(
            waypoints[0],
            waypoints[(waypoints.size * 0.35).toInt()],
            waypoints[(waypoints.size * 0.70).toInt()]
        )

        val road = generateRoadMesh(splinePoints, trackType = TrackType.ALPINE_PASS)
        val kerbs = generateKerbMesh(splinePoints, trackType = TrackType.ALPINE_PASS)
        val barriers = generateBarrierMesh(splinePoints, trackType = TrackType.ALPINE_PASS)
        val env = generateAlpineSceneryMesh(splinePoints)
        val gantry = generateStartGantryMesh(splinePoints[0])
        val sky = generateSkyDomeMesh(TrackType.ALPINE_PASS, TrackLightingConfig.ALPINE_PASS)

        return TrackModel(TrackId.ALPINE_PASS, road, kerbs, barriers, env, gantry, sky, waypoints, sectors, TrackLightingConfig.ALPINE_PASS)
    }

    // =============================================================================================
    // 3. NIGHT NEON METROPOLIS (Night City Boulevard & Urban Street Circuit)
    // =============================================================================================
    fun buildNeonMetropolis(): TrackModel {
        val controlPoints = listOf(
            TrackSplinePoint(0f, 0f, -80f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 0f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 150f, 0f, 15f),
            TrackSplinePoint(0f, 0f, 260f, 0f, 15f),
            // 90-degree right turn around glass commercial skyscraper
            TrackSplinePoint(30f, 0f, 320f, 2.5f, 15f, hasRightKerb = true, hasRunoffLeft = true),
            TrackSplinePoint(90f, 0f, 340f, 3.5f, 15f, hasRightKerb = true, hasRunoffLeft = true),
            // Downtown neon boulevard
            TrackSplinePoint(180f, 0f, 340f, 0f, 16f),
            TrackSplinePoint(280f, 0f, 340f, 0f, 16f),
            // Chicane through urban square
            TrackSplinePoint(340f, 0f, 300f, -4f, 14.5f, hasLeftKerb = true),
            TrackSplinePoint(360f, 0f, 220f, 4f, 14.5f, hasRightKerb = true),
            // South avenue heading back
            TrackSplinePoint(360f, 0f, 100f, 0f, 15f),
            TrackSplinePoint(360f, 0f, -20f, 0f, 15f),
            TrackSplinePoint(340f, 0f, -120f, 3f, 15f, hasRightKerb = true),
            // Final hairpin before finish
            TrackSplinePoint(260f, 0f, -180f, -8f, 14.5f, hasLeftKerb = true, hasRunoffRight = true),
            TrackSplinePoint(150f, 0f, -190f, -8f, 14.5f, hasLeftKerb = true, hasRunoffRight = true),
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

        val road = generateRoadMesh(splinePoints, trackType = TrackType.NEON_METROPOLIS)
        val kerbs = generateKerbMesh(splinePoints, trackType = TrackType.NEON_METROPOLIS)
        val barriers = generateBarrierMesh(splinePoints, trackType = TrackType.NEON_METROPOLIS)
        val env = generateMetropolisSceneryMesh(splinePoints)
        val gantry = generateStartGantryMesh(splinePoints[0])
        val sky = generateSkyDomeMesh(TrackType.NEON_METROPOLIS, TrackLightingConfig.NEON_METROPOLIS)

        return TrackModel(TrackId.NEON_METROPOLIS, road, kerbs, barriers, env, gantry, sky, waypoints, sectors, TrackLightingConfig.NEON_METROPOLIS)
    }

    private enum class TrackType {
        GRAND_PRIX, ALPINE_PASS, NEON_METROPOLIS
    }

    // =============================================================================================
    // Spline Interpolation with Catmull-Rom
    // =============================================================================================
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
                        hasRightKerb = if (t < 0.5f) p1.hasRightKerb else p2.hasRightKerb,
                        hasRunoffLeft = if (t < 0.5f) p1.hasRunoffLeft else p2.hasRunoffLeft,
                        hasRunoffRight = if (t < 0.5f) p1.hasRunoffRight else p2.hasRunoffRight
                    )
                )
            }
        }
        return result
    }

    // =============================================================================================
    // Generic Mesh Buffer Helper with Advanced Primitive Generators
    // =============================================================================================
    private class MeshBuffer {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, a: Float = 1f): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            vertices.add(nx); vertices.add(ny); vertices.add(nz)
            vertices.add(r); vertices.add(g); vertices.add(b); vertices.add(a)
            return idx
        }

        fun addTriangle(i0: Short, i1: Short, i2: Short) {
            indices.add(i0); indices.add(i1); indices.add(i2)
        }

        fun addQuad(i0: Short, i1: Short, i2: Short, i3: Short) {
            indices.add(i0); indices.add(i1); indices.add(i2)
            indices.add(i0); indices.add(i2); indices.add(i3)
        }

        fun addBox(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
            // Front (+Z)
            var i0 = addVertex(minX, minY, maxZ, 0f, 0f, 1f, r, g, b, a)
            var i1 = addVertex(maxX, minY, maxZ, 0f, 0f, 1f, r, g, b, a)
            var i2 = addVertex(maxX, maxY, maxZ, 0f, 0f, 1f, r, g, b, a)
            var i3 = addVertex(minX, maxY, maxZ, 0f, 0f, 1f, r, g, b, a)
            addQuad(i0, i1, i2, i3)

            // Back (-Z)
            i0 = addVertex(maxX, minY, minZ, 0f, 0f, -1f, r, g, b, a)
            i1 = addVertex(minX, minY, minZ, 0f, 0f, -1f, r, g, b, a)
            i2 = addVertex(minX, maxY, minZ, 0f, 0f, -1f, r, g, b, a)
            i3 = addVertex(maxX, maxY, minZ, 0f, 0f, -1f, r, g, b, a)
            addQuad(i0, i1, i2, i3)

            // Top (+Y)
            i0 = addVertex(minX, maxY, maxZ, 0f, 1f, 0f, r, g, b, a)
            i1 = addVertex(maxX, maxY, maxZ, 0f, 1f, 0f, r, g, b, a)
            i2 = addVertex(maxX, maxY, minZ, 0f, 1f, 0f, r, g, b, a)
            i3 = addVertex(minX, maxY, minZ, 0f, 1f, 0f, r, g, b, a)
            addQuad(i0, i1, i2, i3)

            // Bottom (-Y)
            i0 = addVertex(minX, minY, minZ, 0f, -1f, 0f, r, g, b, a)
            i1 = addVertex(maxX, minY, minZ, 0f, -1f, 0f, r, g, b, a)
            i2 = addVertex(maxX, minY, maxZ, 0f, -1f, 0f, r, g, b, a)
            i3 = addVertex(minX, minY, maxZ, 0f, -1f, 0f, r, g, b, a)
            addQuad(i0, i1, i2, i3)

            // Left (-X)
            i0 = addVertex(minX, minY, minZ, -1f, 0f, 0f, r, g, b, a)
            i1 = addVertex(minX, minY, maxZ, -1f, 0f, 0f, r, g, b, a)
            i2 = addVertex(minX, maxY, maxZ, -1f, 0f, 0f, r, g, b, a)
            i3 = addVertex(minX, maxY, minZ, -1f, 0f, 0f, r, g, b, a)
            addQuad(i0, i1, i2, i3)

            // Right (+X)
            i0 = addVertex(maxX, minY, maxZ, 1f, 0f, 0f, r, g, b, a)
            i1 = addVertex(maxX, minY, minZ, 1f, 0f, 0f, r, g, b, a)
            i2 = addVertex(maxX, maxY, minZ, 1f, 0f, 0f, r, g, b, a)
            i3 = addVertex(maxX, maxY, maxZ, 1f, 0f, 0f, r, g, b, a)
            addQuad(i0, i1, i2, i3)
        }

        fun build(): Mesh {
            val vData = FloatArray(vertices.size)
            for (k in vertices.indices) vData[k] = vertices[k]
            val iData = ShortArray(indices.size)
            for (k in indices.indices) iData[k] = indices[k]
            return Mesh(vData, iData)
        }
    }

    // =============================================================================================
    // 4. ROAD SURFACE GENERATOR WITH AUTHENTIC MOTORSPORT MARKINGS & RUBBER GROOVE
    // =============================================================================================
    private fun generateRoadMesh(
        spline: List<TrackSplinePoint>,
        trackType: TrackType
    ): Mesh {
        val mb = MeshBuffer()
        val count = spline.size

        // Surface color palettes
        val asphaltDry = floatArrayOf(0.24f, 0.25f, 0.27f)
        val asphaltWet = floatArrayOf(0.12f, 0.13f, 0.15f)
        val asphaltMountain = floatArrayOf(0.28f, 0.27f, 0.26f)
        val racingGroove = floatArrayOf(0.12f, 0.13f, 0.14f)
        val skidMarkDark = floatArrayOf(0.06f, 0.06f, 0.07f)
        val whiteLine = floatArrayOf(0.96f, 0.96f, 0.98f)
        val yellowLine = floatArrayOf(0.96f, 0.80f, 0.14f)
        val gridBoxYellow = floatArrayOf(1.0f, 0.86f, 0.16f)
        val gravelRunoff = floatArrayOf(0.48f, 0.44f, 0.36f)
        val astroturfVerge = floatArrayOf(0.14f, 0.38f, 0.12f)
        val mountainShoulder = floatArrayOf(0.38f, 0.35f, 0.31f)
        val urbanCurbRunoff = floatArrayOf(0.42f, 0.44f, 0.47f)

        val isWet = trackType == TrackType.NEON_METROPOLIS
        val baseAsphalt = when (trackType) {
            TrackType.NEON_METROPOLIS -> asphaltWet
            TrackType.ALPINE_PASS -> asphaltMountain
            TrackType.GRAND_PRIX -> asphaltDry
        }
        val defaultOuterRunoff = when (trackType) {
            TrackType.NEON_METROPOLIS -> urbanCurbRunoff
            TrackType.ALPINE_PASS -> mountainShoulder
            TrackType.GRAND_PRIX -> gravelRunoff
        }
        val defaultInnerRunoff = when (trackType) {
            TrackType.GRAND_PRIX -> astroturfVerge
            TrackType.ALPINE_PASS -> mountainShoulder
            TrackType.NEON_METROPOLIS -> urbanCurbRunoff
        }

        for (i in 0 until count) {
            val curr = spline[i]
            val next = spline[(i + 1) % count]

            // Forward tangent
            val tx = next.x - curr.x
            val ty = next.y - curr.y
            val tz = next.z - curr.z
            val tHoriz = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val tangentX = tx / tHoriz
            val tangentZ = tz / tHoriz

            // Right normal
            val perpX = -tangentZ
            val perpZ = tangentX

            // Bank angle rotation
            val bankRad = Math.toRadians(curr.bankAngleDeg.toDouble()).toFloat()
            val cosB = cos(bankRad)
            val sinB = sin(bankRad)

            // Right vector tilted with banking
            val rx = perpX * cosB
            val ry = sinB
            val rz = perpZ * cosB

            // Up normal vector
            val nx = -perpX * sinB
            val ny = cosB
            val nz = -perpZ * sinB

            val halfW = curr.width * 0.5f

            // Start line checkered pattern at i = 0
            val isStartLine = i == 0
            // Starting grid markings for i in 1..8
            val isGridZone = i in 1..8
            // Centerline dash pattern (3 segments on, 3 segments off)
            val isCenterMarked = (i % 6) < 3

            // Braking zones with heavy tire skid marks
            val isBrakingZone = when (trackType) {
                TrackType.GRAND_PRIX -> (curr.z in 210f..285f) || (curr.z in 400f..440f) || (curr.z in -90f..-30f)
                TrackType.ALPINE_PASS -> (curr.z in 160f..190f) || (curr.z in -180f..-140f)
                TrackType.NEON_METROPOLIS -> (curr.z in 220f..260f) || (curr.z in 310f..340f)
            }

            // Runway width offsets:
            // 0: Left Runoff Outer (Gravel/Asphalt)
            // 1: Left Runoff Inner (Astroturf/Apron)
            // 2: Left Boundary Line (Solid White)
            // 3: Left Shoulder / Outer Lane
            // 4: Left Racing Groove (Dark rubber / skid marks)
            // 5: Track Centerline (Crown peak: +0.038m)
            // 6: Right Racing Groove (Dark rubber / skid marks)
            // 7: Right Shoulder / Outer Lane
            // 8: Right Boundary Line (Solid White)
            // 9: Right Runoff Inner (Astroturf/Apron)
            // 10: Right Runoff Outer (Gravel/Asphalt)

            val runoffLeftW = if (curr.hasRunoffLeft) 4.2f else 1.2f
            val runoffRightW = if (curr.hasRunoffRight) 4.2f else 1.2f

            val colLeftBorder = whiteLine
            val colRightBorder = whiteLine

            // Determine center line color
            val colCenter = when (trackType) {
                TrackType.ALPINE_PASS -> {
                    if (curr.hasLeftKerb || curr.hasRightKerb || !isCenterMarked) yellowLine else asphaltMountain
                }
                TrackType.NEON_METROPOLIS -> {
                    if (isStartLine) whiteLine else if (isCenterMarked) whiteLine else asphaltWet
                }
                TrackType.GRAND_PRIX -> {
                    if (isStartLine) whiteLine else if (isCenterMarked) whiteLine else racingGroove
                }
            }

            // Left & Right lane colors with dynamic skid marks
            var colLaneLeft = if (isBrakingZone) skidMarkDark else if (isWet) asphaltWet else racingGroove
            var colLaneRight = if (isBrakingZone) skidMarkDark else if (isWet) asphaltWet else racingGroove

            if (isStartLine) {
                // Checkered starting line pattern alternating across lanes
                colLaneLeft = if (i % 2 == 0) whiteLine else baseAsphalt
                colLaneRight = if (i % 2 == 1) whiteLine else baseAsphalt
            } else if (isGridZone && trackType == TrackType.GRAND_PRIX) {
                val isLeftBox = (i % 4) == 2
                val isRightBox = (i % 4) == 0
                if (isLeftBox) colLaneLeft = gridBoxYellow
                if (isRightBox) colLaneRight = gridBoxYellow
            }

            // Col 0: Left Runoff Outer
            var px = curr.x - rx * (halfW + runoffLeftW)
            var py = curr.y - ry * (halfW + runoffLeftW) - 0.04f
            var pz = curr.z - rz * (halfW + runoffLeftW)
            mb.addVertex(px, py, pz, nx, ny, nz, defaultOuterRunoff[0], defaultOuterRunoff[1], defaultOuterRunoff[2])

            // Col 1: Left Runoff Inner / Astroturf Verge
            px = curr.x - rx * (halfW + 0.50f)
            py = curr.y - ry * (halfW + 0.50f) - 0.01f
            pz = curr.z - rz * (halfW + 0.50f)
            mb.addVertex(px, py, pz, nx, ny, nz, defaultInnerRunoff[0], defaultInnerRunoff[1], defaultInnerRunoff[2])

            // Col 2: Left Boundary Line
            px = curr.x - rx * halfW
            py = curr.y - ry * halfW
            pz = curr.z - rz * halfW
            mb.addVertex(px, py, pz, nx, ny, nz, colLeftBorder[0], colLeftBorder[1], colLeftBorder[2])

            // Col 3: Left Shoulder
            px = curr.x - rx * (halfW * 0.72f)
            py = curr.y - ry * (halfW * 0.72f) + 0.015f
            pz = curr.z - rz * (halfW * 0.72f)
            mb.addVertex(px, py, pz, nx, ny, nz, baseAsphalt[0], baseAsphalt[1], baseAsphalt[2])

            // Col 4: Left Racing Groove (camber crown: +0.025m)
            px = curr.x - rx * (halfW * 0.38f)
            py = curr.y - ry * (halfW * 0.38f) + 0.025f
            pz = curr.z - rz * (halfW * 0.38f)
            mb.addVertex(px, py, pz, nx, ny, nz, colLaneLeft[0], colLaneLeft[1], colLaneLeft[2])

            // Col 5: Track Centerline (crown peak: +0.038m)
            px = curr.x
            py = curr.y + 0.038f
            pz = curr.z
            mb.addVertex(px, py, pz, nx, ny, nz, colCenter[0], colCenter[1], colCenter[2])

            // Col 6: Right Racing Groove (camber crown: +0.025m)
            px = curr.x + rx * (halfW * 0.38f)
            py = curr.y + ry * (halfW * 0.38f) + 0.025f
            pz = curr.z + rz * (halfW * 0.38f)
            mb.addVertex(px, py, pz, nx, ny, nz, colLaneRight[0], colLaneRight[1], colLaneRight[2])

            // Col 7: Right Shoulder
            px = curr.x + rx * (halfW * 0.72f)
            py = curr.y + ry * (halfW * 0.72f) + 0.015f
            pz = curr.z + rz * (halfW * 0.72f)
            mb.addVertex(px, py, pz, nx, ny, nz, baseAsphalt[0], baseAsphalt[1], baseAsphalt[2])

            // Col 8: Right Boundary Line
            px = curr.x + rx * halfW
            py = curr.y + ry * halfW
            pz = curr.z + rz * halfW
            mb.addVertex(px, py, pz, nx, ny, nz, colRightBorder[0], colRightBorder[1], colRightBorder[2])

            // Col 9: Right Runoff Inner / Astroturf Verge
            px = curr.x + rx * (halfW + 0.50f)
            py = curr.y + ry * (halfW + 0.50f) - 0.01f
            pz = curr.z + rz * (halfW + 0.50f)
            mb.addVertex(px, py, pz, nx, ny, nz, defaultInnerRunoff[0], defaultInnerRunoff[1], defaultInnerRunoff[2])

            // Col 10: Right Runoff Outer
            px = curr.x + rx * (halfW + runoffRightW)
            py = curr.y + ry * (halfW + runoffRightW) - 0.04f
            pz = curr.z + rz * (halfW + runoffRightW)
            mb.addVertex(px, py, pz, nx, ny, nz, defaultOuterRunoff[0], defaultOuterRunoff[1], defaultOuterRunoff[2])
        }

        val cols = 11
        for (i in 0 until count) {
            val nextI = (i + 1) % count
            val base0 = (i * cols).toShort()
            val base1 = (nextI * cols).toShort()

            for (c in 0 until cols - 1) {
                val i0 = (base0 + c).toShort()
                val i1 = (base0 + c + 1).toShort()
                val i2 = (base1 + c + 1).toShort()
                val i3 = (base1 + c).toShort()
                mb.addQuad(i0, i1, i2, i3)
            }
        }

        return mb.build()
    }

    // =============================================================================================
    // 5. FIA RACING KERBS WITH 3D BEVELED PROFILE & TRACK-SPECIFIC ALTERNATING COLORS
    // =============================================================================================
    private fun generateKerbMesh(spline: List<TrackSplinePoint>, trackType: TrackType): Mesh {
        val mb = MeshBuffer()
        val redKerb = floatArrayOf(0.92f, 0.12f, 0.12f)
        val whiteKerb = floatArrayOf(0.96f, 0.96f, 0.98f)
        val yellowKerb = floatArrayOf(0.96f, 0.82f, 0.10f)
        val blackKerb = floatArrayOf(0.12f, 0.12f, 0.14f)
        val blueKerb = floatArrayOf(0.10f, 0.45f, 0.92f)

        val count = spline.size
        val kerbWidth = 1.45f
        val kerbHeight = 0.09f

        for (i in 0 until count) {
            val curr = spline[i]
            val next = spline[(i + 1) % count]

            // Color alternating scheme per track
            val color = when (trackType) {
                TrackType.GRAND_PRIX -> if ((i / 2) % 2 == 0) redKerb else whiteKerb
                TrackType.ALPINE_PASS -> if ((i / 2) % 2 == 0) yellowKerb else blackKerb
                TrackType.NEON_METROPOLIS -> if ((i / 2) % 2 == 0) blueKerb else whiteKerb
            }

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val perpX = -(tz / tLen)
            val perpZ = (tx / tLen)

            // Right Kerb
            if (curr.hasRightKerb) {
                val halfW = curr.width * 0.5f
                val p0X = curr.x + perpX * halfW
                val p0Z = curr.z + perpZ * halfW
                val p1X = curr.x + perpX * (halfW + kerbWidth * 0.45f)
                val p1Z = curr.z + perpZ * (halfW + kerbWidth * 0.45f)
                val p2X = curr.x + perpX * (halfW + kerbWidth)
                val p2Z = curr.z + perpZ * (halfW + kerbWidth)

                val np0X = next.x + perpX * (next.width * 0.5f)
                val np0Z = next.z + perpZ * (next.width * 0.5f)
                val np1X = next.x + perpX * (next.width * 0.5f + kerbWidth * 0.45f)
                val np1Z = next.z + perpZ * (next.width * 0.5f + kerbWidth * 0.45f)
                val np2X = next.x + perpX * (next.width * 0.5f + kerbWidth)
                val np2Z = next.z + perpZ * (next.width * 0.5f + kerbWidth)

                // Outer bevel
                val v0 = mb.addVertex(p0X, curr.y + 0.01f, p0Z, 0f, 1f, 0f, color[0], color[1], color[2])
                val v1 = mb.addVertex(p1X, curr.y + kerbHeight, p1Z, -perpX * 0.5f, 0.86f, -perpZ * 0.5f, color[0], color[1], color[2])
                val v2 = mb.addVertex(np1X, next.y + kerbHeight, np1Z, -perpX * 0.5f, 0.86f, -perpZ * 0.5f, color[0], color[1], color[2])
                val v3 = mb.addVertex(np0X, next.y + 0.01f, np0Z, 0f, 1f, 0f, color[0], color[1], color[2])
                mb.addQuad(v0, v1, v2, v3)

                // Inner slope
                val v4 = mb.addVertex(p2X, curr.y + 0.02f, p2Z, perpX * 0.5f, 0.86f, perpZ * 0.5f, color[0], color[1], color[2])
                val v5 = mb.addVertex(np2X, next.y + 0.02f, np2Z, perpX * 0.5f, 0.86f, perpZ * 0.5f, color[0], color[1], color[2])
                mb.addQuad(v1, v4, v5, v2)
            }

            // Left Kerb
            if (curr.hasLeftKerb) {
                val halfW = curr.width * 0.5f
                val p0X = curr.x - perpX * halfW
                val p0Z = curr.z - perpZ * halfW
                val p1X = curr.x - perpX * (halfW + kerbWidth * 0.45f)
                val p1Z = curr.z - perpZ * (halfW + kerbWidth * 0.45f)
                val p2X = curr.x - perpX * (halfW + kerbWidth)
                val p2Z = curr.z - perpZ * (halfW + kerbWidth)

                val np0X = next.x - perpX * (next.width * 0.5f)
                val np0Z = next.z - perpZ * (next.width * 0.5f)
                val np1X = next.x - perpX * (next.width * 0.5f + kerbWidth * 0.45f)
                val np1Z = next.z - perpZ * (next.width * 0.5f + kerbWidth * 0.45f)
                val np2X = next.x - perpX * (next.width * 0.5f + kerbWidth)
                val np2Z = next.z - perpZ * (next.width * 0.5f + kerbWidth)

                val v0 = mb.addVertex(p2X, curr.y + 0.02f, p2Z, -perpX * 0.5f, 0.86f, -perpZ * 0.5f, color[0], color[1], color[2])
                val v1 = mb.addVertex(p1X, curr.y + kerbHeight, p1Z, perpX * 0.5f, 0.86f, perpZ * 0.5f, color[0], color[1], color[2])
                val v2 = mb.addVertex(np1X, next.y + kerbHeight, np1Z, perpX * 0.5f, 0.86f, perpZ * 0.5f, color[0], color[1], color[2])
                val v3 = mb.addVertex(np2X, next.y + 0.02f, np2Z, -perpX * 0.5f, 0.86f, -perpZ * 0.5f, color[0], color[1], color[2])
                mb.addQuad(v0, v1, v2, v3)

                val v4 = mb.addVertex(p0X, curr.y + 0.01f, p0Z, 0f, 1f, 0f, color[0], color[1], color[2])
                val v5 = mb.addVertex(np0X, next.y + 0.01f, np0Z, 0f, 1f, 0f, color[0], color[1], color[2])
                mb.addQuad(v1, v4, v5, v2)
            }
        }

        return mb.build()
    }

    // =============================================================================================
    // 6. CRASH BARRIERS, GUARDRAILS, CATCH FENCING & TIRE WALLS
    // =============================================================================================
    private fun generateBarrierMesh(
        spline: List<TrackSplinePoint>,
        trackType: TrackType
    ): Mesh {
        val mb = MeshBuffer()
        val armcoColor = floatArrayOf(0.82f, 0.84f, 0.88f)
        val postColor = floatArrayOf(0.35f, 0.36f, 0.38f)
        val fenceColor = floatArrayOf(0.65f, 0.68f, 0.72f, 0.75f)
        val concreteWall = floatArrayOf(0.76f, 0.77f, 0.79f)
        val tireColor = floatArrayOf(0.12f, 0.12f, 0.13f)
        val tireCapColor = floatArrayOf(0.92f, 0.15f, 0.15f)
        val chevronYellow = floatArrayOf(1.0f, 0.85f, 0.10f)

        val guardrailHeight = when (trackType) {
            TrackType.ALPINE_PASS -> 1.25f
            TrackType.NEON_METROPOLIS -> 1.15f
            TrackType.GRAND_PRIX -> 1.20f
        }

        val count = spline.size
        for (i in 0 until count step 2) {
            val nextI = (i + 2) % count
            val curr = spline[i]
            val next = spline[nextI]

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val perpX = -(tz / tLen)
            val perpZ = (tx / tLen)

            val offsetRight = (curr.width * 0.5f) + if (curr.hasRunoffRight) 4.6f else 3.3f
            val offsetLeft = (curr.width * 0.5f) + if (curr.hasRunoffLeft) 4.6f else 3.3f

            val hLow = curr.y + 0.22f
            val hHigh = curr.y + guardrailHeight
            val nextHLow = next.y + 0.22f
            val nextHHigh = next.y + guardrailHeight

            // Right Barrier
            val rx0 = curr.x + perpX * offsetRight
            val rz0 = curr.z + perpZ * offsetRight
            val rx1 = next.x + perpX * offsetRight
            val rz1 = next.z + perpZ * offsetRight

            var i0 = mb.addVertex(rx0, hLow, rz0, -perpX, 0f, -perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i1 = mb.addVertex(rx1, nextHLow, rz1, -perpX, 0f, -perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i2 = mb.addVertex(rx1, nextHHigh, rz1, -perpX, 0f, -perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            var i3 = mb.addVertex(rx0, hHigh, rz0, -perpX, 0f, -perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            mb.addQuad(i0, i1, i2, i3)

            // Right vertical post
            mb.addBox(rx0 - 0.12f, curr.y, rz0 - 0.12f, rx0 + 0.12f, hHigh + 0.1f, rz0 + 0.12f, postColor[0], postColor[1], postColor[2])

            // Left Barrier
            val lx0 = curr.x - perpX * offsetLeft
            val lz0 = curr.z - perpZ * offsetLeft
            val lx1 = next.x - perpX * offsetLeft
            val lz1 = next.z - perpZ * offsetLeft

            i0 = mb.addVertex(lx0, hLow, lz0, perpX, 0f, perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i1 = mb.addVertex(lx0, hHigh, lz0, perpX, 0f, perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i2 = mb.addVertex(lx1, nextHHigh, lz1, perpX, 0f, perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            i3 = mb.addVertex(lx1, nextHLow, lz1, perpX, 0f, perpZ, armcoColor[0], armcoColor[1], armcoColor[2])
            mb.addQuad(i0, i1, i2, i3)

            // Left vertical post
            mb.addBox(lx0 - 0.12f, curr.y, lz0 - 0.12f, lx0 + 0.12f, hHigh + 0.1f, lz0 + 0.12f, postColor[0], postColor[1], postColor[2])

            // Catch fencing along spectator zones (Grand Prix)
            if (trackType == TrackType.GRAND_PRIX && (curr.z in -60f..150f || curr.z in 360f..440f)) {
                // Tall catch fence posts (+2.8m)
                mb.addBox(rx0 - 0.08f, hHigh, rz0 - 0.08f, rx0 + 0.08f, hHigh + 2.4f, rz0 + 0.08f, postColor[0], postColor[1], postColor[2])
                val f0 = mb.addVertex(rx0, hHigh, rz0, -perpX, 0f, -perpZ, fenceColor[0], fenceColor[1], fenceColor[2], 0.7f)
                val f1 = mb.addVertex(rx1, nextHHigh, rz1, -perpX, 0f, -perpZ, fenceColor[0], fenceColor[1], fenceColor[2], 0.7f)
                val f2 = mb.addVertex(rx1, nextHHigh + 2.3f, rz1, -perpX, 0f, -perpZ, fenceColor[0], fenceColor[1], fenceColor[2], 0.7f)
                val f3 = mb.addVertex(rx0, hHigh + 2.3f, rz0, -perpX, 0f, -perpZ, fenceColor[0], fenceColor[1], fenceColor[2], 0.7f)
                mb.addQuad(f0, f1, f2, f3)
            }

            // High-impact tire barriers on runoff zones (Grand Prix)
            if (trackType == TrackType.GRAND_PRIX && (curr.hasRunoffLeft || curr.hasRunoffRight)) {
                val tX = if (curr.hasRunoffLeft) lx0 - perpX * 1.1f else rx0 + perpX * 1.1f
                val tZ = if (curr.hasRunoffLeft) lz0 - perpZ * 1.1f else rz0 + perpZ * 1.1f
                val cap = if ((i / 4) % 2 == 0) tireCapColor else tireColor
                mb.addBox(tX - 0.65f, curr.y, tZ - 0.65f, tX + 0.65f, curr.y + 1.05f, tZ + 0.65f, cap[0], cap[1], cap[2])
            }

            // Urban jersey concrete barriers & chevron markers (Neon Metropolis)
            if (trackType == TrackType.NEON_METROPOLIS && (curr.hasRunoffLeft || curr.hasRunoffRight)) {
                val cX = if (curr.hasRunoffLeft) lx0 - perpX * 0.7f else rx0 + perpX * 0.7f
                val cZ = if (curr.hasRunoffLeft) lz0 - perpZ * 0.7f else rz0 + perpZ * 0.7f
                mb.addBox(cX - 0.45f, curr.y, cZ - 0.45f, cX + 0.45f, curr.y + 1.10f, cZ + 0.45f, concreteWall[0], concreteWall[1], concreteWall[2])
                if ((i / 4) % 2 == 0) {
                    mb.addBox(cX - 0.47f, curr.y + 0.35f, cZ - 0.47f, cX + 0.47f, curr.y + 0.75f, cZ + 0.47f, chevronYellow[0], chevronYellow[1], chevronYellow[2])
                }
            }
        }

        return mb.build()
    }

    // =============================================================================================
    // 7. COASTAL GRAND PRIX SCENERY (Pits, Hospitality, Grandstands, Palm Trees & Ocean Bay)
    // =============================================================================================
    private fun generateGrandPrixSceneryMesh(spline: List<TrackSplinePoint>): Mesh {
        val mb = MeshBuffer()

        // 1. Vast Coastal Bay & Ocean Horizon
        val waterColor = floatArrayOf(0.06f, 0.38f, 0.62f)
        val sandColor = floatArrayOf(0.85f, 0.80f, 0.68f)
        val grass1 = floatArrayOf(0.20f, 0.42f, 0.18f)
        val grass2 = floatArrayOf(0.18f, 0.38f, 0.16f)

        // Seaward expanse (Z from -400 to 900, X from -850 to -60)
        val seaI0 = mb.addVertex(-850f, -4.5f, -400f, 0f, 1f, 0f, waterColor[0], waterColor[1], waterColor[2])
        val seaI1 = mb.addVertex(-60f, -4.5f, -400f, 0f, 1f, 0f, waterColor[0], waterColor[1], waterColor[2])
        val seaI2 = mb.addVertex(-60f, -4.5f, 900f, 0f, 1f, 0f, waterColor[0], waterColor[1], waterColor[2])
        val seaI3 = mb.addVertex(-850f, -4.5f, 900f, 0f, 1f, 0f, waterColor[0], waterColor[1], waterColor[2])
        mb.addQuad(seaI0, seaI1, seaI2, seaI3)

        // Sandy shoreline beach transition
        val s0 = mb.addVertex(-60f, -4.5f, -400f, 0f, 1f, 0f, sandColor[0], sandColor[1], sandColor[2])
        val s1 = mb.addVertex(-35f, -0.2f, -400f, 0f, 1f, 0f, sandColor[0], sandColor[1], sandColor[2])
        val s2 = mb.addVertex(-35f, -0.2f, 900f, 0f, 1f, 0f, sandColor[0], sandColor[1], sandColor[2])
        val s3 = mb.addVertex(-60f, -4.5f, 900f, 0f, 1f, 0f, sandColor[0], sandColor[1], sandColor[2])
        mb.addQuad(s0, s1, s2, s3)

        // Coastal Infield Green Landscaping
        val g0 = mb.addVertex(-35f, -0.1f, -400f, 0f, 1f, 0f, grass1[0], grass1[1], grass1[2])
        val g1 = mb.addVertex(380f, -0.1f, -400f, 0f, 1f, 0f, grass2[0], grass2[1], grass2[2])
        val g2 = mb.addVertex(380f, -0.1f, 900f, 0f, 1f, 0f, grass1[0], grass1[1], grass1[2])
        val g3 = mb.addVertex(-35f, -0.1f, 900f, 0f, 1f, 0f, grass2[0], grass2[1], grass2[2])
        mb.addQuad(g0, g1, g2, g3)

        // Coastal Marina Breakwater & Moored Luxury Yachts
        val breakwaterStone = floatArrayOf(0.44f, 0.42f, 0.40f)
        val yachtWhite = floatArrayOf(0.95f, 0.95f, 0.98f)
        val yachtBlue = floatArrayOf(0.12f, 0.25f, 0.55f)
        mb.addBox(-160f, -4.5f, 180f, -145f, 0.8f, 420f, breakwaterStone[0], breakwaterStone[1], breakwaterStone[2])
        for (y in 0 until 4) {
            val yZ = 220f + y * 50f
            mb.addBox(-135f, -4.2f, yZ, -115f, -1.8f, yZ + 16f, yachtWhite[0], yachtWhite[1], yachtWhite[2])
            mb.addBox(-130f, -1.8f, yZ + 2f, -120f, -0.4f, yZ + 12f, yachtBlue[0], yachtBlue[1], yachtBlue[2])
            // Yacht mast
            mb.addBox(-125.2f, -0.4f, yZ + 7f, -124.8f, 7.5f, yZ + 7.4f, breakwaterStone[0], breakwaterStone[1], breakwaterStone[2])
        }

        // 2. Modern Pit Complex & Team Garages along Main Straight (Left side, X = -26m to -14m)
        val concreteWhite = floatArrayOf(0.90f, 0.90f, 0.92f)
        val pitDoorGray = floatArrayOf(0.28f, 0.32f, 0.38f)
        val glassCyan = floatArrayOf(0.15f, 0.45f, 0.65f)
        val sponsorRed = floatArrayOf(0.85f, 0.15f, 0.15f)

        // Main Pit Building (Z = -60 to 150)
        mb.addBox(-26f, 0f, -60f, -14f, 6.5f, 150f, concreteWhite[0], concreteWhite[1], concreteWhite[2])
        // VIP Hospitality Second Floor
        mb.addBox(-25.5f, 6.5f, -55f, -14.5f, 10.0f, 145f, glassCyan[0], glassCyan[1], glassCyan[2])
        // Cantilever Roof Canopy in Racing Red
        mb.addBox(-28f, 10.2f, -58f, -12f, 10.8f, 148f, sponsorRed[0], sponsorRed[1], sponsorRed[2])

        // 8 Team Pit Garage Doors
        for (g in 0 until 8) {
            val zStart = -45f + g * 23f
            mb.addBox(-13.8f, 0.1f, zStart, -13.6f, 4.4f, zStart + 19f, pitDoorGray[0], pitDoorGray[1], pitDoorGray[2])
        }

        // Race Control Timing Tower at Start Line
        mb.addBox(-28f, 0f, -8f, -18f, 15.5f, 16f, concreteWhite[0], concreteWhite[1], concreteWhite[2])
        mb.addBox(-27.5f, 11f, -7f, -17.5f, 15.0f, 15f, glassCyan[0], glassCyan[1], glassCyan[2])

        // Pit Lane Dividing Wall (separating track from pit lane)
        val pitWallGray = floatArrayOf(0.78f, 0.80f, 0.82f)
        mb.addBox(-11.5f, 0f, -60f, -10.8f, 1.15f, 150f, pitWallGray[0], pitWallGray[1], pitWallGray[2])

        // 3. Multi-Tiered Main Grandstand opposite Start Straight (Right side, X = 18m to 36m)
        val seatBlue = floatArrayOf(0.18f, 0.35f, 0.85f)
        val seatRed = floatArrayOf(0.85f, 0.18f, 0.18f)

        // Tiered seating rows (Z = -40 to 140)
        for (tier in 0 until 5) {
            val tierX0 = 18.0f + tier * 3.4f
            val tierX1 = tierX0 + 3.2f
            val tierY0 = 0.5f + tier * 1.6f
            val tierY1 = tierY0 + 1.25f
            val seatColor = if (tier % 2 == 0) seatBlue else seatRed
            mb.addBox(tierX0, 0f, -40f, tierX1, tierY1, 140f, seatColor[0], seatColor[1], seatColor[2])
        }
        // Grandstand Cantilever Shade Roof
        mb.addBox(16.5f, 10.5f, -42f, 36f, 11.2f, 142f, concreteWhite[0], concreteWhite[1], concreteWhite[2])

        // 4. Turn 1 Stadium Grandstand (X = 62 to 78, Z = 380 to 440)
        mb.addBox(62f, 0f, 380f, 78f, 8.5f, 440f, concreteWhite[0], concreteWhite[1], concreteWhite[2])
        mb.addBox(60f, 8.8f, 378f, 80f, 9.6f, 442f, sponsorRed[0], sponsorRed[1], sponsorRed[2])

        // Large Video Scoreboard Screen at Turn 1
        val screenBorder = floatArrayOf(0.18f, 0.20f, 0.24f)
        val screenEmissive = floatArrayOf(0.20f, 1.2f, 1.5f) // Glowing cyan telemetry screen
        mb.addBox(79f, 9.6f, 400f, 79.5f, 16.5f, 420f, screenBorder[0], screenBorder[1], screenBorder[2])
        mb.addBox(78.8f, 10.2f, 402f, 79.0f, 15.8f, 418f, screenEmissive[0], screenEmissive[1], screenEmissive[2])

        // 5. Coastal Palm Trees along shoreline and infield promenade
        val trunkColor = floatArrayOf(0.40f, 0.30f, 0.22f)
        val frondColor = floatArrayOf(0.18f, 0.48f, 0.16f)

        val palmLocations = listOf(
            Vector3f(-28f, 0f, -70f), Vector3f(-30f, 0f, 0f), Vector3f(-28f, 0f, 80f),
            Vector3f(-32f, 0f, 160f), Vector3f(-30f, 0f, 250f), Vector3f(-20f, 0f, 330f),
            Vector3f(150f, 3.5f, 620f), Vector3f(180f, 3.8f, 580f), Vector3f(240f, 2.5f, 520f),
            Vector3f(160f, 0.2f, -140f), Vector3f(100f, 0.3f, -130f), Vector3f(40f, 0.1f, -90f),
            Vector3f(-45f, -0.2f, -120f), Vector3f(-48f, -0.2f, 20f), Vector3f(-46f, -0.2f, 280f),
            Vector3f(-44f, -0.2f, 480f), Vector3f(75f, 2.0f, 330f), Vector3f(85f, 2.2f, 480f)
        )

        val sponsorBillboardLocations = listOf(
            Vector3f(38f, 0f, 200f), Vector3f(38f, 0f, 500f), Vector3f(-28f, 0f, 400f)
        )

        for (loc in palmLocations) {
            // Slender curved trunk
            mb.addBox(loc.x - 0.25f, loc.y, loc.z - 0.25f, loc.x + 0.25f, loc.y + 7.8f, loc.z + 0.25f, trunkColor[0], trunkColor[1], trunkColor[2])
            // Spreading palm fronds (Crossed diamond canopies)
            mb.addBox(loc.x - 3.4f, loc.y + 7.5f, loc.z - 0.7f, loc.x + 3.4f, loc.y + 8.1f, loc.z + 0.7f, frondColor[0], frondColor[1], frondColor[2])
            mb.addBox(loc.x - 0.7f, loc.y + 7.5f, loc.z - 3.4f, loc.x + 0.7f, loc.y + 8.1f, loc.z + 3.4f, frondColor[0], frondColor[1], frondColor[2])
        }

        // 6. Braking Distance Marker Boards approaching Turn 1 (Right side)
        val boardWhite = floatArrayOf(0.96f, 0.96f, 0.96f)
        val boardBlack = floatArrayOf(0.04f, 0.04f, 0.04f)
        val markerZ = listOf(260f to "50", 220f to "100", 180f to "150", 140f to "200")

        for ((z, _) in markerZ) {
            mb.addBox(8.8f, 0.5f, z - 0.1f, 9.0f, 2.3f, z + 2.0f, boardWhite[0], boardWhite[1], boardWhite[2])
            mb.addBox(8.7f, 0.8f, z + 0.2f, 8.75f, 2.0f, z + 1.7f, boardBlack[0], boardBlack[1], boardBlack[2])
        }

        // 7. Trackside Sponsor Hoardings (Apex GT, Pirelli, Castrol, Brembo)
        val sponsorCyan = floatArrayOf(0.0f, 0.85f, 0.95f)
        val sponsorYellow = floatArrayOf(0.95f, 0.82f, 0.10f)
        val sponsorRedBanner = floatArrayOf(0.90f, 0.15f, 0.18f)
        mb.addBox(9.8f, 0.2f, 140f, 10.1f, 1.9f, 185f, sponsorCyan[0], sponsorCyan[1], sponsorCyan[2])
        mb.addBox(48f, 1.8f, 370f, 56f, 3.4f, 370.4f, sponsorYellow[0], sponsorYellow[1], sponsorYellow[2])
        mb.addBox(275f, 1.2f, 220f, 275.4f, 2.8f, 260f, sponsorRedBanner[0], sponsorRedBanner[1], sponsorRedBanner[2])

        // 8. Trackside Marshall Intervention Posts
        val postWhite = floatArrayOf(0.92f, 0.92f, 0.92f)
        val postOrange = floatArrayOf(1.0f, 0.45f, 0.05f)
        val marshallLocations = listOf(
            Vector3f(9.5f, 0f, 280f),
            Vector3f(54f, 1.5f, 650f),
            Vector3f(275f, 2.0f, 400f),
            Vector3f(15f, 0f, -120f)
        )
        for (m in marshallLocations) {
            mb.addBox(m.x - 0.8f, m.y, m.z - 0.8f, m.x + 0.8f, m.y + 2.5f, m.z + 0.8f, postWhite[0], postWhite[1], postWhite[2])
            mb.addBox(m.x - 0.9f, m.y + 2.5f, m.z - 0.9f, m.x + 0.9f, m.y + 2.7f, m.z + 0.9f, postOrange[0], postOrange[1], postOrange[2])
        }

        return mb.build()
    }

    // =============================================================================================
    // 8. ALPINE MOUNTAIN PASS SCENERY (Crags, Gorge Viaduct, Pine Forests & Snow Peaks)
    // =============================================================================================
    private fun generateAlpineSceneryMesh(spline: List<TrackSplinePoint>): Mesh {
        val mb = MeshBuffer()
        val rockGray = floatArrayOf(0.42f, 0.40f, 0.38f)
        val rockDark = floatArrayOf(0.28f, 0.26f, 0.25f)
        val snowWhite = floatArrayOf(0.94f, 0.95f, 0.98f)
        val pineBark = floatArrayOf(0.32f, 0.22f, 0.16f)
        val pineNeedle = floatArrayOf(0.12f, 0.28f, 0.15f)
        val stoneBridge = floatArrayOf(0.48f, 0.46f, 0.44f)

        // 1. High Mountain Terrain Grid
        val gridSize = 16
        val extent = 680f
        val step = (extent * 2) / gridSize

        for (gz in 0..gridSize) {
            val z = -extent + gz * step
            for (gx in 0..gridSize) {
                val x = -extent + gx * step
                val dist = sqrt(x * x + z * z)
                val h = if (dist > 150f) ((dist - 150f) * 0.42f).coerceAtMost(180f) else 0f
                val color = if (h > 70f) snowWhite else if ((gx + gz) % 2 == 0) rockGray else rockDark
                mb.addVertex(x, h, z, 0f, 1f, 0f, color[0], color[1], color[2])
            }
        }

        val cols = gridSize + 1
        for (gz in 0 until gridSize) {
            for (gx in 0 until gridSize) {
                val i0 = (gz * cols + gx).toShort()
                val i1 = (gz * cols + gx + 1).toShort()
                val i2 = ((gz + 1) * cols + gx + 1).toShort()
                val i3 = ((gz + 1) * cols + gx).toShort()
                mb.addQuad(i0, i1, i2, i3)
            }
        }

        // 2. Granite Cliff Faces along tight switchback cuts
        val cliffCoords = listOf(
            Triple(110f, 15f, 195f), Triple(165f, 22f, 150f),
            Triple(145f, 28f, 20f), Triple(115f, 30f, -50f),
            Triple(85f, 24f, -215f), Triple(-35f, 14f, -170f),
            Triple(45f, 8f, 160f), Triple(130f, 26f, -120f),
            Triple(10f, 6f, 100f), Triple(-50f, 10f, -50f) // Additional rocks
        )
        for ((cx, cy, cz) in cliffCoords) {
            mb.addBox(cx - 14f, cy - 8f, cz - 14f, cx + 14f, cy + 24f, cz + 14f, rockDark[0], rockDark[1], rockDark[2])
            mb.addBox(cx - 10f, cy + 24f, cz - 10f, cx + 10f, cy + 34f, cz + 10f, snowWhite[0], snowWhite[1], snowWhite[2])
            // Extra craggy rock detail
            mb.addBox(cx - 5f, cy + 18f, cz + 5f, cx + 5f, cy + 30f, cz + 15f, rockGray[0], rockGray[1], rockGray[2])
        }

        // 3. Mountain Viaduct / Masonry Gorge Bridge (spanning ridge gap at Z = 80 to 120, X = 42 to 65)
        mb.addBox(40f, 0f, 80f, 48f, 10.5f, 125f, stoneBridge[0], stoneBridge[1], stoneBridge[2])
        mb.addBox(38f, 10.5f, 75f, 50f, 11.3f, 130f, stoneBridge[0], stoneBridge[1], stoneBridge[2])
        // Viaduct support arch piers
        mb.addBox(42f, 0f, 90f, 46f, 8.5f, 96f, stoneBridge[0], stoneBridge[1], stoneBridge[2])
        mb.addBox(42f, 0f, 110f, 46f, 8.5f, 116f, stoneBridge[0], stoneBridge[1], stoneBridge[2])

        // 4. Avalanche Gallery / Rock-Cut Tunnel Shelter (at Z = 30m to 70m along ridge)
        val tunnelConcrete = floatArrayOf(0.65f, 0.65f, 0.68f)
        mb.addBox(135f, 29.5f, 30f, 148f, 35.5f, 70f, tunnelConcrete[0], tunnelConcrete[1], tunnelConcrete[2])
        // Concrete support pillars on valley side
        for (p in 0 until 6) {
            val pZ = 32f + p * 6.5f
            mb.addBox(134.5f, 29.5f, pZ, 135.5f, 34.5f, pZ + 1.2f, tunnelConcrete[0], tunnelConcrete[1], tunnelConcrete[2])
        }

        // 5. Alpine Pine Forests (Tiered Conifers of varied heights)
        val pinePositions = listOf(
            Vector3f(15f, 2f, 40f), Vector3f(-25f, 1f, 80f), Vector3f(45f, 7f, 140f),
            Vector3f(75f, 12f, 130f), Vector3f(125f, 18f, 160f), Vector3f(175f, 22f, 100f),
            Vector3f(120f, 26f, 60f), Vector3f(80f, 28f, 10f), Vector3f(145f, 27f, -80f),
            Vector3f(105f, 22f, -160f), Vector3f(65f, 19f, -180f), Vector3f(10f, 15f, -190f),
            Vector3f(-20f, 10f, -130f), Vector3f(-55f, 4f, -70f), Vector3f(-35f, 1f, -20f),
            Vector3f(55f, 5f, 80f), Vector3f(-15f, 2f, 120f), Vector3f(160f, 24f, 170f),
            Vector3f(150f, 29f, 0f), Vector3f(-45f, 8f, -140f), Vector3f(50f, 22f, -220f)
        )

        for (pos in pinePositions) {
            // Trunk
            mb.addBox(pos.x - 0.35f, pos.y, pos.z - 0.35f, pos.x + 0.35f, pos.y + 4.8f, pos.z + 0.35f, pineBark[0], pineBark[1], pineBark[2])
            // Tier 1 (Wide bottom foliage)
            mb.addBox(pos.x - 3.0f, pos.y + 3.2f, pos.z - 3.0f, pos.x + 3.0f, pos.y + 5.5f, pos.z + 3.0f, pineNeedle[0], pineNeedle[1], pineNeedle[2])
            // Tier 2 (Middle cone)
            mb.addBox(pos.x - 2.2f, pos.y + 5.2f, pos.z - 2.2f, pos.x + 2.2f, pos.y + 7.2f, pos.z + 2.2f, pineNeedle[0], pineNeedle[1], pineNeedle[2])
            // Tier 3 (Pyramidal top with snow dust)
            val topColor = if (pos.y > 16f) snowWhite else pineNeedle
            mb.addBox(pos.x - 1.3f, pos.y + 6.8f, pos.z - 1.3f, pos.x + 1.3f, pos.y + 9.0f, pos.z + 1.3f, topColor[0], topColor[1], topColor[2])
        }

        // 6. Alpine Summit Pass Marker Sign (at Summit Hairpin)
        val timberWood = floatArrayOf(0.45f, 0.32f, 0.20f)
        val signCream = floatArrayOf(0.92f, 0.90f, 0.82f)
        mb.addBox(92f, 26f, -196f, 92.4f, 28.8f, -195.6f, timberWood[0], timberWood[1], timberWood[2])
        mb.addBox(91.2f, 27.6f, -196.8f, 92.8f, 28.8f, -194.8f, signCream[0], signCream[1], signCream[2])

        // 7. Red & White Roadside Snow Poles
        val poleRed = floatArrayOf(0.92f, 0.15f, 0.15f)
        val poleWhite = floatArrayOf(0.95f, 0.95f, 0.95f)
        val splineCount = spline.size
        for (sp in 0 until splineCount step 8) {
            val pt = spline[sp]
            val poleColor = if ((sp / 8) % 2 == 0) poleRed else poleWhite
            mb.addBox(pt.x + 7.8f, pt.y, pt.z + 7.8f, pt.x + 7.95f, pt.y + 2.4f, pt.z + 7.95f, poleColor[0], poleColor[1], poleColor[2])
        }

        return mb.build()
    }

    // =============================================================================================
    // 9. NEON METROPOLIS SCENERY (Sidewalks, Skyscraper Skyline, Neon Billboards & Skybridge)
    // =============================================================================================
    private fun generateMetropolisSceneryMesh(spline: List<TrackSplinePoint>): Mesh {
        val mb = MeshBuffer()

        // 1. Urban Concrete Sidewalks flanking the main boulevard
        val sidewalkColor = floatArrayOf(0.44f, 0.46f, 0.50f)
        val count = spline.size

        for (i in 0 until count step 2) {
            val nextI = (i + 2) % count
            val curr = spline[i]
            val next = spline[nextI]

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val perpX = -(tz / tLen)
            val perpZ = (tx / tLen)

            val innerOffset = curr.width * 0.5f + 0.25f
            val outerOffset = innerOffset + 4.0f

            // Right Sidewalk
            val rx0 = curr.x + perpX * innerOffset
            val rz0 = curr.z + perpZ * innerOffset
            val rx1 = next.x + perpX * innerOffset
            val rz1 = next.z + perpZ * innerOffset
            val rx2 = next.x + perpX * outerOffset
            val rz2 = next.z + perpZ * outerOffset
            val rx3 = curr.x + perpX * outerOffset
            val rz3 = curr.z + perpZ * outerOffset

            val v0 = mb.addVertex(rx0, 0.16f, rz0, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val v1 = mb.addVertex(rx1, 0.16f, rz1, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val v2 = mb.addVertex(rx2, 0.16f, rz2, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val v3 = mb.addVertex(rx3, 0.16f, rz3, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            mb.addQuad(v0, v1, v2, v3)

            // Left Sidewalk
            val lx0 = curr.x - perpX * innerOffset
            val lz0 = curr.z - perpZ * innerOffset
            val lx1 = next.x - perpX * innerOffset
            val lz1 = next.z - perpZ * innerOffset
            val lx2 = next.x - perpX * outerOffset
            val lz2 = next.z - perpZ * outerOffset
            val lx3 = curr.x - perpX * outerOffset
            val lz3 = curr.z - perpZ * outerOffset

            val lv0 = mb.addVertex(lx0, 0.16f, lz0, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val lv1 = mb.addVertex(lx1, 0.16f, lz1, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val lv2 = mb.addVertex(lx2, 0.16f, lz2, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            val lv3 = mb.addVertex(lx3, 0.16f, lz3, 0f, 1f, 0f, sidewalkColor[0], sidewalkColor[1], sidewalkColor[2])
            mb.addQuad(lv0, lv1, lv2, lv3)
        }

        // 2. High-Tech Arched Streetlights along avenues
        val poleColor = floatArrayOf(0.25f, 0.28f, 0.32f)
        val lampGlow = floatArrayOf(1.8f, 1.6f, 1.2f) // warm street illumination (emissive)
        for (i in 0 until count step 10) {
            val curr = spline[i]
            val next = spline[(i + 1) % count]

            val tx = next.x - curr.x
            val tz = next.z - curr.z
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val perpX = -(tz / tLen)
            val perpZ = (tx / tLen)

            val lampX = curr.x + perpX * (curr.width * 0.5f + 1.8f)
            val lampZ = curr.z + perpZ * (curr.width * 0.5f + 1.8f)
            // Vertical pole
            mb.addBox(lampX - 0.15f, 0f, lampZ - 0.15f, lampX + 0.15f, 8.0f, lampZ + 0.15f, poleColor[0], poleColor[1], poleColor[2])
            // Overhanging luminaire
            mb.addBox(lampX - 1.4f, 7.6f, lampZ - 0.25f, lampX + 0.2f, 8.1f, lampZ + 0.25f, lampGlow[0], lampGlow[1], lampGlow[2])
        }

        // 3. Diverse Skyscraper Cityscape with Stepped Architecture & Neon Crowns
        val darkFacade = floatArrayOf(0.08f, 0.10f, 0.14f)
        val glassFacade = floatArrayOf(0.12f, 0.16f, 0.24f)
        val neonCyan = floatArrayOf(0.0f, 1.8f, 2.2f) // Emissive
        val neonMagenta = floatArrayOf(2.2f, 0.2f, 1.8f) // Emissive
        val neonAmber = floatArrayOf(2.2f, 1.5f, 0.2f) // Emissive
        val windowWarmLit = floatArrayOf(1.8f, 1.5f, 0.9f) // Emissive window grid

        data class BuildingDef(val x: Float, val z: Float, val w: Float, val d: Float, val h: Float, val accent: FloatArray, val isStepped: Boolean)

        val buildings = listOf(
            BuildingDef(-65f, 60f, 45f, 50f, 115f, neonCyan, true),
            BuildingDef(-75f, 180f, 55f, 55f, 150f, neonMagenta, true),
            BuildingDef(-80f, 290f, 40f, 40f, 100f, neonAmber, false),
            BuildingDef(120f, 395f, 60f, 50f, 130f, neonCyan, true),
            BuildingDef(220f, 400f, 50f, 55f, 165f, neonMagenta, true),
            BuildingDef(315f, 385f, 45f, 45f, 110f, neonAmber, false),
            BuildingDef(395f, 260f, 50f, 50f, 135f, neonCyan, true),
            BuildingDef(410f, 150f, 55f, 50f, 120f, neonMagenta, false),
            BuildingDef(415f, 10f, 50f, 50f, 145f, neonCyan, true),
            BuildingDef(385f, -145f, 48f, 48f, 95f, neonAmber, false),
            BuildingDef(290f, -240f, 55f, 55f, 140f, neonMagenta, true),
            BuildingDef(180f, -250f, 50f, 50f, 125f, neonCyan, false),
            BuildingDef(90f, -240f, 45f, 45f, 90f, neonAmber, false),
            BuildingDef(-70f, -130f, 50f, 50f, 110f, neonMagenta, true),
            BuildingDef(-85f, -30f, 55f, 50f, 155f, neonCyan, true),
            BuildingDef(-120f, 120f, 48f, 48f, 105f, neonAmber, false),
            BuildingDef(260f, 460f, 52f, 52f, 130f, neonCyan, true),
            BuildingDef(460f, 180f, 50f, 50f, 125f, neonMagenta, false),
            // New building variants for better diversity
            BuildingDef(-140f, 250f, 65f, 40f, 160f, neonAmber, true),
            BuildingDef(0f, 450f, 70f, 30f, 110f, neonCyan, false),
            BuildingDef(500f, 0f, 45f, 65f, 180f, neonMagenta, true),
            BuildingDef(100f, -400f, 80f, 40f, 100f, neonAmber, false)
        )

        for (b in buildings) {
            if (b.isStepped) {
                // Base Tower
                mb.addBox(b.x - b.w * 0.5f, 0f, b.z - b.d * 0.5f, b.x + b.w * 0.5f, b.h * 0.65f, b.z + b.d * 0.5f, darkFacade[0], darkFacade[1], darkFacade[2])
                // Stepped Upper Tower
                val stepW = b.w * 0.75f
                val stepD = b.d * 0.75f
                mb.addBox(b.x - stepW * 0.5f, b.h * 0.65f, b.z - stepD * 0.5f, b.x + stepW * 0.5f, b.h, b.z + stepD * 0.5f, glassFacade[0], glassFacade[1], glassFacade[2])
                // Glowing Neon Rooftop Crown
                mb.addBox(b.x - stepW * 0.52f, b.h, b.z - stepD * 0.52f, b.x + stepW * 0.52f, b.h + 2.6f, b.z + stepD * 0.52f, b.accent[0], b.accent[1], b.accent[2])
                // Spire Antenna with beacon light
                mb.addBox(b.x - 0.4f, b.h + 2.6f, b.z - 0.4f, b.x + 0.4f, b.h + 18f, b.z + 0.4f, b.accent[0], b.accent[1], b.accent[2])

                // Illuminated Office Window Stripes along facade
                mb.addBox(b.x - stepW * 0.45f, b.h * 0.70f, b.z + stepD * 0.51f, b.x + stepW * 0.45f, b.h * 0.72f, b.z + stepD * 0.52f, windowWarmLit[0], windowWarmLit[1], windowWarmLit[2])
                mb.addBox(b.x - stepW * 0.45f, b.h * 0.82f, b.z + stepD * 0.51f, b.x + stepW * 0.45f, b.h * 0.84f, b.z + stepD * 0.52f, windowWarmLit[0], windowWarmLit[1], windowWarmLit[2])
            } else {
                // Monolithic Glass Tower
                mb.addBox(b.x - b.w * 0.5f, 0f, b.z - b.d * 0.5f, b.x + b.w * 0.5f, b.h, b.z + b.d * 0.5f, glassFacade[0], glassFacade[1], glassFacade[2])
                // Glowing Rooftop Bevel
                mb.addBox(b.x - b.w * 0.51f, b.h, b.z - b.d * 0.51f, b.x + b.w * 0.51f, b.h + 2.2f, b.z + b.d * 0.51f, b.accent[0], b.accent[1], b.accent[2])
            }
        }

        // 4. Glazed Pedestrian Skybridge across Downtown Boulevard (at X = 230m, Z = 340m)
        val skybridgeColor = floatArrayOf(0.18f, 0.22f, 0.28f)
        mb.addBox(225f, 9.0f, 325f, 235f, 13.8f, 355f, skybridgeColor[0], skybridgeColor[1], skybridgeColor[2])
        // Skybridge glowing horizontal neon accent
        mb.addBox(224.5f, 8.6f, 324f, 235.5f, 9.4f, 356f, neonCyan[0], neonCyan[1], neonCyan[2])

        // 5. Overhead Highway Signage Gantries
        val gantrySteel = floatArrayOf(0.35f, 0.38f, 0.42f)
        val signNeon = floatArrayOf(0.0f, 1.8f, 2.2f)
        // Gantry 1 over North Boulevard (Z = 120m)
        mb.addBox(-10f, 0f, 120f, -9f, 8.5f, 121f, gantrySteel[0], gantrySteel[1], gantrySteel[2])
        mb.addBox(9f, 0f, 120f, 10f, 8.5f, 121f, gantrySteel[0], gantrySteel[1], gantrySteel[2])
        mb.addBox(-9.5f, 6.8f, 120f, 9.5f, 8.2f, 121f, gantrySteel[0], gantrySteel[1], gantrySteel[2])
        mb.addBox(-7.0f, 5.2f, 120.2f, 7.0f, 7.0f, 120.8f, signNeon[0], signNeon[1], signNeon[2])

        // 6. Neon Advertising Billboard Towers
        val billboardLocations = listOf(
            Triple(25f, 310f, neonMagenta),
            Triple(350f, 280f, neonCyan),
            Triple(335f, -100f, neonAmber),
            Triple(160f, 360f, neonCyan)
        )
        for ((bx, bz, bColor) in billboardLocations) {
            mb.addBox(bx - 0.45f, 0f, bz - 0.45f, bx + 0.45f, 13f, bz + 0.45f, gantrySteel[0], gantrySteel[1], gantrySteel[2])
            mb.addBox(bx - 6.0f, 13f, bz - 0.5f, bx + 6.0f, 19.5f, bz + 0.5f, bColor[0], bColor[1], bColor[2])
        }

        return mb.build()
    }

    // =============================================================================================
    // 10. START / FINISH LINE GANTRY WITH F1 LED STARTING LIGHT PODS
    // =============================================================================================
    private fun generateStartGantryMesh(startPoint: TrackSplinePoint): Mesh {
        val mb = MeshBuffer()
        val frameColor = floatArrayOf(0.22f, 0.24f, 0.28f)
        val lightRed = floatArrayOf(2.5f, 0.2f, 0.2f) // Emissive
        val sponsorLivery = floatArrayOf(0.95f, 0.95f, 0.98f)

        val span = startPoint.width * 0.5f + 3.0f
        val height = 7.5f

        // Left & Right Support Pillars
        mb.addBox(-span - 0.7f, startPoint.y, startPoint.z - 0.5f, -span, startPoint.y + height, startPoint.z + 0.5f, frameColor[0], frameColor[1], frameColor[2])
        mb.addBox(span, startPoint.y, startPoint.z - 0.5f, span + 0.7f, startPoint.y + height, startPoint.z + 0.5f, frameColor[0], frameColor[1], frameColor[2])

        // Overhead Truss Bridge
        mb.addBox(-span, startPoint.y + height - 1.4f, startPoint.z - 0.6f, span, startPoint.y + height, startPoint.z + 0.6f, frameColor[0], frameColor[1], frameColor[2])
        // Overhead Livery Banner Board
        mb.addBox(-span + 1.5f, startPoint.y + height - 1.2f, startPoint.z + 0.62f, span - 1.5f, startPoint.y + height - 0.2f, startPoint.z + 0.68f, sponsorLivery[0], sponsorLivery[1], sponsorLivery[2])

        // 5 F1 LED Starting Light Pods
        for (k in -2..2) {
            val lx = k * 1.9f
            mb.addBox(lx - 0.55f, startPoint.y + height - 1.15f, startPoint.z + 0.7f, lx + 0.55f, startPoint.y + height - 0.35f, startPoint.z + 0.82f, lightRed[0], lightRed[1], lightRed[2])
        }

        return mb.build()
    }

    // =============================================================================================
    // 11. PANORAMIC SKY DOME & HORIZON GENERATOR
    // =============================================================================================
    private fun generateSkyDomeMesh(trackType: TrackType, lighting: TrackLightingConfig): Mesh {
        val mb = MeshBuffer()
        val radius = 620f
        val rings = 8
        val sectors = 16

        val zenith = lighting.skyZenithColor
        val horizon = lighting.skyHorizonColor

        for (r in 0..rings) {
            val phi = (r.toFloat() / rings) * (Math.PI.toFloat() * 0.48f) // 0 (zenith) to ~86 degrees (horizon)
            val sinPhi = sin(phi)
            val cosPhi = cos(phi)

            // Interpolate color from zenith to horizon
            val t = (r.toFloat() / rings)
            val cr = zenith[0] + (horizon[0] - zenith[0]) * t
            val cg = zenith[1] + (horizon[1] - zenith[1]) * t
            val cb = zenith[2] + (horizon[2] - zenith[2]) * t

            for (s in 0..sectors) {
                val theta = (s.toFloat() / sectors) * (Math.PI.toFloat() * 2f)
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                val x = radius * sinPhi * cosTheta
                val y = radius * cosPhi - 40f // Lower horizon slightly below track plane
                val z = radius * sinPhi * sinTheta

                // Inward normal facing camera
                mb.addVertex(x, y, z, -sinPhi * cosTheta, -cosPhi, -sinPhi * sinTheta, cr, cg, cb)
            }
        }

        val stride = sectors + 1
        for (r in 0 until rings) {
            for (s in 0 until sectors) {
                val i0 = (r * stride + s).toShort()
                val i1 = (r * stride + (s + 1)).toShort()
                val i2 = ((r + 1) * stride + (s + 1)).toShort()
                val i3 = ((r + 1) * stride + s).toShort()
                // Winding order for inward-facing dome
                mb.addQuad(i0, i3, i2, i1)
            }
        }

        return mb.build()
    }

    private fun addBrakingMarkers(baseMesh: Mesh, spline: List<TrackSplinePoint>): Mesh {
        // For simplicity in this phase, returning the base mesh until advanced marker logic is implemented.
        return baseMesh
    }
}
