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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.example.working_timer.ui.components.FooterNavigationBar
import com.example.working_timer.util.PauseButtonColor
import com.example.working_timer.util.ResumeButtonColor
import com.example.working_timer.util.StartButtonColor
import com.example.working_timer.util.StatusDefaultColor
import com.example.working_timer.util.StatusPauseColor
import com.example.working_timer.util.StatusWorkingColor
import com.example.working_timer.util.StopButtonColor
import kotlinx.coroutines.launch

data class MainScreenState(
    val uiState: TimerUiState,
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
        )
    )
}

@Composable
fun MainScreen(
    state: MainScreenState,
    actions: MainScreenActions
) {
    Scaffold(
        snackbarHost = { SnackbarHost(state.snackbarHostState) },
        bottomBar = {
            FooterNavigationBar(
                selectedIndex = 0,
                onTimerClick = {},
                onLogClick = actions.onNavigateToLog
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            Text(
                text = state.uiState.status,
                textAlign = TextAlign.Center,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when (state.uiState.status) {
                    "労働中" -> StatusWorkingColor
                    "休憩中" -> StatusPauseColor
                    else -> StatusDefaultColor
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = state.uiState.timerText,
                textAlign = TextAlign.Center,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            // 開始ボタン
            if (!state.uiState.isTimerRunning && !state.uiState.isPaused) {
                Button(
                    onClick = actions.onStartTimer,
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StartButtonColor)
                ) {
                    Text("開始", fontSize = 20.sp, color = Color.White)
                }
            }

            // 終了、休憩ボタン
            if (state.uiState.isTimerRunning) {
                Row(
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
                        Text("終了", fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = actions.onPauseTimer,
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PauseButtonColor)
                    ) {
                        Text("休憩", fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(Modifier.weight(1f))
                }
            }

            // 再開ボタン
            if (state.uiState.isPaused) {
                Button(
                    onClick = actions.onResumeTimer,
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ResumeButtonColor)
                ) {
                    Text("再開", fontSize = 20.sp, color = Color.White)
                }
            }

            Spacer(Modifier.weight(1f))
        }

        // 保存確認ダイアログ
        if (state.uiState.showSaveDialog) {
            AlertDialog(
                onDismissRequest = actions.onDismissSaveDialog,
                title = { Text(
                    text = "確認",
                    style = typography.headlineSmall,
                ) },
                text = { Text(
                    state.uiState.dialogMessage,
                    style = typography.bodyMedium,
                ) },
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

@Preview(showBackground = true, name = "BeforeStart")
@Composable
fun MainScreenPreviewBeforeStart() {
    val state = TimerUiState(
        status = "",
        timerText = "00:00",
        isTimerRunning = false,
        isPaused = false,
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
    val state = TimerUiState(
        status = "労働中",
        timerText = "00:12:34",
        isTimerRunning = true,
        isPaused = false
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
    val state = TimerUiState(
        status = "休憩中",
        timerText = "05:00",
        isTimerRunning = false,
        isPaused = true
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
    val sampleState = TimerUiState(
        status = "労働中",
        timerText = "01:23:45",
        isTimerRunning = true,
        isPaused = false,
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
    val state = TimerUiState(
        status = "労働中",
        timerText = "00:00",
        isTimerRunning = true,
        isPaused = false,
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
