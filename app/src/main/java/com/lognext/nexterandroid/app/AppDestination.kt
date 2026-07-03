package com.lognext.nexterandroid.app

sealed class AppDestination(val route: String, val label: String) {
    object Home : AppDestination("home", "Inicio")
    object Clock : AppDestination("clock", "Jornada")
    object People : AppDestination("people", "Personas")
    object More : AppDestination("more", "Mas")
}
