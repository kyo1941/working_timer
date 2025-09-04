package com.example.working_timer.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.working_timer.di.IoDispatcher
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.util.SharedPrefKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManagerImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DataStoreManager {
    companion object {
        private val START_DATE_KEY = stringPreferencesKey(SharedPrefKeys.START_DATE_KEY)
        private val START_TIME_STRING_KEY = stringPreferencesKey(SharedPrefKeys.START_TIME_STRING_KEY)
        private val ELAPSED_TIME_KEY = longPreferencesKey(SharedPrefKeys.ELAPSED_TIME_KEY)
    }

    override suspend fun saveTimerState(
        startDate: String,
        startTime: String,
        elapsedTime: Long
    ) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[START_DATE_KEY] = startDate
                preferences[START_TIME_STRING_KEY] = startTime
                preferences[ELAPSED_TIME_KEY] = elapsedTime
            }
        }
    }

    override suspend fun updateElapsedTime(elapsedTime: Long) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[ELAPSED_TIME_KEY] = elapsedTime
            }
        }
    }

    override suspend fun clearTimerState() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences.remove(START_DATE_KEY)
                preferences.remove(START_TIME_STRING_KEY)
                preferences.remove(ELAPSED_TIME_KEY)
            }
        }
    }

    override fun getElapsedTime(): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[ELAPSED_TIME_KEY] ?: 0L
        }
    }

    override fun getStartDate(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[START_DATE_KEY]
        }
    }

    override fun getStartTime(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[START_TIME_STRING_KEY]
        }
    }

    override suspend fun getElapsedTimeSync(): Long {
        return withContext(ioDispatcher) {
            getElapsedTime().first()
        }
    }

    override suspend fun getStartDateSync(): String? {
        return withContext(ioDispatcher) {
            getStartDate().first()
        }
    }

    override suspend fun getStartTimeSync(): String? {
        return withContext(ioDispatcher) {
            getStartTime().first()
        }
    }
}