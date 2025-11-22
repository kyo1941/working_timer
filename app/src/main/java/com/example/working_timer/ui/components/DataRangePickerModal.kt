package com.example.working_timer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.working_timer.R
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
                Text(stringResource(id = R.string.date_range_picker_ok_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.date_range_picker_close_button))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {
                Text(
                    text = stringResource(id = R.string.date_range_picker_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    fontWeight = FontWeight.SemiBold
                )
            },
            headline = {
                val startDate =
                    formatMillisToDateString(dateRangePickerState.selectedStartDateMillis)
                val endDate = formatMillisToDateString(dateRangePickerState.selectedEndDateMillis)

                val startLabel = stringResource(id = R.string.date_range_picker_start_date_label)
                val endLabel = stringResource(id = R.string.date_range_picker_end_date_label)

                val displayText = when {
                    startDate.isNotEmpty() && endDate.isNotEmpty() -> stringResource(id = R.string.date_range_picker_headline, startDate, endDate)
                    startDate.isNotEmpty() -> stringResource(id = R.string.date_range_picker_headline, startDate, endLabel)
                    else -> stringResource(id = R.string.date_range_picker_headline, startLabel, endLabel)
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
                .heightIn(max = 500.dp)
        )
    }
}

@Preview(showBackground = true, name = "DateRangePickerModal")
@Composable
fun DateRangePickerModalPreview() {
    DateRangePickerModal(
        onDateRangeSelected = { },
        onDismiss = { }
    )
}
