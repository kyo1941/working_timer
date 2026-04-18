package com.example.working_timer.usecase

interface MainUseCase {
    fun startTimer()
    fun pauseTimer()
    fun resumeTimer()
    fun stopTimer()

    fun dismissSaveDialog()

    fun saveWork()
    fun discardWork()
}