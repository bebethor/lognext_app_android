package com.lognext.nexterandroid.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lognext.nexterandroid.R

val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold),
    Font(R.font.space_grotesk, FontWeight.ExtraBold)
)

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
    val TopBarDate = 18.sp
    val Avatar = 15.sp
    val IconButton = 17.sp

    val ScreenTitle = 22.sp
    val ScreenSubtitle = 14.sp
    val CardTitle = 18.sp
    val SectionTitle = 16.sp

    val Body = 15.sp
    val Callout = 14.sp
    val Footnote = 12.sp
    val Caption = 11.sp

    val Button = 15.sp
    val SmallButton = 14.sp
    val Badge = 11.sp
    val Metric = 26.sp
    val Clock = 36.sp
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

private val NexterMaterialTypography = Typography(
    defaultFontFamily = SpaceGrotesk
)

@Composable
fun NexterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = if (isNexterDarkTheme()) NexterDarkColorScheme else NexterLightColorScheme,
        typography = NexterMaterialTypography,
        content = content
    )
}

@Composable
fun isNexterDarkTheme(): Boolean {
    val configuration = LocalConfiguration.current
    val uiModeNight = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return uiModeNight == Configuration.UI_MODE_NIGHT_YES || isSystemInDarkTheme()
}
