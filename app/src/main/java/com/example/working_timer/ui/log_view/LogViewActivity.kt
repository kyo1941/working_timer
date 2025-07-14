package com.example.working_timer.ui.log_view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.working_timer.ui.components.MaterialTimePickerDialog
import com.example.working_timer.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    LogViewScreen(
                        onNavigateToTimer = {
                            startActivity(Intent(this, MainActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }
}