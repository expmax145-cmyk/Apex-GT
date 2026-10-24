package com.example.racing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.ActiveChampionshipState
import com.example.racing.model.CarDefinition
import com.example.racing.model.ChampionshipTier

@Composable
fun ChampionshipScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeChamp by viewModel.activeChampionship.collectAsState()
    val unlockedCups by viewModel.unlockedCups.collectAsState()
    val completedCups by viewModel.completedCups.collectAsState()
    val trophiesEarned by viewModel.trophiesEarned.collectAsState()
    val careerPoints by viewModel.careerChampionshipPoints.collectAsState()
    val credits by viewModel.playerCredits.collectAsState()
    val playerLevel by viewModel.playerLevel.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()

    var showAbandonConfirm by remember { mutableStateOf(false) }

    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color(0xFF04060A),
                    0.4f to Color(0xFF0A0F1A),
                    1.0f to Color(0xFF030508)
                )
            )
            .padding(safeInsets)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("championship_back_button")
                            .background(Color(0xDD0D131E), RectangleShape)
                            .border(1.dp, Color(0x33FFFFFF), RectangleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "CHAMPIONSHIP TOUR",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "APEX GT OFFICIAL TOURNAMENT SERIES",
                            color = Color(0xFF0066FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Economy Pill
                Surface(
                    color = Color(0xDD0D131E),
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$%,d CR".format(credits),
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LVL $playerLevel",
                            color = Color(0xFF8A99A8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Career Overview Stats Bar
            CareerChampionshipSummaryCard(
                careerPoints = careerPoints,
                trophiesCount = trophiesEarned.size,
                selectedCar = selectedCar
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Active Cup Banner (If active)
            activeChamp?.let { champ ->
                ActiveTournamentCard(
                    champState = champ,
                    onResume = { viewModel.resumeActiveChampionship() },
                    onAbandon = { showAbandonConfirm = true }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Cup Tiers List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(ChampionshipTier.entries) { tier ->
                    val (isUnlocked, unlockDesc) = viewModel.checkCupUnlockStatus(tier)
                    val (isVehicleEligible, vehicleDesc) = viewModel.checkVehicleEligibility(tier, selectedCar)
                    val isCompleted = completedCups.contains(tier.id)
                    val isActive = activeChamp?.tier == tier

                    CupTierCard(
                        tier = tier,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        isActive = isActive,
                        isVehicleEligible = isVehicleEligible,
                        unlockDesc = unlockDesc,
                        vehicleDesc = vehicleDesc,
                        selectedCar = selectedCar,
                        playerCredits = credits,
                        onEnter = { viewModel.startChampionship(tier) },
                        onResume = { viewModel.resumeActiveChampionship() }
                    )
                }
            }
        }

        // Abandon Confirmation Overlay
        if (showAbandonConfirm) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF0F1522),
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF3823)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ABANDON CHAMPIONSHIP?",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Current tournament points and round progress will be lost. Unlocked cups and career points are preserved.",
                            color = Color(0xFF8A99A8),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAbandonConfirm = false },
                                shape = RectangleShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CANCEL", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    viewModel.abandonActiveChampionship()
                                    showAbandonConfirm = false
                                },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3823)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CONFIRM", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerChampionshipSummaryCard(
    careerPoints: Int,
    trophiesCount: Int,
    selectedCar: CarDefinition
) {
    Surface(
        color = Color(0xDD0B101A),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CAREER TROPHIES: $trophiesCount / 3",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "TOTAL CAREER POINTS: $careerPoints PTS",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Current Vehicle Tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ACTIVE CAR:",
                    color = Color(0xFF7E8B9B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0x330066FF),
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0066FF))
                ) {
                    Text(
                        text = selectedCar.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTournamentCard(
    champState: ActiveChampionshipState,
    onResume: () -> Unit,
    onAbandon: () -> Unit
) {
    val tier = champState.tier
    val currentRoundDisplay = champState.currentRoundIndex + 1
    val playerPos = champState.getPlayerPosition()
    val playerPoints = champState.standings.find { it.isPlayer }?.points ?: 0

    Surface(
        color = Color(0xEE0E182B),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
        modifier = Modifier
            .testTag("active_tournament_banner")
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF00E5FF),
                        shape = RectangleShape
                    ) {
                        Text(
                            text = "ACTIVE TOURNAMENT",
                            color = Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${tier.title} • Round $currentRoundDisplay of ${champState.totalRounds}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "NEXT: ${champState.currentTrack.title}",
                    color = Color(0xFF7E8B9B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Standings Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CURRENT STANDING: ",
                        color = Color(0xFF8A99A8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = when (playerPos) {
                            1 -> "1ST PLACE (LEADER)"
                            2 -> "2ND PLACE"
                            3 -> "3RD PLACE"
                            else -> "${playerPos}TH PLACE"
                        },
                        color = if (playerPos == 1) Color(0xFFFFD700) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$playerPoints PTS",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAbandon,
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF3823)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF3823))
                    ) {
                        Text("ABANDON", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onResume,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESUME ROUND", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CupTierCard(
    tier: ChampionshipTier,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    isActive: Boolean,
    isVehicleEligible: Boolean,
    unlockDesc: String,
    vehicleDesc: String,
    selectedCar: CarDefinition,
    playerCredits: Int,
    onEnter: () -> Unit,
    onResume: () -> Unit
) {
    val tierColor = when (tier) {
        ChampionshipTier.BRONZE -> Color(0xFFCD7F32)
        ChampionshipTier.SILVER -> Color(0xFFC0C0C0)
        ChampionshipTier.GOLD -> Color(0xFFFFD700)
    }

    val canAfford = playerCredits >= tier.entryFee

    Surface(
        color = Color(0xDD0D131E),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color(0xFF00E5FF) else if (isCompleted) tierColor else if (isUnlocked) Color(0x44FFFFFF) else Color(0x1AFFFFFF)
        ),
        modifier = Modifier
            .testTag("cup_card_${tier.id.lowercase()}")
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Cup Badge, Title, Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(tierColor.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, tierColor, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.EmojiEvents else if (!isUnlocked) Icons.Default.Lock else Icons.Default.EmojiEvents,
                            contentDescription = tier.title,
                            tint = if (isUnlocked) tierColor else Color(0xFF6E7A8A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tier.title.uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            if (isCompleted) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = tierColor.copy(alpha = 0.2f),
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, tierColor)
                                ) {
                                    Text(
                                        text = "TROPHY EARNED",
                                        color = tierColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = tier.subtitle,
                            color = Color(0xFF8A99A8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Rounds pill
                Surface(
                    color = Color(0x330066FF),
                    shape = RectangleShape
                ) {
                    Text(
                        text = "${tier.rounds.size} STAGES",
                        color = Color(0xFF80B3FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = tier.description,
                color = Color(0xFF9EABB8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stages Track Preview Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tier.rounds.forEachIndexed { idx, track ->
                    Surface(
                        color = Color(0x22FFFFFF),
                        shape = RectangleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "STAGE ${idx + 1}",
                                color = Color(0xFF7E8B9B),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rewards & Eligibility Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PRIZE: $%,d CR • +%,d XP".format(tier.creditReward, tier.xpReward),
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (tier.entryFee > 0) "ENTRY: $%,d CR".format(tier.entryFee) else "ENTRY: FREE",
                        color = Color(0xFF7E8B9B),
                        fontSize = 10.sp
                    )
                }

                // Action Button or Lock Reason
                if (!isUnlocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFF6644),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unlockDesc,
                            color = Color(0xFFFF6644),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!isVehicleEligible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = vehicleDesc,
                            color = Color(0xFFFFB300),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isActive) {
                    Button(
                        onClick = onResume,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        modifier = Modifier.testTag("resume_cup_${tier.id.lowercase()}")
                    ) {
                        Text("RESUME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onEnter,
                        enabled = canAfford,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleted) Color(0xFF1B283D) else Color(0xFF0066FF)
                        ),
                        modifier = Modifier.testTag("enter_cup_${tier.id.lowercase()}")
                    ) {
                        Text(
                            text = if (isCompleted) "REPLAY CUP" else "ENTER TOURNAMENT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
