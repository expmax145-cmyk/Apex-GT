package com.example.racing.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
    val trackImage = when (track) {
        TrackId.GRAND_PRIX -> R.drawable.ic_track_gp
        TrackId.ALPINE_PASS -> R.drawable.ic_track_alpine
        TrackId.NEON_METROPOLIS -> R.drawable.ic_track_neon
    }

    Surface(
        color = if (isSelected) Color(0x330066FF) else Color(0xDD0D131E),
        shape = RectangleShape,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF0066FF) else Color(0x2BFFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
    ) {
        Column {
            Image(
                painter = painterResource(id = trackImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${track.lengthKm} KM • ${track.difficulty} • ${track.turns} TURNS",
                    color = Color(0xFF7E8B9B),
                    fontSize = 10.sp
                )
            }
        }
    }
}
