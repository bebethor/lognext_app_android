package com.lognext.nexterandroid.app

sealed class AppDestination(val route: String, val label: String) {
    object Home : AppDestination("home", "Tareas")
    object Clock : AppDestination("clock", "Jornada")
    object People : AppDestination("people", "People")
    object More : AppDestination("more", "Más")
}
