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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreen(
    editWorkViewModel: EditWorkViewModel = hiltViewModel(),
    id: Int,
    startDay: String,
    isNew: Boolean,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDayPicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDayPicker by remember { mutableStateOf(false) }

    var showElapsedPicker by remember { mutableStateOf(false) }

    val uiState by editWorkViewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 初期値を設定する
        editWorkViewModel.init(id, isNew, startDay)

        // イベントを監視する
        editWorkViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is EditWorkViewModel.UiEvent.ShowSnackbar -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
                EditWorkViewModel.UiEvent.SaveSuccess -> { onNavigateBack() }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val context = LocalContext.current
            Text(
                text = context.getString(if (isNew) R.string.new_record else R.string.edit_record),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.weight(0.3f))

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
                                append(formatMonthDay(uiState.startDay))
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
                                append(uiState.startTime)
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
                                append(formatMonthDay(uiState.endDay))
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
                                append(uiState.endTime)
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
                            if (uiState.elapsedHour > 0) {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(String.format("%2d", uiState.elapsedHour))
                                }
                                append(" 時間 ")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(String.format("%2d", uiState.elapsedMinute))
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

            Spacer(modifier = Modifier.weight(0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("キャンセル")
                }

                Spacer(modifier = Modifier.width(24.dp))

                Button(
                    onClick = {
                        editWorkViewModel.saveWork(
                            id = id,
                            isNew = isNew,
                            forceSave = false
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        if (showStartTimePicker) {
            MaterialTimePickerDialog(
                initialTime = parseTime(uiState.startTime),
                onDismiss = { showStartTimePicker = false },
                onTimeSelected = {
                    editWorkViewModel.updateStartTime(it)
                    showStartTimePicker = false
                }
            )
        }

        if (showEndTimePicker) {
            MaterialTimePickerDialog(
                initialTime = parseTime(uiState.endTime),
                onDismiss = { showEndTimePicker = false },
                onTimeSelected = {
                    editWorkViewModel.updateEndTime(it)
                    showEndTimePicker = false
                }
            )
        }

        if (showStartDayPicker) {
            DatePickerModal(
                initialDate = uiState.startDay,
                onDateSelected = {
                    editWorkViewModel.updateStartDay(it)
                    showStartDayPicker = false
                },
                onDismiss = { showStartDayPicker = false }
            )
        }

        if (showEndDayPicker) {
            DatePickerModal(
                initialDate = uiState.endDay,
                onDateSelected = {
                    editWorkViewModel.updateEndDay(it)
                    showEndDayPicker = false
                },
                onDismiss = { showEndDayPicker = false }
            )
        }

        if (showElapsedPicker) {
            MaterialTimePickerDialog(
                initialTime = Pair(uiState.elapsedHour, uiState.elapsedMinute),
                onDismiss = { showElapsedPicker = false },
                onTimeSelected = { timeString ->
                    val (h, m) = timeString.split(":").map { it.toIntOrNull() ?: 0 }
                    editWorkViewModel.updateElapsedTime(h, m)
                    showElapsedPicker = false
                },
                showToggleIcon = false
            )
        }

        if (uiState.showZeroMinutesError) {
            AlertDialog(
                onDismissRequest = { editWorkViewModel.clearZeroMinutesError() },
                title = { Text("エラー") },
                text = { Text("1分以上からのみ記録が可能です。") },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    TextButton(onClick = { editWorkViewModel.clearZeroMinutesError() }) {
                        Text("OK")
                    }
                }
            )
        }

        if (uiState.showStartEndError) {
            AlertDialog(
                onDismissRequest = { editWorkViewModel.clearStartEndError() },
                title = { Text("エラー") },
                text = { Text("開始時刻が終了時刻を超えています。") },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    TextButton(onClick = { editWorkViewModel.clearStartEndError() }) {
                        Text("OK")
                    }
                }
            )
        }

        if (uiState.showElapsedTimeOver) {
            AlertDialog(
                onDismissRequest = { editWorkViewModel.clearElapsedTimeOver() },
                title = { Text("注意") },
                text = { Text("活動時間が時間差より大きいです。\nこのまま保存しますか？") },
                confirmButton = {
                    TextButton(onClick = {
                        editWorkViewModel.clearElapsedTimeOver()
                        editWorkViewModel.saveWork(
                            id = id,
                            isNew = isNew,
                            forceSave = true
                        )
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editWorkViewModel.clearElapsedTimeOver() }) {
                        Text("キャンセル")
                    }
                }
            )
        }
    }
}

fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(0, 0)
}

fun formatMonthDay(fullDate: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val date = inputFormat.parse(fullDate)
        outputFormat.format(date)
    } catch (e: Exception) {
        fullDate
    }
}
