package com.example.working_timer

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import androidx.appcompat.app.AlertDialog
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
import com.example.working_timer.data.Work


class LogViewActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WorkAdapter

    private var selectedDay: String = ""

    private fun loadWorkList(day: String) {
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

        adapter = WorkAdapter(
            emptyList(),
            onDeleteClickListener = { work ->
                AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("本当にこの記録を削除しますか？")
                    .setPositiveButton("はい") { _, _ ->
                        lifecycleScope.launch {
                            val dao = AppDatabase.getDatabase(applicationContext).workDao()
                            dao.delete(work.id)

                            val updatedWorks = dao.getWorksByDay(work.day)
                            adapter.updateData(updatedWorks)
                        }
                    }
                    .setNegativeButton("いいえ", null)
                    .show()},
            onEditClickListener = { work ->
                val intent = Intent(this, EditWorkActivity::class.java).apply {
                    putExtra("id", work.id)
                    putExtra("day", work.day)
                    putExtra("start_time", work.start_time)
                    putExtra("end_time", work.end_time)
                    putExtra("elapsed_time", work.elapsed_time)
                }
                startActivity(intent)
            })
        recyclerView.adapter = adapter

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDay = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth)
            loadWorkList(selectedDay)
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

    override fun onResume() {
        super.onResume()
        if(selectedDay.isNotEmpty()) {
            loadWorkList(selectedDay)
        }
    }
}