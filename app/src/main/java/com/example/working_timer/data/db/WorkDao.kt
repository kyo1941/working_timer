package com.example.working_timer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * from works WHERE id = :id")
    fun getWork(id: Int): Flow<Work>

    @Query("SELECT * from works WHERE start_day = :day")
    suspend fun getWorksByDay(day: String): List<Work>

    @Insert
    suspend fun insert(work: Work)

    @Query("DELETE FROM works WHERE id = :id")
    suspend fun delete(id: Int)

    @Update
    suspend fun update(work: Work)
}