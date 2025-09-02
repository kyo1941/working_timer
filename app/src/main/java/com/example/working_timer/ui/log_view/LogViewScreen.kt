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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.working_timer.ui.components.DateRangePickerModal
import com.example.working_timer.ui.components.FooterNavigationBar
import com.example.working_timer.R
import com.example.working_timer.ui.components.WorkItemComposable
import java.text.NumberFormat
import com.example.working_timer.util.BorderColor
import com.example.working_timer.util.ButtonBackgroundColor

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreen(
    viewModel: LogViewViewModel = hiltViewModel(),
    onNavigateToTimer: () -> Unit,
    onNavigateToEditWork: (Int, String, Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Date formatter for calendar updates
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Date Range Pickerの表示を制御するState
    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {

        if (uiState.selectedDay.isNotEmpty()) {
            viewModel.loadWorkList(uiState.selectedDay)
        } else {
            viewModel.init()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.calender_view, null)
                val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    viewModel.setSelectedDay(year, month, dayOfMonth)
                }
                view
            },
            update = { view ->
                val calendarView = view as CalendarView
                val dateMillis = if (uiState.selectedDay.isNotEmpty()) sdf.parse(uiState.selectedDay)?.time else null
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
            itemsIndexed(uiState.workList) { index, work ->
                WorkItemComposable(
                    work = work,
                    onDelete = { viewModel.showDeleteDialog(work) },
                    onEdit = {
                        onNavigateToEditWork(
                            work.id,
                            work.start_day,
                            false
                        )
                    }
                )
                if (index < uiState.workList.lastIndex) {
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
                    onNavigateToEditWork(
                        0,
                        uiState.selectedDay,
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
                    contentDescription = "追加"
                )
            }
            FloatingActionButton(
                onClick = { showDateRangePicker = true },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                containerColor = ButtonBackgroundColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.CurrencyYen,
                    contentDescription = "給料計算"
                )
            }
        }
        // 下部ナビゲーションバー
        FooterNavigationBar(
            selectedIndex = 1,
            onTimerClick = onNavigateToTimer,
            onLogClick = {}
        )
    }

    // 削除ダイアログ
    if (uiState.showDeleteDialog && uiState.workToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("確認") },
            text = { Text("本当にこの記録を削除しますか？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWork(uiState.workToDelete!!) }) { Text("はい") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) { Text("いいえ") }
            }
        )
    }

    // 集計ダイアログ
    if (uiState.showSumDialog) {
        SumDialog(
            startDate = uiState.sumStartDate,
            endDate = uiState.sumEndDate,
            totalHours = uiState.totalHours,
            totalMinutes = uiState.totalMinutes,
            totalWage = uiState.totalWage,
            calculationMode = uiState.timeCalculationMode,
            onDismiss = { viewModel.hideSumDialog() },
            onWageChange = { viewModel.updateTotalWage(it) },
            onCalculationModeChange = { viewModel.setTimeCalculationMode(it) }
        )
    }

    // 日付範囲選択ダイアログ
    if (showDateRangePicker) {
        DateRangePickerModal(
            onDateRangeSelected = { pair ->
                val (startDate, endDate) = pair
                if (startDate != null && endDate != null) {
                    viewModel.showSumDialog(startDate, endDate)
                }
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
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
                TimeCalculationMode.NORMAL -> "通常"
                TimeCalculationMode.ROUND_UP -> "繰り上げ"
                TimeCalculationMode.ROUND_DOWN -> "繰り下げ"
            }
        }
    }
    val selectedModeIndex = calculationMode.ordinal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("計算結果") },
        text = {
            Column {
                Text(
                    "期間: ${formattedStartDate} ~ ${formattedEndDate}",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "合計勤務時間: ${totalHours}時間 ${totalMinutes}分",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "給料: ${
                        NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)
                    } 円",
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
                    label = { Text("時給") },
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
                        "期間 ${formattedStartDate} ~ ${formattedEndDate}",
                        "時給 ${wage} 円",
                        "合計 ${totalHours}時間 ${totalMinutes}分",
                        "給料 ${NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)} 円"
                    )
                    val shareText = lines.joinToString("\n")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "共有"))
                }) { Text("共有") }
                TextButton(onClick = onDismiss) { Text("閉じる") }
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