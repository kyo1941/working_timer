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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditWorkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        onSave = { newStart, newEnd, newElapsed ->
                            try {
                                lifecycleScope.launch {
                                    val dao = AppDatabase.getDatabase(applicationContext).workDao()
                                    dao.updateWork(
                                        id = id,
                                        day = day,
                                        startTime = newStart,
                                        endTime = newEnd,
                                        elapsedTime = newElapsed
                                    )
                                    finish()
                                }
                            } catch (e: Exception) {
                                Log.e("EditWorkActivity", "Database update failed", e)
                                scope.launch {
                                    snackbarHostState.showSnackbar("更新に失敗しました")
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
