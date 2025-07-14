package com.example.working_timer.ui.main

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.working_timer.util.SharedPrefKeys
import com.example.working_timer.data.Work
import com.example.working_timer.domain.repository.TimerListener
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val isTooShortError: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workRepository: WorkRepository,
    private val timerManager: TimerManager
) : ViewModel(), TimerListener {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    private val PREFS_NAME = SharedPrefKeys.PREFS_NAME
    private val START_DATE_KEY = SharedPrefKeys.START_DATE_KEY
    private val START_TIME_STRING_KEY = SharedPrefKeys.START_TIME_STRING_KEY
    private val ELAPSED_TIME_KEY = SharedPrefKeys.ELAPSED_TIME_KEY

    init {
        timerManager.setListener(this)
        loadElapsedTime()
    }

    // elapsedTimeをSharedPreferencesから読み込み、UIに反映
    private fun loadElapsedTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedElapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
        updateTimerText(savedElapsedTime)
        updateUiState()
    }


    override fun onTimerTick(elapsedTime: Long) {
        updateTimerText(elapsedTime)
        saveElapsedTime(elapsedTime)
    }

    override fun updateUI() {
        updateUiState()
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

    private fun saveElapsedTime(elapsedTime: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(ELAPSED_TIME_KEY, elapsedTime).apply()
    }

    // UIの状態を更新するヘルパー関数
    private fun updateUiState() {
        val isRunning = timerManager.isTimerRunning()
        val elapsedTime = timerManager.getElapsedTime()

        val status = when {
            isRunning -> "労働中"
            elapsedTime > 0 -> "休憩中"
            else -> ""
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val elapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
        val startDate = prefs.getString(START_DATE_KEY, null)
        val startTime = prefs.getString(START_TIME_STRING_KEY, null)

        if (startDate == null || startTime == null) {
            _uiState.value = _uiState.value.copy(
                showSaveDialog = true,
                dialogMessage = "開始日または開始時刻が正しく取得できませんでした。",
                isTooShortError = true
            )
            return
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
            isTooShortError = false // 追加
        )
    }

    // ダイアログを閉じる
    fun dismissSaveDialog() {
        _uiState.value = _uiState.value.copy(showSaveDialog = false, isTooShortError = false) // 追加
    }

    // 作業を保存
    suspend fun saveWork(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val elapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
        val startDate = prefs.getString(START_DATE_KEY, null) ?: return false
        val startTime = prefs.getString(START_TIME_STRING_KEY, null) ?: return false

        if (elapsedTime < 60000) {
            _uiState.value = _uiState.value.copy(
                showSaveDialog = true,
                dialogMessage = "1分未満の作業は保存できません．再開または破棄を選択してください．",
                isTooShortError = true
            )
            return false
        }

        val endTimeCalendar = Calendar.getInstance()
        val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val endDate = sdfDate.format(endTimeCalendar.time)

        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val endTime = sdfTime.format(Date())
        val saveElapsedTime = (elapsedTime / 1000 / 60) * 60

        val work = Work(
            start_day = startDate,
            end_day = endDate,
            start_time = startTime,
            end_time = endTime,
            elapsed_time = saveElapsedTime.toInt()
        )

        // 保存処理
        try {
            workRepository.insert(work)
            timerManager.stopTimer()
            updateUiState()
            return true
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error saving work", e)
            return false
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
}