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
}
