package com.lognext.nexterandroid.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object NexterColors {
    val Navy = Color(0xFF000029)
    val Red = Color(0xFFFA3C0F)
    val PageBackground = Color(0xFFF0F2F5)
    val DarkPageBackground = Color(0xFF090A16)
    val DarkCardBackground = Color(0xFF1A1B26)
    val White = Color.White
    val Navy08 = Color(0x14000029)
    val Yellow = Color(0xFFFFFA96)
    val Green = Color(0xFF64F07D)
    val Blue = Color(0xFF3791F5)
    val Aqua = Color(0xFF3CE6E6)
    val Violet = Color(0xFFC896FF)

    @Composable
    fun pageBackground(): Color = if (isNexterDarkTheme()) DarkPageBackground else PageBackground

    @Composable
    fun cardBackground(): Color = if (isNexterDarkTheme()) DarkCardBackground else White

    @Composable
    fun primaryText(): Color = if (isNexterDarkTheme()) White else Navy

    @Composable
    fun secondaryText(): Color = if (isNexterDarkTheme()) White.copy(alpha = 0.68f) else Navy.copy(alpha = 0.55f)

    @Composable
    fun tertiaryText(): Color = if (isNexterDarkTheme()) White.copy(alpha = 0.42f) else Navy.copy(alpha = 0.35f)

    @Composable
    fun border(): Color = if (isNexterDarkTheme()) White.copy(alpha = 0.10f) else Navy.copy(alpha = 0.08f)
}

object NexterTypography {
    val TopBarDate = 22.sp
    val Avatar = 18.sp
    val IconButton = 19.sp

    val ScreenTitle = 26.sp
    val ScreenSubtitle = 17.sp
    val CardTitle = 22.sp
    val SectionTitle = 20.sp

    val Body = 18.sp
    val Callout = 17.sp
    val Footnote = 15.sp
    val Caption = 14.sp

    val Button = 18.sp
    val SmallButton = 17.sp
    val Badge = 14.sp
    val Metric = 32.sp
    val Clock = 44.sp
}

private val NexterLightColorScheme = lightColors(
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

private val NexterDarkColorScheme = darkColors(
    primary = NexterColors.Red,
    primaryVariant = NexterColors.Navy,
    secondary = NexterColors.Red,
    background = NexterColors.DarkPageBackground,
    surface = NexterColors.DarkCardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun NexterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isNexterDarkTheme()) NexterDarkColorScheme else NexterLightColorScheme,
        content = content
    )
}

@Composable
fun isNexterDarkTheme(): Boolean {
    val configuration = LocalConfiguration.current
    val uiModeNight = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiModeNight == Configuration.UI_MODE_NIGHT_YES || isSystemInDarkTheme()
}
