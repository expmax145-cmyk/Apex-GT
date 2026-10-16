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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun RacingSurfaceView(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        object : GLSurfaceView(context) {
            private var lastTouchX = 0f
            private var lastTouchY = 0f
            private var prevPinchDist = -1f
            private var lastTapTime = 0L

            init {
                setEGLContextClientVersion(3)
                preserveEGLContextOnPause = true
                viewModel.renderer?.let { r ->
                    setRenderer(r)
                    renderMode = RENDERMODE_CONTINUOUSLY
                }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                val screen = viewModel.currentScreen.value
                if (screen == GameScreen.GARAGE || screen == GameScreen.MAIN_MENU) {
                    val r = viewModel.renderer ?: return super.onTouchEvent(event)
                    
                    if (event.pointerCount >= 2) {
                        // Pinch to Zoom
                        val x0 = event.getX(0)
                        val y0 = event.getY(0)
                        val x1 = event.getX(1)
                        val y1 = event.getY(1)
                        val dist = kotlin.math.hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()).toFloat()

                        if (prevPinchDist > 0f) {
                            val pinchDelta = dist - prevPinchDist
                            r.showroomZoomDistance = (r.showroomZoomDistance - pinchDelta * 0.009f).coerceIn(3.2f, 8.5f)
                        }
                        prevPinchDist = dist
                        lastTouchX = (x0 + x1) * 0.5f
                        lastTouchY = (y0 + y1) * 0.5f
                        return true
                    } else {
                        prevPinchDist = -1f
                    }

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTapTime < 320L) {
                                // Double-tap detected: smooth reset to hero 3/4 view
                                r.resetShowroomCamera()
                            }
                            lastTapTime = now
                            lastTouchX = event.x
                            lastTouchY = event.y
                            if (screen == GameScreen.MAIN_MENU) {
                                r.autoRotateShowroom = false
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - lastTouchX
                            val dy = event.y - lastTouchY
                            r.showroomOrbitAngleDeg += dx * 0.38f
                            r.showroomElevationDeg = (r.showroomElevationDeg - dy * 0.22f).coerceIn(5f, 52f)
                            lastTouchX = event.x
                            lastTouchY = event.y
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (screen == GameScreen.MAIN_MENU) {
                                r.autoRotateShowroom = true
                            }
                        }
                    }
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
    }

    // Lifecycle observer for GLSurfaceView pause/resume and power management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    glView.onResume()
                    viewModel.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    glView.onPause()
                    viewModel.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                if (!viewModel.isGamePaused) {
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
                } else {
                    lastTimeNanos = SystemClock.elapsedRealtimeNanos()
                    accumulatorSec = 0f
                }

                try {
                    Thread.sleep(if (viewModel.isGamePaused) 100 else 10)
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
