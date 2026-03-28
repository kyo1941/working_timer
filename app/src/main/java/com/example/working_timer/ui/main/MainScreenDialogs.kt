package com.example.working_timer.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.working_timer.R
import com.example.working_timer.ui.components.SaveDialog

@Composable
internal fun MainScreenDialogs(
    dialogStatus: DialogStatus,
    onSaveWork: () -> Unit,
    onResumeTimer: () -> Unit,
    onDiscardWork: () -> Unit,
    onDismissSaveDialog: () -> Unit
) {
    when (dialogStatus) {
        is DialogStatus.SaveDialog -> SaveDialog(
            startDate = dialogStatus.startDate,
            elapsedTime = dialogStatus.elapsedTime,
            onConfirm = onSaveWork,
            onNeutral = {
                onResumeTimer()
                onDismissSaveDialog()
            },
            onDismiss = {
                onDiscardWork()
                onDismissSaveDialog()
            }
        )

        is DialogStatus.TooShortTimeErrorDialog -> ErrorAlertDialog(
            message = stringResource(R.string.error_time_too_short),
            onClick = {
                onResumeTimer()
                onDismissSaveDialog()
            },
            onDismiss = {
                onDiscardWork()
                onDismissSaveDialog()
            }
        )

        is DialogStatus.DataNotFoundErrorDialog -> ErrorAlertDialog(
            message = stringResource(R.string.error_data_not_found),
            onClick = {
                onResumeTimer()
                onDismissSaveDialog()
            },
            onDismiss = {
                onDiscardWork()
                onDismissSaveDialog()
            }
        )
    }
}
