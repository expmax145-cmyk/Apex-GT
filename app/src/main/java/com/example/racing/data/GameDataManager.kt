package com.example.racing.data

import android.content.Context
import android.content.SharedPreferences
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.ControlType
import com.example.racing.model.TrackId

class GameDataManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apex_racing_prefs", Context.MODE_PRIVATE)

    var credits: Int
        get() = prefs.getInt("credits", 15000)
        set(value) = prefs.edit().putInt("credits", value).apply()

    fun grantDevelopmentCredits(amount: Int = 999999) {
        credits = amount
    }

    var selectedCarId: String
        get() = prefs.getString("selected_car_id", CarDefinition.ALL_CARS[0].id) ?: CarDefinition.ALL_CARS[0].id
        set(value) = prefs.edit().putString("selected_car_id", value).apply()

    var selectedTrackId: TrackId
        get() = try {
            TrackId.valueOf(prefs.getString("selected_track_id", TrackId.GRAND_PRIX.name) ?: TrackId.GRAND_PRIX.name)
        } catch (_: Exception) {
            TrackId.GRAND_PRIX
        }
        set(value) = prefs.edit().putString("selected_track_id", value.name).apply()

    var unlockedCars: Set<String>
        get() = prefs.getStringSet("unlocked_cars", setOf(CarDefinition.ALL_CARS[0].id)) ?: setOf(CarDefinition.ALL_CARS[0].id)
        set(value) = prefs.edit().putStringSet("unlocked_cars", value).apply()

    var controlType: ControlType
        get() = try {
            ControlType.valueOf(prefs.getString("control_type", ControlType.TOUCH_BUTTONS.name) ?: ControlType.TOUCH_BUTTONS.name)
        } catch (_: Exception) {
            ControlType.TOUCH_BUTTONS
        }
        set(value) = prefs.edit().putString("control_type", value.name).apply()

    var graphicsPreset: String
        get() = prefs.getString("graphics_preset", "HIGH") ?: "HIGH"
        set(value) = prefs.edit().putString("graphics_preset", value).apply()

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 0.75f)
        set(value) = prefs.edit().putFloat("sound_volume", value).apply()

    var soundMuted: Boolean
        get() = prefs.getBoolean("sound_muted", false)
        set(value) = prefs.edit().putBoolean("sound_muted", value).apply()

    var steerSensitivity: Float
        get() = prefs.getFloat("steer_sensitivity", 1.0f)
        set(value) = prefs.edit().putFloat("steer_sensitivity", value).apply()

    fun getCarColorIndex(carId: String): Int {
        return prefs.getInt("car_color_$carId", 0)
    }

    fun setCarColorIndex(carId: String, colorIdx: Int) {
        prefs.edit().putInt("car_color_$carId", colorIdx).apply()
    }

    fun getCarPaintFinish(carId: String): com.example.racing.engine.PaintFinishType {
        val name = prefs.getString("car_paint_finish_$carId", com.example.racing.engine.PaintFinishType.METALLIC.name)
        return try {
            com.example.racing.engine.PaintFinishType.valueOf(name ?: com.example.racing.engine.PaintFinishType.METALLIC.name)
        } catch (_: Exception) {
            com.example.racing.engine.PaintFinishType.METALLIC
        }
    }

    fun setCarPaintFinish(carId: String, finish: com.example.racing.engine.PaintFinishType) {
        prefs.edit().putString("car_paint_finish_$carId", finish.name).apply()
    }

    fun getCarUpgrades(carId: String): CarUpgradeState {
        val eng = prefs.getInt("upgrade_eng_$carId", 0)
        val tire = prefs.getInt("upgrade_tire_$carId", 0)
        val susp = prefs.getInt("upgrade_susp_$carId", 0)
        val wt = prefs.getInt("upgrade_wt_$carId", 0)
        return CarUpgradeState(eng, tire, susp, wt)
    }

    fun saveCarUpgrades(carId: String, state: CarUpgradeState) {
        prefs.edit()
            .putInt("upgrade_eng_$carId", state.engineStage)
            .putInt("upgrade_tire_$carId", state.tireStage)
            .putInt("upgrade_susp_$carId", state.suspensionStage)
            .putInt("upgrade_wt_$carId", state.weightReduction)
            .apply()
    }

    fun getBestLapTime(track: TrackId = TrackId.GRAND_PRIX): Float {
        return prefs.getFloat("best_lap_time_${track.name}", 0f)
    }

    fun saveBestLapTime(track: TrackId, timeSec: Float) {
        val currentBest = getBestLapTime(track)
        if (currentBest == 0f || timeSec < currentBest) {
            prefs.edit().putFloat("best_lap_time_${track.name}", timeSec).apply()
        }
    }
}
