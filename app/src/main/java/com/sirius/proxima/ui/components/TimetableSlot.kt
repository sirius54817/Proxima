package com.sirius.proxima.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.data.model.TimetableEntry
import com.sirius.proxima.ui.theme.*

@Composable
fun TimetableSlot(
    hourSlot: Int,
    entry: TimetableEntry?,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeLabel = formatHour(hourSlot)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time label
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MutedForeground,
            modifier = Modifier.width(80.dp)
        )

        if (entry != null) {
            // Filled slot
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                onClick = onEdit
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val isDeleted = entry.subjectName == "[Deleted Subject]"
                        Text(
                            text = entry.subjectName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isDeleted) Muted else MaterialTheme.colorScheme.onSurface
                        )
                        if (entry.classNumber.isNotBlank()) {
                            Text(
                                text = entry.classNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MutedForeground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            // Empty slot - dashed border
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add class",
                    tint = Muted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add class",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
        }
    }
}

fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12:00 AM"
        hour < 12 -> "$hour:00 AM"
        hour == 12 -> "12:00 PM"
        else -> "${hour - 12}:00 PM"
    }
}

@Preview(showBackground = true)
@Composable
fun TimetableSlotEmptyPreview() {
    ProximaTheme {
        TimetableSlot(
            hourSlot = 9,
            entry = null,
            onAdd = {},
            onEdit = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimetableSlotFilledPreview() {
    ProximaTheme {
        TimetableSlot(
            hourSlot = 10,
            entry = TimetableEntry(
                id = 1,
                dayOfWeek = 1,
                hourSlot = 10,
                subjectId = 1,
                subjectName = "Mathematics",
                classNumber = "Room 301"
            ),
            onAdd = {},
            onEdit = {},
            onDelete = {}
        )
    }
}



