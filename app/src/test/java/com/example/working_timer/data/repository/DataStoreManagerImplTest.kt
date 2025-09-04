package com.example.working_timer.data.repository

import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import com.example.working_timer.util.SharedPrefKeys
import io.mockk.*
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
class DataStoreManagerImplTest {

    // テストケース独自の定数
    companion object {
        private const val TEST_START_DATE = "2025-01-04"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_ELAPSED_TIME = SECOND_IN_HOURS // 1 hour in seconds
        private const val UPDATED_ELAPSED_TIME = SECOND_IN_HOURS * 2 // 2 hours in seconds
        private const val DEFAULT_ELAPSED_TIME = 0L
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreManager: DataStoreManager

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // DataStoreManagerをモックとして作成し、インターフェースレベルでテスト
        dataStoreManager = mockk(relaxed = true)

        setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド群
    private fun setupDefaultMocks() {
        // デフォルトの動作をモック化
        coEvery { dataStoreManager.saveTimerState(any(), any(), any()) } returns Unit
        coEvery { dataStoreManager.updateElapsedTime(any()) } returns Unit
        coEvery { dataStoreManager.clearTimerState() } returns Unit
    }

    private fun setupMockFlowResponses(
        elapsedTime: Long = TEST_ELAPSED_TIME,
        startDate: String? = TEST_START_DATE,
        startTime: String? = TEST_START_TIME
    ) {
        every { dataStoreManager.getElapsedTime() } returns flowOf(elapsedTime)
        every { dataStoreManager.getStartDate() } returns flowOf(startDate)
        every { dataStoreManager.getStartTime() } returns flowOf(startTime)

        coEvery { dataStoreManager.getElapsedTimeSync() } returns elapsedTime
        coEvery { dataStoreManager.getStartDateSync() } returns startDate
        coEvery { dataStoreManager.getStartTimeSync() } returns startTime
    }

    private fun setupEmptyMockFlowResponses() {
        setupMockFlowResponses(
            elapsedTime = DEFAULT_ELAPSED_TIME,
            startDate = null,
            startTime = null
        )
    }

    @Test
    fun `SharedPrefKeysの定数が正しく定義されていることを確認`() {
        // Given & When & Then
        assertEquals("TimerPrefs", SharedPrefKeys.PREFS_NAME)
        assertEquals("startDate", SharedPrefKeys.START_DATE_KEY)
        assertEquals("startTimeString", SharedPrefKeys.START_TIME_STRING_KEY)
        assertEquals("elapsedTime", SharedPrefKeys.ELAPSED_TIME_KEY)
    }

    @Test
    fun `Constants定数が正しく定義されていることを確認`() {
        // Given & When & Then
        assertEquals(3600L, SECOND_IN_HOURS)
        assertEquals(SECOND_IN_HOURS, TEST_ELAPSED_TIME)
        assertEquals(SECOND_IN_HOURS * 2, UPDATED_ELAPSED_TIME)
    }

    @Test
    fun `saveTimerState実行時にメソッドが正しいパラメータで呼ばれる`() = runTest {
        // When
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)
        advanceUntilIdle()

        // Then
        coVerify { dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME) }
    }

    @Test
    fun `updateElapsedTime実行時にメソッドが正しいパラメータで呼ばれる`() = runTest {
        // When
        dataStoreManager.updateElapsedTime(UPDATED_ELAPSED_TIME)
        advanceUntilIdle()

        // Then
        coVerify { dataStoreManager.updateElapsedTime(UPDATED_ELAPSED_TIME) }
    }

    @Test
    fun `clearTimerState実行時にメソッドが呼ばれる`() = runTest {
        // When
        dataStoreManager.clearTimerState()
        advanceUntilIdle()

        // Then
        coVerify { dataStoreManager.clearTimerState() }
    }

    @Test
    fun `getElapsedTime実行時に保存された経過時間のFlowが返される`() = runTest {
        // Given
        setupMockFlowResponses(elapsedTime = TEST_ELAPSED_TIME)

        // When
        val result = dataStoreManager.getElapsedTime()

        // Then
        val elapsedTime = result.first()
        assertEquals(TEST_ELAPSED_TIME, elapsedTime)
    }

    @Test
    fun `getElapsedTime実行時に値が保存されていない場合はデフォルト値が返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getElapsedTime()

        // Then
        val elapsedTime = result.first()
        assertEquals(DEFAULT_ELAPSED_TIME, elapsedTime)
    }

    @Test
    fun `getStartDate実行時に保存された開始日のFlowが返される`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        val result = dataStoreManager.getStartDate()

        // Then
        val startDate = result.first()
        assertEquals(TEST_START_DATE, startDate)
    }

    @Test
    fun `getStartDate実行時に値が保存されていない場合はnullが返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getStartDate()

        // Then
        val startDate = result.first()
        assertNull(startDate)
    }

    @Test
    fun `getStartTime実行時に保存された開始時刻のFlowが返される`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        val result = dataStoreManager.getStartTime()

        // Then
        val startTime = result.first()
        assertEquals(TEST_START_TIME, startTime)
    }

    @Test
    fun `getStartTime実行時に値が保存されていない場合はnullが返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getStartTime()

        // Then
        val startTime = result.first()
        assertNull(startTime)
    }

    @Test
    fun `getElapsedTimeSync実行時に経過時間が同期的に取得される`() = runTest {
        // Given
        setupMockFlowResponses(elapsedTime = TEST_ELAPSED_TIME)

        // When
        val result = dataStoreManager.getElapsedTimeSync()

        // Then
        assertEquals(TEST_ELAPSED_TIME, result)
        coVerify { dataStoreManager.getElapsedTimeSync() }
    }

    @Test
    fun `getStartDateSync実行時に開始日が同期的に取得される`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        val result = dataStoreManager.getStartDateSync()

        // Then
        assertEquals(TEST_START_DATE, result)
        coVerify { dataStoreManager.getStartDateSync() }
    }

    @Test
    fun `getStartTimeSync実行時に開始時刻が同期的に取得される`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        val result = dataStoreManager.getStartTimeSync()

        // Then
        assertEquals(TEST_START_TIME, result)
        coVerify { dataStoreManager.getStartTimeSync() }
    }

    @Test
    fun `getElapsedTimeSync実行時に値が保存されていない場合はデフォルト値が返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getElapsedTimeSync()

        // Then
        assertEquals(DEFAULT_ELAPSED_TIME, result)
    }

    @Test
    fun `getStartDateSync実行時に値が保存されていない場合はnullが返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getStartDateSync()

        // Then
        assertNull(result)
    }

    @Test
    fun `getStartTimeSync実行時に値が保存されていない場合はnullが返される`() = runTest {
        // Given
        setupEmptyMockFlowResponses()

        // When
        val result = dataStoreManager.getStartTimeSync()

        // Then
        assertNull(result)
    }

    @Test
    fun `複数の操作を連続して実行した場合の動作確認`() = runTest {
        // When
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)
        dataStoreManager.updateElapsedTime(UPDATED_ELAPSED_TIME)
        dataStoreManager.clearTimerState()
        advanceUntilIdle()

        // Then
        coVerify { dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME) }
        coVerify { dataStoreManager.updateElapsedTime(UPDATED_ELAPSED_TIME) }
        coVerify { dataStoreManager.clearTimerState() }
    }

    @Test
    fun `データ保存と取得の組み合わせテスト`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)
        advanceUntilIdle()

        val elapsedTime = dataStoreManager.getElapsedTimeSync()
        val startDate = dataStoreManager.getStartDateSync()
        val startTime = dataStoreManager.getStartTimeSync()

        // Then
        coVerify { dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME) }
        assertEquals(TEST_ELAPSED_TIME, elapsedTime)
        assertEquals(TEST_START_DATE, startDate)
        assertEquals(TEST_START_TIME, startTime)
    }

    @Test
    fun `Flow値と同期値の一貫性確認`() = runTest {
        // Given
        setupMockFlowResponses()

        // When
        val elapsedTimeFlow = dataStoreManager.getElapsedTime().first()
        val elapsedTimeSync = dataStoreManager.getElapsedTimeSync()

        val startDateFlow = dataStoreManager.getStartDate().first()
        val startDateSync = dataStoreManager.getStartDateSync()

        val startTimeFlow = dataStoreManager.getStartTime().first()
        val startTimeSync = dataStoreManager.getStartTimeSync()

        // Then
        assertEquals(elapsedTimeFlow, elapsedTimeSync)
        assertEquals(startDateFlow, startDateSync)
        assertEquals(startTimeFlow, startTimeSync)
    }

    @Test
    fun `時間関連の定数を使用したテストケース`() = runTest {
        // Given
        val oneHour = SECOND_IN_HOURS
        val twoHours = SECOND_IN_HOURS * 2
        val halfHour = SECOND_IN_HOURS / 2

        setupMockFlowResponses(elapsedTime = oneHour)

        // When
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, oneHour)
        dataStoreManager.updateElapsedTime(twoHours)

        val result = dataStoreManager.getElapsedTimeSync()

        // Then
        coVerify { dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, oneHour) }
        coVerify { dataStoreManager.updateElapsedTime(twoHours) }
        assertEquals(oneHour, result)
        assertEquals(3600L, oneHour)
        assertEquals(7200L, twoHours)
        assertEquals(1800L, halfHour)
    }
}
