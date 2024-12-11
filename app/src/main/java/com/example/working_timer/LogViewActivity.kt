package com.example.working_timer

import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity

class LogViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        // 必要に応じて、CalendarView の設定を行う
        // 例: 選択された日付のリスナーを設定する
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // 選択された日付に基づいて履歴データを取得・表示する処理を追加
        }

    }
}