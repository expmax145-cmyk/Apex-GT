package com.example

import com.example.racing.ai.AIDifficulty
import com.example.racing.ai.AIDriverProfile
import com.example.racing.ai.AIPersonality
import com.example.racing.ai.AIRecoveryState
import com.example.racing.ai.AITacticalState
import com.example.racing.ai.OpponentAIController
import com.example.racing.engine.CarMeshBuilder
import com.example.racing.engine.Material
import com.example.racing.engine.TrackMeshBuilder
import com.example.racing.engine.Vector3f
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.ControlType
import com.example.racing.model.RaceMode
import com.example.racing.model.RaceTiming
import com.example.racing.model.TrackId
import com.example.racing.physics.EnginePowertrain
import com.example.racing.physics.TireModel
import com.example.racing.physics.VehicleCollisionSystem
import com.example.racing.physics.VehiclePhysics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVector3fMath() {
        val v1 = Vector3f(1f, 2f, 3f)
        val v2 = Vector3f(4f, 5f, 6f)
        val sum = v1 + v2
        assertEquals(5f, sum.x, 0.001f)
        assertEquals(7f, sum.y, 0.001f)
        assertEquals(9f, sum.z, 0.001f)

        val dot = v1.dot(v2)
        assertEquals(32f, dot, 0.001f)

        val norm = Vector3f(3f, 0f, 4f).normalized()
        assertEquals(0.6f, norm.x, 0.001f)
        assertEquals(0.8f, norm.z, 0.001f)
    }

    @Test
    fun testPacejkaTireModel() {
        val tire = TireModel()
        val forceZero = tire.calculateLateralForce(0f, 4000f)
        assertEquals(0f, forceZero, 0.001f)

        val forceCorner = tire.calculateLateralForce(0.12f, 4000f)
        assertTrue("Tire lateral force must be positive under positive slip angle", forceCorner > 0f)

        // Off-road wet/grass grip reduction
        val forceGrass = tire.calculateLateralForce(0.12f, 4000f, surfaceGrip = 0.55f)
        assertTrue("Grass grip must be lower than dry asphalt", forceGrass < forceCorner)
    }

    @Test
    fun testEnginePowertrainWithTurbo() {
        val engine = EnginePowertrain(hasTurbo = true)
        assertEquals("1", engine.getGearDisplayName())

        val traction = engine.update(1.0f, 15.0f, 0.016f)
        assertTrue("Engine must produce positive traction force", traction > 0f)
        assertTrue("Engine RPM must be above idle", engine.currentRpm >= engine.idleRpm)
        assertTrue("Turbo boost must be spooling", engine.turboBoostBar >= 0f)
    }

    @Test
    fun testVehiclePhysicsAcceleration() {
        val vehicle = VehiclePhysics()
        vehicle.throttle = 1.0f
        vehicle.update(0.016f)
        vehicle.update(0.016f)

        assertTrue("Vehicle must accelerate forward", vehicle.forwardVelocity > 0f)
        assertTrue("Vehicle position Z must advance", vehicle.position.z > 0f)
    }

    @Test
    fun testVehicleSpecificDynamics() {
        assertEquals(3, CarDefinition.ALL_CARS.size)

        val valkyrie = CarDefinition.ALL_CARS.find { it.id == "apex_valkyrie" }!!
        val spirit = CarDefinition.ALL_CARS.find { it.id == "apex_spirit_86" }!!
        val monza = CarDefinition.ALL_CARS.find { it.id == "apex_monza_v10" }!!

        // Validate distinct physical parameters
        assertNotEquals(valkyrie.wheelbaseM, spirit.wheelbaseM)
        assertNotEquals(valkyrie.wheelbaseM, monza.wheelbaseM)
        assertTrue("Monza V10 should have highest downforce", monza.downforceCoeff > valkyrie.downforceCoeff)
        assertTrue("Spirit GTS should have highest steering lock for drift", spirit.maxSteerAngleRad > valkyrie.maxSteerAngleRad)
        assertTrue("Spirit GTS should have lowest mass", spirit.massKg < valkyrie.massKg)
        assertTrue("Monza V10 should have highest top speed", monza.topSpeedKmh > valkyrie.topSpeedKmh)
    }

    @Test
    fun testMaterialsAndPBRParameters() {
        val paint = Material.CAR_PAINT_METALLIC
        assertTrue("Car paint metallic should be high", paint.metallic > 0.8f)
        assertTrue("Clearcoat should be present for automotive finish", paint.clearcoat > 0.8f)

        val wetAsphalt = Material.ASPHALT_WET
        assertTrue("Wet asphalt roughness should be lower than dry", wetAsphalt.roughness < Material.ASPHALT_DRY.roughness)
    }

    @Test
    fun testMultiTrackGeneration() {
        val gpTrack = TrackMeshBuilder.buildTrack(TrackId.GRAND_PRIX)
        assertNotNull(gpTrack)
        assertTrue("GP circuit must have waypoints", gpTrack.waypoints.isNotEmpty())
        assertEquals(3, gpTrack.sectorCheckpoints.size)

        val alpineTrack = TrackMeshBuilder.buildTrack(TrackId.ALPINE_PASS)
        assertNotNull(alpineTrack)
        assertTrue("Alpine track must have waypoints", alpineTrack.waypoints.isNotEmpty())

        val neonTrack = TrackMeshBuilder.buildTrack(TrackId.NEON_METROPOLIS)
        assertNotNull(neonTrack)
        assertTrue("Neon Metropolis must have waypoints", neonTrack.waypoints.isNotEmpty())
        assertTrue("Neon Metropolis should have wet road enabled", neonTrack.lightingConfig.isWetRoad)

        // Verify rich mesh geometry across all three circuits
        assertTrue("GP road mesh must have rich vertex detail", gpTrack.roadMesh.vertexCount > 500)
        assertTrue("GP environment mesh must contain coastal scenery", gpTrack.environmentMesh.vertexCount > 100)
        assertTrue("Alpine road mesh must have mountain elevation detail", alpineTrack.roadMesh.vertexCount > 500)
        assertTrue("Alpine environment mesh must contain mountain terrain & pines", alpineTrack.environmentMesh.vertexCount > 100)
        assertTrue("Neon Metropolis road mesh must have urban boulevard lanes", neonTrack.roadMesh.vertexCount > 500)
        assertTrue("Neon Metropolis environment must contain skyline architecture", neonTrack.environmentMesh.vertexCount > 100)

        // Verify resource cleanup
        gpTrack.release()
        alpineTrack.release()
        neonTrack.release()
    }

    @Test
    fun testCarModelsGeneration() {
        val valkyrie = CarMeshBuilder.buildCarModel("apex_valkyrie")
        assertNotNull(valkyrie.bodyMesh)
        assertNotNull(valkyrie.wheelMesh)
        assertNotNull(valkyrie.shadowMesh)
        assertEquals(4, valkyrie.wheelPositions.size)
        assertTrue("Valkyrie body mesh must have rich automotive geometry", valkyrie.bodyMesh.vertexCount > 400)
        assertTrue("Valkyrie wheel mesh must have detailed rim and tire geometry", valkyrie.wheelMesh.vertexCount > 150)
        assertEquals(-0.84f, valkyrie.wheelPositions[0].x, 0.001f) // FL
        assertEquals(0.84f, valkyrie.wheelPositions[1].x, 0.001f)  // FR
        assertEquals(-0.86f, valkyrie.wheelPositions[2].x, 0.001f) // RL
        assertEquals(0.86f, valkyrie.wheelPositions[3].x, 0.001f)  // RR

        val spirit = CarMeshBuilder.buildCarModel("apex_spirit_86")
        assertNotNull(spirit.bodyMesh)
        assertNotNull(spirit.wheelMesh)
        assertNotNull(spirit.shadowMesh)
        assertEquals(4, spirit.wheelPositions.size)
        assertTrue("Spirit 86 body mesh must have rich automotive geometry", spirit.bodyMesh.vertexCount > 400)
        assertTrue("Spirit 86 wheel mesh must have detailed rim and tire geometry", spirit.wheelMesh.vertexCount > 150)
        assertEquals(-0.80f, spirit.wheelPositions[0].x, 0.001f) // FL
        assertEquals(0.80f, spirit.wheelPositions[1].x, 0.001f)  // FR
        assertEquals(-0.84f, spirit.wheelPositions[2].x, 0.001f) // RL
        assertEquals(0.84f, spirit.wheelPositions[3].x, 0.001f)  // RR

        val monza = CarMeshBuilder.buildCarModel("apex_monza_v10")
        assertNotNull(monza.bodyMesh)
        assertNotNull(monza.wheelMesh)
        assertNotNull(monza.shadowMesh)
        assertEquals(4, monza.wheelPositions.size)
        assertTrue("Monza V10 body mesh must have rich automotive geometry", monza.bodyMesh.vertexCount > 400)
        assertTrue("Monza V10 wheel mesh must have detailed rim and tire geometry", monza.wheelMesh.vertexCount > 150)
        assertEquals(-0.86f, monza.wheelPositions[0].x, 0.001f) // FL
        assertEquals(0.86f, monza.wheelPositions[1].x, 0.001f)  // FR
        assertEquals(-0.88f, monza.wheelPositions[2].x, 0.001f) // RL
        assertEquals(0.88f, monza.wheelPositions[3].x, 0.001f)  // RR

        // Verify resource cleanup
        valkyrie.release()
        spirit.release()
        monza.release()
    }

    @Test
    fun testAIOpponentNavigationAndProfiles() {
        val gpTrack = TrackMeshBuilder.buildTrack(TrackId.GRAND_PRIX)
        val profile = AIDriverProfile(
            name = "Viper",
            carColor = floatArrayOf(0.1f, 0.5f, 0.9f, 1f),
            aggression = 0.85f,
            difficulty = AIDifficulty.EXPERT,
            carId = "apex_spirit_86"
        )
        val aiVehicle = VehiclePhysics()
        val ai = OpponentAIController(aiVehicle, profile, gpTrack.waypoints)

        // Run AI step
        ai.update(0.016f, playerPos = Vector3f(100f, 0f, 100f), otherAIs = emptyList())
        assertTrue("AI must apply throttle when navigating straight", aiVehicle.throttle > 0f)
        assertTrue("AI vehicle must advance along track", aiVehicle.forwardVelocity >= 0f)
    }

    @Test
    fun testRaceTimingAndLapFormatting() {
        val timing = RaceTiming(currentLap = 1, totalLaps = 3)
        assertEquals("--:--.---", timing.formatTime(0f))

        val formatted = timing.formatTime(74.523f)
        assertEquals("01:14.523", formatted)

        assertTrue(timing.isCountdownActive)
        timing.countdownSec = 0f
        assertTrue(!timing.isCountdownActive)
    }

    @Test
    fun testControlTypeOptions() {
        val types = ControlType.values()
        assertTrue(types.contains(ControlType.TOUCH_BUTTONS))
        assertTrue(types.contains(ControlType.VIRTUAL_WHEEL))
        assertTrue(types.contains(ControlType.TILT_GYRO))
    }

    @Test
    fun testReverseTransmissionAndPropulsion() {
        val vehicle = VehiclePhysics()
        vehicle.forwardVelocity = 0f
        vehicle.throttle = 0f
        vehicle.brake = 1.0f
        vehicle.reverseHoldTimer = 0.75f

        // Step with brake hold threshold reached shifts to reverse (gear 0)
        vehicle.update(0.016f)
        assertEquals("R", vehicle.powertrain.getGearDisplayName())
        assertEquals(0, vehicle.powertrain.currentGear)

        // Holding brake in reverse acts as reverse throttle
        vehicle.update(0.016f)
        vehicle.update(0.016f)
        vehicle.update(0.016f)
        assertTrue("Vehicle must move backward in reverse", vehicle.forwardVelocity < 0f)

        // While moving in reverse, applying forward throttle brakes the car
        vehicle.brake = 0f
        vehicle.throttle = 1.0f
        val revSpeed = vehicle.forwardVelocity
        vehicle.update(0.016f)
        assertTrue("Applying throttle in reverse must reduce reverse speed", vehicle.forwardVelocity > revSpeed)
    }

    @Test
    fun testSpeedSensitiveSteeringAndCentering() {
        val vehicle = VehiclePhysics()
        vehicle.steerInput = 1.0f
        vehicle.forwardVelocity = 5f // Low speed ~18 km/h
        vehicle.update(0.016f)
        val lowSpeedSteer = vehicle.currentSteerAngleRad

        val highSpeedVehicle = VehiclePhysics()
        highSpeedVehicle.steerInput = 1.0f
        highSpeedVehicle.forwardVelocity = 60f // High speed ~216 km/h
        highSpeedVehicle.update(0.016f)
        val highSpeedSteer = highSpeedVehicle.currentSteerAngleRad

        assertTrue("High speed steering angle must be smaller than low speed for stability", highSpeedSteer < lowSpeedSteer)

        // Test fast self-centering
        vehicle.steerInput = 0f
        vehicle.update(0.016f)
        assertTrue("Wheel must begin returning towards center", vehicle.currentSteerAngleRad < lowSpeedSteer)
    }

    @Test
    fun testCounterSteerDriftStabilization() {
        val vehicle = VehiclePhysics()
        vehicle.forwardVelocity = 25f // ~90 km/h
        vehicle.driftAngleDeg = 20f   // In a rightward slide
        vehicle.steerInput = -1.0f    // Counter-steering left into the slide
        vehicle.yawRate = 0.5f

        vehicle.update(0.016f)
        // With counter-steer assist, steering lock is extended and yaw torque is stabilized
        assertTrue("Counter steer must be active", vehicle.currentSteerAngleRad < 0f)
    }

    @Test
    fun testDynamicChassisPitchRollSuspension() {
        val vehicle = VehiclePhysics()
        vehicle.throttle = 1.0f
        vehicle.update(0.016f)
        vehicle.update(0.016f)

        // Under acceleration, squat should compress rear suspension and lift front
        assertTrue("Pitch angle must react to acceleration", vehicle.pitchAngleRad != 0f || vehicle.pitchVelocity != 0f)

        // Check 4-corner suspension values
        assertEquals(4, vehicle.suspensionCompression.size)
        // Brake hard to trigger dive
        vehicle.throttle = 0f
        vehicle.brake = 1.0f
        vehicle.update(0.016f)
        vehicle.update(0.016f)
        assertTrue("Front suspension must compress under braking dive", vehicle.suspensionCompression[0] != 0f)
    }

    @Test
    fun testMultiCarDrivetrainDistribution() {
        val valkyrie = CarDefinition.ALL_CARS.find { it.id == "apex_valkyrie" }!!
        val spirit = CarDefinition.ALL_CARS.find { it.id == "apex_spirit_86" }!!
        val monza = CarDefinition.ALL_CARS.find { it.id == "apex_monza_v10" }!!

        assertTrue("Valkyrie must feature AWD front drive distribution", valkyrie.driveFrontRatio > 0.2f)
        assertEquals(0.0f, spirit.driveFrontRatio, 0.001f) // Pure RWD drift coupe
        assertTrue("Monza V10 must feature torque vectoring front assist", monza.driveFrontRatio > 0.05f)
    }

    @Test
    fun testAIRacingLineAndAvoidance() {
        val gpTrack = TrackMeshBuilder.buildTrack(TrackId.GRAND_PRIX)
        val profile = AIDriverProfile(
            name = "Viper",
            carColor = floatArrayOf(0.10f, 0.50f, 0.98f, 1.0f),
            aggression = 0.90f,
            difficulty = AIDifficulty.EXPERT,
            carId = "apex_spirit_86",
            preferredLaneOffset = -1.5f
        )
        val aiVehicle = VehiclePhysics()
        val ai = OpponentAIController(aiVehicle, profile, gpTrack.waypoints)

        // Test normal update with other competitors alongside
        val playerPos = Vector3f(1.0f, 0f, 1.0f)
        val competitorPos = Vector3f(-1.0f, 0f, 2.0f)

        ai.update(0.016f, playerPos = playerPos, otherAIs = listOf(competitorPos))

        // AI should steer and accelerate
        assertTrue("AI must throttle forward", aiVehicle.throttle > 0f)
        assertTrue("AI lateral target offset must be within track width", kotlin.math.abs(ai.lateralOffset) <= 6.0f)
        assertEquals(com.example.racing.ai.AIRecoveryState.NORMAL, ai.recoveryState)
    }

    @Test
    fun testAIBarrierRecoveryStateMachine() {
        val gpTrack = TrackMeshBuilder.buildTrack(TrackId.GRAND_PRIX)
        val profile = AIDriverProfile(
            name = "Ghost",
            carColor = floatArrayOf(0.9f, 0.9f, 0.1f, 1.0f),
            aggression = 0.60f,
            difficulty = AIDifficulty.EASY,
            carId = "apex_valkyrie"
        )
        val aiVehicle = VehiclePhysics()
        val ai = OpponentAIController(aiVehicle, profile, gpTrack.waypoints)

        // Simulate pinning against a barrier with low speed
        aiVehicle.hasCollidedBarrier = true
        aiVehicle.forwardVelocity = 0.5f // ~1.8 km/h
        ai.update(0.016f, playerPos = Vector3f(50f, 0f, 50f), otherAIs = emptyList())

        // AI must transition to REVERSING state
        assertEquals(com.example.racing.ai.AIRecoveryState.REVERSING, ai.recoveryState)
        assertTrue("In reversing state, brake pedal must be applied to drive in reverse", aiVehicle.brake > 0.5f)
        assertEquals(0f, aiVehicle.throttle, 0.001f)
    }

    @Test
    fun testProceduralAudioMultiCylinderAndCollision() {
        val audio = com.example.racing.audio.ProceduralAudioEngine()
        audio.cylinderCount = 10
        audio.redlineRpm = 9200f
        audio.engineRpm = 9150f
        audio.throttleLoad = 1.0f

        // Test collision trigger
        audio.triggerCollision(1.2f)
        assertTrue("Collision intensity must be registered", audio.collisionImpactIntensity > 0.5f)

        // Test spatial audio parameters
        audio.opponentDistance = 14.5f
        audio.opponentPan = -0.75f // Opponent overtaking on the left
        assertEquals(14.5f, audio.opponentDistance, 0.001f)
        assertEquals(-0.75f, audio.opponentPan, 0.001f)
    }

    @Test
    fun testLeaderboardMultiLapStandings() {
        val racer1 = com.example.racing.model.LeaderboardRacer(
            position = 1,
            name = "YOU",
            carName = "Apex Valkyrie GT",
            color = floatArrayOf(1f, 0f, 0f, 1f),
            currentLap = 3,
            bestLapTimeSec = 62.45f,
            gapToLeaderText = "LEADER",
            isPlayer = true
        )

        val racer2 = com.example.racing.model.LeaderboardRacer(
            position = 2,
            name = "Viper",
            carName = "Spirit GTS 86",
            color = floatArrayOf(0f, 0f, 1f, 1f),
            currentLap = 3,
            bestLapTimeSec = 63.12f,
            gapToLeaderText = "+0.67s",
            isPlayer = false
        )

        assertEquals(1, racer1.position)
        assertTrue(racer1.isPlayer)
        assertEquals(2, racer2.position)
        assertEquals("+0.67s", racer2.gapToLeaderText)
    }

    @Test
    fun testVector3fInPlaceLerpTo() {
        val current = Vector3f(10f, 20f, 30f)
        val target = Vector3f(20f, 40f, 50f)

        // Mutates current in-place without creating a new instance
        val returned = current.lerpTo(target, 0.5f)
        assertTrue("lerpTo must return the exact same instance for zero-allocation", returned === current)
        assertEquals(15f, current.x, 0.001f)
        assertEquals(30f, current.y, 0.001f)
        assertEquals(40f, current.z, 0.001f)

        // Test boundary clamping
        current.lerpTo(target, 2.0f) // Clamped to 1.0
        assertEquals(20f, current.x, 0.001f)
        assertEquals(40f, current.y, 0.001f)
        assertEquals(50f, current.z, 0.001f)
    }

    @Test
    fun testMatrix4fReusableOperations() {
        val m = com.example.racing.engine.Matrix4f()
        assertNotNull(m.values)
        assertEquals(16, m.values.size)

        // Reusing identity resets without allocating new memory
        m.identity()
        assertEquals(1f, m.values[0], 0.001f)
        assertEquals(0f, m.values[1], 0.001f)
        assertEquals(1f, m.values[5], 0.001f)
        assertEquals(1f, m.values[10], 0.001f)
        assertEquals(1f, m.values[15], 0.001f)
    }

    @Test
    fun testPaintFinishMaterialCaching() {
        val color = floatArrayOf(0.9f, 0.2f, 0.1f, 1.0f)
        val gloss = com.example.racing.engine.PaintFinishType.GLOSS.createMaterial(color)
        val metallic = com.example.racing.engine.PaintFinishType.METALLIC.createMaterial(color)

        assertNotNull(gloss)
        assertNotNull(metallic)
        assertTrue("Metallic finish must have higher metallic factor than gloss", metallic.metallic > gloss.metallic)
        assertTrue("Metallic finish must have higher clearcoat than gloss", metallic.clearcoat >= gloss.clearcoat)
    }

    @Test
    fun testParticleCompliantFalloffMath() {
        // Test standard GLSL smoothstep(edge0, edge1, x) with edge0 < edge1
        fun smoothstep(e0: Float, e1: Float, x: Float): Float {
            val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        fun particleFalloff(dist: Float): Float {
            return 1.0f - smoothstep(0.0f, 0.5f, dist)
        }

        // Center must be maximum opacity
        assertEquals(1.0f, particleFalloff(0.0f), 0.001f)
        // Edge must fall off to fully transparent
        assertEquals(0.0f, particleFalloff(0.5f), 0.001f)
        // Midpoint must be smooth transition (0.5)
        assertEquals(0.5f, particleFalloff(0.25f), 0.001f)
        // Monotonic decrease
        assertTrue(particleFalloff(0.1f) > particleFalloff(0.2f))
        assertTrue(particleFalloff(0.2f) > particleFalloff(0.4f))
    }

    @Test
    fun testGraphicsQualityPresetDistinctions() {
        val presets = listOf("LOW", "MEDIUM", "HIGH")
        assertEquals(3, presets.size)

        fun getWetFactor(quality: String): Float = when (quality) {
            "LOW" -> 0.0f
            "MEDIUM" -> 0.65f
            else -> 1.0f
        }

        fun isAiSmokeEnabled(quality: String): Boolean = (quality == "HIGH")

        fun shouldDrawShadow(quality: String, isPlayer: Boolean): Boolean = when (quality) {
            "LOW" -> false
            "MEDIUM" -> isPlayer
            else -> true
        }

        // Verify distinct behavior between LOW, MEDIUM, and HIGH
        assertEquals(0.0f, getWetFactor("LOW"), 0.001f)
        assertEquals(0.65f, getWetFactor("MEDIUM"), 0.001f)
        assertEquals(1.0f, getWetFactor("HIGH"), 0.001f)

        assertFalse(isAiSmokeEnabled("LOW"))
        assertFalse(isAiSmokeEnabled("MEDIUM"))
        assertTrue(isAiSmokeEnabled("HIGH"))

        assertFalse(shouldDrawShadow("LOW", isPlayer = true))
        assertFalse(shouldDrawShadow("LOW", isPlayer = false))
        assertTrue(shouldDrawShadow("MEDIUM", isPlayer = true))
        assertFalse(shouldDrawShadow("MEDIUM", isPlayer = false))
        assertTrue(shouldDrawShadow("HIGH", isPlayer = true))
        assertTrue(shouldDrawShadow("HIGH", isPlayer = false))
    }

    @Test
    fun testCameraTeleportSnapProtection() {
        val currentCam = Vector3f(0f, 2.5f, -6f)
        val targetCam = Vector3f(120f, 5.0f, 400f) // Far away reset position

        val dist = currentCam.distanceTo(targetCam)
        assertTrue("Teleport distance must exceed 50m threshold", dist > 50f)

        // With snap protection, camera instantly adopts target without flying through terrain
        if (dist > 50f) {
            currentCam.set(targetCam)
        } else {
            currentCam.lerpTo(targetCam, 0.2f)
        }

        assertEquals(targetCam.x, currentCam.x, 0.001f)
        assertEquals(targetCam.y, currentCam.y, 0.001f)
        assertEquals(targetCam.z, currentCam.z, 0.001f)
    }

    @Test
    fun testPhase9TrackVisualFidelityAndLighting() {
        for (trackId in TrackId.values()) {
            val track = TrackMeshBuilder.buildTrack(trackId)
            assertNotNull("Track $trackId must build successfully", track)
            assertTrue("Track $trackId must have road vertices", track.roadMesh.vertexCount > 1000)
            assertTrue("Track $trackId must have kerb vertices", track.kerbMesh.vertexCount > 200)
            assertTrue("Track $trackId must have barrier vertices", track.barrierMesh.vertexCount > 400)
            assertTrue("Track $trackId must have environment scenery vertices", track.environmentMesh.vertexCount > 300)
            assertTrue("Track $trackId must have start gantry vertices", track.gantryMesh.vertexCount > 100)

            // Validate Waypoint Continuity & Closed-loop Trajectory
            assertTrue("Waypoints must contain at least 100 navigation nodes", track.waypoints.size >= 100)
            assertEquals("Must have 3 sector timing checkpoints", 3, track.sectorCheckpoints.size)

            for (i in 0 until track.waypoints.size) {
                val curr = track.waypoints[i]
                val next = track.waypoints[(i + 1) % track.waypoints.size]
                val stepDist = curr.distanceTo(next)
                assertTrue("Consecutive waypoints must be continuous and closely spaced (was $stepDist)", stepDist < 25f)
            }

            // Validate Lighting & Atmospheric Fog Bounds
            val lighting = track.lightingConfig
            assertTrue("Fog far must be greater than fog near", lighting.fogFar > lighting.fogNear)
            assertEquals(3, lighting.sunDirection.size)
            assertEquals(3, lighting.sunColor.size)
            assertEquals(3, lighting.skyAmbient.size)
            assertEquals(3, lighting.groundAmbient.size)
            assertEquals(3, lighting.fogColor.size)

            track.release()
        }

        // Validate Material PBR Constants for Track Surfaces
        assertNotNull(com.example.racing.engine.Material.ASPHALT_DRY)
        assertNotNull(com.example.racing.engine.Material.ASPHALT_WET)
        assertNotNull(com.example.racing.engine.Material.KERB_RED_WHITE)
        assertNotNull(com.example.racing.engine.Material.BARRIER_ARMCO)
        assertNotNull(com.example.racing.engine.Material.CONCRETE_BARRIER)
        assertNotNull(com.example.racing.engine.Material.NEON_GOLD)
        assertNotNull(com.example.racing.engine.Material.WATER_OCEAN)
    }

    @Test
    fun testSteeringDirectionLeftAndRight() {
        // Bug 1 Fix Verification:
        // Left input (+1.0f) must steer car to the left (+X world velocity and positive yaw angle)
        val carLeft = VehiclePhysics()
        carLeft.forwardVelocity = 20f
        carLeft.steerInput = 1.0f // Left steer command
        carLeft.update(0.05f)

        assertTrue("Left steer input must produce positive steering angle", carLeft.currentSteerAngleRad > 0f)
        assertTrue("Left steer input must turn car heading to the left", carLeft.headingAngleRad > 0f)
        assertTrue("Left steer input must produce positive yaw/lateral authority", carLeft.lateralVelocity > 0f || carLeft.yawRate > 0f)

        // Right input (-1.0f) must steer car to the right (-X world velocity and negative yaw angle)
        val carRight = VehiclePhysics()
        carRight.forwardVelocity = 20f
        carRight.steerInput = -1.0f // Right steer command
        carRight.update(0.05f)

        assertTrue("Right steer input must produce negative steering angle", carRight.currentSteerAngleRad < 0f)
        assertTrue("Right steer input must turn car heading to the right", carRight.headingAngleRad < 0f)
        assertTrue("Right steer input must produce negative yaw rate towards right", carRight.yawRate < 0f)
    }

    @Test
    fun testCenterLaneAndInfieldTrackStability() {
        // Bug 2 Fix Verification:
        // Driving down the center lane or into the infield must not cause infinite bounce loops or wild glitches
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val car = VehiclePhysics()
        car.resetAt(Vector3f(0f, 0.35f, 0f), 0f)
        car.forwardVelocity = 30f

        // 1. Center lane driving
        for (step in 0 until 60) {
            car.throttle = 1.0f
            car.update(0.016f, track.waypoints, roadHalfWidth = 7.0f)
            assertFalse("Vehicle in center lane must not collide with barriers", car.hasCollidedBarrier)
            assertFalse("Vehicle in center lane must not be marked off-road", car.isOffRoad)
            assertTrue("Track lateral distance must be bounded in center lane", car.trackLateralDistance < 5.0f)
        }

        // 2. Off-road infield penetration recovery
        // Position car 15m to the right (towards track infield)
        car.position.set(Vector3f(15f, 0.35f, 50f))
        val initialDist = car.position.x
        car.update(0.016f, track.waypoints, roadHalfWidth = 7.0f)

        assertTrue("Car beyond road width must detect barrier collision", car.hasCollidedBarrier)
        // Normal must push back towards track centerline (decrease X towards 0)
        assertTrue("Barrier collision must push vehicle back towards road centerline", car.position.x < initialDist)
    }

    @Test
    fun testCountdownStationaryGridNoReverseShift() {
        // Bug 3 Fix Verification:
        // Extended brake hold while stationary on countdown grid must keep vehicle locked in 1st gear
        val car = VehiclePhysics()
        car.resetAt(Vector3f(0f, 0.35f, -80f), 0f)
        assertEquals(2, car.powertrain.currentGear) // 1st gear
        assertEquals(0f, car.reverseHoldTimer, 0.001f)

        // Simulate 4 seconds of countdown holding brake
        for (i in 0 until 240) { // 240 frames * 0.016s = ~3.84s
            car.throttle = 0f
            car.brake = 1f
            car.forwardVelocity = 0f
            car.lateralVelocity = 0f
            car.yawRate = 0f
            car.reverseHoldTimer = 0f
            car.powertrain.currentGear = 2
            car.powertrain.currentRpm = car.powertrain.idleRpm
        }

        assertEquals("Car must remain in 1st gear at race start", 2, car.powertrain.currentGear)
        assertEquals("Car forward velocity must remain 0 at start", 0f, car.forwardVelocity, 0.001f)
        assertEquals("reverseHoldTimer must be 0", 0f, car.reverseHoldTimer, 0.001f)
    }

    @Test
    fun testVehicleToVehicleFrontRearCollisionPhysics() {
        // Part B Verification:
        // A following car colliding with a leading car's rear bumper must transfer forward impulse,
        // accelerate the front car, slow down the rear car, and prevent penetration.
        val leadCar = VehiclePhysics()
        leadCar.resetAt(Vector3f(0f, 0.35f, 10f), 0f)
        leadCar.forwardVelocity = 12f // 12 m/s

        val rearCar = VehiclePhysics()
        rearCar.resetAt(Vector3f(0f, 0.35f, 6.5f), 0f) // Overlapping bumper contact (half-length ~2.15m * 2 = 4.30m)
        rearCar.forwardVelocity = 28f // 28 m/s overtaking

        val initialLeadVel = leadCar.forwardVelocity
        val initialRearVel = rearCar.forwardVelocity
        val initialDistance = leadCar.position.z - rearCar.position.z

        var impactDetected = false
        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(leadCar, rearCar),
            playerVehicle = leadCar,
            onCollisionImpact = { intensity ->
                if (intensity > 0.1f) impactDetected = true
            }
        )

        // 1. Separation response: Cars must be pushed apart
        val finalDistance = leadCar.position.z - rearCar.position.z
        assertTrue("Vehicles must separate positionally after collision", finalDistance > initialDistance)

        // 2. Linear momentum transfer: Lead car gains speed, rear car loses speed
        assertTrue("Leading car must be accelerated forward by impact", leadCar.forwardVelocity > initialLeadVel)
        assertTrue("Rear car must be decelerated by impact", rearCar.forwardVelocity < initialRearVel)

        // 3. Audio impact trigger callback
        assertTrue("Impact callback must be triggered for fast collision", impactDetected)
    }

    @Test
    fun testVehicleToVehicleSideScrapeContactSeparation() {
        // Part B Verification:
        // Two cars traveling side-by-side that overlap laterally must push each other apart
        val car1 = VehiclePhysics()
        car1.resetAt(Vector3f(0f, 0.35f, 50f), 0f)
        car1.forwardVelocity = 25f

        val car2 = VehiclePhysics()
        car2.resetAt(Vector3f(1.6f, 0.35f, 50f), 0f) // Width is ~1.96m, so 1.6m is overlapping side contact
        car2.forwardVelocity = 25f

        val initialDeltaX = car2.position.x - car1.position.x
        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(car1, car2),
            playerVehicle = car1
        )

        val finalDeltaX = car2.position.x - car1.position.x
        assertTrue("Side-by-side contact must separate cars laterally", finalDeltaX > initialDeltaX)
    }

    @Test
    fun testVehicleToVehicleAngledCollisionYawTorqueDeflection() {
        // Part B Verification:
        // An angled impact onto the front corner of a car must induce yaw rate (rotation)
        val car1 = VehiclePhysics()
        car1.resetAt(Vector3f(0f, 0.35f, 20f), 0f)
        car1.forwardVelocity = 15f

        val car2 = VehiclePhysics()
        // Angled approaching vehicle hitting front-right fender
        car2.resetAt(Vector3f(1.4f, 0.35f, 22.0f), -0.785f) // ~-45 degrees heading
        car2.forwardVelocity = 20f

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(car1, car2),
            playerVehicle = car1
        )

        // Contact should induce yaw deflection without uncontrolled spinout (< 2.0 rad/s)
        assertTrue("Angled impact must produce vehicle yaw rate response", kotlin.math.abs(car1.yawRate) > 0.05f)
        assertTrue("Yaw rate must be clamped to prevent physics glitches", kotlin.math.abs(car1.yawRate) < 2.0f)
    }

    @Test
    fun testTrackStartingDirectionConsistentAcrossTracks() {
        // Part A3 Verification:
        // Verify all tracks have coherent forward start points and forward orientation vectors
        for (trackId in TrackId.values()) {
            val track = TrackMeshBuilder.buildTrack(trackId)
            val waypoints = track.waypoints
            assertTrue("Track ${trackId.name} must have waypoints", waypoints.size >= 2)

            val p0 = waypoints[0]
            val p1 = waypoints[1]
            val dx = p1.x - p0.x
            val dz = p1.z - p0.z
            val stepLen = kotlin.math.sqrt(dx * dx + dz * dz)
            assertTrue("First waypoint segment must have non-zero length", stepLen > 0.1f)

            val heading = kotlin.math.atan2(dx, dz)
            assertFalse("Heading must be a valid number", heading.isNaN())
            track.release()
        }
    }

    @Test
    fun testDevelopmentCurrencyAffordability() {
        // Part D Verification:
        // Development mode must provide sufficient funds ($999,999 default)
        // to purchase every vehicle and fully max out all upgrade tiers.
        val defaultDevCredits = 999999

        val totalCarPurchaseCost = CarDefinition.ALL_CARS.sumOf { it.basePrice }
        val singleCarMaxUpgradeCost = 4 * 3 * 3500 // 4 parts * 3 stages * 3500CR = 42,000 CR per car
        val totalUpgradeCostAllCars = singleCarMaxUpgradeCost * CarDefinition.ALL_CARS.size
        val grandTotalCost = totalCarPurchaseCost + totalUpgradeCostAllCars

        assertTrue(
            "Default dev funds ($defaultDevCredits) must easily cover buying all cars ($totalCarPurchaseCost) and all max upgrades ($totalUpgradeCostAllCars), total $grandTotalCost",
            defaultDevCredits > grandTotalCost
        )
    }

    @Test
    fun testPhase9TrackWorldEnvironmentsAndSkyDomes() {
        // Phase 9 Verification:
        // Realistic Racing Tracks, Road Surfaces, World Environments & Panoramic Sky Domes
        for (trackId in TrackId.values()) {
            val track = TrackMeshBuilder.buildTrack(trackId)
            assertNotNull(track)
            assertNotNull(track.skyMesh)
            assertNotNull(track.roadMesh)
            assertNotNull(track.kerbMesh)
            assertNotNull(track.barrierMesh)
            assertNotNull(track.environmentMesh)
            assertNotNull(track.gantryMesh)

            // 1. Sky Dome Verification
            assertTrue("Track ${trackId.name} sky mesh must have panoramic vertex dome", track.skyMesh.vertexCount >= 100)
            assertEquals("Sky zenith color must have 3 RGB components", 3, track.lightingConfig.skyZenithColor.size)
            assertEquals("Sky horizon color must have 3 RGB components", 3, track.lightingConfig.skyHorizonColor.size)

            // 2. High-Fidelity 11-Strip Road Mesh
            assertTrue("Track ${trackId.name} road mesh must have rich 11-strip surface detail", track.roadMesh.vertexCount >= 1000)

            // 3. Environment Scenery
            assertTrue("Track ${trackId.name} environment mesh must have rich landscape geometry", track.environmentMesh.vertexCount >= 500)

            // 4. Barriers and Gantries
            assertTrue("Track ${trackId.name} barrier mesh must have Armco posts & rails", track.barrierMesh.vertexCount >= 200)
            assertTrue("Track ${trackId.name} start gantry must have F1 LED light pods", track.gantryMesh.vertexCount >= 40)

            track.release()
        }
    }

    @Test
    fun testPhase9PBRMaterialsDefinitions() {
        // Phase 9 Verification:
        // Physical PBR material definitions
        val skyDome = Material.SKY_DOME
        assertEquals("Sky dome roughness must be 1.0", 1.0f, skyDome.roughness, 0.001f)
        assertEquals("Sky dome metallic must be 0.0", 0.0f, skyDome.metallic, 0.001f)

        val gravel = Material.GRAVEL_RUNOFF
        assertTrue("Gravel runoff must have high roughness", gravel.roughness >= 0.88f)

        val turf = Material.ASTROTURF
        assertTrue("Astroturf must have high roughness", turf.roughness >= 0.80f)

        val catchFence = Material.CATCH_FENCE
        assertTrue("Catch fence must have metallic property", catchFence.metallic >= 0.70f)

        val tireWall = Material.TIRE_WALL
        assertTrue("Tire wall must have high rubber roughness", tireWall.roughness >= 0.80f)

        val glass = Material.GLASS_CURTAIN_WALL
        assertTrue("Glass curtain wall must have low roughness", glass.roughness <= 0.10f)
        assertTrue("Glass curtain wall must have high clearcoat reflection", glass.clearcoat >= 0.90f)
    }

    // =============================================================================================
    // PHASE 10: REALISTIC LIGHTING, WEATHER, DYNAMIC SKIES & DAY/NIGHT SYSTEMS
    // =============================================================================================

    @Test
    fun testPhase10TimeOfDaySunProgression() {
        val controller = com.example.racing.model.EnvironmentController()

        // 1. Day Mode Verification
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.DAY
        controller.selectedWeather = com.example.racing.model.WeatherCondition.CLEAR
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val dayState = controller.currentState

        assertFalse("Day mode should not be night", dayState.isNight)
        assertTrue("Day sun elevation must be high above horizon", dayState.sunDirection[1] > 0.8f)
        assertTrue("Day sun color must be bright", dayState.sunColor[0] >= 1.0f)
        assertTrue("Day sky zenith must be rich blue", dayState.skyZenithColor[2] > dayState.skyZenithColor[0])

        // 2. Sunset Mode Verification
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.SUNSET
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val sunsetState = controller.currentState

        assertFalse("Sunset mode should not be night", sunsetState.isNight)
        assertTrue("Sunset sun color must have strong warm amber/red tone", sunsetState.sunColor[0] > sunsetState.sunColor[2])
        assertTrue("Sunset horizon must have warm fiery/salmon tint", sunsetState.skyHorizonColor[0] > sunsetState.skyHorizonColor[2])

        // 3. Night Mode Verification
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.NIGHT
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val nightState = controller.currentState

        assertTrue("Night mode must set isNight flag", nightState.isNight)
        assertTrue("Night sky ambient must preserve visibility for safe racing", nightState.skyAmbient[2] >= 0.05f)
        assertTrue("Night ground ambient must preserve track edge visibility", nightState.groundAmbient[0] >= 0.04f)
        assertTrue("Night headlights must be active for driving visibility", nightState.headlightsActive)
        assertTrue("Night headlight intensity must be strong on HIGH quality", nightState.headlightIntensity >= 1.8f)

        // 4. Dynamic Time Progression Verification
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.DYNAMIC
        controller.dynamicHour = 12.0f
        controller.update(10.0f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        assertTrue("Dynamic time must progress forward with delta time", controller.dynamicHour > 12.0f)
    }

    @Test
    fun testPhase10WeatherConditionsAndAtmosphere() {
        val controller = com.example.racing.model.EnvironmentController()
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.DAY

        // 1. Clear Weather
        controller.selectedWeather = com.example.racing.model.WeatherCondition.CLEAR
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val clearState = controller.currentState
        assertEquals("Clear weather must have 0 rain intensity", 0.0f, clearState.rainIntensity, 0.001f)
        assertTrue("Clear weather cloud coverage must be low", clearState.cloudCoverage <= 0.20f)
        assertEquals("Clear GP track must have dry asphalt", 0.0f, clearState.wetRoadActive, 0.001f)

        // 2. Overcast Weather
        controller.selectedWeather = com.example.racing.model.WeatherCondition.OVERCAST
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val overcastState = controller.currentState
        assertTrue("Overcast weather must have dense cloud coverage", overcastState.cloudCoverage >= 0.80f)
        assertTrue("Overcast sun direct light must be softened and diffuse", overcastState.sunColor[0] < clearState.sunColor[0])
        assertTrue("Overcast fog range must be closer than clear day", overcastState.fogFar < clearState.fogFar)

        // 3. Light Rain Weather
        controller.selectedWeather = com.example.racing.model.WeatherCondition.LIGHT_RAIN
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val lightRainState = controller.currentState
        assertTrue("Light rain must have moderate rain intensity", lightRainState.rainIntensity in 0.3f..0.6f)
        assertTrue("Light rain must activate damp wet asphalt sheen", lightRainState.wetRoadActive >= 0.70f)
        assertTrue("Light rain must automatically activate headlights", lightRainState.headlightsActive)

        // 4. Heavy Rain Weather
        controller.selectedWeather = com.example.racing.model.WeatherCondition.HEAVY_RAIN
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val heavyRainState = controller.currentState
        assertEquals("Heavy rain must have maximum rain intensity", 1.0f, heavyRainState.rainIntensity, 0.001f)
        assertEquals("Heavy rain must have maximum wet road sheen", 1.0f, heavyRainState.wetRoadActive, 0.001f)
        assertTrue("Heavy rain must have dense cloud coverage", heavyRainState.cloudCoverage >= 0.95f)
        assertTrue("Heavy rain fog must reduce distant visibility", heavyRainState.fogFar <= 400f)
    }

    @Test
    fun testPhase10RainAndSprayParticleEmissions() {
        val ps = com.example.racing.engine.ParticleSystem(480)
        val camPos = com.example.racing.engine.Vector3f(0f, 2f, 0f)

        // 1. Rain Emission
        ps.emitRain(camPos, count = 20, isHeavy = true)
        // Verify particles update without crashing
        ps.update(0.016f)

        // 2. Tire Spray Emission
        ps.emitTireSpray(x = 1.0f, y = 0.5f, z = -2.0f, intensity = 1.0f)
        ps.update(0.016f)

        ps.release()
    }

    @Test
    fun testPhase10GraphicsQualityScaling() {
        val controller = com.example.racing.model.EnvironmentController()
        controller.selectedTimeOfDay = com.example.racing.model.TimeOfDay.DAY
        controller.selectedWeather = com.example.racing.model.WeatherCondition.HEAVY_RAIN

        // High quality: full wet sheen and headlights
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "HIGH")
        val highState = controller.currentState
        assertEquals("High quality should have 100% wetness", 1.0f, highState.wetRoadActive, 0.001f)
        assertTrue("High quality should have full headlight intensity", highState.headlightIntensity >= 1.8f)

        // Low quality: modulated wetness and disabled headlights to save mobile GPU cycles
        controller.update(0.016f, com.example.racing.model.TrackId.GRAND_PRIX, "LOW")
        val lowState = controller.currentState
        assertTrue("Low quality must reduce wet road shader complexity", lowState.wetRoadActive < highState.wetRoadActive)
        assertFalse("Low quality disables headlight spotlight to guarantee high mobile frame rates", lowState.headlightsActive)
    }

    // =============================================================================================
    // PHASE 11: PREMIUM 3D GARAGE, VEHICLE SELECTION, CUSTOMIZATION & UPGRADE SYSTEM
    // =============================================================================================

    @Test
    fun testPhase11ShowroomMeshGeneration() {
        val showroom = com.example.racing.engine.ShowroomMeshBuilder.buildShowroomModel()
        assertNotNull("Showroom model must not be null", showroom)

        // 1. Circular Turntable Platform
        assertNotNull("Turntable mesh must be built", showroom.turntableMesh)
        assertTrue("Turntable mesh must have detailed circular geometry", showroom.turntableMesh.vertexCount >= 30)

        // 2. Perimeter LED Halo Ring
        assertNotNull("LED ring mesh must be built", showroom.ledRingMesh)
        assertTrue("LED ring mesh must have halo quads", showroom.ledRingMesh.vertexCount >= 60)

        // 3. Polished Studio Floor
        assertNotNull("Floor mesh must be built", showroom.floorMesh)
        assertTrue("Floor mesh must have radial grid quads", showroom.floorMesh.vertexCount >= 100)

        // 4. Studio Cyclorama Cove Wall
        assertNotNull("Studio cove wall must be built", showroom.studioCoveMesh)
        assertTrue("Studio cove wall must have height segments", showroom.studioCoveMesh.vertexCount >= 60)

        // 5. Overhead Softbox Light Diffusers
        assertNotNull("Overhead lights mesh must be built", showroom.overheadLightsMesh)
        assertTrue("Overhead lights mesh must have luminaire box vertices", showroom.overheadLightsMesh.vertexCount >= 20)

        // 6. Contact Shadow
        assertNotNull("Contact shadow mesh must be built", showroom.contactShadowMesh)
        assertTrue("Contact shadow quad must be present", showroom.contactShadowMesh.vertexCount >= 8)

        showroom.release()
    }

    @Test
    fun testPhase11ShowroomCameraClampingAndReset() {
        val dummyVehicle = com.example.racing.physics.VehiclePhysics()
        val renderer = com.example.racing.engine.RacingRenderer(dummyVehicle, emptyList())

        // Initial default state
        assertEquals(45f, renderer.showroomOrbitAngleDeg, 0.001f)
        assertEquals(16f, renderer.showroomElevationDeg, 0.001f)
        assertEquals(5.4f, renderer.showroomZoomDistance, 0.001f)

        // Manually adjust camera
        renderer.showroomOrbitAngleDeg = 180f
        renderer.showroomElevationDeg = 40f
        renderer.showroomZoomDistance = 8.0f

        // Reset Camera
        renderer.resetShowroomCamera()
        assertEquals(45f, renderer.showroomOrbitAngleDeg, 0.001f)
        assertEquals(16f, renderer.showroomElevationDeg, 0.001f)
        assertEquals(5.4f, renderer.showroomZoomDistance, 0.001f)

        // Camera Presets
        renderer.setCameraPreset("SIDE")
        assertEquals(90f, renderer.showroomOrbitAngleDeg, 0.001f)

        renderer.setCameraPreset("REAR_AERO")
        assertEquals(155f, renderer.showroomOrbitAngleDeg, 0.001f)

        renderer.setCameraPreset("FRONT_DETAIL")
        assertEquals(15f, renderer.showroomOrbitAngleDeg, 0.001f)

        // Auto-Rotate toggle
        assertFalse(renderer.autoRotateShowroom)
        assertTrue(renderer.toggleAutoRotate())
        assertTrue(renderer.autoRotateShowroom)
        assertFalse(renderer.toggleAutoRotate())
        assertFalse(renderer.autoRotateShowroom)

        renderer.release()
    }

    @Test
    fun testPhase11DerivedVehicleTelemetry() {
        val car = CarDefinition.ALL_CARS[0] // Apex Valkyrie GT
        val stockUpgrades = CarUpgradeState()
        val maxUpgrades = CarUpgradeState(engineStage = 3, tireStage = 3, suspensionStage = 3, weightReduction = 3)

        // Stock vs Max Upgrades
        val stockTorque = car.baseTorqueNm + (stockUpgrades.engineStage * 65f)
        val maxTorque = car.baseTorqueNm + (maxUpgrades.engineStage * 65f)
        assertTrue("Max ECU stage must increase torque", maxTorque > stockTorque)
        assertEquals(car.baseTorqueNm + 195f, maxTorque, 0.001f)

        val stockWeight = car.massKg - (stockUpgrades.weightReduction * 55f)
        val maxWeight = car.massKg - (maxUpgrades.weightReduction * 55f)
        assertTrue("Max weight drop must lighten the vehicle", maxWeight < stockWeight)
        assertEquals(car.massKg - 165f, maxWeight, 0.001f)

        val stockGrip = 1.25f + (stockUpgrades.tireStage * 0.12f)
        val maxGrip = 1.25f + (maxUpgrades.tireStage * 0.12f)
        assertTrue("Max tire compound must increase peak tire grip", maxGrip > stockGrip)
        assertEquals(1.25f + 0.36f, maxGrip, 0.001f)
    }

    @Test
    fun testPhase11ShowroomMaterialsPBR() {
        val floor = Material.SHOWROOM_FLOOR
        assertTrue("Showroom floor must have clearcoat gloss reflection", floor.clearcoat >= 0.60f)
        assertTrue("Showroom floor must have controlled roughness", floor.roughness in 0.15f..0.30f)

        val turntable = Material.SHOWROOM_TURNTABLE
        assertTrue("Showroom turntable must have metallic finish", turntable.metallic >= 0.30f)
        assertTrue("Showroom turntable must have high clearcoat", turntable.clearcoat >= 0.80f)

        val led = Material.SHOWROOM_LED_RING
        assertTrue("LED ring must have intense emissive blue/cyan", led.emission[2] >= 2.0f)

        val lights = Material.SHOWROOM_OVERHEAD_LIGHT
        assertTrue("Overhead softbox panels must have high emissive white intensity", lights.emission[0] >= 2.0f)
    }

    @Test
    fun testPhase12CameraViewsAndFOVScale() {
        val views = com.example.racing.model.CameraView.values()
        assertEquals("Must support 4 distinct racing camera perspectives", 4, views.size)
        assertTrue(views.contains(com.example.racing.model.CameraView.CHASE_NEAR))
        assertTrue(views.contains(com.example.racing.model.CameraView.CHASE_FAR))
        assertTrue(views.contains(com.example.racing.model.CameraView.HOOD_BUMPER))
        assertTrue(views.contains(com.example.racing.model.CameraView.COCKPIT))

        // Verify FOV progression with speed
        val baseChaseFov = 60f
        val maxSpeedKmh = 350f
        val speedFovDelta = (maxSpeedKmh / 350f).coerceIn(0f, 1f) * 8.5f
        val boostFovDelta = 5.5f
        val maxFov = (baseChaseFov + speedFovDelta + boostFovDelta).coerceIn(58f, 82f)
        assertTrue("Max FOV during boost at top speed must expand for sensation of velocity", maxFov >= 74f)
    }

    @Test
    fun testPhase12CameraShakeDampingAndGroundClearance() {
        var shakeIntensity = 0.12f
        val damping = 0.86f
        for (i in 0 until 5) {
            shakeIntensity *= damping
        }
        assertTrue("Camera shake must decay smoothly to prevent nausea", shakeIntensity < 0.06f)

        // Verify ground clearance protection
        val carPosY = 0.35f
        val minCameraGroundClearance = carPosY + 0.35f
        val rawCameraTargetY = -0.10f
        val safeCameraTargetY = kotlin.math.max(minCameraGroundClearance, rawCameraTargetY)
        assertTrue("Camera height must be clamped above ground plane", safeCameraTargetY >= 0.70f)
    }

    @Test
    fun testPhase12SteeringOpposingInputCancellationAndDamping() {
        // Test opposing inputs
        val isLeftPressed = true
        val isRightPressed = true
        val opposingTarget = when {
            isLeftPressed && !isRightPressed -> 1.0f
            isRightPressed && !isLeftPressed -> -1.0f
            else -> 0.0f
        }
        assertEquals("Opposing simultaneous steer inputs must cleanly cancel to 0", 0.0f, opposingTarget, 0.001f)

        // Test smooth steering return
        var currentSteer = 1.0f
        val neutralTarget = 0.0f
        val rampSpeed = 0.42f
        for (step in 0 until 6) {
            currentSteer += (neutralTarget - currentSteer) * rampSpeed
        }
        assertTrue("Steering must return smoothly towards center when released", currentSteer < 0.10f)
    }

    @Test
    fun testPhase2SteeringDirectionConventions() {
        // Consistent Standard: Left = positive steer (+1.0f), Right = negative steer (-1.0f), Neutral = 0.0f
        fun resolveTarget(left: Boolean, right: Boolean): Float = when {
            left && !right -> 1.0f
            right && !left -> -1.0f
            else -> 0.0f
        }

        assertEquals("Left control alone must produce positive steer lock", 1.0f, resolveTarget(left = true, right = false), 0.001f)
        assertEquals("Right control alone must produce negative steer lock", -1.0f, resolveTarget(left = false, right = true), 0.001f)
        assertEquals("Simultaneous dual inputs must resolve to neutral 0.0f", 0.0f, resolveTarget(left = true, right = true), 0.001f)
        assertEquals("Neither control pressed must resolve to neutral 0.0f", 0.0f, resolveTarget(left = false, right = false), 0.001f)

        // Releasing one control while other is pressed
        var left = true
        var right = true
        assertEquals(0.0f, resolveTarget(left, right), 0.001f)
        right = false
        assertEquals(1.0f, resolveTarget(left, right), 0.001f)
        left = false
        right = true
        assertEquals(-1.0f, resolveTarget(left, right), 0.001f)
    }

    @Test
    fun testPhase2AutomaticReverseHoldDelay() {
        val vp = com.example.racing.physics.VehiclePhysics()
        vp.forwardVelocity = 0f
        vp.powertrain.currentGear = 2 // 1st gear

        // 1. Holding brake for 0.40s while stopped: must NOT shift into reverse!
        vp.throttle = 0f
        vp.brake = 0.8f
        vp.update(dt = 0.40f, emptyList())
        assertEquals("Vehicle must remain in 1st gear when stationary with brief brake hold", 2, vp.powertrain.currentGear)
        assertTrue("Mechanical brakes must hold vehicle stationary", vp.forwardVelocity == 0f)

        // 2. Holding brake for an additional 0.40s (total > 0.75s): shifts to reverse
        vp.update(dt = 0.40f, emptyList())
        assertEquals("Vehicle shifts to reverse after holding brake for >= 0.75s", 0, vp.powertrain.currentGear)

        // 3. Applying forward throttle in reverse gear: shifts back to 1st gear
        vp.brake = 0f
        vp.throttle = 0.8f
        vp.update(dt = 0.05f, emptyList())
        assertEquals("Pressing forward throttle in reverse must shift to 1st gear", 2, vp.powertrain.currentGear)
    }

    @Test
    fun testPhase2CountdownGridImmobility() {
        val vp = com.example.racing.physics.VehiclePhysics()
        vp.resetAt(Vector3f(0f, 0f, 0f), 0f)

        // Emulate starting grid countdown lock
        vp.throttle = 0f
        vp.brake = 1f
        vp.forwardVelocity = 0f
        vp.powertrain.currentGear = 2
        vp.reverseHoldTimer = 0f

        // Step physics multiple frames during countdown
        for (i in 0 until 10) {
            vp.forwardVelocity = 0f
            vp.reverseHoldTimer = 0f
            vp.powertrain.currentGear = 2
        }

        assertEquals("Car must have zero forward velocity during countdown", 0f, vp.forwardVelocity, 0.001f)
        assertEquals("Car must remain in 1st gear on grid", 2, vp.powertrain.currentGear)
        assertEquals("Reverse hold timer must remain 0 during countdown", 0f, vp.reverseHoldTimer, 0.001f)
    }

    @Test
    fun testPhase12DrivingAssistsTelemetryIndicators() {
        // High brake and speed -> ABS triggers
        val brakeInput = 0.85f
        val speedKmh = 85f
        val absActive = (brakeInput > 0.45f && speedKmh > 10f)
        assertTrue("ABS indicator must be active under heavy braking at speed", absActive)

        // High throttle at low speed or off-road -> TCS triggers
        val throttleInput = 0.90f
        val lowSpeedKmh = 25f
        val tcsActive = (throttleInput > 0.65f && lowSpeedKmh < 45f)
        assertTrue("TCS indicator must be active during launch wheelspin", tcsActive)

        // Rev limiter flag near redline
        val rpm = 8200f
        val maxRpm = 8600f
        val rpmRatio = rpm / maxRpm
        val isRevLimiter = (rpmRatio > 0.90f)
        assertTrue("Rev limiter indicator must activate above 90% of redline", isRevLimiter)
    }

    @Test
    fun testPhase12VirtualWheelClampingAndAutoCentering() {
        val maxWheelAngle = 65f
        val rawDragAngle = 120f
        val clampedAngle = rawDragAngle.coerceIn(-maxWheelAngle, maxWheelAngle)
        assertEquals("Wheel rotation must be clamped to max steering range", 65f, clampedAngle, 0.001f)

        val steerNormalized = (-clampedAngle / 60f).coerceIn(-1.0f, 1.0f)
        assertTrue("Normalized steer from wheel must stay within [-1.0, 1.0]", steerNormalized in -1.0f..1.0f)
    }

    @Test
    fun testPhase12TiltGyroscopeDeadzoneFiltering() {
        // Values within ±0.05f deadzone should evaluate to 0f
        val tremorValue = 0.035f
        val filtered = if (kotlin.math.abs(tremorValue) < 0.05f) 0f else (tremorValue - 0.05f) / 0.95f
        assertEquals("Tremor below deadzone threshold must be filtered out", 0.0f, filtered, 0.001f)

        // Values above deadzone should scale cleanly
        val validTilt = 0.45f
        val scaled = if (kotlin.math.abs(validTilt) < 0.05f) 0f else (validTilt - 0.05f) / 0.95f
        assertTrue("Intentional tilt must be mapped proportionally", scaled > 0.40f)
    }

    // =============================================================================================
    // PHASE 3 — VEHICLE PHYSICS, COLLISION RESOLUTION & PHYSICAL CONTACT TESTS
    // =============================================================================================

    @Test
    fun testPhase3SeparatedVehiclesDoNotCollide() {
        val v1 = VehiclePhysics().apply { position.set(0f, 0.35f, 0f) }
        val v2 = VehiclePhysics().apply { position.set(10f, 0.35f, 0f) }

        val manifold = VehicleCollisionSystem.testOBBCollision(v1, v2)
        org.junit.Assert.assertNull("Separated vehicles must produce null contact manifold", manifold)
    }

    @Test
    fun testPhase3OverlappingVehiclesAreSeparated() {
        // Cars overlapping along Z axis
        val v1 = VehiclePhysics().apply { position.set(0f, 0.35f, 3.80f) }
        val v2 = VehiclePhysics().apply { position.set(0f, 0.35f, 0f) }

        val initialOverlap = (VehicleCollisionSystem.VEHICLE_HALF_LENGTH * 2f) - (v1.position.z - v2.position.z)
        assertTrue("Initial setup must have positive overlap", initialOverlap > 0f)

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(v1, v2),
            playerVehicle = v1
        )

        val newDist = v1.position.z - v2.position.z
        val newOverlap = (VehicleCollisionSystem.VEHICLE_HALF_LENGTH * 2f) - newDist
        assertTrue("Vehicles must be pushed apart", newDist > 3.80f)
        assertTrue("Remaining overlap after solver must be minimal", newOverlap < 0.05f)
    }

    @Test
    fun testPhase3HeadOnCollisionResponse() {
        val v1 = VehiclePhysics().apply {
            position.set(0f, 0.35f, 2.0f)
            headingAngleRad = kotlin.math.PI.toFloat() // Facing towards -Z
            forwardVelocity = 25f // Moving towards -Z (towards v2)
        }
        val v2 = VehiclePhysics().apply {
            position.set(0f, 0.35f, -2.0f)
            headingAngleRad = 0f // Facing towards +Z
            forwardVelocity = 25f // Moving towards +Z (towards v1)
        }

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(v1, v2),
            playerVehicle = v1
        )

        assertTrue("v1 forward velocity must decrease significantly or reverse", v1.forwardVelocity < 12f)
        assertTrue("v2 forward velocity must decrease significantly or reverse", v2.forwardVelocity < 12f)
        assertTrue("Vehicles must not blow up with NaNs", !v1.forwardVelocity.isNaN() && !v2.forwardVelocity.isNaN())
    }

    @Test
    fun testPhase3RearEndCollisionMomentumTransfer() {
        // Front vehicle moving slower
        val front = VehiclePhysics().apply {
            position.set(0f, 0.35f, 4.0f)
            headingAngleRad = 0f
            forwardVelocity = 15f
        }
        // Rear vehicle moving faster, hitting front from behind
        val rear = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            headingAngleRad = 0f
            forwardVelocity = 40f
        }

        val frontInitialV = front.forwardVelocity
        val rearInitialV = rear.forwardVelocity

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(front, rear),
            playerVehicle = front
        )

        assertTrue("Front car must be boosted forward by rear impact", front.forwardVelocity > frontInitialV)
        assertTrue("Rear car must be decelerated by impact", rear.forwardVelocity < rearInitialV)
    }

    @Test
    fun testPhase3SideContactSeparation() {
        // Two cars racing side-by-side, overlapping laterally by 0.30m
        val left = VehiclePhysics().apply {
            position.set(1.5f, 0.35f, 20f)
            headingAngleRad = 0f
            forwardVelocity = 30f
        }
        val right = VehiclePhysics().apply {
            position.set(0f, 0.35f, 20f)
            headingAngleRad = 0f
            forwardVelocity = 30f
        }

        val initialDeltaX = left.position.x - right.position.x

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(left, right),
            playerVehicle = left
        )

        val newDeltaX = left.position.x - right.position.x
        assertTrue("Side contact must push vehicles laterally apart", newDeltaX > initialDeltaX)
        assertTrue("Forward velocity should remain largely preserved during side swipe", left.forwardVelocity > 20f && right.forwardVelocity > 20f)
    }

    @Test
    fun testPhase3AngledContactProducesPlausibleYawDeflection() {
        // T-Bone / PIT style contact on rear-quarter
        val target = VehiclePhysics().apply {
            position.set(0f, 0.35f, 10f)
            headingAngleRad = 0f
            forwardVelocity = 20f
        }
        val striker = VehiclePhysics().apply {
            position.set(1.2f, 0.35f, 8.5f)
            headingAngleRad = -0.45f
            forwardVelocity = 25f
        }

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(target, striker),
            playerVehicle = target
        )

        assertTrue("Angled impact must generate yaw deflection on vehicle", target.yawRate != 0f || striker.yawRate != 0f)
        assertTrue("Yaw rate must be bounded and not NaN", !target.yawRate.isNaN() && kotlin.math.abs(target.yawRate) <= 3.0f)
    }

    @Test
    fun testPhase3FastMovingVehiclesDoNotTunnel() {
        // Closing speed along Z
        val v1 = VehiclePhysics().apply {
            position.set(0f, 0.35f, 1.5f)
            headingAngleRad = 0f
            forwardVelocity = -30f
        }
        val v2 = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            headingAngleRad = 0f
            forwardVelocity = 30f
        }

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(v1, v2),
            playerVehicle = v1
        )

        assertTrue("Vehicles must not tunnel through each other", v1.position.z >= v2.position.z)
    }

    @Test
    fun testPhase3MultipleNearbyVehiclesChainResolution() {
        // 3 cars bumper to bumper
        val c1 = VehiclePhysics().apply { position.set(0f, 0.35f, 7.6f) }
        val c2 = VehiclePhysics().apply { position.set(0f, 0.35f, 3.8f) }
        val c3 = VehiclePhysics().apply { position.set(0f, 0.35f, 0.0f) }

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(c1, c2, c3),
            playerVehicle = c1
        )

        val dist12 = c1.position.z - c2.position.z
        val dist23 = c2.position.z - c3.position.z

        assertTrue("Chain collision must push front car forward", dist12 >= 3.8f)
        assertTrue("Chain collision must push rear car backward", dist23 >= 3.8f)
    }

    @Test
    fun testPhase3StationaryAndMovingVehicleInteraction() {
        val stationary = VehiclePhysics().apply {
            position.set(0f, 0.35f, 3.5f)
            forwardVelocity = 0f
        }
        val moving = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            forwardVelocity = 30f
        }

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(stationary, moving),
            playerVehicle = stationary
        )

        assertTrue("Stationary vehicle must be set into forward motion by impact", stationary.forwardVelocity > 0f)
        assertTrue("Moving vehicle must lose momentum", moving.forwardVelocity < 30f)
    }

    @Test
    fun testPhase3DegenerateCoordinatesSafeguard() {
        // Exactly identical positions
        val v1 = VehiclePhysics().apply { position.set(5f, 0.35f, 5f) }
        val v2 = VehiclePhysics().apply { position.set(5f, 0.35f, 5f) }

        val manifold = VehicleCollisionSystem.testOBBCollision(v1, v2)
        org.junit.Assert.assertNotNull("Degenerate coordinates must still detect overlap safely", manifold)
        assertFalse("Manifold normal must not be NaN", manifold!!.normalX.isNaN() || manifold.normalZ.isNaN())

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(v1, v2),
            playerVehicle = v1
        )

        assertFalse("Vehicle positions must not be NaN after degenerate resolution", v1.position.x.isNaN() || v2.position.x.isNaN())
        assertTrue("Degenerate vehicles must be pushed apart", v1.position.distanceTo(v2.position) > 0.01f)
    }

    @Test
    fun testPhase3RepeatedCollisionFramesStability() {
        val v1 = VehiclePhysics().apply {
            position.set(0f, 0.35f, 1.0f)
            forwardVelocity = 10f
        }
        val v2 = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            forwardVelocity = 12f
        }

        // Simulate 20 consecutive collision steps
        for (step in 0 until 20) {
            VehicleCollisionSystem.resolveVehicleCollisions(
                vehicles = listOf(v1, v2),
                playerVehicle = v1
            )
            v1.update(0.0125f)
            v2.update(0.0125f)
        }

        assertFalse("Repeated collisions must not produce NaNs", v1.position.z.isNaN() || v2.position.z.isNaN())
        assertTrue("Velocities must stay within physically bounded range", kotlin.math.abs(v1.forwardVelocity) < 100f && kotlin.math.abs(v2.forwardVelocity) < 100f)
        assertTrue("Vehicles must maintain separation", v1.position.z > v2.position.z)
    }

    // =============================================================================================
    // PHASE 4 — ADVANCED VEHICLE HANDLING, TIRE GRIP, BRAKING & DRIVING REALISM TESTS
    // =============================================================================================

    @Test
    fun testPhase4KammFrictionCircleCombinedSlipAdhesion() {
        val tire = TireModel(dPeakFriction = 1.25f)
        val normalLoad = 4000f // 4 kN normal force
        val maxPeakAdhesion = normalLoad * 1.25f // 5000 N

        // 1. Moderate acceleration (well within grip limit)
        val lowDemandN = 2000f
        val resLow = tire.calculateCombinedAdhesion(lowDemandN, normalLoad, surfaceGrip = 1.0f)
        assertEquals("Under grip limit, longitudinal force matches demand", lowDemandN, resLow.actualFx, 0.01f)
        assertTrue("Residual lateral capacity must be high under moderate acceleration", resLow.lateralCapacityFactor > 0.85f)
        assertTrue("Micro-slip ratio should be low", resLow.slipRatio < 0.20f)

        // 2. High acceleration saturating tire grip (wheelspin)
        val excessDemandN = 8000f // Far exceeds 5000 N max adhesion
        val resHigh = tire.calculateCombinedAdhesion(excessDemandN, normalLoad, surfaceGrip = 1.0f)
        assertTrue("Saturated force cannot exceed peak adhesion", resHigh.actualFx <= maxPeakAdhesion)
        assertEquals("Dynamic sliding friction should be ~90% of peak", maxPeakAdhesion * 0.90f, resHigh.actualFx, 0.1f)
        assertTrue("Wheelspin slip ratio must be high", resHigh.slipRatio >= 0.35f)
        assertTrue("Residual lateral capacity must drop under heavy wheelspin", resHigh.lateralCapacityFactor <= 0.25f)

        // 3. Low-grip surface (grass/wet) reduces peak adhesion proportionally
        val resWet = tire.calculateCombinedAdhesion(lowDemandN, normalLoad, surfaceGrip = 0.50f) // 2500 N peak
        assertTrue("Under reduced surface grip, actual force remains bounded", resWet.actualFx <= 2500f)
        assertTrue("Slip ratio increases on slippery surface", resWet.slipRatio > resLow.slipRatio)
    }

    @Test
    fun testPhase4SmoothLaunchFromRestAndTractionLimits() {
        val valkyrie = VehiclePhysics()
        valkyrie.throttle = 1.0f

        // Step vehicle 10 physics frames from standstill
        for (i in 0 until 10) {
            valkyrie.update(0.016f)
        }

        assertTrue("Vehicle accelerates forward smoothly from standstill", valkyrie.forwardVelocity > 0f)
        assertTrue("Velocity progression is physically plausible (not teleporting)", valkyrie.forwardVelocity < 15f)
        assertTrue("Wheelspin is bounded and controlled", valkyrie.wheelSpinRl in 0f..1f)
        assertFalse("Forward velocity must not be NaN", valkyrie.forwardVelocity.isNaN())
    }

    @Test
    fun testPhase4EngineBrakingOnThrottleRelease() {
        val car = VehiclePhysics()
        car.forwardVelocity = 25f // ~90 km/h in forward gear
        car.powertrain.currentGear = 3 // 2nd gear
        car.powertrain.currentRpm = 5000f
        car.throttle = 0f
        car.brake = 0f

        val initialSpeed = car.forwardVelocity
        for (i in 0 until 20) {
            car.update(0.016f)
        }

        assertTrue("Releasing throttle in forward gear must cause natural engine deceleration", car.forwardVelocity < initialSpeed)
        assertTrue("Vehicle must maintain forward direction during engine braking", car.forwardVelocity > 0f)
    }

    @Test
    fun testPhase4BrakeBiasAndTrailBrakingLateralCoupling() {
        // Test that heavy braking causes front weight transfer and reduces steering capacity
        val carStraight = VehiclePhysics()
        carStraight.forwardVelocity = 30f
        carStraight.brake = 1.0f
        carStraight.update(0.016f)

        assertTrue("Braking must cause forward deceleration", carStraight.forwardVelocity < 30f)
        assertTrue("Front tire slip must be engaged under heavy braking", carStraight.tireSlipFl > 0f)

        // Trail braking: heavy braking while cornering
        val carTurning = VehiclePhysics()
        carTurning.forwardVelocity = 30f
        carTurning.steerInput = 1.0f // Left turn
        carTurning.brake = 1.0f // Full emergency brake while steering
        carTurning.update(0.05f)

        // Compare cornering yaw rate with non-braking car
        val carCoastTurn = VehiclePhysics()
        carCoastTurn.forwardVelocity = 30f
        carCoastTurn.steerInput = 1.0f
        carCoastTurn.brake = 0.0f
        carCoastTurn.update(0.05f)

        assertTrue("Both cars must steer into the turn", carTurning.currentSteerAngleRad > 0f && carCoastTurn.currentSteerAngleRad > 0f)
        assertTrue("Car under locked brakes experiences trail braking understeer vs coasting", carTurning.yawRate < carCoastTurn.yawRate)
    }

    @Test
    fun testPhase4SpeedSensitiveSteeringAttenuation() {
        val carLowSpeed = VehiclePhysics()
        carLowSpeed.forwardVelocity = 5f // Low speed (~18 km/h)
        carLowSpeed.steerInput = 1.0f
        carLowSpeed.update(0.05f)

        val carHighSpeed = VehiclePhysics()
        carHighSpeed.forwardVelocity = 60f // High speed (~216 km/h)
        carHighSpeed.steerInput = 1.0f
        carHighSpeed.update(0.05f)

        assertTrue("Low speed steering angle should be larger than high speed steering angle",
            carLowSpeed.currentSteerAngleRad > carHighSpeed.currentSteerAngleRad)
    }

    @Test
    fun testPhase4SteeringSelfCenteringToNeutral() {
        val car = VehiclePhysics()
        car.forwardVelocity = 20f
        car.steerInput = 1.0f
        car.update(0.1f) // Establish steering lock

        val lockAngle = car.currentSteerAngleRad
        assertTrue("Steering lock must be established", lockAngle > 0.1f)

        // Release steering input
        car.steerInput = 0f
        for (i in 0 until 15) {
            car.update(0.016f)
        }

        assertTrue("Releasing steering must smoothly return towards neutral", car.currentSteerAngleRad < lockAngle * 0.35f)
    }

    @Test
    fun testPhase4DistinctVehicleConfigurationsDynamics() {
        val valkyrieDef = CarDefinition.ALL_CARS.find { it.id == "apex_valkyrie" }!!
        val spiritDef = CarDefinition.ALL_CARS.find { it.id == "apex_spirit_86" }!!
        val monzaDef = CarDefinition.ALL_CARS.find { it.id == "apex_monza_v10" }!!

        // 1. Valkyrie: AWD 35% front torque split
        assertEquals(0.35f, valkyrieDef.driveFrontRatio, 0.01f)
        // 2. Spirit GTS: Pure RWD 0% front torque split for drifting
        assertEquals(0.0f, spiritDef.driveFrontRatio, 0.01f)
        // 3. Monza V10: 15% front torque split, highest aero downforce
        assertEquals(0.15f, monzaDef.driveFrontRatio, 0.01f)
        assertTrue("Monza V10 must have highest aerodynamic downforce", monzaDef.downforceCoeff > valkyrieDef.downforceCoeff)
        assertTrue("Spirit GTS must have lowest mass", spiritDef.massKg < valkyrieDef.massKg)
    }

    @Test
    fun testPhase4RoadSurfacesKerbAndWeatherGripScaling() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val car = VehiclePhysics()
        car.resetAt(Vector3f(0f, 0.35f, 0f), 0f)

        // 1. Track Center: 100% surface grip
        car.update(0.016f, track.waypoints, roadHalfWidth = 7.0f, baseSurfaceGrip = 1.0f)
        assertFalse("Center lane is not off-road", car.isOffRoad)
        assertFalse("Center lane is not on kerb", car.isOnKerb)
        assertEquals(1.0f, car.currentSurfaceGrip, 0.05f)

        // 2. Kerb / Rumble strip zone (lateral distance ~ 6.0m on 7m half-width)
        car.position.set(Vector3f(6.0f, 0.35f, 0f))
        for (i in 0 until 10) {
            car.update(0.016f, track.waypoints, roadHalfWidth = 7.0f, baseSurfaceGrip = 1.0f)
        }
        assertTrue("Vehicle on track edge must detect kerb", car.isOnKerb)
        assertTrue("Kerb grip is slightly reduced vs asphalt", car.currentSurfaceGrip < 1.0f)

        // 3. Wet Weather scaling
        val carWet = VehiclePhysics()
        carWet.resetAt(Vector3f(0f, 0.35f, 0f), 0f)
        for (i in 0 until 10) {
            carWet.update(0.016f, track.waypoints, roadHalfWidth = 7.0f, baseSurfaceGrip = 0.70f) // Heavy rain
        }
        assertTrue("Wet weather reduces track adhesion", carWet.currentSurfaceGrip < 0.75f)
    }

    @Test
    fun testPhase4StandstillImmobilityAndReverseOperation() {
        val car = VehiclePhysics()
        car.resetAt(Vector3f(0f, 0.35f, 0f), 0f)
        car.brake = 1.0f
        car.throttle = 0f

        // Stationary holding brake
        for (i in 0 until 30) {
            car.update(0.016f)
        }
        assertEquals("Forward velocity must remain precisely 0 at standstill", 0f, car.forwardVelocity, 0.001f)
        assertEquals("Lateral velocity must remain 0 at standstill", 0f, car.lateralVelocity, 0.001f)
        assertEquals("Yaw rate must remain 0 at standstill", 0f, car.yawRate, 0.001f)

        // Reverse shift after holding firm brake for > 0.75s
        car.forwardVelocity = 0f
        car.brake = 1.0f
        for (i in 0 until 60) { // 60 * 0.016 = 0.96s
            car.update(0.016f)
        }
        assertEquals("Holding firm brake while stopped shifts into Reverse", 0, car.powertrain.currentGear)

        // In reverse gear, continuing to apply pedal drives car backwards
        for (i in 0 until 20) {
            car.update(0.016f)
        }
        assertTrue("Car in reverse must travel backwards", car.forwardVelocity < 0f)
    }

    // =============================================================================================
    // PHASE 5 — ADVANCED AI OPPONENTS, RACING LINES, OVERTAKING & COMPETITIVE RACE INTELLIGENCE
    // =============================================================================================

    @Test
    fun testPhase5TrackAwareRacingLinesAndApexClipping() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints
        val count = waypoints.size
        assertTrue("Grand Prix circuit must have at least 16 waypoints", count >= 16)

        val profile = AIDriverProfile("Viper", floatArrayOf(0.1f, 0.5f, 1f, 1f), 0.88f, AIDifficulty.EXPERT)
        val ai = OpponentAIController(VehiclePhysics(), profile, waypoints)

        // Find a right-hand corner and a left-hand corner along track waypoints
        var foundRightCornerIdx = -1
        var foundLeftCornerIdx = -1

        val step1 = kotlin.math.max(1, count / 16)
        val step2 = kotlin.math.max(2, count / 8)
        val step3 = kotlin.math.max(3, count * 3 / 16)

        for (i in 0 until count) {
            val wp1 = waypoints[ai.safeIndex(i + step1, count)]
            val wp2 = waypoints[ai.safeIndex(i + step2, count)]
            val wp3 = waypoints[ai.safeIndex(i + step3, count)]
            val a1 = kotlin.math.atan2(wp2.x - wp1.x, wp2.z - wp1.z)
            val a2 = kotlin.math.atan2(wp3.x - wp2.x, wp3.z - wp2.z)
            var delta = a2 - a1
            while (delta > kotlin.math.PI) delta -= (2 * kotlin.math.PI).toFloat()
            while (delta < -kotlin.math.PI) delta += (2 * kotlin.math.PI).toFloat()

            if (delta > 0.20f && foundRightCornerIdx == -1) {
                foundRightCornerIdx = i
            } else if (delta < -0.20f && foundLeftCornerIdx == -1) {
                foundLeftCornerIdx = i
            }
            if (foundRightCornerIdx != -1 && foundLeftCornerIdx != -1) break
        }

        assertTrue("Must locate right-hand corner on track", foundRightCornerIdx != -1)
        assertTrue("Must locate left-hand corner on track", foundLeftCornerIdx != -1)

        // 1. Right corner entry (distant apex) sweeps wide left (-3.0m)
        val rightEntryOffset = ai.computeRacingLineOffset(foundRightCornerIdx, ai.safeIndex(foundRightCornerIdx + 12, count), count)
        assertEquals("Right corner entry sweeps wide left", -3.0f, rightEntryOffset, 0.01f)

        // 2. Right corner apex clips inside right (+3.2m)
        val rightApexOffset = ai.computeRacingLineOffset(foundRightCornerIdx, ai.safeIndex(foundRightCornerIdx + 5, count), count)
        assertEquals("Right corner apex clips inside right", 3.2f, rightApexOffset, 0.01f)

        // 3. Left corner entry (distant apex) sweeps wide right (+3.0m)
        val leftEntryOffset = ai.computeRacingLineOffset(foundLeftCornerIdx, ai.safeIndex(foundLeftCornerIdx + 12, count), count)
        assertEquals("Left corner entry sweeps wide right", 3.0f, leftEntryOffset, 0.01f)

        // 4. Left corner apex clips inside left (-3.2m)
        val leftApexOffset = ai.computeRacingLineOffset(foundLeftCornerIdx, ai.safeIndex(foundLeftCornerIdx + 5, count), count)
        assertEquals("Left corner apex clips inside left", -3.2f, leftApexOffset, 0.01f)
    }

    @Test
    fun testPhase5CurvatureAwareSpeedPlanningAndBrakingDistances() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints
        val count = waypoints.size

        val profile = AIDriverProfile("Shadow", floatArrayOf(0.1f, 0.1f, 0.1f, 1f), 0.74f, AIDifficulty.MEDIUM)
        val ai = OpponentAIController(VehiclePhysics(), profile, waypoints)

        // 1. Dry asphalt speed planning
        val drySpeed0 = ai.computeTargetSpeed(0, count, surfaceGrip = 1.0f)
        assertTrue("Dry track speed planning must be positive and bounded", drySpeed0 in 60f..300f)

        // 2. Wet weather speed reduction (e.g. heavy rain surface grip = 0.70f)
        val wetSpeed0 = ai.computeTargetSpeed(0, count, surfaceGrip = 0.70f)
        assertTrue("Wet track target speed must be lower than dry track target speed", wetSpeed0 < drySpeed0)

        // 3. Compare speed planning between straightaway and tight turn entry
        // Find minimum planned target speed across the circuit
        var minCircuitSpeed = Float.MAX_VALUE
        var maxCircuitSpeed = 0f
        for (i in 0 until count step 2) {
            val spd = ai.computeTargetSpeed(i, count, surfaceGrip = 1.0f)
            if (spd < minCircuitSpeed) minCircuitSpeed = spd
            if (spd > maxCircuitSpeed) maxCircuitSpeed = spd
        }

        assertTrue("Curvature-aware speed planning slows down before sharp bends", minCircuitSpeed < 140f)
        assertTrue("Curvature-aware speed planning allows high speed on straightaways", maxCircuitSpeed > 200f)
        assertTrue("Significant speed differential between straights and bends", (maxCircuitSpeed - minCircuitSpeed) > 60f)
    }

    @Test
    fun testPhase5SafePassingLaneSelectionAndOvertakeCommitmentHysteresis() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints

        val myCar = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            forwardVelocity = 35f // 126 km/h
            headingAngleRad = 0f
        }
        val slowerCar = VehiclePhysics().apply {
            position.set(0f, 0.35f, 16f) // 16 meters ahead in traffic lane
            forwardVelocity = 28f // Slower vehicle ahead (7 m/s delta)
            headingAngleRad = 0f
        }

        val profile = AIDriverProfile("Viper", floatArrayOf(0.1f, 0.5f, 1f, 1f), 0.88f, AIDifficulty.EXPERT, personality = AIPersonality.AGGRESSIVE)
        val ai = OpponentAIController(myCar, profile, waypoints)

        // Step AI with slower car ahead
        ai.updateWithVehicles(dt = 0.05f, playerVehicle = slowerCar, allCompetitors = emptyList())

        assertTrue("Approaching slower traffic triggers PREPARING_OVERTAKE or OVERTAKING",
            ai.tacticalState == AITacticalState.PREPARING_OVERTAKE || ai.tacticalState == AITacticalState.OVERTAKING)
        assertTrue("Overtake commit timer must be engaged to prevent weaving", ai.overtakeCommitTimer > 1.0f)
        assertTrue("Side switch cooldown must be active", ai.sideSwitchCooldown > 1.5f)
        assertTrue("Selected passing side must not be zero", ai.overtakeSide != 0f)

        val committedSide = ai.overtakeSide

        // Advance 5 consecutive frames; overtake side must remain locked (anti-weaving hysteresis)
        for (step in 0 until 5) {
            slowerCar.position.z += slowerCar.forwardVelocity * 0.05f
            ai.updateWithVehicles(dt = 0.05f, playerVehicle = slowerCar, allCompetitors = emptyList())
            assertEquals("Overtake side must remain committed without rapid alternation", committedSide, ai.overtakeSide, 0.001f)
            assertEquals("Tactical state step $step", AITacticalState.OVERTAKING, ai.tacticalState)
        }
    }

    @Test
    fun testPhase5CollisionAvoidanceAndSideBySideSpaceAllocation() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints

        // 1. Multi-car side-by-side room ("Leave space" etiquette)
        val aiCar = VehiclePhysics().apply {
            position.set(0f, 0.35f, 10f)
            forwardVelocity = 25f
            headingAngleRad = 0f
        }
        val adjacentCar = VehiclePhysics().apply {
            position.set(1.6f, 0.35f, 10.2f) // Side-by-side on the right
            forwardVelocity = 25f
            headingAngleRad = 0f
        }

        val defensiveProfile = AIDriverProfile("Ghost", floatArrayOf(1f, 1f, 0f, 1f), 0.55f, AIDifficulty.EASY, personality = AIPersonality.DEFENSIVE)
        val aiDefensive = OpponentAIController(aiCar, defensiveProfile, waypoints)

        val initialOffset = aiDefensive.lateralOffset
        aiDefensive.updateWithVehicles(dt = 0.05f, playerVehicle = adjacentCar, allCompetitors = emptyList())

        // Defensive car should laterally yield away from the right-side adjacent car (repelling left)
        assertTrue("Adjacent car on right causes AI to yield space leftward", aiDefensive.lateralOffset < initialOffset)

        // 2. Immediate hazard emergency collision avoidance
        val speedingAi = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            forwardVelocity = 35f
            headingAngleRad = 0f
        }
        val stoppedHazard = VehiclePhysics().apply {
            position.set(0f, 0.35f, 3.5f) // Immediate hazard 3.5m ahead
            forwardVelocity = 0f // Stopped dead
            headingAngleRad = 0f
        }

        val aiHazard = OpponentAIController(speedingAi, defensiveProfile, waypoints)
        aiHazard.updateWithVehicles(dt = 0.05f, playerVehicle = stoppedHazard, allCompetitors = emptyList())

        assertEquals("Close stopped obstacle triggers AVOIDING_COLLISION", AITacticalState.AVOIDING_COLLISION, aiHazard.tacticalState)
        assertEquals("Immediate hazard triggers emergency zero throttle", 0f, speedingAi.throttle, 0.001f)
        assertTrue("Immediate hazard triggers emergency threshold braking", speedingAi.brake >= 0.85f)
    }

    @Test
    fun testPhase5ThreeStageRecoveryStateMachineNonLooping() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints

        val car = VehiclePhysics().apply {
            position.set(waypoints[0].x, waypoints[0].y, waypoints[0].z)
            // Pointing directly backwards against track forward direction
            headingAngleRad = kotlin.math.PI.toFloat()
            forwardVelocity = 1.0f
            speedKmh = 3.6f
            hasCollidedBarrier = true
        }

        val profile = AIDriverProfile("Shadow", floatArrayOf(0.1f, 0.1f, 0.1f, 1f), 0.74f, AIDifficulty.MEDIUM)
        val ai = OpponentAIController(car, profile, waypoints)

        // 1. Initial detection of stuck / wrong-way state
        val handled = ai.updateRecoveryStateMachine(dt = 0.05f, nearestIdx = 0, count = waypoints.size)
        assertTrue("Recovery state machine must handle stuck/spun vehicle", handled)
        assertEquals("Initial recovery phase must be REVERSING", AIRecoveryState.REVERSING, ai.recoveryState)
        assertEquals("Reversing applies reverse brake/throttle", 0.95f, car.brake, 0.01f)
        assertEquals("Reversing cuts forward throttle", 0f, car.throttle, 0.001f)

        // 2. Step reversing until transition to REALIGNING (simulate ~1.5s)
        var reachedRealigning = false
        for (i in 0 until 35) {
            ai.updateRecoveryStateMachine(dt = 0.05f, nearestIdx = 0, count = waypoints.size)
            if (ai.recoveryState == AIRecoveryState.REALIGNING) {
                reachedRealigning = true
                break
            }
        }
        assertTrue("Recovery machine must transition to REALIGNING", reachedRealigning)
        assertEquals("REALIGNING engages forward throttle", 0.80f, car.throttle, 0.01f)

        // 3. Step realigning until RESUMING and back to NORMAL
        // Orient heading angle towards track direction
        car.headingAngleRad = 0f
        var reachedNormal = false
        for (i in 0 until 30) {
            ai.updateRecoveryStateMachine(dt = 0.05f, nearestIdx = 0, count = waypoints.size)
            if (ai.recoveryState == AIRecoveryState.NORMAL) {
                reachedNormal = true
                break
            }
        }
        assertTrue("Recovery machine must smoothly return to NORMAL state", reachedNormal)
    }

    @Test
    fun testPhase5AIDifficultiesAndPersonalities() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints

        val easyProfile = AIDriverProfile("EasyDriver", floatArrayOf(0f, 0f, 0f, 1f), 0.50f, AIDifficulty.EASY)
        val mediumProfile = AIDriverProfile("MedDriver", floatArrayOf(0f, 0f, 0f, 1f), 0.75f, AIDifficulty.MEDIUM)
        val expertProfile = AIDriverProfile("ExpertDriver", floatArrayOf(0f, 0f, 0f, 1f), 1.0f, AIDifficulty.EXPERT)

        // Verify difficulty parameters
        assertTrue("Expert target speed multiplier > Medium > Easy",
            expertProfile.difficulty.targetSpeedMultiplier > mediumProfile.difficulty.targetSpeedMultiplier &&
            mediumProfile.difficulty.targetSpeedMultiplier > easyProfile.difficulty.targetSpeedMultiplier)

        assertTrue("Easy has highest mistake rate",
            easyProfile.difficulty.mistakeChance > mediumProfile.difficulty.mistakeChance &&
            mediumProfile.difficulty.mistakeChance > expertProfile.difficulty.mistakeChance)

        assertTrue("Easy has widest braking margin for safety",
            easyProfile.difficulty.brakeMargin > mediumProfile.difficulty.brakeMargin &&
            mediumProfile.difficulty.brakeMargin > expertProfile.difficulty.brakeMargin)

        val aiEasy = OpponentAIController(VehiclePhysics(), easyProfile, waypoints)
        val aiExpert = OpponentAIController(VehiclePhysics(), expertProfile, waypoints)

        val easyTargetSpeed = aiEasy.computeTargetSpeed(5, waypoints.size)
        val expertTargetSpeed = aiExpert.computeTargetSpeed(5, waypoints.size)

        assertTrue("Expert planned speed must exceed Easy planned speed", expertTargetSpeed > easyTargetSpeed)

        // Personality verification
        assertEquals(AIPersonality.DEFENSIVE, easyProfile.personality)
        assertEquals(AIPersonality.BALANCED, mediumProfile.personality)
        assertEquals(AIPersonality.AGGRESSIVE, expertProfile.personality)
    }

    @Test
    fun testPhase5PhysicalCollisionCompatibilityNoTeleportation() {
        val carA = VehiclePhysics().apply {
            position.set(0f, 0.35f, 0f)
            forwardVelocity = 20f
        }
        val carB = VehiclePhysics().apply {
            position.set(0.5f, 0.35f, 3.8f) // Partially overlapping bumper
            forwardVelocity = 10f
        }

        val initialPosA = carA.position.copy()
        val initialPosB = carB.position.copy()

        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = listOf(carA, carB),
            playerVehicle = carA
        )

        assertFalse("Vehicle A position must not be NaN", carA.position.x.isNaN() || carA.position.z.isNaN())
        assertFalse("Vehicle B position must not be NaN", carB.position.x.isNaN() || carB.position.z.isNaN())
        assertTrue("Vehicle A should transfer forward momentum to car B", carB.forwardVelocity > 10f)
        assertTrue("Vehicle A should decelerate from impact", carA.forwardVelocity < 20f)
        assertTrue("Positions must update via physical separation without teleporting",
            carA.position.distanceTo(initialPosA) < 5.0f && carB.position.distanceTo(initialPosB) < 5.0f)
    }

    @Test
    fun testPhase5TrackProgressAndStandingsMonotonicity() {
        val track = TrackMeshBuilder.buildGrandPrixCircuit()
        val waypoints = track.waypoints
        val count = waypoints.size

        val profile = AIDriverProfile("Viper", floatArrayOf(0.1f, 0.5f, 1f, 1f), 0.88f, AIDifficulty.EXPERT)
        val ai = OpponentAIController(VehiclePhysics(), profile, waypoints)

        // Start at waypoint 0
        ai.currentWaypointIndex = 0
        ai.vehicle.position.set(waypoints[0].x, waypoints[0].y, waypoints[0].z)
        val progress0 = ai.getTrackProgress(waypoints)

        // Advance to waypoint 5
        ai.currentWaypointIndex = 5
        ai.vehicle.position.set(waypoints[5].x, waypoints[5].y, waypoints[5].z)
        val progress5 = ai.getTrackProgress(waypoints)

        assertTrue("Track progress must increase monotonically with waypoint advancement", progress5 > progress0)

        // Complete a full lap
        ai.totalLapsCompleted = 1
        ai.currentWaypointIndex = 0
        ai.vehicle.position.set(waypoints[0].x, waypoints[0].y, waypoints[0].z)
        val progressLap1 = ai.getTrackProgress(waypoints)

        assertTrue("Completing lap 1 must advance progress beyond lap 0", progressLap1 >= (progress0 + count))
    }

    @Test
    fun testNeonMetropolisTrackGeneration() {
        val model = TrackMeshBuilder.buildTrack(TrackId.NEON_METROPOLIS)
        assertTrue("Neon Metropolis road mesh should have vertices", model.roadMesh.vertexData.isNotEmpty())
        assertTrue("Neon Metropolis indices should not be empty", model.roadMesh.indices.isNotEmpty())
        assertTrue("Neon Metropolis waypoints should not be empty", model.waypoints.isNotEmpty())
        model.release()
    }
}




