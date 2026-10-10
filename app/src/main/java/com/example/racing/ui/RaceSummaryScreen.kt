package com.example.racing.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.racing.model.RaceMode

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
        1 -> 6500
        2 -> 4000
        3 -> 2500
        else -> 1000
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xDD080B12))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF101522),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, badgeColor),
            modifier = Modifier.width(460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trophy Icon
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Trophy",
                    tint = badgeColor,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = posTitle,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Table
                Surface(
                    color = Color(0xFF161E30),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        StatLine(label = "BEST LAP TIME", value = timing.formatTime(timing.bestLapTimeSec), isHighlight = true)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatLine(label = "FINAL LAP TIME", value = timing.formatTime(timing.lastLapTimeSec))
                        Spacer(modifier = Modifier.height(8.dp))
                        StatLine(label = "PRIZE REWARD", value = "+%,d CR".format(rewardCr), isGold = true)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                        modifier = Modifier
                            .testTag("summary_menu_button")
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Menu", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "MAIN MENU", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.startRace(viewModel.activeRaceMode.value) },
                        modifier = Modifier
                            .testTag("summary_restart_button")
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3D00)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RACE AGAIN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
                isGold -> Color(0xFFFFD700)
                isHighlight -> Color(0xFF00E5FF)
                else -> Color.White
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
