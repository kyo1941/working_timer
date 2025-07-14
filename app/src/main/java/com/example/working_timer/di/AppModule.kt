package com.example.working_timer.di

import android.content.Context
import androidx.room.Room
import com.example.working_timer.data.AppDatabase
import com.example.working_timer.data.WorkDao
import com.example.working_timer.data.repository.DataStoreManagerImpl
import com.example.working_timer.data.repository.TimerManagerImpl
import com.example.working_timer.data.repository.WorkRepositoryImpl
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindWorkRepository(workRepositoryImpl: WorkRepositoryImpl): WorkRepository

    @Binds
    @Singleton
    abstract fun bindTimerManager(timerManagerImpl: TimerManagerImpl): TimerManager

    @Binds
    @Singleton
    abstract fun bindDataStoreManager(dataStoreManagerImpl: DataStoreManagerImpl): DataStoreManager
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkDao(appDatabase: AppDatabase): WorkDao {
        return appDatabase.workDao()
    }
}