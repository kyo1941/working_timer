package com.example.working_timer.ui.log_view

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.widget.CalendarView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.working_timer.ui.components.DateRangePickerModal
import com.example.working_timer.ui.edit_work.EditWorkActivity
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
    viewModel: LogViewViewModel = viewModel(),
    onNavigateToTimer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // EditWorkActivityから結果を受け取るためのランチャー
    val editWorkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.loadWorkList(uiState.selectedDay)
            }
        }
    )

    // Date Range Pickerの表示を制御するState
    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.calender_view, null)
                val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
                calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    val selected = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth)
                    viewModel.loadWorkList(selected)
                }
                view
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
                        val intent = Intent(context, EditWorkActivity::class.java).apply {
                            putExtra("is_new", false)
                            putExtra("id", work.id)
                            putExtra("start_day", work.start_day)
                            putExtra("end_day", work.end_day)
                            putExtra("start_time", work.start_time)
                            putExtra("end_time", work.end_time)
                            putExtra("elapsed_time", work.elapsed_time)
                        }
                        editWorkLauncher.launch(intent)
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
                    val intent = Intent(context, EditWorkActivity::class.java).apply {
                        putExtra("is_new", true)
                        putExtra("start_day", uiState.selectedDay)
                        putExtra("end_day", uiState.selectedDay)
                    }
                    editWorkLauncher.launch(intent)
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

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val formattedStartDate =
        remember(startDate) { if (startDate != null) sdf.format(startDate) else "N/A" }
    val formattedEndDate = remember(endDate) { if (endDate != null) sdf.format(endDate) else "N/A" }

    val calculationModes = listOf("通常", "繰り上げ", "繰り下げ")
    val selectedModeIndex = when(calculationMode) {
        TimeCalculationMode.NORMAL -> 0
        TimeCalculationMode.ROUND_UP -> 1
        TimeCalculationMode.ROUND_DOWN -> 2
    }

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
                    onValueChange = {
                        wage = it.toLongOrNull() ?: 0L
                        onWageChange(wage)
                    },
                    label = { Text("時給") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedControl(
                    items = calculationModes,
                    selectedIndex = selectedModeIndex,
                    onSelectionChange = { index ->
                        val mode = when (index) {
                            0 -> TimeCalculationMode.NORMAL
                            1 -> TimeCalculationMode.ROUND_UP
                            2 -> TimeCalculationMode.ROUND_DOWN
                            else -> TimeCalculationMode.NORMAL
                        }
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
                        putExtra(Intent.EXTRA_TEXT, "$shareText")
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