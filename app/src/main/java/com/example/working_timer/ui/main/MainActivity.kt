package com.example.working_timer.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.working_timer.ui.log_view.LogViewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onNavigateToLog = {
                            // ログ画面への遷移
                            startActivity(Intent(this, LogViewActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }
}
