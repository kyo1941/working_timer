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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.working_timer.ui.components.FooterNavigationBar
import com.example.working_timer.util.PauseButtonColor
import com.example.working_timer.util.ResumeButtonColor
import com.example.working_timer.util.StartButtonColor
import com.example.working_timer.util.StatusDefaultColor
import com.example.working_timer.util.StatusPauseColor
import com.example.working_timer.util.StatusWorkingColor
import com.example.working_timer.util.StopButtonColor
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = viewModel(),
    onNavigateToLog: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 通知権限ランチャー
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        scope.launch {
            if (isGranted) {
                snackbarHostState.showSnackbar("通知が許可されました。")
            } else {
                snackbarHostState.showSnackbar("通知が拒否されました。")
            }
        }
    }

    // 通知権限が許可されているか確認
    fun isNotificationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12以前は通知権限が不要
        }
    }

    LaunchedEffect(uiState.navigateToLog) {
        if (uiState.navigateToLog) {
            onNavigateToLog()
            mainViewModel.onNavigationHandled()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FooterNavigationBar(
                selectedIndex = 0,
                onTimerClick = {},
                onLogClick = onNavigateToLog
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
                text = uiState.status,
                textAlign = TextAlign.Center,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when (uiState.status) {
                    "労働中" -> StatusWorkingColor
                    "休憩中" -> StatusPauseColor
                    else -> StatusDefaultColor
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = uiState.timerText,
                textAlign = TextAlign.Center,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            // 開始ボタン
            if (!uiState.isTimerRunning && !uiState.isPaused) {
                Button(
                    onClick = {
                        mainViewModel.startTimer()
                        if (!isNotificationGranted()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("通知をONにすると，タイマーの進行状況が確認できます．")
                            }
                            // 通知権限が許可されていない場合はリクエスト
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StartButtonColor)
                ) {
                    Text("開始", fontSize = 20.sp, color = Color.White)
                }
            }

            // 終了、休憩ボタン
            if (uiState.isTimerRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { mainViewModel.stopTimer() },
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StopButtonColor)
                    ) {
                        Text("終了", fontSize = 20.sp, color = Color.White)
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = { mainViewModel.pauseTimer() },
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
            if (uiState.isPaused) {
                Button(
                    onClick = { mainViewModel.resumeTimer() },
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
        if (uiState.showSaveDialog) {
            AlertDialog(
                onDismissRequest = { mainViewModel.dismissSaveDialog() },
                title = { Text("確認") },
                text = { Text(
                    uiState.dialogMessage,
                    fontWeight = FontWeight.Medium
                ) },
                properties = DialogProperties(dismissOnClickOutside = false),
                confirmButton = {
                    // ダイアログのメッセージによってボタンの挙動を変える
                    if (uiState.isErrorDialog) {
                        Row {
                            TextButton(onClick = {
                                mainViewModel.discardWork()
                                mainViewModel.dismissSaveDialog()
                            }) {
                                Text("破棄")
                            }
                            TextButton(onClick = {
                                mainViewModel.resumeTimer()
                                mainViewModel.dismissSaveDialog()
                            }) {
                                Text("再開")
                            }
                        }
                    } else {
                        Row {
                            TextButton(onClick = {
                                mainViewModel.discardWork()
                                mainViewModel.dismissSaveDialog()
                            }) {
                                Text("破棄")
                            }

                            Spacer(modifier = Modifier.width(64.dp))

                            TextButton(onClick = {
                                mainViewModel.resumeTimer()
                                mainViewModel.dismissSaveDialog()
                            }) {
                                Text("再開")
                            }
                            TextButton(onClick = {
                                mainViewModel.saveWork()
                            }) {
                                Text("保存")
                            }
                        }
                    }
                }
            )
        }
    }
}
