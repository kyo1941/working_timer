
package com.example.working_timer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {
    @Query("SELECT * from works WHERE id = :id")
    fun getWork(id: Int): Flow<Work>

    @Query("SELECT * from works WHERE day = :day")
    suspend fun getWorksByDay(day: String): List<Work>

    @Insert
    suspend fun insert(work: Work)

    @Query("DELETE FROM works WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("""
    UPDATE works SET 
        day = :day,
        start_time = :startTime,
        end_time = :endTime,
        elapsed_time = :elapsedTime
    WHERE id = :id
    """)
    suspend fun updateWork(
        id: Int,
        day: String,
        startTime: String,
        endTime: String,
        elapsedTime: Int
    )
}