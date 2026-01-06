package com.example.working_timer.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.FloatingActionButton
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
import com.example.working_timer.ui.components.SaveDialog
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
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        mainViewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(context.getString(R.string.error_save_prefix) + message)
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.navigateToLog.collect {
            onNavigateToLog()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        scope.launch {
            val message = if (isGranted) {
                context.getString(R.string.permission_is_granted)
            } else {
                context.getString(R.string.permission_is_denied)
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
                        snackbarHostState.showSnackbar(context.getString(R.string.explain_notification_permission))
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            onStopTimer = mainViewModel::stopTimer,
            onPauseTimer = mainViewModel::pauseTimer,
            onResumeTimer = mainViewModel::resumeTimer,
            onDiscardWork = mainViewModel::discardWork,
            onSaveWork = mainViewModel::saveWork,
            onDismissSaveDialog = mainViewModel::dismissSaveDialog
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
                    text = when (state.uiState.timerStatus) {
                        TimerStatus.Working -> stringResource(R.string.working_status)
                        TimerStatus.Resting -> stringResource(R.string.resting_status)
                    },
                    textAlign = TextAlign.Center,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (state.uiState.timerStatus) {
                        TimerStatus.Working -> StatusWorkingColor
                        TimerStatus.Resting -> StatusPauseColor
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                state.uiState.displayText.forEach { char ->
                    AnimatedContent(
                        targetState = char,
                        transitionSpec = {
                            slideInVertically { height -> height } togetherWith
                                    slideOutVertically { height -> -height }
                        }
                    ) { targetChar ->
                        Text(
                            text = targetChar.toString(),
                            textAlign = TextAlign.Center,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            when (state.uiState.timerStatus) {
                TimerStatus.Working -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(Modifier.weight(1f))

                    TimerButton(
                        text = stringResource(R.string.stop_timer_button_text),
                        color = StopButtonColor,
                        onClick = actions.onStopTimer
                    )

                    Spacer(Modifier.weight(1f))

                    TimerButton(
                        text = stringResource(R.string.pause_timer_button_text),
                        color = PauseButtonColor,
                        onClick = actions.onPauseTimer
                    )

                    Spacer(Modifier.weight(1f))
                }

                TimerStatus.Resting -> TimerButton(
                    text = stringResource(R.string.resume_timer_button_text),
                    color = ResumeButtonColor,
                    onClick = actions.onResumeTimer
                )

                null -> TimerButton(
                    text = stringResource(R.string.start_timer_button_text),
                    color = StartButtonColor,
                    onClick = actions.onStartTimer
                )
            }

            Spacer(Modifier.weight(1f))
        }

        when (state.uiState.dialogStatus) {
            is DialogStatus.SaveDialog -> SaveDialog(
                startDate = state.uiState.dialogStatus.startDate,
                elapsedTime = state.uiState.dialogStatus.elapsedTime,
                onConfirm = actions.onSaveWork,
                onNeutral = {
                    actions.onResumeTimer()
                    actions.onDismissSaveDialog()
                },
                onDismiss = {
                    actions.onDiscardWork()
                    actions.onDismissSaveDialog()
                }
            )

            is DialogStatus.TooShortTimeErrorDialog -> ErrorAlertDialog(
                message = stringResource(R.string.error_time_too_short),
                onClick = {
                    actions.onResumeTimer()
                    actions.onDismissSaveDialog()
                },
                onDismiss = {
                    actions.onDiscardWork()
                    actions.onDismissSaveDialog()
                }
            )

            is DialogStatus.DataNotFoundErrorDialog -> ErrorAlertDialog(
                message = stringResource(R.string.error_data_not_found),
                onClick = {
                    actions.onResumeTimer()
                    actions.onDismissSaveDialog()
                },
                onDismiss = {
                    actions.onDiscardWork()
                    actions.onDismissSaveDialog()
                }
            )

            null -> {}
        }
    }
}

@Composable
private fun TimerButton(text: String, color: Color, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        shape = RoundedCornerShape(40.dp),
        modifier = Modifier.size(100.dp)
    ) {
        Text(text, fontSize = 20.sp, color = Color.White)
    }
}

@Composable
private fun ErrorAlertDialog(
    message: String,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = stringResource(R.string.title_save_dialog),
                style = typography.headlineSmall
            )
        },
        text = {
            Text(
                text = message,
                style = typography.bodyMedium
            )
        },
        properties = DialogProperties(dismissOnClickOutside = false),
        confirmButton = {
            TextButton(onClick = onClick) {
                Text(stringResource(R.string.resume_timer_button_text))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard_dialog_button_text))
            }
        }
    )
}

@Preview(showBackground = true, name = "BeforeStart")
@Composable
fun MainScreenPreviewBeforeStart() {
    val state = MainUiState(timerStatus = null)
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
        timerStatus = TimerStatus.Working,
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
        timerStatus = TimerStatus.Resting,
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
        timerStatus = TimerStatus.Working,
        elapsedTime = 5025000L,
        dialogStatus = DialogStatus.SaveDialog(
            startDate = "2025-09-02",
            elapsedTime = 5025000L
        )
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
        timerStatus = TimerStatus.Working,
        dialogStatus = DialogStatus.TooShortTimeErrorDialog
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
