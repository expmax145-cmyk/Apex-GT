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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.ControlType

@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val controlType by viewModel.controlType.collectAsState()
    val graphicsPreset by viewModel.graphicsPreset.collectAsState()

    var volumeVal by remember { mutableFloatStateOf(viewModel.dataManager.soundVolume) }
    var steerSens by remember { mutableFloatStateOf(viewModel.dataManager.steerSensitivity) }
    var isMuted by remember { androidx.compose.runtime.mutableStateOf(viewModel.dataManager.soundMuted) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .padding(20.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                    modifier = Modifier
                        .testTag("settings_back_button")
                        .size(44.dp)
                        .background(Color(0xFF131A26), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "SETTINGS & PERFORMANCE PROFILES",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Two-column layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Column 1: Graphics & Controls
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Graphics Preset
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "GRAPHICS QUALITY PRESET",
                                color = Color(0xFF78909C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("LOW", "MEDIUM", "HIGH").forEach { preset ->
                                    val isSelected = graphicsPreset == preset
                                    Surface(
                                        color = if (isSelected) Color(0xFF1E2638) else Color(0xFF0A0E17),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF00E5FF) else Color(0x22FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setGraphicsPreset(preset) }
                                    ) {
                                        Text(
                                            text = preset,
                                            color = if (isSelected) Color.White else Color(0xFF78909C),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Steering Control Scheme
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "STEERING CONTROL SCHEME",
                                color = Color(0xFF78909C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    ControlType.TOUCH_BUTTONS to "BUTTONS",
                                    ControlType.VIRTUAL_WHEEL to "WHEEL",
                                    ControlType.TILT_GYRO to "GYRO TILT"
                                ).forEach { (type, label) ->
                                    val isSelected = controlType == type
                                    Surface(
                                        color = if (isSelected) Color(0xFF1E2638) else Color(0xFF0A0E17),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF3D00) else Color(0x22FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.setControlType(type) }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) Color.White else Color(0xFF78909C),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Steering Sensitivity
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "STEERING SENSITIVITY",
                                    color = Color(0xFF78909C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "%.1fx".format(steerSens),
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = steerSens,
                                onValueChange = {
                                    steerSens = it
                                    viewModel.dataManager.steerSensitivity = it
                                },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00E5FF),
                                    activeTrackColor = Color(0xFF00E5FF),
                                    inactiveTrackColor = Color(0xFF1B2332)
                                )
                            )
                        }
                    }
                }

                // Column 2: Audio & Information
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Audio Volume
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "AUDIO SYNTHESIZER VOLUME",
                                    color = Color(0xFF78909C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Switch(
                                    checked = !isMuted,
                                    onCheckedChange = {
                                        isMuted = !it
                                        viewModel.toggleMute(isMuted)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E5FF),
                                        checkedTrackColor = Color(0xFF1E2638)
                                    )
                                )
                            }
                            Slider(
                                value = volumeVal,
                                onValueChange = {
                                    volumeVal = it
                                    viewModel.setSoundVolume(it)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF3D00),
                                    activeTrackColor = Color(0xFFFF3D00),
                                    inactiveTrackColor = Color(0xFF1B2332)
                                )
                            )
                        }
                    }

                    // Tech Spec Card
                    Surface(
                        color = Color(0xFF101522),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ENGINE ARCHITECTURE",
                                color = Color(0xFF78909C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Native OpenGL ES 3.0 Real-Time Renderer\n• Pacejka Magic Formula Vehicle Dynamics\n• 4-Wheel Independent Spring/Damper Suspension\n• Multi-Harmonic Procedural Audio Engine",
                                color = Color(0xFFB0BEC5),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
