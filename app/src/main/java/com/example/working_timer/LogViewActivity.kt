package com.example.working_timer

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import com.example.working_timer.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.Locale

class LogViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        var intent = intent
        var startDate = intent.getStringExtra("startDate") ?: ""
        var elapsedTime = intent.getLongExtra("elapsedTime", 0L)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        // 必要に応じて、CalendarView の設定を行う
        // 例: 選択された日付のリスナーを設定する
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            var day = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth)
            lifecycleScope.launch {
                val database = AppDatabase.getDatabase(applicationContext)
                val works = database.workDao().getWorksByDay(day)

                if(works.isNotEmpty()) {
                    for(work in works) {
                        Log.d("LogViewActivity", "ID: ${work.id},  Work: $work")
                    }
                } else {
                    Log.d("LogViewActivity", "No works found for day: $day")
                }
            }
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