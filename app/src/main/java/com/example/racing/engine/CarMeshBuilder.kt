package com.example.racing.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Procedural 3D Automotive Mesh Generator for Apex GT.
 * Synthesizes high-fidelity vehicle bodywork with authentic automotive proportions,
 * sculpted aerodynamic surfaces, functional wheel arches, curved glass greenhouses,
 * detailed cockpits, multi-element wings, and realistic wheel assemblies.
 */
object CarMeshBuilder {

    class CarModel(
        val bodyMesh: Mesh,
        val wheelMesh: Mesh,
        val shadowMesh: Mesh,
        val wheelPositions: Array<Vector3f> // FL, FR, RL, RR offsets
    ) {
        fun release() {
            bodyMesh.release()
            wheelMesh.release()
            shadowMesh.release()
        }
    }

    /**
     * Builds the appropriate 3D model for the given vehicle ID.
     */
    fun buildCarModel(carId: String): CarModel {
        return when (carId) {
            "apex_spirit_86" -> buildSpiritGTS()
            "apex_monza_v10" -> buildMonzaV10()
            else -> buildValkyrieGT()
        }
    }

    // =========================================================================
    // 1. APEX VALKYRIE GT — Mid-Engine Endurance Supercar (LM GTE / GT3 inspired)
    // =========================================================================
    fun buildValkyrieGT(): CarModel {
        val md = MeshData()

        // Color palettes
        val bodyColor = floatArrayOf(0.96f, 0.96f, 0.97f, 1.0f) // Takes player paint multiplier
        val carbonColor = floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f)
        val glossBlack = floatArrayOf(0.08f, 0.08f, 0.09f, 1.0f)
        val glassColor = floatArrayOf(0.18f, 0.24f, 0.32f, 0.85f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f) // Xenon projector
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f) // Dynamic brake LED
        val interiorDark = floatArrayOf(0.07f, 0.07f, 0.08f, 1.0f)
        val metalTrim = floatArrayOf(0.85f, 0.86f, 0.88f, 1.0f)
        val rollCageColor = floatArrayOf(0.20f, 0.20f, 0.22f, 1.0f)

        // A. Extended Carbon Front Splitter & Corner Dive Planes (Canards)
        md.addTrapezoid(
            bottomMinX = -0.98f, bottomMaxX = 0.98f, bottomY = 0.08f, bottomMinZ = 1.85f, bottomMaxZ = 2.28f,
            topMinX = -0.94f, topMaxX = 0.94f, topY = 0.13f, topMinZ = 1.90f, topMaxZ = 2.22f,
            color = carbonColor
        )
        // Aerodynamic endplate fences on splitter
        md.addBox(-0.99f, 0.08f, 2.05f, -0.96f, 0.24f, 2.28f, carbonColor)
        md.addBox(0.96f, 0.08f, 2.05f, 0.99f, 0.24f, 2.28f, carbonColor)
        // Dual carbon dive planes (canards) on front bumper sides
        md.addDivePlane(-0.96f, 0.28f, 2.00f, -0.84f, 0.32f, 2.15f, isLeft = true, color = carbonColor)
        md.addDivePlane(0.84f, 0.32f, 2.15f, 0.96f, 0.28f, 2.00f, isLeft = false, color = carbonColor)
        md.addDivePlane(-0.95f, 0.38f, 1.92f, -0.85f, 0.42f, 2.06f, isLeft = true, color = carbonColor)
        md.addDivePlane(0.85f, 0.42f, 2.06f, 0.95f, 0.38f, 1.92f, isLeft = false, color = carbonColor)

        // B. Front Bumper Fascia & Center Keel Intake Grille
        md.addBox(-0.88f, 0.13f, 1.85f, 0.88f, 0.35f, 2.16f, bodyColor)
        md.addBox(-0.52f, 0.14f, 2.12f, 0.52f, 0.30f, 2.18f, glossBlack) // Center intake recess
        md.addBox(-0.84f, 0.14f, 2.08f, -0.56f, 0.28f, 2.16f, glossBlack) // Left brake duct
        md.addBox(0.56f, 0.14f, 2.08f, 0.84f, 0.28f, 2.16f, glossBlack)  // Right brake duct

        // C. Sculpted Sloping Hood with Deep Aerodynamic Extraction Vents
        md.addCurvedSurface(
            xLeft = -0.85f, xRight = 0.85f,
            zFront = 2.15f, zBack = 1.08f,
            yFront = 0.36f, yBack = 0.62f,
            yCrown = 0.04f,
            color = bodyColor
        )
        // Dual sculpted carbon hood extraction louvers
        md.addBox(-0.54f, 0.48f, 1.30f, -0.16f, 0.54f, 1.75f, carbonColor)
        md.addBox(0.16f, 0.48f, 1.30f, 0.54f, 0.54f, 1.75f, carbonColor)
        // Louver slats
        for (k in 0 until 4) {
            val lz = 1.38f + k * 0.10f
            md.addBox(-0.52f, 0.53f, lz, -0.18f, 0.56f, lz + 0.04f, glossBlack)
            md.addBox(0.18f, 0.53f, lz, 0.52f, 0.56f, lz + 0.04f, glossBlack)
        }

        // D. Sculpted Front Wheel Fenders with Arched Cutouts (Wheel position FL/FR: z=1.35, y=0.35)
        md.addCurvedWheelArch(
            centerX = -0.84f, centerY = 0.35f, centerZ = 1.35f,
            archRadius = 0.41f, innerWidth = 0.18f, outerWidth = 0.08f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.84f, centerY = 0.35f, centerZ = 1.35f,
            archRadius = 0.41f, innerWidth = 0.18f, outerWidth = 0.08f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = false
        )

        // E. Pinched Coke-Bottle Waistline & Side Radiator Intercooler Pods
        md.addBox(-0.80f, 0.16f, -0.65f, -0.70f, 0.58f, 0.85f, bodyColor) // Pinched left door
        md.addBox(0.70f, 0.16f, -0.65f, 0.80f, 0.58f, 0.85f, bodyColor)  // Pinched right door
        // Side aerodynamic floor skirts with rear vortex flick
        md.addBox(-0.92f, 0.10f, -1.95f, -0.82f, 0.15f, 1.85f, carbonColor)
        md.addBox(0.82f, 0.10f, -1.95f, 0.92f, 0.15f, 1.85f, carbonColor)
        // Side intake pods feeding mid-engine radiators
        md.addBox(-0.88f, 0.25f, -0.75f, -0.78f, 0.62f, -0.05f, bodyColor)
        md.addBox(-0.86f, 0.30f, -0.15f, -0.80f, 0.58f, 0.05f, glossBlack) // Left intercooler intake
        md.addBox(0.78f, 0.25f, -0.75f, 0.88f, 0.62f, -0.05f, bodyColor)
        md.addBox(0.80f, 0.30f, -0.15f, 0.86f, 0.58f, 0.05f, glossBlack)  // Right intercooler intake

        // F. Muscular Rear Wheel Haunches (Wheel position RL/RR: z=-1.30, y=0.36)
        md.addCurvedWheelArch(
            centerX = -0.86f, centerY = 0.36f, centerZ = -1.30f,
            archRadius = 0.42f, innerWidth = 0.20f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.86f, centerY = 0.36f, centerZ = -1.30f,
            archRadius = 0.42f, innerWidth = 0.20f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = false
        )

        // G. Cockpit Canopy Greenhouse (Curved panoramic glass & roof)
        // Aerodynamic roof canopy
        md.addCurvedSurface(
            xLeft = -0.62f, xRight = 0.62f,
            zFront = 0.38f, zBack = -0.65f,
            yFront = 1.02f, yBack = 1.04f,
            yCrown = 0.04f,
            color = bodyColor
        )
        // Dorsal roof scoop (feeds mid-engine V8)
        md.addTrapezoid(
            bottomMinX = -0.14f, bottomMaxX = 0.14f, bottomY = 1.02f, bottomMinZ = -0.45f, bottomMaxZ = 0.25f,
            topMinX = -0.10f, topMaxX = 0.10f, topY = 1.14f, topMinZ = -0.50f, topMaxZ = 0.20f,
            color = carbonColor
        )
        md.addBox(-0.09f, 1.03f, 0.20f, 0.09f, 1.13f, 0.23f, glossBlack) // Scoop opening mouth

        // Raked Windshield
        val ws0 = md.addVertex(-0.70f, 0.62f, 1.08f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val ws1 = md.addVertex(0.70f, 0.62f, 1.08f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val ws2 = md.addVertex(0.60f, 1.02f, 0.38f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val ws3 = md.addVertex(-0.60f, 1.02f, 0.38f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(ws0, ws1, ws2, ws3)

        // Sloping Polycarbonate Rear Engine Deck Window
        val rw0 = md.addVertex(0.60f, 1.03f, -0.65f, 0f, 0.58f, -0.81f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw1 = md.addVertex(-0.60f, 1.03f, -0.65f, 0f, 0.58f, -0.81f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw2 = md.addVertex(-0.72f, 0.66f, -1.35f, 0f, 0.58f, -0.81f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw3 = md.addVertex(0.72f, 0.66f, -1.35f, 0f, 0.58f, -0.81f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(rw0, rw1, rw2, rw3)

        // Side Glass & A/B Pillars
        md.addBox(-0.70f, 0.64f, -0.65f, -0.60f, 1.01f, 0.38f, glassColor)
        md.addBox(0.60f, 0.64f, -0.65f, 0.70f, 1.01f, 0.38f, glassColor)
        // Carbon aerodynamic wing mirrors
        md.addBox(-0.84f, 0.72f, 0.40f, -0.70f, 0.78f, 0.52f, carbonColor)
        md.addBox(0.70f, 0.72f, 0.40f, 0.84f, 0.78f, 0.52f, carbonColor)

        // H. Cockpit Interior (Dashboard, Steering Wheel, Seat, Roll Cage)
        md.addBox(-0.55f, 0.58f, 0.50f, 0.55f, 0.73f, 0.88f, interiorDark) // Dash
        md.addBox(-0.24f, 0.65f, 0.55f, -0.06f, 0.70f, 0.57f, floatArrayOf(0.1f, 0.8f, 1.0f, 1.0f)) // Digital telemetry cluster
        md.addBox(-0.22f, 0.68f, 0.42f, -0.08f, 0.84f, 0.45f, carbonColor) // F1/GT steering wheel
        md.addBox(-0.35f, 0.35f, -0.28f, 0.05f, 0.88f, 0.12f, interiorDark) // Bucket seat
        // Visible roll cage main hoop and cross members
        md.addBox(-0.55f, 0.62f, -0.58f, -0.51f, 1.00f, -0.54f, rollCageColor)
        md.addBox(0.51f, 0.62f, -0.58f, 0.55f, 1.00f, -0.54f, rollCageColor)
        md.addBox(-0.55f, 0.98f, -0.58f, 0.55f, 1.02f, -0.54f, rollCageColor)

        // I. Rear Engine Decklid & Visible V8 Plenum Details
        md.addBox(-0.86f, 0.60f, -1.95f, 0.86f, 0.68f, -1.35f, bodyColor)
        // Engine intake runners (visible under rear window glass)
        for (i in 0 until 4) {
            val rz = -0.80f - i * 0.12f
            md.addBox(-0.20f, 0.65f, rz, -0.08f, 0.70f, rz + 0.06f, metalTrim)
            md.addBox(0.08f, 0.65f, rz, 0.20f, 0.70f, rz + 0.06f, metalTrim)
        }

        // J. High-Downforce Swan-Neck GT Rear Wing with Camber & Slotted Endplates
        // Curved swan-neck wing pylons
        md.addBox(-0.48f, 0.68f, -1.78f, -0.42f, 1.15f, -1.68f, carbonColor)
        md.addBox(0.42f, 0.68f, -1.78f, 0.48f, 1.15f, -1.68f, carbonColor)
        // Aerodynamic 3D curved wing blade
        md.addAirfoilWing(
            spanMinX = -0.98f, spanMaxX = 0.98f,
            chordZBack = -1.88f, chordZFront = -1.50f,
            elevY = 1.15f, camber = 0.04f,
            color = carbonColor
        )
        // Wing endplates with vertical aerodynamic vortex fences
        md.addBox(-1.00f, 1.02f, -1.92f, -0.97f, 1.28f, -1.48f, carbonColor)
        md.addBox(0.97f, 1.02f, -1.92f, 1.00f, 1.28f, -1.48f, carbonColor)

        // K. Aggressive Venturi Rear Diffuser with Multiple Strakes
        md.addTrapezoid(
            bottomMinX = -0.88f, bottomMaxX = 0.88f, bottomY = 0.10f, bottomMinZ = -2.22f, bottomMaxZ = -1.85f,
            topMinX = -0.85f, topMaxX = 0.85f, topY = 0.28f, topMinZ = -2.15f, topMaxZ = -1.90f,
            color = carbonColor
        )
        // 5 vertical diffuser fins
        for (f in -2..2) {
            val fx = f * 0.35f
            md.addBox(fx - 0.015f, 0.08f, -2.25f, fx + 0.015f, 0.26f, -1.88f, carbonColor)
        }

        // L. Automotive Headlights & LED Taillights
        // Projector Headlights
        md.addProjectorHeadlights(-0.82f, -0.54f, 0.40f, 0.50f, 2.12f, lightColor, carbonColor, isLeft = true)
        md.addProjectorHeadlights(0.54f, 0.82f, 0.40f, 0.50f, 2.12f, lightColor, carbonColor, isLeft = false)
        // Continuous Full-Width LED Taillight Bar with Dynamic Brake Illumination
        md.addBox(-0.84f, 0.54f, -2.02f, 0.84f, 0.60f, -1.96f, taillightColor)

        // M. Dual High-Exit Titanium Exhaust Tips with Heat Shield
        md.addBox(-0.25f, 0.42f, -2.03f, 0.25f, 0.52f, -1.98f, carbonColor) // Heat shield
        md.addExhaustPipe(-0.16f, 0.47f, -2.05f, 0.05f, metalTrim)
        md.addExhaustPipe(0.16f, 0.47f, -2.05f, 0.05f, metalTrim)

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(
            radius = 0.35f, width = 0.28f, rimRadius = 0.24f, spokeCount = 6, carType = "valkyrie"
        )
        val shadowMesh = createContactShadowMesh(length = 4.45f, width = 2.05f)
        val wheelPositions = arrayOf(
            Vector3f(-0.84f, 0.35f, 1.35f),
            Vector3f(0.84f, 0.35f, 1.35f),
            Vector3f(-0.86f, 0.36f, -1.30f),
            Vector3f(0.86f, 0.36f, -1.30f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    // =========================================================================
    // 2. APEX SPIRIT GTS 86 — Front-Engine Japanese Sports Coupe (Tuner / Drift)
    // =========================================================================
    fun buildSpiritGTS(): CarModel {
        val md = MeshData()

        val bodyColor = floatArrayOf(0.96f, 0.96f, 0.97f, 1.0f) // Takes player paint multiplier
        val trimBlack = floatArrayOf(0.14f, 0.14f, 0.15f, 1.0f)
        val carbonColor = floatArrayOf(0.11f, 0.11f, 0.12f, 1.0f)
        val glassColor = floatArrayOf(0.16f, 0.20f, 0.28f, 0.85f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f)
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f)
        val intercoolerAlloy = floatArrayOf(0.80f, 0.83f, 0.88f, 1.0f)
        val metalChrome = floatArrayOf(0.88f, 0.88f, 0.92f, 1.0f)
        val towHookRed = floatArrayOf(0.95f, 0.15f, 0.15f, 1.0f)

        // A. Tuner Front Bumper, Lip Splitter & Exposed Front-Mount Intercooler
        md.addTrapezoid(
            bottomMinX = -0.88f, bottomMaxX = 0.88f, bottomY = 0.10f, bottomMinZ = 1.88f, bottomMaxZ = 2.18f,
            topMinX = -0.84f, topMaxX = 0.84f, topY = 0.15f, topMinZ = 1.90f, topMaxZ = 2.14f,
            color = trimBlack
        )
        // Main bumper fascia
        md.addBox(-0.84f, 0.15f, 1.82f, 0.84f, 0.44f, 2.12f, bodyColor)
        // Large trapezoidal front intake opening exposing polished FMIC (Front Mount Intercooler)
        md.addBox(-0.52f, 0.16f, 2.06f, 0.52f, 0.36f, 2.14f, trimBlack) // Recessed intake cavity
        md.addBox(-0.46f, 0.18f, 2.08f, 0.46f, 0.34f, 2.11f, intercoolerAlloy) // Aluminum core
        // Intercooler tube fins
        for (g in 0 until 5) {
            val fy = 0.19f + g * 0.032f
            md.addBox(-0.45f, fy, 2.11f, 0.45f, fy + 0.012f, 2.12f, floatArrayOf(0.65f, 0.68f, 0.72f, 1.0f))
        }
        // Front JDM tow hook
        md.addBox(0.48f, 0.17f, 2.12f, 0.55f, 0.22f, 2.19f, towHookRed)

        // B. Long Sculpted Hood with Power Bulge & Extractor Vents
        md.addCurvedSurface(
            xLeft = -0.81f, xRight = 0.81f,
            zFront = 2.05f, zBack = 0.98f,
            yFront = 0.44f, yBack = 0.68f,
            yCrown = 0.05f,
            color = bodyColor
        )
        // Center power bulge
        md.addBox(-0.25f, 0.56f, 1.25f, 0.25f, 0.64f, 1.90f, bodyColor)
        // Dual hood heat extraction vents
        md.addBox(-0.62f, 0.58f, 1.35f, -0.38f, 0.62f, 1.65f, carbonColor)
        md.addBox(0.38f, 0.58f, 1.35f, 0.62f, 0.62f, 1.65f, carbonColor)

        // C. Bolt-on Widebody Front Fender Flares (Wheel position FL/FR: z=1.25, y=0.34)
        md.addCurvedWheelArch(
            centerX = -0.80f, centerY = 0.34f, centerZ = 1.25f,
            archRadius = 0.39f, innerWidth = 0.18f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = trimBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.80f, centerY = 0.34f, centerZ = 1.25f,
            archRadius = 0.39f, innerWidth = 0.18f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = trimBlack, isLeft = false
        )

        // D. Side Body, Aerodynamic Rocker Skirts with Winglet
        md.addBox(-0.84f, 0.26f, -0.85f, 0.84f, 0.66f, 0.85f, bodyColor)
        // Widebody aero side skirts
        md.addBox(-0.89f, 0.12f, -1.85f, -0.79f, 0.22f, 1.80f, trimBlack)
        md.addBox(0.79f, 0.12f, -1.85f, 0.89f, 0.22f, 1.80f, trimBlack)
        // Rear skirt winglet
        md.addBox(-0.90f, 0.20f, -0.85f, -0.82f, 0.36f, -0.72f, carbonColor)
        md.addBox(0.82f, 0.20f, -0.85f, 0.90f, 0.36f, -0.72f, carbonColor)

        // E. Flared Rear Widebody Quarter Fenders (Wheel position RL/RR: z=-1.25, y=0.34)
        md.addCurvedWheelArch(
            centerX = -0.84f, centerY = 0.34f, centerZ = -1.25f,
            archRadius = 0.40f, innerWidth = 0.20f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = trimBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.84f, centerY = 0.34f, centerZ = -1.25f,
            archRadius = 0.40f, innerWidth = 0.20f, outerWidth = 0.09f,
            fenderColor = bodyColor, linerColor = trimBlack, isLeft = false
        )

        // F. Fastback Coupe Greenhouse with Double-Bubble Roof & Tapered Pillars
        md.addCurvedSurface(
            xLeft = -0.62f, xRight = 0.62f,
            zFront = 0.32f, zBack = -0.58f,
            yFront = 1.06f, yBack = 1.08f,
            yCrown = 0.04f,
            color = bodyColor
        )
        // Double-bubble helmet indent (center valley)
        md.addBox(-0.10f, 1.05f, -0.55f, 0.10f, 1.08f, 0.30f, bodyColor)

        // Raked Front Windshield
        val w0 = md.addVertex(-0.67f, 0.68f, 0.98f, 0f, 0.60f, 0.80f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w1 = md.addVertex(0.67f, 0.68f, 0.98f, 0f, 0.60f, 0.80f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w2 = md.addVertex(0.59f, 1.06f, 0.32f, 0f, 0.60f, 0.80f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w3 = md.addVertex(-0.59f, 1.06f, 0.32f, 0f, 0.60f, 0.80f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(w0, w1, w2, w3)

        // Sweeping Fastback Rear Window
        val rw0 = md.addVertex(0.59f, 1.07f, -0.58f, 0f, 0.52f, -0.85f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw1 = md.addVertex(-0.59f, 1.07f, -0.58f, 0f, 0.52f, -0.85f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw2 = md.addVertex(-0.69f, 0.70f, -1.48f, 0f, 0.52f, -0.85f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw3 = md.addVertex(0.69f, 0.70f, -1.48f, 0f, 0.52f, -0.85f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(rw0, rw1, rw2, rw3)

        // Side Windows & Quarter Glass
        md.addBox(-0.67f, 0.70f, -0.58f, -0.60f, 1.04f, 0.32f, glassColor)
        md.addBox(0.60f, 0.70f, -0.58f, 0.67f, 1.04f, 0.32f, glassColor)
        // Aerodynamic bullet mirrors
        md.addBox(-0.80f, 0.76f, 0.38f, -0.68f, 0.82f, 0.48f, trimBlack)
        md.addBox(0.68f, 0.76f, 0.38f, 0.80f, 0.82f, 0.48f, trimBlack)

        // G. Drift Cockpit (Deep Dish Wheel, Tall Hydraulic E-Brake, Bucket Seats)
        md.addBox(-0.54f, 0.62f, 0.42f, 0.54f, 0.74f, 0.78f, trimBlack) // Dashboard
        md.addBox(-0.25f, 0.72f, 0.34f, -0.05f, 0.92f, 0.38f, floatArrayOf(0.92f, 0.18f, 0.18f, 1.0f)) // Deep-dish red wheel
        md.addBox(-0.16f, 0.80f, 0.33f, -0.14f, 0.92f, 0.39f, floatArrayOf(0.98f, 0.98f, 0.98f, 1.0f)) // Centering stripe
        md.addBox(0.04f, 0.52f, 0.05f, 0.08f, 0.85f, 0.10f, metalChrome) // Tall aluminum hydraulic e-brake lever
        md.addBox(-0.35f, 0.34f, -0.28f, 0.05f, 0.86f, 0.12f, trimBlack) // Driver bucket seat
        md.addBox(0.12f, 0.34f, -0.28f, 0.48f, 0.86f, 0.12f, trimBlack) // Passenger bucket seat

        // H. Integrated Molded Ducktail Trunk Spoiler
        md.addBox(-0.78f, 0.70f, -1.96f, 0.78f, 0.79f, -1.70f, bodyColor)
        md.addTrapezoid(
            bottomMinX = -0.76f, bottomMaxX = 0.76f, bottomY = 0.78f, bottomMinZ = -1.98f, bottomMaxZ = -1.82f,
            topMinX = -0.74f, topMaxX = 0.74f, topY = 0.88f, topMinZ = -2.02f, topMaxZ = -1.88f,
            color = carbonColor
        )

        // I. Headlights & Quad L-Ribbon Taillights
        md.addProjectorHeadlights(-0.80f, -0.52f, 0.44f, 0.54f, 2.04f, lightColor, trimBlack, isLeft = true)
        md.addProjectorHeadlights(0.52f, 0.80f, 0.44f, 0.54f, 2.04f, lightColor, trimBlack, isLeft = false)
        // Taillights
        md.addBox(-0.80f, 0.54f, -1.98f, -0.42f, 0.64f, -1.90f, taillightColor)
        md.addBox(0.42f, 0.54f, -1.98f, 0.80f, 0.64f, -1.90f, taillightColor)

        // J. Angled Japanese Cannon Exhaust with Burnt Titanium Gradient
        val burntTitanium = floatArrayOf(0.40f, 0.65f, 0.95f, 1.0f)
        md.addBox(0.45f, 0.16f, -2.12f, 0.65f, 0.28f, -1.92f, carbonColor) // Heat shield cutout
        md.addExhaustPipe(0.55f, 0.22f, -2.14f, 0.065f, burntTitanium)

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(
            radius = 0.34f, width = 0.26f, rimRadius = 0.23f, spokeCount = 6, carType = "spirit"
        )
        val shadowMesh = createContactShadowMesh(length = 4.25f, width = 1.95f)
        val wheelPositions = arrayOf(
            Vector3f(-0.80f, 0.34f, 1.25f),
            Vector3f(0.80f, 0.34f, 1.25f),
            Vector3f(-0.84f, 0.34f, -1.25f),
            Vector3f(0.84f, 0.34f, -1.25f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    // =========================================================================
    // 3. APEX MONZA V10 — Trackday Prototype Hypercar (Le Mans / LMP Inspired)
    // =========================================================================
    fun buildMonzaV10(): CarModel {
        val md = MeshData()

        val bodyColor = floatArrayOf(0.96f, 0.96f, 0.97f, 1.0f) // Takes player paint multiplier
        val carbonColor = floatArrayOf(0.10f, 0.10f, 0.11f, 1.0f)
        val glossBlack = floatArrayOf(0.06f, 0.06f, 0.07f, 1.0f)
        val glassColor = floatArrayOf(0.10f, 0.18f, 0.25f, 0.90f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f)
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f)
        val goldExhaust = floatArrayOf(0.95f, 0.78f, 0.28f, 1.0f) // Screaming V10 Inconel
        val metalTrim = floatArrayOf(0.82f, 0.84f, 0.88f, 1.0f)

        // A. Extended Prototype Carbon Splitter & Front Keel Wings
        md.addTrapezoid(
            bottomMinX = -1.04f, bottomMaxX = 1.04f, bottomY = 0.06f, bottomMinZ = 1.95f, bottomMaxZ = 2.40f,
            topMinX = -0.98f, topMaxX = 0.98f, topY = 0.11f, topMinZ = 2.00f, topMaxZ = 2.32f,
            color = carbonColor
        )
        // High vertical front splitter fences
        md.addBox(-1.05f, 0.06f, 2.10f, -1.02f, 0.28f, 2.38f, carbonColor)
        md.addBox(1.02f, 0.06f, 2.10f, 1.05f, 0.28f, 2.38f, carbonColor)

        // B. Separated Front Wheel Aerodynamic Pontoons with Pressure-Relief Louvers
        // Wheel position FL/FR: z=1.45, y=0.36
        md.addCurvedWheelArch(
            centerX = -0.86f, centerY = 0.36f, centerZ = 1.45f,
            archRadius = 0.42f, innerWidth = 0.20f, outerWidth = 0.10f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.86f, centerY = 0.36f, centerZ = 1.45f,
            archRadius = 0.42f, innerWidth = 0.20f, outerWidth = 0.10f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = false
        )
        // Top wheel arch pressure louvers (prevents aerodynamic front lift)
        for (i in 0 until 4) {
            val lz = 1.35f + i * 0.07f
            md.addBox(-0.96f, 0.58f, lz, -0.74f, 0.60f, lz + 0.035f, carbonColor)
            md.addBox(0.74f, 0.58f, lz, 0.96f, 0.60f, lz + 0.035f, carbonColor)
        }

        // C. Center Aerodynamic Drop Nose & Air Extraction Channel
        md.addTrapezoid(
            bottomMinX = -0.38f, bottomMaxX = 0.38f, bottomY = 0.18f, bottomMinZ = 1.15f, bottomMaxZ = 2.30f,
            topMinX = -0.32f, topMaxX = 0.32f, topY = 0.44f, topMinZ = 1.10f, topMaxZ = 2.22f,
            color = bodyColor
        )
        // Center air keel divider
        md.addBox(-0.02f, 0.10f, 1.70f, 0.02f, 0.32f, 2.25f, carbonColor)

        // D. Narrow Teardrop Fighter-Jet Cockpit Canopy
        md.addCurvedSurface(
            xLeft = -0.46f, xRight = 0.46f,
            zFront = 0.30f, zBack = -0.58f,
            yFront = 0.90f, yBack = 0.94f,
            yCrown = 0.04f,
            color = bodyColor
        )
        // Roof ram-air intake snorkel for high-revving V10 engine
        md.addTrapezoid(
            bottomMinX = -0.15f, bottomMaxX = 0.15f, bottomY = 0.92f, bottomMinZ = -0.45f, bottomMaxZ = 0.25f,
            topMinX = -0.11f, topMaxX = 0.11f, topY = 1.08f, topMinZ = -0.52f, topMaxZ = 0.20f,
            color = carbonColor
        )
        md.addBox(-0.10f, 0.94f, 0.20f, 0.10f, 1.06f, 0.23f, glossBlack) // Snorkel intake mouth

        // Panoramic Wrap-Around Cockpit Windshield
        val w0 = md.addVertex(-0.52f, 0.48f, 0.88f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w1 = md.addVertex(0.52f, 0.48f, 0.88f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w2 = md.addVertex(0.44f, 0.92f, 0.30f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w3 = md.addVertex(-0.44f, 0.92f, 0.30f, 0f, 0.55f, 0.83f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(w0, w1, w2, w3)

        // Side Bubble Glass
        md.addBox(-0.50f, 0.50f, -0.55f, -0.42f, 0.90f, 0.30f, glassColor)
        md.addBox(0.42f, 0.50f, -0.55f, 0.50f, 0.90f, 0.30f, glassColor)

        // E. Full-Length LMP Carbon Shark Fin Stabilizer (extends from roof intake to wing)
        md.addBox(-0.02f, 0.62f, -1.95f, 0.02f, 1.18f, -0.45f, carbonColor)

        // F. Deep Venturi Side Pods & Hollowed Flow-Through Doors
        md.addBox(-0.94f, 0.18f, -1.05f, -0.56f, 0.54f, 0.65f, bodyColor)
        md.addBox(0.56f, 0.18f, -1.05f, 0.94f, 0.54f, 0.65f, bodyColor)
        // Exposed carbon monocoque side floor skirts
        md.addBox(-1.02f, 0.08f, -2.15f, -0.92f, 0.14f, 1.95f, carbonColor)
        md.addBox(0.92f, 0.08f, -2.15f, 1.02f, 0.14f, 1.95f, carbonColor)

        // G. Rear Wheel Aero Fairings (Wheel position RL/RR: z=-1.40, y=0.36)
        md.addCurvedWheelArch(
            centerX = -0.88f, centerY = 0.36f, centerZ = -1.40f,
            archRadius = 0.43f, innerWidth = 0.22f, outerWidth = 0.10f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = true
        )
        md.addCurvedWheelArch(
            centerX = 0.88f, centerY = 0.36f, centerZ = -1.40f,
            archRadius = 0.43f, innerWidth = 0.22f, outerWidth = 0.10f,
            fenderColor = bodyColor, linerColor = glossBlack, isLeft = false
        )

        // H. Extreme Le Mans Carbon Dual-Element Rear Wing with Swan-Neck Pylons
        // Swan-neck struts
        md.addBox(-0.35f, 0.65f, -2.08f, -0.28f, 1.28f, -1.98f, carbonColor)
        md.addBox(0.28f, 0.65f, -2.08f, 0.35f, 1.28f, -1.98f, carbonColor)
        // Primary mainplane wing
        md.addAirfoilWing(
            spanMinX = -1.10f, spanMaxX = 1.10f,
            chordZBack = -2.25f, chordZFront = -1.82f,
            elevY = 1.25f, camber = 0.05f,
            color = carbonColor
        )
        // Secondary high-angle flap
        md.addAirfoilWing(
            spanMinX = -1.06f, spanMaxX = 1.06f,
            chordZBack = -2.32f, chordZFront = -2.05f,
            elevY = 1.32f, camber = 0.03f,
            color = carbonColor
        )
        // Vertical endplates with night-racing LED edge light strips
        md.addBox(-1.12f, 1.04f, -2.32f, -1.08f, 1.42f, -1.78f, carbonColor)
        md.addBox(1.08f, 1.04f, -2.32f, 1.12f, 1.42f, -1.78f, carbonColor)
        md.addBox(-1.11f, 1.08f, -2.30f, -1.09f, 1.38f, -2.27f, taillightColor) // Left LED strip
        md.addBox(1.09f, 1.08f, -2.30f, 1.11f, 1.38f, -2.27f, taillightColor)  // Right LED strip

        // I. Massive Underfloor Venturi Tunnels with 6 Vertical Strakes
        md.addTrapezoid(
            bottomMinX = -0.92f, bottomMaxX = 0.92f, bottomY = 0.07f, bottomMinZ = -2.38f, bottomMaxZ = -1.90f,
            topMinX = -0.88f, topMaxX = 0.88f, topY = 0.32f, topMinZ = -2.28f, topMaxZ = -1.95f,
            color = carbonColor
        )
        for (st in -3..2) {
            val sx = st * 0.28f + 0.14f
            md.addBox(sx - 0.015f, 0.06f, -2.40f, sx + 0.015f, 0.30f, -1.92f, carbonColor)
        }

        // J. Top-Exit Upward-Firing Screaming V10 Inconel Exhausts with Gold Heat Shield
        md.addBox(-0.24f, 0.60f, -1.45f, 0.24f, 0.65f, -1.15f, goldExhaust) // Gold heat shield
        md.addExhaustPipe(-0.10f, 0.68f, -1.30f, 0.055f, goldExhaust)
        md.addExhaustPipe(0.10f, 0.68f, -1.30f, 0.055f, goldExhaust)

        // K. Stacked Multi-LED Projector Headlights
        md.addBox(-0.95f, 0.32f, 2.12f, -0.82f, 0.52f, 2.22f, carbonColor) // Housing Left
        md.addBox(-0.92f, 0.35f, 2.18f, -0.85f, 0.42f, 2.24f, lightColor)
        md.addBox(-0.92f, 0.44f, 2.15f, -0.85f, 0.50f, 2.21f, lightColor)

        md.addBox(0.82f, 0.32f, 2.12f, 0.95f, 0.52f, 2.22f, carbonColor) // Housing Right
        md.addBox(0.85f, 0.35f, 2.18f, 0.92f, 0.42f, 2.24f, lightColor)
        md.addBox(0.85f, 0.44f, 2.15f, 0.92f, 0.50f, 2.21f, lightColor)

        // L. Interior Details (Carbon Tub, F1 Yoke, Display)
        md.addBox(-0.35f, 0.45f, 0.35f, 0.35f, 0.60f, 0.65f, glossBlack)
        md.addBox(-0.16f, 0.58f, 0.30f, 0.04f, 0.72f, 0.33f, carbonColor) // F1 telemetry yoke
        md.addBox(-0.25f, 0.28f, -0.25f, 0.05f, 0.80f, 0.15f, carbonColor) // Carbon shell seat

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(
            radius = 0.36f, width = 0.32f, rimRadius = 0.25f, spokeCount = 8, carType = "monza"
        )
        val shadowMesh = createContactShadowMesh(length = 4.80f, width = 2.20f)
        val wheelPositions = arrayOf(
            Vector3f(-0.86f, 0.36f, 1.45f),
            Vector3f(0.86f, 0.36f, 1.45f),
            Vector3f(-0.88f, 0.36f, -1.40f),
            Vector3f(0.88f, 0.36f, -1.40f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    // =========================================================================
    // DETAILED WHEEL MESH GENERATOR (Authentic Tires, Rims, Discs, Calipers)
    // =========================================================================
    private fun createDetailedWheelMesh(
        radius: Float,
        width: Float,
        rimRadius: Float,
        spokeCount: Int,
        carType: String
    ): Mesh {
        val md = MeshData()
        val segments = 20

        val tireColor = floatArrayOf(0.14f, 0.14f, 0.15f, 1.0f)
        val rimLipColor = floatArrayOf(0.92f, 0.92f, 0.96f, 1.0f)
        val rimSpokeColor = when (carType) {
            "spirit" -> floatArrayOf(0.82f, 0.72f, 0.45f, 1.0f) // Bronze JDM alloy
            "monza" -> floatArrayOf(0.18f, 0.18f, 0.20f, 1.0f)  // Matte carbon race wheel
            else -> floatArrayOf(0.88f, 0.88f, 0.92f, 1.0f)     // Forged silver GT wheel
        }
        val rotorColor = floatArrayOf(0.72f, 0.74f, 0.78f, 1.0f) // Steel / carbon-ceramic rotor
        val rotorHatColor = floatArrayOf(0.20f, 0.20f, 0.22f, 1.0f) // Center rotor mounting hat
        val caliperColor = when (carType) {
            "spirit" -> floatArrayOf(0.95f, 0.78f, 0.12f, 1.0f) // Yellow Brembo
            "monza" -> floatArrayOf(0.20f, 0.90f, 0.35f, 1.0f)  // Acid Green racing caliper
            else -> floatArrayOf(0.95f, 0.12f, 0.12f, 1.0f)     // Brembo Racing Red
        }

        val halfW = width * 0.5f
        val shoulderW = halfW * 0.82f // Beveled tire shoulder width
        val shoulderR = radius * 0.97f // Rounded tire shoulder radius
        val angleStep = (2.0 * PI / segments).toFloat()

        // 1. Full Cylindrical Tread with Chamfered/Rounded Shoulders
        for (s in 0 until segments) {
            val a0 = s * angleStep
            val a1 = (s + 1) * angleStep
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val y0 = s0 * radius; val z0 = c0 * radius
            val y1 = s1 * radius; val z1 = c1 * radius

            // Center tread surface
            val t0 = md.addVertex(-shoulderW, y0, z0, 0f, s0, c0, tireColor[0], tireColor[1], tireColor[2])
            val t1 = md.addVertex(shoulderW, y0, z0, 0f, s0, c0, tireColor[0], tireColor[1], tireColor[2])
            val t2 = md.addVertex(shoulderW, y1, z1, 0f, s1, c1, tireColor[0], tireColor[1], tireColor[2])
            val t3 = md.addVertex(-shoulderW, y1, z1, 0f, s1, c1, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(t0, t1, t2, t3)

            // Outer shoulder bevel
            val os0 = md.addVertex(shoulderW, y0, z0, 0.5f, s0 * 0.7f, c0 * 0.7f, tireColor[0], tireColor[1], tireColor[2])
            val os1 = md.addVertex(halfW, s0 * shoulderR, c0 * shoulderR, 0.7f, s0 * 0.5f, c0 * 0.5f, tireColor[0], tireColor[1], tireColor[2])
            val os2 = md.addVertex(halfW, s1 * shoulderR, c1 * shoulderR, 0.7f, s1 * 0.5f, c1 * 0.5f, tireColor[0], tireColor[1], tireColor[2])
            val os3 = md.addVertex(shoulderW, y1, z1, 0.5f, s1 * 0.7f, c1 * 0.7f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(os0, os1, os2, os3)

            // Inner shoulder bevel
            val is0 = md.addVertex(-halfW, s0 * shoulderR, c0 * shoulderR, -0.7f, s0 * 0.5f, c0 * 0.5f, tireColor[0], tireColor[1], tireColor[2])
            val is1 = md.addVertex(-shoulderW, y0, z0, -0.5f, s0 * 0.7f, c0 * 0.7f, tireColor[0], tireColor[1], tireColor[2])
            val is2 = md.addVertex(-shoulderW, y1, z1, -0.5f, s1 * 0.7f, c1 * 0.7f, tireColor[0], tireColor[1], tireColor[2])
            val is3 = md.addVertex(-halfW, s1 * shoulderR, c1 * shoulderR, -0.7f, s1 * 0.5f, c1 * 0.5f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(is0, is1, is2, is3)

            // Outer Sidewall (curves inward from shoulder to rim flange)
            val ry0 = s0 * rimRadius; val rz0 = c0 * rimRadius
            val ry1 = s1 * rimRadius; val rz1 = c1 * rimRadius

            val sw0 = md.addVertex(halfW, s0 * shoulderR, c0 * shoulderR, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw1 = md.addVertex(halfW * 0.95f, ry0, rz0, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw2 = md.addVertex(halfW * 0.95f, ry1, rz1, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw3 = md.addVertex(halfW, s1 * shoulderR, c1 * shoulderR, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(sw0, sw1, sw2, sw3)

            // Inner Sidewall
            val isw0 = md.addVertex(-halfW * 0.95f, ry0, rz0, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw1 = md.addVertex(-halfW, s0 * shoulderR, c0 * shoulderR, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw2 = md.addVertex(-halfW, s1 * shoulderR, c1 * shoulderR, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw3 = md.addVertex(-halfW * 0.95f, ry1, rz1, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(isw0, isw1, isw2, isw3)

            // Outer Stepped Rim Barrel & Polished Lip
            val rimLipX = halfW * 0.92f
            val rimFaceX = halfW * 0.70f
            val rlip0 = md.addVertex(rimLipX, ry0, rz0, 0f, -s0, -c0, rimLipColor[0], rimLipColor[1], rimLipColor[2])
            val rlip1 = md.addVertex(rimFaceX, ry0 * 0.95f, rz0 * 0.95f, 0f, -s0, -c0, rimLipColor[0], rimLipColor[1], rimLipColor[2])
            val rlip2 = md.addVertex(rimFaceX, ry1 * 0.95f, rz1 * 0.95f, 0f, -s1, -c1, rimLipColor[0], rimLipColor[1], rimLipColor[2])
            val rlip3 = md.addVertex(rimLipX, ry1, rz1, 0f, -s1, -c1, rimLipColor[0], rimLipColor[1], rimLipColor[2])
            md.addQuad(rlip0, rlip1, rlip2, rlip3)
        }

        // 2. Sculpted Alloy Wheel Spokes & Central Wheel Hub
        val hubRadius = rimRadius * 0.28f
        val rimFaceX = halfW * 0.72f
        val hubX = halfW * 0.65f

        // Center hub cap with centerlock nut
        for (s in 0 until segments) {
            val a0 = s * angleStep
            val a1 = (s + 1) * angleStep
            val hy0 = sin(a0) * hubRadius; val hz0 = cos(a0) * hubRadius
            val hy1 = sin(a1) * hubRadius; val hz1 = cos(a1) * hubRadius

            val h0 = md.addVertex(hubX, 0f, 0f, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            val h1 = md.addVertex(hubX, hy0, hz0, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            val h2 = md.addVertex(hubX, hy1, hz1, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            md.addTriangle(h0, h1, h2)
        }

        // Distinct Spoke Patterns based on Vehicle Personality
        val spokeAngleStep = (2.0 * PI / spokeCount).toFloat()
        val spokeWidthRad = 0.12f

        for (sp in 0 until spokeCount) {
            val centerA = sp * spokeAngleStep
            val aLeft = centerA - spokeWidthRad
            val aRight = centerA + spokeWidthRad

            val sHubY0 = sin(aLeft) * hubRadius; val sHubZ0 = cos(aLeft) * hubRadius
            val sHubY1 = sin(aRight) * hubRadius; val sHubZ1 = cos(aRight) * hubRadius

            val sRimY0 = sin(aLeft) * (rimRadius * 0.92f); val sRimZ0 = cos(aLeft) * (rimRadius * 0.92f)
            val sRimY1 = sin(aRight) * (rimRadius * 0.92f); val sRimZ1 = cos(aRight) * (rimRadius * 0.92f)

            // Front spoke quad
            val sp0 = md.addVertex(hubX, sHubY0, sHubZ0, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            val sp1 = md.addVertex(hubX, sHubY1, sHubZ1, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            val sp2 = md.addVertex(rimFaceX, sRimY1, sRimZ1, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            val sp3 = md.addVertex(rimFaceX, sRimY0, sRimZ0, 1f, 0f, 0f, rimSpokeColor[0], rimSpokeColor[1], rimSpokeColor[2])
            md.addQuad(sp0, sp1, sp2, sp3)

            // Valkyrie: Twin-spoke secondary split strut
            if (carType == "valkyrie") {
                val twinOffset = 0.08f
                val tRimY0 = sin(centerA + twinOffset) * (rimRadius * 0.88f)
                val tRimZ0 = cos(centerA + twinOffset) * (rimRadius * 0.88f)
                val tsp0 = md.addVertex(hubX, sHubY1, sHubZ1, 1f, 0f, 0f, rimLipColor[0], rimLipColor[1], rimLipColor[2])
                val tsp1 = md.addVertex(rimFaceX, tRimY0, tRimZ0, 1f, 0f, 0f, rimLipColor[0], rimLipColor[1], rimLipColor[2])
                val tsp2 = md.addVertex(rimFaceX, sRimY1, sRimZ1, 1f, 0f, 0f, rimLipColor[0], rimLipColor[1], rimLipColor[2])
                md.addTriangle(tsp0, tsp1, tsp2)
            }
        }

        // Monza V10: Aerodynamic Carbon Turbofan Outer Ring
        if (carType == "monza") {
            val fanInnerR = rimRadius * 0.72f
            val fanOuterR = rimRadius * 0.94f
            val carbonTurbofan = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f)
            for (s in 0 until segments) {
                val a0 = s * angleStep; val a1 = (s + 1) * angleStep
                val tf0 = md.addVertex(rimFaceX * 1.05f, sin(a0) * fanInnerR, cos(a0) * fanInnerR, 1f, 0f, 0f, carbonTurbofan[0], carbonTurbofan[1], carbonTurbofan[2])
                val tf1 = md.addVertex(rimFaceX * 1.05f, sin(a0) * fanOuterR, cos(a0) * fanOuterR, 1f, 0f, 0f, carbonTurbofan[0], carbonTurbofan[1], carbonTurbofan[2])
                val tf2 = md.addVertex(rimFaceX * 1.05f, sin(a1) * fanOuterR, cos(a1) * fanOuterR, 1f, 0f, 0f, carbonTurbofan[0], carbonTurbofan[1], carbonTurbofan[2])
                val tf3 = md.addVertex(rimFaceX * 1.05f, sin(a1) * fanInnerR, cos(a1) * fanInnerR, 1f, 0f, 0f, carbonTurbofan[0], carbonTurbofan[1], carbonTurbofan[2])
                md.addQuad(tf0, tf1, tf2, tf3)
            }
        }

        // 3. Ventilated and Cross-Drilled Brake Rotor with Center Mounting Hat
        val rotorRadius = rimRadius * 0.78f
        val discX = halfW * 0.15f
        for (s in 0 until segments) {
            val a0 = s * angleStep; val a1 = (s + 1) * angleStep
            val dy0 = sin(a0) * rotorRadius; val dz0 = cos(a0) * rotorRadius
            val dy1 = sin(a1) * rotorRadius; val dz1 = cos(a1) * rotorRadius

            val hy0 = sin(a0) * hubRadius * 1.15f; val hz0 = cos(a0) * hubRadius * 1.15f
            val hy1 = sin(a1) * hubRadius * 1.15f; val hz1 = cos(a1) * hubRadius * 1.15f

            // Inner dark rotor hat
            val rh0 = md.addVertex(discX, 0f, 0f, 1f, 0f, 0f, rotorHatColor[0], rotorHatColor[1], rotorHatColor[2])
            val rh1 = md.addVertex(discX, hy0, hz0, 1f, 0f, 0f, rotorHatColor[0], rotorHatColor[1], rotorHatColor[2])
            val rh2 = md.addVertex(discX, hy1, hz1, 1f, 0f, 0f, rotorHatColor[0], rotorHatColor[1], rotorHatColor[2])
            md.addTriangle(rh0, rh1, rh2)

            // Outer friction rotor ring
            val rf0 = md.addVertex(discX, hy0, hz0, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            val rf1 = md.addVertex(discX, dy0, dz0, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            val rf2 = md.addVertex(discX, dy1, dz1, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            val rf3 = md.addVertex(discX, hy1, hz1, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            md.addQuad(rf0, rf1, rf2, rf3)
        }

        // 4. Large Multi-Piston Racing Brake Caliper
        val calMinY = rotorRadius * 0.45f
        val calMaxY = rotorRadius * 1.05f
        val calMinZ = rotorRadius * 0.50f
        val calMaxZ = rotorRadius * 1.02f
        md.addBox(discX - 0.04f, calMinY, calMinZ, discX + 0.08f, calMaxY, calMaxZ, caliperColor)
        // Caliper bridge mounting bolts
        md.addBox(discX + 0.08f, calMinY + 0.02f, calMinZ + 0.04f, discX + 0.095f, calMinY + 0.06f, calMinZ + 0.08f, rimLipColor)
        md.addBox(discX + 0.08f, calMaxY - 0.06f, calMaxZ - 0.08f, discX + 0.095f, calMaxY - 0.02f, calMaxZ - 0.04f, rimLipColor)

        return md.toMesh()
    }

    // =========================================================================
    // CONTACT SHADOW MESH GENERATOR (Realistic Curved Multi-Ring Grounding Quad)
    // =========================================================================
    private fun createContactShadowMesh(length: Float, width: Float): Mesh {
        val md = MeshData()
        val segments = 16
        val coreColor = floatArrayOf(0.02f, 0.02f, 0.03f, 0.72f)
        val midColor = floatArrayOf(0.02f, 0.02f, 0.03f, 0.35f)
        val fadeColor = floatArrayOf(0.02f, 0.02f, 0.03f, 0.0f)

        val halfL = length * 0.5f
        val halfW = width * 0.5f
        val angleStep = (2.0 * PI / segments).toFloat()

        // Multi-ring rounded oval contact shadow
        for (s in 0 until segments) {
            val a0 = s * angleStep
            val a1 = (s + 1) * angleStep
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            // Inner core ring
            val cX0 = s0 * (halfW * 0.58f); val cZ0 = c0 * (halfL * 0.65f)
            val cX1 = s1 * (halfW * 0.58f); val cZ1 = c1 * (halfL * 0.65f)
            val i0 = md.addVertex(0f, 0.02f, 0f, 0f, 1f, 0f, coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            val i1 = md.addVertex(cX0, 0.02f, cZ0, 0f, 1f, 0f, coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            val i2 = md.addVertex(cX1, 0.02f, cZ1, 0f, 1f, 0f, coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            md.addTriangle(i0, i1, i2)

            // Mid ring
            val mX0 = s0 * (halfW * 0.82f); val mZ0 = c0 * (halfL * 0.86f)
            val mX1 = s1 * (halfW * 0.82f); val mZ1 = c1 * (halfL * 0.86f)
            val q0 = md.addVertex(cX0, 0.02f, cZ0, 0f, 1f, 0f, coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            val q1 = md.addVertex(mX0, 0.02f, mZ0, 0f, 1f, 0f, midColor[0], midColor[1], midColor[2], midColor[3])
            val q2 = md.addVertex(mX1, 0.02f, mZ1, 0f, 1f, 0f, midColor[0], midColor[1], midColor[2], midColor[3])
            val q3 = md.addVertex(cX1, 0.02f, cZ1, 0f, 1f, 0f, coreColor[0], coreColor[1], coreColor[2], coreColor[3])
            md.addQuad(q0, q1, q2, q3)

            // Outer soft ambient occlusion falloff fringe
            val oX0 = s0 * halfW; val oZ0 = c0 * halfL
            val oX1 = s1 * halfW; val oZ1 = c1 * halfL
            val f0 = md.addVertex(mX0, 0.02f, mZ0, 0f, 1f, 0f, midColor[0], midColor[1], midColor[2], midColor[3])
            val f1 = md.addVertex(oX0, 0.02f, oZ0, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])
            val f2 = md.addVertex(oX1, 0.02f, oZ1, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])
            val f3 = md.addVertex(mX1, 0.02f, mZ1, 0f, 1f, 0f, midColor[0], midColor[1], midColor[2], midColor[3])
            md.addQuad(f0, f1, f2, f3)
        }

        return md.toMesh()
    }

    // =========================================================================
    // GEOMETRIC MESH BUILDER PRIMITIVES & SUBDIVISION OPERATORS
    // =========================================================================
    private class MeshData {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Short>()

        fun addVertex(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            r: Float, g: Float, b: Float, a: Float = 1.0f
        ): Short {
            val idx = (vertices.size / 10).toShort()
            vertices.add(x); vertices.add(y); vertices.add(z)
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 0.0001f) {
                vertices.add(nx / len); vertices.add(ny / len); vertices.add(nz / len)
            } else {
                vertices.add(0f); vertices.add(1f); vertices.add(0f)
            }
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

        fun addBox(
            minX: Float, minY: Float, minZ: Float,
            maxX: Float, maxY: Float, maxZ: Float,
            color: FloatArray
        ) {
            // Front (+Z)
            var i0 = addVertex(minX, minY, maxZ, 0f, 0f, 1f, color[0], color[1], color[2], color[3])
            var i1 = addVertex(maxX, minY, maxZ, 0f, 0f, 1f, color[0], color[1], color[2], color[3])
            var i2 = addVertex(maxX, maxY, maxZ, 0f, 0f, 1f, color[0], color[1], color[2], color[3])
            var i3 = addVertex(minX, maxY, maxZ, 0f, 0f, 1f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)

            // Back (-Z)
            i0 = addVertex(maxX, minY, minZ, 0f, 0f, -1f, color[0], color[1], color[2], color[3])
            i1 = addVertex(minX, minY, minZ, 0f, 0f, -1f, color[0], color[1], color[2], color[3])
            i2 = addVertex(minX, maxY, minZ, 0f, 0f, -1f, color[0], color[1], color[2], color[3])
            i3 = addVertex(maxX, maxY, minZ, 0f, 0f, -1f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)

            // Top (+Y)
            i0 = addVertex(minX, maxY, maxZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            i1 = addVertex(maxX, maxY, maxZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            i2 = addVertex(maxX, maxY, minZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            i3 = addVertex(minX, maxY, minZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)

            // Bottom (-Y)
            i0 = addVertex(minX, minY, minZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            i1 = addVertex(maxX, minY, minZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            i2 = addVertex(maxX, minY, maxZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            i3 = addVertex(minX, minY, maxZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)

            // Left (-X)
            i0 = addVertex(minX, minY, minZ, -1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i1 = addVertex(minX, minY, maxZ, -1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i2 = addVertex(minX, maxY, maxZ, -1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i3 = addVertex(minX, maxY, minZ, -1f, 0f, 0f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)

            // Right (+X)
            i0 = addVertex(maxX, minY, maxZ, 1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i1 = addVertex(maxX, minY, minZ, 1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i2 = addVertex(maxX, maxY, minZ, 1f, 0f, 0f, color[0], color[1], color[2], color[3])
            i3 = addVertex(maxX, maxY, maxZ, 1f, 0f, 0f, color[0], color[1], color[2], color[3])
            addQuad(i0, i1, i2, i3)
        }

        fun addTrapezoid(
            bottomMinX: Float, bottomMaxX: Float, bottomY: Float, bottomMinZ: Float, bottomMaxZ: Float,
            topMinX: Float, topMaxX: Float, topY: Float, topMinZ: Float, topMaxZ: Float,
            color: FloatArray
        ) {
            // Bottom 4 vertices
            val b0 = addVertex(bottomMinX, bottomY, bottomMaxZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            val b1 = addVertex(bottomMaxX, bottomY, bottomMaxZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            val b2 = addVertex(bottomMaxX, bottomY, bottomMinZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            val b3 = addVertex(bottomMinX, bottomY, bottomMinZ, 0f, -1f, 0f, color[0], color[1], color[2], color[3])
            addQuad(b0, b1, b2, b3)

            // Top 4 vertices
            val t0 = addVertex(topMinX, topY, topMaxZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            val t1 = addVertex(topMaxX, topY, topMaxZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            val t2 = addVertex(topMaxX, topY, topMinZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            val t3 = addVertex(topMinX, topY, topMinZ, 0f, 1f, 0f, color[0], color[1], color[2], color[3])
            addQuad(t0, t1, t2, t3)

            // Front face (+Z)
            val f0 = addVertex(bottomMinX, bottomY, bottomMaxZ, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            val f1 = addVertex(bottomMaxX, bottomY, bottomMaxZ, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            val f2 = addVertex(topMaxX, topY, topMaxZ, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            val f3 = addVertex(topMinX, topY, topMaxZ, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            addQuad(f0, f1, f2, f3)

            // Back face (-Z)
            val k0 = addVertex(bottomMaxX, bottomY, bottomMinZ, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            val k1 = addVertex(bottomMinX, bottomY, bottomMinZ, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            val k2 = addVertex(topMinX, topY, topMinZ, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            val k3 = addVertex(topMaxX, topY, topMinZ, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            addQuad(k0, k1, k2, k3)

            // Left face (-X)
            val l0 = addVertex(bottomMinX, bottomY, bottomMinZ, -0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val l1 = addVertex(bottomMinX, bottomY, bottomMaxZ, -0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val l2 = addVertex(topMinX, topY, topMaxZ, -0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val l3 = addVertex(topMinX, topY, topMinZ, -0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            addQuad(l0, l1, l2, l3)

            // Right face (+X)
            val r0 = addVertex(bottomMaxX, bottomY, bottomMaxZ, 0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val r1 = addVertex(bottomMaxX, bottomY, bottomMinZ, 0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val r2 = addVertex(topMaxX, topY, topMinZ, 0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            val r3 = addVertex(topMaxX, topY, topMaxZ, 0.9f, 0.4f, 0f, color[0], color[1], color[2], color[3])
            addQuad(r0, r1, r2, r3)
        }

        /**
         * Subdivided curved surface for hoods and roof panels with central crown.
         */
        fun addCurvedSurface(
            xLeft: Float, xRight: Float,
            zFront: Float, zBack: Float,
            yFront: Float, yBack: Float,
            yCrown: Float,
            color: FloatArray
        ) {
            val stepsX = 4
            val stepsZ = 4
            val grid = Array(stepsX + 1) { ShortArray(stepsZ + 1) }

            for (ix in 0..stepsX) {
                val u = ix.toFloat() / stepsX
                val x = xLeft + u * (xRight - xLeft)
                // Crown curvature along X (parabolic arch)
                val arch = 1.0f - (2.0f * u - 1.0f) * (2.0f * u - 1.0f)
                val curCrown = yCrown * arch

                for (iz in 0..stepsZ) {
                    val v = iz.toFloat() / stepsZ
                    val z = zFront + v * (zBack - zFront)
                    val baseSlopeY = yFront + v * (yBack - yFront)
                    val y = baseSlopeY + curCrown

                    // Normal calculation with upward bias and outward flare
                    val nx = (2.0f * u - 1.0f) * 0.4f
                    val ny = 0.90f
                    val nz = ((yBack - yFront) / (zBack - zFront)) * -0.4f

                    grid[ix][iz] = addVertex(x, y, z, nx, ny, nz, color[0], color[1], color[2], color[3])
                }
            }

            for (ix in 0 until stepsX) {
                for (iz in 0 until stepsZ) {
                    addQuad(
                        grid[ix][iz],
                        grid[ix + 1][iz],
                        grid[ix + 1][iz + 1],
                        grid[ix][iz + 1]
                    )
                }
            }
        }

        /**
         * Sculpted wheel arch cutout with inner dark liner and flared fender contour.
         */
        fun addCurvedWheelArch(
            centerX: Float, centerY: Float, centerZ: Float,
            archRadius: Float, innerWidth: Float, outerWidth: Float,
            fenderColor: FloatArray, linerColor: FloatArray,
            isLeft: Boolean
        ) {
            val segments = 8
            val angleStart = (PI * 0.12).toFloat()
            val angleEnd = (PI * 0.88).toFloat()
            val angleStep = (angleEnd - angleStart) / segments

            val dirX = if (isLeft) -1.0f else 1.0f
            val baseInnerX = centerX - dirX * innerWidth
            val baseOuterX = centerX + dirX * outerWidth

            for (s in 0 until segments) {
                val a0 = angleStart + s * angleStep
                val a1 = angleStart + (s + 1) * angleStep

                val z0 = centerZ + cos(a0) * archRadius
                val y0 = centerY + sin(a0) * archRadius
                val z1 = centerZ + cos(a1) * archRadius
                val y1 = centerY + sin(a1) * archRadius

                // Flared fender lip face
                val flareNormalY = 0.5f
                val flareNormalZ = cos((a0 + a1) * 0.5f) * 0.4f
                val f0 = addVertex(baseInnerX, y0, z0, dirX * 0.8f, flareNormalY, flareNormalZ, fenderColor[0], fenderColor[1], fenderColor[2], fenderColor[3])
                val f1 = addVertex(baseOuterX, y0 + 0.05f, z0, dirX, flareNormalY, flareNormalZ, fenderColor[0], fenderColor[1], fenderColor[2], fenderColor[3])
                val f2 = addVertex(baseOuterX, y1 + 0.05f, z1, dirX, flareNormalY, flareNormalZ, fenderColor[0], fenderColor[1], fenderColor[2], fenderColor[3])
                val f3 = addVertex(baseInnerX, y1, z1, dirX * 0.8f, flareNormalY, flareNormalZ, fenderColor[0], fenderColor[1], fenderColor[2], fenderColor[3])

                if (isLeft) {
                    addQuad(f0, f1, f2, f3)
                } else {
                    addQuad(f1, f0, f3, f2)
                }

                // Inner dark wheel liner tub
                val l0 = addVertex(centerX, y0, z0, 0f, -1f, 0f, linerColor[0], linerColor[1], linerColor[2], linerColor[3])
                val l1 = addVertex(baseInnerX, y0, z0, 0f, -1f, 0f, linerColor[0], linerColor[1], linerColor[2], linerColor[3])
                val l2 = addVertex(baseInnerX, y1, z1, 0f, -1f, 0f, linerColor[0], linerColor[1], linerColor[2], linerColor[3])
                val l3 = addVertex(centerX, y1, z1, 0f, -1f, 0f, linerColor[0], linerColor[1], linerColor[2], linerColor[3])
                if (isLeft) {
                    addQuad(l0, l1, l2, l3)
                } else {
                    addQuad(l1, l0, l3, l2)
                }
            }
        }

        /**
         * Aerodynamic curved 3D airfoil wing profile.
         */
        fun addAirfoilWing(
            spanMinX: Float, spanMaxX: Float,
            chordZBack: Float, chordZFront: Float,
            elevY: Float, camber: Float,
            color: FloatArray
        ) {
            val chordMidZ = (chordZFront + chordZBack) * 0.5f

            // Top curved high-camber surface
            val t0 = addVertex(spanMinX, elevY, chordZFront, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            val t1 = addVertex(spanMaxX, elevY, chordZFront, 0f, 0.4f, 0.9f, color[0], color[1], color[2], color[3])
            val t2 = addVertex(spanMaxX, elevY + camber, chordMidZ, 0f, 1.0f, 0f, color[0], color[1], color[2], color[3])
            val t3 = addVertex(spanMinX, elevY + camber, chordMidZ, 0f, 1.0f, 0f, color[0], color[1], color[2], color[3])
            addQuad(t0, t1, t2, t3)

            val t4 = addVertex(spanMinX, elevY + camber, chordMidZ, 0f, 0.9f, -0.4f, color[0], color[1], color[2], color[3])
            val t5 = addVertex(spanMaxX, elevY + camber, chordMidZ, 0f, 0.9f, -0.4f, color[0], color[1], color[2], color[3])
            val t6 = addVertex(spanMaxX, elevY + 0.015f, chordZBack, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            val t7 = addVertex(spanMinX, elevY + 0.015f, chordZBack, 0f, 0.4f, -0.9f, color[0], color[1], color[2], color[3])
            addQuad(t4, t5, t6, t7)

            // Bottom flat high-pressure surface
            val b0 = addVertex(spanMaxX, elevY - 0.02f, chordZFront, 0f, -1.0f, 0f, color[0], color[1], color[2], color[3])
            val b1 = addVertex(spanMinX, elevY - 0.02f, chordZFront, 0f, -1.0f, 0f, color[0], color[1], color[2], color[3])
            val b2 = addVertex(spanMinX, elevY - 0.01f, chordZBack, 0f, -1.0f, 0f, color[0], color[1], color[2], color[3])
            val b3 = addVertex(spanMaxX, elevY - 0.01f, chordZBack, 0f, -1.0f, 0f, color[0], color[1], color[2], color[3])
            addQuad(b0, b1, b2, b3)
        }

        fun addDivePlane(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            isLeft: Boolean, color: FloatArray
        ) {
            val normalX = if (isLeft) -0.7f else 0.7f
            val p0 = addVertex(x0, y0, z0, normalX, 0.7f, 0.3f, color[0], color[1], color[2], color[3])
            val p1 = addVertex(x1, y1, z1, normalX, 0.7f, 0.3f, color[0], color[1], color[2], color[3])
            val p2 = addVertex(x1 + (if (isLeft) -0.06f else 0.06f), y1 + 0.025f, z1 - 0.12f, normalX, 0.7f, 0.3f, color[0], color[1], color[2], color[3])
            val p3 = addVertex(x0 + (if (isLeft) -0.06f else 0.06f), y0 + 0.025f, z0 - 0.12f, normalX, 0.7f, 0.3f, color[0], color[1], color[2], color[3])
            if (isLeft) addQuad(p0, p1, p2, p3) else addQuad(p1, p0, p3, p2)
        }

        fun addProjectorHeadlights(
            minX: Float, maxX: Float, minY: Float, maxY: Float, z: Float,
            lightColor: FloatArray, housingColor: FloatArray, isLeft: Boolean
        ) {
            // Dark aerodynamic recessed housing
            addBox(minX, minY, z - 0.08f, maxX, maxY, z, housingColor)
            // Twin projector optics with DRL eyebrow
            val midX = (minX + maxX) * 0.5f
            addBox(minX + 0.02f, minY + 0.02f, z, midX - 0.01f, maxY - 0.02f, z + 0.02f, lightColor)
            addBox(midX + 0.01f, minY + 0.02f, z, maxX - 0.02f, maxY - 0.02f, z + 0.02f, lightColor)
            // Swept DRL light ribbon along top edge
            addBox(minX + 0.01f, maxY - 0.02f, z, maxX - 0.01f, maxY, z + 0.025f, lightColor)
        }

        fun addExhaustPipe(
            centerX: Float, centerY: Float, centerZ: Float,
            pipeRadius: Float, metalColor: FloatArray
        ) {
            val segments = 10
            val darkInterior = floatArrayOf(0.04f, 0.04f, 0.05f, 1.0f)
            val angleStep = (2.0 * PI / segments).toFloat()

            for (s in 0 until segments) {
                val a0 = s * angleStep
                val a1 = (s + 1) * angleStep
                val y0 = centerY + sin(a0) * pipeRadius
                val z0 = centerZ
                val x0 = centerX + cos(a0) * pipeRadius
                val y1 = centerY + sin(a1) * pipeRadius
                val z1 = centerZ
                val x1 = centerX + cos(a1) * pipeRadius

                // Metallic outer annular ring
                val r0 = addVertex(x0, y0, z0, 0f, 0f, -1f, metalColor[0], metalColor[1], metalColor[2], metalColor[3])
                val r1 = addVertex(x1, y1, z1, 0f, 0f, -1f, metalColor[0], metalColor[1], metalColor[2], metalColor[3])
                val r2 = addVertex(centerX, centerY, z0, 0f, 0f, -1f, darkInterior[0], darkInterior[1], darkInterior[2], darkInterior[3])
                addTriangle(r0, r1, r2)
            }
        }

        fun toMesh(): Mesh {
            val vData = FloatArray(vertices.size)
            for (i in vertices.indices) vData[i] = vertices[i]
            val iData = ShortArray(indices.size)
            for (i in indices.indices) iData[i] = indices[i]
            return Mesh(vData, iData)
        }
    }
}
