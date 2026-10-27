package com.example.racing.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReplayHud(viewModel: GameViewModel) {
    val replayTime by viewModel.replayTimeMs.collectAsState()
    val isPaused by viewModel.isReplayPaused.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val isAutoCamera by viewModel.isAutoCamera.collectAsState()
    val isPhotoMode by viewModel.isPhotoMode.collectAsState()
    val currentCamera by viewModel.cameraView.collectAsState()
    
    val replayMaxTimeMs = 60000L // Default or fetched from VM

    Box(modifier = Modifier.fillMaxSize()) {
        // Photo Mode Overlay (Clean View)
        if (isPhotoMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewModel.togglePhotoMode() }
            ) {
                // Flash animation or capture sound could go here
                Text(
                    "PHOTO MODE - TAP TO RETURN",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
                
                // Camera info
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RectangleShape)
                        .padding(8.dp)
                ) {
                    Text("FREE CAMERA ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("DRAG TO ORBIT | PINCH TO ZOOM", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        } else {
            // Replay Controls
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.closeReplay() },
                        modifier = Modifier.testTag("exit_replay_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit", tint = Color.White)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "REPLAY MODE",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            formatTime(replayTime),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.togglePhotoMode() },
                        modifier = Modifier.testTag("photo_mode_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Photo Mode", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(16.dp)
                ) {
                    // Timeline
                    Slider(
                        value = replayTime.toFloat(),
                        onValueChange = { viewModel.setReplayTime(it.toLong()) },
                        valueRange = 0f..replayMaxTimeMs.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replay_timeline"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF007BFF),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed
                        ReplayControlButton(
                            icon = Icons.Default.Speed,
                            label = "${playbackSpeed}x",
                            onClick = {
                                val next = when(playbackSpeed) {
                                    0.5f -> 1.0f
                                    1.0f -> 2.0f
                                    else -> 0.5f
                                }
                                viewModel.setPlaybackSpeed(next)
                            },
                            tag = "replay_speed_button"
                        )

                        // Play/Pause
                        IconButton(
                            onClick = { viewModel.toggleReplayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("replay_play_pause_button")
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Play" else "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        // Camera Cycle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.cycleReplayCamera(false) }) {
                                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Prev Camera", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    currentCamera.name.replace("_", " "),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.cycleReplayCamera(true) }) {
                                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Camera", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            
                            // Auto Cam Toggle
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isAutoCamera) Color(0xFF007BFF) else Color.DarkGray,
                                        RectangleShape
                                    )
                                    .clickable { viewModel.toggleAutoCamera() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoMode, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AUTO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReplayControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val millis = ms % 1000
    return "%02d:%02d.%03d".format(minutes, seconds, millis)
}
