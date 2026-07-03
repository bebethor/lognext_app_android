package com.lognext.nexterandroid.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.AuthState
import com.lognext.nexterandroid.features.clock.ClockScreen
import com.lognext.nexterandroid.features.common.NexterTopBar
import com.lognext.nexterandroid.features.home.HomeScreen
import com.lognext.nexterandroid.features.more.MoreScreen
import com.lognext.nexterandroid.features.people.PeopleScreen
import com.lognext.nexterandroid.ui.theme.NexterColors
import kotlinx.coroutines.launch

@Composable
fun NexterApp() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val authState by AppDependencies.authRepository.authState.collectAsState()
    val authenticatedState = authState as? AuthState.Authenticated
    val showAuthenticatedChrome = authenticatedState != null
    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Clock,
        AppDestination.People,
        AppDestination.More
    )

    Scaffold(
        topBar = {
            authenticatedState?.let { state ->
                NexterTopBar(
                    displayName = state.user.displayName,
                    onSignOut = { scope.launch { AppDependencies.authRepository.signOut() } }
                )
            }
        },
        bottomBar = {
            if (!showAuthenticatedChrome) return@Scaffold

            val backStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = backStackEntry?.destination?.route

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexterColors.cardBackground())
            ) {
                Divider(color = NexterColors.border(), thickness = 1.dp)
                BottomNavigation(
                    backgroundColor = NexterColors.cardBackground(),
                    contentColor = NexterColors.Red,
                    elevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                ) {
                    destinations.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val itemColor = if (selected) {
                            NexterColors.Red
                        } else {
                            NexterColors.tertiaryText()
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
                            unselectedContentColor = NexterColors.tertiaryText(),
                            label = { Text(destination.label, fontSize = 8.sp) },
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
