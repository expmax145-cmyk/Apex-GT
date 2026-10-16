package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.racing.data.GameDataManager
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.ControlType
import com.example.racing.model.TrackId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex GT", appName)
    }

    @Test
    fun `verify game data manager initialization and defaults`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)
        assertNotNull(dataManager)
        assertEquals(15000, dataManager.credits)
        assertEquals("apex_valkyrie", dataManager.selectedCarId)
        assertEquals(TrackId.GRAND_PRIX, dataManager.selectedTrackId)
        assertTrue(dataManager.unlockedCars.contains("apex_valkyrie"))
    }

    @Test
    fun `verify upgrades persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)

        val newUpgrades = CarUpgradeState(engineStage = 2, tireStage = 1, suspensionStage = 3, weightReduction = 1)
        dataManager.saveCarUpgrades("apex_valkyrie", newUpgrades)

        val retrieved = dataManager.getCarUpgrades("apex_valkyrie")
        assertEquals(2, retrieved.engineStage)
        assertEquals(1, retrieved.tireStage)
        assertEquals(3, retrieved.suspensionStage)
        assertEquals(1, retrieved.weightReduction)
    }

    @Test
    fun `verify best lap time persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)

        dataManager.saveBestLapTime(TrackId.ALPINE_PASS, 68.45f)
        val best = dataManager.getBestLapTime(TrackId.ALPINE_PASS)
        assertEquals(68.45f, best, 0.001f)
    }

    @Test
    fun `verify control type and graphics settings persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)

        dataManager.controlType = ControlType.VIRTUAL_WHEEL
        assertEquals(ControlType.VIRTUAL_WHEEL, dataManager.controlType)

        dataManager.graphicsPreset = "HIGH"
        assertEquals("HIGH", dataManager.graphicsPreset)
    }

    @Test
    fun `verify vehicle customization persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)

        // Color index persistence
        dataManager.setCarColorIndex("apex_monza_v10", 2)
        assertEquals(2, dataManager.getCarColorIndex("apex_monza_v10"))

        // Paint finish persistence
        dataManager.setCarPaintFinish("apex_monza_v10", com.example.racing.engine.PaintFinishType.PEARLESCENT)
        assertEquals(com.example.racing.engine.PaintFinishType.PEARLESCENT, dataManager.getCarPaintFinish("apex_monza_v10"))
    }

    @Test
    fun `verify vehicle economy purchasing and affordability`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataManager = GameDataManager(context)

        // Disable dev unlimited funds to test standard economy deduction
        dataManager.setDevUnlimitedFundsActive(false)

        // Initial default credits
        dataManager.credits = 30000
        assertEquals(30000, dataManager.credits)

        // Can afford a 25,000 CR car
        assertTrue(dataManager.canAfford(25000))
        val purchased = dataManager.deductCredits(25000)
        assertTrue(purchased)
        assertEquals(5000, dataManager.credits)

        // Cannot afford another 25,000 CR car without dev unlimited mode
        org.junit.Assert.assertFalse(dataManager.canAfford(25000))
        val failedPurchase = dataManager.deductCredits(25000)
        org.junit.Assert.assertFalse(failedPurchase)
        assertEquals(5000, dataManager.credits)

        // With dev unlimited mode active, can afford and credits are preserved
        dataManager.setDevUnlimitedFundsActive(true)
        assertTrue(dataManager.canAfford(500000))
        assertTrue(dataManager.deductCredits(500000))
        assertEquals(5000, dataManager.credits)
    }

    @Test
    fun `verify race pause and resume lifecycle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        org.junit.Assert.assertFalse("Game must start unpaused", vm.isGamePaused)
        vm.pauseRace()
        assertTrue("pauseRace must set isGamePaused to true", vm.isGamePaused)
        assertTrue("Telemetry must report paused state", vm.telemetry.value.isPaused)

        vm.resumeRace()
        org.junit.Assert.assertFalse("resumeRace must set isGamePaused to false", vm.isGamePaused)
        org.junit.Assert.assertFalse("Telemetry must report unpaused state", vm.telemetry.value.isPaused)
    }

    @Test
    fun `verify camera view toggling and cycling`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        assertEquals(com.example.racing.model.CameraView.CHASE_NEAR, vm.cameraView.value)
        vm.toggleCamera()
        assertEquals(com.example.racing.model.CameraView.CHASE_FAR, vm.cameraView.value)
        vm.toggleCamera()
        assertEquals(com.example.racing.model.CameraView.HOOD_BUMPER, vm.cameraView.value)
        vm.toggleCamera()
        assertEquals(com.example.racing.model.CameraView.COCKPIT, vm.cameraView.value)
        vm.toggleCamera()
        assertEquals(com.example.racing.model.CameraView.CHASE_NEAR, vm.cameraView.value)
    }

    @Test
    fun `verify control type quick switcher updates data manager`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        vm.setControlType(ControlType.TILT_GYRO)
        assertEquals(ControlType.TILT_GYRO, vm.controlType.value)
        assertEquals(ControlType.TILT_GYRO, vm.dataManager.controlType)

        vm.setControlType(ControlType.TOUCH_BUTTONS)
        assertEquals(ControlType.TOUCH_BUTTONS, vm.controlType.value)
        assertEquals(ControlType.TOUCH_BUTTONS, vm.dataManager.controlType)
    }

    @Test
    fun `verify AI competitors initialization, distinct personalities and vehicles`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        assertEquals(3, vm.aiOpponents.size)
        val viper = vm.aiOpponents[0]
        val shadow = vm.aiOpponents[1]
        val ghost = vm.aiOpponents[2]

        // Verify Viper
        assertEquals("Viper", viper.profile.name)
        assertEquals(com.example.racing.ai.AIDifficulty.EXPERT, viper.profile.difficulty)
        assertEquals(com.example.racing.ai.AIPersonality.AGGRESSIVE, viper.profile.personality)
        assertEquals("apex_spirit_86", viper.profile.carId)

        // Verify Shadow
        assertEquals("Shadow", shadow.profile.name)
        assertEquals(com.example.racing.ai.AIDifficulty.MEDIUM, shadow.profile.difficulty)
        assertEquals(com.example.racing.ai.AIPersonality.BALANCED, shadow.profile.personality)
        assertEquals("apex_monza_v10", shadow.profile.carId)

        // Verify Ghost
        assertEquals("Ghost", ghost.profile.name)
        assertEquals(com.example.racing.ai.AIDifficulty.EASY, ghost.profile.difficulty)
        assertEquals(com.example.racing.ai.AIPersonality.DEFENSIVE, ghost.profile.personality)
        assertEquals("apex_valkyrie", ghost.profile.carId)
    }

    @Test
    fun `verify AI track progress and multi-vehicle update stability`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        val sampleWaypoints = listOf(
            com.example.racing.engine.Vector3f(0f, 0f, 0f),
            com.example.racing.engine.Vector3f(0f, 0f, 50f),
            com.example.racing.engine.Vector3f(50f, 0f, 100f),
            com.example.racing.engine.Vector3f(100f, 0f, 50f),
            com.example.racing.engine.Vector3f(50f, 0f, 0f)
        )

        val viper = vm.aiOpponents[0]
        val testAI = com.example.racing.ai.OpponentAIController(viper.vehicle, viper.profile, sampleWaypoints)

        // Initial progress check
        val initialProgress = testAI.getTrackProgress(sampleWaypoints)
        assertTrue(initialProgress >= 0f)

        // Run multi-vehicle AI update step
        testAI.updateWithVehicles(0.016f, vm.playerVehicle, vm.aiOpponents)
        assertEquals(com.example.racing.ai.AIRecoveryState.NORMAL, testAI.recoveryState)
        assertTrue(testAI.vehicle.throttle >= 0f)
        assertTrue(testAI.vehicle.steerInput in -1.0f..1.0f)
    }

    @Test
    fun `verify garage navigation, showroom mode toggling and camera presets`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        // Navigate to Garage
        vm.openGarage()
        assertEquals(com.example.racing.ui.GameScreen.GARAGE, vm.currentScreen.value)
        val r = vm.renderer
        assertNotNull(r)
        assertTrue("Showroom mode must be active in Garage", r!!.isShowroomMode)
        org.junit.Assert.assertFalse("Main menu mode must be disabled in Garage", r.isMainMenuMode)

        // Verify camera angle presets
        vm.setCameraPreset("HERO_3_4")
        assertEquals(45f, r.showroomOrbitAngleDeg, 0.1f)
        assertEquals(16f, r.showroomElevationDeg, 0.1f)

        vm.setCameraPreset("SIDE")
        assertEquals(90f, r.showroomOrbitAngleDeg, 0.1f)

        vm.setCameraPreset("REAR_AERO")
        assertEquals(155f, r.showroomOrbitAngleDeg, 0.1f)

        vm.setCameraPreset("FRONT_DETAIL")
        assertEquals(15f, r.showroomOrbitAngleDeg, 0.1f)

        // Reset camera
        vm.resetShowroomCamera()
        assertEquals(45f, r.showroomOrbitAngleDeg, 0.1f)
        assertEquals(16f, r.showroomElevationDeg, 0.1f)
        assertEquals(5.4f, r.showroomZoomDistance, 0.1f)
    }

    @Test
    fun `verify fleet vehicle selection and tuning upgrade physics application`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        // Test vehicle switching to Monza V10
        val monza = com.example.racing.model.CarDefinition.ALL_CARS.first { it.id == "apex_monza_v10" }
        vm.selectCar(monza)
        assertEquals(monza.id, vm.selectedCar.value.id)
        assertEquals(monza.id, vm.dataManager.selectedCarId)

        // Test performance upgrade on ECU with realistic economy deduction
        vm.dataManager.setDevUnlimitedFundsActive(false)
        vm.dataManager.credits = 10000
        val initialTopSpeed = vm.getVehicleTopSpeed(monza, vm.carUpgrades.value)
        val initialTorque = vm.getVehicleTorque(monza, vm.carUpgrades.value)

        val upgraded = vm.upgradePart("ENGINE")
        assertTrue("Upgrading engine stage must succeed", upgraded)
        assertEquals(1, vm.carUpgrades.value.engineStage)
        assertEquals(6500, vm.playerCredits.value)

        val upgradedTopSpeed = vm.getVehicleTopSpeed(monza, vm.carUpgrades.value)
        val upgradedTorque = vm.getVehicleTorque(monza, vm.carUpgrades.value)
        assertTrue(upgradedTopSpeed > initialTopSpeed)
        assertTrue(upgradedTorque > initialTorque)
        assertEquals(initialTorque + 65f, upgradedTorque, 0.1f)
    }

    @Test
    fun `verify track mesh procedural generation across Grand Prix, Alpine Pass, and Neon Metropolis`() {
        val gpTrack = com.example.racing.engine.TrackMeshBuilder.buildTrack(com.example.racing.model.TrackId.GRAND_PRIX)
        assertNotNull(gpTrack)
        assertTrue("Grand Prix must have valid waypoints", gpTrack.waypoints.isNotEmpty())
        assertTrue("Grand Prix must have 3 sector checkpoints", gpTrack.sectorCheckpoints.size == 3)
        assertNotNull(gpTrack.roadMesh)
        assertNotNull(gpTrack.kerbMesh)
        assertNotNull(gpTrack.barrierMesh)
        assertNotNull(gpTrack.environmentMesh)
        assertNotNull(gpTrack.gantryMesh)
        assertNotNull(gpTrack.skyMesh)
        org.junit.Assert.assertFalse("Grand Prix is dry road", gpTrack.lightingConfig.isWetRoad)

        val alpineTrack = com.example.racing.engine.TrackMeshBuilder.buildTrack(com.example.racing.model.TrackId.ALPINE_PASS)
        assertNotNull(alpineTrack)
        assertTrue("Alpine Pass must have valid waypoints", alpineTrack.waypoints.isNotEmpty())
        assertTrue("Alpine Pass must feature mountain elevation", alpineTrack.waypoints.any { it.y > 10f })
        assertNotNull(alpineTrack.roadMesh)
        assertNotNull(alpineTrack.environmentMesh)

        val neonTrack = com.example.racing.engine.TrackMeshBuilder.buildTrack(com.example.racing.model.TrackId.NEON_METROPOLIS)
        assertNotNull(neonTrack)
        assertTrue("Neon Metropolis must have valid waypoints", neonTrack.waypoints.isNotEmpty())
        assertTrue("Neon Metropolis is wet night road", neonTrack.lightingConfig.isWetRoad)
        assertNotNull(neonTrack.roadMesh)
        assertNotNull(neonTrack.environmentMesh)
    }

    @Test
    fun `verify track selection updates renderer and physics waypoints`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.racing.ui.GameViewModel(app)

        vm.selectTrack(com.example.racing.model.TrackId.ALPINE_PASS)
        assertEquals(com.example.racing.model.TrackId.ALPINE_PASS, vm.selectedTrack.value)
        assertEquals(com.example.racing.model.TrackId.ALPINE_PASS, vm.dataManager.selectedTrackId)

        vm.selectTrack(com.example.racing.model.TrackId.NEON_METROPOLIS)
        assertEquals(com.example.racing.model.TrackId.NEON_METROPOLIS, vm.selectedTrack.value)
        assertEquals(com.example.racing.model.TrackId.NEON_METROPOLIS, vm.dataManager.selectedTrackId)
    }
}

