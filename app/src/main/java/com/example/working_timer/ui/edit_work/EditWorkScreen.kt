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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.working_timer.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


data class EditWorkScreenState(
    val uiState: EditWorkUiState,
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
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by editWorkViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Pickerの表示状態を管理
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDayPicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDayPicker by remember { mutableStateOf(false) }
    var showElapsedPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // イベントを監視する
        editWorkViewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val message = when (event.error) {
                        is EditWorkError.InvalidDateTimeFormat -> context.getString(R.string.edit_work_view_model_error_invalid_date_time_format)
                        is EditWorkError.DatabaseError -> context.getString(R.string.edit_work_view_model_error_database)
                        is EditWorkError.UnknownError -> {
                            val detail = event.error.message
                                ?: context.getString(R.string.edit_work_view_model_error_unknown_detail)
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
            isNew = (id == 0),
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
            onSaveWork = { editWorkViewModel.saveWork(id, it) },
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
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = context.getString(if (state.isNew) R.string.new_record else R.string.edit_record),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .padding(vertical = 8.dp)
                    .padding(top = 24.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // 左側: 開始、終了
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DateTimeSection(
                        label = stringResource(id = R.string.edit_work_screen_start_label),
                        day = state.uiState.startDay,
                        time = state.uiState.startTime,
                        onShowDayPicker = actions.onShowStartDayPicker,
                        onShowTimePicker = actions.onShowStartTimePicker,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    DateTimeSection(
                        label = stringResource(id = R.string.edit_work_screen_end_label),
                        day = state.uiState.endDay,
                        time = state.uiState.endTime,
                        onShowDayPicker = actions.onShowEndDayPicker,
                        onShowTimePicker = actions.onShowEndTimePicker
                    )
                }

                // 右側: 活動時間
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ElapsedSection(state, actions)
                }
            }

            EditWorkActionButtons(actions, Modifier.padding(bottom = 32.dp))
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            DateTimeSection(
                label = stringResource(id = R.string.edit_work_screen_start_label),
                day = state.uiState.startDay,
                time = state.uiState.startTime,
                onShowDayPicker = actions.onShowStartDayPicker,
                onShowTimePicker = actions.onShowStartTimePicker,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            DateTimeSection(
                label = stringResource(id = R.string.edit_work_screen_end_label),
                day = state.uiState.endDay,
                time = state.uiState.endTime,
                onShowDayPicker = actions.onShowEndDayPicker,
                onShowTimePicker = actions.onShowEndTimePicker,
                modifier = Modifier.padding(bottom = 40.dp)
            )
            ElapsedSection(state, actions, Modifier.padding(bottom = 40.dp))

            Spacer(modifier = Modifier.weight(0.2f))

            EditWorkActionButtons(actions)

            Spacer(modifier = Modifier.weight(1f))
        }
    }

    EditWorkDialogs(state, actions)
}

private class EditWorkScreenStateProvider : PreviewParameterProvider<EditWorkScreenState> {
    private val baseState = EditWorkScreenState(
        uiState = EditWorkUiState(
            startDay = "2024-03-15",
            endDay = "2024-03-15",
            startTime = "09:00",
            endTime = "18:00",
            elapsedHour = 8,
            elapsedMinute = 0
        ),
        isNew = true,
        showStartTimePicker = false,
        showStartDayPicker = false,
        showEndTimePicker = false,
        showEndDayPicker = false,
        showElapsedPicker = false
    )

    override val values = sequenceOf(
        baseState,
        baseState.copy(
            isNew = false,
            uiState = baseState.uiState.copy(startTime = "14:30", endTime = "17:45", elapsedHour = 3, elapsedMinute = 15)
        ),
        baseState.copy(
            uiState = baseState.uiState.copy(startTime = "09:00", endTime = "09:00", elapsedHour = 0, elapsedMinute = 0, showZeroMinutesError = true)
        ),
        baseState.copy(
            isNew = false,
            uiState = baseState.uiState.copy(startTime = "18:00", endTime = "09:00", elapsedHour = 0, elapsedMinute = 30, showStartEndError = true)
        ),
        baseState.copy(
            isNew = false,
            uiState = baseState.uiState.copy(startTime = "09:00", endTime = "17:00", elapsedHour = 10, elapsedMinute = 0, showElapsedTimeOver = true)
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun EditWorkScreenPreview(
    @PreviewParameter(EditWorkScreenStateProvider::class) state: EditWorkScreenState
) = EditWorkScreen(state = state, actions = previewEditWorkScreenActions)

@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun EditWorkScreenPreviewLandscape(
    @PreviewParameter(EditWorkScreenStateProvider::class) state: EditWorkScreenState
) = EditWorkScreen(state = state, actions = previewEditWorkScreenActions)

private val previewEditWorkScreenActions = EditWorkScreenActions(
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
