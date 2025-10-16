package com.example.working_timer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "works")
data class Work(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,    // 値そのものが意味を持つものを主キーにしない
    val start_day: String,
    val end_day: String,
    val start_time: String,
    val end_time: String,
    val elapsed_time: Long
)