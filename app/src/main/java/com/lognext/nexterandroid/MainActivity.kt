package com.lognext.nexterandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.lognext.nexterandroid.app.NexterApp
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.ui.theme.NexterTheme

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AppDependencies.absenceNotifications.open(intent.getStringExtra("absence_subscription"))
    }

    override fun onResume() {
        super.onResume()
        AppDependencies.absenceNotifications.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        AppDependencies.initialize(applicationContext)
        AppDependencies.absenceNotifications.open(intent?.getStringExtra("absence_subscription"))
        setContent {
            NexterTheme {
                NexterApp()
            }
        }
    }
}
