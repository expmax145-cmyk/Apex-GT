package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RacingDarkColorScheme = darkColorScheme(
    primary = ApexRed,
    secondary = ApexCyan,
    tertiary = ApexGold,
    background = ApexDarkBg,
    surface = ApexSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = ApexTextPrimary,
    onSurface = ApexTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
