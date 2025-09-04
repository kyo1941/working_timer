package com.example.working_timer.data.repository

import com.example.working_timer.data.db.Work
import com.example.working_timer.data.db.WorkDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WorkRepositoryImplTest {

    // テストケース独自の定数
    companion object {
        private const val TEST_WORK_ID = 1
        private const val TEST_START_DAY = "2025-01-04"
        private const val TEST_END_DAY = "2025-01-04"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_END_TIME = "17:00"
        private const val TEST_ELAPSED_TIME = 480
        private const val TEST_DAY = "2025-01-04"
        private const val UPDATED_ELAPSED_TIME = 500
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWorkDao: WorkDao
    private lateinit var workRepository: WorkRepositoryImpl

    // テスト用のデフォルトワーク
    private val defaultTestWork = createTestWork()
    private val defaultTestWorkList = listOf(
        defaultTestWork,
        createTestWork(
            id = 2,
            startTime = "18:00",
            endTime = "20:00",
            elapsedTime = 120
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockWorkDao = mockk(relaxed = true)
        workRepository = WorkRepositoryImpl(mockWorkDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド群
    private fun setupMockWorkFlow(work: Work = defaultTestWork) {
        val workFlow = flowOf(work)
        every { mockWorkDao.getWork(work.id) } returns workFlow
    }

    private fun setupMockWorksByDay(works: List<Work> = defaultTestWorkList, day: String = TEST_DAY) {
        coEvery { mockWorkDao.getWorksByDay(day) } returns works
    }

    private fun setupEmptyWorksByDay(day: String = TEST_DAY) {
        setupMockWorksByDay(emptyList(), day)
    }

    private fun setupInsertScenario(work: Work = defaultTestWork) {
        coEvery { mockWorkDao.insert(work) } returns Unit
    }

    private fun setupDeleteScenario(workId: Int = TEST_WORK_ID) {
        coEvery { mockWorkDao.delete(workId) } returns Unit
    }

    private fun setupUpdateScenario(work: Work) {
        coEvery { mockWorkDao.update(work) } returns Unit
    }

    @Test
    fun `getWork実行時にDaoからWorkのFlowが返される`() = runTest {
        // Given
        setupMockWorkFlow()

        // When
        val result = workRepository.getWork(TEST_WORK_ID)

        // Then
        val resultWork = result.first()
        assertEquals(defaultTestWork, resultWork)
        verify { mockWorkDao.getWork(TEST_WORK_ID) }
    }

    @Test
    fun `getWorksByDay実行時に指定した日のWorkリストが返される`() = runTest {
        // Given
        setupMockWorksByDay()

        // When
        val result = workRepository.getWorksByDay(TEST_DAY)

        // Then
        assertEquals(defaultTestWorkList, result)
        assertEquals(2, result.size)
        coVerify { mockWorkDao.getWorksByDay(TEST_DAY) }
    }

    @Test
    fun `getWorksByDay実行時にWorkが見つからない場合は空のリストが返される`() = runTest {
        // Given
        val emptyDay = "2025-01-05"
        setupEmptyWorksByDay(emptyDay)

        // When
        val result = workRepository.getWorksByDay(emptyDay)

        // Then
        assertTrue(result.isEmpty())
        coVerify { mockWorkDao.getWorksByDay(emptyDay) }
    }

    @Test
    fun `insert実行時に正しいWorkでDaoのinsertが呼ばれる`() = runTest {
        // Given
        setupInsertScenario()

        // When
        workRepository.insert(defaultTestWork)
        advanceUntilIdle()

        // Then
        coVerify { mockWorkDao.insert(defaultTestWork) }
    }

    @Test
    fun `delete実行時に正しいIDでDaoのdeleteが呼ばれる`() = runTest {
        // Given
        setupDeleteScenario()

        // When
        workRepository.delete(TEST_WORK_ID)
        advanceUntilIdle()

        // Then
        coVerify { mockWorkDao.delete(TEST_WORK_ID) }
    }

    @Test
    fun `update実行時に正しいWorkでDaoのupdateが呼ばれる`() = runTest {
        // Given
        val updatedWork = defaultTestWork.copy(elapsed_time = UPDATED_ELAPSED_TIME)
        setupUpdateScenario(updatedWork)

        // When
        workRepository.update(updatedWork)
        advanceUntilIdle()

        // Then
        coVerify { mockWorkDao.update(updatedWork) }
    }

    @Test
    fun `複数のWorkに対してgetWorksByDayが正しく動作する`() = runTest {
        // Given
        val multipleWorks = listOf(
            createTestWork(id = 1, elapsedTime = 240),
            createTestWork(id = 2, elapsedTime = 360),
            createTestWork(id = 3, elapsedTime = 180)
        )
        setupMockWorksByDay(multipleWorks)

        // When
        val result = workRepository.getWorksByDay(TEST_DAY)

        // Then
        assertEquals(3, result.size)
        assertEquals(240, result[0].elapsed_time)
        assertEquals(360, result[1].elapsed_time)
        assertEquals(180, result[2].elapsed_time)
        coVerify { mockWorkDao.getWorksByDay(TEST_DAY) }
    }

    @Test
    fun `異なるIDのWorkに対してgetWorkが正しく動作する`() = runTest {
        // Given
        val differentWork = createTestWork(id = 999, startTime = "10:00")
        setupMockWorkFlow(differentWork)

        // When
        val result = workRepository.getWork(999)

        // Then
        val resultWork = result.first()
        assertEquals(999, resultWork.id)
        assertEquals("10:00", resultWork.start_time)
        verify { mockWorkDao.getWork(999) }
    }

    private fun createTestWork(
        id: Int = TEST_WORK_ID,
        startDay: String = TEST_START_DAY,
        endDay: String = TEST_END_DAY,
        startTime: String = TEST_START_TIME,
        endTime: String = TEST_END_TIME,
        elapsedTime: Int = TEST_ELAPSED_TIME
    ): Work {
        return Work(
            id = id,
            start_day = startDay,
            end_day = endDay,
            start_time = startTime,
            end_time = endTime,
            elapsed_time = elapsedTime
        )
    }
}
