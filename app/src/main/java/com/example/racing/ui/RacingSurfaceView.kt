package com.example.racing.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RacingSurfaceView(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val glView = remember {
        object : GLSurfaceView(context) {
            private var lastTouchX = 0f
            private var lastTouchY = 0f

            init {
                setEGLContextClientVersion(3)
                viewModel.renderer?.let { r ->
                    setRenderer(r)
                    renderMode = RENDERMODE_CONTINUOUSLY
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (viewModel.currentScreen.value == GameScreen.GARAGE) {
                    val r = viewModel.renderer ?: return super.onTouchEvent(event)
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            lastTouchX = event.x
                            lastTouchY = event.y
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - lastTouchX
                            val dy = event.y - lastTouchY
                            r.showroomOrbitAngleDeg += dx * 0.45f
                            r.showroomElevationDeg = (r.showroomElevationDeg - dy * 0.25f).coerceIn(5f, 50f)
                            lastTouchX = event.x
                            lastTouchY = event.y
                        }
                    }
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
    }

    // High frequency fixed-timestep physics update thread
    DisposableEffect(Unit) {
        var isLooping = true
        var lastTimeNanos = SystemClock.elapsedRealtimeNanos()
        var accumulatorSec = 0f
        val fixedDtSec = 0.0125f // 80Hz stable physics timestep

        val thread = Thread({
            while (isLooping) {
                val nowNanos = SystemClock.elapsedRealtimeNanos()
                val frameTimeSec = ((nowNanos - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                lastTimeNanos = nowNanos

                accumulatorSec += frameTimeSec

                // Fixed sub-stepping accumulator: prevents spiral of death
                var steps = 0
                while (accumulatorSec >= fixedDtSec && steps < 4) {
                    viewModel.updateGamePhysics(fixedDtSec)
                    accumulatorSec -= fixedDtSec
                    steps++
                }

                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }, "ApexPhysicsThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        onDispose {
            isLooping = false
            thread.interrupt()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier.fillMaxSize()
    )
}
