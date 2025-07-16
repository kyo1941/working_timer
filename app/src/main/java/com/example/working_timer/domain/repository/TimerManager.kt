package com.example.working_timer.domain.repository

interface TimerManager {
    fun startTimer()
    fun pauseTimer()
    fun resumeTimer()
    fun stopTimer()
    fun isTimerRunning(): Boolean
    fun getElapsedTime(): Long
    fun setListener(listener: TimerListener)
    fun removeListener()
}

interface TimerListener {
    fun onTimerTick(elapsedTime: Long)
    fun updateUI()
    fun onError(error: String)
}