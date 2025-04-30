package com.example.working_timer

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.CalendarView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope
import com.example.working_timer.data.AppDatabase
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Calendar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.core.content.ContextCompat
import com.example.working_timer.data.Work
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import java.text.SimpleDateFormat


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

        val addButton = findViewById<ImageButton>(R.id.addButton)
        val sumButton = findViewById<ImageButton>(R.id.sumButton)

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

                            val updatedWorks = dao.getWorksByDay(work.start_day)
                            adapter.updateData(updatedWorks)
                        }
                    }
                    .setNegativeButton("いいえ", null)
                    .show()},
            onEditClickListener = { work ->
                val intent = Intent(this, EditWorkActivity::class.java).apply {
                    putExtra("id", work.id)
                    putExtra("start_day", work.start_day)
                    putExtra("end_day", work.end_day)
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

        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        sumButton.setOnClickListener {
            val constraintsBuilder = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now())

            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("計算する期間を選択してください")
                .setCalendarConstraints(constraintsBuilder.build())
                .build()

            dateRangePicker.show(supportFragmentManager, "date_range_picker")

            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                val startDate = selection.first ?: return@addOnPositiveButtonClickListener
                val endDate = selection.second ?: return@addOnPositiveButtonClickListener

                lifecycleScope.launch {
                    val database = AppDatabase.getDatabase(applicationContext)
                    val dao = database.workDao()

                    val calendar = Calendar.getInstance()

                    calendar.timeInMillis = startDate
                    var totalTime = 0L

                    while(calendar.timeInMillis <= endDate) {
                        val day = sdf.format(calendar.time)
                        val works = dao.getWorksByDay(day)

                        for(work in works) {
                            totalTime += work.elapsed_time
                        }

                        calendar.add(Calendar.DAY_OF_MONTH, 1)
                    }

                    val totalHours = totalTime / 3600
                    val totalMinutes = (totalTime % 3600) / 60
                    AlertDialog.Builder(this@LogViewActivity)
                        .setTitle("合計時間")
                        .setMessage(if (totalHours > 0) "${totalHours}時間 ${totalMinutes}分" else "${totalMinutes}分")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }

        addButton.setOnClickListener {
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

        val today = Calendar.getInstance()
        selectedDay = String.format(Locale.getDefault(), "%04d/%02d/%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
        loadWorkList(selectedDay)
    }

    override fun onResume() {
        super.onResume()
        if(selectedDay.isNotEmpty()) {
            loadWorkList(selectedDay)
        }
    }
}