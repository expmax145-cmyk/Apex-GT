package com.example.racing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.racing.engine.PaintFinishType
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId

/**
 * Phase 11 — Premium 3D Automotive Showroom & Garage Interface.
 * Features:
 * - Interactive 3D vehicle inspection with orbit touch rotation, pinch zoom, and reset
 * - Camera view presets: 3/4 Hero, Side Profile, Rear Aero, Front Detail
 * - Turntable auto-rotate toggle
 * - Vehicle selector strip with real ownership status and prices
 * - Live vehicle telemetry & performance metrics (Top Speed, 0-100 Accel, Power/Torque, Grip, Weight, Drivetrain)
 * - Real-time paint customizer with factory palette & clearcoat finishes (Gloss, Metallic, Matte Satin, Pearlescent)
 * - Tuner performance upgrades with real parameter improvements applied directly to physics
 * - Economy purchasing with confirmation dialog and release-protected development funds
 */
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val credits by viewModel.playerCredits.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val unlockedIds by viewModel.unlockedCarIds.collectAsState()
    val upgrades by viewModel.carUpgrades.collectAsState()
    val selectedColorIdx by viewModel.carColorIndex.collectAsState()
    val selectedPaintFinish by viewModel.carPaintFinish.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Cars, 1: Specs, 2: Paint, 3: Tuning
    var isAutoRotateActive by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }

    val isUnlocked = unlockedIds.contains(selectedCar.id)
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 700.dp
        val isShort = maxHeight < 400.dp
        val horizontalPad = if (isCompact) 12.dp else 24.dp

        // =========================================================================================
        // 1. TOP BAR: Back, Showroom Branding, Camera Controls, Credits Balance & Dev Cash
        // =========================================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = safePadding.calculateTopPadding() + (if (isShort) 4.dp else 10.dp)
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Showroom Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("garage_back_button")
                        .size(44.dp)
                        .background(Color(0xD90A0E18), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "APEX 3D SHOWROOM",
                        color = Color.White,
                        fontSize = if (isCompact) 14.sp else 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "INTERACTIVE INSPECTION & TUNING",
                        color = Color(0xFF00E5FF),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Right: Inspection Controls (Reset / Auto-Rotate) & Credits Balance
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reset Camera Action
                IconButton(
                    onClick = { viewModel.resetShowroomCamera() },
                    modifier = Modifier
                        .testTag("reset_camera_button")
                        .size(38.dp)
                        .background(Color(0xD90A0E18), CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Camera View",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Auto-Rotate Toggle Action
                IconButton(
                    onClick = {
                        isAutoRotateActive = viewModel.toggleAutoRotate()
                    },
                    modifier = Modifier
                        .testTag("auto_rotate_button")
                        .size(38.dp)
                        .background(
                            if (isAutoRotateActive) Color(0xFF00E5FF) else Color(0xD90A0E18),
                            CircleShape
                        )
                        .border(1.dp, if (isAutoRotateActive) Color.White else Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Toggle Turntable Rotation",
                        tint = if (isAutoRotateActive) Color(0xFF070B12) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Credits Balance Card
                Surface(
                    color = Color(0xD90A0E18),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFD700))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CR", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%,d".format(credits),
                            color = Color.White,
                            fontSize = if (isCompact) 13.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Development Currency Injector (Release-Protected)
                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.grantDevelopmentFunds() },
                        modifier = Modifier
                            .testTag("dev_add_funds_button")
                            .height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+$100K DEV",
                            color = Color(0xFF070B12),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // =========================================================================================
        // 2. VEHICLE HERO TITLE & CAMERA PRESET CHIPS (Floating Top-Left)
        // =========================================================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = horizontalPad,
                    top = safePadding.calculateTopPadding() + (if (isShort) 50.dp else 68.dp)
                )
        ) {
            Surface(
                color = Color(0xD90A0E18),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedCar.name.uppercase(),
                            color = Color.White,
                            fontSize = if (isCompact) 15.sp else 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Category Pill
                        Surface(
                            color = Color(0x3300E5FF),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E5FF))
                        ) {
                            Text(
                                text = selectedCar.category.uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Ownership status
                    if (isUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OWNED — ACTIVE IN RACE",
                                color = Color(0xFF00E676),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "LOCKED — %,d CR".format(selectedCar.basePrice),
                            color = Color(0xFFFFD700),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Camera View Angle Preset Chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CameraPresetChip(title = "3/4 HERO", onClick = { viewModel.setCameraPreset("HERO_3_4") })
                CameraPresetChip(title = "SIDE", onClick = { viewModel.setCameraPreset("SIDE") })
                CameraPresetChip(title = "REAR AERO", onClick = { viewModel.setCameraPreset("REAR_AERO") })
                CameraPresetChip(title = "FRONT", onClick = { viewModel.setCameraPreset("FRONT_DETAIL") })
            }
        }

        // =========================================================================================
        // 3. BOTTOM CONTROL DASHBOARD: Tabs, Customizer Panels, Track Selector & Start/Buy Button
        // =========================================================================================
        Surface(
            color = Color(0xF5090D16),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = horizontalPad,
                        end = horizontalPad,
                        top = 10.dp,
                        bottom = safePadding.calculateBottomPadding() + 8.dp
                    )
            ) {
                // Tab Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left 4 Tabs: Cars, Specs, Paint, Tuning
                    Row(
                        modifier = Modifier.weight(0.58f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GarageTabButton(
                            title = "CARS",
                            icon = Icons.Default.DirectionsCar,
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        GarageTabButton(
                            title = "SPECS",
                            icon = Icons.Default.Info,
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        GarageTabButton(
                            title = "PAINT",
                            icon = Icons.Default.ColorLens,
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                        GarageTabButton(
                            title = "TUNING",
                            icon = Icons.Default.Build,
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Track Selector + Start / Buy Primary Action Button
                    Row(
                        modifier = Modifier.weight(0.42f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackSelectorControl(
                            selectedTrack = selectedTrack,
                            onSelectNextTrack = {
                                val all = TrackId.values()
                                val nextIdx = (selectedTrack.ordinal + 1) % all.size
                                viewModel.selectTrack(all[nextIdx])
                            },
                            modifier = Modifier.weight(1.05f)
                        )

                        // Primary Action Button (START RACE or BUY VEHICLE)
                        Button(
                            onClick = {
                                if (isUnlocked) {
                                    viewModel.startRace(RaceMode.CIRCUIT_RACE)
                                } else {
                                    showPurchaseDialog = true
                                }
                            },
                            modifier = Modifier
                                .testTag(if (isUnlocked) "start_race_button" else "buy_vehicle_button")
                                .weight(0.95f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUnlocked) Color(0xFFFF3D00) else Color(0xFFFF9100)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.ShoppingBag,
                                contentDescription = if (isUnlocked) "Start Race" else "Buy Vehicle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isUnlocked) "START" else "BUY",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customizer Subpanel Content
                when (activeTab) {
                    0 -> VehiclesTab(
                        viewModel = viewModel,
                        selectedCar = selectedCar,
                        unlockedIds = unlockedIds
                    )
                    1 -> SpecsTab(
                        viewModel = viewModel,
                        car = selectedCar,
                        upgrades = upgrades
                    )
                    2 -> PaintTab(
                        selectedCar = selectedCar,
                        selectedColorIdx = selectedColorIdx,
                        selectedFinish = selectedPaintFinish,
                        onSelectColor = { viewModel.selectCarColor(it) },
                        onSelectFinish = { viewModel.selectCarPaintFinish(it) }
                    )
                    3 -> TuningTab(
                        viewModel = viewModel,
                        upgrades = upgrades,
                        credits = credits,
                        car = selectedCar
                    )
                }
            }
        }

        // =========================================================================================
        // 4. VEHICLE PURCHASE CONFIRMATION DIALOG
        // =========================================================================================
        if (showPurchaseDialog) {
            val canAfford = credits >= selectedCar.basePrice || BuildConfig.DEBUG
            AlertDialog(
                onDismissRequest = { showPurchaseDialog = false },
                title = {
                    Text(
                        text = "Purchase ${selectedCar.name}?",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Category: ${selectedCar.category}",
                            color = Color(0xFF90A4AE),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Price: %,d CR".format(selectedCar.basePrice),
                            color = Color(0xFFFFD700),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your Balance: %,d CR".format(credits),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Remaining: %,d CR".format(credits - selectedCar.basePrice),
                            color = if (canAfford) Color(0xFF00E676) else Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPurchaseDialog = false
                            viewModel.buySelectedCar()
                        },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                        modifier = Modifier.testTag("confirm_purchase_button")
                    ) {
                        Text("CONFIRM PURCHASE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPurchaseDialog = false },
                        modifier = Modifier.testTag("cancel_purchase_button")
                    ) {
                        Text("CANCEL", color = Color(0xFF90A4AE))
                    }
                },
                containerColor = Color(0xFF131A28),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun CameraPresetChip(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xD90A0E18),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = Color(0xFF00E5FF),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TrackSelectorControl(
    selectedTrack: TrackId,
    onSelectNextTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackIcon = when (selectedTrack) {
        TrackId.GRAND_PRIX -> Icons.Default.Flag
        TrackId.ALPINE_PASS -> Icons.Default.Landscape
        TrackId.NEON_METROPOLIS -> Icons.Default.LocationCity
    }
    val trackColor = when (selectedTrack) {
        TrackId.GRAND_PRIX -> Color(0xFFFFD700)
        TrackId.ALPINE_PASS -> Color(0xFF00E5FF)
        TrackId.NEON_METROPOLIS -> Color(0xFFFF4081)
    }

    Surface(
        color = Color(0xFF131A27),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, trackColor.copy(alpha = 0.5f)),
        modifier = modifier
            .testTag("track_selector_button")
            .height(46.dp)
            .clickable(onClick = onSelectNextTrack)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = trackIcon,
                    contentDescription = selectedTrack.title,
                    tint = trackColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "CIRCUIT",
                        color = Color(0xFF78909C),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedTrack.title,
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = "➔",
                color = trackColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GarageTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) Color(0xFF1E283C) else Color(0xFF101520),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
        ),
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color(0xFF00E5FF) else Color(0xFF78909C),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFF78909C),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VehiclesTab(
    viewModel: GameViewModel,
    selectedCar: CarDefinition,
    unlockedIds: Set<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarDefinition.ALL_CARS.forEach { car ->
            val isSelected = car.id == selectedCar.id
            val isOwned = unlockedIds.contains(car.id)

            Surface(
                color = if (isSelected) Color(0xFF1B2436) else Color(0xFF0E131E),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFFFF3D00) else Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.selectCar(car) }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = car.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                    Text(
                        text = car.category,
                        color = Color(0xFF90A4AE),
                        fontSize = 8.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    if (isOwned) {
                        Text(
                            text = if (isSelected) "SELECTED" else "OWNED",
                            color = if (isSelected) Color(0xFF00E676) else Color(0xFF81C784),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "%,d CR".format(car.basePrice),
                            color = Color(0xFFFFD700),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpecsTab(
    viewModel: GameViewModel,
    car: CarDefinition,
    upgrades: CarUpgradeState
) {
    val topSpeed = viewModel.getVehicleTopSpeed(car, upgrades)
    val torque = viewModel.getVehicleTorque(car, upgrades)
    val weight = viewModel.getVehicleWeight(car, upgrades)
    val zeroToHundred = viewModel.getVehicleZeroToHundred(car, upgrades)
    val grip = viewModel.getVehicleGrip(upgrades)
    val drivetrain = if (car.driveFrontRatio > 0.1f) "AWD Torque Vectoring" else "RWD Precision Drift"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Column 1: Top Speed, 0-100 Accel, Power/Torque
        Column(modifier = Modifier.weight(1f)) {
            SpecMetricRow("TOP SPEED", "%.0f km/h".format(topSpeed), topSpeed / 400f)
            Spacer(modifier = Modifier.height(4.dp))
            SpecMetricRow("0-100 KM/H", "%.2f s".format(zeroToHundred), (5.0f - zeroToHundred) / 3.2f)
            Spacer(modifier = Modifier.height(4.dp))
            SpecMetricRow("TORQUE", "%.0f Nm".format(torque), torque / 1000f)
        }

        // Column 2: Lateral Handling, Curb Weight, Drivetrain
        Column(modifier = Modifier.weight(1f)) {
            SpecMetricRow("TIRE GRIP", "%.2f µ".format(grip), grip / 1.8f)
            Spacer(modifier = Modifier.height(4.dp))
            SpecMetricRow("CURB WEIGHT", "%.0f kg".format(weight), (1600f - weight) / 700f)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "DRIVETRAIN", color = Color(0xFF78909C), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                Text(text = drivetrain, color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpecMetricRow(label: String, value: String, fraction: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color(0xFF78909C), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.05f, 1.0f) },
            color = Color(0xFF00E5FF),
            trackColor = Color(0x33102027),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}

@Composable
fun PaintTab(
    selectedCar: CarDefinition,
    selectedColorIdx: Int,
    selectedFinish: PaintFinishType,
    onSelectColor: (Int) -> Unit,
    onSelectFinish: (PaintFinishType) -> Unit
) {
    Column {
        Text(
            text = "FACTORY AUTOMOTIVE COLOR",
            color = Color(0xFF90A4AE),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(selectedCar.availableColors.indices.toList()) { idx ->
                val (name, rgb) = selectedCar.availableColors[idx]
                val isSelected = idx == selectedColorIdx
                val swatchColor = Color(rgb[0], rgb[1], rgb[2], 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectColor(idx) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(swatchColor, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else Color(0xFF78909C),
                        fontSize = 8.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "CLEARCOAT & SURFACE FINISH",
            color = Color(0xFF90A4AE),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(5.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PaintFinishType.values().toList()) { finish ->
                val isSelected = finish == selectedFinish
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) Color(0xFF263238) else Color(0x33102027),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectFinish(finish) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = finish.displayName,
                        color = if (isSelected) Color.White else Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun TuningTab(
    viewModel: GameViewModel,
    upgrades: CarUpgradeState,
    credits: Int,
    car: CarDefinition
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UpgradeCard(
            name = "ECU TUNING",
            delta = "+65 Nm Torque",
            level = upgrades.engineStage,
            maxLevel = 3,
            canUpgrade = upgrades.engineStage < 3 && (credits >= 3500 || BuildConfig.DEBUG),
            onUpgrade = { viewModel.upgradePart("ENGINE") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "TIRE COMPOUND",
            delta = "+0.12 Peak Grip",
            level = upgrades.tireStage,
            maxLevel = 3,
            canUpgrade = upgrades.tireStage < 3 && (credits >= 3500 || BuildConfig.DEBUG),
            onUpgrade = { viewModel.upgradePart("TIRES") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "SUSPENSION",
            delta = "+0.15 Aero Downforce",
            level = upgrades.suspensionStage,
            maxLevel = 3,
            canUpgrade = upgrades.suspensionStage < 3 && (credits >= 3500 || BuildConfig.DEBUG),
            onUpgrade = { viewModel.upgradePart("SUSPENSION") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "WEIGHT DROP",
            delta = "-55 kg Carbon Diet",
            level = upgrades.weightReduction,
            maxLevel = 3,
            canUpgrade = upgrades.weightReduction < 3 && (credits >= 3500 || BuildConfig.DEBUG),
            onUpgrade = { viewModel.upgradePart("WEIGHT") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun UpgradeCard(
    name: String,
    delta: String,
    level: Int,
    maxLevel: Int,
    canUpgrade: Boolean,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF131824),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = delta,
                color = Color(0xFF00E5FF),
                fontSize = 7.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "STAGE $level/$maxLevel",
                color = if (level == maxLevel) Color(0xFFFFD700) else Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E2638),
                    disabledContainerColor = Color(0xFF0F1420)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = if (level == maxLevel) "MAX" else "3,500 CR",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
