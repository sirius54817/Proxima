package com.sirius.proxima.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sirius.proxima.ui.theme.Border
import com.sirius.proxima.ui.theme.DangerRed
import com.sirius.proxima.ui.theme.ProximaTheme

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDangerous: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isDangerous) DangerRed else MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            if (isDangerous) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(confirmText)
                }
            } else {
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Text(dismissText, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    )
}

@Preview
@Composable
fun ConfirmDialogPreview() {
    ProximaTheme {
        ConfirmDialog(
            title = "Delete Subject",
            message = "Are you sure you want to delete this subject?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun ConfirmDialogDangerousPreview() {
    ProximaTheme {
        ConfirmDialog(
            title = "Clear All Data",
            message = "This will delete all data from the app AND your Google Drive backup. This cannot be undone.",
            confirmText = "Clear All",
            isDangerous = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

