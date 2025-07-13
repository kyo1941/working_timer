package com.example.working_timer.data.repository

import com.example.working_timer.data.Work
import com.example.working_timer.data.WorkDao
import com.example.working_timer.domain.repository.WorkRepository
import javax.inject.Inject

class WorkRepositoryImpl @Inject constructor(
    private val workDao: WorkDao
) : WorkRepository {
    override fun getWork(id: Int) = workDao.getWork(id)

    override suspend fun getWorksByDay(day: String) = workDao.getWorksByDay(day)

    override suspend fun insert(work: Work) = workDao.insert(work)

    override suspend fun delete(id: Int) = workDao.delete(id)

    override suspend fun update(work: Work) = workDao.update(work)
}