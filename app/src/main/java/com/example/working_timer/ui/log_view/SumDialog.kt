package com.example.working_timer.ui.log_view

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.working_timer.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun SumDialog(
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
                    stringResource(
                        id = R.string.log_view_sum_dialog_period,
                        formattedStartDate,
                        formattedEndDate
                    ),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(
                        id = R.string.log_view_sum_dialog_total_work_time,
                        totalHours,
                        totalMinutes
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(
                        id = R.string.log_view_sum_dialog_salary,
                        NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)
                    ),
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
                    onValueChange = { input ->
                        // Empty の時にのみ 0 にフォールバックされる
                        wage = input.filter { it.isDigit() }.take(7).toLongOrNull() ?: 0L
                        onWageChange(wage)
                    },
                    label = { Text(stringResource(id = R.string.log_view_sum_dialog_hourly_wage_label)) },
                    singleLine = true,
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
                        context.getString(
                            R.string.log_view_share_period,
                            formattedStartDate,
                            formattedEndDate
                        ),
                        context.getString(R.string.log_view_share_hourly_wage, wage.toString()),
                        context.getString(
                            R.string.log_view_share_total_work_time,
                            totalHours,
                            totalMinutes
                        ),
                        context.getString(
                            R.string.log_view_share_salary,
                            NumberFormat.getNumberInstance(Locale.JAPAN).format(totalWage)
                        )
                    )
                    val shareText = lines.joinToString("\n")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.log_view_share_subject)
                        )
                    )
                }) { Text(stringResource(id = R.string.log_view_sum_dialog_share_button)) }
                TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.log_view_sum_dialog_close_button)) }
            }
        }
    )
}
