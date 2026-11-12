package com.example.racing.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racing.model.RaceMode
import com.example.ui.theme.*

/**
 * Redesigned Main Menu with sharp motorsport geometry and premium blue 3D theme.
 */
@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val credits by viewModel.playerCredits.collectAsState()
    val playerName by viewModel.playerName.collectAsState()
    val playerLevel by viewModel.playerLevel.collectAsState()
    
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MotorsportMidnight)
    ) {
        // 3D Car Showcase (Background)
        RacingSurfaceView(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MotorsportMidnight.copy(alpha = 0.4f),
                        0.2f to Color.Transparent,
                        0.8f to Color.Transparent,
                        1f to MotorsportMidnight.copy(alpha = 0.5f)
                    )
                )
        )

        // Main UI Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(safeInsets)
                .padding(24.dp)
        ) {
            // TOP-LEFT: Player Profile
            PlayerProfilePanel(
                name = playerName,
                level = playerLevel,
                onClick = { viewModel.navigateTo(GameScreen.PROFILE) },
                modifier = Modifier.align(Alignment.TopStart)
            )

            // TOP-RIGHT: Currency and Settings
            Row(
                modifier = Modifier.align(Alignment.TopEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrencyDisplay(credits = credits)
                
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.SETTINGS) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MotorsportGlass, RectangleShape)
                        .border(1.dp, MotorsportRacingBlue, RectangleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }

            // BOTTOM-RIGHT: Tracks, Garage, Start
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MotorsportButton(
                    onClick = { viewModel.navigateTo(GameScreen.TRACKS) },
                    text = "TRACKS",
                    accentColor = MotorsportElectricBlue,
                    modifier = Modifier.testTag("menu_tracks_button")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    MotorsportButton(
                        onClick = { viewModel.navigateTo(GameScreen.GARAGE) },
                        text = "GARAGE",
                        accentColor = MotorsportRacingBlue,
                        modifier = Modifier.testTag("menu_garage_button")
                    )

                    MotorsportButton(
                        onClick = { viewModel.startRace(RaceMode.CIRCUIT_RACE) },
                        text = "START",
                        accentColor = Color(0xFFFF3823),
                        modifier = Modifier.testTag("menu_start_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerProfilePanel(
    name: String,
    level: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MotorsportGlass,
        shape = ParallelogramShape(10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MotorsportRacingBlue),
        modifier = modifier.testTag("menu_profile_panel")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.avatar_racer_1),
                contentDescription = "Player Avatar",
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, MotorsportCyan, CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = name.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "LEVEL $level",
                    color = MotorsportCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CurrencyDisplay(credits: Int) {
    Surface(
        color = MotorsportGlass,
        shape = ParallelogramShape(10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700)),
        modifier = Modifier.testTag("menu_currency_display")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CR",
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "$%,d".format(credits),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MotorsportButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MotorsportElectricBlue
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        onClick = onClick,
        color = MotorsportGlass,
        shape = ParallelogramShape(12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
        interactionSource = interactionSource,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp)
        ) {
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                    .background(accentColor.copy(alpha = 0.3f))
            )
        }
    }
}
