package com.example.working_timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
                EditWorkScreen(
                    id = id,
                    day = day,
                    startTime = startTime,
                    endTime = endTime,
                    elapsedTime = elapsedTime,
                    onSave = { newStart, newEnd, newElapsed ->
                        lifecycleScope.launch {
                            val dao = AppDatabase.getDatabase(applicationContext).workDao()
                            dao.updateWork(
                                id = id,
                                day = day,
                                startTime = newStart,
                                endTime = newEnd,
                                elapsedTime = newElapsed,
                                startMills = timeStringToMillis(day, newStart),
                                endMills = timeStringToMillis(day, newEnd)
                            )
                            finish()
                        }
                    }
                )
            }
        }
    }

    private fun timeStringToMillis(day: String, time: String): Long {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return sdf.parse("$day $time")?.time ?: 0L
    }
}
