package com.example.racing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.racing.model.TrackId

@Composable
fun TracksScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val selectedTrack by viewModel.selectedTrack.collectAsState()
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF05080E)
    ) {
        Column(
            modifier = Modifier
                .padding(safeInsets)
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "SELECT TRACK",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(TrackId.values()) { track ->
                    TrackCard(
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
private fun TrackCard(
    track: TrackId,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0x33FF3823) else Color(0xDD0D131E),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFF3823) else Color(0x2BFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (track) {
                TrackId.GRAND_PRIX -> Icons.Default.Flag
                TrackId.ALPINE_PASS -> Icons.Default.Landscape
                TrackId.NEON_METROPOLIS -> Icons.Default.LocationCity
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFFF3823) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${track.lengthKm} KM • ${track.difficulty} • ${track.turns} TURNS",
                    color = Color(0xFF7E8B9B),
                    fontSize = 12.sp
                )
            }
        }
    }
}
