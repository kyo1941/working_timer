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
import kotlinx.coroutines.flow.collectLatest

class EditWorkActivity : ComponentActivity() {

    private val viewModel: EditWorkViewModel by viewModels {
        EditWorkViewModelFactory(AppDatabase.getDatabase(applicationContext).workDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isNew = intent.getBooleanExtra("is_new", false)
        val id = intent.getIntExtra("id", 0)
        val startDay = intent.getStringExtra("start_day") ?: ""
        val endDay = intent.getStringExtra("end_day") ?: ""
        val startTime = intent.getStringExtra("start_time")?.substring(0, 5) ?: "00:00"
        val endTime = intent.getStringExtra("end_time")?.substring(0, 5) ?: "00:00"
        val elapsedTime = intent.getIntExtra("elapsed_time", 0)

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


