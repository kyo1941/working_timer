package com.example.working_timer.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.text.SimpleDateFormat
import java.util.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerModal(
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    fun formatMillisToDateString(millis: Long?): String {
        return millis?.let {
            SimpleDateFormat("M月d日", Locale.JAPAN).format(Date(it))
        } ?: ""
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onDateRangeSelected(
                        Pair(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = "期間を選択",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    fontWeight = FontWeight.SemiBold
                )
            },
            headline = {
                val startDate = formatMillisToDateString(dateRangePickerState.selectedStartDateMillis)
                val endDate = formatMillisToDateString(dateRangePickerState.selectedEndDateMillis)

                val displayText = when {
                    startDate.isNotEmpty() && endDate.isNotEmpty() -> "$startDate - $endDate"
                    startDate.isNotEmpty() -> "$startDate - 終了日"
                    else -> "開始日 - 終了日"
                }

                Text(
                    text = displayText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        )
    }
}