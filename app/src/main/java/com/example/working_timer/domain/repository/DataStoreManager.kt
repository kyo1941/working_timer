package com.example.working_timer.domain.repository

import kotlinx.coroutines.flow.Flow

interface DataStoreManager {
    suspend fun saveTimerState(startDate: String, startTime: String, elapsedTime: Long)
    suspend fun updateElapsedTime(elapsedTime: Long)
    suspend fun clearTimerState()

    fun getElapsedTime(): Flow<Long>
    fun getStartDate(): Flow<String?>
    fun getStartTime(): Flow<String?>

    suspend fun getElapsedTimeSync(): Long
    suspend fun getStartDateSync(): String?
    suspend fun getStartTimeSync(): String?
}