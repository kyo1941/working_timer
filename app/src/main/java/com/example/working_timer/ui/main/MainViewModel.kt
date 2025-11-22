package com.example.working_timer.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerListener
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.ONE_MINUTE_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val displayText =  if (hours > 0) {
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
) : ViewModel(), TimerListener {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: MutableSharedFlow<String> = _snackbarEvent

    private val _navigateToLog = MutableSharedFlow<Unit>()
    val navigateToLog: MutableSharedFlow<Unit> = _navigateToLog

    init {
        timerManager.setListener(this)
        loadElapsedTime()
    }

    private fun loadElapsedTime() {
        viewModelScope.launch {
            val savedElapsedTime = dataStoreManager.getElapsedTimeSync()
            _uiState.update { it.copy(elapsedTime = savedElapsedTime) }
            updateUiState()
        }
    }

    override fun onTimerTick(elapsedTime: Long) {
        _uiState.update { it.copy(elapsedTime = elapsedTime) }
    }

    override fun updateUI() {
        updateUiState()
    }

    override fun onError(error: String) {
        viewModelScope.launch {
            _snackbarEvent.emit(error)
        }
    }

    private fun updateUiState() {
        val isRunning = timerManager.isTimerRunning()
        val elapsedTime = timerManager.getElapsedTime()

        val timerStatus = when {
            isRunning -> TimerStatus.Working
            elapsedTime > 0 -> TimerStatus.Resting
            else -> null
        }

        _uiState.update {
            it.copy(
                timerStatus = timerStatus,
                elapsedTime = elapsedTime
            )
        }
    }

    fun startTimer() {
        timerManager.startTimer()
        updateUiState()
    }

    fun pauseTimer() {
        timerManager.pauseTimer()
        updateUiState()
    }

    fun resumeTimer() {
        timerManager.resumeTimer()
        updateUiState()
    }

    fun stopTimer() {
        timerManager.pauseTimer()
        showSaveDialog()
    }

    private fun showSaveDialog() {
        viewModelScope.launch {
            val elapsedTime = timerManager.getElapsedTime()
            val startDate = dataStoreManager.getStartDateSync()
            val startTime = dataStoreManager.getStartTimeSync()

            if (startDate == null || startTime == null) {
                _uiState.update { it.copy(dialogStatus = DialogStatus.DataNotFoundErrorDialog) }
                return@launch
            }

            _uiState.update {
                it.copy(
                    dialogStatus = DialogStatus.SaveDialog(
                        startDate = startDate,
                        elapsedTime = elapsedTime
                    )
                )
            }
        }
    }

    fun dismissSaveDialog() {
        _uiState.update { it.copy(dialogStatus = null) }
    }

    fun saveWork() {
        viewModelScope.launch {
            val elapsedTime = timerManager.getElapsedTime()
            val startDate = dataStoreManager.getStartDateSync() ?: return@launch
            val startTime = dataStoreManager.getStartTimeSync() ?: return@launch

            if (elapsedTime < ONE_MINUTE_MS) {
                _uiState.update { it.copy(dialogStatus = DialogStatus.TooShortTimeErrorDialog) }
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
            }
            timerManager.stopTimer()
            updateUiState()
            dismissSaveDialog()
        }
    }

    fun discardWork() {
        timerManager.stopTimer()
        updateUiState()
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.removeListener()
    }
}