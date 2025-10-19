package com.example.working_timer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.working_timer.R
import com.example.working_timer.util.BackgroundColor


@Composable
fun SaveDialog(
    startDate: String,
    elapsedTime: Long,
    onConfirm: () -> Unit,
    onNeutral: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.title_save_dialog),
                    style = typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.check_save_work),
                    style = typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundColor)
                        .padding(8.dp)
                ) {
                    val (labelStartDate, valueStartDate, labelElapsedTime, valueElapsedTime) = createRefs()
                    Text(
                        text = stringResource(R.string.start_date_label),
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(labelStartDate) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                        }
                    )

                    Text(
                        text = startDate,
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(valueStartDate) {
                            end.linkTo(parent.end)
                        }
                    )

                    Text(
                        text = stringResource(R.string.elapsed_time_label),
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(labelElapsedTime) {
                            start.linkTo(parent.start)
                            top.linkTo(labelStartDate.bottom, margin = 4.dp)
                        }
                    )

                    Text(
                        text = formatElapsedTime(elapsedTime),
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(valueElapsedTime) {
                            end.linkTo(parent.end)
                            top.linkTo(valueStartDate.bottom, margin = 4.dp)
                        }
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextButton(
                        onClick = onNeutral
                    ) {
                        Text(text = stringResource(R.string.resume_timer_button_text))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(text = stringResource(R.string.discard_dialog_button_text))
                    }

                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text(text = stringResource(R.string.save_dialog_button_text))
                    }
                }
            }
        }

    }
}

@Composable
private fun formatElapsedTime(elapsedTime: Long) : String {
    val totalSeconds = elapsedTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        stringResource(R.string.elapsed_time_hms_ja, hours, minutes, seconds)
    } else {
        stringResource(R.string.elapsed_time_ms_ja, minutes, seconds)
    }
}

@Composable
@Preview
private fun SaveDialogPreview() {
    SaveDialog(
        startDate = "2024-06-01",
        elapsedTime = 3661000L,
        onConfirm = {},
        onNeutral = {},
        onDismiss = {}
    )
}
