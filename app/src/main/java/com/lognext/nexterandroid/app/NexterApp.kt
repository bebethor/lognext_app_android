package com.lognext.nexterandroid.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lognext.nexterandroid.R
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

            Column {
                Divider(color = NexterColors.border(), thickness = 1.dp)
                BottomNavigation(
                    backgroundColor = NexterColors.cardBackground(),
                    contentColor = NexterColors.Red,
                    elevation = 8.dp
                ) {
                    destinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val itemColor = if (selected) {
                            NexterColors.Red
                        } else {
                            NexterColors.secondaryText()
                        }
                        BottomNavigationItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    popUpTo(AppDestination.Home.route)
                                }
                            },
                            selectedContentColor = NexterColors.Red,
                            unselectedContentColor = NexterColors.secondaryText(),
                            label = { Text(destination.label) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = destination.iconRes()),
                                    contentDescription = destination.label,
                                    tint = itemColor
                                )
                            }
                        )
                    }
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

private fun AppDestination.iconRes(): Int {
    return when (this) {
        AppDestination.Home -> R.drawable.ic_tab_tasks
        AppDestination.Clock -> R.drawable.ic_tab_clock_check
        AppDestination.People -> R.drawable.ic_tab_people
        AppDestination.More -> R.drawable.ic_tab_more_circle
    }
}
