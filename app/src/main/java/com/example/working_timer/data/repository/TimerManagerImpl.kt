package com.example.working_timer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.example.working_timer.domain.repository.TimerListener
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TimerManager {
    private var timerService: TimerService? = null
    private var isBound = false
    private var listener: TimerListener? = null

    private var pendingStart = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            isBound = true
            timerService?.setListener(object : TimerService.TimerServiceListener {
                override fun onTimerTick(elapsedTime: Long) {
                    listener?.onTimerTick(elapsedTime)
                }
                override fun updateUI() {
                    listener?.updateUI()
                }
            })

            if (pendingStart) {
                timerService?.startTimer()
                pendingStart = false
                listener?.updateUI()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    override fun startTimer() {
        if (!isBound) {
            pendingStart = true
            bindService()
            return
        }
        timerService?.startTimer()
        pendingStart = false
    }

    override fun pauseTimer() {
        timerService?.pauseTimer()
    }

    override fun resumeTimer() {
        timerService?.resumeTimer()
    }

    override fun stopTimer() {
        timerService?.stopTimer()
        unbindService()
    }

    override fun isTimerRunning(): Boolean {
        return timerService?.isTimerRunning() ?: false
    }

    override fun getElapsedTime(): Long {
        return timerService?.getElapsedTime() ?: 0L
    }

    override fun setListener(listener: TimerListener) {
        this.listener = listener
    }

    override fun removeListener() {
        this.listener = null
    }

    private fun bindService() {
        Intent(context, TimerService::class.java).also { intent ->
            val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                pendingStart = false
                listener?.onError("タイマーの開始に失敗しました。")
            }
        }
    }

    private fun unbindService() {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }
}