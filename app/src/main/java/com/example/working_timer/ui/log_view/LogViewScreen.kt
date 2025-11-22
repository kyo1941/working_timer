package com.example.working_timer.ui.log_view

import android.content.Intent
import android.view.LayoutInflater
import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.working_timer.ui.components.DateRangePickerModal
import com.example.working_timer.R
import com.example.working_timer.ui.components.WorkItemComposable
import java.text.NumberFormat
import com.example.working_timer.util.BorderColor
import com.example.working_timer.util.ButtonBackgroundColor
import com.example.working_timer.data.db.Work
import java.text.SimpleDateFormat
import java.util.*

data class LogViewScreenState(
    val uiState: LogViewUiState,
    val showDateRangePicker: Boolean
)

data class LogViewScreenActions(
    val onNavigateToTimer: () -> Unit,
    val onNavigateToEditWork: (Int, String, Boolean) -> Unit,
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
    onNavigateToEditWork: (Int, String, Boolean) -> Unit
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
    // Date formatter for calendar updates
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.calender_view, null)
                val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    actions.onDateSelected(year, month, dayOfMonth)
                }
                view
            },
            update = { view ->
                val calendarView = view as CalendarView
                val dateMillis =
                    if (state.uiState.selectedDay.isNotEmpty()) sdf.parse(state.uiState.selectedDay)?.time else null
                if (dateMillis != null) {
                    calendarView.date = dateMillis
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(
            color = BorderColor,
            thickness = 1.dp
        )

        LazyColumn(modifier = Modifier.weight(0.8f)) {
            itemsIndexed(state.uiState.workList) { index, work ->
                WorkItemComposable(
                    work = work,
                    onDelete = { actions.onShowDeleteDialog(work) },
                    onEdit = {
                        actions.onNavigateToEditWork(
                            work.id,
                            work.start_day,
                            false
                        )
                    }
                )
                if (index < state.uiState.workList.lastIndex) {
                    HorizontalDivider(
                        color = BorderColor,
                        thickness = 1.dp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    actions.onNavigateToEditWork(
                        0,
                        state.uiState.selectedDay,
                        true
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                containerColor = ButtonBackgroundColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Add,
                    modifier = Modifier
                        .height(24.dp)
                        .width(24.dp),
                    contentDescription = stringResource(id = R.string.log_view_add_button_description)
                )
            }
            FloatingActionButton(
                onClick = actions.onShowDateRangePicker,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                containerColor = ButtonBackgroundColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.CurrencyYen,
                    contentDescription = stringResource(id = R.string.log_view_calculate_salary_button_description)
                )
            }
        }
    }

    // 削除ダイアログ
    if (state.uiState.showDeleteDialog && state.uiState.workToDelete != null) {
        AlertDialog(
            onDismissRequest = actions.onHideDeleteDialog,
            title = { Text(stringResource(id = R.string.log_view_delete_dialog_title)) },
            text = { Text(stringResource(id = R.string.log_view_delete_dialog_message)) },
            confirmButton = {
                Row {
                    Spacer(modifier = Modifier.weight(0.1f))

                    TextButton(
                        onClick = actions.onHideDeleteDialog
                    ) { Text(stringResource(id = R.string.log_view_delete_dialog_no_button)) }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = { actions.onDeleteWork(state.uiState.workToDelete) }
                    ) { Text(stringResource(id = R.string.log_view_delete_dialog_yes_button)) }

                    Spacer(modifier = Modifier.weight(0.1f))
                }

            },
        )
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
        DateRangePickerModal(
            onDateRangeSelected = { pair ->
                val (startDate, endDate) = pair
                actions.onDateRangeSelected(startDate, endDate)
            },
            onDismiss = actions.onHideDateRangePicker
        )
    }
}

@Composable
fun SumDialog(
    startDate: Long?,
    endDate: Long?,
    totalHours: Long,
    totalMinutes: Long,
    totalWage: Long,
    calculationMode: TimeCalculationMode,
    onDismiss: () -> Unit,
    onWageChange: (Long) -> Unit,
    onCalculationModeChange: (TimeCalculationMode) -> Unit
) {
    var wage by remember { mutableStateOf(0L) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        wage = 0L
        onWageChange(wage)
    }

    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val formattedStartDate =
        remember(startDate) { if (startDate != null) sdf.format(startDate) else "N/A" }
    val formattedEndDate = remember(endDate) { if (endDate != null) sdf.format(endDate) else "N/A" }

    val calculationModes = remember {
        TimeCalculationMode.entries.map {
            when (it) {
                TimeCalculationMode.NORMAL -> context.getString(R.string.log_view_time_calculation_mode_normal)
                TimeCalculationMode.ROUND_UP -> context.getString(R.string.log_view_time_calculation_mode_round_up)
                TimeCalculationMode.ROUND_DOWN -> context.getString(R.string.log_view_time_calculation_mode_round_down)
            }
        }
    }
    val selectedModeIndex = calculationMode.ordinal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.log_view_sum_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(id = R.string.log_view_sum_dialog_period, formattedStartDate, formattedEndDate),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = R.string.log_view_sum_dialog_total_work_time, totalHours, totalMinutes),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(id = R.string.log_view_sum_dialog_salary, NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = if (wage == 0L) "" else wage.toString(),
                    textStyle = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    ),
                    onValueChange = {
                        wage = it.toLongOrNull() ?: 0L
                        onWageChange(wage)
                    },
                    label = { Text(stringResource(id = R.string.log_view_sum_dialog_hourly_wage_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedControl(
                    items = calculationModes,
                    selectedIndex = selectedModeIndex,
                    onSelectionChange = { index ->
                        val mode = TimeCalculationMode.entries[index]
                        onCalculationModeChange(mode)
                        if (index != selectedModeIndex) {
                            onWageChange(wage)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val lines = listOf(
                        context.getString(R.string.log_view_share_period, formattedStartDate, formattedEndDate),
                        context.getString(R.string.log_view_share_hourly_wage, wage.toString()),
                        context.getString(R.string.log_view_share_total_work_time, totalHours, totalMinutes),
                        context.getString(R.string.log_view_share_salary, NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage))
                    )
                    val shareText = lines.joinToString("\n")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.log_view_share_subject)))
                }) { Text(stringResource(id = R.string.log_view_sum_dialog_share_button)) }
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.log_view_sum_dialog_close_button)) }
            }

        }
    )
}


@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    ),
                    onClick = { onSelectionChange(index) }
                ) {
                    Text(
                        text = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun LogViewScreenPreviewEmpty() {
    val emptyUiState = LogViewUiState(
        selectedDay = "2025-01-02",
        workList = emptyList(),
        showDeleteDialog = false,
        workToDelete = null,
        showSumDialog = false,
        sumStartDate = null,
        sumEndDate = null,
        totalHours = 0L,
        totalMinutes = 0L,
        totalWage = 0L,
        timeCalculationMode = TimeCalculationMode.NORMAL
    )

    LogViewScreen(
        state = LogViewScreenState(
            uiState = emptyUiState,
            showDateRangePicker = false
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = {},
            onNavigateToEditWork = { _, _, _ -> },
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
    )
}

@Preview(showBackground = true, name = "With Work List")
@Composable
fun LogViewScreenPreviewWithWorkList() {
    val sampleWorkList = listOf(
        Work(
            id = 1,
            start_day = "2025-01-02",
            end_day = "2025-01-02",
            start_time = "09:00",
            end_time = "17:00",
            elapsed_time = 4800
        ),
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

    val uiStateWithWork = LogViewUiState(
        selectedDay = "2025-01-02",
        workList = sampleWorkList,
        showDeleteDialog = false,
        workToDelete = null,
        showSumDialog = false,
        sumStartDate = null,
        sumEndDate = null,
        totalHours = 0L,
        totalMinutes = 0L,
        totalWage = 0L,
        timeCalculationMode = TimeCalculationMode.NORMAL
    )

    LogViewScreen(
        state = LogViewScreenState(
            uiState = uiStateWithWork,
            showDateRangePicker = false
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = {},
            onNavigateToEditWork = { _, _, _ -> },
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
    )
}

@Preview(showBackground = true, name = "Delete Dialog")
@Composable
fun LogViewScreenPreviewDeleteDialog() {
    val workToDelete = Work(
        id = 1,
        start_day = "2025-01-02",
        end_day = "2025-01-02",
        start_time = "09:00",
        end_time = "17:00",
        elapsed_time = 480
    )

    val uiStateWithDeleteDialog = LogViewUiState(
        selectedDay = "2025-01-02",
        workList = listOf(workToDelete),
        showDeleteDialog = true,
        workToDelete = workToDelete,
        showSumDialog = false,
        sumStartDate = null,
        sumEndDate = null,
        totalHours = 0L,
        totalMinutes = 0L,
        totalWage = 0L,
        timeCalculationMode = TimeCalculationMode.NORMAL
    )

    LogViewScreen(
        state = LogViewScreenState(
            uiState = uiStateWithDeleteDialog,
            showDateRangePicker = false
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = {},
            onNavigateToEditWork = { _, _, _ -> },
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
    )
}

@Preview(showBackground = true, name = "Sum Dialog")
@Composable
fun LogViewScreenPreviewSumDialog() {
    val uiStateWithSumDialog = LogViewUiState(
        selectedDay = "2025-01-02",
        workList = emptyList(),
        showDeleteDialog = false,
        workToDelete = null,
        showSumDialog = true,
        sumStartDate = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L, // 1週間前
        sumEndDate = System.currentTimeMillis(),
        totalHours = 40L,
        totalMinutes = 30L,
        totalWage = 40500L,
        timeCalculationMode = TimeCalculationMode.NORMAL
    )

    LogViewScreen(
        state = LogViewScreenState(
            uiState = uiStateWithSumDialog,
            showDateRangePicker = false
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = {},
            onNavigateToEditWork = { _, _, _ -> },
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
    )
}

@Preview(showBackground = true, name = "Date Range Picker")
@Composable
fun LogViewScreenPreviewDateRangePicker() {
    val uiState = LogViewUiState(
        selectedDay = "2025-01-02",
        workList = emptyList(),
        showDeleteDialog = false,
        workToDelete = null,
        showSumDialog = false,
        sumStartDate = null,
        sumEndDate = null,
        totalHours = 0L,
        totalMinutes = 0L,
        totalWage = 0L,
        timeCalculationMode = TimeCalculationMode.NORMAL
    )

    LogViewScreen(
        state = LogViewScreenState(
            uiState = uiState,
            showDateRangePicker = true
        ),
        actions = LogViewScreenActions(
            onNavigateToTimer = {},
            onNavigateToEditWork = { _, _, _ -> },
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
    )
}
