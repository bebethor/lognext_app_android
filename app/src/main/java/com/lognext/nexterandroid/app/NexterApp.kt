@file:Suppress("DEPRECATION")

package com.lognext.nexterandroid.app

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lognext.nexterandroid.features.common.EmployeeCategory
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lognext.nexterandroid.R
import com.lognext.nexterandroid.core.AppDependencies
import com.lognext.nexterandroid.core.auth.AuthState
// import com.lognext.nexterandroid.features.clock.ClockScreen
import com.lognext.nexterandroid.features.common.NexterTopBar
import com.lognext.nexterandroid.features.home.HomeScreen
import com.lognext.nexterandroid.features.more.MoreScreen
import com.lognext.nexterandroid.features.people.PeopleScreen
import com.lognext.nexterandroid.ui.theme.NexterColors
import com.lognext.nexterandroid.ui.theme.NexterTypography
import com.lognext.nexterandroid.ui.theme.isNexterDarkTheme
import kotlinx.coroutines.launch

@Composable
fun NexterApp() {
    val scope = rememberCoroutineScope()
    val isDark = isNexterDarkTheme()
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val authState by AppDependencies.authRepository.authState.collectAsState()
    val authenticatedState = authState as? AuthState.Authenticated
    var employeeCategory by remember(authenticatedState) { mutableStateOf<EmployeeCategory?>(null) }
    LaunchedEffect(authenticatedState) {
        if (authenticatedState != null && !com.lognext.nexterandroid.core.AppConfig.UseFakeLogin) {
            try {
                employeeCategory = EmployeeCategory.fromJobTitle(AppDependencies.peopleService.me().jobTitle)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                employeeCategory = null
            }
        }
    }
    LaunchedEffect(AppDependencies.authRepository) {
        AppDependencies.authRepository.restoreSession()
    }
    val destinations = listOf(
        AppDestination.Home,
        // Jornada oculta temporalmente: descomentar esta entrada y su ruta para recuperarla.
        // AppDestination.Clock,
        AppDestination.People,
        AppDestination.More
    )

    SideEffect {
        val window = activity?.window ?: return@SideEffect
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }

    // Remove the authenticated navigation graph on logout, regardless of the selected tab.
    // Its destinations and ViewModels must not survive into the next session.
    if (authenticatedState == null) {
        HomeScreen()
        return
    }
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            authenticatedState.let { state ->
                NexterTopBar(
                    employeeCategory = employeeCategory,
                    displayName = state.user.displayName,
                    onSignOut = { scope.launch { AppDependencies.authRepository.signOut() } }
                )
            }
        },
        bottomBar = {
            val backStackEntry = navController.currentBackStackEntryAsState().value
            val currentRoute = backStackEntry?.destination?.route

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(
                        start = 26.dp,
                        end = 26.dp,
                        top = 8.dp,
                        bottom = navigationBarHeight + 10.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BottomNavigation(
                    backgroundColor = NexterColors.cardBackground(),
                    contentColor = NexterColors.Red,
                    elevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .border(BorderStroke(1.dp, NexterColors.border()), RoundedCornerShape(100.dp))
                ) {
                    destinations.forEach { destination ->
                        val label = stringResource(destination.labelRes)
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
                            label = { Text(label, fontSize = NexterTypography.Caption) },
                            icon = {
                                Icon(
                                    painter = painterResource(id = destination.iconRes()),
                                    contentDescription = label,
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
            // composable(AppDestination.Clock.route) { ClockScreen() }
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
