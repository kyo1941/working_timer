package com.example.working_timer.ui.log_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

// 時間計算のモードを定義する列挙型
enum class TimeCalculationMode {
    NORMAL,    // 通常計算
    ROUND_UP,  // 繰り上げ
    ROUND_DOWN // 繰り下げ
}

// UI状態を保持するデータクラス
data class LogViewUiState(
    val selectedDay: String = "",
    val workList: List<Work> = emptyList(),
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val workToDelete: Work? = null,
    val showSumDialog: Boolean = false,
    val sumStartDate: Long? = null,
    val sumEndDate: Long? = null,
    val totalHours: Long = 0L,
    val totalMinutes: Long = 0L,
    val totalWage: Long = 0L,
    val timeCalculationMode: TimeCalculationMode = TimeCalculationMode.NORMAL
)

@HiltViewModel
class LogViewViewModel @Inject constructor(
    private val workRepository: WorkRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogViewUiState())
    val uiState: StateFlow<LogViewUiState> = _uiState
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var initialTotalTime: Long = 0L

    // 初回起動時に現在日時を取得する
    fun init() {
        val cal = Calendar.getInstance()
        setSelectedDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        loadWorkList(_uiState.value.selectedDay)
    }

    fun setSelectedDay(year: Int, month: Int, day: Int) {
        val selected = String.format("%04d-%02d-%02d", year, month + 1, day)
        loadWorkList(selected)
    }

    fun loadWorkList(day: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedDay = day)
        viewModelScope.launch {
            val works = workRepository.getWorksByDay(day)
            _uiState.value = _uiState.value.copy(workList = works, isLoading = false)
        }
    }

    fun showDeleteDialog(work: Work) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = true, workToDelete = work)
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(showDeleteDialog = false, workToDelete = null)
    }

    fun deleteWork(work: Work) {
        viewModelScope.launch {
            workRepository.delete(work.id)
            loadWorkList(_uiState.value.selectedDay)
            hideDeleteDialog()
        }
    }

    fun showSumDialog(start: Long, end: Long) {
        _uiState.value = _uiState.value.copy(showSumDialog = true, sumStartDate = start, sumEndDate = end)
        calculateSum(start, end)
    }

    fun hideSumDialog() {
        _uiState.value = _uiState.value.copy(showSumDialog = false, sumStartDate = null, sumEndDate = null)
    }

    fun setTimeCalculationMode(mode: TimeCalculationMode) {
        _uiState.value = _uiState.value.copy(timeCalculationMode = mode)
    }

    private fun calculateSum(start: Long, end: Long) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = start
            var totalTime = 0L
            while (calendar.timeInMillis <= end) {
                val day = sdf.format(calendar.time)
                val works = workRepository.getWorksByDay(day)
                for (work in works) {
                    totalTime += work.elapsed_time
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            val totalHours = totalTime / SECOND_IN_HOURS
            val totalMinutes = (totalTime % SECOND_IN_HOURS) / SECOND_IN_MINUTES

            initialTotalTime = totalTime

            _uiState.value = _uiState.value.copy(
                totalHours = totalHours,
                totalMinutes = totalMinutes
            )
        }
    }

    fun changeCalcMode(wage: Long) {
        val adjustTotalTime = when(_uiState.value.timeCalculationMode) {
            TimeCalculationMode.ROUND_UP -> Math.ceil(initialTotalTime.toDouble() / SECOND_IN_HOURS).toLong() * SECOND_IN_HOURS
            TimeCalculationMode.ROUND_DOWN -> Math.floor(initialTotalTime.toDouble() / SECOND_IN_HOURS).toLong() * SECOND_IN_HOURS
            else -> initialTotalTime
        }

        val adjustTotalHours = adjustTotalTime / SECOND_IN_HOURS
        val adjustTotalMinutes = (adjustTotalTime % SECOND_IN_HOURS) / SECOND_IN_MINUTES
        val totalWage = (adjustTotalTime * wage) / SECOND_IN_HOURS

        _uiState.value = _uiState.value.copy(
            totalHours = adjustTotalHours,
            totalMinutes = adjustTotalMinutes,
            totalWage = totalWage
        )
    }

    companion object {
        private const val SECOND_IN_HOURS = 3600L
        private const val SECOND_IN_MINUTES = 60L
    }
}
