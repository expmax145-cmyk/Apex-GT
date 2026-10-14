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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.LeaderboardRacer

@Composable
fun RaceSummaryScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val timing = viewModel.raceTiming
    val pos = timing.finishPosition
    val posTitle = when (pos) {
        1 -> "1ST PLACE - VICTORY!"
        2 -> "2ND PLACE - PODIUM"
        3 -> "3RD PLACE - PODIUM"
        else -> "$pos TH PLACE FINISH"
    }
    val badgeColor = when (pos) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color(0xFF78909C)
    }
    val rewardCr = when (pos) {
        1 -> 7500
        2 -> 4500
        3 -> 3000
        else -> 1500
    }

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
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor),
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
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
                    Text(
                        text = posTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Official Classification Table
                if (results.isNotEmpty()) {
                    Text(
                        text = "OFFICIAL RACE CLASSIFICATION",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF141C2B),
                        shape = RoundedCornerShape(10.dp),
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

                // Stats Table
                Surface(
                    color = Color(0xFF161E30),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        StatLine(label = "BEST LAP TIME", value = timing.formatTime(timing.bestLapTimeSec), isHighlight = true)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatLine(label = "FINAL LAP TIME", value = timing.formatTime(timing.lastLapTimeSec))
                        Spacer(modifier = Modifier.height(6.dp))
                        StatLine(label = "PRIZE REWARD", value = "+%,d CR".format(rewardCr), isGold = true)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
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
                        shape = RoundedCornerShape(12.dp)
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
                        shape = RoundedCornerShape(12.dp)
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
            .background(rowBg, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(posColor.copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, posColor, CircleShape),
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
                fontSize = 13.sp,
                fontWeight = if (racer.isPlayer) FontWeight.Black else FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• ${racer.carName}",
                color = Color(0xFF78909C),
                fontSize = 11.sp
            )
        }

        Text(
            text = racer.gapToLeaderText,
            color = if (racer.position == 1) Color(0xFF00E676) else Color(0xFFCFD8DC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
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
