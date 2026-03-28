package com.example.working_timer.ui.log_view

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.working_timer.data.db.Work
import com.example.working_timer.ui.components.DateRangePickerDialog
import com.example.working_timer.util.BorderColor
import java.text.SimpleDateFormat
import java.util.Locale

data class LogViewScreenState(
    val uiState: LogViewUiState,
    val showDateRangePicker: Boolean
)

data class LogViewScreenActions(
    val onNavigateToTimer: () -> Unit,
    val onNavigateToEditWork: (Int, String) -> Unit,
    val onDateSelected: (Int, Int, Int) -> Unit,
    val onShowDeleteDialog: (Work) -> Unit,
    val onHideDeleteDialog: () -> Unit,
    val onDeleteWork: (Work) -> Unit,
    val onShowDateRangePicker: () -> Unit,
    val onHideDateRangePicker: () -> Unit,
    val onDateRangeSelected: (Long?, Long?) -> Unit,
    val onHideSumDialog: () -> Unit,
    val onUpdateTotalWage: (Long) -> Unit,
    val onSetTimeCalculationMode: (TimeCalculationMode) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreenHolder(
    modifier: Modifier = Modifier,
    viewModel: LogViewViewModel = hiltViewModel(),
    onNavigateToTimer: () -> Unit,
    onNavigateToEditWork: (Int, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (uiState.selectedDay.isNotEmpty()) {
            viewModel.loadWorkList(uiState.selectedDay)
        } else {
            viewModel.init()
        }
    }

    LogViewScreen(
        state = LogViewScreenState(
            uiState = uiState,
            showDateRangePicker = showDateRangePicker
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = onNavigateToTimer,
            onNavigateToEditWork = onNavigateToEditWork,
            onDateSelected = { year, month, dayOfMonth ->
                viewModel.setSelectedDay(year, month, dayOfMonth)
            },
            onShowDeleteDialog = { work ->
                viewModel.showDeleteDialog(work)
            },
            onHideDeleteDialog = {
                viewModel.hideDeleteDialog()
            },
            onDeleteWork = { work ->
                viewModel.deleteWork(work)
            },
            onShowDateRangePicker = {
                showDateRangePicker = true
            },
            onHideDateRangePicker = {
                showDateRangePicker = false
            },
            onDateRangeSelected = { startDate, endDate ->
                if (startDate != null && endDate != null) {
                    viewModel.showSumDialog(startDate, endDate)
                }
            },
            onHideSumDialog = {
                viewModel.hideSumDialog()
            },
            onUpdateTotalWage = { wage ->
                viewModel.updateTotalWage(wage)
            },
            onSetTimeCalculationMode = { mode ->
                viewModel.setTimeCalculationMode(mode)
            }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreen(
    state: LogViewScreenState,
    actions: LogViewScreenActions,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                CalendarSection(
                    state = state,
                    actions = actions,
                    sdf = sdf,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                LogViewBottomButtons(
                    state = state,
                    actions = actions,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            VerticalDivider(
                color = BorderColor,
                thickness = 1.dp
            )

            WorkListSection(
                state = state,
                actions = actions,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            CalendarSection(
                state = state,
                actions = actions,
                sdf = sdf,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                color = BorderColor,
                thickness = 1.dp
            )

            Box(
                modifier = Modifier.weight(1f)
            ) {
                WorkListSection(state, actions, Modifier.fillMaxSize())

                LogViewBottomButtons(
                    state = state,
                    actions = actions,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }

    // 削除ダイアログ
    if (state.uiState.showDeleteDialog) {
        state.uiState.workToDelete?.let {
            DeleteConfirmDialog(
                workToDelete = it,
                onDismiss = actions.onHideDeleteDialog,
                onConfirm = actions.onDeleteWork
            )
        }
    }

    // 集計ダイアログ
    if (state.uiState.showSumDialog) {
        SumDialog(
            startDate = state.uiState.sumStartDate,
            endDate = state.uiState.sumEndDate,
            totalHours = state.uiState.totalHours,
            totalMinutes = state.uiState.totalMinutes,
            totalWage = state.uiState.totalWage,
            calculationMode = state.uiState.timeCalculationMode,
            onDismiss = actions.onHideSumDialog,
            onWageChange = actions.onUpdateTotalWage,
            onCalculationModeChange = actions.onSetTimeCalculationMode
        )
    }

    // 日付範囲選択ダイアログ
    if (state.showDateRangePicker) {
        DateRangePickerDialog(
            onDateRangeSelected = { pair ->
                val (startDate, endDate) = pair
                actions.onDateRangeSelected(startDate, endDate)
            },
            onDismiss = actions.onHideDateRangePicker
        )
    }
}

private class LogViewScreenStateProvider : PreviewParameterProvider<LogViewScreenState> {
    override val values = sequenceOf(
        LogViewScreenState(
            uiState = LogViewUiState(selectedDay = "2025-01-02", isLoading = true),
            showDateRangePicker = false
        ),
        LogViewScreenState(
            uiState = LogViewUiState(selectedDay = "2025-01-02", workList = previewSampleWorkList),
            showDateRangePicker = false
        ),
        LogViewScreenState(
            uiState = LogViewUiState(
                selectedDay = "2025-01-02",
                workList = listOf(previewSampleWork),
                showDeleteDialog = true,
                workToDelete = previewSampleWork
            ),
            showDateRangePicker = false
        ),
        LogViewScreenState(
            uiState = LogViewUiState(
                selectedDay = "2025-01-02",
                showSumDialog = true,
                sumStartDate = 1746057600000L,
                sumEndDate = 1746662400000L,
                totalHours = 40L,
                totalMinutes = 30L,
                totalWage = 40500L
            ),
            showDateRangePicker = false
        ),
        LogViewScreenState(
            uiState = LogViewUiState(selectedDay = "2025-01-02"),
            showDateRangePicker = true
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun LogViewScreenPreview(
    @PreviewParameter(LogViewScreenStateProvider::class) state: LogViewScreenState
) = LogViewScreen(state = state, actions = previewLogViewScreenActions)

@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
private fun LogViewScreenPreviewLandscape(
    @PreviewParameter(LogViewScreenStateProvider::class) state: LogViewScreenState
) = LogViewScreen(state = state, actions = previewLogViewScreenActions)

private val previewSampleWork = Work(
    id = 1,
    start_day = "2025-01-02",
    end_day = "2025-01-02",
    start_time = "09:00",
    end_time = "17:00",
    elapsed_time = 4800
)

private val previewSampleWorkList = listOf(
    previewSampleWork,
    Work(
        id = 2,
        start_day = "2025-01-02",
        end_day = "2025-01-02",
        start_time = "10:00",
        end_time = "14:00",
        elapsed_time = 2400
    ),
    Work(
        id = 3,
        start_day = "2025-01-02",
        end_day = "2025-01-02",
        start_time = "18:00",
        end_time = "22:00",
        elapsed_time = 3800
    )
)

private val previewLogViewScreenActions = LogViewScreenActions(
    onNavigateToTimer = {},
    onNavigateToEditWork = { _, _ -> },
    onDateSelected = { _, _, _ -> },
    onShowDeleteDialog = {},
    onHideDeleteDialog = {},
    onDeleteWork = {},
    onShowDateRangePicker = {},
    onHideDateRangePicker = {},
    onDateRangeSelected = { _, _ -> },
    onHideSumDialog = {},
    onUpdateTotalWage = {},
    onSetTimeCalculationMode = {}
)
