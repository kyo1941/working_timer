package com.example.working_timer.ui.edit_work

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import com.example.working_timer.util.Constants.SECOND_IN_MINUTES
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    object SaveSuccess : UiEvent
}

data class EditWorkUiState(
    val startDay: String = "",
    val endDay: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val elapsedHour: Long = 0L,
    val elapsedMinute: Long = 0L,
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

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun init(id: Int, isNew: Boolean, startDay: String) {
        if (isNew) {
            // 新規作成時は初期値を設定する
            _uiState.update {
                it.copy(
                    startDay = startDay,
                    endDay = startDay,
                    startTime = "00:00",
                    endTime = "00:00",
                    elapsedHour = 0,
                    elapsedMinute = 0
                )
            }
        } else {
            // DBから記録を読み込む
            getWork(id)
        }
    }

    fun saveWork(
        id: Int,
        isNew: Boolean,
        forceSave: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val elapsedTime =
                    currentState.elapsedHour * SECOND_IN_HOURS + currentState.elapsedMinute * SECOND_IN_MINUTES

                val dateTimeFormat = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
                val startDateTimeMillis = try {
                    dateTimeFormat.parse("${currentState.startDay} ${currentState.startTime}")?.time
                } catch (e: ParseException) {
                    null
                }
                val endDateTimeMillis = try {
                    dateTimeFormat.parse("${currentState.endDay} ${currentState.endTime}")?.time
                } catch (e: ParseException) {
                    null
                }

                if (startDateTimeMillis == null || endDateTimeMillis == null) {
                    _uiEvent.emit(UiEvent.ShowSnackbar(ERROR_MSG_DATE_TIME_PATTERN))
                    return@launch
                }

                if (elapsedTime <= 0) {
                    _uiState.update { it.copy(showZeroMinutesError = true) }
                    return@launch
                }

                if (startDateTimeMillis > endDateTimeMillis) {
                    _uiState.update { it.copy(showStartEndError = true) }
                    return@launch
                }

                val diffSeconds = (endDateTimeMillis - startDateTimeMillis) / 1000
                if (!forceSave && diffSeconds < elapsedTime) {
                    _uiState.update { it.copy(showElapsedTimeOver = true) }
                    return@launch
                }

                if (!isNew) {
                    val work = Work(
                        id = id,
                        start_day = currentState.startDay,
                        start_time = currentState.startTime,
                        end_day = currentState.endDay,
                        end_time = currentState.endTime,
                        elapsed_time = elapsedTime
                    )
                    workRepository.update(work)
                } else {
                    val work = Work(
                        start_day = currentState.startDay,
                        start_time = currentState.startTime,
                        end_day = currentState.endDay,
                        end_time = currentState.endTime,
                        elapsed_time = elapsedTime
                    )
                    workRepository.insert(work)
                }
                _uiEvent.emit(UiEvent.SaveSuccess)

            } catch (e: SQLiteException) {
                _uiEvent.emit(UiEvent.ShowSnackbar(ERROR_MSG_DB_FAILED))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowSnackbar("$ERROR_MSG_UNKNOWN ${e.localizedMessage ?: "詳細不明"}"))
            }
        }
    }

    private fun getWork(id: Int) {
        viewModelScope.launch {
            workRepository.getWork(id).firstOrNull()?.let { work ->
                _uiState.update {
                    it.copy(
                        startDay = work.start_day,
                        endDay = work.end_day,
                        startTime = work.start_time,
                        endTime = work.end_time,
                        elapsedHour = work.elapsed_time / SECOND_IN_HOURS,
                        elapsedMinute = (work.elapsed_time % SECOND_IN_HOURS) / SECOND_IN_MINUTES
                    )
                }
            }
        }
    }

    fun updateStartDay(value: String) {
        _uiState.update { it.copy(startDay = value) }
    }

    fun updateEndDay(value: String) {
        _uiState.update { it.copy(endDay = value) }
    }

    fun updateStartTime(value: String) {
        _uiState.update { it.copy(startTime = value) }
    }

    fun updateEndTime(value: String) {
        _uiState.update { it.copy(endTime = value) }
    }

    fun updateElapsedTime(hour: Long, minute: Long) {
        _uiState.update { it.copy(elapsedHour = hour, elapsedMinute = minute) }
    }

    fun clearZeroMinutesError() {
        _uiState.update { it.copy(showZeroMinutesError = false) }
    }

    fun clearStartEndError() {
        _uiState.update { it.copy(showStartEndError = false) }
    }

    fun clearElapsedTimeOver() {
        _uiState.update { it.copy(showElapsedTimeOver = false) }
    }

    companion object {
        const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"
        const val ERROR_MSG_DATE_TIME_PATTERN = "日付または時刻の形式が無効です。"
        const val ERROR_MSG_DB_FAILED = "データベースエラーが発生しました。"
        const val ERROR_MSG_UNKNOWN = "予期しないエラーが発生しました:"
    }
}
