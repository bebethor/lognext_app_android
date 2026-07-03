package com.lognext.nexterandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lognext.nexterandroid.app.NexterApp
import com.lognext.nexterandroid.ui.theme.NexterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexterTheme {
                NexterApp()
            }
        }
    }
}
