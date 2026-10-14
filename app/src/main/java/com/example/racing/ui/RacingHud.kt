package com.example.racing.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
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
import com.example.racing.model.ControlType
import com.example.racing.model.RaceMode
import kotlin.math.abs

@Composable
fun RacingHud(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val raceMode by viewModel.activeRaceMode.collectAsState()
    val controlType by viewModel.controlType.collectAsState()
    val context = LocalContext.current

    var isLeftPressed by remember { mutableStateOf(false) }
    var isRightPressed by remember { mutableStateOf(false) }
    var isGasPressed by remember { mutableStateOf(false) }
    var isBrakePressed by remember { mutableStateOf(false) }
    var isHandbrakePressed by remember { mutableStateOf(false) }
    var isBoostPressed by remember { mutableStateOf(false) }

    var wheelSteerAngle by remember { mutableFloatStateOf(0f) }
    var tiltSteerValue by remember { mutableFloatStateOf(0f) }

    // Accelerometer Sensor for TILT_GYRO mode
    DisposableEffect(controlType) {
        if (controlType == ControlType.TILT_GYRO) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null && event.values.size >= 2) {
                        // Landscape orientation: Y-axis represents tilt
                        val tiltY = event.values[1]
                        tiltSteerValue = (tiltY / 4.8f).coerceIn(-1.0f, 1.0f)
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
            tiltSteerValue = 0f
            onDispose { }
        }
    }

    var touchSteerSmooth by remember { mutableFloatStateOf(0f) }

    // Determine steer input based on active control type
    val touchTarget = when {
        isLeftPressed && !isRightPressed -> 1.0f
        isRightPressed && !isLeftPressed -> -1.0f
        else -> 0.0f
    }
    val rampSpeed = if (touchTarget == 0f) 0.35f else 0.28f
    touchSteerSmooth += (touchTarget - touchSteerSmooth) * rampSpeed
    if (abs(touchSteerSmooth) < 0.01f && touchTarget == 0f) touchSteerSmooth = 0f

    val steerValue = when (controlType) {
        ControlType.VIRTUAL_WHEEL -> (-wheelSteerAngle / 60f).coerceIn(-1.0f, 1.0f)
        ControlType.TILT_GYRO -> tiltSteerValue
        ControlType.TOUCH_BUTTONS -> touchSteerSmooth
    }

    viewModel.setSteerInput(steerValue)
    viewModel.setThrottle(if (isGasPressed) 1.0f else 0.0f)
    viewModel.setBrake(if (isBrakePressed) 1.0f else 0.0f)
    viewModel.setHandbrake(isHandbrakePressed)
    viewModel.triggerBoost(isBoostPressed)

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 700.dp
        val isNarrow = maxWidth < 620.dp
        val isShort = maxHeight < 380.dp

        val hPadding = if (isNarrow) 8.dp else if (isCompact) 14.dp else 20.dp
        val bottomPadding = if (isShort) 8.dp else 16.dp

        // Adaptive pedal dimensions
        val pedalSpacing = if (isNarrow) 6.dp else if (isCompact) 8.dp else 12.dp
        val gasW = if (isNarrow) 58.dp else if (isCompact) 66.dp else 76.dp
        val gasH = if (isShort) 76.dp else if (isCompact) 84.dp else 98.dp
        val brakeW = if (isNarrow) 52.dp else if (isCompact) 60.dp else 68.dp
        val brakeH = if (isShort) 66.dp else if (isCompact) 74.dp else 85.dp
        val nitroW = if (isNarrow) 48.dp else if (isCompact) 56.dp else 64.dp
        val nitroH = if (isShort) 50.dp else if (isCompact) 58.dp else 66.dp
        val eBrakeW = if (isNarrow) 48.dp else if (isCompact) 54.dp else 62.dp
        val eBrakeH = if (isShort) 48.dp else if (isCompact) 50.dp else 56.dp

        val steerBtnSize = if (isNarrow) 56.dp else if (isCompact) 64.dp else 72.dp
        val steerSpacing = if (isNarrow) 8.dp else if (isCompact) 12.dp else 16.dp
        val wheelSize = if (isNarrow) 85.dp else if (isCompact) 95.dp else 105.dp

        // 1. Top Bar: Minimap, Timing, Sector Splits & Camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = hPadding, vertical = if (isShort) 6.dp else 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Top: Pause Button & Race Position
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("pause_button")
                        .size(44.dp)
                        .background(Color(0x99121620), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (raceMode == RaceMode.CIRCUIT_RACE) {
                    Surface(
                        color = Color(0xCC0E131F),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "POS",
                                color = Color(0xFF8E99A8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${telemetry.racePosition}",
                                color = Color(0xFFFFD700),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/${telemetry.totalRacers}",
                                color = Color(0xFF8E99A8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = Color(0xCC0E131F),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LAP",
                                color = Color(0xFF8E99A8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${telemetry.currentLap}/${telemetry.totalLaps}",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Weather and Time of Day indicator badge
                val timeOfDay by viewModel.timeOfDay.collectAsState()
                val weatherCondition by viewModel.weatherCondition.collectAsState()
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xCC0E131F),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeOfDay.name,
                            color = Color(0xFFFF9100),
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

            // Center Top: Track Radar Minimap
            MinimapRadar(
                viewModel = viewModel,
                modifier = Modifier
                    .size(width = 135.dp, height = 78.dp)
                    .background(Color(0xA60A0D14), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            )

            // Right Top: Lap Timing, Sector Splits & Camera Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xCC0E131F),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                                color = Color(0xFF8E99A8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = telemetry.bestLapTime,
                                color = Color(0xFF81C784),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.toggleCamera() },
                    modifier = Modifier
                        .testTag("camera_toggle_button")
                        .size(44.dp)
                        .background(Color(0x99121620), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Switch Camera",
                        tint = Color.White
                    )
                }
            }
        }

        // 2. Off-Road / Drift Badge Alerts
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (telemetry.isOffRoad) {
                Box(
                    modifier = Modifier
                        .background(Color(0xDDE53935), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "OFF-TRACK • LOW TRACTION",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 3. Center Bottom: Advanced Digital Instrument Cluster with G-Meter
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            InstrumentCluster(telemetry = telemetry)
        }

        // 4. Left Bottom Controls: Adapts to ControlType (Touch Buttons, Virtual Wheel, or Tilt Gyro)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = hPadding, bottom = bottomPadding)
        ) {
            when (controlType) {
                ControlType.TOUCH_BUTTONS -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(steerSpacing)) {
                        PedalButton(
                            label = "◀",
                            color = Color(0xFF242A38),
                            isPressed = isLeftPressed,
                            onPressChange = { isLeftPressed = it },
                            modifier = Modifier
                                .testTag("steer_left_button")
                                .size(steerBtnSize)
                        )

                        PedalButton(
                            label = "▶",
                            color = Color(0xFF242A38),
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
                        wheelAngle = wheelSteerAngle,
                        onAngleChange = { wheelSteerAngle = it },
                        modifier = Modifier
                            .testTag("virtual_wheel_control")
                            .size(wheelSize)
                    )
                }
                ControlType.TILT_GYRO -> {
                    TiltIndicator(
                        steerValue = tiltSteerValue,
                        modifier = Modifier
                            .testTag("tilt_gyro_indicator")
                            .size(width = if (isNarrow) 90.dp else 110.dp, height = if (isShort) 42.dp else 50.dp)
                    )
                }
            }
        }

        // 5. Right Bottom Controls: Throttle, Brake, Handbrake, Nitro
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = hPadding, bottom = bottomPadding),
            horizontalArrangement = Arrangement.spacedBy(pedalSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            PedalButton(
                label = "E-BRAKE",
                color = Color(0xFF5D4037),
                isPressed = isHandbrakePressed,
                onPressChange = { isHandbrakePressed = it },
                modifier = Modifier
                    .testTag("handbrake_button")
                    .size(width = eBrakeW, height = eBrakeH)
            )

            PedalButton(
                label = "NITRO",
                color = if (telemetry.boostAvailable) Color(0xFF00897B) else Color(0xFF37474F),
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

        // 6. Start Countdown Overlay (3, 2, 1, GO!)
        AnimatedVisibility(
            visible = telemetry.isCountdownActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val count = telemetry.countdownNumber
            val text = if (count > 0) count.toString() else "GO!"
            val color = if (count > 0) Color(0xFFFF3D00) else Color(0xFF00E676)

            Surface(
                color = Color(0xDD0A0E18),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(4.dp, color),
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
    }
}

@Composable
fun VirtualSteeringWheel(
    wheelAngle: Float,
    onAngleChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedAngle by animateFloatAsState(targetValue = wheelAngle, label = "wheelAngle")

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { onAngleChange(0f) },
                    onDragCancel = { onAngleChange(0f) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newAngle = (wheelAngle + dragAmount.x * 0.9f).coerceIn(-65f, 65f)
                        onAngleChange(newAngle)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().rotate(animatedAngle)) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)

            // Outer Rim
            drawCircle(
                color = Color(0xFF252D3D),
                radius = r - 4f,
                style = Stroke(width = 12f)
            )
            // Grip Accent Stripe (Center Marker)
            drawArc(
                color = Color(0xFFFF3D00),
                startAngle = 260f,
                sweepAngle = 20f,
                useCenter = false,
                style = Stroke(width = 14f)
            )
            // Spokes
            drawLine(
                color = Color(0xFF384357),
                start = Offset(c.x - r + 10f, c.y),
                end = Offset(c.x + r - 10f, c.y),
                strokeWidth = 10f
            )
            drawLine(
                color = Color(0xFF384357),
                start = c,
                end = Offset(c.x, c.y + r - 10f),
                strokeWidth = 10f
            )
            // Center Horn Boss
            drawCircle(color = Color(0xFF141924), radius = r * 0.42f)
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

@Composable
fun TiltIndicator(
    steerValue: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xCC0E131F),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF)),
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
                val markerX = (cx - steerValue * (cx - 10f)).coerceIn(10f, size.width - 10f)
                drawCircle(color = Color(0xFFFFD700), radius = 5f, center = Offset(markerX, cy))
            }
        }
    }
}

@Composable
fun InstrumentCluster(telemetry: RaceTelemetryUiState) {
    Surface(
        color = Color(0xDD0D111A),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed, Gear & RPM Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp)
            ) {
                // RPM Bar
                val rpmRatio = (telemetry.rpm / telemetry.maxRpm).coerceIn(0f, 1f)
                val rpmBarColor = if (rpmRatio > 0.88f) Color(0xFFFF1744) else Color(0xFF00E5FF)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { rpmRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = rpmBarColor,
                        trackColor = Color(0xFF1E2638),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Speed & Gear
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1B2230), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = telemetry.gear,
                            color = Color(0xFFFFD54F),
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
                        color = Color(0xFF8E99A8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp, start = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "N2O",
                        color = Color(0xFF00B0FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    LinearProgressIndicator(
                        progress = { telemetry.boostPercent / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF16212D),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Real-time G-Force Crosshair
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "G-FORCE",
                    color = Color(0xFF78909C),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Canvas(modifier = Modifier.size(38.dp)) {
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

            drawPath(
                path = path,
                color = Color(0x66FFFFFF),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // AI Opponents
            for (ai in viewModel.aiOpponents) {
                val pt = mapCoord(ai.vehicle.position.x, ai.vehicle.position.z)
                drawCircle(color = Color(0xFF00E5FF), radius = 3.5f, center = pt)
            }

            // Player
            val playerPt = mapCoord(viewModel.playerVehicle.position.x, viewModel.playerVehicle.position.z)
            drawCircle(color = Color(0xFFFFD700), radius = 5.0f, center = playerPt)
        }
    }
}

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
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPressed) 2.dp else 1.dp,
            color = if (isPressed) Color.White else Color(0x44FFFFFF)
        ),
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        tryAwaitRelease()
                        onPressChange(false)
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
