package com.example.working_timer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import com.example.working_timer.domain.repository.TimerListener
import com.example.working_timer.service.TimerService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerImplTest {

    // テストケース独自の定数
    companion object {
        private const val TEST_ELAPSED_TIME = 12345L
        private const val TEST_TIMER_TICK_TIME = 1000L
        private const val TEST_COMPONENT_NAME = "test"
        private const val ERROR_MESSAGE = "タイマーの開始に失敗しました。"
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockTimerService: TimerService
    private lateinit var mockBinder: TimerService.LocalBinder
    private lateinit var mockTimerListener: TimerListener
    private lateinit var timerManagerImpl: TimerManagerImpl
    private lateinit var serviceConnection: ServiceConnection

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockContext = mockk(relaxed = true)
        mockTimerService = mockk(relaxed = true)
        mockBinder = mockk(relaxed = true)
        mockTimerListener = mockk(relaxed = true)

        timerManagerImpl = TimerManagerImpl(mockContext)

        setupDefaultMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド群
    private fun setupDefaultMocks() {
        every { mockBinder.getService() } returns mockTimerService
        every { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) } returns true

        val connectionSlot = slot<ServiceConnection>()
        every { mockContext.bindService(any<Intent>(), capture(connectionSlot), any<Int>()) } answers {
            serviceConnection = connectionSlot.captured
            true
        }
    }

    private fun setupServiceBound() {
        timerManagerImpl.startTimer()
        simulateServiceConnection()
    }

    private fun setupServiceBindingFailure() {
        every { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) } returns false
    }

    private fun setupTimerServiceMocks(
        isRunning: Boolean = false,
        elapsedTime: Long = TEST_ELAPSED_TIME
    ) {
        every { mockTimerService.isTimerRunning() } returns isRunning
        every { mockTimerService.getElapsedTime() } returns elapsedTime
    }

    private fun simulateServiceConnection() {
        serviceConnection.onServiceConnected(
            ComponentName(TEST_COMPONENT_NAME, TEST_COMPONENT_NAME),
            mockBinder
        )
    }

    private fun simulateServiceDisconnection() {
        serviceConnection.onServiceDisconnected(
            ComponentName(TEST_COMPONENT_NAME, TEST_COMPONENT_NAME)
        )
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Throwable) {
            fail("Expected no exception to be thrown, but got $e")
        }
    }

    @Test
    fun `startTimer実行時にサービスがバインド済みの場合は直接タイマーが開始される`() = runTest {
        // Given
        setupServiceBound()

        // When
        timerManagerImpl.startTimer()

        // Then
        verify { mockTimerService.startTimer() }
    }

    @Test
    fun `startTimer実行時にサービス未バインドの場合はサービスバインドが実行される`() = runTest {
        // Given - サービスがバインドされていない状態

        // When
        timerManagerImpl.startTimer()

        // Then
        verify { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), Context.BIND_AUTO_CREATE) }
    }

    @Test
    fun `startTimer実行時にサービスバインドが失敗した場合はエラーが通知される`() = runTest {
        // Given
        setupServiceBindingFailure()
        timerManagerImpl.setListener(mockTimerListener)

        // When
        timerManagerImpl.startTimer()

        // Then
        verify { mockTimerListener.onError(ERROR_MESSAGE) }
    }

    @Test
    fun `pauseTimer実行時にタイマーサービスのpauseTimerが呼ばれる`() = runTest {
        // Given
        setupServiceBound()

        // When
        timerManagerImpl.pauseTimer()

        // Then
        verify { mockTimerService.pauseTimer() }
    }

    @Test
    fun `resumeTimer実行時にタイマーサービスのresumeTimerが呼ばれる`() = runTest {
        // Given
        setupServiceBound()

        // When
        timerManagerImpl.resumeTimer()

        // Then
        verify { mockTimerService.resumeTimer() }
    }

    @Test
    fun `stopTimer実行時にタイマーサービスのstopTimerが呼ばれサービスがアンバインドされる`() = runTest {
        // Given
        setupServiceBound()

        // When
        timerManagerImpl.stopTimer()

        // Then
        verify { mockTimerService.stopTimer() }
        verify { mockContext.unbindService(any<ServiceConnection>()) }
    }

    @Test
    fun `isTimerRunning実行時にサービスバインド済みの場合はサービスの状態が返される`() = runTest {
        // Given
        setupServiceBound()
        setupTimerServiceMocks(isRunning = true)

        // When
        val result = timerManagerImpl.isTimerRunning()

        // Then
        assertTrue(result)
        verify { mockTimerService.isTimerRunning() }
    }

    @Test
    fun `isTimerRunning実行時にサービス未バインドの場合はfalseが返される`() = runTest {
        // Given - サービスがバインドされていない状態

        // When
        val result = timerManagerImpl.isTimerRunning()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getElapsedTime実行時にサービスバインド済みの場合は経過時間が返される`() = runTest {
        // Given
        setupServiceBound()
        setupTimerServiceMocks(elapsedTime = TEST_ELAPSED_TIME)

        // When
        val result = timerManagerImpl.getElapsedTime()

        // Then
        assertEquals(TEST_ELAPSED_TIME, result)
        verify { mockTimerService.getElapsedTime() }
    }

    @Test
    fun `getElapsedTime実行時にサービス未バインドの場合は0が返される`() = runTest {
        // Given - サービスがバインドされていない状態

        // When
        val result = timerManagerImpl.getElapsedTime()

        // Then
        assertEquals(0L, result)
    }

    @Test
    fun `setListener実行時にリスナーが設定されサービス接続時にコールバックが転送される`() = runTest {
        // Given
        timerManagerImpl.setListener(mockTimerListener)

        // When
        setupServiceBound()

        // Then
        val serviceListener = slot<TimerService.TimerServiceListener>()
        verify { mockTimerService.setListener(capture(serviceListener)) }

        // コールバックの転送をテスト
        serviceListener.captured.onTimerTick(TEST_TIMER_TICK_TIME)
        verify { mockTimerListener.onTimerTick(TEST_TIMER_TICK_TIME) }

        serviceListener.captured.updateUI()
        verify { mockTimerListener.updateUI() }
    }

    @Test
    fun `removeListener実行時にリスナーがクリアされる`() = runTest {
        // Given
        timerManagerImpl.setListener(mockTimerListener)

        // When
        timerManagerImpl.removeListener()
        setupServiceBound()

        // Then
        val serviceListener = slot<TimerService.TimerServiceListener>()
        verify { mockTimerService.setListener(capture(serviceListener)) }

        // リスナーがクリアされているため呼び出されない
        serviceListener.captured.onTimerTick(TEST_TIMER_TICK_TIME)
        verify(exactly = 0) { mockTimerListener.onTimerTick(any<Long>()) }
    }

    @Test
    fun `onServiceConnected実行時にペンディング開始フラグがtrueの場合はタイマーが開始される`() = runTest {
        // Given
        timerManagerImpl.setListener(mockTimerListener)

        // When - ペンディング開始フラグがtrueの状態でサービス接続
        timerManagerImpl.startTimer()
        simulateServiceConnection() // サービス接続をシミュレート

        // Then
        verify { mockTimerService.startTimer() }
        verify { mockTimerListener.updateUI() }
    }

    @Test
    fun `onServiceDisconnected実行時にサービス状態がリセットされる`() = runTest {
        // Given
        setupServiceBound()

        // When
        simulateServiceDisconnection()

        // Then - サービス切断後の状態確認
        assertFalse(timerManagerImpl.isTimerRunning())
        assertEquals(0L, timerManagerImpl.getElapsedTime())
    }

    @Test
    fun `複数回のstartTimer呼び出しで適切にペンディング状態が管理される`() = runTest {
        // Given
        timerManagerImpl.setListener(mockTimerListener)

        // When - 複数回startTimerを呼び出し（サービス接続前）
        timerManagerImpl.startTimer()
        timerManagerImpl.startTimer() // 2回目
        simulateServiceConnection() // サービス接続をシミュレート

        // Then - サービス接続前は複数回bindServiceが呼ばれるが、タイマー開始は1回のみ
        verify(exactly = 2) { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) }
        verify { mockTimerService.startTimer() }
        verify { mockTimerListener.updateUI() }
    }

    @Test
    fun `サービス接続状態でのタイマー操作が正しく動作する`() = runTest {
        // Given
        setupServiceBound()
        setupTimerServiceMocks(isRunning = true, elapsedTime = 5000L)

        // When & Then - 各操作をテスト
        assertTrue(timerManagerImpl.isTimerRunning())
        assertEquals(5000L, timerManagerImpl.getElapsedTime())

        timerManagerImpl.pauseTimer()
        verify { mockTimerService.pauseTimer() }

        timerManagerImpl.resumeTimer()
        verify { mockTimerService.resumeTimer() }

        timerManagerImpl.stopTimer()
        verify { mockTimerService.stopTimer() }
        verify { mockContext.unbindService(any<ServiceConnection>()) }
    }

    @Test
    fun `pauseTimer実行時にサービス未バインドの場合は何も起こらない`() = runTest {
        // When
        timerManagerImpl.pauseTimer()

        // Then
        // 例外が発生しないことを確認
        verify(exactly = 0) { mockTimerService.pauseTimer() }
    }

    @Test
    fun `resumeTimer実行時にサービス未バインドの場合は何も起こらない`() = runTest {
        // When
        timerManagerImpl.resumeTimer()

        // Then
        // 例外が発生しないことを確認
        verify(exactly = 0) { mockTimerService.resumeTimer() }
    }

    @Test
    fun `stopTimer実行時にサービス未バインドの場合は何も起こらない`() = runTest {
        // When
        timerManagerImpl.stopTimer()

        // Then
        // 例外が発生しないことを確認
        verify(exactly = 0) { mockTimerService.stopTimer() }
        verify(exactly = 0) { mockContext.unbindService(any()) }
    }

    @Test
    fun `onServiceDisconnected実行後にisTimerRunningがfalseを返す`() = runTest {
        // Given
        setupServiceBound()
        setupTimerServiceMocks(isRunning = true)

        // When
        simulateServiceDisconnection()

        // Then
        val result = timerManagerImpl.isTimerRunning()
        assertFalse(result)
    }

    @Test
    fun `onServiceDisconnected実行後にgetElapsedTimeが0を返す`() = runTest {
        // Given
        setupServiceBound()
        setupTimerServiceMocks(elapsedTime = TEST_ELAPSED_TIME)

        // When
        simulateServiceDisconnection()

        // Then
        val result = timerManagerImpl.getElapsedTime()
        assertEquals(0L, result)
    }

    @Test
    fun `リスナーがnullの時にonTimerTickが呼ばれてもクラッシュしない`() = runTest {
        // Given
        // リスナーは設定しない
        setupServiceBound()

        val serviceListener = slot<TimerService.TimerServiceListener>()
        verify { mockTimerService.setListener(capture(serviceListener)) }

        // When & Then - クラッシュしないことを確認
        assertDoesNotThrow {
            serviceListener.captured.onTimerTick(TEST_TIMER_TICK_TIME)
        }
    }

    @Test
    fun `リスナーがnullの時にupdateUIが呼ばれてもクラッシュしない`() = runTest {
        // Given
        // リスナーは設定しない
        setupServiceBound()

        val serviceListener = slot<TimerService.TimerServiceListener>()
        verify { mockTimerService.setListener(capture(serviceListener)) }

        // When & Then - クラッシュしないことを確認
        assertDoesNotThrow {
            serviceListener.captured.updateUI()
        }
    }

    @Test
    fun `リスナーがnullの時にサービスバインドが失敗してもクラッシュしない`() = runTest {
        // Given
        setupServiceBindingFailure()
        // リスナーは設定しない

        // When & Then - クラッシュしないことを確認
        assertDoesNotThrow {
            timerManagerImpl.startTimer()
        }
        verify(exactly = 0) { mockTimerListener.onError(any()) }
    }
}
