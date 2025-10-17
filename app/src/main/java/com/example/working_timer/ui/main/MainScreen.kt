package com.example.working_timer.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.working_timer.R
import com.example.working_timer.util.PauseButtonColor
import com.example.working_timer.util.ResumeButtonColor
import com.example.working_timer.util.StartButtonColor
import com.example.working_timer.util.StatusPauseColor
import com.example.working_timer.util.StatusWorkingColor
import com.example.working_timer.util.StopButtonColor
import kotlinx.coroutines.launch

data class MainScreenState(
    val uiState: MainUiState,
    val snackbarHostState: SnackbarHostState
)

data class MainScreenActions(
    val onNavigateToLog: () -> Unit,
    val onStartTimer: () -> Unit,
    val onStopTimer: () -> Unit,
    val onPauseTimer: () -> Unit,
    val onResumeTimer: () -> Unit,
    val onDiscardWork: () -> Unit,
    val onSaveWork: () -> Unit,
    val onDismissSaveDialog: () -> Unit
)

@Composable
fun MainScreenHolder(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
    onNavigateToLog: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ViewModelからのsnackbarMessage監視
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mainViewModel.clearSnackbarMessage()
        }
    }

    // ナビゲーション処理はステートフルで管理
    LaunchedEffect(uiState.navigateToLog) {
        if (uiState.navigateToLog) {
            onNavigateToLog()
            mainViewModel.onNavigationHandled()
        }
    }

    // 通知権限ランチャー
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        scope.launch {
            val message = if (isGranted) {
                "通知が許可されました。"
            } else {
                "通知が拒否されました。"
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    MainScreen(
        state = MainScreenState(
            uiState = uiState,
            snackbarHostState = snackbarHostState
        ),
        actions = MainScreenActions(
            onNavigateToLog = onNavigateToLog,
            onStartTimer = {
                mainViewModel.startTimer()
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (!hasPermission) {
                    scope.launch {
                        snackbarHostState.showSnackbar("通知をONにすると、タイマーの進行状況が確認できます。")
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            onStopTimer = { mainViewModel.stopTimer() },
            onPauseTimer = { mainViewModel.pauseTimer() },
            onResumeTimer = { mainViewModel.resumeTimer() },
            onDiscardWork = { mainViewModel.discardWork() },
            onSaveWork = { mainViewModel.saveWork() },
            onDismissSaveDialog = { mainViewModel.dismissSaveDialog() }
        ),
        modifier = modifier
    )
}

@Composable
fun MainScreen(
    state: MainScreenState,
    actions: MainScreenActions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(state.snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            state.uiState.timerStatus?.let {
                Text(
                    text = when(state.uiState.timerStatus) {
                        TimerStatus.WORKING -> stringResource(R.string.working_status)
                        TimerStatus.RESTING -> stringResource(R.string.resting_status)
                    },
                    textAlign = TextAlign.Center,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (state.uiState.timerStatus) {
                        TimerStatus.WORKING -> StatusWorkingColor
                        TimerStatus.RESTING -> StatusPauseColor
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = formatElapsedTime(state.uiState.elapsedTime),
                textAlign = TextAlign.Center,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            when (state.uiState.timerStatus) {
                TimerStatus.WORKING -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = actions.onStopTimer,
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StopButtonColor)
                    ) {
                        Text(stringResource(R.string.stop_timer_button_text), fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = actions.onPauseTimer,
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PauseButtonColor)
                    ) {
                        Text(stringResource(R.string.pause_timer_button_text), fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(Modifier.weight(1f))
                }

                TimerStatus.RESTING -> Button(
                    onClick = actions.onResumeTimer,
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResumeButtonColor)
                ) {
                    Text(stringResource(R.string.resume_timer_button_text), fontSize = 20.sp, color = Color.White)
                }

                null -> Button(
                    onClick = actions.onStartTimer,
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StartButtonColor)
                ) {
                    Text(stringResource(R.string.start_timer_button_text), fontSize = 20.sp, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))
        }

        // 保存確認ダイアログ
        if (state.uiState.showSaveDialog) {
            AlertDialog(
                onDismissRequest = actions.onDismissSaveDialog,
                title = {
                    Text(
                        text = "確認",
                        style = typography.headlineSmall,
                    )
                },
                text = {
                    Text(
                        state.uiState.dialogMessage,
                        style = typography.bodyMedium,
                    )
                },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    // ダイアログのメッセージによってボタンの挙動を変える
                    if (state.uiState.isErrorDialog) {
                        Row {
                            Spacer(modifier = Modifier.weight(0.1f))
                            TextButton(onClick = {
                                actions.onDiscardWork()
                                actions.onDismissSaveDialog()
                            }) {
                                Text("破棄")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(onClick = {
                                actions.onResumeTimer()
                                actions.onDismissSaveDialog()
                            }) {
                                Text("再開")
                            }

                            Spacer(modifier = Modifier.weight(0.1f))
                        }
                    } else {
                        Row {
                            TextButton(onClick = {
                                actions.onDiscardWork()
                                actions.onDismissSaveDialog()
                            }) {
                                Text("破棄")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(onClick = {
                                actions.onResumeTimer()
                                actions.onDismissSaveDialog()
                            }) {
                                Text("再開")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            TextButton(onClick = actions.onSaveWork) {
                                Text("保存")
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun formatElapsedTime(elapsedTime: Long): String {
    val totalSeconds = elapsedTime / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        stringResource(R.string.elapsed_time_hms, hours, minutes, seconds)
    } else {
        stringResource(R.string.elapsed_time_ms, minutes, seconds)
    }
}

@Preview(showBackground = true, name = "BeforeStart")
@Composable
fun MainScreenPreviewBeforeStart() {
    val state = MainUiState(
        timerStatus = null,
        showSaveDialog = false
    )
    MainScreen(
        state = MainScreenState(
            uiState = state,
            snackbarHostState = remember { SnackbarHostState() }
        ),
        actions = MainScreenActions(
            onNavigateToLog = {}, onStartTimer = {}, onStopTimer = {}, onPauseTimer = {},
            onResumeTimer = {}, onDiscardWork = {}, onSaveWork = {}, onDismissSaveDialog = {}
        )
    )
}

@Preview(showBackground = true, name = "Working")
@Composable
fun MainScreenPreviewWorking() {
    val state = MainUiState(
        timerStatus = TimerStatus.WORKING,
        elapsedTime = 5025000L,
    )
    MainScreen(
        state = MainScreenState(
            uiState = state,
            snackbarHostState = remember { SnackbarHostState() }
        ),
        actions = MainScreenActions(
            onNavigateToLog = {}, onStartTimer = {}, onStopTimer = {}, onPauseTimer = {},
            onResumeTimer = {}, onDiscardWork = {}, onSaveWork = {}, onDismissSaveDialog = {}
        )
    )
}

@Preview(showBackground = true, name = "Paused")
@Composable
fun MainScreenPreviewPaused() {
    val state = MainUiState(
        timerStatus = TimerStatus.RESTING,
        elapsedTime = 754000L,
    )
    MainScreen(
        state = MainScreenState(
            uiState = state,
            snackbarHostState = remember { SnackbarHostState() }
        ),
        actions = MainScreenActions(
            onNavigateToLog = {}, onStartTimer = {}, onStopTimer = {}, onPauseTimer = {},
            onResumeTimer = {}, onDiscardWork = {}, onSaveWork = {}, onDismissSaveDialog = {}
        )
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreviewSaveDialog() {
    val sampleState = MainUiState(
        timerStatus = TimerStatus.WORKING,
        elapsedTime = 5025000L,
        showSaveDialog = true,
        dialogMessage = """
            開始日 ： 2025-09-02
            経過時間 ： 1時間 23分

            今回の作業記録を保存しますか？
        """.trimIndent(),
        isErrorDialog = false
    )

    MainScreen(
        state = MainScreenState(
            uiState = sampleState,
            snackbarHostState = remember { SnackbarHostState() }
        ),
        actions = MainScreenActions(
            onNavigateToLog = {}, onStartTimer = {}, onStopTimer = {}, onPauseTimer = {},
            onResumeTimer = {}, onDiscardWork = {}, onSaveWork = {}, onDismissSaveDialog = {}
        )
    )
}

@Preview(showBackground = true, name = "SaveDialog_Error")
@Composable
fun MainScreenPreviewSaveDialogError() {
    val state = MainUiState(
        timerStatus = TimerStatus.WORKING,
        showSaveDialog = true,
        dialogMessage = "1分未満の作業は保存できません。再開または破棄を選択してください。",
        isErrorDialog = true
    )
    MainScreen(
        state = MainScreenState(
            uiState = state,
            snackbarHostState = remember { SnackbarHostState() }
        ),
        actions = MainScreenActions(
            onNavigateToLog = {}, onStartTimer = {}, onStopTimer = {}, onPauseTimer = {},
            onResumeTimer = {}, onDiscardWork = {}, onSaveWork = {}, onDismissSaveDialog = {}
        )
    )
}
