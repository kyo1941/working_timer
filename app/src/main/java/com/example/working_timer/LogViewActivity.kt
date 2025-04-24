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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.core.content.ContextCompat


class LogViewActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_view)

        recyclerView = findViewById(R.id.workRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        var divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.recycler_divider)?.let {
            divider.setDrawable(it)
        }
        recyclerView.addItemDecoration(divider)

        adapter = WorkAdapter(emptyList())
        recyclerView.adapter = adapter

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            var day = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth)
            lifecycleScope.launch {
                val database = AppDatabase.getDatabase(applicationContext)
                val works = database.workDao().getWorksByDay(day)

                adapter.updateData(works)

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