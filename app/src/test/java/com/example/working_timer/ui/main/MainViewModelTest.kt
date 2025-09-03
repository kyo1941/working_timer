package com.example.working_timer.ui.main

import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.ui.main.MainViewModel.Companion.EMPTY_STATUS
import com.example.working_timer.ui.main.MainViewModel.Companion.ERROR_MSG_DATA_NOT_FOUND
import com.example.working_timer.ui.main.MainViewModel.Companion.ERROR_MSG_SAVE_FAILED
import com.example.working_timer.ui.main.MainViewModel.Companion.ERROR_MSG_TIME_TOO_SHORT
import com.example.working_timer.ui.main.MainViewModel.Companion.RESTING_STATUS
import com.example.working_timer.ui.main.MainViewModel.Companion.WORKING_STATUS
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

    // 定数定義
    companion object {
        private const val THIRTY_MINUTES_MS = 1800000L
        private const val ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS = 3661000L
        private const val THIRTY_SECONDS_MS = 30000L
        private const val ONE_MINUTE_ONE_SECOND_MS = 61000L

        private const val TEST_DATE = "2025-01-03"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_ERROR_MESSAGE = "タイマーエラーが発生しました"
        private const val TEST_SNACKBAR_MESSAGE = "テストメッセージ"
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

        // デフォルトのモック動作を設定
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

    // 共通セットアップメソッド
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
        // Given
        val localMockDataStoreManager = mockk<DataStoreManager>()
        coEvery { localMockDataStoreManager.getElapsedTimeSync() } returns ONE_HOUR_MS
        coEvery { localMockDataStoreManager.getStartDateSync() } returns null
        coEvery { localMockDataStoreManager.getStartTimeSync() } returns null

        val localMockTimerManager = mockk<TimerManager> {
            every { setListener(any()) } just Runs
            every { isTimerRunning() } returns false
            every { getElapsedTime() } returns ONE_HOUR_MS
        }

        // When
        val newViewModel = MainViewModel(mockWorkRepository, localMockTimerManager, localMockDataStoreManager)

        // Then
        assertEquals(ONE_HOUR_TEXT, newViewModel.uiState.value.timerText)
        assertEquals(ONE_HOUR_MS, newViewModel.uiState.value.elapsedTime)
    }

    @Test
    fun `startTimer実行時にTimerManagerが開始され状態が更新される`() {
        // Given
        setupTimerManagerForRunning()
        every { mockTimerManager.startTimer() } just Runs

        // When
        viewModel.startTimer()

        // Then
        verifyStandardTimerActions("start")
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals(WORKING_STATUS, uiState.status)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `pauseTimer実行時にTimerManagerが一時停止され状態が更新される`() {
        // Given
        setupTimerManagerForStopped()
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.pauseTimer()

        // Then
        verifyStandardTimerActions("pause")
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals(RESTING_STATUS, uiState.status)
        assertTrue(uiState.isPaused)
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
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals(WORKING_STATUS, uiState.status)
        assertFalse(uiState.isPaused)
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
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertFalse(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains(TEST_DATE))
        assertTrue(uiState.dialogMessage.contains(" 1時間"))
    }

    @Test
    fun `stopTimer実行時に開始日が取得できない場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave(startDate = null)
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains(ERROR_MSG_DATA_NOT_FOUND))
    }

    @Test
    fun `saveWork実行時に1分未満の場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns THIRTY_SECONDS_MS
        setupDataStoreManagerForSave()

        // When
        viewModel.saveWork()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains(ERROR_MSG_TIME_TOO_SHORT))
    }

    @Test
    fun `saveWork実行時に正常な場合は作業が保存されログ画面に遷移する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupSuccessfulWorkSave()

        // When
        viewModel.saveWork()

        // Then
        coVerify {
            mockWorkRepository.insert(match { work ->
                work.start_day == TEST_DATE &&
                work.start_time == TEST_START_TIME &&
                work.elapsed_time == 3600 // 秒単位
            })
        }
        verifyStandardTimerActions("stop")

        val uiState = viewModel.uiState.value
        assertFalse(uiState.showSaveDialog)
        assertTrue(uiState.navigateToLog)
    }

    @Test
    fun `saveWork実行時に保存に失敗した場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave()
        coEvery { mockWorkRepository.insert(any()) } throws Exception("Database error")

        // When
        viewModel.saveWork()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains(ERROR_MSG_SAVE_FAILED))
        assertTrue(uiState.dialogMessage.contains("Database error"))
    }

    @Test
    fun `discardWork実行時にタイマーが停止され状態が更新される`() {
        // Given
        every { mockTimerManager.stopTimer() } just Runs
        setupTimerManagerForStopped(0L)

        // When
        viewModel.discardWork()

        // Then
        verifyStandardTimerActions("stop")
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertFalse(uiState.isPaused)
        assertEquals(EMPTY_STATUS, uiState.status)
    }

    @Test
    fun `dismissSaveDialog実行時にダイアログが閉じられる`() = runTest {
        // Given - まず保存ダイアログを表示する状態にする
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupDataStoreManagerForSave()
        every { mockTimerManager.pauseTimer() } just Runs

        viewModel.stopTimer()

        // When
        viewModel.dismissSaveDialog()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showSaveDialog)
        assertFalse(uiState.isErrorDialog)
    }

    @Test
    fun `onTimerTick実行時にタイマーテキストが更新される`() {
        // When
        viewModel.onTimerTick(ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(ONE_HOUR_ONE_MINUTE_ONE_SECOND_TEXT, uiState.timerText)
        assertEquals(ONE_HOUR_ONE_MINUTE_ONE_SECOND_MS, uiState.elapsedTime)
    }

    @Test
    fun `onTimerTick実行時に1時間未満の場合は分秒表示になる`() {
        // When
        viewModel.onTimerTick(ONE_MINUTE_ONE_SECOND_MS)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(ONE_MINUTE_ONE_SECOND_TEXT, uiState.timerText)
        assertEquals(ONE_MINUTE_ONE_SECOND_MS, uiState.elapsedTime)
    }

    @Test
    fun `onError実行時にスナックバーメッセージが設定される`() {
        // When
        viewModel.onError(TEST_ERROR_MESSAGE)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(TEST_ERROR_MESSAGE, uiState.snackbarMessage)
    }

    @Test
    fun `clearSnackbarMessage実行時にスナックバーメッセージがクリアされる`() {
        // Given - まずエラーメッセージを設定
        viewModel.onError(TEST_SNACKBAR_MESSAGE)

        // When
        viewModel.clearSnackbarMessage()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.snackbarMessage)
    }

    @Test
    fun `onNavigationHandled実行時にナビゲーションフラグがリセットされる`() = runTest {
        // Given - まず保存を実行してナビゲーションフラグを立てる
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        setupSuccessfulWorkSave()

        viewModel.saveWork()

        // When
        viewModel.onNavigationHandled()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.navigateToLog)
    }

    @Test
    fun `updateUI実行時に状態が更新される`() {
        // Given
        setupTimerManagerForRunning(THIRTY_MINUTES_MS)

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals(WORKING_STATUS, uiState.status)
        assertEquals(THIRTY_MINUTES_TEXT, uiState.timerText)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `経過時間0でタイマー停止時は空のステータスになる`() {
        // Given
        setupTimerManagerForStopped(0L)

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals(EMPTY_STATUS, uiState.status)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `経過時間ありでタイマー停止時は休憩中ステータスになる`() {
        // Given
        setupTimerManagerForStopped(THIRTY_MINUTES_MS)

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals(RESTING_STATUS, uiState.status)
        assertTrue(uiState.isPaused)
    }

    @Test
    fun `stopTimer実行時に経過時間が1時間未満なら分単位でダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns THIRTY_MINUTES_MS
        setupDataStoreManagerForSave()
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.dialogMessage.contains(" 30分"))
    }

    @Test
    fun `saveWork実行時にstartDateがnullなら何もせず終了する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns TEST_START_TIME

        // When
        viewModel.saveWork()

        // Then
        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWork実行時にstartTimeがnullなら何もせず終了する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        coEvery { mockDataStoreManager.getStartDateSync() } returns TEST_DATE
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        // When
        viewModel.saveWork()

        // Then
        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWorkでDB保存後にタイマー停止が失敗した場合`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns ONE_HOUR_MS
        coEvery { mockDataStoreManager.getStartDateSync() } returns TEST_DATE
        coEvery { mockDataStoreManager.getStartTimeSync() } returns TEST_START_TIME

        // insertは成功する
        coEvery { mockWorkRepository.insert(any()) } just Runs
        // stopTimerで例外を発生させる
        every { mockTimerManager.stopTimer() } throws Exception("Timer stop error")

        // When
        viewModel.saveWork()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains(ERROR_MSG_SAVE_FAILED))

        // 同時に、insertは呼ばれたことも確認する
        coVerify(exactly = 1) { mockWorkRepository.insert(any()) }
    }
}
