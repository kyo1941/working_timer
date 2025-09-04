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
        private const val TEST_ELAPSED_TIME = 28800L // 8 hours in seconds
        private const val TEST_WAGE = 1000L
        private const val TEST_START_TIMESTAMP = 1702569600000L // 2023-12-15 00:00:00
        private const val TEST_END_TIMESTAMP = 1702656000000L // 2023-12-16 00:00:00
        private const val TWO_HALF_HOUR_SECONDS = 9000L // 2.5 hours
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockWorkRepository: WorkRepository
    private lateinit var viewModel: LogViewViewModel

    // テスト用のデフォルトワーク
    private val defaultTestWork = createTestWork()
    private val defaultTestWorkList = listOf(defaultTestWork)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockWorkRepository = mockk(relaxed = true)
        viewModel = LogViewViewModel(mockWorkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド群
    private fun setupMockWorkList(works: List<Work> = defaultTestWorkList) {
        coEvery { mockWorkRepository.getWorksByDay(any()) } returns works
    }

    private fun setupEmptyWorkList() {
        setupMockWorkList(emptyList())
    }

    private fun setupSumDialogWithWorks(works: List<Work> = defaultTestWorkList) {
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns works
        coEvery { mockWorkRepository.getWorksByDay(neq(TEST_DATE_STRING)) } returns emptyList()
        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
    }

    private fun setupDeleteScenario(workToDelete: Work = defaultTestWork) {
        coEvery { mockWorkRepository.delete(workToDelete.id) } returns Unit
        coEvery { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) } returns emptyList()
        viewModel.loadWorkList(TEST_DATE_STRING)
    }

    @Test
    fun `初期化時に現在日時が設定され作業リストが読み込まれる`() = runTest {
        setupMockWorkList()

        viewModel.init()
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertTrue(uiState.selectedDay.isNotEmpty())
        assertEquals(1, uiState.workList.size)
    }

    @Test
    fun `setSelectedDay実行時に日付が正しくフォーマットされ作業リストが読み込まれる`() = runTest {
        setupMockWorkList()

        viewModel.setSelectedDay(TEST_YEAR, TEST_MONTH, TEST_DAY)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(TEST_DATE_STRING, uiState.selectedDay)
        assertEquals(1, uiState.workList.size)
        assertFalse(uiState.isLoading)
        coVerify { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) }
    }

    @Test
    fun `loadWorkList実行時にローディング状態が設定され作業リストが更新される`() = runTest {
        setupMockWorkList()

        viewModel.loadWorkList(TEST_DATE_STRING)

        // ローディング状態を確認
        val loadingState = viewModel.uiState.first()
        assertTrue(loadingState.isLoading)
        assertEquals(TEST_DATE_STRING, loadingState.selectedDay)

        advanceUntilIdle()

        // 最終状態を確認
        val finalState = viewModel.uiState.first()
        assertFalse(finalState.isLoading)
        assertEquals(1, finalState.workList.size)
        assertEquals(TEST_DATE_STRING, finalState.selectedDay)
    }

    @Test
    fun `showDeleteDialog実行時に削除ダイアログが表示される`() = runTest {
        viewModel.showDeleteDialog(defaultTestWork)

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.showDeleteDialog)
        assertEquals(defaultTestWork, uiState.workToDelete)
    }

    @Test
    fun `hideDeleteDialog実行時に削除ダイアログが非表示になる`() = runTest {
        viewModel.showDeleteDialog(defaultTestWork)
        viewModel.hideDeleteDialog()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showDeleteDialog)
        assertNull(uiState.workToDelete)
    }

    @Test
    fun `deleteWork実行時に作業が削除され作業リストが再読み込みされる`() = runTest {
        setupDeleteScenario()
        advanceUntilIdle()

        viewModel.deleteWork(defaultTestWork)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showDeleteDialog)
        assertNull(uiState.workToDelete)
        coVerify { mockWorkRepository.delete(TEST_WORK_ID) }
        coVerify(exactly = 2) { mockWorkRepository.getWorksByDay(TEST_DATE_STRING) }
    }

    @Test
    fun `showSumDialog実行時に合計ダイアログが表示され日付範囲が設定される`() = runTest {
        setupEmptyWorkList()

        viewModel.showSumDialog(TEST_START_TIMESTAMP, TEST_END_TIMESTAMP)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertTrue(uiState.showSumDialog)
        assertEquals(TEST_START_TIMESTAMP, uiState.sumStartDate)
        assertEquals(TEST_END_TIMESTAMP, uiState.sumEndDate)
    }

    @Test
    fun `hideSumDialog実行時に合計ダイアログが非表示になる`() = runTest {
        setupSumDialogWithWorks(emptyList())
        advanceUntilIdle()

        viewModel.hideSumDialog()

        val uiState = viewModel.uiState.first()
        assertFalse(uiState.showSumDialog)
        assertNull(uiState.sumStartDate)
        assertNull(uiState.sumEndDate)
    }

    @Test
    fun `setTimeCalculationMode実行時に時間計算モードが更新される`() = runTest {
        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_UP)

        val uiState = viewModel.uiState.first()
        assertEquals(TimeCalculationMode.ROUND_UP, uiState.timeCalculationMode)
    }

    @Test
    fun `calculateSum実行時に合計時間が正しく計算される`() = runTest {
        val testWorks = listOf(
            createTestWork(elapsedTime = SECOND_IN_HOURS), // 1時間
            createTestWork(id = 2, elapsedTime = SECOND_IN_HOURS * 2) // 2時間
        )
        setupSumDialogWithWorks(testWorks)
        advanceUntilIdle()

        val uiState = viewModel.uiState.first()
        assertEquals(3L, uiState.totalHours) // 合計3時間
        assertEquals(0L, uiState.totalMinutes) // 0分
    }

    @Test
    fun `updateTotalWage実行時にNORMALモードで給与が正しく計算される`() = runTest {
        setupSumDialogWithWorks()
        advanceUntilIdle()

        viewModel.updateTotalWage(TEST_WAGE)

        val uiState = viewModel.uiState.first()
        assertEquals(8L, uiState.totalHours) // 8時間
        assertEquals(0L, uiState.totalMinutes) // 0分
        assertEquals(8000L, uiState.totalWage) // 8時間 * 1000円
    }

    @Test
    fun `updateTotalWage実行時にROUND_UPモードで時間が切り上げられる`() = runTest {
        val testWorks = listOf(createTestWork(elapsedTime = TWO_HALF_HOUR_SECONDS)) // 2.5時間
        setupSumDialogWithWorks(testWorks)
        advanceUntilIdle()

        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_UP)
        viewModel.updateTotalWage(TEST_WAGE)

        val uiState = viewModel.uiState.first()
        assertEquals(3L, uiState.totalHours) // 3時間に切り上げ
        assertEquals(0L, uiState.totalMinutes)
        assertEquals(3000L, uiState.totalWage) // 3時間 * 1000円
    }

    @Test
    fun `updateTotalWage実行時にROUND_DOWNモードで時間が切り下げられる`() = runTest {
        val testWorks = listOf(createTestWork(elapsedTime = TWO_HALF_HOUR_SECONDS)) // 2.5時間
        setupSumDialogWithWorks(testWorks)
        advanceUntilIdle()

        viewModel.setTimeCalculationMode(TimeCalculationMode.ROUND_DOWN)
        viewModel.updateTotalWage(TEST_WAGE)

        val uiState = viewModel.uiState.first()
        assertEquals(2L, uiState.totalHours) // 2時間に切り下げ
        assertEquals(0L, uiState.totalMinutes)
        assertEquals(2000L, uiState.totalWage) // 2時間 * 1000円
    }

    @Test
    fun `初期UI状態は正しいデフォルト値を持つ`() = runTest {
        val uiState = viewModel.uiState.first()

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
        elapsedTime: Long = TEST_ELAPSED_TIME
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
