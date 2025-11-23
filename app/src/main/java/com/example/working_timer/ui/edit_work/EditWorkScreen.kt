package com.example.working_timer.ui.edit_work

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
import androidx.compose.ui.res.stringResource
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


data class EditWorkScreenState(
    val uiState: EditWorkUiState,
    val snackbarHostState: SnackbarHostState,
    val isNew: Boolean,
    val showStartTimePicker: Boolean,
    val showStartDayPicker: Boolean,
    val showEndTimePicker: Boolean,
    val showEndDayPicker: Boolean,
    val showElapsedPicker: Boolean
)

data class EditWorkScreenActions(
    val onNavigateBack: () -> Unit,
    val onUpdateStartTime: (String) -> Unit,
    val onUpdateEndTime: (String) -> Unit,
    val onUpdateStartDay: (String) -> Unit,
    val onUpdateEndDay: (String) -> Unit,
    val onUpdateElapsedTime: (Long, Long) -> Unit,
    val onSaveWork: (Boolean) -> Unit,
    val onClearZeroMinutesError: () -> Unit,
    val onClearStartEndError: () -> Unit,
    val onClearElapsedTimeOver: () -> Unit,
    val onShowStartTimePicker: () -> Unit,
    val onHideStartTimePicker: () -> Unit,
    val onShowStartDayPicker: () -> Unit,
    val onHideStartDayPicker: () -> Unit,
    val onShowEndTimePicker: () -> Unit,
    val onHideEndTimePicker: () -> Unit,
    val onShowEndDayPicker: () -> Unit,
    val onHideEndDayPicker: () -> Unit,
    val onShowElapsedPicker: () -> Unit,
    val onHideElapsedPicker: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreenHolder(
    modifier: Modifier = Modifier,
    editWorkViewModel: EditWorkViewModel = hiltViewModel(),
    id: Int,
    startDay: String,
    isNew: Boolean,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by editWorkViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                is UiEvent.ShowSnackbar -> {
                    val message = when (event.error) {
                        is EditWorkError.InvalidDateTimeFormat -> context.getString(R.string.edit_work_view_model_error_invalid_date_time_format)
                        is EditWorkError.DatabaseError -> context.getString(R.string.edit_work_view_model_error_database)
                        is EditWorkError.UnknownError -> {
                            val detail = event.error.message ?:  context.getString(R.string.edit_work_view_model_error_unknown_detail)
                             context.getString(R.string.edit_work_view_model_error_unknown, detail)
                        }
                    }
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }

                UiEvent.SaveSuccess -> {
                    onNavigateBack()
                }
            }
        }
    }

    EditWorkScreen(
        state = EditWorkScreenState(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            isNew = isNew,
            showStartTimePicker = showStartTimePicker,
            showStartDayPicker = showStartDayPicker,
            showEndTimePicker = showEndTimePicker,
            showEndDayPicker = showEndDayPicker,
            showElapsedPicker = showElapsedPicker
        ),
        actions = EditWorkScreenActions(
            onNavigateBack = onNavigateBack,
            onUpdateStartTime = { editWorkViewModel.updateStartTime(it) },
            onUpdateEndTime = { editWorkViewModel.updateEndTime(it) },
            onUpdateStartDay = { editWorkViewModel.updateStartDay(it) },
            onUpdateEndDay = { editWorkViewModel.updateEndDay(it) },
            onUpdateElapsedTime = { hour, minute ->
                editWorkViewModel.updateElapsedTime(
                    hour,
                    minute
                )
            },
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
            onHideElapsedPicker = { showElapsedPicker = false }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkScreen(
    state: EditWorkScreenState,
    actions: EditWorkScreenActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = state.snackbarHostState) }
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current
            Text(
                text = context.getString(if (state.isNew) R.string.new_record else R.string.edit_record),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(id = R.string.edit_work_screen_start_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = actions.onShowStartDayPicker) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(formatMonthDay(state.uiState.startDay))
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(modifier = Modifier.width(32.dp))
                TextButton(onClick = actions.onShowStartTimePicker) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(state.uiState.startTime)
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.edit_work_screen_end_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = actions.onShowEndDayPicker) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(formatMonthDay(state.uiState.endDay))
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(modifier = Modifier.width(32.dp))
                TextButton(onClick = actions.onShowEndTimePicker) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(state.uiState.endTime)
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            textDecoration = TextDecoration.Underline,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Text(
                text = stringResource(id = R.string.edit_work_screen_elapsed_time_label),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = actions.onShowElapsedPicker) {
                    Text(
                        text = buildAnnotatedString {
                            if (state.uiState.elapsedHour > 0) {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(String.format("%2d", state.uiState.elapsedHour))
                                }
                                append(stringResource(id = R.string.edit_work_screen_hour_unit))
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(String.format("%2d", state.uiState.elapsedMinute))
                            }
                            append(stringResource(id = R.string.edit_work_screen_minute_unit))
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = actions.onNavigateBack,
                    modifier = Modifier
                        .width(120.dp)
                        .height(56.dp)
                ) {
                    Text(stringResource(id = R.string.edit_work_screen_cancel_button))
                }

                Spacer(modifier = Modifier.width(64.dp))

                Button(
                    onClick = { actions.onSaveWork(false) },
                    modifier = Modifier
                        .width(120.dp)
                        .height(56.dp)
                ) {
                    Text(stringResource(id = R.string.edit_work_screen_save_button))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

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
            DatePickerModal(
                initialDate = state.uiState.startDay,
                onDateSelected = {
                    actions.onUpdateStartDay(it)
                    actions.onHideStartDayPicker()
                },
                onDismiss = actions.onHideStartDayPicker
            )
        }

        if (state.showEndDayPicker) {
            DatePickerModal(
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
}

private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(0, 0)
}

private fun formatMonthDay(fullDate: String): String {
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
        state = EditWorkScreenState(
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
            showElapsedPicker = false
        ),
        actions = EditWorkScreenActions(
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
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 編集")
@Composable
fun EditWorkScreenPreviewEdit() {
    EditWorkScreen(
        state = EditWorkScreenState(
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
            showElapsedPicker = false
        ),
        actions = EditWorkScreenActions(
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
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 0分エラーダイアログ")
@Composable
fun EditWorkScreenPreviewZeroMinutesError() {
    EditWorkScreen(
        state = EditWorkScreenState(
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
            showElapsedPicker = false
        ),
        actions = EditWorkScreenActions(
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
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 開始終了時刻エラーダイアログ")
@Composable
fun EditWorkScreenPreviewStartEndError() {
    EditWorkScreen(
        state = EditWorkScreenState(
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
            showElapsedPicker = false
        ),
        actions = EditWorkScreenActions(
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
    )
}

@Preview(showBackground = true, name = "EditWorkScreen - 活動時間超過警告ダイアログ")
@Composable
fun EditWorkScreenPreviewElapsedTimeOver() {
    EditWorkScreen(
        state = EditWorkScreenState(
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
            showElapsedPicker = false
        ),
        actions = EditWorkScreenActions(
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
    )
}
