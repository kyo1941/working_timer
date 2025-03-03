package com.example.working_timer

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class LogViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        var intent = intent
        var startDate = intent.getStringExtra("startDate") ?: ""
        var elapsedTime = intent.getLongExtra("elapsedTime", 0L)

        Log.d("LogViewActivity", "開始日: $startDate, 合計時間: $elapsedTime")

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        // 必要に応じて、CalendarView の設定を行う
        // 例: 選択された日付のリスナーを設定する
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // 選択された日付に基づいて履歴データを取得・表示する処理を追加
        }


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.navigation_log
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if(item.itemId != bottomNavigationView.selectedItemId) {
                when (item.itemId) {
                    R.id.navigation_timer -> {
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }

                    R.id.navigation_log -> {
                        // 現在の画面なので何もしない
                        true
                    }

                    else -> false
                }
            } else {
                true
            }
        }

    }
}