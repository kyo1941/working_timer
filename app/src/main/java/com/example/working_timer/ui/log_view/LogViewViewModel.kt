package com.example.working_timer.ui.log_view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.AppDatabase
import com.example.working_timer.data.Work
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
    val totalTime: Long = 0L,
    val totalHours: Long = 0L,
    val totalMinutes: Long = 0L,
    val totalWage: Long = 0L
)

class LogViewViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LogViewUiState())
    val uiState: StateFlow<LogViewUiState> = _uiState

    private val workDao = AppDatabase.getDatabase(application).workDao()
    private val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    // 初回起動時に現在日時を取得する
    fun init() {
        if (_uiState.value.selectedDay.isEmpty()) {
            val cal = Calendar.getInstance()
            val today = String.format(
                "%04d/%02d/%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
            loadWorkList(today)
        }
    }

    fun loadWorkList(day: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, selectedDay = day)
        viewModelScope.launch {
            val works = workDao.getWorksByDay(day)
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
            workDao.delete(work.id)
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

    private fun calculateSum(start: Long, end: Long) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = start
            var totalTime = 0L
            while (calendar.timeInMillis <= end) {
                val day = sdf.format(calendar.time)
                val works = workDao.getWorksByDay(day)
                for (work in works) {
                    totalTime += work.elapsed_time
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            val totalHours = totalTime / 3600
            val totalMinutes = (totalTime % 3600) / 60
            _uiState.value = _uiState.value.copy(
                totalTime = totalTime,
                totalHours = totalHours,
                totalMinutes = totalMinutes
            )
        }
    }

    fun updateTotalWage(wage: Long) {
        val totalWage = (_uiState.value.totalTime * wage) / 3600
        _uiState.value = _uiState.value.copy(totalWage = totalWage)
    }
}
