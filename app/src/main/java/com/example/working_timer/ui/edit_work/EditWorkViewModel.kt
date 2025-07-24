package com.example.working_timer.ui.edit_work

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class EditWorkUiState(
    val startDay: String = "",
    val endDay: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val elapsedHour: Int = 0,
    val elapsedMinute: Int = 0,
    val showZeroMinutesError: Boolean = false,
    val showStartEndError: Boolean = false,
    val showElapsedTimeOver: Boolean = false
)

@HiltViewModel
class EditWorkViewModel @Inject constructor(
    private val workRepository: WorkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditWorkUiState())
    val uiState = _uiState.asStateFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private companion object {
        private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
    }

    fun init(id: Int, isNew: Boolean, startDay: String) {
        if (isNew) {
            // 新規作成時は初期値を設定する
            _uiState.value = _uiState.value.copy(
                startDay = startDay,
                endDay = startDay,
                startTime = "00:00",
                endTime = "00:00",
                elapsedHour = 0,
                elapsedMinute = 0
            )
        } else {
            // DBから記録を読み込む
            getWork(id)
        }
    }

    fun saveWork(
        id: Int,
        startDay: String,
        startTime: String,
        endDay: String,
        endTime: String,
        elapsedTime: Int,
        isNew: Boolean,
        forceSave: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val dateTimeFormat = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
                val startDateTimeMillis = try { dateTimeFormat.parse("$startDay $startTime")?.time } catch (e: ParseException) { null }
                val endDateTimeMillis = try { dateTimeFormat.parse("$endDay $endTime")?.time } catch (e: ParseException) { null }

                if (startDateTimeMillis == null || endDateTimeMillis == null) {
                    _uiEvent.emit(UiEvent.ShowSnackbar("日付または時刻の形式が無効です。"))
                    return@launch
                }

                if (elapsedTime <= 0) {
                    _uiState.value = _uiState.value.copy(showZeroMinutesError = true)
                    return@launch
                }

                if (startDateTimeMillis > endDateTimeMillis) {
                    _uiState.value = _uiState.value.copy(showStartEndError = true)
                    return@launch
                }

                val diffSeconds = (endDateTimeMillis - startDateTimeMillis) / 1000
                if (!forceSave && diffSeconds < elapsedTime) {
                    _uiState.value = _uiState.value.copy(showElapsedTimeOver = true)
                    return@launch
                }

                performSave(id, startDay, startTime, endDay, endTime, elapsedTime, isNew)

            } catch (e: SQLiteException) {
                _uiEvent.emit(UiEvent.ShowSnackbar("データベースエラーが発生しました。"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("予期しないエラーが発生しました: ${e.localizedMessage ?: "詳細不明"}"))
            }
        }
    }

    private suspend fun performSave(
        id: Int,
        startDay: String,
        startTime: String,
        endDay: String,
        endTime: String,
        elapsedTime: Int,
        isNew: Boolean
    ) {
        if (!isNew) {
            val work = Work(
                id = id,
                start_day = startDay,
                start_time = startTime,
                end_day = endDay,
                end_time = endTime,
                elapsed_time = elapsedTime
            )
            workRepository.update(work)
        } else {
            val work = Work(
                start_day = startDay,
                start_time = startTime,
                end_day = endDay,
                end_time = endTime,
                elapsed_time = elapsedTime
            )
            workRepository.insert(work)
        }
        _uiEvent.emit(UiEvent.SaveSuccess)
    }

    private fun getWork(id: Int) {
        viewModelScope.launch {
            workRepository.getWork(id).collect { work ->
                _uiState.value = _uiState.value.copy(
                    startDay = work.start_day,
                    endDay = work.end_day,
                    startTime = work.start_time,
                    endTime = work.end_time,
                    elapsedHour = work.elapsed_time / 3600,
                    elapsedMinute = (work.elapsed_time % 3600) / 60
                )
            }
        }
    }

    fun updateStartDay(value: String) {
        _uiState.value = _uiState.value.copy(startDay = value)
    }

    fun updateEndDay(value: String) {
        _uiState.value = _uiState.value.copy(endDay = value)
    }

    fun updateStartTime(value: String) {
        _uiState.value = _uiState.value.copy(startTime = value)
    }

    fun updateEndTime(value: String) {
        _uiState.value = _uiState.value.copy(endTime = value)
    }

    fun updateElapsedTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(elapsedHour = hour, elapsedMinute = minute)
    }

    fun clearZeroMinutesError() {
        _uiState.value = _uiState.value.copy(showZeroMinutesError = false)
    }

    fun clearStartEndError() {
        _uiState.value = _uiState.value.copy(showStartEndError = false)
    }

    fun clearElapsedTimeOver() {
        _uiState.value = _uiState.value.copy(showElapsedTimeOver = false)
    }
}

