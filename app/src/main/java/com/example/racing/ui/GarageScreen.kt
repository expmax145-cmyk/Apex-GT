package com.example.racing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.racing.model.CarDefinition
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId
import com.example.racing.engine.PaintFinishType

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

    var activeTab by remember { mutableIntStateOf(0) } // 0: Vehicles, 1: Paint, 2: Performance

    val isUnlocked = unlockedIds.contains(selectedCar.id)

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 700.dp
        val isShort = maxHeight < 390.dp
        val horizontalPad = if (isCompact) 12.dp else 20.dp

        // 1. Top Bar: Back button, Title & Credits balance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = if (isShort) 8.dp else 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("garage_back_button")
                        .size(44.dp)
                        .background(Color(0xCC0E131F), RoundedCornerShape(12.dp))
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
                        fontSize = if (isCompact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "TOUCH & DRAG TO ROTATE VEHICLE",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Credits Balance & Dev Cash Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xCC0E131F),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFD700)),
                    modifier = Modifier.clickable { viewModel.grantDevelopmentFunds() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "CR", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%,d".format(credits),
                            color = Color.White,
                            fontSize = if (isCompact) 13.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Development Currency Injector Button
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
                        text = "+$500K DEV",
                        color = Color(0xFF060910),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 2. Bottom Control Dashboard
        Surface(
            color = Color(0xF2090D16),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2AFFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {

                // Navigation Row: Left Tabs vs Right Track-Selector & Start Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Customizer Tabs
                    Row(
                        modifier = Modifier.weight(0.55f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GarageTabButton(
                            title = "CARS",
                            icon = Icons.Default.DirectionsCar,
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        GarageTabButton(
                            title = "PAINT",
                            icon = Icons.Default.ColorLens,
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        GarageTabButton(
                            title = "TUNING",
                            icon = Icons.Default.Build,
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Right: Track Selector (Left of Start) + Start Race Button
                    Row(
                        modifier = Modifier.weight(0.45f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Track Selector Control
                        TrackSelectorControl(
                            selectedTrack = selectedTrack,
                            onSelectNextTrack = {
                                val all = TrackId.values()
                                val nextIdx = (selectedTrack.ordinal + 1) % all.size
                                viewModel.selectTrack(all[nextIdx])
                            },
                            modifier = Modifier.weight(1.1f)
                        )

                        // Prominent Start Race Button in bottom-right corner!
                        Button(
                            onClick = {
                                if (isUnlocked) {
                                    viewModel.startRace(RaceMode.CIRCUIT_RACE)
                                } else {
                                    viewModel.buySelectedCar()
                                }
                            },
                            modifier = Modifier
                                .testTag("start_race_button")
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUnlocked) Color(0xFFFF3D00) else Color(0xFFFF9100)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.DirectionsCar,
                                contentDescription = if (isUnlocked) "Start Race" else "Buy Car",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUnlocked) "START" else "BUY",
                                color = Color.White,
                                fontSize = 13.sp,
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
                        unlockedIds = unlockedIds,
                        credits = credits
                    )
                    1 -> PaintTab(
                        selectedCar = selectedCar,
                        selectedColorIdx = selectedColorIdx,
                        selectedFinish = selectedPaintFinish,
                        onSelectColor = { viewModel.selectCarColor(it) },
                        onSelectFinish = { viewModel.selectCarPaintFinish(it) }
                    )
                    2 -> TuningTab(
                        viewModel = viewModel,
                        upgrades = upgrades,
                        credits = credits
                    )
                }
            }
        }
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
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "TRACK",
                        color = Color(0xFF78909C),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedTrack.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = "➔",
                color = trackColor,
                fontSize = 12.sp,
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
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) Color(0xFF00E5FF) else Color(0xFF78909C),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFF78909C),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VehiclesTab(
    viewModel: GameViewModel,
    selectedCar: CarDefinition,
    unlockedIds: Set<String>,
    credits: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isOwned) {
                        Text(
                            text = if (isSelected) "SELECTED" else "OWNED",
                            color = if (isSelected) Color(0xFF00E676) else Color(0xFF81C784),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "%,d CR".format(car.basePrice),
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
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
                            .size(34.dp)
                            .background(swatchColor, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color(0x44FFFFFF),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else Color(0xFF78909C),
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "CLEARCOAT & SURFACE FINISH",
            color = Color(0xFF90A4AE),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
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
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = finish.displayName,
                            color = if (isSelected) Color.White else Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TuningTab(
    viewModel: GameViewModel,
    upgrades: com.example.racing.model.CarUpgradeState,
    credits: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        UpgradeCard(
            name = "ECU STAGE",
            level = upgrades.engineStage,
            maxLevel = 3,
            canUpgrade = upgrades.engineStage < 3 && credits >= 3500,
            onUpgrade = { viewModel.upgradePart("ENGINE") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "TIRE COMPOUND",
            level = upgrades.tireStage,
            maxLevel = 3,
            canUpgrade = upgrades.tireStage < 3 && credits >= 3500,
            onUpgrade = { viewModel.upgradePart("TIRES") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "SUSPENSION",
            level = upgrades.suspensionStage,
            maxLevel = 3,
            canUpgrade = upgrades.suspensionStage < 3 && credits >= 3500,
            onUpgrade = { viewModel.upgradePart("SUSPENSION") },
            modifier = Modifier.weight(1f)
        )
        UpgradeCard(
            name = "WEIGHT DROP",
            level = upgrades.weightReduction,
            maxLevel = 3,
            canUpgrade = upgrades.weightReduction < 3 && credits >= 3500,
            onUpgrade = { viewModel.upgradePart("WEIGHT") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun UpgradeCard(
    name: String,
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
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "STAGE $level/$maxLevel",
                color = if (level == maxLevel) Color(0xFFFFD700) else Color(0xFF00E5FF),
                fontSize = 10.sp,
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
                    text = if (level == maxLevel) "MAX" else "+3,500CR",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
