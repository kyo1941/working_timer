package com.example.working_timer.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import com.example.working_timer.service.TimerService
import com.example.working_timer.service.TimerState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerImplTest {

    companion object {
        private const val TEST_COMPONENT_NAME = "test"
        private val INITIAL_STATE = TimerState(isRunning = false, elapsedTime = 0L)
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockContext: Context
    private lateinit var mockTimerService: TimerService
    private lateinit var mockBinder: TimerService.LocalBinder
    private lateinit var timerManagerImpl: TimerManagerImpl
    private lateinit var serviceConnection: ServiceConnection

    private lateinit var serviceState: MutableStateFlow<TimerState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockContext = mockk(relaxed = true)
        mockTimerService = mockk(relaxed = true)
        mockBinder = mockk(relaxed = true)

        serviceState = MutableStateFlow(INITIAL_STATE)

        every { mockBinder.getService() } returns mockTimerService
        every { mockTimerService.serviceState } returns serviceState

        val connectionSlot = slot<ServiceConnection>()
        every {
            mockContext.bindService(
                any<Intent>(),
                capture(connectionSlot),
                any<Int>()
            )
        } answers {
            serviceConnection = connectionSlot.captured
            true
        }

        timerManagerImpl = TimerManagerImpl(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun simulateServiceConnection() {
        serviceConnection.onServiceConnected(
            ComponentName(TEST_COMPONENT_NAME, TEST_COMPONENT_NAME),
            mockBinder
        )
    }

    @Test
    fun `init時にサービスがbindされる`() {
        verify {
            mockContext.bindService(
                any<Intent>(),
                any<ServiceConnection>(),
                Context.BIND_AUTO_CREATE
            )
        }
    }

    @Test
    fun `startTimer実行時に未バインドならpendingStartになり再bindされる`() {
        viewModelSafe {
            timerManagerImpl.startTimer()
        }

        verify(atLeast = 2) {
            mockContext.bindService(
                any<Intent>(),
                any<ServiceConnection>(),
                Context.BIND_AUTO_CREATE
            )
        }
    }

    @Test
    fun `pendingStart中にserviceConnectedしたらserviceのstartTimerが呼ばれる`() = runTest {
        timerManagerImpl.startTimer()

        simulateServiceConnection()
        advanceUntilIdle()

        verify { mockTimerService.startTimer() }
    }

    @Test
    fun `serviceConnected後にstartTimerするとserviceのstartTimerが呼ばれる`() = runTest {
        simulateServiceConnection()
        advanceUntilIdle()

        timerManagerImpl.startTimer()

        verify { mockTimerService.startTimer() }
    }

    @Test
    fun `pauseTimer実行時にバインド済みならserviceのpauseTimerが呼ばれる`() = runTest {
        simulateServiceConnection()
        advanceUntilIdle()

        timerManagerImpl.pauseTimer()

        verify { mockTimerService.pauseTimer() }
    }

    @Test
    fun `pauseTimer実行時に未バインドなら何も起きない`() {
        timerManagerImpl.pauseTimer()

        verify(exactly = 0) { mockTimerService.pauseTimer() }
    }

    @Test
    fun `resumeTimer実行時にバインド済みならserviceのresumeTimerが呼ばれる`() = runTest {
        simulateServiceConnection()
        advanceUntilIdle()

        timerManagerImpl.resumeTimer()

        verify { mockTimerService.resumeTimer() }
    }

    @Test
    fun `stopTimer実行時にserviceのstopTimerが呼ばれunbindされる`() = runTest {
        simulateServiceConnection()
        advanceUntilIdle()

        timerManagerImpl.stopTimer()

        verify { mockTimerService.stopTimer() }
        verify { mockContext.unbindService(any<ServiceConnection>()) }
    }

    @Test
    fun `serviceStateが更新されるとtimerManagerのtimerStateに反映される`() = runTest {
        val valuesDeferred = backgroundScope.async {
            timerManagerImpl.timerState.take(2).toList(mutableListOf())
        }

        simulateServiceConnection()
        advanceUntilIdle()

        val next = TimerState(isRunning = true, elapsedTime = 12_345L)
        serviceState.value = next
        advanceUntilIdle()

        val values = valuesDeferred.await()
        assertTrue(values.contains(next))
        assertEquals(next, timerManagerImpl.timerState.value)
    }

    @Test
    fun `onServiceDisconnected後は未接続扱いになりtimerStateは初期値に戻る`() = runTest {
        val valuesDeferred = backgroundScope.async {
            timerManagerImpl.timerState.take(3).toList(mutableListOf())
        }

        simulateServiceConnection()
        advanceUntilIdle()

        val next = TimerState(isRunning = false, elapsedTime = 9_000L)
        serviceState.value = next
        advanceUntilIdle()

        serviceConnection.onServiceDisconnected(
            ComponentName(
                TEST_COMPONENT_NAME,
                TEST_COMPONENT_NAME
            )
        )
        advanceUntilIdle()

        val values = valuesDeferred.await()
        assertTrue(values.contains(next))
        assertEquals(INITIAL_STATE, timerManagerImpl.timerState.value)

        timerManagerImpl.pauseTimer()
        timerManagerImpl.resumeTimer()
        verify(exactly = 0) { mockTimerService.pauseTimer() }
        verify(exactly = 0) { mockTimerService.resumeTimer() }
    }

    private fun viewModelSafe(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            fail("Expected no exception, but got $t")
        }
    }
}
