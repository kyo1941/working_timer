package com.example.working_timer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * from works WHERE id = :id")
    fun getWork(id: Int): Flow<Work>

    @Insert
    suspend fun insert(work: Work)
}