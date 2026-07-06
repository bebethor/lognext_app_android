@file:Suppress("DEPRECATION")

package com.lognext.nexterandroid.app

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
import com.lognext.nexterandroid.ui.theme.isNexterDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NexterApp() {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val isDark = isNexterDarkTheme()
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val authState by AppDependencies.authRepository.authState.collectAsState()
    val authenticatedState = authState as? AuthState.Authenticated
    val showAuthenticatedChrome = authenticatedState != null
    var showStartupSplash by remember { mutableStateOf(true) }
    val destinations = listOf(
        AppDestination.Home,
        AppDestination.Clock,
        AppDestination.People,
        AppDestination.More
    )

    LaunchedEffect(Unit) {
        delay(900)
        showStartupSplash = false
    }

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

    if (showStartupSplash) {
        StartupSplash(isDark = isDark)
        return
    }

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
                        .height(64.dp)
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
                            label = { Text(label, fontSize = 10.sp) },
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
            composable(AppDestination.Clock.route) { ClockScreen() }
            composable(AppDestination.People.route) { PeopleScreen() }
            composable(AppDestination.More.route) { MoreScreen() }
        }
    }
}

@Composable
private fun StartupSplash(isDark: Boolean) {
    val logoRes = if (isDark) R.drawable.lognext_logo_negative else R.drawable.lognext_logo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) NexterColors.DarkPageBackground else Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Lognext",
                modifier = Modifier
                    .width(180.dp)
                    .height(42.dp)
            )
            Spacer(modifier = Modifier.height(26.dp))
            CircularProgressIndicator(
                color = NexterColors.Red,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.splash_loading),
                color = NexterColors.secondaryText(),
                fontSize = 14.sp
            )
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
