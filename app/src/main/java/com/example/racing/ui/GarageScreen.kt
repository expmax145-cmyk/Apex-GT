package com.example.racing.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.engine.PaintFinishType
import com.example.racing.model.CarDefinition
import com.example.racing.model.CarUpgradeState
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId

/**
 * Phase 15: Premium 3D Automotive Showroom, Vehicle Selection & Customization Overhaul.
 *
 * Implements a world-class motorsport garage experience:
 * - Live interactive 3D vehicle inspection rendered via OpenGL ES 3.0 turntable
 * - Camera perspective presets (3/4 Hero, Side Profile, Rear Aero, Front Detail)
 * - Turntable auto-rotate toggle and double-tap view reset
 * - Multi-vehicle carousel with real physics definitions (Apex Valkyrie GT, Spirit GTS 86, Monza V10)
 * - Clear ownership, pricing, and progression states
 * - Real-time factory color palette and PBR clearcoat surface finish customizer
 * - Engineering tuning upgrades (ECU, Tires, Suspension/Aero, Weight Reduction) directly updating physics
 * - Full landscape ergonomics with WindowInsets handling and BackHandler integration
 */
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    // Back navigation integration to seamlessly return to the Main Menu
    BackHandler {
        viewModel.navigateTo(GameScreen.MAIN_MENU)
    }

    val credits by viewModel.playerCredits.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val unlockedIds by viewModel.unlockedCarIds.collectAsState()
    val upgrades by viewModel.carUpgrades.collectAsState()
    val selectedColorIdx by viewModel.carColorIndex.collectAsState()
    val selectedPaintFinish by viewModel.carPaintFinish.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Fleet, 1: Specs, 2: Paint, 3: Tuner
    var isAutoRotateActive by remember { mutableStateOf(false) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var activePreset by remember { mutableStateOf("HERO_3_4") }

    val isUnlocked = unlockedIds.contains(selectedCar.id)
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Ambient studio vignette to maintain high contrast for UI over 3D showroom
            .background(
                Brush.verticalGradient(
                    0.00f to Color(0xCC05080E),
                    0.15f to Color(0x3305080E),
                    0.65f to Color(0x2205080E),
                    1.00f to Color(0xF2060910)
                )
            )
            .padding(safePadding)
    ) {
        val isCompactWidth = maxWidth < 760.dp
        val isShortHeight = maxHeight < 410.dp
        val horizontalPad = if (isCompactWidth) 14.dp else 22.dp

        // =========================================================================================
        // 1. TOP HEADER BAR: Back, Showroom Brand Lockup, Camera Presets & Credits Balance
        // =========================================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = if (isShortHeight) 6.dp else 10.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Back button & Showroom Brand Identity
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("garage_back_button")
                        .size(42.dp)
                        .background(Color(0xD90B101A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Main Menu",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Motorsport Red Accent Bar & Brand Label
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(if (isShortHeight) 24.dp else 30.dp)
                        .background(Color(0xFFFF3823), RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "APEX SHOWROOM",
                        color = Color.White,
                        fontSize = if (isCompactWidth) 15.sp else 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "MOTORSPORT CUSTOMIZATION & TUNING",
                        color = Color(0xFF00E5FF),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Right: Camera Angle Presets & Economy Wallet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Reset Camera View Action
                IconButton(
                    onClick = {
                        activePreset = "HERO_3_4"
                        viewModel.resetShowroomCamera()
                    },
                    modifier = Modifier
                        .testTag("reset_camera_button")
                        .size(36.dp)
                        .background(Color(0xD90B101A), CircleShape)
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Camera View",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Turntable Auto-Rotate Action
                IconButton(
                    onClick = {
                        isAutoRotateActive = viewModel.toggleAutoRotate()
                    },
                    modifier = Modifier
                        .testTag("auto_rotate_button")
                        .size(36.dp)
                        .background(
                            if (isAutoRotateActive) Color(0xFF00E5FF) else Color(0xD90B101A),
                            CircleShape
                        )
                        .border(1.dp, if (isAutoRotateActive) Color.White else Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Toggle Turntable Rotation",
                        tint = if (isAutoRotateActive) Color(0xFF060910) else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Camera Angle Presets (Rendered compactly if width permits)
                if (!isCompactWidth) {
                    Spacer(modifier = Modifier.width(4.dp))
                    CameraAngleChip(
                        label = "3/4 HERO",
                        isSelected = activePreset == "HERO_3_4",
                        onClick = {
                            activePreset = "HERO_3_4"
                            viewModel.setCameraPreset("HERO_3_4")
                        }
                    )
                    CameraAngleChip(
                        label = "SIDE",
                        isSelected = activePreset == "SIDE",
                        onClick = {
                            activePreset = "SIDE"
                            viewModel.setCameraPreset("SIDE")
                        }
                    )
                    CameraAngleChip(
                        label = "REAR AERO",
                        isSelected = activePreset == "REAR_AERO",
                        onClick = {
                            activePreset = "REAR_AERO"
                            viewModel.setCameraPreset("REAR_AERO")
                        }
                    )
                    CameraAngleChip(
                        label = "FRONT",
                        isSelected = activePreset == "FRONT_DETAIL",
                        onClick = {
                            activePreset = "FRONT_DETAIL"
                            viewModel.setCameraPreset("FRONT_DETAIL")
                        }
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Credits Balance Pill
                Surface(
                    color = Color(0xEE0B101A),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0x44FFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color(0xFFFFB300), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CR",
                                color = Color(0xFF060910),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "%,d".format(credits),
                            color = Color.White,
                            fontSize = if (isCompactWidth) 12.5.sp else 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Rapid Test Credit Grant Action (+500K CR)
                Button(
                    onClick = { viewModel.grantDevelopmentFunds(500000) },
                    modifier = Modifier
                        .testTag("dev_add_funds_button")
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(17.dp),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+$500K",
                        color = Color(0xFF060910),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // =========================================================================================
        // 2. FLOATING VEHICLE QUICK BADGE (Top-Left under top bar)
        // =========================================================================================
        Surface(
            color = Color(0xD90B101A),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = horizontalPad,
                    top = if (isShortHeight) 52.dp else 66.dp
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedCar.name.uppercase(),
                            color = Color.White,
                            fontSize = if (isShortHeight) 13.sp else 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0x3300E5FF),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFF00E5FF))
                        ) {
                            Text(
                                text = selectedCar.category.uppercase(),
                                color = Color(0xFF00E5FF),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }

                    // Ownership status badge
                    if (isUnlocked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OWNED • READY TO RACE",
                                color = Color(0xFF00E676),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LOCKED • %,d CR".format(selectedCar.basePrice),
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================================
        // 3. LOWER AUTOMOTIVE CONTROL DOCK: Tabs, Subpanel Content & Race Launch
        // =========================================================================================
        Surface(
            color = Color(0xF2070B13),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = horizontalPad,
                        end = horizontalPad,
                        top = 8.dp,
                        bottom = if (isShortHeight) 6.dp else 10.dp
                    )
            ) {
                // 3.1 Primary Dock Header: Tabs on Left, Track Selector & Launch CTA on Right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: 4 Section Navigation Tabs
                    Row(
                        modifier = Modifier.weight(if (isCompactWidth) 0.58f else 0.62f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GarageTabButton(
                            title = "FLEET",
                            icon = Icons.Default.DirectionsCar,
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            modifier = Modifier.weight(1f),
                            isShort = isShortHeight
                        )
                        GarageTabButton(
                            title = "SPECS",
                            icon = Icons.Default.Speed,
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            modifier = Modifier.weight(1f),
                            isShort = isShortHeight
                        )
                        GarageTabButton(
                            title = "PAINT",
                            icon = Icons.Default.ColorLens,
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            modifier = Modifier.weight(1f),
                            isShort = isShortHeight
                        )
                        GarageTabButton(
                            title = "TUNER",
                            icon = Icons.Default.Build,
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            modifier = Modifier.weight(1f),
                            isShort = isShortHeight
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Right: Track Selector + Primary Launch/Purchase Button
                    Row(
                        modifier = Modifier.weight(if (isCompactWidth) 0.42f else 0.38f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackSelectorButton(
                            selectedTrack = selectedTrack,
                            onSelectNextTrack = {
                                val all = TrackId.values()
                                val nextIdx = (selectedTrack.ordinal + 1) % all.size
                                viewModel.selectTrack(all[nextIdx])
                            },
                            modifier = Modifier.weight(1.05f),
                            isShort = isShortHeight
                        )

                        // Primary Action Button: Start Race or Purchase Vehicle
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
                                .height(if (isShortHeight) 38.dp else 42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUnlocked) Color(0xFFFF3823) else Color(0xFFFFB300)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.ShoppingBag,
                                contentDescription = if (isUnlocked) "Start Race" else "Buy Vehicle",
                                tint = if (isUnlocked) Color.White else Color(0xFF060910),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUnlocked) "START" else "UNLOCK",
                                color = if (isUnlocked) Color.White else Color(0xFF060910),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isShortHeight) 6.dp else 8.dp))

                // 3.2 Subpanel Content according to Active Tab
                when (activeTab) {
                    0 -> FleetSelectionSubpanel(
                        viewModel = viewModel,
                        selectedCar = selectedCar,
                        unlockedIds = unlockedIds,
                        isShort = isShortHeight
                    )
                    1 -> SpecsTelemetrySubpanel(
                        viewModel = viewModel,
                        car = selectedCar,
                        upgrades = upgrades,
                        isShort = isShortHeight
                    )
                    2 -> PaintCustomizerSubpanel(
                        selectedCar = selectedCar,
                        selectedColorIdx = selectedColorIdx,
                        selectedFinish = selectedPaintFinish,
                        onSelectColor = { viewModel.selectCarColor(it) },
                        onSelectFinish = { viewModel.selectCarPaintFinish(it) },
                        isShort = isShortHeight
                    )
                    3 -> PerformanceTunerSubpanel(
                        viewModel = viewModel,
                        upgrades = upgrades,
                        credits = credits,
                        isShort = isShortHeight
                    )
                }
            }
        }

        // =========================================================================================
        // 4. VEHICLE PURCHASE CONFIRMATION MODAL
        // =========================================================================================
        if (showPurchaseDialog) {
            val canAfford = credits >= selectedCar.basePrice
            AlertDialog(
                onDismissRequest = { showPurchaseDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Acquire ${selectedCar.name}?",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = selectedCar.description,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFF0A0E18),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Vehicle Price:", color = Color(0xFF90A4AE), fontSize = 12.sp)
                                    Text("%,d CR".format(selectedCar.basePrice), color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Balance:", color = Color(0xFF90A4AE), fontSize = 12.sp)
                                    Text("%,d CR".format(credits), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Balance After Purchase:", color = Color(0xFF90A4AE), fontSize = 12.sp)
                                    Text(
                                        "%,d CR".format(credits - selectedCar.basePrice),
                                        color = if (canAfford) Color(0xFF00E676) else Color(0xFFFF5252),
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPurchaseDialog = false
                            viewModel.buySelectedCar()
                        },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3823)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_purchase_button")
                    ) {
                        Text("CONFIRM PURCHASE", fontWeight = FontWeight.Black, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPurchaseDialog = false },
                        modifier = Modifier.testTag("cancel_purchase_button")
                    ) {
                        Text("CANCEL", color = Color(0xFF90A4AE), fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color(0xFF111824),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Camera View Angle Preset Chip.
 */
@Composable
private fun CameraAngleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(if (isSelected) Color(0x3300E5FF) else Color(0xD90B101A))
    val borderCol by animateColorAsState(if (isSelected) Color(0xFF00E5FF) else Color(0x28FFFFFF))

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFB0BEC5),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

/**
 * Tab Navigation Button for the lower dashboard.
 */
@Composable
private fun GarageTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isShort: Boolean = false
) {
    val bg by animateColorAsState(if (selected) Color(0xFF1B2436) else Color(0xFF0E131E))
    val borderCol by animateColorAsState(if (selected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
    val contentCol by animateColorAsState(if (selected) Color(0xFF00E5FF) else Color(0xFF7E8B9B))

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
            .height(if (isShort) 38.dp else 42.dp)
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
                tint = contentCol,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFF7E8B9B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Circuit selection pill in the bottom dock.
 */
@Composable
private fun TrackSelectorButton(
    selectedTrack: TrackId,
    onSelectNextTrack: () -> Unit,
    modifier: Modifier = Modifier,
    isShort: Boolean = false
) {
    val trackIcon = when (selectedTrack) {
        TrackId.GRAND_PRIX -> Icons.Default.Flag
        TrackId.ALPINE_PASS -> Icons.Default.Landscape
        TrackId.NEON_METROPOLIS -> Icons.Default.LocationCity
    }
    val trackColor = when (selectedTrack) {
        TrackId.GRAND_PRIX -> Color(0xFFFFB300)
        TrackId.ALPINE_PASS -> Color(0xFF00E5FF)
        TrackId.NEON_METROPOLIS -> Color(0xFFFF4081)
    }

    Surface(
        color = Color(0xFF0E1420),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, trackColor.copy(alpha = 0.45f)),
        modifier = modifier
            .testTag("track_selector_button")
            .height(if (isShort) 38.dp else 42.dp)
            .clickable(onClick = onSelectNextTrack)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = trackIcon,
                    contentDescription = selectedTrack.title,
                    tint = trackColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "TRACK",
                        color = Color(0xFF7E8B9B),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedTrack.title,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next Track",
                tint = trackColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Tab 0: Fleet Selection Subpanel.
 * Renders high-fidelity vehicle selection cards for all 3 supported machines.
 */
@Composable
private fun FleetSelectionSubpanel(
    viewModel: GameViewModel,
    selectedCar: CarDefinition,
    unlockedIds: Set<String>,
    isShort: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarDefinition.ALL_CARS.forEach { car ->
            val isSelected = car.id == selectedCar.id
            val isOwned = unlockedIds.contains(car.id)

            val borderStroke = when {
                isSelected -> BorderStroke(1.5.dp, Color(0xFFFF3823))
                isOwned -> BorderStroke(1.dp, Color(0x4400E5FF))
                else -> BorderStroke(1.dp, Color(0x22FFFFFF))
            }

            Surface(
                color = if (isSelected) Color(0xFF161F2E) else Color(0xFF0C111A),
                shape = RoundedCornerShape(10.dp),
                border = borderStroke,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.selectCar(car) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isShort) 6.dp else 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = car.name,
                            color = Color.White,
                            fontSize = if (isShort) 11.sp else 12.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Text(
                            text = "${car.topSpeedKmh.toInt()} KM/H",
                            color = Color(0xFF00E5FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = car.category,
                        color = Color(0xFF7E8B9B),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isOwned) {
                            Text(
                                text = if (isSelected) "SELECTED" else "OWNED",
                                color = if (isSelected) Color(0xFF00E676) else Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        } else {
                            Text(
                                text = "%,d CR".format(car.basePrice),
                                color = Color(0xFFFFB300),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "ACCEL ${car.accelerationRating}/10",
                            color = Color(0xFF90A4AE),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Live Specifications & Performance Telemetry Subpanel.
 */
@Composable
private fun SpecsTelemetrySubpanel(
    viewModel: GameViewModel,
    car: CarDefinition,
    upgrades: CarUpgradeState,
    isShort: Boolean = false
) {
    val topSpeed = viewModel.getVehicleTopSpeed(car, upgrades)
    val torque = viewModel.getVehicleTorque(car, upgrades)
    val weight = viewModel.getVehicleWeight(car, upgrades)
    val zeroToHundred = viewModel.getVehicleZeroToHundred(car, upgrades)
    val grip = viewModel.getVehicleGrip(upgrades)
    val drivetrain = if (car.driveFrontRatio > 0.1f) "AWD Torque Vectoring" else "RWD Precision Drift"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Column: Top Speed, 0-100 Accel, Engine Torque
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (isShort) 2.dp else 4.dp)) {
            SpecRowWithDelta(
                label = "TOP SPEED",
                displayValue = "%.0f km/h".format(topSpeed),
                fraction = topSpeed / 400f,
                accentColor = Color(0xFFFF3823),
                deltaText = if (upgrades.engineStage > 0 || upgrades.weightReduction > 0) "+${((upgrades.engineStage * 6.5f) + (upgrades.weightReduction * 3f)).toInt()} km/h" else null
            )
            SpecRowWithDelta(
                label = "0-100 KM/H",
                displayValue = "%.2f s".format(zeroToHundred),
                fraction = (5.0f - zeroToHundred) / 3.1f,
                accentColor = Color(0xFF00E5FF),
                deltaText = if (upgrades.engineStage > 0 || upgrades.tireStage > 0) "-0.${(upgrades.engineStage * 18 + upgrades.tireStage * 15)}s" else null
            )
            SpecRowWithDelta(
                label = "TORQUE",
                displayValue = "%.0f Nm".format(torque),
                fraction = torque / 1000f,
                accentColor = Color(0xFFFFB300),
                deltaText = if (upgrades.engineStage > 0) "+${upgrades.engineStage * 65} Nm" else null
            )
        }

        // Right Column: Tire Grip, Curb Weight, Drivetrain
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (isShort) 2.dp else 4.dp)) {
            SpecRowWithDelta(
                label = "TIRE GRIP",
                displayValue = "%.2f µ".format(grip),
                fraction = grip / 1.8f,
                accentColor = Color(0xFF00E676),
                deltaText = if (upgrades.tireStage > 0) "+0.${upgrades.tireStage * 12} µ" else null
            )
            SpecRowWithDelta(
                label = "CURB WEIGHT",
                displayValue = "%.0f kg".format(weight),
                fraction = (1600f - weight) / 650f,
                accentColor = Color(0xFF00E5FF),
                deltaText = if (upgrades.weightReduction > 0) "-${upgrades.weightReduction * 55} kg" else null
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "DRIVETRAIN", color = Color(0xFF7E8B9B), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                Text(text = drivetrain, color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpecRowWithDelta(
    label: String,
    displayValue: String,
    fraction: Float,
    accentColor: Color,
    deltaText: String? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, color = Color(0xFF7E8B9B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                if (deltaText != null) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = deltaText, color = Color(0xFF00E676), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(text = displayValue, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.05f, 1.0f) },
            color = accentColor,
            trackColor = Color(0xFF141C2B),
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
        )
    }
}

/**
 * Tab 2: Factory Paint & PBR Surface Finish Customizer Subpanel.
 */
@Composable
private fun PaintCustomizerSubpanel(
    selectedCar: CarDefinition,
    selectedColorIdx: Int,
    selectedFinish: PaintFinishType,
    onSelectColor: (Int) -> Unit,
    onSelectFinish: (PaintFinishType) -> Unit,
    isShort: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Factory Color Palette
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = "FACTORY AUTOMOTIVE PALETTE",
                color = Color(0xFF90A4AE),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                .size(if (isShort) 28.dp else 32.dp)
                                .background(swatchColor, CircleShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = name,
                            color = if (isSelected) Color.White else Color(0xFF7E8B9B),
                            fontSize = 8.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Clearcoat & Surface Finish
        Column(modifier = Modifier.weight(0.9f)) {
            Text(
                text = "CLEARCOAT & SURFACE FINISH",
                color = Color(0xFF90A4AE),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(PaintFinishType.values().toList()) { finish ->
                    val isSelected = finish == selectedFinish
                    Surface(
                        color = if (isSelected) Color(0xFF1E283C) else Color(0xFF0E131E),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
                        ),
                        modifier = Modifier.clickable { onSelectFinish(finish) }
                    ) {
                        Text(
                            text = finish.displayName,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = if (isShort) 5.dp else 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Performance Engineering Tuner Subpanel.
 */
@Composable
private fun PerformanceTunerSubpanel(
    viewModel: GameViewModel,
    upgrades: CarUpgradeState,
    credits: Int,
    isShort: Boolean = false
) {
    val upgradeCost = 3500

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TunerCard(
            name = "ECU TUNING",
            delta = "+65 Nm Torque",
            level = upgrades.engineStage,
            maxLevel = 3,
            canUpgrade = upgrades.engineStage < 3 && credits >= upgradeCost,
            cost = upgradeCost,
            onUpgrade = { viewModel.upgradePart("ENGINE") },
            modifier = Modifier.weight(1f),
            isShort = isShort
        )
        TunerCard(
            name = "TIRE COMPOUND",
            delta = "+0.12 Peak Grip",
            level = upgrades.tireStage,
            maxLevel = 3,
            canUpgrade = upgrades.tireStage < 3 && credits >= upgradeCost,
            cost = upgradeCost,
            onUpgrade = { viewModel.upgradePart("TIRES") },
            modifier = Modifier.weight(1f),
            isShort = isShort
        )
        TunerCard(
            name = "SUSPENSION & AERO",
            delta = "+0.15 Downforce",
            level = upgrades.suspensionStage,
            maxLevel = 3,
            canUpgrade = upgrades.suspensionStage < 3 && credits >= upgradeCost,
            cost = upgradeCost,
            onUpgrade = { viewModel.upgradePart("SUSPENSION") },
            modifier = Modifier.weight(1f),
            isShort = isShort
        )
        TunerCard(
            name = "WEIGHT REDUCTION",
            delta = "-55 kg Carbon Diet",
            level = upgrades.weightReduction,
            maxLevel = 3,
            canUpgrade = upgrades.weightReduction < 3 && credits >= upgradeCost,
            cost = upgradeCost,
            onUpgrade = { viewModel.upgradePart("WEIGHT") },
            modifier = Modifier.weight(1f),
            isShort = isShort
        )
    }
}

@Composable
private fun TunerCard(
    name: String,
    delta: String,
    level: Int,
    maxLevel: Int,
    canUpgrade: Boolean,
    cost: Int,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    isShort: Boolean = false
) {
    Surface(
        color = Color(0xFF0F1420),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = if (isShort) 4.dp else 6.dp),
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
                color = if (level == maxLevel) Color(0xFFFFB300) else Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(3.dp))
            Button(
                onClick = onUpgrade,
                enabled = canUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B2436),
                    disabledContainerColor = Color(0xFF0A0D15)
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(26.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (level == maxLevel) "MAX" else "%,d CR".format(cost),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = if (canUpgrade) Color.White else Color(0xFF616161)
                )
            }
        }
    }
}
