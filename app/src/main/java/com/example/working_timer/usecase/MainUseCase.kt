package com.example.working_timer.usecase

interface MainUseCase {
    fun startTimer()
    fun pauseTimer()
    fun resumeTimer()
    fun stopTimer()

    suspend fun dismissSaveDialog()

    suspend fun saveWork()
    suspend fun discardWork()
}