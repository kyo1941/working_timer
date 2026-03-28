package com.example.working_timer.ui.log_view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.working_timer.R
import com.example.working_timer.data.db.Work

@Composable
internal fun DeleteConfirmDialog(
    workToDelete: Work,
    onDismiss: () -> Unit,
    onConfirm: (Work) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.log_view_delete_dialog_title)) },
        text = { Text(stringResource(id = R.string.log_view_delete_dialog_message)) },
        confirmButton = {
            Row {
                Spacer(modifier = Modifier.weight(0.1f))

                TextButton(
                    onClick = onDismiss
                ) { Text(stringResource(id = R.string.log_view_delete_dialog_no_button)) }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { onConfirm(workToDelete) }
                ) { Text(stringResource(id = R.string.log_view_delete_dialog_yes_button)) }

                Spacer(modifier = Modifier.weight(0.1f))
            }
        },
    )
}
