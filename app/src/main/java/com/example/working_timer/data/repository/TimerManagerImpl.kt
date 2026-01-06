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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TimerManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TimerManager {
    private var isBound = false

    private var pendingStart = false

    private val timerServiceFlow = MutableStateFlow<TimerService?>(null)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val timerState: StateFlow<TimerState> =
        timerServiceFlow
            .flatMapLatest { service ->
                service?.serviceState ?: flowOf(TimerState())
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TimerState()
            )

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder

            timerServiceFlow.value = binder.getService()
            isBound = true

            if (pendingStart) {
                timerServiceFlow.value?.startTimer()
                pendingStart = false
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBound = false
            timerServiceFlow.value = null
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
        timerServiceFlow.value?.startTimer()
        pendingStart = false
    }

    override fun pauseTimer() {
        timerServiceFlow.value?.pauseTimer()
    }

    override fun resumeTimer() {
        timerServiceFlow.value?.resumeTimer()
    }

    override fun stopTimer() {
        timerServiceFlow.value?.stopTimer()
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
        timerServiceFlow.value = null
    }
}