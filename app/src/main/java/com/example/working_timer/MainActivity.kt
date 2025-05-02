package com.example.working_timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.util.Log
import androidx.lifecycle.lifecycleScope
import android.app.AlertDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.working_timer.data.AppDatabase
import com.example.working_timer.data.Work
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.working_timer.data.WorkDao
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TimerService.TimerServiceListener {

    private lateinit var statusTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button

    private var timerService: TimerService? = null
    private var isBound = false

    private var transitionToLogView = false

    private var endTimeCalendar: Calendar = Calendar.getInstance()

    private val PREFS_NAME = "TimerPrefs"
    private val START_DATE_KEY = "startDate"
    private val START_TIME_STRING_KEY = "startTimeString"
    private val ELAPSED_TIME_KEY = "elapsedTime"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Snackbar.make(findViewById(android.R.id.content), "通知が許可されました。", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.bottomNavigationView)
                        .show()
            } else {
                Snackbar.make(findViewById(android.R.id.content), "通知が拒否されました。", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.bottomNavigationView)
                        .show()
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            isBound = true
            timerService?.setListener(this@MainActivity)
            updateUI()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val hasRequestedPermission = prefs.getBoolean("hasRequestedPermission", false)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED && !hasRequestedPermission
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                prefs.edit().putBoolean("hasRequestedPermission", true).apply()
            }
        }

        statusTextView = findViewById(R.id.statusTextView)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        pauseButton = findViewById(R.id.pauseButton)
        resumeButton = findViewById(R.id.resumeButton)

        startButton.setOnClickListener {
            if (isBound && timerService != null) {
                timerService?.setListener(this)
            }
            timerService?.startTimer()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(android.R.id.content), "通知をONにすると、タイマーの進行状況が確認できます。", Snackbar.LENGTH_SHORT)
                        .setAnchorView(R.id.bottomNavigationView)
                        .show()
            }

            updateUI()
        }
        stopButton.setOnClickListener {
            if (!isBound || timerService == null) return@setOnClickListener

            timerService?.pauseTimer()

            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val elapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
            val startDate = prefs.getString(START_DATE_KEY, "")
            val startTime = prefs.getString(START_TIME_STRING_KEY, "")


            // 経過時間を表示形式に変換
            val rep_sec_time = elapsedTime / 1000
            val hours = (rep_sec_time / 3600).toInt()
            val minutes = ((rep_sec_time / 60) % 60).toInt()
            val seconds = (rep_sec_time % 60).toInt()
            val formattedTime = if (hours > 0) {
                String.format("%2d時間 %2d分", hours, minutes)
            } else {
                String.format("%2d分", minutes)
            }

            // AlertDialogを作成
            var builder = AlertDialog.Builder(this)
            builder.setTitle("確認")
            builder.setMessage("""
                開始日 ： $startDate
                経過時間 ： $formattedTime
                
                今回の作業記録を保存しますか？
            """.trimIndent())

            builder.setPositiveButton("保存") { dialog, which ->
                if (startDate == null || startTime == null) {
                    // 画面に「正しく表示できなかった旨」を伝えたい
                    return@setPositiveButton
                }

                // YESボタンがクリックされた時の処理
                endTimeCalendar = Calendar.getInstance()
                val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val endDate = sdfDate.format(endTimeCalendar.time)

                val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                val endTime = sdfTime.format(Date())
                val saveElapsedTime = (elapsedTime / 1000 / 60) * 60

                if (elapsedTime < 60000) {
                    AlertDialog.Builder(this)
                        .setTitle("注意")
                        .setMessage("1分未満の作業は保存できません。\n再開または破棄を選択してください。")
                        .setPositiveButton("再開") { _, _ ->
                            timerService?.resumeTimer()
                            updateUI()
                        }
                        .setNegativeButton("破棄") { _, _ ->
                            timerService?.stopTimer()
                            updateUI()
                        }
                        .show()
                    return@setPositiveButton
                }

                val work = Work(
                    start_day = startDate,
                    end_day = endDate,
                    start_time = startTime,
                    end_time = endTime,
                    elapsed_time = saveElapsedTime.toInt()
                )

                Log.d("MainActivity", "Work created: $work")

                val database = AppDatabase.getDatabase(applicationContext)
                val workDao = database.workDao()

                lifecycleScope.launch {
                    workDao.insert(work)
                    Log.d("MainActivity", "Work inserted: $work")
                }

                timerService?.stopTimer()
                unbindService(connection)
                isBound = false
                timerService = null
                updateUI()

                // LogView への遷移（任意）
                transitionToLogView = true
                val intent = Intent(this, LogViewActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }

            builder.setNeutralButton("再開") { dialog, which ->
                // 中断ボタンがクリックされた時の処理
                timerService?.resumeTimer()
                updateUI()
            }

            builder.setNegativeButton("破棄") { dialog, which ->
                // NOボタンがクリックされた時の処理
                timerService?.stopTimer()
                updateUI()
            }

            // AlertDialogを表示
            builder.show()

            updateUI()
        }
        pauseButton.setOnClickListener {
            timerService?.pauseTimer()
            updateUI()
        }
        resumeButton.setOnClickListener {
            timerService?.resumeTimer()
            updateUI()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.navigation_timer
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if (item.itemId != bottomNavigationView.selectedItemId) {
                when (item.itemId) {
                    R.id.navigation_timer -> {
                        // 現在の画面なので何もしない
                        true
                    }

                    R.id.navigation_log -> {
                        transitionToLogView = true
                        val intent = Intent(this, LogViewActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                        true
                    }

                    else -> false
                }
            } else {
                true
            }
        }

        // SharedPreferences から elapsedTime を読み込む
        val prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        val elapsedTime = prefs.getLong("elapsedTime", 0L)
        updateTimerText(elapsedTime) // 読み込んだ elapsedTime で UI を更新
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (transitionToLogView && timerService?.isTimerRunning() == true) {
            timerService?.pauseTimer()
        }
    }

    override fun onTimerTick(elapsedTime: Long) {
        updateTimerText(elapsedTime)
    }

    private fun updateTimerText(elapsedTime: Long) {
        // SharedPreferences に開始日を保存
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong("elapsedTime", elapsedTime)
        editor.apply()

        val rep_sec_time = elapsedTime / 1000
        val hours = (rep_sec_time / 3600).toInt()
        val minutes = ((rep_sec_time / 60) % 60).toInt()
        val seconds = (rep_sec_time % 60).toInt()
        val formattedTime = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
        timerTextView.text = formattedTime
    }

    private fun updateStatusAndButtons(status: String) {
        statusTextView.text = status

        when (status) {
            "労働中" -> statusTextView.setTextColor(ContextCompat.getColor(this, R.color.start_button_color))
            "休憩中" -> statusTextView.setTextColor(ContextCompat.getColor(this, R.color.pause_button_color))
            else -> statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
        }

        startButton.visibility = if (status == "") Button.VISIBLE else Button.GONE
        stopButton.visibility = if (status == "労働中") Button.VISIBLE else Button.GONE
        pauseButton.visibility = if (status == "労働中") Button.VISIBLE else Button.GONE
        resumeButton.visibility = if (status == "休憩中") Button.VISIBLE else Button.GONE
    }

    override fun updateUI() {
        runOnUiThread {
            if (isBound && timerService != null) {
                val isRunning = timerService!!.isTimerRunning()
                val elapsedTime = timerService!!.getElapsedTime()

                updateTimerText(elapsedTime)

                val status = when {
                    isRunning -> "労働中"
                    elapsedTime > 0 -> "休憩中"
                    else -> ""
                }
                updateStatusAndButtons(status)
            } else {
                // Serviceに接続されていない場合、UIを初期状態に戻す
                updateStatusAndButtons("")
                updateTimerText(0)
            }
        }
    }
}