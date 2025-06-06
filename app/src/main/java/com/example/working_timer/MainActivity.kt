package com.example.working_timer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onNavigateToLog = {
                            // ログ画面への遷移
                            startActivity(android.content.Intent(this, LogViewActivity::class.java))
                            overridePendingTransition(0, 0)
                        }
                    )
                }
            }
        }
    }
}
