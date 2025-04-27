package com.example.working_timer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.working_timer.data.AppDatabase
import com.example.working_timer.data.Work
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditWorkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isNew = intent.getBooleanExtra("is_new", false)
        val id = intent.getIntExtra("id", 0)
        val day = intent.getStringExtra("day") ?: ""
        val startTime = intent.getStringExtra("start_time")?.substring(0, 5) ?: "00:00"
        val endTime = intent.getStringExtra("end_time")?.substring(0, 5) ?: "00:00"
        val elapsedTime = intent.getIntExtra("elapsed_time", 0)

        setContent {
            MaterialTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    EditWorkScreen(
                        id = id,
                        day = day,
                        startTime = startTime,
                        endTime = endTime,
                        elapsedTime = elapsedTime,
                        isNew = isNew,
                        onSave = { newStart, newEnd, newElapsed ->
                            lifecycleScope.launch {
                                try {
                                    val dao = AppDatabase.getDatabase(applicationContext).workDao()
                                    if(!isNew) {
                                        val work = Work(
                                            id = id,
                                            day = day,
                                            start_time = newStart,
                                            end_time = newEnd,
                                            elapsed_time = newElapsed
                                        )
                                        dao.update(work)
                                        finish()
                                    } else {
                                        val work = Work(
                                            day = day,
                                            start_time = newStart,
                                            end_time = newEnd,
                                            elapsed_time = newElapsed
                                        )
                                        dao.insert(work)
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    Log.e("EditWorkActivity", "Database update failed", e)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("更新に失敗しました")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
