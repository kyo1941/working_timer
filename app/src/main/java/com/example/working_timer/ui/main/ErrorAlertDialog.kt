package com.example.working_timer.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.working_timer.R

@Composable
internal fun ErrorAlertDialog(
    message: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = stringResource(R.string.title_error_dialog),
                style = typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = typography.bodyMedium
            )
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        confirmButton = {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.resume_timer_button_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard_dialog_button_text))
            }
        }
    )
}
