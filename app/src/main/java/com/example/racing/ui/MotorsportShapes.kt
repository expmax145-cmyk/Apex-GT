package com.example.racing.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Custom Parallelogram shape for motorsport-themed UI buttons and panels.
 */
class ParallelogramShape(private val angleDeg: Float = 15f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val offset = size.height * Math.tan(angleRad.toDouble()).toFloat()
        
        val path = Path().apply {
            if (layoutDirection == LayoutDirection.Ltr) {
                moveTo(offset, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - offset, size.height)
                lineTo(0f, size.height)
                close()
            } else {
                moveTo(0f, 0f)
                lineTo(size.width - offset, 0f)
                lineTo(size.width, size.height)
                lineTo(offset, size.height)
                close()
            }
        }
        return Outline.Generic(path)
    }
}

/**
 * Sharp-edged rectangle with a diagonal cut corner.
 */
class AngledCornerShape(private val cutSize: Float = 12f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cut = cutSize * density.density
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, cut)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
