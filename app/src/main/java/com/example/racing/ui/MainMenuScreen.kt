package com.example.racing.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MilitaryTech
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
    val playerName by viewModel.playerName.collectAsState()
    val playerLevel by viewModel.playerLevel.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val timeOfDay by viewModel.timeOfDay.collectAsState()
    val weatherCondition by viewModel.weatherCondition.collectAsState()
    val upgrades by viewModel.carUpgrades.collectAsState()
    val activeChamp by viewModel.activeChampionship.collectAsState()
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
            ProfileAndEconomyBar(
                credits = credits,
                playerName = playerName,
                playerLevel = playerLevel,
                isCompact = isCompactWidth,
                onAddFunds = { viewModel.grantDevelopmentFunds() },
                onOpenSettings = { viewModel.navigateTo(GameScreen.SETTINGS) },
                onOpenProfile = { viewModel.navigateTo(GameScreen.PROFILE) }
            )

            Spacer(modifier = Modifier.height(if (isShortHeight) 6.dp else 12.dp))

        // Middle Area: Car Showcase UI (Center), Garage+Tracks+Start (Right)
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Car Showcase (Center)
            RacingSurfaceView(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Controls (Right)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Active Championship Resume Pill
                activeChamp?.let { champ ->
                    Surface(
                        onClick = { viewModel.resumeActiveChampionship() },
                        color = Color(0xEE0B1526),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                        modifier = Modifier
                            .testTag("menu_resume_championship_pill")
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RESUME ${champ.tier.title.uppercase()} • ROUND ${champ.currentRoundIndex + 1}/${champ.totalRounds}",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MotorsportButton(
                        onClick = { viewModel.navigateTo(GameScreen.CHAMPIONSHIP) },
                        text = "CHAMPIONSHIP",
                        accentColor = Color(0xFF00E5FF),
                        modifier = Modifier.testTag("menu_championship_button")
                    )
                    MotorsportButton(
                        onClick = { viewModel.navigateTo(GameScreen.TRACKS) },
                        text = "TRACKS",
                        modifier = Modifier.testTag("menu_tracks_button")
                    )
                    MotorsportButton(
                        onClick = { viewModel.navigateTo(GameScreen.GARAGE) },
                        text = "GARAGE",
                        modifier = Modifier.testTag("menu_garage_button")
                    )
                    MotorsportButton(
                        onClick = { viewModel.startRace(RaceMode.CIRCUIT_RACE) },
                        text = "RACE",
                        accentColor = Color(0xFFFF3823),
                        modifier = Modifier.testTag("menu_start_button")
                    )
                }
            }
        }
        }
    }
}

/**
 * Top menu bar with player profile, currency, and settings.
 */
@Composable
private fun ProfileAndEconomyBar(
    credits: Int,
    playerName: String,
    playerLevel: Int,
    isCompact: Boolean,
    onAddFunds: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile
        Surface(
            color = Color(0xDD0D131E),
            shape = RectangleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2BFFFFFF)),
            modifier = Modifier.clickable(onClick = onOpenProfile)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar_14),
                    contentDescription = "Racer Avatar",
                    modifier = Modifier.size(32.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = playerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "LEVEL $playerLevel", color = Color(0xFF7E8B9B), fontSize = 10.sp)
                }
            }
        }

        // Money + Settings
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                color = Color(0xDD0D131E),
                shape = RectangleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFB300)),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = "$%,d".format(credits), color = Color(0xFFFFB300), modifier = Modifier.padding(12.dp, 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.background(Color(0xDD0D131E), RectangleShape)) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
            }
        }
    }
}

// ... (rest of the file components)

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
        shape = RectangleShape,
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
@Composable
fun MotorsportButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF0066FF)
) {
    Surface(
        onClick = onClick,
        color = Color(0xDD0D131E),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
