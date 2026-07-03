package com.lognext.nexterandroid.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexterColors = lightColors(
    primary = Color(0xFF146C94),
    primaryVariant = Color(0xFF0B4D68),
    secondary = Color(0xFF2D9C7F),
    background = Color(0xFFF7F9FB),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF172026),
    onSurface = Color(0xFF172026)
)

@Composable
fun NexterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = NexterColors,
        content = content
    )
}
