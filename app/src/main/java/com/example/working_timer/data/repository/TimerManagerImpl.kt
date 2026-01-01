package com.example.working_timer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.service.TimerService
import com.example.working_timer.service.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TimerManager {
    private var timerService: TimerService? = null
    private var isBound = false

    private var pendingStart = false

    private val _timerState = MutableStateFlow<TimerState>(TimerState())
    override val timerState = _timerState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            isBound = true

            if (pendingStart) {
                timerService?.startTimer()
                pendingStart = false
            }

            job?.cancel()
            job = scope.launch {
                timerService?.serviceState?.collect { state ->
                    _timerState.value = state
                }
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
            timerService = null
        }
    }

    init {
        bindService()
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

    private fun bindService() {
        Intent(context, TimerService::class.java).also { intent ->
            val bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                pendingStart = false
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