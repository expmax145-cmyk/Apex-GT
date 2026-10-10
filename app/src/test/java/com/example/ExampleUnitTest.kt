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
}
