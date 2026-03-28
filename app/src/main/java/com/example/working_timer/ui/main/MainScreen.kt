package com.example.working_timer.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.working_timer.R
import kotlinx.coroutines.launch

data class MainScreenState(
    val uiState: MainUiState
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
    snackbarHostState: SnackbarHostState,
    onNavigateToLog: () -> Unit
) {
    val uiState by mainViewModel.uiState.collectAsState()

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
            uiState = uiState
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
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        TimerStatusSection(state.uiState.timerStatus)

        Spacer(modifier = Modifier.height(32.dp))

        TimerDisplaySection(state.uiState.displayText)

        Spacer(Modifier.weight(1f))

        TimerControlButtons(
            timerStatus = state.uiState.timerStatus,
            onStartTimer = actions.onStartTimer,
            onStopTimer = actions.onStopTimer,
            onPauseTimer = actions.onPauseTimer,
            onResumeTimer = actions.onResumeTimer
        )

        Spacer(Modifier.weight(1f))
    }

    state.uiState.dialogStatus?.let {
        MainScreenDialogs(
            dialogStatus = it,
            onSaveWork = actions.onSaveWork,
            onResumeTimer = actions.onResumeTimer,
            onDiscardWork = actions.onDiscardWork,
            onDismissSaveDialog = actions.onDismissSaveDialog
        )
    }
}

private class MainUiStateProvider : PreviewParameterProvider<MainUiState> {
    override val values = sequenceOf(
        MainUiState(timerStatus = null),
        MainUiState(timerStatus = TimerStatus.Working, elapsedTime = 5025000L),
        MainUiState(timerStatus = TimerStatus.Resting, elapsedTime = 754000L),
        MainUiState(
            timerStatus = TimerStatus.Working,
            elapsedTime = 5025000L,
            dialogStatus = DialogStatus.SaveDialog(startDate = "2025-09-02", elapsedTime = 5025000L)
        ),
        MainUiState(timerStatus = TimerStatus.Working, dialogStatus = DialogStatus.TooShortTimeErrorDialog),
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview(@PreviewParameter(MainUiStateProvider::class) uiState: MainUiState) = MainScreen(
    state = MainScreenState(uiState = uiState),
    actions = previewMainScreenActions
)

@Preview(showBackground = true, device = "spec:parent=pixel_5,orientation=landscape")
@Composable
fun MainScreenPreviewLandscape(@PreviewParameter(MainUiStateProvider::class) uiState: MainUiState) = MainScreen(
    state = MainScreenState(uiState = uiState),
    actions = previewMainScreenActions
)

private val previewMainScreenActions = MainScreenActions(
    onNavigateToLog = {},
    onStartTimer = {},
    onStopTimer = {},
    onPauseTimer = {},
    onResumeTimer = {},
    onDiscardWork = {},
    onSaveWork = {},
    onDismissSaveDialog = {}
)
