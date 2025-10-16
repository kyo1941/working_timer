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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// UIの状態を保持するためのデータクラス
data class TimerUiState(
    val status: String = "",
    val timerText: String = "00:00",
    val isTimerRunning: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedTime: Long = 0L,
    val showSaveDialog: Boolean = false,
    val dialogMessage: String = "",
    val isErrorDialog: Boolean = false,
    val snackbarMessage: String? = null,
    val navigateToLog: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val workRepository: WorkRepository,
    private val timerManager: TimerManager,
    private val dataStoreManager: DataStoreManager
) : ViewModel(), TimerListener {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    init {
        timerManager.setListener(this)
        loadElapsedTime()
    }

    private fun loadElapsedTime() {
        viewModelScope.launch {
            val savedElapsedTime = dataStoreManager.getElapsedTimeSync() ?: 0L
            updateTimerText(savedElapsedTime)
            updateUiState()
        }
    }

    override fun onTimerTick(elapsedTime: Long) {
        updateTimerText(elapsedTime)
    }

    override fun updateUI() {
        updateUiState()
    }

    override fun onError(error: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = error)
    }

    fun clearSnackbarMessage() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private fun updateTimerText(elapsedTime: Long) {
        val rep_sec_time = elapsedTime / 1000
        val hours = (rep_sec_time / 3600).toInt()
        val minutes = ((rep_sec_time / 60) % 60).toInt()
        val seconds = (rep_sec_time % 60).toInt()
        val formattedTime = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        _uiState.value = _uiState.value.copy(timerText = formattedTime, elapsedTime = elapsedTime)
    }

    // UIの状態を更新するヘルパー関数
    private fun updateUiState() {
        val isRunning = timerManager.isTimerRunning()
        val elapsedTime = timerManager.getElapsedTime()

        val status = when {
            isRunning -> WORKING_STATUS
            elapsedTime > 0 -> RESTING_STATUS
            else -> EMPTY_STATUS
        }
        _uiState.value = _uiState.value.copy(
            status = status,
            isTimerRunning = isRunning,
            isPaused = !isRunning && elapsedTime > 0,
            elapsedTime = elapsedTime
        )
        updateTimerText(elapsedTime)
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

    // ダイアログを閉じる
    fun dismissSaveDialog() {
        _uiState.value = _uiState.value.copy(
            showSaveDialog = false,
            isErrorDialog = false
        )
    }

    // 作業を保存
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

            // 保存処理
            try {
                workRepository.insert(work)
                timerManager.stopTimer()
                updateUiState()
                dismissSaveDialog()
                _uiState.value = _uiState.value.copy(navigateToLog = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    showSaveDialog = true,
                    dialogMessage = "$ERROR_MSG_SAVE_FAILED ${e.message}",
                    isErrorDialog = true
                )
            }
        }
    }

    // 作業を破棄
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
        const val WORKING_STATUS = "労働中"
        const val RESTING_STATUS = "休憩中"
        const val EMPTY_STATUS = ""
        const val ERROR_MSG_SAVE_FAILED = "保存に失敗しました。再度お試しください。\nエラー:"
        const val ERROR_MSG_TIME_TOO_SHORT =
            "1分未満の作業は保存できません。再開または破棄を選択してください。"
        const val ERROR_MSG_DATA_NOT_FOUND = "開始日または開始時刻が正しく取得できませんでした。"
    }
}