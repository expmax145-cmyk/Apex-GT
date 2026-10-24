package com.example.racing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.ChampionshipDriverStanding
import com.example.racing.model.ChampionshipTier
import com.example.racing.model.LeaderboardRacer
import com.example.racing.model.RaceMode

@Composable
fun RaceSummaryScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val timing = viewModel.raceTiming
    val pos = timing.finishPosition
    val activeMode by viewModel.activeRaceMode.collectAsState()
    val activeChamp by viewModel.activeChampionship.collectAsState()

    val isChampionship = activeMode == RaceMode.CHAMPIONSHIP && activeChamp != null
    val champState = activeChamp

    val isCupCompleted = isChampionship && champState?.isCupCompleted == true
    val playerWonCup = isCupCompleted && champState?.getSortedStandings()?.firstOrNull()?.isPlayer == true

    val posTitle = when {
        isCupCompleted && playerWonCup -> "CHAMPIONSHIP VICTORY!"
        isCupCompleted -> "CHAMPIONSHIP CONCLUDED"
        isChampionship -> "${champState?.tier?.title?.uppercase()} • ROUND ${champState?.currentRoundIndex?.plus(1)}"
        pos == 1 -> "1ST PLACE - VICTORY!"
        pos == 2 -> "2ND PLACE - PODIUM"
        pos == 3 -> "3RD PLACE - PODIUM"
        else -> "$pos TH PLACE FINISH"
    }

    val badgeColor = when {
        isCupCompleted && playerWonCup -> Color(0xFFFFD700)
        pos == 1 -> Color(0xFFFFD700)
        pos == 2 -> Color(0xFFC0C0C0)
        pos == 3 -> Color(0xFFCD7F32)
        else -> Color(0xFF78909C)
    }

    val rewardCr = when (pos) {
        1 -> 7500
        2 -> 4500
        3 -> 3000
        else -> 1500
    }
    val roundPts = if (isChampionship) ChampionshipTier.getPointsForPosition(pos) else 0

    val results = viewModel.lastRaceResults

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xEE080B12))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF101522),
            shape = RectangleShape,
            border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy Icon and Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = badgeColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = posTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        if (isChampionship && !isCupCompleted) {
                            Text(
                                text = "STAGE FINISH: POSITION $pos",
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Championship Celebration Banner (If Cup Completed)
                if (isCupCompleted) {
                    Surface(
                        color = if (playerWonCup) Color(0x33FFD700) else Color(0x22FFFFFF),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor),
                        modifier = Modifier
                            .testTag("championship_trophy_banner")
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (playerWonCup) "🏆 ${champState?.tier?.trophyName?.uppercase()} AWARDED!" else "TOURNAMENT COMPLETE",
                                color = if (playerWonCup) Color(0xFFFFD700) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (playerWonCup)
                                    "CUP CHAMPION BONUS: +$%,d CR • +%,d XP".format(champState?.tier?.creditReward ?: 0, champState?.tier?.xpReward ?: 0)
                                else
                                    "FINAL RANK: #${champState?.getPlayerPosition()} • Great driving!",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Official Classification Table (Per Race)
                if (results.isNotEmpty()) {
                    Text(
                        text = "STAGE CLASSIFICATION",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF141C2B),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 10.dp)) {
                            results.forEach { racer ->
                                RacerClassificationRow(racer = racer)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Cumulative Championship Standings (If Championship)
                if (isChampionship && champState != null) {
                    val sortedStandings = champState.getSortedStandings()
                    Text(
                        text = "CUMULATIVE CHAMPIONSHIP STANDINGS",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF0D1726),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                        modifier = Modifier
                            .testTag("championship_standings_table")
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp)) {
                            sortedStandings.forEachIndexed { idx, standing ->
                                ChampionshipStandingRow(rank = idx + 1, standing = standing)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Stats Table
                Surface(
                    color = Color(0xFF161E30),
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StatLine(label = "STAGE TIME", value = timing.formatTime(timing.lastLapTimeSec), isHighlight = true)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isChampionship) {
                            StatLine(label = "CHAMPIONSHIP POINTS", value = "+$roundPts PTS", isHighlight = true)
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        StatLine(label = "STAGE PRIZE", value = "+%,d CR".format(rewardCr), isGold = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (isChampionship && champState != null) {
                    if (isCupCompleted) {
                        // Cup is finished: Return to Championship menu
                        Button(
                            onClick = {
                                viewModel.abandonActiveChampionship()
                                viewModel.navigateTo(GameScreen.CHAMPIONSHIP)
                            },
                            modifier = Modifier
                                .testTag("summary_championship_finish_button")
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                        ) {
                            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CLAIM & RETURN TO TOURNAMENT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        // Intermediate round: Next Round or Save & Exit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                                modifier = Modifier
                                    .testTag("summary_save_menu_button")
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RectangleShape
                            ) {
                                Icon(imageVector = Icons.Default.Home, contentDescription = "Menu", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "SAVE & EXIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.advanceToNextChampionshipRound() },
                                modifier = Modifier
                                    .testTag("summary_next_round_button")
                                    .weight(1.3f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                shape = RectangleShape
                            ) {
                                Text(
                                    text = "NEXT ROUND",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.Black)
                            }
                        }
                    }
                } else {
                    // Regular Race Mode Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                            modifier = Modifier
                                .testTag("summary_menu_button")
                                .weight(1f)
                                .height(46.dp),
                            shape = RectangleShape
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Menu", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "MAIN MENU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.startRace(viewModel.activeRaceMode.value) },
                            modifier = Modifier
                                .testTag("summary_restart_button")
                                .weight(1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                            shape = RectangleShape
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "RACE AGAIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RacerClassificationRow(racer: LeaderboardRacer) {
    val posColor = when (racer.position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF78909C)
    }

    val rowBg = if (racer.isPlayer) Color(0x33FF6D00) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(posColor.copy(alpha = 0.2f), RectangleShape)
                    .border(1.dp, posColor, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${racer.position}",
                    color = posColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = racer.name + if (racer.isPlayer) " (YOU)" else "",
                color = if (racer.isPlayer) Color(0xFFFFB74D) else Color.White,
                fontSize = 12.sp,
                fontWeight = if (racer.isPlayer) FontWeight.Black else FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ${racer.carName}",
                color = Color(0xFF78909C),
                fontSize = 10.sp
            )
        }

        Text(
            text = racer.gapToLeaderText,
            color = if (racer.position == 1) Color(0xFF00E676) else Color(0xFFCFD8DC),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ChampionshipStandingRow(rank: Int, standing: ChampionshipDriverStanding) {
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF78909C)
    }

    val rowBg = if (standing.isPlayer) Color(0x330066FF) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(rankColor.copy(alpha = 0.2f), RectangleShape)
                    .border(1.dp, rankColor, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = rankColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = standing.driverName + if (standing.isPlayer) " (YOU)" else "",
                color = if (standing.isPlayer) Color(0xFF00E5FF) else Color.White,
                fontSize = 12.sp,
                fontWeight = if (standing.isPlayer) FontWeight.Black else FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ${standing.wins} WINS",
                color = Color(0xFF7E8B9B),
                fontSize = 10.sp
            )
        }

        Text(
            text = "${standing.points} PTS",
            color = Color(0xFFFFD700),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun StatLine(label: String, value: String, isHighlight: Boolean = false, isGold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF90A4AE),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = when {
                isGold -> Color(0xFFFFD54F)
                isHighlight -> Color(0xFF00E5FF)
                else -> Color.White
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
