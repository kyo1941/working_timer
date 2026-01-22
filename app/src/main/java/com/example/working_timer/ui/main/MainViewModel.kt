package com.example.working_timer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.service.TimerState
import com.example.working_timer.util.Constants.ONE_MINUTE_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface TimerStatus {
    object Working : TimerStatus
    object Resting : TimerStatus
}

sealed interface DialogStatus {
    data class SaveDialog(val startDate: String, val elapsedTime: Long) : DialogStatus
    object TooShortTimeErrorDialog : DialogStatus
    object DataNotFoundErrorDialog : DialogStatus
}

data class MainUiState(
    val timerStatus: TimerStatus? = null,
    val elapsedTime: Long = 0L,
    val dialogStatus: DialogStatus? = null,
) {
    private val totalSeconds = elapsedTime / 1000
    private val hours = totalSeconds / 3600
    private val minutes = (totalSeconds % 3600) / 60
    private val seconds = totalSeconds % 60

    val displayText = if (hours > 0) {
        String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    private val timerManager: TimerManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {
    private val dialogStatus = MutableStateFlow<DialogStatus?>(null)

    val uiState: StateFlow<MainUiState> = combine(
        timerManager.timerState,
        dialogStatus,
    ) { timerState: TimerState, dialog: DialogStatus? ->
        val timerStatus = when {
            timerState.isRunning -> TimerStatus.Working
            timerState.elapsedTime > 0L -> TimerStatus.Resting
            else -> null
        }

        MainUiState(
            timerStatus = timerStatus,
            elapsedTime = timerState.elapsedTime,
            dialogStatus = dialog,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: MutableSharedFlow<String> = _snackbarEvent

    private val _navigateToLog = MutableSharedFlow<Unit>()
    val navigateToLog: MutableSharedFlow<Unit> = _navigateToLog

    fun startTimer() {
        timerManager.startTimer()
    }

    fun pauseTimer() {
        timerManager.pauseTimer()
    }

    fun resumeTimer() {
        timerManager.resumeTimer()
    }

    fun stopTimer() {
        timerManager.pauseTimer()
        showSaveDialog()
    }

    private fun showSaveDialog() {
        viewModelScope.launch {
            val elapsedTime = timerManager.timerState.value.elapsedTime
            val startDate = dataStoreManager.getStartDateSync()
            val startTime = dataStoreManager.getStartTimeSync()

            if (startDate == null || startTime == null) {
                dialogStatus.update { DialogStatus.DataNotFoundErrorDialog }
                return@launch
            }

            dialogStatus.update {
                DialogStatus.SaveDialog(
                    startDate = startDate,
                    elapsedTime = elapsedTime
                )
            }
            timerManager.setActionsEnabled(false)
        }
    }

    fun dismissSaveDialog() {
        dialogStatus.update { null }
        timerManager.setActionsEnabled(true)
    }

    fun saveWork() {
        viewModelScope.launch {
            val elapsedTime = timerManager.timerState.value.elapsedTime
            val startDate = dataStoreManager.getStartDateSync() ?: return@launch
            val startTime = dataStoreManager.getStartTimeSync() ?: return@launch

            if (elapsedTime < ONE_MINUTE_MS) {
                dialogStatus.update { DialogStatus.TooShortTimeErrorDialog }
                return@launch
            }

            val endTimeCalendar = Calendar.getInstance()
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val endDate = sdfDate.format(endTimeCalendar.time)

            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val endTime = sdfTime.format(Date())
            val saveElapsedTime = (elapsedTime / 1000 / 60) * 60

            val work = Work(
                start_day = startDate,
                end_day = endDate,
                start_time = startTime,
                end_time = endTime,
                elapsed_time = saveElapsedTime
            )

            try {
                workRepository.insert(work)
                _navigateToLog.emit(Unit)
            } catch (e: Exception) {
                snackbarEvent.emit(e.message ?: "不明なエラー")
            } finally {
                dismissSaveDialog()
                timerManager.stopTimer()
            }
        }
    }

    fun discardWork() {
        dismissSaveDialog()
        timerManager.stopTimer()
    }
}