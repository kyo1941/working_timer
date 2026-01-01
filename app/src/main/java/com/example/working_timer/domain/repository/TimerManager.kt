package com.example.working_timer.domain.repository

import com.example.working_timer.service.TimerState
import kotlinx.coroutines.flow.StateFlow

interface TimerManager {
    val timerState: StateFlow<TimerState>
    fun startTimer()
    fun pauseTimer()
    fun resumeTimer()
    fun stopTimer()
}
