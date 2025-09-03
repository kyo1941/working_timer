package com.example.working_timer.ui.log_view

import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class LogViewViewModelTest {

    // テストケース独自の定数
    companion object {
        private const val TEST_YEAR = 2023
        private const val TEST_MONTH = 11 // 0-based (December)
        private const val TEST_DAY = 15
        private const val TEST_DATE_STRING = "2023-12-15"
        private const val TEST_WORK_ID = 1
        private const val TEST_START_DAY = "2023-12-15"
        private const val TEST_END_DAY = "2023-12-15"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_END_TIME = "17:00"
        private const val TEST_ELAPSED_TIME = 28800 // 8 hours in seconds
        private const val TEST_WAGE = 1000L
        private const val TEST_START_TIMESTAMP = 1702569600000L // 2023-12-15 00:00:00
        private const val TEST_END_TIMESTAMP = 1702656000000L // 2023-12-16 00:00:00
        private const val TWO_HALF_HOUR_SECONDS = 9000 // 2.5 hours
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWorkRepository: WorkRepository
    private lateinit var viewModel: LogViewViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockWorkRepository = mockk()
        viewModel = LogViewViewModel(mockWorkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド
    private fun setupMockWorkList(works: List<Work> = listOf(createTestWork())) {
        coEvery { mockWorkRepository.getWorksByDay(any()) } returns works
    }

    @Test
    fun `初期化時に現在日時が設定され作業リストが読み込まれる`() = runTest {
        // Given
        setupMockWorkList()

        // When
        viewModel.init()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertTrue(uiState.selectedDay.isNotEmpty())
        assertEquals(1, uiState.workList.size)
    }

    @Test
    fun `setSelectedDay実行時に日付が正しくフォーマットされ作業リストが読み込まれる`() = runTest {
        // Given
        setupMockWorkList()

        // When
        viewModel.setSelectedDay(TEST_YEAR, TEST_MONTH, TEST_DAY)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(TEST_DATE_STRING, uiState.selectedDay)
        assertEquals(1, uiState.workList.size)
        assertFalse(uiState.isLoading)
        coVerify { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) }
    }

    @Test
    fun `loadWorkList実行時にローディング状態が設定され作業リストが更新される`() = runTest {
        // Given
        setupMockWorkList()

        // When
        viewModel.loadWorkList(TEST_DATE_STRING)

        // ローディング状態を確認
        val loadingState = viewModel.uiState.first()
        assertTrue(loadingState.isLoading)
        assertEquals(TEST_DATE_STRING, loadingState.selectedDay)

        advanceUntilIdle()

        // Then
        val finalState = viewModel.uiState.first()
        assertFalse(finalState.isLoading)
        assertEquals(1, finalState.workList.size)
        assertEquals(TEST_DATE_STRING, finalState.selectedDay)
    }

    @Test
    fun `showDeleteDialog実行時に削除ダイアログが表示される`() = runTest {
        // Given
        val testWork = createTestWork()

        // When
        viewModel.showDeleteDialog(testWork)

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.showDeleteDialog)
        assertEquals(testWork, uiState.workToDelete)
    }

    @Test
    fun `hideDeleteDialog実行時に削除ダイアログが非表示になる`() = runTest {
        // Given
        val testWork = createTestWork()
        viewModel.showDeleteDialog(testWork)

        // When
        viewModel.hideDeleteDialog()

        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showDeleteDialog)
        assertNull(uiState.workToDelete)
    }

    @Test
    fun `deleteWork実行時に作業が削除され作業リストが再読み込みされる`() = runTest {
        // Given
        val testWork = createTestWork()
        val updatedWorks = emptyList<Work>()
        coEvery { mockWorkRepository.delete(TEST_WORK_ID) } returns Unit
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns updatedWorks

        // 初期状態を設定
        viewModel.loadWorkList(TEST_DATE_STRING)
        advanceUntilIdle()

        // When
        viewModel.deleteWork(testWork)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showDeleteDialog)
        assertNull(uiState.workToDelete)
        coVerify { mockWorkRepository.delete(TEST_WORK_ID) }
        coVerify(exactly = 2) { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) }
    }

    @Test
    fun `showSumDialog実行時に合計ダイアログが表示され日付範囲が設定される`() = runTest {
        // Given
        coEvery { mockWorkRepository.getWorksByDay(any()) } returns emptyList()

        // When
        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.showSumDialog)
        assertEquals(TEST_START_TIMESTAMP, uiState.sumStartDate)
        assertEquals(TEST_END_TIMESTAMP, uiState.sumEndDate)
    }

    @Test
    fun `hideSumDialog実行時に合計ダイアログが非表示になる`() = runTest {
        // Given
        coEvery { mockWorkRepository.getWorksByDay(any()) } returns emptyList()
        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // When
        viewModel.hideSumDialog()

        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showSumDialog)
        assertNull(uiState.sumStartDate)
        assertNull(uiState.sumEndDate)
    }

    @Test
    fun `setTimeCalculationMode実行時に時間計算モードが更新される`() = runTest {
        // When
        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_UP)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(TimeCalculationMode.ROUND_UP, uiState.timeCalculationMode)
    }

    @Test
    fun `calculateSum実行時に合計時間が正しく計算される`() = runTest {
        // Given
        val testWorks = listOf(
            createTestWork(elapsedTime = SECOND_IN_HOURS.toInt()), // 1時間
            createTestWork(id = 2, elapsedTime = SECOND_IN_HOURS.toInt() * 2) // 2時間
        )

        // テスト用の日付 2023-12-15 の日付でのみ testWorks を返すように設定
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns testWorks

        // それ以外の任意の日付では空のリストを返すように設定
        coEvery { mockWorkRepository.getWorksByDay(neq(TEST_DATE_STRING)) } returns emptyList()

        // When
        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(3L, uiState.totalHours) // 合計3時間
        assertEquals(0L, uiState.totalMinutes) // 0分
    }

    @Test
    fun `updateTotalWage実行時にNORMALモードで給与が正しく計算される`() = runTest {
        // Given
        val testWorks = listOf(createTestWork(elapsedTime = TEST_ELAPSED_TIME))

        // テスト用の日付 2023-12-15 の日付でのみ testWorks を返すように設定
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns testWorks

        // それ以外の任意の日付では空のリストを返すように設定
        coEvery { mockWorkRepository.getWorksByDay(neq(TEST_DATE_STRING)) } returns emptyList()

        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // When
        viewModel.updateTotalWage(TEST_WAGE)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(8L, uiState.totalHours) // 8時間
        assertEquals(0L, uiState.totalMinutes) // 0分
        assertEquals(8000L, uiState.totalWage) // 8時間 * 1000円
    }

    @Test
    fun `updateTotalWage実行時にROUND_UPモードで時間が切り上げられる`() = runTest {
        // Given
        val testWorks = listOf(createTestWork(elapsedTime = TWO_HALF_HOUR_SECONDS)) // 2.5時間

        // テスト用の日付 2023-12-15 の日付でのみ testWorks を返すように設定
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns testWorks

        // それ以外の任意の日付では空のリストを返すように設定
        coEvery { mockWorkRepository.getWorksByDay(neq(TEST_DATE_STRING)) } returns emptyList()

        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // When
        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_UP)
        viewModel.updateTotalWage(TEST_WAGE)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(3L, uiState.totalHours) // 3時間に切り上げ
        assertEquals(0L, uiState.totalMinutes)
        assertEquals(3000L, uiState.totalWage) // 3時間 * 1000円
    }

    @Test
    fun `updateTotalWage実行時にROUND_DOWNモードで時間が切り下げられる`() = runTest {
        // Given
        val testWorks = listOf(createTestWork(elapsedTime = TWO_HALF_HOUR_SECONDS)) // 2.5時間

        // テスト用の日付 2023-12-15 の日付でのみ testWorks を返すように設定
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns testWorks

        // それ以外の任意の日付では空のリストを返すように設定
        coEvery { mockWorkRepository.getWorksByDay(neq(TEST_DATE_STRING)) } returns emptyList()

        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        // When
        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_DOWN)
        viewModel.updateTotalWage(TEST_WAGE)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(2L, uiState.totalHours) // 2時間に切り下げ
        assertEquals(0L, uiState.totalMinutes)
        assertEquals(2000L, uiState.totalWage) // 2時間 * 1000円
    }

    @Test
    fun `初期UI状態は正しいデフォルト値を持つ`() = runTest {
        // When
        val uiState = viewModel.uiState.first()

        // Then
        assertEquals("", uiState.selectedDay)
        assertTrue(uiState.workList.isEmpty())
        assertFalse(uiState.isLoading)
        assertFalse(uiState.showDeleteDialog)
        assertNull(uiState.workToDelete)
        assertFalse(uiState.showSumDialog)
        assertNull(uiState.sumStartDate)
        assertNull(uiState.sumEndDate)
        assertEquals(0L, uiState.totalHours)
        assertEquals(0L, uiState.totalMinutes)
        assertEquals(0L, uiState.totalWage)
        assertEquals(TimeCalculationMode.NORMAL, uiState.timeCalculationMode)
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
