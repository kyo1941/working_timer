package com.example.working_timer.ui.edit_work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
internal fun DateTimeSection(
    label: String,
    day: String,
    time: String,
    onShowDayPicker: () -> Unit,
    onShowTimePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Start
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onShowDayPicker) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(formatMonthDay(day))
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
            Spacer(modifier = Modifier.width(32.dp))
            TextButton(onClick = onShowTimePicker) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(time)
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        textDecoration = TextDecoration.Underline,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
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
