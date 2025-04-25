package com.example.working_timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight


@Composable
fun EditWorkScreen(
    id: Int,
    day: String,
    startTime: String,
    endTime: String,
    elapsedTime: Int,
    onSave: (String, String, Int) -> Unit
) {
    var start by remember { mutableStateOf(startTime) }
    var end by remember { mutableStateOf(endTime) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append("開始時刻  ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(start)
                    }
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = { showStartPicker = true }) {
                Text("入力")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    append("終了時刻  ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(end)
                    }
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = { showEndPicker = true }) {
                Text("入力")
            }
        }

        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    (context as? android.app.Activity)?.finish()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("キャンセル")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    val newElapsed = calculateElapsed(start, end)
                    onSave("$start:00", "$end:00", newElapsed)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("保存")
            }
        }
    }

    if (showStartPicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(start),
            onDismiss = { showStartPicker = false },
            onTimeSelected = {
                start = it
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        MaterialTimePickerDialog(
            initialTime = parseTime(end),
            onDismiss = { showEndPicker = false },
            onTimeSelected = {
                end = it
                showEndPicker = false
            }
        )
    }
}

fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":").mapNotNull { it.toIntOrNull() }
    return if (parts.size == 2) Pair(parts[0], parts[1]) else Pair(0, 0)
}

fun calculateElapsed(start: String, end: String): Int {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startDate = format.parse(start)
    val endDate = format.parse(end)
    val diffMillis = endDate.time - startDate.time
    return (diffMillis / 1000).toInt().coerceAtLeast(0)
}

