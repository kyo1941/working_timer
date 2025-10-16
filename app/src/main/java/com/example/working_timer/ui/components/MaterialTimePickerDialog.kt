package com.example.working_timer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialTimePickerDialog(
    initialTime: Pair<Int, Int>,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit,
    showToggleIcon: Boolean = true
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.first,
        initialMinute = initialTime.second,
        is24Hour = true
    )

    // 基本はPickerを初期値にするが，活動時間だけはInputにする
    var isInputMode by remember { mutableStateOf(!showToggleIcon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "時刻を選択",
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInputMode) {
                        TimeInput(state = state)
                    } else {
                        TimePicker(state = state)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showToggleIcon) {
                        // 左端：モード切り替えアイコン
                        IconButton(onClick = { isInputMode = !isInputMode }) {
                            Icon(
                                imageVector = if (isInputMode) Icons.Filled.AccessTime else Icons.Filled.Keyboard,
                                contentDescription = "モード切替"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    Row {
                        // 右側：キャンセルボタン
                        TextButton(onClick = onDismiss) {
                            Text("キャンセル")
                        }
                        // OKボタン
                        TextButton(onClick = {
                            onTimeSelected(String.format("%02d:%02d", state.hour, state.minute))
                        }) {
                            Text("OK")
                        }
                    }

                }
            }
        },
        confirmButton = {}, // カスタムするので空
        dismissButton = {}  // カスタムするので空
    )
}
