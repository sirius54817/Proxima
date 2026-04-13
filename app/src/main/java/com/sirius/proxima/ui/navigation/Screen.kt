package com.sirius.proxima.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Timetable : Screen("timetable", "Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Sis : Screen("sis", "SIS", Icons.Filled.School, Icons.Outlined.School)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object SubjectHistory : Screen("subject_history/{subjectId}", "Subject History", Icons.Filled.Home, Icons.Outlined.Home) {
        fun createRoute(subjectId: Int): String = "subject_history/$subjectId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Timetable, Screen.Sis, Screen.Settings)


