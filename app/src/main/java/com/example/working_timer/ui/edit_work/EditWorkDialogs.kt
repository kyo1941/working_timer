package com.example.working_timer.ui.edit_work

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.example.working_timer.R
import com.example.working_timer.ui.components.DatePickerDialog
import com.example.working_timer.ui.components.MaterialTimePickerDialog

@Composable
internal fun EditWorkDialogs(
    state: EditWorkScreenState,
    actions: EditWorkScreenActions
) {
    if (state.showStartTimePicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(state.uiState.startTime),
            onDismiss = actions.onHideStartTimePicker,
            onTimeSelected = {
                actions.onUpdateStartTime(it)
                actions.onHideStartTimePicker()
            }
        )
    }

    if (state.showEndTimePicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(state.uiState.endTime),
            onDismiss = actions.onHideEndTimePicker,
            onTimeSelected = {
                actions.onUpdateEndTime(it)
                actions.onHideEndTimePicker()
            }
        )
    }

    if (state.showStartDayPicker) {
        DatePickerDialog(
            initialDate = state.uiState.startDay,
            onDateSelected = {
                actions.onUpdateStartDay(it)
                actions.onHideStartDayPicker()
            },
            onDismiss = actions.onHideStartDayPicker
        )
    }

    if (state.showEndDayPicker) {
        DatePickerDialog(
            initialDate = state.uiState.endDay,
            onDateSelected = {
                actions.onUpdateEndDay(it)
                actions.onHideEndDayPicker()
            },
            onDismiss = actions.onHideEndDayPicker
        )
    }

    if (state.showElapsedPicker) {
        MaterialTimePickerDialog(
            initialTime = Pair(
                state.uiState.elapsedHour.toInt(),
                state.uiState.elapsedMinute.toInt()
            ),
            onDismiss = actions.onHideElapsedPicker,
            onTimeSelected = { timeString ->
                val (h, m) = timeString.split(":").map { it.toLongOrNull() ?: 0L }
                actions.onUpdateElapsedTime(h, m)
                actions.onHideElapsedPicker()
            },
            showToggleIcon = false
        )
    }

    if (state.uiState.showZeroMinutesError) {
        AlertDialog(
            onDismissRequest = actions.onClearZeroMinutesError,
            title = { Text(stringResource(id = R.string.edit_work_screen_error_dialog_title)) },
            text = { Text(stringResource(id = R.string.edit_work_screen_zero_minutes_error_message)) },
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = {
                TextButton(onClick = actions.onClearZeroMinutesError) {
                    Text(stringResource(id = R.string.edit_work_screen_dialog_ok_button))
                }
            }
        )
    }

    if (state.uiState.showStartEndError) {
        AlertDialog(
            onDismissRequest = actions.onClearStartEndError,
            title = { Text(stringResource(id = R.string.edit_work_screen_error_dialog_title)) },
            text = { Text(stringResource(id = R.string.edit_work_screen_start_end_error_message)) },
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = {
                TextButton(onClick = actions.onClearStartEndError) {
                    Text(stringResource(id = R.string.edit_work_screen_dialog_ok_button))
                }
            }
        )
    }

    if (state.uiState.showElapsedTimeOver) {
        AlertDialog(
            onDismissRequest = actions.onClearElapsedTimeOver,
            title = { Text(stringResource(id = R.string.edit_work_screen_warning_dialog_title)) },
            text = { Text(stringResource(id = R.string.edit_work_screen_elapsed_time_over_warning_message)) },
            confirmButton = {
                Row {
                    Spacer(modifier = Modifier.weight(0.1f))
                    TextButton(onClick = actions.onClearElapsedTimeOver) {
                        Text(stringResource(id = R.string.edit_work_screen_warning_dialog_cancel_button))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = {
                        actions.onClearElapsedTimeOver()
                        actions.onSaveWork(true)
                    }) {
                        Text(stringResource(id = R.string.edit_work_screen_warning_dialog_save_button))
                    }

                    Spacer(modifier = Modifier.weight(0.1f))
                }
            },
        )
    }
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(0, 0)
}
