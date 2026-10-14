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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.racing.model.RaceMode
import com.example.racing.model.TrackId

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val credits by viewModel.playerCredits.collectAsState()
    val selectedCar by viewModel.selectedCar.collectAsState()
    val selectedTrack by viewModel.selectedTrack.collectAsState()

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xCC080C14), Color(0xF205080E))
                )
            )
    ) {
        val isCompact = maxWidth < 700.dp
        val isShort = maxHeight < 390.dp
        val hPad = if (isCompact) 12.dp else 22.dp
        val vPad = if (isShort) 8.dp else 14.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = hPad, vertical = vPad)
        ) {
            // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFFF3D00), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "APEX GT",
                        color = Color.White,
                        fontSize = if (isCompact) 22.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0x33FF3D00),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x88FF3D00))
                    ) {
                        Text(
                            text = "3D RACING",
                            color = Color(0xFFFF6E40),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "PRECISION AUTOMOTIVE SIMULATION",
                    color = Color(0xFF78909C),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Top Right: Credits & Settings Button + Dev Cash Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF131A26),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFD700)),
                    modifier = Modifier.clickable { viewModel.grantDevelopmentFunds() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CR",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "%,d".format(credits),
                            color = Color.White,
                            fontSize = if (isCompact) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Development Cash Injector Button
                Button(
                    onClick = { viewModel.grantDevelopmentFunds() },
                    modifier = Modifier
                        .testTag("dev_add_funds_main_button")
                        .height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(17.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+$500K",
                        color = Color(0xFF060910),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.SETTINGS) },
                    modifier = Modifier
                        .testTag("settings_button")
                        .size(44.dp)
                        .background(Color(0xFF131A26), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Center Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (isShort) 48.dp else 62.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 18.dp)
        ) {
            // Left Column: Track Selector & Mode Cards
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Track Banner Quick Selector
                Surface(
                    color = Color(0xFF121826),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4400E5FF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val all = TrackId.values()
                            val next = all[(selectedTrack.ordinal + 1) % all.size]
                            viewModel.selectTrack(next)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (selectedTrack) {
                                TrackId.GRAND_PRIX -> Icons.Default.Flag
                                TrackId.ALPINE_PASS -> Icons.Default.Landscape
                                TrackId.NEON_METROPOLIS -> Icons.Default.LocationCity
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = selectedTrack.title,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ACTIVE TRACK",
                                    color = Color(0xFF78909C),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedTrack.title} (${selectedTrack.lengthKm} km • ${selectedTrack.turns} Turns)",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Text(
                            text = "CHANGE ➔",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Environment Quick Selector (Time of Day & Weather)
                val timeOfDay by viewModel.timeOfDay.collectAsState()
                val weatherCondition by viewModel.weatherCondition.collectAsState()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time of Day Pill
                    Surface(
                        color = Color(0xFF101624),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FF9100)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.cycleTimeOfDay() }
                            .testTag("main_menu_time_of_day_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "TIME OF DAY",
                                    color = Color(0xFF78909C),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = timeOfDay.displayName,
                                    color = Color(0xFFFF9100),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(text = "↻", color = Color(0xFFFF9100), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Weather Pill
                    Surface(
                        color = Color(0xFF101624),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E5FF)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.cycleWeather() }
                            .testTag("main_menu_weather_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "WEATHER",
                                    color = Color(0xFF78909C),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = weatherCondition.displayName,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(text = "↻", color = Color(0xFF00E5FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Event Mode Cards
                ModeCard(
                    title = "CIRCUIT CUP",
                    subtitle = "3 Laps against 3 AI Competitors • Grid Start",
                    icon = Icons.Default.Flag,
                    accentColor = Color(0xFFFF3D00),
                    onClick = { viewModel.startRace(RaceMode.CIRCUIT_RACE) },
                    testTag = "circuit_race_card"
                )

                ModeCard(
                    title = "TIME ATTACK",
                    subtitle = "Hunt Personal Lap Records • Sector Split Deltas",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF00E5FF),
                    onClick = { viewModel.startRace(RaceMode.TIME_ATTACK) },
                    testTag = "time_attack_card"
                )

                ModeCard(
                    title = "FREE DRIVE",
                    subtitle = "Open Circuit • Telemetry & Drift Practice",
                    icon = Icons.Default.DirectionsCar,
                    accentColor = Color(0xFF00E676),
                    onClick = { viewModel.startRace(RaceMode.FREE_DRIVE) },
                    testTag = "free_drive_card"
                )
            }

            // Right Column: Current Car & Garage Shortcut
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = Color(0xCC0E131F),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT VEHICLE",
                                color = Color(0xFF78909C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedCar.category,
                                color = Color(0xFFFF9100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = selectedCar.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = selectedCar.description,
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        StatRow(name = "TOP SPEED", value = selectedCar.topSpeedKmh / 400f, display = "${selectedCar.topSpeedKmh.toInt()} km/h")
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(name = "ACCEL", value = selectedCar.accelerationRating / 10f, display = "${selectedCar.accelerationRating}/10")
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(name = "HANDLING", value = selectedCar.handlingRating / 10f, display = "${selectedCar.handlingRating}/10")
                    }
                }

                Button(
                    onClick = { viewModel.openGarage() },
                    modifier = Modifier
                        .testTag("garage_button")
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Garage",
                        tint = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "GARAGE & TUNING WORKSHOP",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = Color(0xCC0F1420),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier
            .testTag(testTag)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF90A4AE),
                    fontSize = 10.sp
                )
            }

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = accentColor
            )
        }
    }
}

@Composable
fun StatRow(name: String, value: Float, display: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            color = Color(0xFF78909C),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp)
        )
        androidx.compose.material3.LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .padding(horizontal = 8.dp),
            color = Color(0xFFFF3D00),
            trackColor = Color(0xFF1B2332)
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
