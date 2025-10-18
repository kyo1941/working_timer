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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.working_timer.util.BackgroundColor


@Composable
fun SaveDialog(
    stateDate: String,
    elapsedTime: Long,
    onConfirm: () -> Unit,
    onNeutral: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* なにもしない */ },
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
                    text = "確認",
                    style = typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "作業内容を保存しますか？",
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
                        text = "開始日",
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(labelStartDate) {
                            start.linkTo(parent.start)
                            top.linkTo(parent.top)
                        }
                    )

                    Text(
                        text = stateDate,
                        style = typography.bodyMedium,
                        modifier = Modifier.constrainAs(valueStartDate) {
                            end.linkTo(parent.end)
                        }
                    )

                    Text(
                        text = "経過時間",
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

                ) {
                    TextButton(
                        onClick = onNeutral
                    ) {
                        Text(text = "再開")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(text = "破棄")
                    }

                    TextButton(
                        onClick = onConfirm
                    ) {
                        Text(text = "保存")
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

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
@Preview
private fun SaveDialogPreview() {
    SaveDialog(
        stateDate = "2024-06-01",
        elapsedTime = 3661000L,
        onConfirm = {},
        onNeutral = {},
        onDismiss = {}
    )
}
