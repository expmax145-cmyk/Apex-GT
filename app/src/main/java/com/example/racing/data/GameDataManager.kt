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
        if (com.example.BuildConfig.DEBUG) {
            credits = amount
        }
    }

    val isDevUnlimitedFundsActive: Boolean
        get() = com.example.BuildConfig.DEBUG && prefs.getBoolean("dev_unlimited_funds", true)

    fun setDevUnlimitedFundsActive(active: Boolean) {
        if (com.example.BuildConfig.DEBUG) {
            prefs.edit().putBoolean("dev_unlimited_funds", active).apply()
        }
    }

    fun canAfford(cost: Int): Boolean {
        if (com.example.BuildConfig.DEBUG && isDevUnlimitedFundsActive) return true
        return credits >= cost
    }

    fun deductCredits(cost: Int): Boolean {
        if (com.example.BuildConfig.DEBUG && isDevUnlimitedFundsActive) {
            return true
        }
        if (credits >= cost) {
            credits -= cost
            return true
        }
        return false
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

    var timeOfDay: com.example.racing.model.TimeOfDay
        get() = try {
            com.example.racing.model.TimeOfDay.valueOf(prefs.getString("time_of_day", com.example.racing.model.TimeOfDay.DYNAMIC.name) ?: com.example.racing.model.TimeOfDay.DYNAMIC.name)
        } catch (_: Exception) {
            com.example.racing.model.TimeOfDay.DYNAMIC
        }
        set(value) = prefs.edit().putString("time_of_day", value.name).apply()

    var weatherCondition: com.example.racing.model.WeatherCondition
        get() = try {
            com.example.racing.model.WeatherCondition.valueOf(prefs.getString("weather_condition", com.example.racing.model.WeatherCondition.CLEAR.name) ?: com.example.racing.model.WeatherCondition.CLEAR.name)
        } catch (_: Exception) {
            com.example.racing.model.WeatherCondition.CLEAR
        }
        set(value) = prefs.edit().putString("weather_condition", value.name).apply()

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

    var playerName: String
        get() = prefs.getString("player_name", "Apex Racer") ?: "Apex Racer"
        set(value) = prefs.edit().putString("player_name", value).apply()

    var playerLevel: Int
        get() = prefs.getInt("player_level", 14)
        set(value) = prefs.edit().putInt("player_level", value).apply()

    var racesCompleted: Int
        get() = prefs.getInt("races_completed", 32)
        set(value) = prefs.edit().putInt("races_completed", value).apply()

    var racesWon: Int
        get() = prefs.getInt("races_won", 18)
        set(value) = prefs.edit().putInt("races_won", value).apply()

    var driverXp: Int
        get() = prefs.getInt("driver_xp", 7420)
        set(value) = prefs.edit().putInt("driver_xp", value).apply()

    // =============================================================================================
    // Phase 17: Championship & Career Mode Progression Persistence
    // =============================================================================================

    var unlockedCups: Set<String>
        get() = prefs.getStringSet("unlocked_cups", setOf("BRONZE")) ?: setOf("BRONZE")
        set(value) = prefs.edit().putStringSet("unlocked_cups", value).apply()

    var completedCups: Set<String>
        get() = prefs.getStringSet("completed_cups", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("completed_cups", value).apply()

    var trophiesEarned: Set<String>
        get() = prefs.getStringSet("trophies_earned", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("trophies_earned", value).apply()

    var careerChampionshipPoints: Int
        get() = prefs.getInt("career_championship_points", 0)
        set(value) = prefs.edit().putInt("career_championship_points", value).apply()

    var championshipEntries: Int
        get() = prefs.getInt("championship_entries", 0)
        set(value) = prefs.edit().putInt("championship_entries", value).apply()

    var championshipWins: Int
        get() = prefs.getInt("championship_wins", 0)
        set(value) = prefs.edit().putInt("championship_wins", value).apply()

    fun unlockCup(cupId: String) {
        val current = unlockedCups.toMutableSet()
        current.add(cupId.uppercase())
        unlockedCups = current
    }

    fun isCupUnlocked(cupId: String): Boolean {
        if (cupId.equals("BRONZE", ignoreCase = true)) return true
        return unlockedCups.contains(cupId.uppercase())
    }

    fun recordCupCompletion(tier: com.example.racing.model.ChampionshipTier, playerWon: Boolean) {
        val completed = completedCups.toMutableSet()
        completed.add(tier.id)
        completedCups = completed

        if (playerWon) {
            val trophies = trophiesEarned.toMutableSet()
            trophies.add(tier.id)
            trophiesEarned = trophies
            championshipWins += 1

            // Unlock next cup tier
            when (tier) {
                com.example.racing.model.ChampionshipTier.BRONZE -> unlockCup("SILVER")
                com.example.racing.model.ChampionshipTier.SILVER -> unlockCup("GOLD")
                com.example.racing.model.ChampionshipTier.GOLD -> { /* Pinnacle reached */ }
            }
        }
    }

    fun saveActiveChampionship(state: com.example.racing.model.ActiveChampionshipState) {
        val serializedStandings = state.standings.joinToString(";") { s ->
            "${s.driverName}|${s.carName}|${s.carId}|${s.points}|${s.wins}|${s.podiums}|${s.bestFinish}|${s.isPlayer}"
        }

        prefs.edit()
            .putString("active_cup_id", state.tier.id)
            .putInt("active_round_index", state.currentRoundIndex)
            .putBoolean("active_round_finished", state.isCurrentRoundFinished)
            .putBoolean("active_cup_completed", state.isCupCompleted)
            .putBoolean("active_rewards_claimed", state.finalRewardsClaimed)
            .putString("active_standings_data", serializedStandings)
            .apply()
    }

    fun loadActiveChampionship(): com.example.racing.model.ActiveChampionshipState? {
        val cupId = prefs.getString("active_cup_id", null) ?: return null
        val tier = com.example.racing.model.ChampionshipTier.fromId(cupId) ?: return null
        val roundIndex = prefs.getInt("active_round_index", 0)
        val roundFinished = prefs.getBoolean("active_round_finished", false)
        val cupCompleted = prefs.getBoolean("active_cup_completed", false)
        val rewardsClaimed = prefs.getBoolean("active_rewards_claimed", false)
        val rawStandings = prefs.getString("active_standings_data", null) ?: return null

        val standings = mutableListOf<com.example.racing.model.ChampionshipDriverStanding>()
        try {
            val entries = rawStandings.split(";")
            for (entry in entries) {
                if (entry.isBlank()) continue
                val parts = entry.split("|")
                if (parts.size >= 8) {
                    standings.add(
                        com.example.racing.model.ChampionshipDriverStanding(
                            driverName = parts[0],
                            carName = parts[1],
                            carId = parts[2],
                            points = parts[3].toIntOrNull() ?: 0,
                            wins = parts[4].toIntOrNull() ?: 0,
                            podiums = parts[5].toIntOrNull() ?: 0,
                            bestFinish = parts[6].toIntOrNull() ?: 4,
                            isPlayer = parts[7].toBooleanStrictOrNull() ?: false
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return null
        }

        if (standings.isEmpty()) return null

        return com.example.racing.model.ActiveChampionshipState(
            tier = tier,
            currentRoundIndex = roundIndex,
            standings = standings,
            isCurrentRoundFinished = roundFinished,
            isCupCompleted = cupCompleted,
            finalRewardsClaimed = rewardsClaimed
        )
    }

    fun clearActiveChampionship() {
        prefs.edit()
            .remove("active_cup_id")
            .remove("active_round_index")
            .remove("active_round_finished")
            .remove("active_cup_completed")
            .remove("active_rewards_claimed")
            .remove("active_standings_data")
            .apply()
    }
}
