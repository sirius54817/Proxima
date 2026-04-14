package com.sirius.proxima.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme

@Composable
fun AcademicToolsScreen(
    onOpenGpa: () -> Unit,
    onOpenAssignments: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenCredits: () -> Unit,
    onOpenHolidayCalendar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Academic Tools",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose a tool",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedForeground
            )
        }

        item { ToolEntry("GPA / CGPA Calculator", "Input grades and credits to calculate GPA instantly", onOpenGpa) }
        item { ToolEntry("Assignment / Deadline Tracker", "Track deadlines with reminders", onOpenAssignments) }
        item { ToolEntry("Exam Timetable", "Manage exams with countdown timers", onOpenExams) }
        item { ToolEntry("Credit Tracker", "Track completed vs required graduation credits", onOpenCredits) }
        item { ToolEntry("Holiday & Working Day Calendar", "Mark days and sync public holidays", onOpenHolidayCalendar) }
    }
}

@Composable
private fun ToolEntry(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedForeground)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun AcademicToolsScreenPreview() {
    ProximaTheme {
        AcademicToolsScreen(
            onOpenGpa = {},
            onOpenAssignments = {},
            onOpenExams = {},
            onOpenCredits = {},
            onOpenHolidayCalendar = {}
        )
    }
}

