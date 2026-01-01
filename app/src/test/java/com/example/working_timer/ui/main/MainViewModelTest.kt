package com.example.working_timer.ui.main

import app.cash.turbine.test
import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.service.TimerState
import com.example.working_timer.util.Constants.ONE_HOUR_MS
import com.example.working_timer.util.Constants.ONE_MINUTE_MS
import io.mockk.every
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    companion object {
        private const val THIRTY_MINUTES_MS = 1_800_000L
        private const val THIRTY_SECONDS_MS = 30_000L

        private const val TEST_DATE = "2025-01-03"
        private const val TEST_START_TIME = "09:00"

        private const val ONE_HOUR_TEXT = "01:00:00"
        private const val THIRTY_MINUTES_TEXT = "30:00"
        private const val ONE_MINUTE_ONE_SECOND_TEXT = "01:01"
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var mockWorkRepository: WorkRepository
    private lateinit var mockTimerManager: TimerManager
    private lateinit var mockDataStoreManager: DataStoreManager
    private lateinit var mockTimerState: MutableStateFlow<TimerState>

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockWorkRepository = mockk(relaxed = true)
        mockDataStoreManager = mockk()

        mockTimerState = MutableStateFlow(TimerState())
        mockTimerManager = mockk {
            every { timerState } returns this@MainViewModelTest.mockTimerState
            every { startTimer() } just Runs
            every { pauseTimer() } just Runs
            every { resumeTimer() } just Runs
            every { stopTimer() } just Runs
        }

        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        viewModel = MainViewModel(mockWorkRepository, mockTimerManager, mockDataStoreManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setupDataStoreManagerForSave(
        startDate: String? = TEST_DATE,
        startTime: String? = TEST_START_TIME
    ) {
        coEvery { mockDataStoreManager.getStartDateSync() } returns startDate
        coEvery { mockDataStoreManager.getStartTimeSync() } returns startTime
    }

    @Test
    fun `timerStateがrunningならWorkingステータスになる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = true, elapsedTime = ONE_MINUTE_MS)

            val updated = awaitItem()
            assertEquals(TimerStatus.Working, updated.timerStatus)
        }
    }

    @Test
    fun `timerStateが停止中で経過時間ありならRestingステータスになる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = false, elapsedTime = THIRTY_MINUTES_MS)

            val updated = awaitItem()
            assertEquals(TimerStatus.Resting, updated.timerStatus)
            assertEquals(THIRTY_MINUTES_TEXT, updated.displayText)
        }
    }

    @Test
    fun `timerStateが停止中で経過時間0ならステータスはnullになる`() = runTest {
        viewModel.uiState.test {
            val initialItem = awaitItem()
            assertNull(initialItem.timerStatus)
            assertEquals(0L, initialItem.elapsedTime)
        }
    }

    @Test
    fun `startTimer実行時にTimerManagerが呼ばれる`() {
        viewModel.startTimer()
        verify(exactly = 1) { mockTimerManager.startTimer() }
    }

    @Test
    fun `pauseTimer実行時にTimerManagerが呼ばれる`() {
        viewModel.pauseTimer()
        verify(exactly = 1) { mockTimerManager.pauseTimer() }
    }

    @Test
    fun `resumeTimer実行時にTimerManagerが呼ばれる`() {
        viewModel.resumeTimer()
        verify(exactly = 1) { mockTimerManager.resumeTimer() }
    }

    @Test
    fun `stopTimer実行時にpauseされ保存ダイアログが表示される`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = true, elapsedTime = ONE_HOUR_MS)
            awaitItem()

            setupDataStoreManagerForSave()

            viewModel.stopTimer()

            verify(exactly = 1) { mockTimerManager.pauseTimer() }

            val withDialog = awaitItem()
            assertEquals(DialogStatus.SaveDialog(TEST_DATE, ONE_HOUR_MS), withDialog.dialogStatus)
        }
    }

    @Test
    fun `stopTimer実行時に開始情報が取れなければDataNotFoundエラーダイアログになる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = true, elapsedTime = ONE_HOUR_MS)
            awaitItem()

            setupDataStoreManagerForSave(startDate = null)

            viewModel.stopTimer()

            val withDialog = awaitItem()
            assertEquals(DialogStatus.DataNotFoundErrorDialog, withDialog.dialogStatus)
        }
    }

    @Test
    fun `saveWork実行時に1分未満ならTooShortTimeエラーダイアログになる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = false, elapsedTime = THIRTY_SECONDS_MS)
            awaitItem()

            setupDataStoreManagerForSave()

            viewModel.saveWork()

            val withDialog = awaitItem()
            assertEquals(DialogStatus.TooShortTimeErrorDialog, withDialog.dialogStatus)
        }
    }

    @Test
    fun `saveWork実行時に正常なら作業が保存されログ画面へ遷移する`() = runTest {
        mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)
        setupDataStoreManagerForSave()

        viewModel.navigateToLog.test {
            viewModel.saveWork()

            coVerify {
                mockWorkRepository.insert(
                    match {
                        it.start_day == TEST_DATE &&
                                it.start_time == TEST_START_TIME &&
                                it.elapsed_time == 3600L
                    }
                )
            }

            assertEquals(Unit, awaitItem())
        }

        verify(exactly = 1) { mockTimerManager.stopTimer() }
        assertNull(viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `saveWork実行時にstartDateがnullならinsertせず終了する`() = runTest {
        mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns TEST_START_TIME

        viewModel.saveWork()

        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWork実行時にstartTimeがnullならinsertせず終了する`() = runTest {
        mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)
        coEvery { mockDataStoreManager.getStartDateSync() } returns TEST_DATE
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        viewModel.saveWork()

        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWork実行後にdismissSaveDialogするとダイアログが閉じる`() = runTest {
        mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)
        setupDataStoreManagerForSave()
        viewModel.stopTimer()

        viewModel.dismissSaveDialog()

        assertNull(viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `discardWork実行時にTimerManagerのstopが呼ばれダイアログが閉じる`() = runTest {
        mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)
        setupDataStoreManagerForSave()
        viewModel.stopTimer()

        viewModel.discardWork()

        verify(exactly = 1) { mockTimerManager.stopTimer() }
        assertNull(viewModel.uiState.value.dialogStatus)
    }

    @Test
    fun `1時間未満の場合は分秒表示になる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = false, elapsedTime = 61_000L)

            val updated = awaitItem()
            assertEquals(ONE_MINUTE_ONE_SECOND_TEXT, updated.displayText)
            assertEquals(61_000L, updated.elapsedTime)
        }
    }

    @Test
    fun `1時間以上の場合は時分秒表示になる`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            mockTimerState.value = TimerState(isRunning = false, elapsedTime = ONE_HOUR_MS)

            val updated = awaitItem()
            assertEquals(ONE_HOUR_TEXT, updated.displayText)
        }
    }
}
