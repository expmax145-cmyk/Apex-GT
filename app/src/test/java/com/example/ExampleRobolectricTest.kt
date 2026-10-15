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
}
