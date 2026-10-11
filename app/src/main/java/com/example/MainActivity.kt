package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.racing.ui.GameScreen
import com.example.racing.ui.GameViewModel
import com.example.racing.ui.GarageScreen
import com.example.racing.ui.MainMenuScreen
import com.example.racing.ui.RaceSummaryScreen
import com.example.racing.ui.RacingHud
import com.example.racing.ui.RacingSurfaceView
import com.example.racing.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF060910)
                ) {
                    ApexRacingApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }
}

@Composable
fun ApexRacingApp(
    viewModel: GameViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle Android system back button
    BackHandler(enabled = currentScreen != GameScreen.MAIN_MENU) {
        when (currentScreen) {
            GameScreen.RACE -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.GARAGE -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.SETTINGS -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.RACE_SUMMARY -> viewModel.navigateTo(GameScreen.MAIN_MENU)
            GameScreen.MAIN_MENU -> { /* Exit handled by OS */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenGL ES 3.0 3D Rendering Canvas (Always present underneath UI)
        RacingSurfaceView(viewModel = viewModel)

        // 2. Composable Screen Overlays
        when (currentScreen) {
            GameScreen.MAIN_MENU -> {
                MainMenuScreen(viewModel = viewModel)
            }
            GameScreen.RACE -> {
                RacingHud(viewModel = viewModel)
            }
            GameScreen.GARAGE -> {
                GarageScreen(viewModel = viewModel)
            }
            GameScreen.SETTINGS -> {
                SettingsScreen(viewModel = viewModel)
            }
            GameScreen.RACE_SUMMARY -> {
                RaceSummaryScreen(viewModel = viewModel)
            }
        }
    }
}
