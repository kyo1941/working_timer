package com.example.working_timer.ui.main

import com.example.working_timer.domain.repository.DataStoreManager
import com.example.working_timer.domain.repository.TimerManager
import com.example.working_timer.domain.repository.WorkRepository
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

    @Test
    fun `初期化時に保存された経過時間がロードされる`() = runTest {
        // Given
        val savedElapsedTime = 3600000L // 1時間
        val localMockDataStoreManager = mockk<DataStoreManager>()
        coEvery { localMockDataStoreManager.getElapsedTimeSync() } returns savedElapsedTime
        coEvery { localMockDataStoreManager.getStartDateSync() } returns null
        coEvery { localMockDataStoreManager.getStartTimeSync() } returns null

        val localMockTimerManager = mockk<TimerManager> {
            every { setListener(any()) } just Runs
            every { isTimerRunning() } returns false
            every { getElapsedTime() } returns savedElapsedTime
        }

        // When
        val newViewModel = MainViewModel(mockWorkRepository, localMockTimerManager, localMockDataStoreManager)

        // Then
        assertEquals("01:00:00", newViewModel.uiState.value.timerText)
        assertEquals(savedElapsedTime, newViewModel.uiState.value.elapsedTime)
    }

    @Test
    fun `startTimer実行時にTimerManagerが開始され状態が更新される`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns true
        every { mockTimerManager.getElapsedTime() } returns 60000L // 1分
        every { mockTimerManager.startTimer() } just Runs

        // When
        viewModel.startTimer()

        // Then
        verify { mockTimerManager.startTimer() }
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals("労働中", uiState.status)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `pauseTimer実行時にTimerManagerが一時停止され状態が更新される`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns 60000L // 1分
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.pauseTimer()

        // Then
        verify { mockTimerManager.pauseTimer() }
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals("休憩中", uiState.status)
        assertTrue(uiState.isPaused)
    }

    @Test
    fun `resumeTimer実行時にTimerManagerが再開され状態が更新される`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns true
        every { mockTimerManager.getElapsedTime() } returns 60000L // 1分
        every { mockTimerManager.resumeTimer() } just Runs

        // When
        viewModel.resumeTimer()

        // Then
        verify { mockTimerManager.resumeTimer() }
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals("労働中", uiState.status)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `stopTimer実行時に保存ダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L // 1時間
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        verify { mockTimerManager.pauseTimer() }
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertFalse(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains("2025-01-03"))
        assertTrue(uiState.dialogMessage.contains(" 1時間"))
    }

    @Test
    fun `stopTimer実行時に開始日が取得できない場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains("開始日または開始時刻が正しく取得できませんでした"))
    }

    @Test
    fun `saveWork実行時に1分未満の場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 30000L // 30秒
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"

        // When
        viewModel.saveWork()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains("1分未満の作業は保存できません"))
    }

    @Test
    fun `saveWork実行時に正常な場合は作業が保存されログ画面に遷移する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L // 1時間
        every { mockTimerManager.stopTimer() } just Runs
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        coEvery { mockWorkRepository.insert(any()) } just Runs

        // When
        viewModel.saveWork()

        // Then
        coVerify {
            mockWorkRepository.insert(match { work ->
                work.start_day == "2025-01-03" &&
                work.start_time == "09:00" &&
                work.elapsed_time == 3600 // 秒単位
            })
        }
        verify { mockTimerManager.stopTimer() }

        val uiState = viewModel.uiState.value
        assertFalse(uiState.showSaveDialog)
        assertTrue(uiState.navigateToLog)
    }

    @Test
    fun `saveWork実行時に保存に失敗した場合はエラーダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        coEvery { mockWorkRepository.insert(any()) } throws Exception("Database error")

        // When
        viewModel.saveWork()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)
        assertTrue(uiState.isErrorDialog)
        assertTrue(uiState.dialogMessage.contains("保存に失敗しました。再度お試しください。"))
        assertTrue(uiState.dialogMessage.contains("Database error"))
    }

    @Test
    fun `discardWork実行時にタイマーが停止され状態が更新される`() {
        // Given
        every { mockTimerManager.stopTimer() } just Runs
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns 0L

        // When
        viewModel.discardWork()

        // Then
        verify { mockTimerManager.stopTimer() }
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertFalse(uiState.isPaused)
        assertEquals("", uiState.status)
    }

    @Test
    fun `dismissSaveDialog実行時にダイアログが閉じられる`() {
        // Given - まず保存ダイアログを表示する状態にする
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        every { mockTimerManager.pauseTimer() } just Runs

        runTest {
            viewModel.stopTimer()
        }

        // When
        viewModel.dismissSaveDialog()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.showSaveDialog)
        assertFalse(uiState.isErrorDialog)
    }

    @Test
    fun `onTimerTick実行時にタイマーテキストが更新される`() {
        // Given
        val elapsedTime = 3661000L // 1時間1分1秒

        // When
        viewModel.onTimerTick(elapsedTime)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("01:01:01", uiState.timerText)
        assertEquals(elapsedTime, uiState.elapsedTime)
    }

    @Test
    fun `onTimerTick実行時に1時間未満の場合は分秒表示になる`() {
        // Given
        val elapsedTime = 61000L // 1分1秒

        // When
        viewModel.onTimerTick(elapsedTime)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals("01:01", uiState.timerText)
        assertEquals(elapsedTime, uiState.elapsedTime)
    }

    @Test
    fun `onError実行時にスナックバーメッセージが設定される`() {
        // Given
        val errorMessage = "タイマーエラーが発生しました"

        // When
        viewModel.onError(errorMessage)

        // Then
        val uiState = viewModel.uiState.value
        assertEquals(errorMessage, uiState.snackbarMessage)
    }

    @Test
    fun `clearSnackbarMessage実行時にスナックバーメッセージがクリアされる`() {
        // Given - まずエラーメッセージを設定
        viewModel.onError("テストメッセージ")

        // When
        viewModel.clearSnackbarMessage()

        // Then
        val uiState = viewModel.uiState.value
        assertNull(uiState.snackbarMessage)
    }

    @Test
    fun `onNavigationHandled実行時にナビゲーションフラグがリセットされる`() {
        // Given - まず保存を実行してナビゲーションフラグを立てる
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        every { mockTimerManager.stopTimer() } just Runs
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        coEvery { mockWorkRepository.insert(any()) } just Runs

        runTest {
            viewModel.saveWork()
        }

        // When
        viewModel.onNavigationHandled()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.navigateToLog)
    }

    @Test
    fun `updateUI実行時に状態が更新される`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns true
        every { mockTimerManager.getElapsedTime() } returns 1800000L // 30分

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isTimerRunning)
        assertEquals("労働中", uiState.status)
        assertEquals("30:00", uiState.timerText)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `経過時間0でタイマー停止時は空のステータスになる`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns 0L

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals("", uiState.status)
        assertFalse(uiState.isPaused)
    }

    @Test
    fun `経過時間ありでタイマー停止時は休憩中ステータスになる`() {
        // Given
        every { mockTimerManager.isTimerRunning() } returns false
        every { mockTimerManager.getElapsedTime() } returns 1800000L // 30分

        // When
        viewModel.updateUI()

        // Then
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isTimerRunning)
        assertEquals("休憩中", uiState.status)
        assertTrue(uiState.isPaused)
    }

    @Test
    fun `stopTimer実行時に経過時間が1時間未満なら分単位でダイアログが表示される`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 1800000L // 30分
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"
        every { mockTimerManager.pauseTimer() } just Runs

        // When
        viewModel.stopTimer()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState.showSaveDialog)

        // 「1時間」や「12時間」のように、数字に「時間」が続くパターンを定義
        val hoursPattern = Regex("""\d+\s*時間""")

        // 上記のパターンがメッセージに含まれていないことを確認
        assertFalse(
            "「XX時間」という形式が含まれています",
            hoursPattern.containsMatchIn(uiState.dialogMessage)
        )

        // "30分" が含まれることを確認
        assertTrue(uiState.dialogMessage.contains("30分"))
    }

    @Test
    fun `saveWork実行時にstartDateがnullなら何もせず終了する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns null
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"

        // When
        viewModel.saveWork()

        // Then
        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWork実行時にstartTimeがnullなら何もせず終了する`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns null

        // When
        viewModel.saveWork()

        // Then
        coVerify(exactly = 0) { mockWorkRepository.insert(any()) }
    }

    @Test
    fun `saveWorkでDB保存後にタイマー停止が失敗した場合`() = runTest {
        // Given
        every { mockTimerManager.getElapsedTime() } returns 3600000L
        coEvery { mockDataStoreManager.getStartDateSync() } returns "2025-01-03"
        coEvery { mockDataStoreManager.getStartTimeSync() } returns "09:00"

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
        assertTrue(uiState.dialogMessage.contains("保存に失敗しました"))

        // 同時に、insertは呼ばれたことも確認する
        coVerify(exactly = 1) { mockWorkRepository.insert(any()) }
    }
}
