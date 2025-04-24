package com.example.working_timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimerService : Service() {

    private val binder = LocalBinder()
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var startDate: String? = null
    private var startTimeString: String? = null
    private var startTimeMills: Long = 0
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private val runnable = object : Runnable {
        override fun run() {
            elapsedTime = System.currentTimeMillis() - startTime
            listener?.onTimerTick(elapsedTime)
            handler.postDelayed(this, 1000) // 1秒ごとに更新
        }
    }

    private var listener: TimerServiceListener? = null

    private val PREFS_NAME = "TimerPrefs"
    private val START_DATE_KEY = "startDate"
    private val START_TIME_STRING_KEY = "startTimeString"
    private val START_TIME_MILLS_KEY = "startTimeMills"
    private val ELAPSED_TIME_KEY = "elapsedTime"

    private var startTimeCalendar: Calendar = Calendar.getInstance() // タイマー開始時の日付を保持

    interface TimerServiceListener {
        fun onTimerTick(elapsedTime: Long)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // SharedPreferences から elapsedTime を復元
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        elapsedTime = prefs.getLong(ELAPSED_TIME_KEY, 0L)
        startTimeString = prefs.getString(START_TIME_STRING_KEY, "")
        startDate = prefs.getString(START_DATE_KEY, "")
        startTimeMills = prefs.getLong(START_TIME_MILLS_KEY, 0L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // サービスが開始されたときに呼ばれる
        startForegroundService()
        return START_STICKY
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

        val sdfTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = sdfTime.format(startTimeCalendar.time) // 開始時の時間を取得

        // SharedPreferences に開始日を保存
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(START_DATE_KEY, formattedDate)
        editor.putLong(ELAPSED_TIME_KEY, elapsedTime)
        editor.putString(START_TIME_STRING_KEY, formattedTime)
        editor.putLong(START_TIME_MILLS_KEY, System.currentTimeMillis())
        editor.apply()

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
        editor.remove(START_TIME_MILLS_KEY)
        editor.apply()
    }

    fun pauseTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(ELAPSED_TIME_KEY, elapsedTime)
        editor.apply()
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
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val channelId = "timer_channel"
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        ) // or FLAG_UPDATE_CURRENT depending on your needs

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Working Timer")
            .setContentText("Timer is running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
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