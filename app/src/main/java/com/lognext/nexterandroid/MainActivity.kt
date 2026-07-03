package com.lognext.nexterandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lognext.nexterandroid.app.NexterApp
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTheme
import com.lognext.nexterandroid.ui.theme.isNexterDarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppDependencies.initialize(applicationContext)
        setContent {
            val isDark = isNexterDarkTheme()
            val view = LocalView.current
            val topBarColor = if (isDark) NexterColors.DarkCardBackground else NexterColors.PageBackground
            val bottomBarColor = if (isDark) NexterColors.DarkCardBackground else NexterColors.White
            SideEffect {
                window.statusBarColor = topBarColor.toArgb()
                window.navigationBarColor = bottomBarColor.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController?.isAppearanceLightStatusBars = !isDark
                insetsController?.isAppearanceLightNavigationBars = !isDark
            }
            NexterTheme {
                NexterApp()
            }
        }
    }
}
