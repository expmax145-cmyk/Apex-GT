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
import com.example.racing.physics.VehiclePhysics
import org.junit.Assert.assertEquals
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
}

