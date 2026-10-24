package com.example.racing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.CarDefinition
import com.example.racing.model.TrackId

/**
 * Phase 15: Premium Racing Player Profile Screen
 *
 * Immersive driver profile featuring driver level, rank, career statistics,
 * win rates, best lap records across tracks, and editable driver credentials
 * wrapped in an elite motorsport telemetry aesthetic.
 */
@Composable
fun ProfileScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val playerName by viewModel.playerName.collectAsState()
    val playerLevel by viewModel.playerLevel.collectAsState()
    val racesCompleted by viewModel.racesCompleted.collectAsState()
    val racesWon by viewModel.racesWon.collectAsState()
    val driverXp by viewModel.driverXp.collectAsState()
    val credits by viewModel.playerCredits.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val unlockedCars by viewModel.unlockedCarIds.collectAsState()
    val careerPoints by viewModel.careerChampionshipPoints.collectAsState()
    val championshipEntries by viewModel.championshipEntries.collectAsState()
    val championshipWins by viewModel.championshipWins.collectAsState()
    val trophiesEarned by viewModel.trophiesEarned.collectAsState()
    val completedCups by viewModel.completedCups.collectAsState()
    val activeChamp by viewModel.activeChampionship.collectAsState()

    var isEditingName by remember { mutableStateOf(false) }
    var tempNameInput by remember { mutableStateOf(playerName) }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    val winRate = if (racesCompleted > 0) (racesWon.toFloat() / racesCompleted.toFloat() * 100f) else 0f
    val xpNextLevel = 10000
    val xpProgress = (driverXp.toFloat() / xpNextLevel.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF060910),
                    0.5f to Color(0xFF0B101A),
                    1.0f to Color(0xFF04060A)
                )
            )
            .padding(safeInsets)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────
            // TOP HEADER BAR
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF131B2E), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF233250), RoundedCornerShape(12.dp))
                            .testTag("back_to_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Menu",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(28.dp)
                            .background(Color(0xFFFF3823), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DRIVER PROFILE & CAREER",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "OFFICIAL FIA MOTORSPORT TELEMETRY",
                            color = Color(0xFF8B9BB4),
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Credits Badge
                Surface(
                    color = Color(0xFF131B2E),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233250))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFFFF3823),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "cr %,d".format(credits),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─────────────────────────────────────────────────────────────
            // SCROLLABLE CONTENT BODY
            // ─────────────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // 1. Driver Credentials Card
                item {
                    Surface(
                        color = Color(0xFF0E1524),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223250))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Driver Avatar Initials
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFFFF3823), Color(0xFF991100))
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = playerName.split(" ")
                                            .take(2)
                                            .mapNotNull { it.firstOrNull()?.uppercase() }
                                            .joinToString("")
                                            .ifEmpty { "AP" }
                                        Text(
                                            text = initials,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column {
                                        if (isEditingName) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = tempNameInput,
                                                    onValueChange = { tempNameInput = it },
                                                    singleLine = true,
                                                    modifier = Modifier.width(220.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFFFF3823),
                                                        unfocusedBorderColor = Color(0xFF334466),
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        viewModel.updatePlayerName(tempNameInput)
                                                        isEditingName = false
                                                    },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(Color(0xFFFF3823), RoundedCornerShape(8.dp))
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Save Name",
                                                        tint = Color.White
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = playerName,
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        tempNameInput = playerName
                                                        isEditingName = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Name",
                                                        tint = Color(0xFF8B9BB4),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MilitaryTech,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "PRO ELITE DRIVER • RANK LVL $playerLevel",
                                                color = Color(0xFFFFD700),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Level Badge Box
                                Surface(
                                    color = Color(0xFF162035),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3E63))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "LEVEL",
                                            color = Color(0xFF8B9BB4),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$playerLevel",
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Driver XP Progress
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "DRIVER XP PROGRESSION",
                                        color = Color(0xFF8B9BB4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "%,d / %,d XP".format(driverXp, xpNextLevel),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { xpProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFFFF3823),
                                    trackColor = Color(0xFF162035),
                                )
                            }
                        }
                    }
                }

                // 2. Career Statistics Grid
                item {
                    Text(
                        text = "CAREER STATISTICS & PERFORMANCE",
                        color = Color(0xFF8B9BB4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatCard(
                            title = "RACES COMPLETED",
                            value = "$racesCompleted",
                            subtitle = "Official Grid Starts",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "VICTORIES",
                            value = "$racesWon",
                            subtitle = "1st Place Finishes",
                            icon = Icons.Default.EmojiEvents,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "WIN RATE",
                            value = "%.1f%%".format(winRate),
                            subtitle = "Podium Efficiency",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Championship Career & Trophies Showcase
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CHAMPIONSHIP CAREER & TROPHIES",
                        color = Color(0xFF8B9BB4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatCard(
                            title = "CUPS ENTERED",
                            value = "$championshipEntries",
                            subtitle = "Tournaments Started",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "CUPS WON",
                            value = "$championshipWins",
                            subtitle = "Championship Titles",
                            icon = Icons.Default.EmojiEvents,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "CAREER POINTS",
                            value = "$careerPoints",
                            subtitle = "Cumulative FIA PTS",
                            icon = Icons.Default.MilitaryTech,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    // Trophies Row
                    Surface(
                        color = Color(0xFF0E1524),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223250))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrophyBadgeItem(
                                title = "BRONZE CUP",
                                isEarned = trophiesEarned.contains("BRONZE"),
                                color = Color(0xFFCD7F32)
                            )
                            TrophyBadgeItem(
                                title = "SILVER CUP",
                                isEarned = trophiesEarned.contains("SILVER"),
                                color = Color(0xFFC0C0C0)
                            )
                            TrophyBadgeItem(
                                title = "GOLD CUP",
                                isEarned = trophiesEarned.contains("GOLD"),
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                // 3. Garage & Selected Car Overview
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FEATURED GARAGE VEHICLE",
                        color = Color(0xFF8B9BB4),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Surface(
                        color = Color(0xFF0E1524),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223250))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedCar.name.uppercase(),
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedCar.category,
                                    color = Color(0xFFFF3823),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Unlocked Vehicles in Garage: ${unlockedCars.size} / ${CarDefinition.ALL_CARS.size}",
                                    color = Color(0xFF8B9BB4),
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.navigateTo(GameScreen.GARAGE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF3823),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "OPEN GARAGE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0E1524),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF223250))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF8B9BB4),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF3823),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF6B7B94),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TrophyBadgeItem(
    title: String,
    isEarned: Boolean,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (isEarned) color.copy(alpha = 0.2f) else Color(0x11FFFFFF), CircleShape)
                .border(1.5.dp, if (isEarned) color else Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = title,
                tint = if (isEarned) color else Color(0xFF556070),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = if (isEarned) Color.White else Color(0xFF556070),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isEarned) "EARNED" else "LOCKED",
            color = if (isEarned) color else Color(0xFF404A58),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black
        )
    }
}
