package com.lognext.nexterandroid.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.AuthState
import com.lognext.nexterandroid.features.clock.ClockScreen
import com.lognext.nexterandroid.features.home.HomeScreen
import com.lognext.nexterandroid.features.more.MoreScreen
import com.lognext.nexterandroid.features.people.PeopleScreen
import com.lognext.nexterandroid.ui.theme.NexterColors

@Composable
fun NexterApp() {
    val navController = rememberNavController()
    val authState by AppDependencies.authRepository.authState.collectAsState()
    val showBottomBar = authState is AuthState.Authenticated
    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Clock,
        AppDestination.People,
        AppDestination.More
    )

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold

            val backStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = backStackEntry?.destination?.route

            BottomNavigation(
                backgroundColor = Color.White,
                contentColor = NexterColors.Red,
                elevation = 0.dp,
                modifier = Modifier.border(BorderStroke(1.dp, NexterColors.Navy.copy(alpha = 0.08f)))
            ) {
                destinations.forEach { destination ->
                    BottomNavigationItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                popUpTo(AppDestination.Home.route)
                            }
                        },
                        selectedContentColor = NexterColors.Red,
                        unselectedContentColor = NexterColors.Navy.copy(alpha = 0.35f),
                        label = { Text(destination.label, fontSize = 9.sp) },
                        icon = { Text(destination.label.first().toString(), fontSize = 18.sp) }
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
