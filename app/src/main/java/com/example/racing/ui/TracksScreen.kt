package com.example.racing.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.racing.model.TrackId
import com.example.ui.theme.*

/**
 * Redesigned Track Selection Screen with vertical 3:4 cards and premium motorsport aesthetic.
 */
@Composable
fun TracksScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MotorsportMidnight
    ) {
        Column(
            modifier = Modifier
                .padding(safeInsets)
                .padding(24.dp)
        ) {
            // Header with Back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Surface(
                    onClick = onBack,
                    color = MotorsportGlass,
                    shape = ParallelogramShape(10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MotorsportRacingBlue),
                    modifier = Modifier.size(width = 80.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Text(
                    text = "WORLD TRACK REGISTRY",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            // Track Cards Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(220.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(TrackId.values()) { track ->
                    TrackSelectionCard(
                        track = track,
                        isSelected = track == selectedTrack,
                        onClick = { viewModel.selectTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackSelectionCard(
    track: TrackId,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val trackImage = when (track) {
        TrackId.GRAND_PRIX -> R.drawable.track_gp_circuit
        TrackId.COASTAL_SPRINT -> R.drawable.track_coastal_sprint
        TrackId.ALPINE_PASS -> R.drawable.track_alpine_pass
        TrackId.DESERT_RUN -> R.drawable.track_desert_run
        TrackId.FOREST_DRIVE -> R.drawable.track_forest_drive
        TrackId.NEON_METROPOLIS -> R.drawable.track_neon_metropolis
        TrackId.COUNTRYSIDE_RUN -> R.drawable.track_countryside_run
        TrackId.INDUSTRIAL_ROUTE -> R.drawable.track_industrial_route
        TrackId.CANYON_CHALLENGE -> R.drawable.track_canyon_challenge
    }

    Surface(
        color = if (isSelected) MotorsportElectricBlue.copy(alpha = 0.15f) else MotorsportGlass,
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) MotorsportCyan else MotorsportRacingBlue.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f) // 3:4 Portrait Ratio
            .clickable(onClick = onClick)
            .testTag("track_card_${track.name.lowercase()}")
    ) {
        Column {
            // Track Image with top-down fade
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Image(
                    painter = painterResource(id = trackImage),
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Highlight overlay for selected state
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, MotorsportCyan, RectangleShape)
                            .background(MotorsportCyan.copy(alpha = 0.1f))
                    )
                }
            }

            // Track Details
            Column(
                modifier = Modifier
                    .background(MotorsportMidnight.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                Text(
                    text = track.title.uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = track.subtitle,
                    color = ApexTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Technical Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "DISTANCE", color = MotorsportCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${track.lengthKm} KM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text(text = "DIFFICULTY", color = MotorsportCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = track.difficulty.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                    Column {
                        Text(text = "TURNS", color = MotorsportCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${track.turns}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
