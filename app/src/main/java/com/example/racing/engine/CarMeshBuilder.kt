package com.example.racing.engine

import kotlin.math.cos
import kotlin.math.sin

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

    // 1. APEX RS-1 VALKYRIE (GT Endurance Supercar)
    fun buildValkyrieGT(): CarModel {
        val md = MeshData()
        val bodyColor = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
        val carbonColor = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f)
        val glassColor = floatArrayOf(0.18f, 0.24f, 0.32f, 0.85f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f)
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f)
        val interiorColor = floatArrayOf(0.08f, 0.08f, 0.10f, 1.0f)

        // Underbody & Front Splitter
        md.addBox(-0.90f, 0.12f, -2.15f, 0.90f, 0.28f, 2.15f, carbonColor)
        md.addBox(-0.96f, 0.10f, 1.85f, 0.96f, 0.16f, 2.28f, carbonColor)

        // Lower Bumper & Intake Grille
        md.addBox(-0.85f, 0.16f, 2.05f, 0.85f, 0.36f, 2.22f, carbonColor)

        // Main Center Body Beltline
        md.addBox(-0.88f, 0.28f, -1.95f, 0.88f, 0.62f, 1.95f, bodyColor)

        // Sloped Hood Nose
        val hF0 = md.addVertex(-0.84f, 0.38f, 2.15f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hF1 = md.addVertex(0.84f, 0.38f, 2.15f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hB1 = md.addVertex(0.86f, 0.62f, 1.05f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hB0 = md.addVertex(-0.86f, 0.62f, 1.05f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        md.addQuad(hF0, hF1, hB1, hB0)

        // Hood Extraction Vents (Carbon Louvers)
        md.addBox(-0.55f, 0.52f, 1.35f, -0.22f, 0.55f, 1.75f, carbonColor)
        md.addBox(0.22f, 0.52f, 1.35f, 0.55f, 0.55f, 1.75f, carbonColor)

        // Cockpit Greenhouse (Glass & Canopy)
        md.addBox(-0.62f, 1.00f, -0.65f, 0.62f, 1.08f, 0.35f, bodyColor)

        // Windshield
        val w0 = md.addVertex(-0.68f, 0.62f, 1.05f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w1 = md.addVertex(0.68f, 0.62f, 1.05f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w2 = md.addVertex(0.60f, 1.02f, 0.35f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w3 = md.addVertex(-0.60f, 1.02f, 0.35f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(w0, w1, w2, w3)

        // Rear window
        val rw0 = md.addVertex(0.60f, 1.02f, -0.65f, 0f, 0.6f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw1 = md.addVertex(-0.60f, 1.02f, -0.65f, 0f, 0.6f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw2 = md.addVertex(-0.70f, 0.65f, -1.35f, 0f, 0.6f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw3 = md.addVertex(0.70f, 0.65f, -1.35f, 0f, 0.6f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(rw0, rw1, rw2, rw3)

        // Side windows
        md.addBox(-0.68f, 0.64f, -0.65f, -0.62f, 1.00f, 0.35f, glassColor)
        md.addBox(0.62f, 0.64f, -0.65f, 0.68f, 1.00f, 0.35f, glassColor)

        // Cockpit Interior: Dashboard, Steering Wheel, Race Bucket Seat
        md.addBox(-0.55f, 0.58f, 0.55f, 0.55f, 0.72f, 0.85f, interiorColor) // Dashboard
        md.addBox(-0.25f, 0.68f, 0.45f, -0.05f, 0.88f, 0.48f, carbonColor)   // Steering wheel
        md.addBox(-0.35f, 0.35f, -0.25f, 0.05f, 0.85f, 0.15f, interiorColor) // Driver bucket seat

        // Rear Decklid & Aero Haunches
        md.addBox(-0.88f, 0.58f, -1.95f, 0.88f, 0.68f, -1.25f, bodyColor)
        md.addBox(-0.85f, 0.12f, -2.18f, 0.85f, 0.32f, -1.90f, carbonColor) // Diffuser

        // GT Rear Wing & Endplates
        md.addBox(-0.52f, 0.68f, -1.75f, -0.46f, 1.10f, -1.65f, carbonColor)
        md.addBox(0.46f, 0.68f, -1.75f, 0.52f, 1.10f, -1.65f, carbonColor)
        md.addBox(-0.95f, 1.10f, -1.82f, 0.95f, 1.16f, -1.52f, carbonColor) // Wing blade
        md.addBox(-0.96f, 1.02f, -1.84f, -0.94f, 1.24f, -1.50f, carbonColor)
        md.addBox(0.94f, 1.02f, -1.84f, 0.96f, 1.24f, -1.50f, carbonColor)

        // Headlights & Taillights
        md.addBox(-0.80f, 0.42f, 2.08f, -0.55f, 0.48f, 2.16f, lightColor)
        md.addBox(0.55f, 0.42f, 2.08f, 0.80f, 0.48f, 2.16f, lightColor)
        md.addBox(-0.82f, 0.54f, -2.00f, 0.82f, 0.60f, -1.94f, taillightColor) // Tail light bar

        // Dual Chrome Exhausts
        val metal = floatArrayOf(0.85f, 0.85f, 0.90f, 1.0f)
        md.addBox(-0.28f, 0.22f, -2.18f, -0.16f, 0.28f, -2.08f, metal)
        md.addBox(0.16f, 0.22f, -2.18f, 0.28f, 0.28f, -2.08f, metal)

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(radius = 0.35f, width = 0.28f, rimRadius = 0.24f, spokeCount = 6)
        val shadowMesh = createContactShadowMesh(length = 4.4f, width = 2.0f)
        val wheelPositions = arrayOf(
            Vector3f(-0.84f, 0.35f, 1.35f),
            Vector3f(0.84f, 0.35f, 1.35f),
            Vector3f(-0.86f, 0.36f, -1.30f),
            Vector3f(0.86f, 0.36f, -1.30f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    // 2. APEX SPIRIT GTS (Tuner / Drift Coupe)
    fun buildSpiritGTS(): CarModel {
        val md = MeshData()
        val bodyColor = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
        val trimBlack = floatArrayOf(0.15f, 0.15f, 0.16f, 1.0f)
        val glassColor = floatArrayOf(0.16f, 0.20f, 0.28f, 0.85f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f)
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f)
        val intercoolerColor = floatArrayOf(0.75f, 0.78f, 0.82f, 1.0f)

        // Lower Chassis & Widebody Rocker Panels
        md.addBox(-0.85f, 0.14f, -2.05f, 0.85f, 0.30f, 2.05f, trimBlack)

        // Front Tuner Bumper with Large Center Intercooler Intake
        md.addBox(-0.82f, 0.16f, 1.85f, 0.82f, 0.42f, 2.12f, bodyColor)
        md.addBox(-0.45f, 0.18f, 2.08f, 0.45f, 0.36f, 2.14f, intercoolerColor) // Front-mount Intercooler

        // Front Lip Splitter
        md.addBox(-0.88f, 0.12f, 1.90f, 0.88f, 0.16f, 2.18f, trimBlack)

        // Main Body & Flared Wheel Arches
        md.addBox(-0.84f, 0.30f, -1.90f, 0.84f, 0.65f, 1.85f, bodyColor)
        // Flared Rear Fenders
        md.addBox(-0.90f, 0.32f, -1.45f, -0.84f, 0.60f, -0.85f, bodyColor)
        md.addBox(0.84f, 0.32f, -1.45f, 0.90f, 0.60f, -0.85f, bodyColor)

        // Hood
        val hF0 = md.addVertex(-0.80f, 0.45f, 2.05f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hF1 = md.addVertex(0.80f, 0.45f, 2.05f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hB1 = md.addVertex(0.82f, 0.68f, 0.95f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        val hB0 = md.addVertex(-0.82f, 0.68f, 0.95f, 0f, 0.7f, 0.7f, bodyColor[0], bodyColor[1], bodyColor[2], bodyColor[3])
        md.addQuad(hF0, hF1, hB1, hB0)

        // Classic Fastback Coupe Cabin (Taller greenhouse, sloped fastback rear)
        md.addBox(-0.62f, 1.05f, -0.55f, 0.62f, 1.12f, 0.30f, bodyColor) // Roof

        // Windshield
        val w0 = md.addVertex(-0.66f, 0.68f, 0.95f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w1 = md.addVertex(0.66f, 0.68f, 0.95f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w2 = md.addVertex(0.58f, 1.07f, 0.30f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w3 = md.addVertex(-0.58f, 1.07f, 0.30f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(w0, w1, w2, w3)

        // Fastback Sloped Rear Window
        val rw0 = md.addVertex(0.58f, 1.07f, -0.55f, 0f, 0.5f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw1 = md.addVertex(-0.58f, 1.07f, -0.55f, 0f, 0.5f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw2 = md.addVertex(-0.68f, 0.70f, -1.45f, 0f, 0.5f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val rw3 = md.addVertex(0.68f, 0.70f, -1.45f, 0f, 0.5f, -0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(rw0, rw1, rw2, rw3)

        // Side windows
        md.addBox(-0.66f, 0.70f, -0.55f, -0.60f, 1.05f, 0.30f, glassColor)
        md.addBox(0.60f, 0.70f, -0.55f, 0.66f, 1.05f, 0.30f, glassColor)

        // Interior: Drift Steering Wheel & Tall E-Brake Lever
        md.addBox(-0.52f, 0.62f, 0.45f, 0.52f, 0.74f, 0.75f, trimBlack) // Dash
        md.addBox(-0.25f, 0.72f, 0.35f, -0.05f, 0.92f, 0.38f, floatArrayOf(0.9f, 0.2f, 0.2f, 1f)) // Red Sparco wheel
        md.addBox(0.04f, 0.50f, 0.05f, 0.08f, 0.82f, 0.10f, floatArrayOf(0.85f, 0.85f, 0.9f, 1f)) // Hydro e-brake handle

        // Ducktail Molded Rear Spoiler
        md.addBox(-0.76f, 0.70f, -1.95f, 0.76f, 0.78f, -1.72f, bodyColor)
        md.addBox(-0.74f, 0.76f, -1.98f, 0.74f, 0.86f, -1.86f, trimBlack) // Ducktail lip

        // Headlights & Taillights
        md.addBox(-0.78f, 0.46f, 2.00f, -0.52f, 0.54f, 2.08f, lightColor)
        md.addBox(0.52f, 0.46f, 2.00f, 0.78f, 0.54f, 2.08f, lightColor)
        md.addBox(-0.78f, 0.54f, -1.95f, -0.42f, 0.62f, -1.88f, taillightColor)
        md.addBox(0.42f, 0.54f, -1.95f, 0.78f, 0.62f, -1.88f, taillightColor)

        // Tuner Cannon Exhaust (Angled single exit)
        val metal = floatArrayOf(0.85f, 0.85f, 0.90f, 1.0f)
        md.addBox(0.45f, 0.20f, -2.12f, 0.62f, 0.30f, -1.95f, metal)

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(radius = 0.34f, width = 0.26f, rimRadius = 0.23f, spokeCount = 5)
        val shadowMesh = createContactShadowMesh(length = 4.2f, width = 1.9f)
        val wheelPositions = arrayOf(
            Vector3f(-0.80f, 0.34f, 1.25f),
            Vector3f(0.80f, 0.34f, 1.25f),
            Vector3f(-0.84f, 0.34f, -1.25f),
            Vector3f(0.84f, 0.34f, -1.25f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    // 3. APEX MONZA V10 (Prototype Le Mans / Trackday Hypercar)
    fun buildMonzaV10(): CarModel {
        val md = MeshData()
        val bodyColor = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
        val carbonColor = floatArrayOf(0.10f, 0.10f, 0.11f, 1.0f)
        val glassColor = floatArrayOf(0.10f, 0.18f, 0.25f, 0.90f)
        val lightColor = floatArrayOf(0.95f, 0.98f, 1.0f, 1.0f)
        val taillightColor = floatArrayOf(0.95f, 0.10f, 0.10f, 1.0f)
        val goldExhaust = floatArrayOf(0.95f, 0.75f, 0.25f, 1.0f)

        // Monocoque LMP Low Flat Floor & Front Keel
        md.addBox(-0.95f, 0.09f, -2.30f, 0.95f, 0.22f, 2.30f, carbonColor)
        md.addBox(-1.02f, 0.08f, 1.95f, 1.02f, 0.14f, 2.38f, carbonColor) // Extended Splitter

        // Dual Front Wheel Arch Aerodynamic Pods
        md.addBox(-0.98f, 0.22f, 0.95f, -0.65f, 0.58f, 2.05f, bodyColor)
        md.addBox(0.65f, 0.22f, 0.95f, 0.98f, 0.58f, 2.05f, bodyColor)

        // Low Center Nose Channel
        md.addBox(-0.35f, 0.22f, 1.15f, 0.35f, 0.44f, 2.25f, bodyColor)

        // Narrow Teardrop Cockpit Bubble
        md.addBox(-0.48f, 0.88f, -0.55f, 0.48f, 0.96f, 0.30f, bodyColor) // Roof

        // Panoramic Wrap-Around Windshield
        val w0 = md.addVertex(-0.52f, 0.48f, 0.85f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w1 = md.addVertex(0.52f, 0.48f, 0.85f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w2 = md.addVertex(0.44f, 0.92f, 0.30f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        val w3 = md.addVertex(-0.44f, 0.92f, 0.30f, 0f, 0.6f, 0.8f, glassColor[0], glassColor[1], glassColor[2], glassColor[3])
        md.addQuad(w0, w1, w2, w3)

        // LMP Dorsal Fin (Shark Fin Stabilizer running along center spine)
        md.addBox(-0.02f, 0.65f, -1.85f, 0.02f, 1.15f, -0.45f, carbonColor)

        // Side Pod Radiator Inlets
        md.addBox(-0.96f, 0.22f, -1.05f, -0.55f, 0.52f, 0.65f, bodyColor)
        md.addBox(0.55f, 0.22f, -1.05f, 0.96f, 0.52f, 0.65f, bodyColor)

        // Rear Wheel Aero Fairings & Haunches
        md.addBox(-0.98f, 0.22f, -2.15f, -0.65f, 0.62f, -1.05f, bodyColor)
        md.addBox(0.65f, 0.22f, -2.15f, 0.98f, 0.62f, -1.05f, bodyColor)

        // Massive Le Mans Carbon Rear Wing (Suspended on swan-neck mounts)
        md.addBox(-0.35f, 0.65f, -2.05f, -0.28f, 1.25f, -1.95f, carbonColor)
        md.addBox(0.28f, 0.65f, -2.05f, 0.35f, 1.25f, -1.95f, carbonColor)
        md.addBox(-1.08f, 1.22f, -2.18f, 1.08f, 1.28f, -1.85f, carbonColor) // High wing blade
        md.addBox(-1.10f, 1.08f, -2.22f, -1.06f, 1.38f, -1.82f, carbonColor) // Endplate Left
        md.addBox(1.06f, 1.08f, -2.22f, 1.10f, 1.38f, -1.82f, carbonColor) // Endplate Right

        // Venturi Tunnels Rear Diffuser
        md.addBox(-0.85f, 0.10f, -2.35f, 0.85f, 0.35f, -1.95f, carbonColor)

        // Top-Exit Screaming V10 Inconel Exhausts
        md.addBox(-0.16f, 0.62f, -1.35f, -0.06f, 0.68f, -1.20f, goldExhaust)
        md.addBox(0.06f, 0.62f, -1.35f, 0.16f, 0.68f, -1.20f, goldExhaust)

        // Vertical LED Headlight Stacks & Rear Wing Edge Lights
        md.addBox(-0.92f, 0.32f, 2.05f, -0.84f, 0.52f, 2.12f, lightColor)
        md.addBox(0.84f, 0.32f, 2.05f, 0.92f, 0.52f, 2.12f, lightColor)
        md.addBox(-1.08f, 1.10f, -2.18f, -1.06f, 1.35f, -2.15f, taillightColor)
        md.addBox(1.06f, 1.10f, -2.18f, 1.08f, 1.35f, -2.15f, taillightColor)

        val bodyMesh = md.toMesh()
        val wheelMesh = createDetailedWheelMesh(radius = 0.36f, width = 0.32f, rimRadius = 0.25f, spokeCount = 8)
        val shadowMesh = createContactShadowMesh(length = 4.7f, width = 2.15f)
        val wheelPositions = arrayOf(
            Vector3f(-0.86f, 0.36f, 1.45f),
            Vector3f(0.86f, 0.36f, 1.45f),
            Vector3f(-0.88f, 0.36f, -1.40f),
            Vector3f(0.88f, 0.36f, -1.40f)
        )
        return CarModel(bodyMesh, wheelMesh, shadowMesh, wheelPositions)
    }

    private fun createContactShadowMesh(length: Float, width: Float): Mesh {
        val md = MeshData()
        val halfL = length * 0.5f
        val halfW = width * 0.5f
        val shadowColor = floatArrayOf(0.02f, 0.02f, 0.03f, 0.70f)
        val fadeColor = floatArrayOf(0.02f, 0.02f, 0.03f, 0.0f)

        // Center solid core
        val c0 = md.addVertex(-halfW * 0.7f, 0.02f, halfL * 0.7f, 0f, 1f, 0f, shadowColor[0], shadowColor[1], shadowColor[2], shadowColor[3])
        val c1 = md.addVertex(halfW * 0.7f, 0.02f, halfL * 0.7f, 0f, 1f, 0f, shadowColor[0], shadowColor[1], shadowColor[2], shadowColor[3])
        val c2 = md.addVertex(halfW * 0.7f, 0.02f, -halfL * 0.7f, 0f, 1f, 0f, shadowColor[0], shadowColor[1], shadowColor[2], shadowColor[3])
        val c3 = md.addVertex(-halfW * 0.7f, 0.02f, -halfL * 0.7f, 0f, 1f, 0f, shadowColor[0], shadowColor[1], shadowColor[2], shadowColor[3])
        md.addQuad(c0, c1, c2, c3)

        // Outer soft falloff fringe
        val o0 = md.addVertex(-halfW, 0.02f, halfL, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])
        val o1 = md.addVertex(halfW, 0.02f, halfL, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])
        val o2 = md.addVertex(halfW, 0.02f, -halfL, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])
        val o3 = md.addVertex(-halfW, 0.02f, -halfL, 0f, 1f, 0f, fadeColor[0], fadeColor[1], fadeColor[2], fadeColor[3])

        md.addQuad(o0, o1, c1, c0) // Front fringe
        md.addQuad(c3, c2, o2, o3) // Rear fringe
        md.addQuad(o0, c0, c3, o3) // Left fringe
        md.addQuad(c1, o1, o2, c2) // Right fringe

        return md.toMesh()
    }

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

        fun toMesh(): Mesh {
            val vData = FloatArray(vertices.size)
            for (i in vertices.indices) vData[i] = vertices[i]
            val iData = ShortArray(indices.size)
            for (i in indices.indices) iData[i] = indices[i]
            return Mesh(vData, iData)
        }
    }

    private fun createDetailedWheelMesh(radius: Float, width: Float, rimRadius: Float, spokeCount: Int): Mesh {
        val md = MeshData()
        val segments = 16

        val tireColor = floatArrayOf(0.14f, 0.14f, 0.15f, 1.0f)
        val rimColor = floatArrayOf(0.85f, 0.85f, 0.90f, 1.0f)
        val caliperColor = floatArrayOf(0.95f, 0.15f, 0.15f, 1.0f)
        val rotorColor = floatArrayOf(0.68f, 0.70f, 0.74f, 1.0f)

        val halfW = width * 0.5f
        val angleStep = (2.0 * Math.PI / segments).toFloat()

        for (s in 0 until segments) {
            val a0 = s * angleStep
            val a1 = (s + 1) * angleStep

            val c0 = cos(a0)
            val s0 = sin(a0)
            val c1 = cos(a1)
            val s1 = sin(a1)

            val y0 = s0 * radius
            val z0 = c0 * radius
            val y1 = s1 * radius
            val z1 = c1 * radius

            // Tread outer surface
            val t0 = md.addVertex(-halfW, y0, z0, 0f, s0, c0, tireColor[0], tireColor[1], tireColor[2])
            val t1 = md.addVertex(halfW, y0, z0, 0f, s0, c0, tireColor[0], tireColor[1], tireColor[2])
            val t2 = md.addVertex(halfW, y1, z1, 0f, s1, c1, tireColor[0], tireColor[1], tireColor[2])
            val t3 = md.addVertex(-halfW, y1, z1, 0f, s1, c1, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(t0, t1, t2, t3)

            // Outer Sidewall
            val ry0 = s0 * rimRadius
            val rz0 = c0 * rimRadius
            val ry1 = s1 * rimRadius
            val rz1 = c1 * rimRadius

            val sw0 = md.addVertex(halfW, y0, z0, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw1 = md.addVertex(halfW, ry0, rz0, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw2 = md.addVertex(halfW, ry1, rz1, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val sw3 = md.addVertex(halfW, y1, z1, 1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(sw0, sw1, sw2, sw3)

            // Inner Sidewall
            val isw0 = md.addVertex(-halfW, ry0, rz0, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw1 = md.addVertex(-halfW, y0, z0, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw2 = md.addVertex(-halfW, y1, z1, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            val isw3 = md.addVertex(-halfW, ry1, rz1, -1f, 0f, 0f, tireColor[0], tireColor[1], tireColor[2])
            md.addQuad(isw0, isw1, isw2, isw3)

            // Outer Alloy Rim Face
            val rimFaceX = halfW * 0.72f
            val rf0 = md.addVertex(rimFaceX, 0f, 0f, 1f, 0f, 0f, rimColor[0], rimColor[1], rimColor[2])
            val rf1 = md.addVertex(rimFaceX, ry0, rz0, 1f, 0f, 0f, rimColor[0], rimColor[1], rimColor[2])
            val rf2 = md.addVertex(rimFaceX, ry1, rz1, 1f, 0f, 0f, rimColor[0], rimColor[1], rimColor[2])
            md.addTriangle(rf0, rf1, rf2)
        }

        // Inner Brake Rotor & Caliper
        val rotorRadius = rimRadius * 0.75f
        for (s in 0 until segments) {
            val a0 = s * angleStep
            val a1 = (s + 1) * angleStep
            val c0 = cos(a0) * rotorRadius
            val s0 = sin(a0) * rotorRadius
            val c1 = cos(a1) * rotorRadius
            val s1 = sin(a1) * rotorRadius

            val d0 = md.addVertex(0f, 0f, 0f, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            val d1 = md.addVertex(0f, s0, c0, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            val d2 = md.addVertex(0f, s1, c1, 1f, 0f, 0f, rotorColor[0], rotorColor[1], rotorColor[2])
            md.addTriangle(d0, d1, d2)
        }

        // Red Brake Caliper
        md.addBox(0.02f, rotorRadius * 0.45f, rotorRadius * 0.55f, 0.08f, rotorRadius * 0.95f, rotorRadius * 0.95f, caliperColor)

        return md.toMesh()
    }
}
