package com.example.working_timer.domain.repository

import com.example.working_timer.data.db.Work
import kotlinx.coroutines.flow.Flow

interface WorkRepository {
    fun getWork(id: Int): Flow<Work>
    suspend fun getWorksByDay(day: String): List<Work>
    suspend fun insert(work: Work)
    suspend fun delete(id: Int)
    suspend fun update(work: Work)
}