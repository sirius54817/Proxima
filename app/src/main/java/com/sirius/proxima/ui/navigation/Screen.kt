package com.sirius.proxima.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Timetable : Screen("timetable", "Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object Study : Screen("study", "Study", Icons.Filled.Timer, Icons.Outlined.Timer)
    object FocusMode : Screen("focus_mode", "Focus Mode", Icons.Filled.Timer, Icons.Outlined.Timer)
    object AcademicTools : Screen("academic_tools", "Tools", Icons.Filled.Construction, Icons.Outlined.Construction)
    object Sis : Screen("sis", "SIS", Icons.Filled.School, Icons.Outlined.School)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object HolidayCalendar : Screen("holiday_calendar", "Holiday Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
    object GpaTool : Screen("tool_gpa", "GPA Tool", Icons.Filled.School, Icons.Outlined.School)
    object AssignmentTool : Screen("tool_assignments", "Assignments", Icons.Filled.School, Icons.Outlined.School)
    object ExamTool : Screen("tool_exams", "Exams", Icons.Filled.School, Icons.Outlined.School)
    object CreditTool : Screen("tool_credits", "Credits", Icons.Filled.School, Icons.Outlined.School)
    object SubjectHistory : Screen("subject_history/{subjectId}", "Subject History", Icons.Filled.Home, Icons.Outlined.Home) {
        fun createRoute(subjectId: Int): String = "subject_history/$subjectId"
    }
    object NotesList : Screen("notes", "Notes", Icons.Filled.School, Icons.Outlined.School)
    object NoteDetail : Screen("note_detail/{noteId}", "Note", Icons.Filled.School, Icons.Outlined.School) {
        fun createRoute(noteId: Int): String = "note_detail/$noteId"
    }
    object StudyPdf : Screen("study_pdf", "Study PDF", Icons.Filled.School, Icons.Outlined.School)
    object StudyPdfViewer : Screen("study_pdf_viewer/{pdfId}", "PDF", Icons.Filled.School, Icons.Outlined.School) {
        fun createRoute(pdfId: Int): String = "study_pdf_viewer/$pdfId"
    }
}

val bottomNavItems = listOf(Screen.Home, Screen.Timetable, Screen.Study, Screen.AcademicTools, Screen.Sis, Screen.Settings)


