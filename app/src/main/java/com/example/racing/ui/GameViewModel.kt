package com.example.racing.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.racing.ai.AIDifficulty
import com.example.racing.ai.AIDriverProfile
import com.example.racing.ai.OpponentAIController
import com.example.racing.audio.ProceduralAudioEngine
import com.example.racing.data.GameDataManager
import com.example.racing.engine.RacingRenderer
import com.example.racing.engine.Vector3f
import com.example.racing.model.CameraView
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.ControlType
import com.example.racing.model.RaceMode
import com.example.racing.model.RaceTiming
import com.example.racing.model.TrackId
import com.example.racing.engine.TrackMeshBuilder
import com.example.racing.physics.EnginePowertrain
import com.example.racing.physics.TireModel
import com.example.racing.physics.VehicleCollisionSystem
import com.example.racing.physics.VehiclePhysics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class GameScreen {
    MAIN_MENU,
    GARAGE,
    RACE,
    RACE_SUMMARY,
    SETTINGS,
    PROFILE
}

data class RaceTelemetryUiState(
    val speedKmh: Float = 0f,
    val rpm: Float = 1000f,
    val maxRpm: Float = 8600f,
    val gear: String = "1",
    val gForceLat: Float = 0f,
    val gForceLong: Float = 0f,
    val driftAngleDeg: Float = 0f,
    val isOffRoad: Boolean = false,
    val currentLap: Int = 1,
    val totalLaps: Int = 3,
    val currentLapTime: String = "00:00.000",
    val bestLapTime: String = "--:--.---",
    val lastLapTime: String = "--:--.---",
    val sectorDelta: String = "",
    val sectorDeltaPositive: Boolean = false,
    val currentSector: Int = 1,
    val racePosition: Int = 1,
    val totalRacers: Int = 4,
    val countdownNumber: Int = 0,
    val isCountdownActive: Boolean = false,
    val boostAvailable: Boolean = true,
    val isPaused: Boolean = false,
    val boostPercent: Float = 100f,
    val isAbsActive: Boolean = false,
    val isTcsActive: Boolean = false,
    val isRevLimiter: Boolean = false,
    val trackProgressPercent: Float = 0f,
    val activeCameraView: CameraView = CameraView.CHASE_NEAR
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val dataManager = GameDataManager(application)
    val audioEngine = ProceduralAudioEngine()

    private val _currentScreen = MutableStateFlow(GameScreen.MAIN_MENU)
    val currentScreen: StateFlow<GameScreen> = _currentScreen.asStateFlow()

    private val _activeRaceMode = MutableStateFlow(RaceMode.CIRCUIT_RACE)
    val activeRaceMode: StateFlow<RaceMode> = _activeRaceMode.asStateFlow()

    private val _selectedTrack = MutableStateFlow(dataManager.selectedTrackId)
    val selectedTrack: StateFlow<TrackId> = _selectedTrack.asStateFlow()

    private val _cameraView = MutableStateFlow(CameraView.CHASE_NEAR)
    val cameraView: StateFlow<CameraView> = _cameraView.asStateFlow()

    private val _telemetry = MutableStateFlow(RaceTelemetryUiState())
    val telemetry: StateFlow<RaceTelemetryUiState> = _telemetry.asStateFlow()

    private val _isGamePaused = MutableStateFlow(false)
    val isGamePausedState: StateFlow<Boolean> = _isGamePaused.asStateFlow()
    var isGamePaused: Boolean
        get() = _isGamePaused.value
        private set(value) { _isGamePaused.value = value }

    private val _selectedCar = MutableStateFlow(CarDefinition.ALL_CARS[0])
    val selectedCar: StateFlow<CarDefinition> = _selectedCar.asStateFlow()

    private val _playerCredits = MutableStateFlow(dataManager.credits)
    val playerCredits: StateFlow<Int> = _playerCredits.asStateFlow()

    private val _playerName = MutableStateFlow(dataManager.playerName)
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _playerLevel = MutableStateFlow(dataManager.playerLevel)
    val playerLevel: StateFlow<Int> = _playerLevel.asStateFlow()

    private val _racesCompleted = MutableStateFlow(dataManager.racesCompleted)
    val racesCompleted: StateFlow<Int> = _racesCompleted.asStateFlow()

    private val _racesWon = MutableStateFlow(dataManager.racesWon)
    val racesWon: StateFlow<Int> = _racesWon.asStateFlow()

    private val _driverXp = MutableStateFlow(dataManager.driverXp)
    val driverXp: StateFlow<Int> = _driverXp.asStateFlow()

    fun updatePlayerName(newName: String) {
        if (newName.isNotBlank()) {
            dataManager.playerName = newName.trim()
            _playerName.value = dataManager.playerName
        }
    }

    private val _unlockedCarIds = MutableStateFlow(dataManager.unlockedCars)
    val unlockedCarIds: StateFlow<Set<String>> = _unlockedCarIds.asStateFlow()

    private val _carUpgrades = MutableStateFlow(dataManager.getCarUpgrades(dataManager.selectedCarId))
    val carUpgrades: StateFlow<CarUpgradeState> = _carUpgrades.asStateFlow()

    private val _carColorIndex = MutableStateFlow(dataManager.getCarColorIndex(dataManager.selectedCarId))
    val carColorIndex: StateFlow<Int> = _carColorIndex.asStateFlow()

    private val _carPaintFinish = MutableStateFlow(dataManager.getCarPaintFinish(dataManager.selectedCarId))
    val carPaintFinish: StateFlow<com.example.racing.engine.PaintFinishType> = _carPaintFinish.asStateFlow()

    private val _controlType = MutableStateFlow(dataManager.controlType)
    val controlType: StateFlow<ControlType> = _controlType.asStateFlow()

    private val _graphicsPreset = MutableStateFlow(dataManager.graphicsPreset)
    val graphicsPreset: StateFlow<String> = _graphicsPreset.asStateFlow()

    private val _timeOfDay = MutableStateFlow(dataManager.timeOfDay)
    val timeOfDay: StateFlow<com.example.racing.model.TimeOfDay> = _timeOfDay.asStateFlow()

    private val _weatherCondition = MutableStateFlow(dataManager.weatherCondition)
    val weatherCondition: StateFlow<com.example.racing.model.WeatherCondition> = _weatherCondition.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<com.example.racing.model.LeaderboardRacer>>(emptyList())
    val leaderboard: StateFlow<List<com.example.racing.model.LeaderboardRacer>> = _leaderboard.asStateFlow()

    var lastRaceResults: List<com.example.racing.model.LeaderboardRacer> = emptyList()
        private set

    var playerVehicle = VehiclePhysics()
    val aiOpponents = mutableListOf<OpponentAIController>()
    private val reusableVehicleList = ArrayList<VehiclePhysics>(8)
    var renderer: RacingRenderer? = null
        private set

    val raceTiming = RaceTiming()
    private var passedSector1 = false
    private var passedSector2 = false
    private var passedSector3 = false
    private var currentSectorIndex = 1
    private var sectorDeltaText = ""
    private var sectorDeltaPositive = false
    private var boostEnergy = 100f
    private var finishLineCooldown = 0f

    init {
        val savedCar = CarDefinition.ALL_CARS.find { it.id == dataManager.selectedCarId } ?: CarDefinition.ALL_CARS[0]
        _selectedCar.value = savedCar
        applyCarConfiguration(savedCar)

        setupAICompetitors()

        renderer = RacingRenderer(playerVehicle, aiOpponents).apply {
            isShowroomMode = true
            isMainMenuMode = true
            autoRotateShowroom = true
            playerPaintColor = getActiveColor()
            playerPaintFinish = dataManager.getCarPaintFinish(savedCar.id)
            graphicsQuality = dataManager.graphicsPreset
            timeOfDay = dataManager.timeOfDay
            weatherCondition = dataManager.weatherCondition
            setCar(savedCar.id)
            setTrack(dataManager.selectedTrackId)
        }

        audioEngine.isMuted = dataManager.soundMuted
        audioEngine.masterVolume = dataManager.soundVolume
        audioEngine.start()
    }

    private fun getActiveColor(): FloatArray {
        val car = _selectedCar.value
        val idx = _carColorIndex.value.coerceIn(0, car.availableColors.size - 1)
        return car.availableColors[idx].second
    }

    private fun setupAICompetitors() {
        aiOpponents.clear()
        val profiles = listOf(
            AIDriverProfile("Viper", floatArrayOf(0.10f, 0.50f, 0.98f, 1.0f), 0.88f, AIDifficulty.EXPERT, carId = "apex_spirit_86", preferredLaneOffset = -1.8f, personality = com.example.racing.ai.AIPersonality.AGGRESSIVE),
            AIDriverProfile("Shadow", floatArrayOf(0.15f, 0.15f, 0.16f, 1.0f), 0.74f, AIDifficulty.MEDIUM, carId = "apex_monza_v10", preferredLaneOffset = 0.0f, personality = com.example.racing.ai.AIPersonality.BALANCED),
            AIDriverProfile("Ghost", floatArrayOf(0.98f, 0.88f, 0.10f, 1.0f), 0.58f, AIDifficulty.EASY, carId = "apex_valkyrie", preferredLaneOffset = 1.8f, personality = com.example.racing.ai.AIPersonality.DEFENSIVE)
        )
        for (p in profiles) {
            val carDef = CarDefinition.ALL_CARS.find { it.id == p.carId } ?: CarDefinition.ALL_CARS[0]
            val cyl = if (carDef.id == "apex_monza_v10") 10 else if (carDef.id == "apex_spirit_86") 6 else 8
            val pt = EnginePowertrain(
                maxTorqueNm = carDef.baseTorqueNm,
                redlineRpm = if (cyl == 10) 9200f else 8600f,
                cylinderCount = cyl,
                hasTurbo = carDef.id != "apex_monza_v10"
            )
            val aiPhys = VehiclePhysics(
                massKg = carDef.massKg,
                wheelbaseM = carDef.wheelbaseM,
                trackWidthM = carDef.trackWidthM,
                cgHeightM = carDef.cgHeightM,
                maxSteerAngleRad = carDef.maxSteerAngleRad,
                downforceCoeff = carDef.downforceCoeff,
                dragCoeff = carDef.dragCoeff,
                driveFrontRatio = carDef.driveFrontRatio,
                powertrain = pt
            )
            aiOpponents.add(OpponentAIController(aiPhys, p, emptyList()))
        }
    }

    private fun applyCarConfiguration(car: CarDefinition) {
        val upgrades = dataManager.getCarUpgrades(car.id)
        _carUpgrades.value = upgrades

        val extraTorque = upgrades.engineStage * 65f
        val tireGrip = 1.25f + (upgrades.tireStage * 0.12f)
        val weight = (car.massKg - upgrades.weightReduction * 55f).coerceAtLeast(950f)

        val cyl = when (car.id) {
            "apex_monza_v10" -> 10
            "apex_spirit_86" -> 6
            else -> 8
        }
        val turbo = car.id != "apex_monza_v10"

        val tireModel = TireModel(dPeakFriction = tireGrip)
        val powertrain = EnginePowertrain(
            maxTorqueNm = car.baseTorqueNm + extraTorque,
            redlineRpm = if (cyl == 10) 9200f else 8600f,
            cylinderCount = cyl,
            hasTurbo = turbo
        )

        audioEngine.cylinderCount = cyl

        playerVehicle = VehiclePhysics(
            massKg = weight,
            wheelbaseM = car.wheelbaseM,
            trackWidthM = car.trackWidthM,
            cgHeightM = car.cgHeightM,
            maxSteerAngleRad = car.maxSteerAngleRad,
            downforceCoeff = car.downforceCoeff,
            dragCoeff = car.dragCoeff,
            driveFrontRatio = car.driveFrontRatio,
            tireModel = tireModel,
            powertrain = powertrain
        )
        playerVehicle.resetAt(Vector3f(0f, 0.35f, -30f), 0f)

        // Keep renderer synchronized with new instance
        renderer?.playerVehicle = playerVehicle
    }

    fun selectTrack(trackId: TrackId) {
        _selectedTrack.value = trackId
        dataManager.selectedTrackId = trackId
        renderer?.setTrack(trackId)
    }

    fun startRace(mode: RaceMode) {
        _activeRaceMode.value = mode
        _currentScreen.value = GameScreen.RACE
        renderer?.isShowroomMode = false
        renderer?.isMainMenuMode = false

        renderer?.setTrack(_selectedTrack.value)
        renderer?.setCar(_selectedCar.value.id)
        renderer?.playerPaintColor = getActiveColor()

        audioEngine.isWetRoad = _selectedTrack.value == TrackId.NEON_METROPOLIS

        val track = renderer?.trackModel ?: TrackMeshBuilder.buildTrack(_selectedTrack.value)
        val waypoints = track.waypoints
        val startPoint = waypoints.firstOrNull() ?: Vector3f(0f, 0.35f, -30f)

        // Calculate true track forward direction and heading at start line
        val headingRad: Float
        val forwardX: Float
        val forwardZ: Float
        if (waypoints.size >= 2) {
            val nextPoint = waypoints[1]
            val dx = nextPoint.x - startPoint.x
            val dz = nextPoint.z - startPoint.z
            headingRad = kotlin.math.atan2(dx, dz)
            forwardX = kotlin.math.sin(headingRad)
            forwardZ = kotlin.math.cos(headingRad)
        } else {
            headingRad = 0f
            forwardX = 0f
            forwardZ = 1f
        }
        val rightX = forwardZ
        val rightZ = -forwardX

        // Staggered grid starting behind the start line, pointing along track direction
        playerVehicle.resetAt(
            Vector3f(
                startPoint.x - forwardX * 10f + rightX * 2.0f,
                startPoint.y,
                startPoint.z - forwardZ * 10f + rightZ * 2.0f
            ),
            headingRad
        )

        val gridOffsets = listOf(
            Vector3f(
                startPoint.x - forwardX * 4f - rightX * 2.0f,
                startPoint.y,
                startPoint.z - forwardZ * 4f - rightZ * 2.0f
            ),
            Vector3f(
                startPoint.x - forwardX * 18f + rightX * 2.0f,
                startPoint.y,
                startPoint.z - forwardZ * 18f + rightZ * 2.0f
            ),
            Vector3f(
                startPoint.x - forwardX * 25f - rightX * 2.0f,
                startPoint.y,
                startPoint.z - forwardZ * 25f - rightZ * 2.0f
            )
        )

        for (i in aiOpponents.indices) {
            val ai = aiOpponents[i]
            ai.vehicle.resetAt(gridOffsets[i], headingRad)
            aiOpponents[i] = OpponentAIController(ai.vehicle, ai.profile, waypoints)
        }

        raceTiming.currentLap = 1
        raceTiming.totalLaps = if (mode == RaceMode.CIRCUIT_RACE) 3 else 999
        raceTiming.currentLapTimeSec = 0f
        raceTiming.lastLapTimeSec = 0f
        raceTiming.bestLapTimeSec = dataManager.getBestLapTime(_selectedTrack.value)
        raceTiming.raceFinished = false
        raceTiming.countdownSec = if (mode == RaceMode.FREE_DRIVE) 0f else 3.99f
        passedSector1 = false
        passedSector2 = false
        passedSector3 = false
        currentSectorIndex = 1
        sectorDeltaText = ""
        boostEnergy = 100f
        finishLineCooldown = 5.0f

        // Guarantee clean inputs and 1st gear engaged upon entering race
        resetDriverInputs()
        playerVehicle.brake = if (mode == RaceMode.FREE_DRIVE) 0f else 1f
        playerVehicle.powertrain.currentGear = 2 // 1st gear
        playerVehicle.powertrain.currentRpm = playerVehicle.powertrain.idleRpm
        playerVehicle.reverseHoldTimer = 0f
        for (ai in aiOpponents) {
            ai.vehicle.brake = if (mode == RaceMode.FREE_DRIVE) 0f else 1f
            ai.vehicle.powertrain.currentGear = 2
            ai.vehicle.powertrain.currentRpm = ai.vehicle.powertrain.idleRpm
            ai.vehicle.reverseHoldTimer = 0f
        }
    }

    fun updateGamePhysics(dt: Float) {
        if (_currentScreen.value != GameScreen.RACE || _isGamePaused.value) {
            audioEngine.throttleLoad = 0f
            audioEngine.tireSlip = 0f
            audioEngine.turboBoost = 0f
            return
        }

        if (finishLineCooldown > 0f) {
            finishLineCooldown -= dt
        }

        // Countdown phase: lock vehicle on starting grid in 1st gear (prevents automatic reverse shifting)
        if (raceTiming.isCountdownActive) {
            raceTiming.countdownSec -= dt
            playerVehicle.throttle = 0f
            playerVehicle.brake = 1f
            playerVehicle.forwardVelocity = 0f
            playerVehicle.lateralVelocity = 0f
            playerVehicle.yawRate = 0f
            playerVehicle.reverseHoldTimer = 0f
            playerVehicle.powertrain.currentGear = 2 // Lock 1st gear
            playerVehicle.powertrain.currentRpm = playerVehicle.powertrain.idleRpm

            for (ai in aiOpponents) {
                ai.vehicle.throttle = 0f
                ai.vehicle.brake = 1f
                ai.vehicle.forwardVelocity = 0f
                ai.vehicle.lateralVelocity = 0f
                ai.vehicle.yawRate = 0f
                ai.vehicle.reverseHoldTimer = 0f
                ai.vehicle.powertrain.currentGear = 2
                ai.vehicle.powertrain.currentRpm = ai.vehicle.powertrain.idleRpm
            }

            // Once countdown expires, cleanly transition to GO
            if (raceTiming.countdownSec <= 0f) {
                raceTiming.countdownSec = 0f
                playerVehicle.brake = 0f
                playerVehicle.reverseHoldTimer = 0f
                playerVehicle.powertrain.currentGear = 2 // 1st gear
                for (ai in aiOpponents) {
                    ai.vehicle.brake = 0f
                    ai.vehicle.reverseHoldTimer = 0f
                    ai.vehicle.powertrain.currentGear = 2
                }
            }

            updateTelemetryState()
            return
        }

        // Boost Energy
        if (playerVehicle.boostActive && boostEnergy > 0f) {
            boostEnergy = (boostEnergy - dt * 28f).coerceAtLeast(0f)
            if (boostEnergy <= 0f) {
                playerVehicle.boostActive = false
            }
        } else if (!playerVehicle.boostActive && boostEnergy < 100f) {
            boostEnergy = (boostEnergy + dt * 8f).coerceAtMost(100f)
        }

        val track = renderer?.trackModel
        val waypoints = track?.waypoints ?: emptyList()

        // Phase 4 Dynamic Weather & Track Surface Adhesion
        val weatherGrip = when (_weatherCondition.value) {
            com.example.racing.model.WeatherCondition.CLEAR -> 1.0f
            com.example.racing.model.WeatherCondition.OVERCAST -> 0.98f
            com.example.racing.model.WeatherCondition.LIGHT_RAIN -> 0.84f
            com.example.racing.model.WeatherCondition.HEAVY_RAIN -> 0.70f
        }
        val trackWetFactor = if (track?.lightingConfig?.isWetRoad == true) 0.88f else 1.0f
        val baseSurfaceGrip = weatherGrip * trackWetFactor

        playerVehicle.update(dt, waypoints, roadHalfWidth = 7.0f, baseSurfaceGrip = baseSurfaceGrip)

        for (i in aiOpponents.indices) {
            aiOpponents[i].updateWithVehicles(dt, playerVehicle, aiOpponents, baseSurfaceGrip = baseSurfaceGrip)
        }

        // 1. Physical Vehicle-to-Vehicle Collisions (OBB SAT & Impulse Response)
        reusableVehicleList.clear()
        reusableVehicleList.add(playerVehicle)
        for (i in aiOpponents.indices) {
            reusableVehicleList.add(aiOpponents[i].vehicle)
        }
        VehicleCollisionSystem.resolveVehicleCollisions(
            vehicles = reusableVehicleList,
            playerVehicle = playerVehicle,
            particleSystem = renderer?.particleSystem,
            onCollisionImpact = { intensity ->
                audioEngine.triggerCollision(intensity)
            }
        )

        // 2. Barrier Collision Audio Feedback
        if (playerVehicle.hasCollidedBarrier && playerVehicle.speedKmh > 12f) {
            audioEngine.triggerCollision((playerVehicle.speedKmh / 55f).coerceIn(0.4f, 1.3f))
        }

        // 3. Rumble Strip / Kerb Audio Detection (synchronized with physical suspension excitation)
        audioEngine.isOnKerb = playerVehicle.isOnKerb && playerVehicle.speedKmh > 18f

        // 4. Opponent Spatial Audio (Closest competitor to player)
        var closestAiDist = Float.MAX_VALUE
        var closestAi: OpponentAIController? = null
        for (ai in aiOpponents) {
            val d = playerVehicle.position.distanceTo(ai.vehicle.position)
            if (d < closestAiDist) {
                closestAiDist = d
                closestAi = ai
            }
        }
        if (closestAi != null && closestAiDist < 35f) {
            audioEngine.opponentDistance = closestAiDist
            audioEngine.opponentRpm = closestAi.vehicle.powertrain.currentRpm

            val toAiX = closestAi.vehicle.position.x - playerVehicle.position.x
            val toAiZ = closestAi.vehicle.position.z - playerVehicle.position.z
            val forwardX = kotlin.math.sin(playerVehicle.headingAngleRad)
            val forwardZ = kotlin.math.cos(playerVehicle.headingAngleRad)
            val rightX = forwardZ
            val rightZ = -forwardX
            val latRel = toAiX * rightX + toAiZ * rightZ
            audioEngine.opponentPan = (latRel / 10f).coerceIn(-1.0f, 1.0f)
        } else {
            audioEngine.opponentDistance = 100f
        }

        // 5. Checkpoint & Lap Validation
        if (!raceTiming.raceFinished && track != null && waypoints.isNotEmpty()) {
            raceTiming.currentLapTimeSec += dt

            val carP = playerVehicle.position
            val sectors = track.sectorCheckpoints
            if (sectors.size >= 3) {
                if (!passedSector1 && carP.distanceTo(sectors[1]) < 22f) {
                    passedSector1 = true
                    currentSectorIndex = 2
                    updateSectorDelta()
                } else if (passedSector1 && !passedSector2 && carP.distanceTo(sectors[2]) < 22f) {
                    passedSector2 = true
                    currentSectorIndex = 3
                    updateSectorDelta()
                } else if (passedSector2 && !passedSector3 && carP.distanceTo(sectors[0]) < 22f) {
                    passedSector3 = true
                }
            }

            // Finish Line Crossing Detection
            val finishPoint = waypoints[0]
            val distToFinish = carP.distanceTo(finishPoint)
            if (passedSector2 && distToFinish < 20f && finishLineCooldown <= 0f) {
                passedSector1 = false
                passedSector2 = false
                passedSector3 = false
                currentSectorIndex = 1
                finishLineCooldown = 8.0f // Prevent multi-triggers on single crossing

                val lapTime = raceTiming.currentLapTimeSec
                raceTiming.lastLapTimeSec = lapTime

                if (raceTiming.bestLapTimeSec == 0f || lapTime < raceTiming.bestLapTimeSec) {
                    raceTiming.bestLapTimeSec = lapTime
                    dataManager.saveBestLapTime(_selectedTrack.value, lapTime)
                    sectorDeltaText = "-0.45s"
                    sectorDeltaPositive = false
                }

                if (_activeRaceMode.value == RaceMode.CIRCUIT_RACE && raceTiming.currentLap >= raceTiming.totalLaps) {
                    raceTiming.raceFinished = true
                    val (finalPos, standings) = computeRaceStandings()
                    raceTiming.finishPosition = finalPos
                    lastRaceResults = standings
                    awardRaceFinishCredits(finalPos)
                    _currentScreen.value = GameScreen.RACE_SUMMARY
                } else {
                    raceTiming.currentLap++
                    raceTiming.currentLapTimeSec = 0f
                }
            }
        }

        // Audio engine parameter synchronization
        audioEngine.engineRpm = playerVehicle.powertrain.currentRpm
        audioEngine.redlineRpm = playerVehicle.powertrain.redlineRpm
        audioEngine.throttleLoad = playerVehicle.throttle
        audioEngine.tireSlip = maxOf(playerVehicle.tireSlipRl, playerVehicle.tireSlipRr, playerVehicle.tireSlipFl, playerVehicle.tireSlipFr)
        audioEngine.turboBoost = playerVehicle.powertrain.turboBoostBar
        if (playerVehicle.powertrain.exhaustBackfireTrigger) {
            audioEngine.triggerPop = true
        }

        // Update real-time leaderboard
        val (_, currentStandings) = computeRaceStandings()
        _leaderboard.value = currentStandings

        updateTelemetryState()
    }

    private fun updateSectorDelta() {
        val delta = ((-0.35f + (Math.random() * 0.7f)).toFloat())
        sectorDeltaPositive = delta > 0f
        sectorDeltaText = if (delta > 0) "+%.2fs".format(delta) else "%.2fs".format(delta)
    }

    private fun computeRaceStandings(): Pair<Int, List<com.example.racing.model.LeaderboardRacer>> {
        val track = renderer?.trackModel
        val waypoints = track?.waypoints ?: return Pair(1, emptyList())
        if (waypoints.isEmpty()) return Pair(1, emptyList())

        val count = waypoints.size

        // 1. Calculate player total progress
        val playerPos = playerVehicle.position
        var closestIdx = 0
        var closestDistSq = Float.MAX_VALUE
        for (i in waypoints.indices) {
            val wp = waypoints[i]
            val dSq = (playerPos.x - wp.x) * (playerPos.x - wp.x) + (playerPos.z - wp.z) * (playerPos.z - wp.z)
            if (dSq < closestDistSq) {
                closestDistSq = dSq
                closestIdx = i
            }
        }
        val playerTotalProgress = (raceTiming.currentLap - 1) * count + closestIdx

        data class StandingEntry(
            val name: String,
            val carName: String,
            val color: FloatArray,
            val currentLap: Int,
            val bestLapTimeSec: Float,
            val totalProgress: Float,
            val isPlayer: Boolean
        )

        val entries = mutableListOf<StandingEntry>()
        val playerCar = _selectedCar.value
        entries.add(
            StandingEntry(
                name = "YOU",
                carName = playerCar.name,
                color = getActiveColor(),
                currentLap = raceTiming.currentLap,
                bestLapTimeSec = raceTiming.bestLapTimeSec,
                totalProgress = playerTotalProgress.toFloat(),
                isPlayer = true
            )
        )

        for (ai in aiOpponents) {
            val aiCarDef = CarDefinition.ALL_CARS.find { it.id == ai.profile.carId } ?: CarDefinition.ALL_CARS[0]
            entries.add(
                StandingEntry(
                    name = ai.profile.name,
                    carName = aiCarDef.name,
                    color = ai.profile.carColor,
                    currentLap = ai.currentLap,
                    bestLapTimeSec = ai.bestLapTimeSec,
                    totalProgress = ai.getTrackProgress(waypoints),
                    isPlayer = false
                )
            )
        }

        // Sort descending by totalProgress
        entries.sortByDescending { it.totalProgress }

        val leaderProgress = entries.firstOrNull()?.totalProgress ?: 1f
        var playerPosition = 1

        val resultList = mutableListOf<com.example.racing.model.LeaderboardRacer>()
        for (i in entries.indices) {
            val e = entries[i]
            val pos = i + 1
            if (e.isPlayer) {
                playerPosition = pos
            }
            val gapText = if (pos == 1) {
                "LEADER"
            } else {
                val progressDiff = leaderProgress - e.totalProgress
                val approxGapSec = (progressDiff * 0.22f).coerceAtLeast(0.1f)
                "+%.2fs".format(approxGapSec)
            }
            resultList.add(
                com.example.racing.model.LeaderboardRacer(
                    position = pos,
                    name = e.name,
                    carName = e.carName,
                    color = e.color,
                    currentLap = e.currentLap,
                    bestLapTimeSec = e.bestLapTimeSec,
                    gapToLeaderText = gapText,
                    isPlayer = e.isPlayer
                )
            )
        }

        return Pair(playerPosition, resultList)
    }

    private fun computeRacePosition(): Int {
        return computeRaceStandings().first
    }

    private fun awardRaceFinishCredits(position: Int) {
        val reward = when (position) {
            1 -> 7500
            2 -> 4500
            3 -> 3000
            else -> 1500
        }
        dataManager.credits += reward
        _playerCredits.value = dataManager.credits
    }

    private fun updateTelemetryState() {
        val timing = raceTiming
        val rpmRatio = (playerVehicle.powertrain.currentRpm / playerVehicle.powertrain.redlineRpm).coerceIn(0f, 1f)
        val absActive = (playerVehicle.brake > 0.40f && playerVehicle.speedKmh > 8f &&
                (playerVehicle.tireSlipFl > 0.30f || playerVehicle.tireSlipFr > 0.30f || playerVehicle.wheelSpinFl > 0.25f))
        val tcsActive = (playerVehicle.throttle > 0.45f &&
                (playerVehicle.wheelSpinRl > 0.25f || playerVehicle.wheelSpinRr > 0.25f || playerVehicle.isOffRoad))

        val track = renderer?.trackModel
        val waypoints = track?.waypoints ?: emptyList()
        val lapProgress = if (waypoints.isNotEmpty()) {
            val count = waypoints.size
            var closestIdx = 0
            var minDistSq = Float.MAX_VALUE
            val px = playerVehicle.position.x
            val pz = playerVehicle.position.z
            for (i in 0 until count) {
                val wp = waypoints[i]
                val dx = px - wp.x
                val dz = pz - wp.z
                val d2 = dx * dx + dz * dz
                if (d2 < minDistSq) {
                    minDistSq = d2
                    closestIdx = i
                }
            }
            (closestIdx.toFloat() / count.toFloat()) * 100f
        } else 0f

        _telemetry.value = RaceTelemetryUiState(
            speedKmh = playerVehicle.speedKmh,
            rpm = playerVehicle.powertrain.currentRpm,
            maxRpm = playerVehicle.powertrain.redlineRpm,
            gear = playerVehicle.powertrain.getGearDisplayName(),
            gForceLat = playerVehicle.gForceLateral,
            gForceLong = playerVehicle.gForceLongitudinal,
            driftAngleDeg = playerVehicle.driftAngleDeg,
            isOffRoad = playerVehicle.isOffRoad,
            currentLap = timing.currentLap,
            totalLaps = timing.totalLaps,
            currentLapTime = timing.formatTime(timing.currentLapTimeSec),
            bestLapTime = timing.formatTime(timing.bestLapTimeSec),
            lastLapTime = timing.formatTime(timing.lastLapTimeSec),
            sectorDelta = sectorDeltaText,
            sectorDeltaPositive = sectorDeltaPositive,
            currentSector = currentSectorIndex,
            racePosition = computeRacePosition(),
            totalRacers = 1 + aiOpponents.size,
            countdownNumber = timing.countdownSec.toInt() + 1,
            isCountdownActive = timing.isCountdownActive,
            boostAvailable = boostEnergy > 20f,
            isPaused = _isGamePaused.value,
            boostPercent = boostEnergy,
            isAbsActive = absActive,
            isTcsActive = tcsActive,
            isRevLimiter = rpmRatio > 0.90f,
            trackProgressPercent = lapProgress,
            activeCameraView = _cameraView.value
        )
    }

    fun setSteerInput(steer: Float) {
        playerVehicle.steerInput = steer * dataManager.steerSensitivity
    }

    fun setThrottle(throttle: Float) {
        if (raceTiming.isCountdownActive) return
        playerVehicle.throttle = throttle
    }

    fun setBrake(brake: Float) {
        if (raceTiming.isCountdownActive) return
        playerVehicle.brake = brake
    }

    fun setHandbrake(active: Boolean) {
        if (raceTiming.isCountdownActive) return
        playerVehicle.handbrake = active
    }

    fun triggerBoost(active: Boolean) {
        if (raceTiming.isCountdownActive) return
        if (boostEnergy > 5f) {
            playerVehicle.boostActive = active
        } else {
            playerVehicle.boostActive = false
        }
    }

    fun resetDriverInputs() {
        playerVehicle.throttle = 0f
        playerVehicle.brake = 0f
        playerVehicle.steerInput = 0f
        playerVehicle.handbrake = false
        playerVehicle.boostActive = false
        playerVehicle.reverseHoldTimer = 0f
    }

    fun toggleCamera() {
        val next = when (_cameraView.value) {
            CameraView.CHASE_NEAR -> CameraView.CHASE_FAR
            CameraView.CHASE_FAR -> CameraView.HOOD_BUMPER
            CameraView.HOOD_BUMPER -> CameraView.COCKPIT
            CameraView.COCKPIT -> CameraView.CHASE_NEAR
        }
        _cameraView.value = next
        renderer?.currentCameraView = next
        updateTelemetryState()
    }

    fun openGarage() {
        navigateTo(GameScreen.GARAGE)
        renderer?.resetShowroomCamera()
    }

    fun selectCar(car: CarDefinition) {
        _selectedCar.value = car
        dataManager.selectedCarId = car.id
        _carUpgrades.value = dataManager.getCarUpgrades(car.id)
        _carColorIndex.value = dataManager.getCarColorIndex(car.id)
        _carPaintFinish.value = dataManager.getCarPaintFinish(car.id)
        applyCarConfiguration(car)
        renderer?.setCar(car.id)
        renderer?.playerPaintColor = getActiveColor()
        renderer?.playerPaintFinish = _carPaintFinish.value
    }

    fun buySelectedCar(): Boolean {
        val car = _selectedCar.value
        if (dataManager.unlockedCars.contains(car.id)) {
            return true // Already owned, prevent double charge
        }
        val cost = car.basePrice
        if (dataManager.canAfford(cost)) {
            if (dataManager.deductCredits(cost)) {
                _playerCredits.value = dataManager.credits
                val updated = dataManager.unlockedCars + car.id
                dataManager.unlockedCars = updated
                _unlockedCarIds.value = updated
                return true
            }
        }
        return false
    }

    fun selectCarColor(colorIdx: Int) {
        val car = _selectedCar.value
        if (colorIdx in car.availableColors.indices) {
            _carColorIndex.value = colorIdx
            dataManager.setCarColorIndex(car.id, colorIdx)
            renderer?.playerPaintColor = car.availableColors[colorIdx].second
        }
    }

    fun selectCarPaintFinish(finish: com.example.racing.engine.PaintFinishType) {
        val car = _selectedCar.value
        _carPaintFinish.value = finish
        dataManager.setCarPaintFinish(car.id, finish)
        renderer?.playerPaintFinish = finish
    }

    fun upgradePart(partType: String): Boolean {
        val car = _selectedCar.value
        val current = dataManager.getCarUpgrades(car.id)
        val cost = 3500

        if (!dataManager.canAfford(cost)) return false

        val updated = when (partType) {
            "ENGINE" -> if (current.engineStage < 3) current.copy(engineStage = current.engineStage + 1) else return false
            "TIRES" -> if (current.tireStage < 3) current.copy(tireStage = current.tireStage + 1) else return false
            "SUSPENSION" -> if (current.suspensionStage < 3) current.copy(suspensionStage = current.suspensionStage + 1) else return false
            "WEIGHT" -> if (current.weightReduction < 3) current.copy(weightReduction = current.weightReduction + 1) else return false
            else -> return false
        }

        if (dataManager.deductCredits(cost)) {
            _playerCredits.value = dataManager.credits
            dataManager.saveCarUpgrades(car.id, updated)
            _carUpgrades.value = updated
            applyCarConfiguration(car)
            return true
        }
        return false
    }

    // Phase 11 — 3D Showroom Camera Controls
    fun resetShowroomCamera() {
        renderer?.resetShowroomCamera()
    }

    fun toggleAutoRotate(): Boolean {
        return renderer?.toggleAutoRotate() ?: false
    }

    fun setCameraPreset(preset: String) {
        renderer?.setCameraPreset(preset)
    }

    // Phase 11 — Derived Telemetry and Performance Metrics
    fun getVehicleTopSpeed(car: CarDefinition, upgrades: CarUpgradeState): Float {
        return car.topSpeedKmh + (upgrades.engineStage * 6.5f) + (upgrades.weightReduction * 3.0f)
    }

    fun getVehicleTorque(car: CarDefinition, upgrades: CarUpgradeState): Float {
        return car.baseTorqueNm + (upgrades.engineStage * 65f)
    }

    fun getVehicleWeight(car: CarDefinition, upgrades: CarUpgradeState): Float {
        return (car.massKg - upgrades.weightReduction * 55f).coerceAtLeast(950f)
    }

    fun getVehicleGrip(upgrades: CarUpgradeState): Float {
        return 1.25f + (upgrades.tireStage * 0.12f)
    }

    fun getVehicleZeroToHundred(car: CarDefinition, upgrades: CarUpgradeState): Float {
        val base0100 = when (car.id) {
            "apex_monza_v10" -> 2.6f
            "apex_spirit_86" -> 4.4f
            else -> 3.1f
        }
        return (base0100 - (upgrades.engineStage * 0.18f) - (upgrades.weightReduction * 0.10f) - (upgrades.tireStage * 0.15f)).coerceAtLeast(1.9f)
    }

    fun navigateTo(screen: GameScreen) {
        resetDriverInputs()
        _currentScreen.value = screen
        when (screen) {
            GameScreen.MAIN_MENU -> {
                renderer?.isShowroomMode = true
                renderer?.isMainMenuMode = true
                renderer?.autoRotateShowroom = true
            }
            GameScreen.GARAGE -> {
                renderer?.isShowroomMode = true
                renderer?.isMainMenuMode = false
                renderer?.autoRotateShowroom = false
                renderer?.setCar(_selectedCar.value.id)
                renderer?.playerPaintColor = getActiveColor()
                renderer?.playerPaintFinish = _carPaintFinish.value
            }
            else -> {
                renderer?.isShowroomMode = false
                renderer?.isMainMenuMode = false
                renderer?.autoRotateShowroom = false
            }
        }
    }

    fun setControlType(type: ControlType) {
        _controlType.value = type
        dataManager.controlType = type
    }

    fun setGraphicsPreset(preset: String) {
        _graphicsPreset.value = preset
        dataManager.graphicsPreset = preset
        renderer?.graphicsQuality = preset
    }

    fun setTimeOfDay(tod: com.example.racing.model.TimeOfDay) {
        _timeOfDay.value = tod
        dataManager.timeOfDay = tod
        renderer?.timeOfDay = tod
    }

    fun setWeather(weather: com.example.racing.model.WeatherCondition) {
        _weatherCondition.value = weather
        dataManager.weatherCondition = weather
        renderer?.weatherCondition = weather
    }

    fun cycleTimeOfDay() {
        val all = com.example.racing.model.TimeOfDay.values()
        val next = all[(_timeOfDay.value.ordinal + 1) % all.size]
        setTimeOfDay(next)
    }

    fun cycleWeather() {
        val all = com.example.racing.model.WeatherCondition.values()
        val next = all[(_weatherCondition.value.ordinal + 1) % all.size]
        setWeather(next)
    }

    fun setSoundVolume(volume: Float) {
        dataManager.soundVolume = volume
        audioEngine.masterVolume = volume
    }

    fun toggleMute(muted: Boolean) {
        dataManager.soundMuted = muted
        audioEngine.isMuted = muted
    }

    // =============================================================================================
    // DEVELOPMENT & DEBUG MODE
    // =============================================================================================
    fun grantDevelopmentFunds(amount: Int = 500000) {
        dataManager.credits += amount
        _playerCredits.value = dataManager.credits
    }

    fun setUnlimitedFunds() {
        dataManager.credits = 9999999
        _playerCredits.value = dataManager.credits
    }

    fun unlockAllCarsForDevelopment() {
        val allIds = CarDefinition.ALL_CARS.map { it.id }.toSet()
        dataManager.unlockedCars = allIds
        _unlockedCarIds.value = allIds
    }

    fun pauseRace() {
        _isGamePaused.value = true
        resetDriverInputs()
        audioEngine.throttleLoad = 0f
        audioEngine.tireSlip = 0f
        audioEngine.turboBoost = 0f
        audioEngine.stop()
        updateTelemetryState()
    }

    fun resumeRace() {
        _isGamePaused.value = false
        audioEngine.isMuted = dataManager.soundMuted
        audioEngine.masterVolume = dataManager.soundVolume
        audioEngine.start()
        updateTelemetryState()
    }

    fun restartRace() {
        _isGamePaused.value = false
        resetDriverInputs()
        startRace(_activeRaceMode.value)
    }

    fun setCameraView(view: CameraView) {
        _cameraView.value = view
        renderer?.currentCameraView = view
        updateTelemetryState()
    }

    fun onPause() {
        _isGamePaused.value = true
        resetDriverInputs()
        audioEngine.stop()
        updateTelemetryState()
    }

    fun onResume() {
        if (_currentScreen.value != GameScreen.RACE) {
            _isGamePaused.value = false
        }
        audioEngine.isMuted = dataManager.soundMuted
        audioEngine.masterVolume = dataManager.soundVolume
        audioEngine.start()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        renderer?.release()
    }
}
