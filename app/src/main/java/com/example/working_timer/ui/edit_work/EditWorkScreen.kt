package com.example.working_timer.ui.edit_work

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import com.example.working_timer.ui.components.DatePickerModal
import com.example.working_timer.ui.components.MaterialTimePickerDialog
import com.example.working_timer.R
import androidx.compose.runtime.collectAsState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreen(
    viewModel: EditWorkViewModel,
    id: Int,
    startDay: String,
    endDay: String,
    startTime: String,
    endTime: String,
    elapsedTime: Int,
    isNew: Boolean,
    onSave: (String, String, String, String, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var startDay by remember { mutableStateOf(startDay) }
    var endDay by remember { mutableStateOf(endDay) }
    var startTime by remember { mutableStateOf(startTime) }
    var endTime by remember { mutableStateOf(endTime) }
    var elapsedHour by remember { mutableStateOf(elapsedTime / 3600) }
    var elapsedMinute by remember { mutableStateOf((elapsedTime % 3600) / 60) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDayPicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDayPicker by remember { mutableStateOf(false) }

    var showElapsedPicker by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState(initial = EditWorkUiState())

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val context = LocalContext.current
        Text (
            text = context.getString(if(isNew) R.string.new_record else R.string.edit_record),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("開始")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showStartDayPicker = true }) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(formatMonthDay(startDay))
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            TextButton(onClick = { showStartTimePicker = true }) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(startTime)
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Text("終了")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showEndDayPicker = true }) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(formatMonthDay(endDay))
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            TextButton(onClick = { showEndTimePicker = true }) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(endTime)
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Text("活動時間")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showElapsedPicker = true }) {
                Text(
                    text = buildAnnotatedString {
                        if (elapsedHour > 0) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(String.format("%2d", elapsedHour))
                            }
                            append(" 時間 ")
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(String.format("%2d", elapsedMinute))
                        }
                        append(" 分")
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    (context as? Activity)?.finish()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("キャンセル")
            }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    val newElapsed = elapsedHour * 3600 + elapsedMinute * 60

                    onSave(startDay, startTime, endDay, endTime, newElapsed, false)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }
    }

    if (showStartTimePicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(startTime),
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                startTime = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(endTime),
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                endTime = it
                showEndTimePicker = false
            }
        )
    }

    if (showStartDayPicker) {
        DatePickerModal(
            initialDate = startDay,
            onDateSelected = {
                startDay = it
                showStartDayPicker = false
            },
            onDismiss = { showStartDayPicker = false }
        )
    }

    if (showEndDayPicker) {
        DatePickerModal(
            initialDate = endDay,
            onDateSelected = {
                endDay = it
                showEndDayPicker = false
            },
            onDismiss = { showEndDayPicker = false }
        )
    }

    if (showElapsedPicker) {
        MaterialTimePickerDialog(
            initialTime = Pair(elapsedHour, elapsedMinute),
            onDismiss = { showElapsedPicker = false },
            onTimeSelected = { timeString ->
                val (h, m) = timeString.split(":").map { it.toIntOrNull() ?: 0 }
                elapsedHour = h
                elapsedMinute = m
                showElapsedPicker = false
            },
            showToggleIcon = false
        )
    }

    if(uiState.showZeroMinutesError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearZeroMinutesError() },
            title = { Text("エラー") },
            text = { Text("1分以上からのみ記録が可能です。") },
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = {
                TextButton(onClick = { viewModel.clearZeroMinutesError() }) {
                    Text("OK")
                }
            }
        )
    }

    if(uiState.showStartEndError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearStartEndError() },
            title = { Text("エラー") },
            text = { Text("開始時刻が終了時刻を超えています。") },
            properties = DialogProperties(dismissOnClickOutside = false),
            confirmButton = {
                TextButton(onClick = { viewModel.clearStartEndError() }) {
                    Text("OK")
                }
            }
        )
    }

    if(uiState.showElapsedTimeOver) {
        AlertDialog(
            onDismissRequest = { viewModel.clearElapsedTimeOver() },
            title = { Text("注意") },
            text = { Text("活動時間が時間差より大きいです。\nこのまま保存しますか？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearElapsedTimeOver()
                    val newElapsed = elapsedHour * 3600 + elapsedMinute * 60
                    onSave(startDay, startTime, endDay, endTime, newElapsed, true)
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearElapsedTimeOver() }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(0, 0)
}

fun formatMonthDay(fullDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val date = inputFormat.parse(fullDate)
        outputFormat.format(date)
    } catch (e: Exception) {
        fullDate
    }
}



