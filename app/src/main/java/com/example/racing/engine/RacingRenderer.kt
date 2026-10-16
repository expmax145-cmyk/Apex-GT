package com.example.racing.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.example.racing.ai.OpponentAIController
import com.example.racing.model.CameraView
import com.example.racing.model.TrackId
import com.example.racing.physics.VehiclePhysics
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class RacingRenderer(
    var playerVehicle: VehiclePhysics,
    val aiOpponents: List<OpponentAIController>
) : GLSurfaceView.Renderer {

    var isShowroomMode: Boolean = false
    var isMainMenuMode: Boolean = false
    var showroomOrbitAngleDeg: Float = 45f
    var showroomElevationDeg: Float = 16f
    var showroomZoomDistance: Float = 5.4f
    var autoRotateShowroom: Boolean = false

    fun resetShowroomCamera() {
        showroomOrbitAngleDeg = 45f
        showroomElevationDeg = 16f
        showroomZoomDistance = 5.4f
    }

    fun toggleAutoRotate(): Boolean {
        autoRotateShowroom = !autoRotateShowroom
        return autoRotateShowroom
    }

    fun setCameraPreset(preset: String) {
        when (preset) {
            "HERO_3_4" -> {
                showroomOrbitAngleDeg = 45f
                showroomElevationDeg = 16f
                showroomZoomDistance = 5.4f
            }
            "SIDE" -> {
                showroomOrbitAngleDeg = 90f
                showroomElevationDeg = 12f
                showroomZoomDistance = 5.6f
            }
            "REAR_AERO" -> {
                showroomOrbitAngleDeg = 155f
                showroomElevationDeg = 20f
                showroomZoomDistance = 5.2f
            }
            "FRONT_DETAIL" -> {
                showroomOrbitAngleDeg = 15f
                showroomElevationDeg = 14f
                showroomZoomDistance = 4.2f
            }
        }
    }

    var currentCameraView: CameraView = CameraView.CHASE_NEAR
    var playerPaintColor: FloatArray = floatArrayOf(0.95f, 0.15f, 0.20f, 1.0f)
    var playerPaintFinish: PaintFinishType = PaintFinishType.METALLIC
    var graphicsQuality: String = "HIGH"
    var timeOfDay: com.example.racing.model.TimeOfDay = com.example.racing.model.TimeOfDay.DAY
    var weatherCondition: com.example.racing.model.WeatherCondition = com.example.racing.model.WeatherCondition.CLEAR

    private var currentCarId: String = "apex_valkyrie"
    private var currentTrackId: TrackId = TrackId.GRAND_PRIX

    @Volatile private var pendingCarId: String? = null
    @Volatile private var pendingTrackId: TrackId? = null

    private var shader: Shader? = null
    val carModels = mutableMapOf<String, CarMeshBuilder.CarModel>()
    var playerCarModel: CarMeshBuilder.CarModel? = null
        private set
    var trackModel: TrackMeshBuilder.TrackModel? = null
        private set
    var showroomModel: ShowroomMeshBuilder.ShowroomModel? = null
        private set
    var particleSystem: ParticleSystem? = null
        private set

    private val projectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelMatrix = Matrix4f()
    private val vpMatrix = FloatArray(16)
    private val reusableWheelMatrix = Matrix4f()

    private val currentCamPos = Vector3f(0f, 2.5f, -6f)
    private val currentCamTarget = Vector3f(0f, 1f, 0f)
    private val targetCamPos = Vector3f()
    private val targetLookAt = Vector3f()
    private var currentFov: Float = 60f
    var cameraShakeIntensity: Float = 0f
    private var cameraShakeTime: Float = 0f

    private var cachedPlayerMaterial: Material? = null
    private var cachedPlayerPaintColor: FloatArray? = null
    private var cachedPlayerFinish: PaintFinishType? = null

    private fun getPlayerMaterial(): Material {
        val currentColor = playerPaintColor
        val currentFinish = playerPaintFinish
        val cached = cachedPlayerMaterial
        if (cached == null ||
            cachedPlayerFinish != currentFinish ||
            cachedPlayerPaintColor == null ||
            !cachedPlayerPaintColor.contentEquals(currentColor)
        ) {
            val newMat = currentFinish.createMaterial(currentColor)
            cachedPlayerMaterial = newMat
            cachedPlayerPaintColor = currentColor.clone()
            cachedPlayerFinish = currentFinish
            return newMat
        }
        return cached
    }

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var isGlInitialized = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.10f, 0.12f, 0.18f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Clean any stale references
        shader?.release()
        carModels.values.forEach { it.release() }
        carModels.clear()
        trackModel?.release()
        particleSystem?.release()
        showroomModel?.release()

        shader = Shader(Shader.DEFAULT_VERTEX_SHADER, Shader.DEFAULT_FRAGMENT_SHADER)

        // Cache all 3 vehicle models to enable multi-car diverse grid
        carModels["apex_valkyrie"] = CarMeshBuilder.buildCarModel("apex_valkyrie")
        carModels["apex_spirit_86"] = CarMeshBuilder.buildCarModel("apex_spirit_86")
        carModels["apex_monza_v10"] = CarMeshBuilder.buildCarModel("apex_monza_v10")

        playerCarModel = carModels[currentCarId] ?: carModels["apex_valkyrie"]
        trackModel = TrackMeshBuilder.buildTrack(currentTrackId)
        showroomModel = ShowroomMeshBuilder.buildShowroomModel()
        particleSystem = ParticleSystem()

        isGlInitialized = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)

        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        projectionMatrix.identity().perspective(62f, aspect, 0.2f, 1200f)
    }

    fun setCar(carId: String) {
        currentCarId = carId
        pendingCarId = carId
    }

    fun setTrack(trackId: TrackId) {
        currentTrackId = trackId
        pendingTrackId = trackId
    }

    override fun onDrawFrame(gl: GL10?) {
        // Safe thread synchronization: Handle pending model swaps on GL thread
        pendingCarId?.let { newCarId ->
            playerCarModel = carModels[newCarId] ?: carModels["apex_valkyrie"]
            pendingCarId = null
        }
        pendingTrackId?.let { newTrackId ->
            trackModel?.release()
            trackModel = TrackMeshBuilder.buildTrack(newTrackId)
            pendingTrackId = null
        }

        val activeShader = shader ?: return
        val activeCar = playerCarModel ?: return
        val activeTrack = trackModel ?: return

        // 1. Lighting & Environment Setup
        if (isShowroomMode) {
            // Dedicated 3D Showroom Automotive Studio Presentation
            GLES30.glClearColor(0.045f, 0.050f, 0.065f, 1.0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

            activeShader.use()

            // 3-Point Automotive Studio Key & Fill Lighting
            activeShader.setVec3("u_SunDirection", -0.45f, 0.85f, 0.55f)
            activeShader.setVec3("u_SunColor", 1.25f, 1.25f, 1.30f)
            activeShader.setVec3("u_SkyAmbient", 0.32f, 0.35f, 0.42f)
            activeShader.setVec3("u_GroundAmbient", 0.14f, 0.16f, 0.20f)
            activeShader.setVec3("u_FogColor", 0.045f, 0.050f, 0.065f)
            activeShader.setFloat("u_FogNear", 35f)
            activeShader.setFloat("u_FogFar", 95f)
            activeShader.setFloat("u_WetRoadActive", 0.0f)
            activeShader.setFloat("u_HeadlightIntensity", 0.0f)

            if (autoRotateShowroom) {
                val rotateSpeed = if (isMainMenuMode) 0.18f else 0.30f
                showroomOrbitAngleDeg = (showroomOrbitAngleDeg + rotateSpeed) % 360f
            }
            setupShowroomCamera()
        } else {
            // Race Track Lighting & Camera
            val lighting = activeTrack.lightingConfig
            GLES30.glClearColor(lighting.fogColor[0], lighting.fogColor[1], lighting.fogColor[2], 1.0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

            activeShader.use()

            activeShader.setVec3("u_SunDirection", lighting.sunDirection[0], lighting.sunDirection[1], lighting.sunDirection[2])
            activeShader.setVec3("u_SunColor", lighting.sunColor[0], lighting.sunColor[1], lighting.sunColor[2])
            activeShader.setVec3("u_SkyAmbient", lighting.skyAmbient[0], lighting.skyAmbient[1], lighting.skyAmbient[2])
            activeShader.setVec3("u_GroundAmbient", lighting.groundAmbient[0], lighting.groundAmbient[1], lighting.groundAmbient[2])
            activeShader.setVec3("u_FogColor", lighting.fogColor[0], lighting.fogColor[1], lighting.fogColor[2])
            activeShader.setFloat("u_FogNear", lighting.fogNear)
            activeShader.setFloat("u_FogFar", lighting.fogFar)

            val wetFactor = when {
                !lighting.isWetRoad -> 0.0f
                graphicsQuality == "LOW" -> 0.0f
                graphicsQuality == "MEDIUM" -> 0.65f
                else -> 1.0f
            }
            activeShader.setFloat("u_WetRoadActive", wetFactor)

            val enableHeadlights = graphicsQuality != "LOW" &&
                (lighting.isWetRoad || activeTrack.trackId == TrackId.NEON_METROPOLIS)

            if (enableHeadlights) {
                val pHead = playerVehicle.headingAngleRad
                val forwardX = sin(pHead)
                val forwardZ = cos(pHead)
                activeShader.setVec3(
                    "u_HeadlightPos",
                    playerVehicle.position.x + forwardX * 1.8f,
                    playerVehicle.position.y + 0.45f,
                    playerVehicle.position.z + forwardZ * 1.8f
                )
                activeShader.setVec3("u_HeadlightDir", forwardX, -0.08f, forwardZ)
                activeShader.setVec3("u_HeadlightColor", 0.95f, 0.98f, 1.0f)
                val hlIntensity = if (graphicsQuality == "HIGH") 1.8f else 1.1f
                activeShader.setFloat("u_HeadlightIntensity", hlIntensity)
            } else {
                activeShader.setFloat("u_HeadlightIntensity", 0.0f)
            }

            setupRacingCamera()
        }

        activeShader.setMat4("u_ProjectionMatrix", projectionMatrix.values)
        activeShader.setMat4("u_ViewMatrix", viewMatrix.values)
        activeShader.setVec3("u_CameraPos", currentCamPos.x, currentCamPos.y, currentCamPos.z)

        // 2. Render Environment (Showroom or Track)
        if (isShowroomMode) {
            showroomModel?.let { sm ->
                modelMatrix.identity()
                activeShader.setMat4("u_ModelMatrix", modelMatrix.values)

                activeShader.setMaterial(Material.SHOWROOM_FLOOR)
                sm.floorMesh.draw()

                activeShader.setMaterial(Material.SHOWROOM_TURNTABLE)
                sm.turntableMesh.draw()

                activeShader.setMaterial(Material.SHOWROOM_LED_RING)
                sm.ledRingMesh.draw()

                activeShader.setMaterial(Material.SHOWROOM_STUDIO_COVE)
                sm.studioCoveMesh.draw()

                activeShader.setMaterial(Material.SHOWROOM_OVERHEAD_LIGHT)
                sm.overheadLightsMesh.draw()

                activeShader.setMaterial(Material.CONTACT_SHADOW)
                sm.contactShadowMesh.draw()
            }
        } else {
            // 2.1 Panoramic Sky Dome (Centered on Camera position, unlit panoramic horizon)
            GLES30.glDisable(GLES30.GL_CULL_FACE)
            GLES30.glDepthMask(false)
            modelMatrix.identity().translate(currentCamPos.x, currentCamPos.y, currentCamPos.z)
            activeShader.setMat4("u_ModelMatrix", modelMatrix.values)
            activeShader.setMaterial(Material.SKY_DOME)
            activeTrack.skyMesh.draw()
            GLES30.glDepthMask(true)
            GLES30.glEnable(GLES30.GL_CULL_FACE)

            modelMatrix.identity()
            activeShader.setMat4("u_ModelMatrix", modelMatrix.values)

            // Terrain Scenery & Trackside Environment
            activeShader.setMaterial(Material.ENVIRONMENT_SCENERY)
            activeTrack.environmentMesh.draw()

            // Asphalt Road
            activeShader.setMaterial(if (activeTrack.lightingConfig.isWetRoad) Material.ASPHALT_WET else Material.ASPHALT_DRY)
            activeTrack.roadMesh.draw()

            // Apex Kerbs
            activeShader.setMaterial(Material.KERB_RED_WHITE)
            activeTrack.kerbMesh.draw()

            // Barriers & Gantry
            activeShader.setMaterial(Material.BARRIER_ARMCO)
            activeTrack.barrierMesh.draw()
            activeShader.setMaterial(Material.ALLOY_RIM)
            activeTrack.gantryMesh.draw()
        }

        // 3. Render Player Vehicle & Wheels
        val playerMaterial = getPlayerMaterial()
        renderVehicle(
            vehicle = playerVehicle,
            car = activeCar,
            shader = activeShader,
            carMaterial = playerMaterial
        )

        // 4. Render AI Opponents with their specific car models and finishes
        if (!isShowroomMode) {
            for (ai in aiOpponents) {
                val aiCarModel = carModels[ai.profile.carId] ?: activeCar
                val aiMaterial = ai.cachedMaterial ?: run {
                    val aiFinish = when (ai.profile.difficulty) {
                        com.example.racing.ai.AIDifficulty.EXPERT -> PaintFinishType.PEARLESCENT
                        com.example.racing.ai.AIDifficulty.MEDIUM -> PaintFinishType.METALLIC
                        else -> PaintFinishType.GLOSS
                    }
                    aiFinish.createMaterial(ai.profile.carColor).also { ai.cachedMaterial = it }
                }
                renderVehicle(
                    vehicle = ai.vehicle,
                    car = aiCarModel,
                    shader = activeShader,
                    carMaterial = aiMaterial
                )
            }
        }

        // 5. Tire Smoke, Dust & Backfire Particles
        if (!isShowroomMode && graphicsQuality != "LOW") {
            particleSystem?.let { ps ->
                // Player tire smoke / dust
                val isSlipping = playerVehicle.tireSlipRl > 0.28f || playerVehicle.tireSlipRr > 0.28f || playerVehicle.handbrake || playerVehicle.isOffRoad
                if (isSlipping && playerVehicle.speedKmh > 8f) {
                    val pPos = playerVehicle.position
                    val pHead = playerVehicle.headingAngleRad
                    val sY = sin(pHead)
                    val cY = cos(pHead)
                    val rlX = pPos.x - cY * 0.85f - sY * 1.3f
                    val rlZ = pPos.z + sY * 0.85f - cY * 1.3f
                    val rrX = pPos.x + cY * 0.85f - sY * 1.3f
                    val rrZ = pPos.z - sY * 0.85f - cY * 1.3f
                    ps.emitTireSmoke(rlX, pPos.y, rlZ, playerVehicle.isOffRoad)
                    ps.emitTireSmoke(rrX, pPos.y, rrZ, playerVehicle.isOffRoad)
                }

                // Exhaust backfire pop flame
                if (playerVehicle.powertrain.exhaustBackfireTrigger) {
                    val pPos = playerVehicle.position
                    val pHead = playerVehicle.headingAngleRad
                    val sY = sin(pHead)
                    val cY = cos(pHead)
                    val exX = pPos.x - sY * 2.1f
                    val exZ = pPos.z - cY * 2.1f
                    ps.emitExhaustFlame(exX, pPos.y + 0.28f, exZ)
                }

                // AI Opponents tire smoke (HIGH only, skipped on MEDIUM to conserve mobile fill-rate)
                if (graphicsQuality == "HIGH") {
                    for (ai in aiOpponents) {
                        if ((ai.vehicle.tireSlipRl > 0.35f || ai.vehicle.tireSlipRr > 0.35f) && ai.vehicle.speedKmh > 10f) {
                            val aPos = ai.vehicle.position
                            ps.emitTireSmoke(aPos.x, aPos.y, aPos.z, ai.vehicle.isOffRoad)
                        }
                    }
                }

                ps.update(0.016f)

                // Multiply Projection * View
                android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrix.values, 0, viewMatrix.values, 0)
                ps.draw(vpMatrix, currentCamPos)
            }
        }
    }

    private fun setupShowroomCamera() {
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)
        val fov = if (isMainMenuMode) 43f else 48f
        projectionMatrix.identity().perspective(fov, aspect, 0.2f, 1200f)

        showroomZoomDistance = showroomZoomDistance.coerceIn(3.2f, 8.5f)
        showroomElevationDeg = showroomElevationDeg.coerceIn(5f, 52f)

        val radOrbit = (showroomOrbitAngleDeg * (PI / 180.0)).toFloat()
        val radElev = (showroomElevationDeg * (PI / 180.0)).toFloat()

        val camX = showroomZoomDistance * cos(radElev) * sin(radOrbit)
        val camY = showroomZoomDistance * sin(radElev) + 0.45f
        val camZ = showroomZoomDistance * cos(radElev) * cos(radOrbit)

        currentCamPos.set(camX, camY, camZ)
        if (isMainMenuMode) {
            // Target slightly offset so vehicle is framed cleanly in the spacious right hero area
            currentCamTarget.set(-0.32f, 0.40f, 0f)
        } else {
            // Target slightly lower so vehicle sits proud in the upper viewport above the bottom dock
            currentCamTarget.set(0f, 0.28f, 0f)
        }

        viewMatrix.identity().lookAt(
            currentCamPos.x, currentCamPos.y, currentCamPos.z,
            currentCamTarget.x, currentCamTarget.y, currentCamTarget.z,
            0f, 1f, 0f
        )
    }

    private fun setupRacingCamera() {
        val carPos = playerVehicle.position
        val yawRad = playerVehicle.headingAngleRad
        val pitchRad = playerVehicle.pitchAngleRad
        val speedKmh = playerVehicle.speedKmh

        // 1. Dynamic Speed-Dependent Field of View (FOV) with boost kick
        val baseFov = when (currentCameraView) {
            CameraView.HOOD_BUMPER -> 68f
            CameraView.COCKPIT -> 63f
            CameraView.CHASE_FAR -> 64f
            CameraView.CHASE_NEAR -> 60f
        }
        val speedFovDelta = (speedKmh / 350f).coerceIn(0f, 1f) * 8.5f
        val boostFovDelta = if (playerVehicle.boostActive) 5.5f else 0f
        val targetFov = (baseFov + speedFovDelta + boostFovDelta).coerceIn(58f, 82f)
        currentFov += (targetFov - currentFov) * 0.14f

        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat().coerceAtLeast(1f)
        projectionMatrix.identity().perspective(currentFov, aspect, 0.2f, 1200f)

        // 2. Camera Positioning & Look-ahead Dynamics
        val sinYaw = sin(yawRad)
        val cosYaw = cos(yawRad)

        // Speed lag: camera gently pulls back under high acceleration / high speed
        val speedLag = (speedKmh / 350f).coerceIn(0f, 1f) * 0.75f

        // Dynamic look-ahead: looks further down track as speed climbs
        val lookaheadDist = (14f + (speedKmh * 0.085f)).coerceIn(14f, 36f)

        // Subtle steer bias pans the camera look-target into the apex of corners
        val steerBias = playerVehicle.currentSteerAngleRad * 2.2f

        // Pitch compensation for hill crests, slopes, elevation changes
        val pitchOffset = sin(pitchRad) * (if (currentCameraView == CameraView.CHASE_FAR) 2.8f else 1.8f)

        when (currentCameraView) {
            CameraView.CHASE_NEAR -> {
                targetCamPos.set(
                    carPos.x - sinYaw * (5.2f + speedLag),
                    max(carPos.y + 0.35f, carPos.y + 1.85f + speedLag * 0.18f + pitchOffset * 0.6f),
                    carPos.z - cosYaw * (5.2f + speedLag)
                )
            }
            CameraView.CHASE_FAR -> {
                targetCamPos.set(
                    carPos.x - sinYaw * (7.6f + speedLag * 1.1f),
                    max(carPos.y + 0.55f, carPos.y + 2.85f + speedLag * 0.25f + pitchOffset * 0.8f),
                    carPos.z - cosYaw * (7.6f + speedLag * 1.1f)
                )
            }
            CameraView.HOOD_BUMPER -> {
                targetCamPos.set(
                    carPos.x + sinYaw * 1.65f,
                    carPos.y + 0.68f + pitchOffset * 0.3f,
                    carPos.z + cosYaw * 1.65f
                )
            }
            CameraView.COCKPIT -> {
                val driverOffsetX = -0.22f
                val worldOffsetX = driverOffsetX * cosYaw
                val worldOffsetZ = -driverOffsetX * sinYaw
                targetCamPos.set(
                    carPos.x - sinYaw * 0.08f + worldOffsetX,
                    carPos.y + 0.96f + pitchOffset * 0.2f,
                    carPos.z - cosYaw * 0.08f + worldOffsetZ
                )
            }
        }

        when (currentCameraView) {
            CameraView.HOOD_BUMPER -> {
                val lookSin = sin(yawRad + steerBias * 0.15f)
                val lookCos = cos(yawRad + steerBias * 0.15f)
                targetLookAt.set(
                    carPos.x + lookSin * 28f,
                    carPos.y + 0.62f + sin(pitchRad) * 6f,
                    carPos.z + lookCos * 28f
                )
            }
            CameraView.COCKPIT -> {
                val driverOffsetX = -0.22f
                val worldOffsetX = driverOffsetX * cosYaw
                val worldOffsetZ = -driverOffsetX * sinYaw
                val lookSin = sin(yawRad + steerBias * 0.20f)
                val lookCos = cos(yawRad + steerBias * 0.20f)
                targetLookAt.set(
                    carPos.x + lookSin * 30f + worldOffsetX,
                    carPos.y + 0.85f + sin(pitchRad) * 7f,
                    carPos.z + lookCos * 30f + worldOffsetZ
                )
            }
            else -> {
                // Chase camera looks ahead down the track, panning slightly into turn
                val lookSin = sin(yawRad + steerBias * 0.22f)
                val lookCos = cos(yawRad + steerBias * 0.22f)
                targetLookAt.set(
                    carPos.x + lookSin * lookaheadDist,
                    carPos.y + 1.12f + sin(pitchRad) * (lookaheadDist * 0.4f),
                    carPos.z + lookCos * lookaheadDist
                )
            }
        }

        val smoothFactor = when (currentCameraView) {
            CameraView.HOOD_BUMPER -> 0.78f
            CameraView.COCKPIT -> 0.72f
            CameraView.CHASE_NEAR -> 0.24f
            CameraView.CHASE_FAR -> 0.18f
        }

        // Prevent wild camera jumps across the map when resetting or teleporting
        if (currentCamPos.distanceTo(targetCamPos) > 45f) {
            currentCamPos.set(targetCamPos)
            currentCamTarget.set(targetLookAt)
        } else {
            currentCamPos.lerpTo(targetCamPos, smoothFactor)
            currentCamTarget.lerpTo(targetLookAt, smoothFactor)
        }

        // 3. Screen Shake / Vibration Dynamics
        if (playerVehicle.isOffRoad && speedKmh > 18f) {
            cameraShakeIntensity = max(cameraShakeIntensity, 0.045f)
        }
        if (playerVehicle.hasCollidedBarrier && speedKmh > 10f) {
            cameraShakeIntensity = max(cameraShakeIntensity, (speedKmh / 150f).coerceIn(0.06f, 0.14f))
        }
        if (speedKmh > 240f || playerVehicle.boostActive) {
            cameraShakeIntensity = max(cameraShakeIntensity, 0.016f)
        }

        cameraShakeTime += 0.38f
        val shakeX = sin(cameraShakeTime * 42f) * cameraShakeIntensity
        val shakeY = cos(cameraShakeTime * 51f) * cameraShakeIntensity * 0.75f
        val shakeZ = sin(cameraShakeTime * 33f) * cameraShakeIntensity * 0.4f
        cameraShakeIntensity *= 0.86f

        viewMatrix.identity().lookAt(
            currentCamPos.x + shakeX, currentCamPos.y + shakeY, currentCamPos.z + shakeZ,
            currentCamTarget.x + shakeX * 0.5f, currentCamTarget.y + shakeY * 0.5f, currentCamTarget.z + shakeZ * 0.5f,
            0f, 1f, 0f
        )
    }

    private fun renderVehicle(
        vehicle: VehiclePhysics,
        car: CarMeshBuilder.CarModel,
        shader: Shader,
        carMaterial: Material
    ) {
        val isShowroomCar = isShowroomMode && (vehicle === playerVehicle)
        val carPos = if (isShowroomCar) Vector3f(0f, 0.35f, 0f) else vehicle.position
        val headingDeg = if (isShowroomCar) 0f else (vehicle.headingAngleRad * (180f / PI.toFloat()))
        val rollDeg = if (isShowroomCar) 0f else (vehicle.rollAngleRad * (180f / PI.toFloat()))
        val pitchDeg = if (isShowroomCar) 0f else (vehicle.pitchAngleRad * (180f / PI.toFloat()))

        // A. Contact Shadow Quad (In Showroom always draw; in race check graphicsQuality)
        val shouldDrawShadow = if (isShowroomCar) true else when (graphicsQuality) {
            "LOW" -> false
            "MEDIUM" -> (vehicle === playerVehicle)
            else -> true
        }
        if (shouldDrawShadow) {
            modelMatrix.identity()
                .translate(carPos.x, carPos.y - 0.32f, carPos.z)
                .rotate(headingDeg, 0f, 1f, 0f)
            shader.setMat4("u_ModelMatrix", modelMatrix.values)
            shader.setMaterial(Material.CONTACT_SHADOW)
            shader.setFloat("u_BrakeIntensity", 0.0f)
            car.shadowMesh.draw()
        }

        // B. Render Car Body
        modelMatrix.identity()
            .translate(carPos.x, carPos.y, carPos.z)
            .rotate(headingDeg, 0f, 1f, 0f)
            .rotate(rollDeg, 0f, 0f, 1f)
            .rotate(pitchDeg, 1f, 0f, 0f)

        shader.setMat4("u_ModelMatrix", modelMatrix.values)
        shader.setMaterial(carMaterial)

        val brakeIntensity = if (isShowroomCar) 0.0f else (if (vehicle.brake > 0.08f) vehicle.brake.coerceIn(0.6f, 1.0f) else 0.0f)
        shader.setFloat("u_BrakeIntensity", brakeIntensity)

        car.bodyMesh.draw()

        // C. Render Wheels (4 corners)
        val steerDeg = if (isShowroomCar) 8.0f else (vehicle.currentSteerAngleRad * (180f / PI.toFloat()))
        val spinDeg = if (isShowroomCar) 0.0f else (vehicle.wheelRotationAngle * (180f / PI.toFloat()))

        for (i in 0 until 4) {
            val wp = car.wheelPositions[i]
            val isFront = (i < 2)
            val isLeft = (i % 2 == 0)
            val suspComp = if (isShowroomCar) 0.0f else vehicle.suspensionCompression[i]

            reusableWheelMatrix.identity()
                .translate(carPos.x, carPos.y, carPos.z)
                .rotate(headingDeg, 0f, 1f, 0f)
                .rotate(rollDeg, 0f, 0f, 1f)
                .rotate(pitchDeg, 1f, 0f, 0f)
                .translate(wp.x, wp.y + suspComp, wp.z)

            if (isFront) {
                reusableWheelMatrix.rotate(steerDeg, 0f, 1f, 0f)
            }

            reusableWheelMatrix.rotate(spinDeg, 1f, 0f, 0f)

            if (!isLeft) {
                reusableWheelMatrix.rotate(180f, 0f, 1f, 0f)
            }

            shader.setMat4("u_ModelMatrix", reusableWheelMatrix.values)
            shader.setMaterial(Material.WHEEL_ASSEMBLY)
            car.wheelMesh.draw()
        }
    }

    fun release() {
        shader?.release()
        carModels.values.forEach { it.release() }
        carModels.clear()
        trackModel?.release()
        showroomModel?.release()
        particleSystem?.release()
        isGlInitialized = false
    }
}
