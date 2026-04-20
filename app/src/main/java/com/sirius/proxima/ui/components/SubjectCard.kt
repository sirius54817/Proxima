package com.sirius.proxima.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.data.model.Subject
import com.sirius.proxima.ui.theme.AttendanceGreen
import com.sirius.proxima.ui.theme.AttendanceRed
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.MutedForeground
import com.sirius.proxima.ui.theme.ProximaTheme

@Composable
fun SubjectCard(
    subject: Subject,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit,
    onMarkOnDuty: () -> Unit,
    onHide: () -> Unit,
    onUnhide: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isInEditMode: Boolean,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isHidden = subject.isHidden
    val isAbove75 = subject.percentage >= 75f
    val percentColor = if (isHidden) MutedForeground else if (isAbove75) AttendanceGreen else AttendanceRed
    val strike = if (isHidden) TextDecoration.LineThrough else TextDecoration.None

    Box(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (isInEditMode && onSelectToggle != null) {
                            onSelectToggle()
                        } else if (!isHidden) {
                            onClick()
                        }
                    },
                    onLongClick = onLongPress
                ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isHidden) Border.copy(alpha = 0.5f) else Border),
            colors = CardDefaults.cardColors(
                containerColor = if (isHidden) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = percentColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = strike
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${subject.attendedClasses}/${subject.totalClasses} classes • ${"%.1f".format(subject.percentage)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedForeground,
                            textDecoration = strike
                        )
                    }

                    if (isInEditMode && onSelectToggle != null) {
                        Checkbox(checked = isSelected, onCheckedChange = { onSelectToggle() })
                    } else if (!isHidden) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MutedForeground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHidden) {
                        "Hidden subject. Long-press to delete or tap Unhide to restore."
                    } else if (isAbove75) {
                        if (subject.canMissClasses > 0) "You can miss ${subject.canMissClasses} more class${if (subject.canMissClasses > 1) "es" else ""}"
                        else "You're at exactly 75% — don't miss any!"
                    } else {
                        if (subject.needToAttendClasses > 0) "Attend ${subject.needToAttendClasses} more class${if (subject.needToAttendClasses > 1) "es" else ""} to reach 75%"
                        else "Attend more classes to reach 75%"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = percentColor.copy(alpha = 0.8f),
                    textDecoration = strike
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isHidden && !isInEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onUnhide,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Unhide",
                                tint = MutedForeground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unhide", color = MutedForeground, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else if (!isInEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onMarkPresent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AttendanceGreen.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Present",
                                tint = AttendanceGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Present", color = AttendanceGreen, style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onMarkOnDuty,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Border),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkHistory,
                                contentDescription = "On Duty",
                                tint = MutedForeground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("On Duty", color = MutedForeground, style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onMarkAbsent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AttendanceRed.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Absent",
                                tint = AttendanceRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Absent", color = AttendanceRed, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                if (isInEditMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isHidden) {
                            OutlinedButton(
                                onClick = onUnhide,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Border),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Unhide",
                                    tint = MutedForeground,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unhide", color = MutedForeground, style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onHide,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Border),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "Hide",
                                    tint = MutedForeground,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hide", color = MutedForeground, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SubjectCardPreview() {
    ProximaTheme {
        SubjectCard(
            subject = Subject(
                id = 1,
                name = "Mathematics",
                totalClasses = 40,
                attendedClasses = 35
            ),
            onMarkPresent = {},
            onMarkAbsent = {},
            onMarkOnDuty = {},
            onHide = {},
            onUnhide = {},
            onDelete = {},
            onEdit = {},
            onClick = {},
            onLongPress = {},
            isInEditMode = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SubjectCardLowAttendancePreview() {
    ProximaTheme {
        SubjectCard(
            subject = Subject(
                id = 2,
                name = "Physics",
                totalClasses = 40,
                attendedClasses = 25
            ),
            onMarkPresent = {},
            onMarkAbsent = {},
            onMarkOnDuty = {},
            onHide = {},
            onUnhide = {},
            onDelete = {},
            onEdit = {},
            onClick = {},
            onLongPress = {},
            isInEditMode = true
        )
    }
}

