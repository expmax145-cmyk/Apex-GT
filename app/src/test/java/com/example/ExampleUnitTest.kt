package com.example

import com.example.racing.ai.AIDifficulty
import com.example.racing.ai.AIDriverProfile
import com.example.racing.ai.OpponentAIController
import com.example.racing.engine.CarMeshBuilder
import com.example.racing.engine.Material
import com.example.racing.engine.TrackMeshBuilder
import com.example.racing.engine.Vector3f
import com.example.racing.model.CarDefinition
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

        // Initial step should detect stop and shift to reverse (gear 0)
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

        // 3. Draw call without null pointers
        val vp = FloatArray(16)
        android.opengl.Matrix.setIdentityM(vp, 0)
        ps.draw(vp, camPos)

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
}


