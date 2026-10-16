package com.example.racing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.racing.model.CarDefinition
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId

/**
 * Phase 14: Premium Motorsport Main Menu & Cinematic Automotive Showcase
 *
 * Designed to provide a photorealistic, immersive gateway into Apex GT.
 * Integrates live OpenGL ES 3.0 hero turntable rendering underneath frosted
 * dark-carbon telemetry panels, with responsive landscape navigation across
 * varied mobile screen aspect ratios.
 */
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val credits by viewModel.playerCredits.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeOfDay by viewModel.timeOfDay.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val upgrades by viewModel.carUpgrades.collectAsState()
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Directional vignette: deep obsidian scrim on the left for contrast and menu readability,
            // opening up to crystal-clear 3D hero vehicle viewport on the right.
            .background(
                Brush.horizontalGradient(
                    0.00f to Color(0xF205080E),
                    0.42f to Color(0xDC080D16),
                    0.65f to Color(0x66080D16),
                    1.00f to Color(0x1A080D16)
                )
            )
            .padding(safeInsets)
    ) {
        val isCompactWidth = maxWidth < 760.dp
        val isShortHeight = maxHeight < 400.dp
        val hPadding = if (isCompactWidth) 14.dp else 24.dp
        val vPadding = if (isShortHeight) 8.dp else 14.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPadding, vertical = vPadding)
        ) {
            // ─────────────────────────────────────────────────────────────
            // TOP STATUS & BRANDING BAR
            // ─────────────────────────────────────────────────────────────
            TopMenuBar(
                credits = credits,
                selectedCar = selectedCar,
                isCompact = isCompactWidth,
                onAddFunds = { viewModel.grantDevelopmentFunds() },
                onOpenSettings = { viewModel.navigateTo(GameScreen.SETTINGS) }
            )

            Spacer(modifier = Modifier.height(if (isShortHeight) 6.dp else 12.dp))

            // ─────────────────────────────────────────────────────────────
            // MAIN VIEWPORT: LEFT ACTION PANE & RIGHT HERO SHOWCASE
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 12.dp else 22.dp)
            ) {
                // Left Column: Race Event Launches & Environment Strip
                LeftMenuPane(
                    viewModel = viewModel,
                    selectedTrack = selectedTrack,
                    timeOfDay = timeOfDay,
                    weatherCondition = weatherCondition,
                    isCompact = isCompactWidth,
                    isShort = isShortHeight,
                    modifier = Modifier
                        .weight(if (isCompactWidth) 1.25f else 1.10f)
                        .fillMaxHeight()
                )

                // Right Column: Hero Vehicle Showcase, Quick Switcher & Workshop Access
                RightHeroPane(
                    viewModel = viewModel,
                    selectedCar = selectedCar,
                    upgrades = upgrades,
                    isCompact = isCompactWidth,
                    isShort = isShortHeight,
                    modifier = Modifier
                        .weight(if (isCompactWidth) 0.95f else 1.05f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Top menu bar with brand identity, current car indicator, credits and settings action.
 */
@Composable
private fun TopMenuBar(
    credits: Int,
    selectedCar: CarDefinition,
    isCompact: Boolean,
    onAddFunds: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Lockup
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Precision motorsport accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (isCompact) 26.dp else 32.dp)
                    .background(Color(0xFFFF3823), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "APEX GT",
                        color = Color.White,
                        fontSize = if (isCompact) 22.sp else 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0x33FF3823),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF3823))
                    ) {
                        Text(
                            text = "GT RACING",
                            color = Color(0xFFFF6E40),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "PRECISION AUTOMOTIVE SIMULATION",
                    color = Color(0xFF7E8B9B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        // Action & Economy Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Credit Balance Chip
            Surface(
                color = Color(0xDD0D131E),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFB300)),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color(0xFFFFB300)),
                        onClick = onAddFunds
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                            color = Color(0xFF090D14),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%,d".format(credits),
                        color = Color.White,
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Quick Development Cash Grant Button
            Button(
                onClick = onAddFunds,
                modifier = Modifier
                    .testTag("dev_add_funds_main_button")
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+$500K",
                    color = Color(0xFF060910),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .testTag("settings_button")
                    .size(44.dp)
                    .background(Color(0xDD0D131E), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Left pane hosting event mode entry cards and track/environment selector strips.
 */
@Composable
private fun LeftMenuPane(
    viewModel: GameViewModel,
    selectedTrack: TrackId,
    timeOfDay: com.example.racing.model.TimeOfDay,
    weatherCondition: com.example.racing.model.WeatherCondition,
    isCompact: Boolean,
    isShort: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(if (isShort) 8.dp else 10.dp)
    ) {
        // Section Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFFFF3823), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "RACE DEPLOYMENT",
                color = Color(0xFF90A4AE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }

        // 1. Circuit Cup Mode Card
        CinematicModeCard(
            title = "CIRCUIT CUP",
            subtitle = "3 Laps against 3 AI Competitors • Grid Start",
            tag = "3 LAPS • AI GRID",
            icon = Icons.Default.Flag,
            accentColor = Color(0xFFFF3823),
            onClick = { viewModel.startRace(RaceMode.CIRCUIT_RACE) },
            testTag = "circuit_race_card",
            isShort = isShort
        )

        // 2. Time Attack Mode Card
        CinematicModeCard(
            title = "TIME ATTACK",
            subtitle = "Hunt Personal Lap Records • Sector Split Deltas",
            tag = "SOLO • DELTA TELEMETRY",
            icon = Icons.Default.Timer,
            accentColor = Color(0xFF00E5FF),
            onClick = { viewModel.startRace(RaceMode.TIME_ATTACK) },
            testTag = "time_attack_card",
            isShort = isShort
        )

        // 3. Free Drive Mode Card
        CinematicModeCard(
            title = "FREE DRIVE",
            subtitle = "Open Circuit • Telemetry & Drift Practice",
            tag = "OPEN TRACK • DRIFT",
            icon = Icons.Default.DirectionsCar,
            accentColor = Color(0xFF00E676),
            onClick = { viewModel.startRace(RaceMode.FREE_DRIVE) },
            testTag = "free_drive_card",
            isShort = isShort
        )

        Spacer(modifier = Modifier.height(2.dp))

        // ─────────────────────────────────────────────────────────────
        // ACTIVE TRACK & ENVIRONMENT COMMAND STRIP
        // ─────────────────────────────────────────────────────────────
        // Track Quick-Cycle Card
        Surface(
            color = Color(0xDD0D131E),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val all = TrackId.values()
                    val next = all[(selectedTrack.ordinal + 1) % all.size]
                    viewModel.selectTrack(next)
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isShort) 8.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val trackIcon = when (selectedTrack) {
                        TrackId.GRAND_PRIX -> Icons.Default.Flag
                        TrackId.ALPINE_PASS -> Icons.Default.Landscape
                        TrackId.NEON_METROPOLIS -> Icons.Default.LocationCity
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0x2200E5FF), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = trackIcon,
                            contentDescription = selectedTrack.title,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CIRCUIT",
                                color = Color(0xFF7E8B9B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${selectedTrack.lengthKm} KM",
                                color = Color(0xFF00E5FF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = selectedTrack.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NEXT TRACK",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Track",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Time of Day & Weather Pill Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time of Day Selector Pill
            Surface(
                color = Color(0xDD0D131E),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFB300)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.cycleTimeOfDay() }
                    .testTag("main_menu_time_of_day_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isShort) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TIME OF DAY",
                            color = Color(0xFF7E8B9B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = timeOfDay.displayName,
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cycle Time",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Weather Selector Pill
            Surface(
                color = Color(0xDD0D131E),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.cycleWeather() }
                    .testTag("main_menu_weather_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = if (isShort) 6.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WEATHER",
                            color = Color(0xFF7E8B9B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = weatherCondition.displayName,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Cycle Weather",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Right pane showcasing the hero vehicle with telemetry, quick switching, and garage workshop CTA.
 */
@Composable
private fun RightHeroPane(
    viewModel: GameViewModel,
    selectedCar: CarDefinition,
    upgrades: com.example.racing.model.CarUpgradeState,
    isCompact: Boolean,
    isShort: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Hint: Interactive 3D Turntable Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                color = Color(0x88090D14),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⟳",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DRAG TO ORBIT • DOUBLE TAP TO RESET",
                        color = Color(0xFFB0BEC5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Hero Car Card & Workshop Launch
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Vehicle Spec & Telemetry Panel
            Surface(
                color = Color(0xEE0D131F),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(if (isShort) 10.dp else 14.dp)
                ) {
                    // Category & Quick Car Switcher Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x33FFB300),
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFB300))
                        ) {
                            Text(
                                text = selectedCar.category.uppercase(),
                                color = Color(0xFFFFB300),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Quick Car Selector Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val allCars = CarDefinition.ALL_CARS
                            val curIdx = allCars.indexOfFirst { it.id == selectedCar.id }.coerceAtLeast(0)

                            IconButton(
                                onClick = {
                                    val prev = allCars[(curIdx - 1 + allCars.size) % allCars.size]
                                    viewModel.selectCar(prev)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Vehicle",
                                    tint = Color.White
                                )
                            }

                            Text(
                                text = "${curIdx + 1}/${allCars.size}",
                                color = Color(0xFF7E8B9B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            IconButton(
                                onClick = {
                                    val next = allCars[(curIdx + 1) % allCars.size]
                                    viewModel.selectCar(next)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Vehicle",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Vehicle Name
                    Text(
                        text = selectedCar.name,
                        color = Color.White,
                        fontSize = if (isShort) 16.sp else 19.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = selectedCar.description,
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Performance Telemetry Bars
                    StatRow(
                        name = "TOP SPEED",
                        value = selectedCar.topSpeedKmh / 400f,
                        display = "${selectedCar.topSpeedKmh.toInt()} km/h",
                        accentColor = Color(0xFFFF3823)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatRow(
                        name = "ACCEL",
                        value = selectedCar.accelerationRating / 10f,
                        display = "${selectedCar.accelerationRating}/10",
                        accentColor = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    StatRow(
                        name = "HANDLING",
                        value = selectedCar.handlingRating / 10f,
                        display = "${selectedCar.handlingRating}/10",
                        accentColor = Color(0xFF00E676)
                    )
                }
            }

            // Primary Workshop Action Button
            Button(
                onClick = { viewModel.openGarage() },
                modifier = Modifier
                    .testTag("garage_button")
                    .fillMaxWidth()
                    .height(if (isShort) 44.dp else 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF161F2E)),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF)),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0x2200E5FF), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Garage",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GARAGE & TUNING WORKSHOP",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Customize Paint, Aero & Upgrades",
                                color = Color(0xFF7E8B9B),
                                fontSize = 9.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Garage",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Reusable Cinematic Motorsport Mode Card with tactical styling.
 */
@Composable
fun CinematicModeCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    isShort: Boolean = false
) {
    Surface(
        color = Color(0xDD0D131E),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2BFFFFFF)),
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = accentColor),
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isShort) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(if (isShort) 36.dp else 40.dp)
                    .background(accentColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = if (isShort) 13.sp else 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = tag,
                            color = accentColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    color = Color(0xFF8A99A8),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Launch",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Telemetry spec row displaying title, bar meter, and numerical value.
 */
@Composable
fun StatRow(
    name: String,
    value: Float,
    display: String,
    accentColor: Color = Color(0xFFFF3823)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = Color(0xFF7E8B9B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(76.dp)
        )
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .padding(horizontal = 8.dp),
            color = accentColor,
            trackColor = Color(0xFF182232)
        )
        Text(
            text = display,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
