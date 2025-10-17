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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface TimerStatus {
    object Working : TimerStatus
    object Resting : TimerStatus
}

data class MainUiState(
    val timerStatus: TimerStatus? = null,
    val elapsedTime: Long = 0L,
    val showSaveDialog: Boolean = false,
    val dialogMessage: String = "",
    val isErrorDialog: Boolean = false,
    val navigateToLog: Boolean = false,
)

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

    init {
        timerManager.setListener(this)
        loadElapsedTime()
    }

    private fun loadElapsedTime() {
        viewModelScope.launch {
            val savedElapsedTime = dataStoreManager.getElapsedTimeSync()
            _uiState.value = _uiState.value.copy(elapsedTime = savedElapsedTime)
            updateUiState()
        }
    }

    override fun onTimerTick(elapsedTime: Long) {
        _uiState.value = _uiState.value.copy(elapsedTime = elapsedTime)
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

        _uiState.value = _uiState.value.copy(
            timerStatus = timerStatus,
            elapsedTime = elapsedTime
        )
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
                _uiState.value = _uiState.value.copy(
                    showSaveDialog = true,
                    dialogMessage = ERROR_MSG_DATA_NOT_FOUND,
                    isErrorDialog = true
                )
                return@launch
            }

            val rep_sec_time = elapsedTime / 1000
            val hours = (rep_sec_time / 3600).toInt()
            val minutes = ((rep_sec_time / 60) % 60).toInt()
            val formattedTime = if (hours > 0) {
                String.format("%2d時間 %2d分", hours, minutes)
            } else {
                String.format("%2d分", minutes)
            }

            _uiState.value = _uiState.value.copy(
                showSaveDialog = true,
                dialogMessage = """
                開始日 ： $startDate
                経過時間 ： $formattedTime

                今回の作業記録を保存しますか？
            """.trimIndent(),
                isErrorDialog = false
            )
        }
    }

    fun dismissSaveDialog() {
        _uiState.value = _uiState.value.copy(
            showSaveDialog = false,
            isErrorDialog = false
        )
    }

    fun saveWork() {
        viewModelScope.launch {
            val elapsedTime = timerManager.getElapsedTime()
            val startDate = dataStoreManager.getStartDateSync() ?: return@launch
            val startTime = dataStoreManager.getStartTimeSync() ?: return@launch

            if (elapsedTime < ONE_MINUTE_MS) {
                _uiState.value = _uiState.value.copy(
                    showSaveDialog = true,
                    dialogMessage = ERROR_MSG_TIME_TOO_SHORT,
                    isErrorDialog = true
                )
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
                _uiState.value = _uiState.value.copy(navigateToLog = true)
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

    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(navigateToLog = false)
    }

    companion object {
        const val ERROR_MSG_TIME_TOO_SHORT =
            "1分未満の作業は保存できません。再開または破棄を選択してください。"
        const val ERROR_MSG_DATA_NOT_FOUND = "開始日または開始時刻が正しく取得できませんでした。"
    }
}