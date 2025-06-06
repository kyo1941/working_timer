package com.example.working_timer

import android.view.LayoutInflater
import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import com.example.working_timer.data.Work

import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreen(
    viewModel: LogViewViewModel = viewModel(),
    onEditWork: (Work) -> Unit,
    onAddWork: (String) -> Unit,
    onNavigateToTimer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Date Range Pickerの表示を制御するState
    var showDateRangePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.init()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                val inflater = LayoutInflater.from(context)
                val view = inflater.inflate(R.layout.activity_log_view, null)
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
                    onEdit = { onEditWork(work) }
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
            FloatingActionButton(
                onClick = { onAddWork(uiState.selectedDay) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
                containerColor = ButtonBackgroundColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_add_24),
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
                    painter = painterResource(id = R.drawable.baseline_currency_yen_24),
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
            onDismiss = { viewModel.hideSumDialog() },
            onWageChange = { viewModel.updateTotalWage(it) }
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
    onDismiss: () -> Unit,
    onWageChange: (Long) -> Unit
) {
    var wage by remember { mutableStateOf(0L) }

    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val formattedStartDate =
        remember(startDate) { if (startDate != null) sdf.format(startDate) else "N/A" }
    val formattedEndDate = remember(endDate) { if (endDate != null) sdf.format(endDate) else "N/A" }

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
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "給料: ${
                        NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)
                    } 円",
                    fontWeight = FontWeight.SemiBold
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

            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("閉じる") }
        }
    )
}