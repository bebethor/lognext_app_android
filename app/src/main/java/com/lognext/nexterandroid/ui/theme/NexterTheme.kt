package com.lognext.nexterandroid.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NexterColors {
    val Navy = Color(0xFF000029)
    val Red = Color(0xFFFA3C0F)
    val PageBackground = Color(0xFFF0F2F5)
    val White = Color.White
    val Navy08 = Color(0x14000029)
    val Yellow = Color(0xFFFFFA96)
    val Green = Color(0xFF64F07D)
    val Blue = Color(0xFF3791F5)
    val Aqua = Color(0xFF3CE6E6)
    val Violet = Color(0xFFC896FF)
}

private val NexterColorScheme = lightColors(
    primary = NexterColors.Navy,
    primaryVariant = Color(0xFF040423),
    secondary = NexterColors.Red,
    background = NexterColors.PageBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = NexterColors.Navy,
    onSurface = NexterColors.Navy
)

@Composable
fun NexterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = NexterColorScheme,
        content = content
    )
}
