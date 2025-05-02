package com.example.working_timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimerService : Service() {

    private val binder = LocalBinder()
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var startDate: String? = null
    private var startTimeString: String? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            elapsedTime = System.currentTimeMillis() - startTime
            listener?.onTimerTick(elapsedTime)
            updateNotificationChannel()
            handler.postDelayed(this, 1000) // 1秒ごとに更新
        }
    }

    private var listener: TimerServiceListener? = null

    private val PREFS_NAME = "TimerPrefs"
    private val START_DATE_KEY = "startDate"
    private val START_TIME_STRING_KEY = "startTimeString"
    private val ELAPSED_TIME_KEY = "elapsedTime"

    private var startTimeCalendar: Calendar = Calendar.getInstance() // タイマー開始時の日付を保持

    interface TimerServiceListener {
        fun onTimerTick(elapsedTime: Long)
        fun updateUI()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // SharedPreferences から elapsedTime を復元
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        elapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
        startTimeString = prefs.getString(START_TIME_STRING_KEY, "")
        startDate = prefs.getString(START_DATE_KEY, "")
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startTimer() {
        startTime = System.currentTimeMillis() - elapsedTime
        isRunning = true
        startTimeCalendar = Calendar.getInstance() // 開始時の日付を記録

        val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val formattedDate = sdfDate.format(startTimeCalendar.time) // 開始時の日付を取得

        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = sdfTime.format(startTimeCalendar.time) // 開始時の時間を取得

        // SharedPreferences に開始日を保存
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(START_DATE_KEY, formattedDate)
        editor.putLong(ELAPSED_TIME_KEY, elapsedTime)
        editor.putString(START_TIME_STRING_KEY, formattedTime)
        editor.apply()

        startForegroundService()

        handler.postDelayed(runnable, 0)
    }

    fun stopTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false

        elapsedTime = 0
        listener?.onTimerTick(elapsedTime) // 停止時に0を通知

        // SharedPreferences から elapsedTime を削除
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove(START_DATE_KEY)
        editor.remove(ELAPSED_TIME_KEY)
        editor.remove(START_TIME_STRING_KEY)
        editor.apply()

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun pauseTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(ELAPSED_TIME_KEY, elapsedTime)
        editor.apply()
        updateNotificationChannel()
    }

    fun resumeTimer() {
        startTime = System.currentTimeMillis() - elapsedTime
        isRunning = true
        handler.postDelayed(runnable, 0)
    }

    fun isTimerRunning(): Boolean {
        return isRunning
    }

    fun getElapsedTime(): Long {
        return elapsedTime
    }

    fun setListener(listener: TimerServiceListener) {
        this.listener = listener
    }

    fun removeListener() {
        this.listener = null
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "timer_channel"
            val channelName = "Timer Service Channel"
            val channelDescription = "Channel for Timer Service"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = "timer_channel"
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val repSecTime = elapsedTime / 1000
        val hours = (repSecTime / 3600).toInt()
        val minutes = ((repSecTime / 60) % 60).toInt()
        val seconds = (repSecTime % 60).toInt()
        val formattedTime = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }


        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$formattedTime   ${if (isRunning) "労働中" else "休憩中"}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)

        if (isRunning) {
            // 中断ボタンを追加
            val pauseIntent = Intent(this, TimerActionReceiver::class.java).apply {
                action = "ACTION_PAUSE_TIMER"
            }
            val pausePendingIntent = PendingIntent.getBroadcast(
                this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground, "一時停止", pausePendingIntent
            )
        } else {
            // 再開ボタンを追加
            val resumeIntent = Intent(this, TimerActionReceiver::class.java).apply {
                action = "ACTION_RESUME_TIMER"
            }
            val resumePendingIntent = PendingIntent.getBroadcast(
                this, 0, resumeIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_launcher_foreground, "再開", resumePendingIntent
            )
        }

        val notification = builder.build()
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, notification)
    }

    private fun startForegroundService() {
        val channelId = "timer_channel"
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("00:00   ${if (isRunning) "労働中" else "休憩中"}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        when (action) {
            "pause" -> pauseTimer()
            "resume" -> resumeTimer()
            else -> Log.e("TimerService", "Unknown action: $action")
        }
        listener?.updateUI()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)

        // elapsedTime を SharedPreferences に保存
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(ELAPSED_TIME_KEY, elapsedTime)
        editor.apply()
    }
}