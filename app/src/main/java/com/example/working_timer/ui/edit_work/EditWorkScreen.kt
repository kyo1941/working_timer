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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreenHolder(
    editWorkViewModel: EditWorkViewModel = hiltViewModel(),
    id: Int,
    startDay: String,
    isNew: Boolean,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by editWorkViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Pickerの表示状態を管理
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDayPicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDayPicker by remember { mutableStateOf(false) }
    var showElapsedPicker by remember { mutableStateOf(false) }

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

    EditWorkScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        isNew = isNew,
        showStartTimePicker = showStartTimePicker,
        showStartDayPicker = showStartDayPicker,
        showEndTimePicker = showEndTimePicker,
        showEndDayPicker = showEndDayPicker,
        showElapsedPicker = showElapsedPicker,
        onNavigateBack = onNavigateBack,
        onUpdateStartTime = { editWorkViewModel.updateStartTime(it) },
        onUpdateEndTime = { editWorkViewModel.updateEndTime(it) },
        onUpdateStartDay = { editWorkViewModel.updateStartDay(it) },
        onUpdateEndDay = { editWorkViewModel.updateEndDay(it) },
        onUpdateElapsedTime = { hour, minute -> editWorkViewModel.updateElapsedTime(hour, minute) },
        onSaveWork = { editWorkViewModel.saveWork(id, isNew, it) },
        onClearZeroMinutesError = { editWorkViewModel.clearZeroMinutesError() },
        onClearStartEndError = { editWorkViewModel.clearStartEndError() },
        onClearElapsedTimeOver = { editWorkViewModel.clearElapsedTimeOver() },
        onShowStartTimePicker = { showStartTimePicker = true },
        onHideStartTimePicker = { showStartTimePicker = false },
        onShowStartDayPicker = { showStartDayPicker = true },
        onHideStartDayPicker = { showStartDayPicker = false },
        onShowEndTimePicker = { showEndTimePicker = true },
        onHideEndTimePicker = { showEndTimePicker = false },
        onShowEndDayPicker = { showEndDayPicker = true },
        onHideEndDayPicker = { showEndDayPicker = false },
        onShowElapsedPicker = { showElapsedPicker = true },
        onHideElapsedPicker = { showElapsedPicker = false },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreen(
    uiState: EditWorkUiState,
    snackbarHostState: SnackbarHostState,
    isNew: Boolean,
    showStartTimePicker: Boolean,
    showStartDayPicker: Boolean,
    showEndTimePicker: Boolean,
    showEndDayPicker: Boolean,
    showElapsedPicker: Boolean,
    onNavigateBack: () -> Unit,
    onUpdateStartTime: (String) -> Unit,
    onUpdateEndTime: (String) -> Unit,
    onUpdateStartDay: (String) -> Unit,
    onUpdateEndDay: (String) -> Unit,
    onUpdateElapsedTime: (Int, Int) -> Unit,
    onSaveWork: (Boolean) -> Unit,
    onClearZeroMinutesError: () -> Unit,
    onClearStartEndError: () -> Unit,
    onClearElapsedTimeOver: () -> Unit,
    onShowStartTimePicker: () -> Unit,
    onHideStartTimePicker: () -> Unit,
    onShowStartDayPicker: () -> Unit,
    onHideStartDayPicker: () -> Unit,
    onShowEndTimePicker: () -> Unit,
    onHideEndTimePicker: () -> Unit,
    onShowEndDayPicker: () -> Unit,
    onHideEndDayPicker: () -> Unit,
    onShowElapsedPicker: () -> Unit,
    onHideElapsedPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                TextButton(onClick = onShowStartDayPicker) {
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
                TextButton(onClick = onShowStartTimePicker) {
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
                TextButton(onClick = onShowEndDayPicker) {
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
                TextButton(onClick = onShowEndTimePicker) {
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
                TextButton(onClick = onShowElapsedPicker) {
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
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("キャンセル")
                }

                Spacer(modifier = Modifier.width(24.dp))

                Button(
                    onClick = { onSaveWork(false) },
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
                onDismiss = onHideStartTimePicker,
                onTimeSelected = {
                    onUpdateStartTime(it)
                    onHideStartTimePicker()
                }
            )
        }

        if (showEndTimePicker) {
            MaterialTimePickerDialog(
                initialTime = parseTime(uiState.endTime),
                onDismiss = onHideEndTimePicker,
                onTimeSelected = {
                    onUpdateEndTime(it)
                    onHideEndTimePicker()
                }
            )
        }

        if (showStartDayPicker) {
            DatePickerModal(
                initialDate = uiState.startDay,
                onDateSelected = {
                    onUpdateStartDay(it)
                    onHideStartDayPicker()
                },
                onDismiss = onHideStartDayPicker
            )
        }

        if (showEndDayPicker) {
            DatePickerModal(
                initialDate = uiState.endDay,
                onDateSelected = {
                    onUpdateEndDay(it)
                    onHideEndDayPicker()
                },
                onDismiss = onHideEndDayPicker
            )
        }

        if (showElapsedPicker) {
            MaterialTimePickerDialog(
                initialTime = Pair(uiState.elapsedHour, uiState.elapsedMinute),
                onDismiss = onHideElapsedPicker,
                onTimeSelected = { timeString ->
                    val (h, m) = timeString.split(":").map { it.toIntOrNull() ?: 0 }
                    onUpdateElapsedTime(h, m)
                    onHideElapsedPicker()
                },
                showToggleIcon = false
            )
        }

        if (uiState.showZeroMinutesError) {
            AlertDialog(
                onDismissRequest = onClearZeroMinutesError,
                title = { Text("エラー") },
                text = { Text("1分以上からのみ記録が可能です。") },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    TextButton(onClick = onClearZeroMinutesError) {
                        Text("OK")
                    }
                }
            )
        }

        if (uiState.showStartEndError) {
            AlertDialog(
                onDismissRequest = onClearStartEndError,
                title = { Text("エラー") },
                text = { Text("開始時刻が終了時刻を超えています。") },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    TextButton(onClick = onClearStartEndError) {
                        Text("OK")
                    }
                }
            )
        }

        if (uiState.showElapsedTimeOver) {
            AlertDialog(
                onDismissRequest = onClearElapsedTimeOver,
                title = { Text("注意") },
                text = { Text("活動時間が時間差より大きいです。\nこのまま保存しますか？") },
                confirmButton = {
                    TextButton(onClick = {
                        onClearElapsedTimeOver()
                        onSaveWork(true)
                    }) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClearElapsedTimeOver) {
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

@Preview(showBackground = true, name = "EditWorkScreen - 新規作成")
@Composable
fun EditWorkScreenPreviewNew() {
    EditWorkScreen(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "09:00",
            endTime = "18:00",
            elapsedHour = 8,
            elapsedMinute = 0
        ),
        snackbarHostState = SnackbarHostState(),
        isNew = true,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false,
        onNavigateBack = {},
        onUpdateStartTime = {},
        onUpdateEndTime = {},
        onUpdateStartDay = {},
        onUpdateEndDay = {},
        onUpdateElapsedTime = { _, _ -> },
        onSaveWork = {},
        onClearZeroMinutesError = {},
        onClearStartEndError = {},
        onClearElapsedTimeOver = {},
        onShowStartTimePicker = {},
        onHideStartTimePicker = {},
        onShowStartDayPicker = {},
        onHideStartDayPicker = {},
        onShowEndTimePicker = {},
        onHideEndTimePicker = {},
        onShowEndDayPicker = {},
        onHideEndDayPicker = {},
        onShowElapsedPicker = {},
        onHideElapsedPicker = {}
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 編集")
@Composable
fun EditWorkScreenPreviewEdit() {
    EditWorkScreen(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "14:30",
            endTime = "17:45",
            elapsedHour = 3,
            elapsedMinute = 15
        ),
        snackbarHostState = SnackbarHostState(),
        isNew = false,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false,
        onNavigateBack = {},
        onUpdateStartTime = {},
        onUpdateEndTime = {},
        onUpdateStartDay = {},
        onUpdateEndDay = {},
        onUpdateElapsedTime = { _, _ -> },
        onSaveWork = {},
        onClearZeroMinutesError = {},
        onClearStartEndError = {},
        onClearElapsedTimeOver = {},
        onShowStartTimePicker = {},
        onHideStartTimePicker = {},
        onShowStartDayPicker = {},
        onHideStartDayPicker = {},
        onShowEndTimePicker = {},
        onHideEndTimePicker = {},
        onShowEndDayPicker = {},
        onHideEndDayPicker = {},
        onShowElapsedPicker = {},
        onHideElapsedPicker = {}
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 0分エラーダイアログ")
@Composable
fun EditWorkScreenPreviewZeroMinutesError() {
    EditWorkScreen(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "09:00",
            endTime = "09:00",
            elapsedHour = 0,
            elapsedMinute = 0,
            showZeroMinutesError = true
        ),
        snackbarHostState = SnackbarHostState(),
        isNew = true,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false,
        onNavigateBack = {},
        onUpdateStartTime = {},
        onUpdateEndTime = {},
        onUpdateStartDay = {},
        onUpdateEndDay = {},
        onUpdateElapsedTime = { _, _ -> },
        onSaveWork = {},
        onClearZeroMinutesError = {},
        onClearStartEndError = {},
        onClearElapsedTimeOver = {},
        onShowStartTimePicker = {},
        onHideStartTimePicker = {},
        onShowStartDayPicker = {},
        onHideStartDayPicker = {},
        onShowEndTimePicker = {},
        onHideEndTimePicker = {},
        onShowEndDayPicker = {},
        onHideEndDayPicker = {},
        onShowElapsedPicker = {},
        onHideElapsedPicker = {}
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 開始終了時刻エラーダイアログ")
@Composable
fun EditWorkScreenPreviewStartEndError() {
    EditWorkScreen(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "18:00",
            endTime = "09:00",
            elapsedHour = 0,
            elapsedMinute = 30,
            showStartEndError = true
        ),
        snackbarHostState = SnackbarHostState(),
        isNew = false,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false,
        onNavigateBack = {},
        onUpdateStartTime = {},
        onUpdateEndTime = {},
        onUpdateStartDay = {},
        onUpdateEndDay = {},
        onUpdateElapsedTime = { _, _ -> },
        onSaveWork = {},
        onClearZeroMinutesError = {},
        onClearStartEndError = {},
        onClearElapsedTimeOver = {},
        onShowStartTimePicker = {},
        onHideStartTimePicker = {},
        onShowStartDayPicker = {},
        onHideStartDayPicker = {},
        onShowEndTimePicker = {},
        onHideEndTimePicker = {},
        onShowEndDayPicker = {},
        onHideEndDayPicker = {},
        onShowElapsedPicker = {},
        onHideElapsedPicker = {}
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 活動時間超過警告ダイアログ")
@Composable
fun EditWorkScreenPreviewElapsedTimeOver() {
    EditWorkScreen(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "09:00",
            endTime = "17:00",
            elapsedHour = 10,
            elapsedMinute = 0,
            showElapsedTimeOver = true
        ),
        snackbarHostState = SnackbarHostState(),
        isNew = false,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false,
        onNavigateBack = {},
        onUpdateStartTime = {},
        onUpdateEndTime = {},
        onUpdateStartDay = {},
        onUpdateEndDay = {},
        onUpdateElapsedTime = { _, _ -> },
        onSaveWork = {},
        onClearZeroMinutesError = {},
        onClearStartEndError = {},
        onClearElapsedTimeOver = {},
        onShowStartTimePicker = {},
        onHideStartTimePicker = {},
        onShowStartDayPicker = {},
        onHideStartDayPicker = {},
        onShowEndTimePicker = {},
        onHideEndTimePicker = {},
        onShowEndDayPicker = {},
        onHideEndDayPicker = {},
        onShowElapsedPicker = {},
        onHideElapsedPicker = {}
    )
}
