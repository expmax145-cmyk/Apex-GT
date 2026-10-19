package com.example.racing.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.CameraView
import com.example.racing.model.ControlType
import com.example.racing.model.RaceMode
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase 12 — Professional Racing HUD, Dynamic Camera Controls & Responsive Mobile Input Systems.
 * Provides high-contrast motorsport instrumentation, adaptive landscape layout, multi-touch driving controls,
 * dynamic assistance feedback (ABS, TCS, Rev-Limiter), and an in-game pause & setup menu overlay.
 */
@Composable
fun RacingHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val raceMode by viewModel.activeRaceMode.collectAsState()
    val controlType by viewModel.controlType.collectAsState()
    val activeTrack by viewModel.selectedTrack.collectAsState()
    val activeCar by viewModel.selectedCar.collectAsState()
    val isGamePaused by viewModel.isGamePausedState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Multi-touch driver inputs
    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }
    var isGasPressed by remember { mutableStateOf(false) }
    var isBrakePressed by remember { mutableStateOf(false) }
    var isHandbrakePressed by remember { mutableStateOf(false) }
    var isBoostPressed by remember { mutableStateOf(false) }
    var touchSteerSmooth by remember { mutableFloatStateOf(0f) }

    // Virtual steering wheel animated state
    val wheelAnimatable = remember { Animatable(0f) }
    var rawWheelAngle by remember { mutableFloatStateOf(0f) }

    // Accelerometer Sensor for TILT_GYRO mode with deadzone & low-pass filtering
    var tiltRawValue by remember { mutableFloatStateOf(0f) }
    var tiltFilteredValue by remember { mutableFloatStateOf(0f) }

    // Ensure driver inputs are cleared if HUD leaves composition
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetDriverInputs()
        }
    }

    DisposableEffect(controlType) {
        if (controlType == ControlType.TILT_GYRO) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.values.size >= 2) {
                        // Landscape orientation: Y-axis represents roll/tilt
                        val tiltY = event.values[1]
                        val rawNormalized = (tiltY / 4.6f).coerceIn(-1.0f, 1.0f)
                        // Deadzone threshold (0.05f) prevents twitching on hand tremor
                        val withDeadzone = if (abs(rawNormalized) < 0.05f) 0f else {
                            if (rawNormalized > 0f) (rawNormalized - 0.05f) / 0.95f else (rawNormalized + 0.05f) / 0.95f
                        }
                        tiltRawValue = withDeadzone
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            if (sm != null && accel != null) {
                sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose {
                sm?.unregisterListener(listener)
            }
        } else {
            tiltRawValue = 0f
            tiltFilteredValue = 0f
            onDispose { }
        }
    }

    // Clean input reset hooks to prevent latched/stale state across pauses, restarts, and mode changes
    LaunchedEffect(controlType) {
        isLeftPressed = false
        isRightPressed = false
        touchSteerSmooth = 0f
        rawWheelAngle = 0f
        wheelAnimatable.snapTo(0f)
        tiltRawValue = 0f
        tiltFilteredValue = 0f
        viewModel.setSteerInput(0f)
    }

    LaunchedEffect(isGamePaused) {
        if (isGamePaused) {
            isLeftPressed = false
            isRightPressed = false
            isGasPressed = false
            isBrakePressed = false
            isHandbrakePressed = false
            isBoostPressed = false
            touchSteerSmooth = 0f
            rawWheelAngle = 0f
            wheelAnimatable.snapTo(0f)
            viewModel.resetDriverInputs()
        }
    }

    LaunchedEffect(telemetry.isCountdownActive) {
        if (telemetry.isCountdownActive) {
            isLeftPressed = false
            isRightPressed = false
            isGasPressed = false
            isBrakePressed = false
            isHandbrakePressed = false
            isBoostPressed = false
            touchSteerSmooth = 0f
            rawWheelAngle = 0f
            wheelAnimatable.snapTo(0f)
            viewModel.resetDriverInputs()
        }
    }

    // Low-pass filter for tilt steering
    tiltFilteredValue += (tiltRawValue - tiltFilteredValue) * 0.24f
    if (abs(tiltFilteredValue) < 0.005f && tiltRawValue == 0f) tiltFilteredValue = 0f

    // Touch button steering ramp & opposing input cancellation
    // Consistent Convention: Left = positive steer (+1.0f), Right = negative steer (-1.0f), Neutral = 0.0f
    val touchTarget = when {
        isLeftPressed && !isRightPressed -> 1.0f   // Pressing LEFT steers car left (+1.0f steer, +X world velocity, positive yaw)
        isRightPressed && !isLeftPressed -> -1.0f  // Pressing RIGHT steers car right (-1.0f steer, -X world velocity, negative yaw)
        else -> 0.0f // Opposing inputs or release cleanly resolve to neutral 0.0f
    }
    val rampSpeed = if (touchTarget == 0f) 0.42f else 0.30f
    touchSteerSmooth += (touchTarget - touchSteerSmooth) * rampSpeed
    if (abs(touchSteerSmooth) < 0.01f && touchTarget == 0f) touchSteerSmooth = 0f

    // Compute steer input for vehicle physics based on active control type
    val steerValue = when (controlType) {
        // Dragging wheel left (-angle) steers left (+1.0f); dragging wheel right (+angle) steers right (-1.0f)
        ControlType.VIRTUAL_WHEEL -> (-wheelAnimatable.value / 60f).coerceIn(-1.0f, 1.0f)
        ControlType.TILT_GYRO -> tiltFilteredValue
        ControlType.TOUCH_BUTTONS -> touchSteerSmooth
    }

    // Apply driver inputs to ViewModel only if game is not paused
    if (!isGamePaused) {
        viewModel.setSteerInput(steerValue)
        viewModel.setThrottle(if (isGasPressed) 1.0f else 0.0f)
        viewModel.setBrake(if (isBrakePressed) 1.0f else 0.0f)
        viewModel.setHandbrake(isHandbrakePressed)
        viewModel.triggerBoost(isBoostPressed)
    }

    // Camera switch toast feedback
    var lastCameraMode by remember { mutableStateOf(telemetry.activeCameraView) }
    var cameraToastVisible by remember { mutableStateOf(false) }

    LaunchedEffect(telemetry.activeCameraView) {
        if (telemetry.activeCameraView != lastCameraMode) {
            lastCameraMode = telemetry.activeCameraView
            cameraToastVisible = true
            kotlinx.coroutines.delay(1600)
            cameraToastVisible = false
        }
    }

    // Safe insets padding for notch, status bar, gesture navigation
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(safeInsets)
    ) {
        val isNarrow = maxWidth < 640.dp
        val isCompact = maxWidth < 760.dp
        val isShort = maxHeight < 390.dp

        val hPadding = if (isNarrow) 10.dp else if (isCompact) 14.dp else 22.dp
        val vPadding = if (isShort) 6.dp else 10.dp

        // Adaptive pedal dimensions
        val pedalSpacing = if (isNarrow) 6.dp else if (isCompact) 8.dp else 12.dp
        val gasW = if (isNarrow) 58.dp else if (isCompact) 66.dp else 76.dp
        val gasH = if (isShort) 76.dp else if (isCompact) 86.dp else 98.dp
        val brakeW = if (isNarrow) 54.dp else if (isCompact) 62.dp else 70.dp
        val brakeH = if (isShort) 66.dp else if (isCompact) 76.dp else 86.dp
        val nitroW = if (isNarrow) 48.dp else if (isCompact) 56.dp else 64.dp
        val nitroH = if (isShort) 50.dp else if (isCompact) 58.dp else 66.dp
        val eBrakeW = if (isNarrow) 48.dp else if (isCompact) 54.dp else 62.dp
        val eBrakeH = if (isShort) 48.dp else if (isCompact) 52.dp else 58.dp

        val steerBtnSize = if (isNarrow) 58.dp else if (isCompact) 66.dp else 74.dp
        val steerSpacing = if (isNarrow) 8.dp else if (isCompact) 12.dp else 16.dp
        val wheelSize = if (isNarrow) 90.dp else if (isCompact) 100.dp else 115.dp

        // ==========================================
        // 1. TOP BAR: Minimap, Timing, Pos & Camera
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPadding, vertical = vPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Top: Pause Button, Race Position & Lap Counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.pauseRace() },
                    modifier = Modifier
                        .testTag("pause_button")
                        .size(44.dp)
                        .background(Color(0x99101420), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause Race",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (raceMode == RaceMode.CIRCUIT_RACE) {
                    // Position Badge
                    Surface(
                        color = Color(0xCC0B101D),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x44FFD700))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POS",
                                color = Color(0xFF90A4AE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${telemetry.racePosition}",
                                color = Color(0xFFFFD700),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/${telemetry.totalRacers}",
                                color = Color(0xFF90A4AE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Lap Badge
                    Surface(
                        color = Color(0xCC0B101D),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LAP",
                                color = Color(0xFF90A4AE),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${telemetry.currentLap}/${telemetry.totalLaps}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Weather and Time of Day indicator badge
                val timeOfDay by viewModel.timeOfDay.collectAsState()
                val weatherCondition by viewModel.weatherCondition.collectAsState()
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xCC0B101D),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x3300E5FF))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeOfDay.name,
                            color = Color(0xFFFFB300),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = weatherCondition.name,
                            color = Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Center Top: Track Radar Minimap with Progress Bar
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MinimapRadar(
                    viewModel = viewModel,
                    modifier = Modifier
                        .size(width = if (isNarrow) 120.dp else 145.dp, height = if (isShort) 66.dp else 78.dp)
                        .background(Color(0xB3070A12), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                )
                // Track Progress indicator line
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { (telemetry.trackProgressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(if (isNarrow) 120.dp else 145.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFFFD700),
                    trackColor = Color(0x44222D3D)
                )
            }

            // Right Top: Lap Timing, Sector Splits & Camera Toggle Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xCC0B101D),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = telemetry.currentLapTime,
                                color = Color(0xFF00E5FF),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (telemetry.sectorDelta.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = telemetry.sectorDelta,
                                    color = if (telemetry.sectorDeltaPositive) Color(0xFFFF5252) else Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row {
                            Text(
                                text = "BEST ",
                                color = Color(0xFF90A4AE),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = telemetry.bestLapTime,
                                color = Color(0xFF81C784),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Camera Switch Button with active mode indicator
                Surface(
                    onClick = { viewModel.toggleCamera() },
                    color = Color(0x99101420),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    modifier = Modifier.testTag("camera_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Switch Camera",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val camLabel = when (telemetry.activeCameraView) {
                            CameraView.CHASE_NEAR -> "CHASE"
                            CameraView.CHASE_FAR -> "FAR"
                            CameraView.HOOD_BUMPER -> "HOOD"
                            CameraView.COCKPIT -> "COCKPIT"
                        }
                        Text(
                            text = camLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. DYNAMIC IN-RACE NOTIFICATION BANNERS
        // ==========================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isShort) 80.dp else 94.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Camera mode toast
            AnimatedVisibility(
                visible = cameraToastVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    color = Color(0xEE0A101C),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CAMERA: ${telemetry.activeCameraView.name.replace('_', ' ')}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Off-track alert
            if (telemetry.isOffRoad) {
                Box(
                    modifier = Modifier
                        .background(Color(0xDDE53935), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "OFF-TRACK • LOW TRACTION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Drift score alert
            if (abs(telemetry.driftAngleDeg) > 12f) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF5722), Color(0xFFFF9800))
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DRIFT +${abs(telemetry.driftAngleDeg).toInt()}°",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // ==========================================
        // 3. INSTRUMENT CLUSTER (Bottom Center)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isShort) 6.dp else 10.dp)
        ) {
            InstrumentCluster(telemetry = telemetry, controlType = controlType)
        }

        // ==========================================
        // 4. LEFT BOTTOM CONTROLS (Steering)
        // ==========================================
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = hPadding, bottom = if (isShort) 8.dp else 14.dp)
        ) {
            when (controlType) {
                ControlType.TOUCH_BUTTONS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(steerSpacing)) {
                        SteerButton(
                            label = "◀",
                            isPressed = isLeftPressed,
                            onPressChange = { isLeftPressed = it },
                            modifier = Modifier
                                .testTag("steer_left_button")
                                .size(steerBtnSize)
                        )

                        SteerButton(
                            label = "▶",
                            isPressed = isRightPressed,
                            onPressChange = { isRightPressed = it },
                            modifier = Modifier
                                .testTag("steer_right_button")
                                .size(steerBtnSize)
                        )
                    }
                }
                ControlType.VIRTUAL_WHEEL -> {
                    VirtualSteeringWheel(
                        wheelAngle = wheelAnimatable.value,
                        onDragAngle = { delta ->
                            rawWheelAngle = (rawWheelAngle + delta).coerceIn(-65f, 65f)
                            scope.launch { wheelAnimatable.snapTo(rawWheelAngle) }
                        },
                        onRelease = {
                            rawWheelAngle = 0f
                            scope.launch {
                                wheelAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        modifier = Modifier
                            .testTag("virtual_wheel_control")
                            .size(wheelSize)
                    )
                }
                ControlType.TILT_GYRO -> {
                    TiltIndicator(
                        steerValue = tiltFilteredValue,
                        modifier = Modifier
                            .testTag("tilt_gyro_indicator")
                            .size(width = if (isNarrow) 95.dp else 115.dp, height = if (isShort) 44.dp else 52.dp)
                    )
                }
            }
        }

        // ==========================================
        // 5. RIGHT BOTTOM CONTROLS (Pedals & Actions)
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = hPadding, bottom = if (isShort) 8.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(pedalSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            PedalButton(
                label = "E-BRAKE",
                color = Color(0xFF6D4C41),
                isPressed = isHandbrakePressed,
                onPressChange = { isHandbrakePressed = it },
                modifier = Modifier
                    .testTag("handbrake_button")
                    .size(width = eBrakeW, height = eBrakeH)
            )

            PedalButton(
                label = "NITRO",
                color = if (telemetry.boostAvailable) Color(0xFF00B0FF) else Color(0xFF37474F),
                isPressed = isBoostPressed,
                onPressChange = { isBoostPressed = it },
                modifier = Modifier
                    .testTag("nitro_button")
                    .size(width = nitroW, height = nitroH)
            )

            PedalButton(
                label = "BRAKE",
                color = Color(0xFFD32F2F),
                isPressed = isBrakePressed,
                onPressChange = { isBrakePressed = it },
                modifier = Modifier
                    .testTag("brake_pedal")
                    .size(width = brakeW, height = brakeH)
            )

            PedalButton(
                label = "GAS",
                color = Color(0xFF2E7D32),
                isPressed = isGasPressed,
                onPressChange = { isGasPressed = it },
                modifier = Modifier
                    .testTag("gas_pedal")
                    .size(width = gasW, height = gasH)
            )
        }

        // ==========================================
        // 6. START COUNTDOWN OVERLAY (3, 2, 1, GO!)
        // ==========================================
        AnimatedVisibility(
            visible = telemetry.isCountdownActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val count = telemetry.countdownNumber
            val text = if (count > 0) count.toString() else "GO!"
            val color = if (count > 0) Color(0xFFFF3D00) else Color(0xFF00E676)

            Surface(
                color = Color(0xDD0A0E18),
                shape = CircleShape,
                border = BorderStroke(4.dp, color),
                modifier = Modifier.size(130.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        color = color,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // ==========================================
        // 7. IN-GAME PAUSE & SETTINGS MENU OVERLAY
        // ==========================================
        if (isGamePaused) {
            PauseMenuOverlay(
                viewModel = viewModel,
                telemetry = telemetry,
                trackName = activeTrack.title,
                carName = activeCar.name,
                controlType = controlType,
                activeCamera = telemetry.activeCameraView
            )
        }
    }
}

/**
 * In-game Pause Menu Overlay. Halts physics, displays stats, and allows instant on-the-fly
 * control scheme switching and camera selection.
 */
@Composable
fun PauseMenuOverlay(
    viewModel: GameViewModel,
    telemetry: RaceTelemetryUiState,
    trackName: String,
    carName: String,
    controlType: ControlType,
    activeCamera: CameraView
) {
    // Intercept back button to resume race
    BackHandler {
        viewModel.resumeRace()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9060912))
            .clickable(enabled = false) {}, // Absorbs clicks
        contentAlignment = Alignment.Center
    ) {
        val isShort = maxHeight < 400.dp
        Surface(
            color = Color(0xF20F1626),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, Color(0x4400E5FF)),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .testTag("pause_menu_modal")
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = if (isShort) 12.dp else 18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFFD700), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RACE PAUSED",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = "$trackName • $carName",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Stats Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF162035), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "POSITION", color = Color(0xFF78909C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "P${telemetry.racePosition}", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "LAP", color = Color(0xFF78909C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${telemetry.currentLap}/${telemetry.totalLaps}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "LAP TIME", color = Color(0xFF78909C), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = telemetry.currentLapTime, color = Color(0xFF00E5FF), fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Control Scheme Quick Switcher
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CONTROL SCHEME",
                        color = Color(0xFF78909C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlType.values().forEach { type ->
                            val isSelected = (controlType == type)
                            val label = when (type) {
                                ControlType.TOUCH_BUTTONS -> "BUTTONS"
                                ControlType.VIRTUAL_WHEEL -> "WHEEL"
                                ControlType.TILT_GYRO -> "GYRO"
                            }
                            Surface(
                                onClick = { viewModel.setControlType(type) },
                                color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1B2538),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF0A0F1A) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera Mode Quick Switcher
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CAMERA PERSPECTIVE",
                        color = Color(0xFF78909C),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CameraView.values().forEach { cam ->
                            val isSelected = (activeCamera == cam)
                            val label = when (cam) {
                                CameraView.CHASE_NEAR -> "CHASE"
                                CameraView.CHASE_FAR -> "FAR"
                                CameraView.HOOD_BUMPER -> "HOOD"
                                CameraView.COCKPIT -> "COCKPIT"
                            }
                            Surface(
                                onClick = { viewModel.setCameraView(cam) },
                                color = if (isSelected) Color(0xFFFFB300) else Color(0xFF1B2538),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFB300) else Color(0x33FFFFFF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFF0A0F1A) else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.resumeRace() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                            .testTag("pause_resume_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0A0F1A))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RESUME", color = Color(0xFF0A0F1A), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.restartRace() },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF9100)),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(46.dp)
                            .testTag("pause_restart_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "RESTART", color = Color(0xFFFF9100), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resumeRace()
                            viewModel.navigateTo(GameScreen.MAIN_MENU)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                        modifier = Modifier
                            .weight(1.0f)
                            .height(46.dp)
                            .testTag("pause_exit_button")
                    ) {
                        Text(text = "EXIT", color = Color(0xFFEF5350), fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Responsive steering button with active visual feedback, non-stick release, and tactile state.
 */
@Composable
fun SteerButton(
    label: String,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPressed) Color(0xFF00E5FF) else Color(0xCC1A2333)
    val textColor = if (isPressed) Color(0xFF0A0F1A) else Color.White
    val borderColor = if (isPressed) Color.White else Color(0x44FFFFFF)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isPressed) 2.5.dp else 1.dp, borderColor),
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressChange(false)
                        }
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/**
 * Virtual steering wheel with tactile drag input and auto-centering damped spring on release.
 */
@Composable
fun VirtualSteeringWheel(
    wheelAngle: Float,
    onDragAngle: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                try {
                    detectDragGestures(
                        onDragEnd = { onRelease() },
                        onDragCancel = { onRelease() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragAngle(dragAmount.x * 0.85f)
                        }
                    )
                } finally {
                    onRelease()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(wheelAngle)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Outer Grip Rim
            drawCircle(
                color = Color(0xFF1E2638),
                radius = r - 5f,
                style = Stroke(width = 13f)
            )
            // Grip accent centering stripe (Top indicator)
            drawArc(
                color = Color(0xFFFF3D00),
                startAngle = 260f,
                sweepAngle = 20f,
                useCenter = false,
                style = Stroke(width = 15f)
            )
            // Metal Horizontal & Vertical Spokes
            drawLine(
                color = Color(0xFF384357),
                start = Offset(c.x - r + 12f, c.y),
                end = Offset(c.x + r - 12f, c.y),
                strokeWidth = 9f
            )
            drawLine(
                color = Color(0xFF384357),
                start = c,
                end = Offset(c.x, c.y + r - 12f),
                strokeWidth = 9f
            )
            // Center Horn Boss with Neon Rim
            drawCircle(color = Color(0xFF101522), radius = r * 0.42f)
            drawCircle(color = Color(0xFF00E5FF), radius = r * 0.42f, style = Stroke(width = 2.5f))
        }

        Text(
            text = "APEX",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Tilt / Gyro steering indicator displaying live steer level and center horizon line.
 */
@Composable
fun TiltIndicator(
    steerValue: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC0B101D),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "GYRO STEER",
                color = Color(0xFF00E5FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Center line
                drawLine(Color(0x66FFFFFF), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 2f)
                // Horizon bar
                drawLine(Color(0x44FFFFFF), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.5f)
                // Steer Marker (moves left for left steer, right for right steer)
                val markerX = (cx + steerValue * (cx - 10f)).coerceIn(10f, size.width - 10f)
                drawCircle(color = Color(0xFFFFD700), radius = 5f, center = Offset(markerX, cy))
            }
        }
    }
}

/**
 * Professional motorsport instrument cluster:
 * - Multi-segment LED tachometer / shift light bar
 * - Bold digital speedometer with KM/H unit
 * - Gear indicator
 * - N2O Nitro progress gauge
 * - G-Force meter crosshair
 * - ABS and TCS driving assistance badges
 */
@Composable
fun InstrumentCluster(
    telemetry: RaceTelemetryUiState,
    controlType: ControlType
) {
    val infiniteTransition = rememberInfiniteTransition(label = "revLimiterFlash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    Surface(
        color = Color(0xDD090D18),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed, Gear & RPM Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(165.dp)
            ) {
                // Multi-zone RPM Shift Lights Bar
                val rpmRatio = (telemetry.rpm / telemetry.maxRpm).coerceIn(0f, 1f)
                val isRedline = (rpmRatio > 0.88f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    val segmentCount = 18
                    val spacing = 2f
                    val segW = (size.width - (segmentCount - 1) * spacing) / segmentCount
                    val activeSegments = (rpmRatio * segmentCount).toInt()

                    for (i in 0 until segmentCount) {
                        val x = i * (segW + spacing)
                        val segRatio = i.toFloat() / segmentCount

                        val baseColor = when {
                            segRatio < 0.65f -> Color(0xFF00E5FF) // Green/cyan zone
                            segRatio < 0.85f -> Color(0xFFFFB300) // Amber zone
                            else -> if (isRedline) Color(0xFFFF1744).copy(alpha = flashAlpha) else Color(0xFFFF1744) // Redline shift alert
                        }

                        val drawColor = if (i < activeSegments) baseColor else Color(0xFF1A2233)
                        drawRoundRect(
                            color = drawColor,
                            topLeft = Offset(x, 0f),
                            size = Size(segW, size.height),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Speed & Gear
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Gear Box
                    val gearBgColor = when (telemetry.gear) {
                        "R" -> Color(0xFFD32F2F)
                        "N" -> Color(0xFF455A64)
                        else -> Color(0xFF1B2334)
                    }
                    Box(
                        modifier = Modifier
                            .background(gearBgColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = telemetry.gear,
                            color = if (telemetry.gear == "R") Color.White else Color(0xFFFFD54F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${telemetry.speedKmh.toInt()}",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "KM/H",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Nitro Gauge Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "N2O",
                        color = Color(0xFF00E5FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = { (telemetry.boostPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF141F2D)
                    )
                    Text(
                        text = "${telemetry.boostPercent.toInt()}%",
                        color = Color(0xFF90A4AE),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Assists & G-Force Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Assist Badges: ABS & TCS
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistBadge(label = "ABS", isActive = telemetry.isAbsActive)
                    AssistBadge(label = "TCS", isActive = telemetry.isTcsActive)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // G-Force Crosshair
                Canvas(modifier = Modifier.size(36.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = cx - 2f
                    drawCircle(Color(0x33FFFFFF), radius = r, style = Stroke(1.5f))
                    drawLine(Color(0x22FFFFFF), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1f)
                    drawLine(Color(0x22FFFFFF), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1f)

                    // Dot position scaled to +/- 1.5G
                    val dotX = (cx + (telemetry.gForceLat / 1.5f) * r).coerceIn(2f, size.width - 2f)
                    val dotY = (cy - (telemetry.gForceLong / 1.5f) * r).coerceIn(2f, size.height - 2f)
                    drawCircle(color = Color(0xFFFF5252), radius = 3.5f, center = Offset(dotX, dotY))
                }
            }
        }
    }
}

@Composable
fun AssistBadge(label: String, isActive: Boolean) {
    val bgColor = if (isActive) Color(0xFFFF9100) else Color(0x33222D3D)
    val textColor = if (isActive) Color(0xFF0A0F1A) else Color(0xFF607D8B)

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 7.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Radar Minimap with anti-aliased track boundaries, player heading indicator, and AI opponents.
 */
@Composable
fun MinimapRadar(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val track = viewModel.renderer?.trackModel
        val wps = track?.waypoints ?: emptyList()

        if (wps.size > 2) {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (wp in wps) {
                if (wp.x < minX) minX = wp.x
                if (wp.x > maxX) maxX = wp.x
                if (wp.z < minZ) minZ = wp.z
                if (wp.z > maxZ) maxZ = wp.z
            }

            // Margins
            minX -= 25f
            maxX += 25f
            minZ -= 25f
            maxZ += 25f

            fun mapCoord(x: Float, z: Float): Offset {
                val normX = ((x - minX) / (maxX - minX)).coerceIn(0f, 1f)
                val normZ = ((z - minZ) / (maxZ - minZ)).coerceIn(0f, 1f)
                return Offset(8f + normX * (w - 16f), h - (8f + normZ * (h - 16f)))
            }

            val path = Path()
            val first = mapCoord(wps[0].x, wps[0].z)
            path.moveTo(first.x, first.y)

            for (i in 1 until wps.size) {
                val pt = mapCoord(wps[i].x, wps[i].z)
                path.lineTo(pt.x, pt.y)
            }
            path.close()

            // Outer track road casing
            drawPath(
                path = path,
                color = Color(0x33FFFFFF),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            // Inner track spline line
            drawPath(
                path = path,
                color = Color(0x88FFFFFF),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // Start / Finish Line marker
            val startPt = mapCoord(wps[0].x, wps[0].z)
            drawCircle(color = Color.White, radius = 2.5f, center = startPt)

            // AI Opponents
            for (ai in viewModel.aiOpponents) {
                val pt = mapCoord(ai.vehicle.position.x, ai.vehicle.position.z)
                drawCircle(color = Color(0xFF00E5FF), radius = 3.5f, center = pt)
            }

            // Player dot with heading needle
            val playerPos = viewModel.playerVehicle.position
            val playerHeading = viewModel.playerVehicle.headingAngleRad
            val playerPt = mapCoord(playerPos.x, playerPos.z)
            drawCircle(color = Color(0xFFFFD700), radius = 4.5f, center = playerPt)

            val headingDirX = sin(playerHeading) * 8f
            val headingDirZ = -cos(playerHeading) * 8f
            drawLine(
                color = Color(0xFFFFD700),
                start = playerPt,
                end = Offset(playerPt.x + headingDirX, playerPt.y + headingDirZ),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * Driving pedal button with high-contrast racing styling, active press state, and tactile feedback.
 */
@Composable
fun PedalButton(
    label: String,
    color: Color,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPressed) color.copy(alpha = 1.0f) else color.copy(alpha = 0.65f)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isPressed) 2.5.dp else 1.dp,
            color = if (isPressed) Color.White else Color(0x44FFFFFF)
        ),
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onPressChange(false)
                        }
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = Color.White,
                fontSize = if (label.length > 2) 11.sp else 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
