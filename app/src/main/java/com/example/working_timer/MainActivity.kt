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
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
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

    private var transiotionToLogView = false

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

        statusTextView = findViewById(R.id.statusTextView)
        timerTextView = findViewById(R.id.timerTextView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        pauseButton = findViewById(R.id.pauseButton)
        resumeButton = findViewById(R.id.resumeButton)

        startButton.setOnClickListener {
            timerService?.startTimer()
            updateUI()
        }
        stopButton.setOnClickListener {
            timerService?.pauseTimer()

            val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
            val elapsedTime = prefs.getLong("elapsedTime", 0L)
            val startDate = prefs.getString("startDate", "") ?: "" // デフォルト値を設定

            // 経過時間を表示形式に変換
            val rep_sec_time = elapsedTime / 1000
            val hours = (rep_sec_time / 3600).toInt()
            val minutes = ((rep_sec_time / 60) % 60).toInt()
            val seconds = (rep_sec_time % 60).toInt()
            val formattedTime = if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }

            // AlertDialogを作成
            var bulider = AlertDialog.Builder(this)
            bulider.setTitle("確認")
            bulider.setMessage("""
                開始日 ： $startDate
                経過時間 ： $formattedTime
                
                今回の作業記録を保存しますか？
            """.trimIndent())

            bulider.setPositiveButton("はい") { dialog, which ->
                // YESボタンがクリックされた時の処理
                timerService?.stopTimer()
                val intent = Intent(this, LogViewActivity::class.java)
                intent.putExtra("startDate", startDate)
                intent.putExtra("elapsedTime", elapsedTime)
                startActivity(intent)
                updateUI()

                // Shared Preferences のデータを削除
                val editor = prefs.edit()
                editor.remove("startDate")
                editor.remove("elapsedTime")
                editor.apply()
            }

            bulider.setNeutralButton("記録を再開") { dialog, which ->
                // 中断ボタンがクリックされた時の処理
                timerService?.resumeTimer()
                updateUI()
            }

            bulider.setNegativeButton("いいえ") { dialog, which ->
                // NOボタンがクリックされた時の処理
                timerService?.stopTimer()
                updateUI()

                // Shared Preferences のデータを削除
                val editor = prefs.edit()
                editor.remove("startDate")
                editor.remove("elapsedTime")
                editor.apply()
            }

            // AlertDialogを表示
            bulider.show()

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
                        transiotionToLogView = true
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
        val prefs = getSharedPreferences("TimerPrefs", Context.MODE_PRIVATE)
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
        if (transiotionToLogView && isBound) {
            timerService?.removeListener()
            unbindService(connection)
            isBound = false
            timerService = null
            transiotionToLogView = false
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

    private fun updateUI() {
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