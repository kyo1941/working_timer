package com.example.working_timer.ui.edit_work

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.working_timer.data.Work
import com.example.working_timer.data.WorkDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EditWorkViewModel(private val workDao: WorkDao) : ViewModel() {

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun saveWork(
        id: Int,
        startDay: String,
        startTime: String,
        endDay: String,
        endTime: String,
        elapsedTime: Int,
        isNew: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (!isNew) {
                    val work = Work(
                        id = id,
                        start_day = startDay,
                        start_time = startTime,
                        end_day = endDay,
                        end_time = endTime,
                        elapsed_time = elapsedTime
                    )
                    workDao.update(work)
                } else {
                    val work = Work(
                        start_day = startDay,
                        start_time = startTime,
                        end_day = endDay,
                        end_time = endTime,
                        elapsed_time = elapsedTime
                    )
                    workDao.insert(work)
                }
                _uiEvent.emit(UiEvent.SaveSuccess)
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is SQLiteException -> "データベースエラーが発生しました。"
                    is IllegalArgumentException -> "無効なデータが入力されました。"
                    else -> "予期しないエラーが発生しました。"
                }
                _uiEvent.emit(UiEvent.ShowSnackbar(errorMessage))
            }
        }
    }
}

class EditWorkViewModelFactory(private val workDao: WorkDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditWorkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditWorkViewModel(workDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



