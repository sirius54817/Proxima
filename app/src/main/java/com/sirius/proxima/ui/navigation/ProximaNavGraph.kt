package com.sirius.proxima.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirius.proxima.di.ServiceLocator
import com.sirius.proxima.ui.screen.HomeScreen
import com.sirius.proxima.ui.screen.SettingsScreen
import com.sirius.proxima.ui.screen.SisScreen
import com.sirius.proxima.ui.screen.SubjectHistoryScreen
import com.sirius.proxima.ui.screen.TimetableScreen
import com.sirius.proxima.ui.theme.Border

@Composable
fun ProximaNavGraph() {
    val context = LocalContext.current
    val settingsDataStore = ServiceLocator.getSettingsDataStore(context.applicationContext)
    val sisUnlocked by settingsDataStore.sisFeaturesUnlocked.collectAsStateWithLifecycle(initialValue = false)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val visibleBottomNavItems = if (sisUnlocked) bottomNavItems else bottomNavItems.filterNot { it == Screen.Sis }
    val showBottomBar = visibleBottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                visibleBottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSubjectHistory = { subjectId ->
                        navController.navigate(Screen.SubjectHistory.createRoute(subjectId))
                    }
                )
            }
            composable(Screen.Timetable.route) {
                TimetableScreen()
            }
            composable(Screen.Sis.route) {
                if (sisUnlocked) {
                    SisScreen()
                } else {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.SubjectHistory.route,
                arguments = listOf(navArgument("subjectId") { type = NavType.IntType })
            ) { backStackEntry ->
                val subjectId = backStackEntry.arguments?.getInt("subjectId") ?: return@composable
                SubjectHistoryScreen(
                    subjectId = subjectId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

