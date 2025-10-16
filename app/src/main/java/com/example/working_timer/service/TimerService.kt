package com.example.working_timer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.working_timer.R
import com.example.working_timer.di.IoDispatcher
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : LifecycleService() {
    @Inject
    lateinit var dataStoreManager: DataStoreManager

    private val binder = LocalBinder()
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var startDate: String? = null
    private var startTimeString: String? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val runnable = object : Runnable {
        override fun run() {
            elapsedTime = System.currentTimeMillis() - startTime
            listener?.onTimerTick(elapsedTime)
            updateNotificationChannel()

            if ((elapsedTime / 1000) % 60 == 0L) {
                lifecycleScope.launch(ioDispatcher) {
                    dataStoreManager.updateElapsedTime(elapsedTime)
                }
            }

            handler.postDelayed(this, 1000)
        }
    }

    private var listener: TimerServiceListener? = null

    private var startTimeCalendar: Calendar = Calendar.getInstance()

    interface TimerServiceListener {
        fun onTimerTick(elapsedTime: Long)
        fun updateUI()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        restoreTimerState()
    }

    private fun restoreTimerState() {
        if (elapsedTime == 0L && startDate == null && startTimeString == null) {
            lifecycleScope.launch(ioDispatcher) {
                val savedElapsedTime = dataStoreManager.getElapsedTimeSync()

                if (savedElapsedTime > 0) {
                    elapsedTime = savedElapsedTime
                    isRunning = false
                    startTimeString = dataStoreManager.getStartTimeSync()
                    startDate = dataStoreManager.getStartDateSync()

                    withContext(Dispatchers.Main) {
                        updateNotificationChannel()
                        listener?.onTimerTick(elapsedTime)
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    fun startTimer() {
        startTime = System.currentTimeMillis() - elapsedTime
        isRunning = true
        startTimeCalendar = Calendar.getInstance()

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = sdfDate.format(startTimeCalendar.time)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = sdfTime.format(startTimeCalendar.time)

        lifecycleScope.launch(ioDispatcher) {
            dataStoreManager.saveTimerState(
                startDate = formattedDate,
                startTime = formattedTime,
                elapsedTime = elapsedTime
            )
        }

        startForegroundService()
        handler.postDelayed(runnable, 0)
    }

    fun stopTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false

        elapsedTime = 0
        listener?.onTimerTick(elapsedTime) // 停止時に0を通知

        lifecycleScope.launch(ioDispatcher) {
            dataStoreManager.clearTimerState()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(1)
        removeListener()
    }

    fun pauseTimer() {
        handler.removeCallbacks(runnable)
        isRunning = false

        lifecycleScope.launch(ioDispatcher) {
            dataStoreManager.updateElapsedTime(elapsedTime)
        }

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
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
        val notificationIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(Routes.TimerDeepLink.routes)).apply {
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
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
            .setSmallIcon(R.drawable.ic_launcher_playstore)
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
                R.drawable.ic_launcher_playstore, "一時停止", pausePendingIntent
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
                R.drawable.ic_launcher_playstore, "再開", resumePendingIntent
            )
        }

        val notification = builder.build()
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, notification)
    }

    private fun startForegroundService() {
        val channelId = "timer_channel"
        val notificationIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(Routes.TimerDeepLink.routes)).apply {
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("00:00   ${if (isRunning) "労働中" else "休憩中"}")
            .setSmallIcon(R.drawable.ic_launcher_playstore)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
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
    }
}