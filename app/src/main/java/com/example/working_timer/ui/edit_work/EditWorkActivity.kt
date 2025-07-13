package com.example.working_timer.ui.edit_work

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.working_timer.data.AppDatabase
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import com.example.working_timer.domain.repository.WorkRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class EditWorkActivity : ComponentActivity() {

    @Inject
    lateinit var workRepository: WorkRepository

    private val viewModel: EditWorkViewModel by viewModels()

    private companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
    }

    private fun setTime(time: String?): String {
        return try {
            val timeStr = time ?: "00:00"
            LocalTime.parse(timeStr, TIME_FORMATTER).format(TIME_FORMATTER)
        } catch (e: Exception) {
            "00:00"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isNew = intent.getBooleanExtra("is_new", false)
        val id = intent.getIntExtra("id", 0)
        val startDay = intent.getStringExtra("start_day") ?: ""
        val endDay = intent.getStringExtra("end_day") ?: ""
        val elapsedTime = intent.getIntExtra("elapsed_time", 0)

        // 変な時刻が入っていないか確認
        val startTime = setTime(intent.getStringExtra("start_time"))
        val endTime = setTime(intent.getStringExtra("end_time"))

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collectLatest { event ->
                        when (event) {
                            is EditWorkViewModel.UiEvent.ShowSnackbar -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(event.message)
                                }
                            }
                            EditWorkViewModel.UiEvent.SaveSuccess -> {
                                setResult(RESULT_OK)
                                finish()
                            }
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    EditWorkScreen(
                        viewModel = viewModel,
                        id = id,
                        startDay = startDay,
                        endDay = endDay,
                        startTime = startTime,
                        endTime = endTime,
                        elapsedTime = elapsedTime,
                        isNew = isNew,
                        onSave = { newStartDay, newStartTime, newEndDay, newEndTime, newElapsed, forceSave ->
                            viewModel.saveWork(
                                id = id,
                                startDay = newStartDay,
                                startTime = newStartTime,
                                endDay = newEndDay,
                                endTime = newEndTime,
                                elapsedTime = newElapsed,
                                isNew = isNew,
                                forceSave = forceSave
                            )
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}


