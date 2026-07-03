package com.lognext.nexterandroid.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lognext.nexterandroid.features.clock.ClockScreen
import com.lognext.nexterandroid.features.home.HomeScreen
import com.lognext.nexterandroid.features.more.MoreScreen
import com.lognext.nexterandroid.features.people.PeopleScreen

@Composable
fun NexterApp() {
    val navController = rememberNavController()
    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Clock,
        AppDestination.People,
        AppDestination.More
    )

    Scaffold(
        bottomBar = {
            val backStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = backStackEntry?.destination?.route

            BottomNavigation {
                destinations.forEach { destination ->
                    BottomNavigationItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                popUpTo(AppDestination.Home.route)
                            }
                        },
                        label = { Text(destination.label) },
                        icon = { Text(destination.label.first().toString()) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route) { HomeScreen() }
            composable(AppDestination.Clock.route) { ClockScreen() }
            composable(AppDestination.People.route) { PeopleScreen() }
            composable(AppDestination.More.route) { MoreScreen() }
        }
    }
}
