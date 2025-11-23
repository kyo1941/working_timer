package com.example.working_timer.ui.main

import app.cash.turbine.test
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.ONE_HOUR_MS
import com.example.working_timer.util.Constants.ONE_MINUTE_MS
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    companion object {
        private const val THIRTY_MINUTES_MS = 1800000L
        private const val ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS = 3661000L
        private const val THIRTY_SECONDS_MS = 30000L
        private const val ONE_MINUTE_ONE_SECOND_MS = 61000L

        private const val TEST_DATE = "2025-01-03"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_ERROR_MESSAGE = "タイマーエラーが発生しました"
        private const val ONE_HOUR_TEXT = "01:00:00"
        private const val THIRTY_MINUTES_TEXT = "30:00"
        private const val ONE_HOUR_ONE_MINUTE_ONE_SECOND_TEXT = "01:01:01"
        private const val ONE_MINUTE_ONE_SECOND_TEXT = "01:01"
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var mockWorkRepository: WorkRepository
    private lateinit var mockTimerManager: TimerManager
    private lateinit var mockDataStoreManager: DataStoreManager
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockWorkRepository = mockk()
        mockTimerManager = mockk()
        mockDataStoreManager = mockk()

        every { mockTimerManager.setListener(any()) } just Runs
        every { mockTimerManager.removeListener() } just Runs
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns 0L

        coEvery { mockDataStoreManager.getElapsedTimeSync() } returns 0L
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        viewModel = MainViewModel(mockWorkRepository, mockTimerManager, mockDataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期化時にTimerManagerにリスナーが設定される`() {
        verify { mockTimerManager.setListener(viewModel) }
    }

    private fun setupTimerManagerForRunning(elapsedTime: Long = ONE_MINUTE_MS) {
        every { mockTimerManager.isTimerRunning() } returns true
        every { mockTimerManager.getElapsedTime() } returns elapsedTime
    }

    private fun setupTimerManagerForStopped(elapsedTime: Long = ONE_MINUTE_MS) {
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns elapsedTime
    }

    private fun setupDataStoreManagerForSave(
        startDate: String? = TEST_DATE,
        startTime: String? = TEST_START_TIME
    ) {
        coEvery { mockDataStoreManager.getStartDateSync() } returns startDate
        coEvery { mockDataStoreManager.getStartTimeSync() } returns startTime
    }

    private fun setupSuccessfulWorkSave() {
        every { mockTimerManager.stopTimer() } just Runs
        setupDataStoreManagerForSave()
        coEvery { mockWorkRepository.insert(any()) } just Runs
    }

    private fun verifyStandardTimerActions(action: String) {
        when (action) {
            "start" -> verify { mockTimerManager.startTimer() }
            "pause" -> verify { mockTimerManager.pauseTimer() }
            "resume" -> verify { mockTimerManager.resumeTimer() }
            "stop" -> verify { mockTimerManager.stopTimer() }
        }
    }

    @Test
    fun `初期化時に保存された経過時間がロードされる`() = runTest {
        val localMockDataStoreManager = mockk<DataStoreManager>()
        coEvery { localMockDataStoreManager.getElapsedTimeSync() } returns ONE_HOUR_MS
        coEvery { localMockDataStoreManager.getStartDateSync() } returns null
        coEvery { localMockDataStoreManager.getStartTimeSync() } returns null

        val localMockTimerManager = mockk<TimerManager> {
            every { setListener(any()) } just Runs
            every { isTimerRunning() } returns false
            every { getElapsedTime() } returns ONE_HOUR_MS
        }

        val newViewModel = MainViewModel(mockWorkRepository, localMockTimerManager, localMockDataStoreManager)

        assertEquals(ONE_HOUR_TEXT, newViewModel.uiState.value.displayText)
        assertEquals(ONE_HOUR_MS, newViewModel.uiState.value.elapsedTime)
    }

    @Test
    fun `startTimer実行時にTimerManagerが開始され状態が更新される`() {
        setupTimerManagerForRunning()
        every { mockTimerManager.startTimer() } just Runs

        viewModel.startTimer()

        verifyStandardTimerActions("start")
        assertEquals(TimerStatus.Working, viewModel.uiState.value.timerStatus)
    }

    @Test
    fun `pauseTimer実行時にTimerManagerが一時停止され状態が更新される`() {
        setupTimerManagerForStopped()
        every { mockTimerManager.pauseTimer() } just Runs

        viewModel.pauseTimer()

        verifyStandardTimerActions("pause")
        assertEquals(TimerStatus.Resting, viewModel.uiState.value.timerStatus)
    }

    @Test
    fun `resumeTimer実行時にTimerManagerが再開され状態が更新される`() {
        // Given
        setupTimerManagerForRunning()
        every { mockTimerManager.resumeTimer() } just Runs

        // When
        viewModel.resumeTimer()

        // Then
        verifyStandardTimerActions("resume")
        assertEquals(TimerStatus.Working, viewModel.uiState.value.timerStatus)
    }

    @Test
    fun `stopTimer実行時に保存ダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave()
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        verifyStandardTimerActions("pause")
        assertEquals(DialogStatus.SaveDialog(TEST_DATE, ONE_HOUR_MS), viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `stopTimer実行時に開始日が取得できない場合はエラーダイアログが表示される`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave(startDate = null)
        every { mockTimerManager.pauseTimer() } just Runs

        viewModel.stopTimer()
        assertEquals(DialogStatus.DataNotFoundErrorDialog, viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `saveWork実行時に1分未満の場合はエラーダイアログが表示される`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns THIRTY_SECONDS_MS
        setupDataStoreManagerForSave()

        viewModel.saveWork()

        assertEquals(DialogStatus.TooShortTimeErrorDialog, viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `saveWork実行時に正常な場合は作業が保存されログ画面に遷移する`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupSuccessfulWorkSave()

        viewModel.saveWork()

        coVerify {
            mockWorkRepository.insert(match { work ->
                work.start_day == TEST_DATE &&
                work.start_time == TEST_START_TIME &&
                work.elapsed_time == 3600L
            })
        }
        verifyStandardTimerActions("stop")

        assertNull(viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `saveWork実行時に保存に失敗した場合はスナックバーが表示される`() = runTest {
        val errorMessage = "Database error"
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave()
        coEvery { mockWorkRepository.insert(any()) } throws Exception(errorMessage)
        every { mockTimerManager.stopTimer() } just Runs

        viewModel.snackbarEvent.test {
            viewModel.saveWork()
            assertEquals(errorMessage, awaitItem())
        }
    }

    @Test
    fun `discardWork実行時にタイマーが停止され状態が更新される`() {
        every { mockTimerManager.stopTimer() } just Runs
        setupTimerManagerForStopped(0L)

        viewModel.discardWork()

        verifyStandardTimerActions("stop")
        val uiState = viewModel.uiState.value
        assertNull(uiState.timerStatus)
        assertEquals(0L, uiState.elapsedTime)
    }

    @Test
    fun `dismissSaveDialog実行時にダイアログが閉じられる`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave()
        every { mockTimerManager.pauseTimer() } just Runs

        viewModel.stopTimer()

        viewModel.dismissSaveDialog()

        assertNull(viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `onTimerTick実行時にタイマーテキストが更新される`() {
        viewModel.onTimerTick(ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS)

        val uiState = viewModel.uiState.value
        assertEquals(ONE_HOUR_ONE_MINUTE_ONE_SECOND_TEXT, uiState.displayText)
        assertEquals(ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS, uiState.elapsedTime)
    }

    @Test
    fun `onTimerTick実行時に1時間未満の場合は分秒表示になる`() {
        viewModel.onTimerTick(ONE_MINUTE_ONE_SECOND_MS)

        val uiState = viewModel.uiState.value
        assertEquals(ONE_MINUTE_ONE_SECOND_TEXT, uiState.displayText)
        assertEquals(ONE_MINUTE_ONE_SECOND_MS, uiState.elapsedTime)
    }

    @Test
    fun `onError実行時にスナックバーメッセージが設定される`() = runTest {
        viewModel.snackbarEvent.test {
            viewModel.onError(TEST_ERROR_MESSAGE)
            assertEquals(TEST_ERROR_MESSAGE, awaitItem())
        }
    }

    @Test
    fun `updateUI実行時に状態が更新される`() {
        setupTimerManagerForRunning(THIRTY_MINUTES_MS)

        viewModel.updateUI()

        val uiState = viewModel.uiState.value
        assertEquals(TimerStatus.Working, uiState.timerStatus)
        assertEquals(THIRTY_MINUTES_TEXT, uiState.displayText)
    }

    @Test
    fun `経過時間0でタイマー停止時は空のステータスになる`() {
        setupTimerManagerForStopped(0L)

        viewModel.updateUI()

        assertNull(viewModel.uiState.value.timerStatus)
    }

    @Test
    fun `経過時間ありでタイマー停止時は休憩中ステータスになる`() {
        setupTimerManagerForStopped(THIRTY_MINUTES_MS)

        viewModel.updateUI()

        assertEquals(TimerStatus.Resting, viewModel.uiState.value.timerStatus)
    }

    @Test
    fun `saveWork実行時にstartDateがnullなら何もせず終了する`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns TEST_START_TIME

        viewModel.saveWork()

        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWork実行時にstartTimeがnullなら何もせず終了する`() = runTest {
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        coEvery { mockDataStoreManager.getStartDateSync() } returns TEST_DATE
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        viewModel.saveWork()

        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }
}
