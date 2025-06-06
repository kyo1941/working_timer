package com.example.working_timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.working_timer.data.Work

class LogViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: LogViewViewModel = viewModel()
            LogViewScreen(
                viewModel = viewModel,
                onEditWork = { work: Work ->
                    val intent = Intent(this, EditWorkActivity::class.java).apply {
                        putExtra("id", work.id)
                        putExtra("start_day", work.start_day)
                        putExtra("end_day", work.end_day)
                        putExtra("start_time", work.start_time)
                        putExtra("end_time", work.end_time)
                        putExtra("elapsed_time", work.elapsed_time)
                    }
                    startActivity(intent)
                },
                onAddWork = { selectedDay: String ->
                    val intent = Intent(this, EditWorkActivity::class.java).apply {
                        putExtra("is_new", true)
                        putExtra("id", 0)
                        putExtra("start_day", selectedDay)
                        putExtra("end_day", selectedDay)
                        putExtra("start_time", "00:00")
                        putExtra("end_time", "00:00")
                        putExtra("elapsed_time", 0)
                    }
                    startActivity(intent)
                },
                onNavigateToTimer = {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                }
            )
        }
    }
}