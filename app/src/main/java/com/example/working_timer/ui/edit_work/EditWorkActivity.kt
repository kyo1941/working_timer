package com.example.working_timer.ui.edit_work

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class EditWorkActivity : ComponentActivity() {
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
                EditWorkScreen(
                    id = id,
                    startDay = startDay,
                    endDay = endDay,
                    startTime = startTime,
                    endTime = endTime,
                    elapsedTime = elapsedTime,
                    isNew = isNew,
                    onNavigateBack = {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}