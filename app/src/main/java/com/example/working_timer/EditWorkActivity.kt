package com.example.working_timer

import android.database.sqlite.SQLiteException
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

class EditWorkActivity : ComponentActivity() {

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

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->
                    EditWorkScreen(
                        id = id,
                        startDay = startDay,
                        endDay = endDay,
                        startTime = startTime,
                        endTime = endTime,
                        elapsedTime = elapsedTime,
                        isNew = isNew,
                        onSave = { newStartDay, newStartTime, newEndDay, newEndTime, newElapsed ->
                            lifecycleScope.launch {
                                try {
                                    val dao = AppDatabase.getDatabase(applicationContext).workDao()
                                    if(!isNew) {
                                        val work = Work(
                                            id = id,
                                            start_day = newStartDay,
                                            start_time = newStartTime,
                                            end_day = newEndDay,
                                            end_time = newEndTime,
                                            elapsed_time = newElapsed
                                        )
                                        dao.update(work)
                                        setResult(RESULT_OK)
                                        finish()
                                    } else {
                                        val work = Work(
                                            start_day = newStartDay,
                                            start_time = newStartTime,
                                            end_day = newEndDay,
                                            end_time = newEndTime,
                                            elapsed_time = newElapsed
                                        )
                                        dao.insert(work)
                                        setResult(RESULT_OK)
                                        finish()
                                    }
                                } catch (e: Exception) {
                                    Log.e("EditWorkActivity", "Database update failed", e)
                                    val errorMessage = when (e) {
                                        is SQLiteException -> "データベースエラーが発生しました。"
                                        is IllegalArgumentException -> "無効なデータが入力されました。"
                                        else -> "予期しないエラーが発生しました。"
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorMessage)
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
