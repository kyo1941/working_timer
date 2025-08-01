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

