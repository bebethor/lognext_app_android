package com.lognext.nexterandroid.app

import androidx.annotation.StringRes
import com.lognext.nexterandroid.R

sealed class AppDestination(val route: String, @StringRes val labelRes: Int) {
    object Home : AppDestination("home", R.string.nav_tasks)
    object Clock : AppDestination("clock", R.string.nav_clock)
    object People : AppDestination("people", R.string.nav_people)
    object More : AppDestination("more", R.string.nav_more)
}
