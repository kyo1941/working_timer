package com.example.working_timer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Work::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao
}