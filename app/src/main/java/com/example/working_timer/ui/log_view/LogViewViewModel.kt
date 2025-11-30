package com.example.working_timer.ui.log_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import com.example.working_timer.util.Constants.SECOND_IN_MINUTES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

enum class TimeCalculationMode {
    NORMAL,    // 通常計算
    ROUND_UP,  // 繰り上げ
    ROUND_DOWN // 繰り下げ
}

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
    val uiState: StateFlow<LogViewUiState> = _uiState.asStateFlow()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var initialTotalTime: Long = 0L

    // 初回起動時に現在日時を取得する
    fun init() {
        val cal = Calendar.getInstance()
        setSelectedDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        loadWorkList(_uiState.value.selectedDay)
    }

    fun setSelectedDay(year: Int, month: Int, day: Int) {
        val selected = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day)
        loadWorkList(selected)
    }

    fun loadWorkList(day: String) {
        _uiState.update { it.copy(isLoading = true, selectedDay = day) }
        viewModelScope.launch {
            val works = workRepository.getWorksByDay(day)
            _uiState.update { it.copy(workList = works, isLoading = false) }
        }
    }

    fun showDeleteDialog(work: Work) {
        _uiState.update { it.copy(showDeleteDialog = true, workToDelete = work) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, workToDelete = null) }
    }

    fun deleteWork(work: Work) {
        viewModelScope.launch {
            workRepository.delete(work.id)
            loadWorkList(_uiState.value.selectedDay)
            hideDeleteDialog()
        }
    }

    fun showSumDialog(start: Long, end: Long) {
        _uiState.update {
            it.copy(showSumDialog = true, sumStartDate = start, sumEndDate = end)
        }
        calculateSum(start, end)
    }

    fun hideSumDialog() {
        _uiState.update {
            it.copy(showSumDialog = false, sumStartDate = null, sumEndDate = null)
        }
    }

    fun setTimeCalculationMode(mode: TimeCalculationMode) {
        _uiState.update { it.copy(timeCalculationMode = mode) }
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

            _uiState.update {
                it.copy(
                    totalHours = totalHours,
                    totalMinutes = totalMinutes
                )
            }
        }
    }

    fun updateTotalWage(wage: Long) {
        val adjustTotalTime = when (_uiState.value.timeCalculationMode) {
            TimeCalculationMode.ROUND_UP -> ceil(initialTotalTime.toDouble() / SECOND_IN_HOURS).toLong() * SECOND_IN_HOURS
            TimeCalculationMode.ROUND_DOWN -> floor(initialTotalTime.toDouble() / SECOND_IN_HOURS).toLong() * SECOND_IN_HOURS
            else -> initialTotalTime
        }

        val adjustTotalHours = adjustTotalTime / SECOND_IN_HOURS
        val adjustTotalMinutes = (adjustTotalTime % SECOND_IN_HOURS) / SECOND_IN_MINUTES
        val totalWage = (adjustTotalTime * wage) / SECOND_IN_HOURS

        _uiState.update {
            it.copy(
                totalHours = adjustTotalHours,
                totalMinutes = adjustTotalMinutes,
                totalWage = totalWage
            )
        }
    }
}
