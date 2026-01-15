package com.example.working_timer.ui.edit_work

import androidx.lifecycle.SavedStateHandle
import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import com.example.working_timer.util.Constants.SECOND_IN_MINUTES
import io.mockk.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import android.database.sqlite.SQLiteException
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalCoroutinesApi::class)
class EditWorkViewModelTest {

    companion object {
        private const val TEST_ID = 1
        private const val TEST_START_DAY = "2025-01-03"
        private const val TEST_END_DAY = "2025-01-03"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_END_TIME = "17:00"
        private const val TEST_ELAPSED_TIME = 28800L // 8時間
        private const val TEST_ELAPSED_HOUR = 8L
        private const val TEST_ELAPSED_MINUTE = 0L

        private const val INVALID_DATE = "invalid-date"
        private const val INVALID_TIME = "invalid-time"

        private const val FUTURE_END_DAY = "2025-01-02"
        private const val FUTURE_END_TIME = "08:00"

        private const val LONG_ELAPSED_HOUR = 10L
        private const val LONG_ELAPSED_MINUTE = 0L

        private const val SHORT_ELAPSED_HOUR = 1L
        private const val SHORT_ELAPSED_MINUTE = 30L
    }

    private lateinit var viewModel: EditWorkViewModel
    private lateinit var mockWorkRepository: WorkRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val savedStateHandle = SavedStateHandle().apply {
            set("id", 0)
            set("startDay", TEST_START_DAY)
        }
        mockWorkRepository = mockk(relaxed = true)
        viewModel = EditWorkViewModel(savedStateHandle, mockWorkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createTestWork(
        id: Int = TEST_ID,
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

    private fun setupMockWorkForEdit(work: Work = createTestWork()) {
        coEvery { mockWorkRepository.getWork(work.id) } returns flowOf(work)
    }

    private fun setupSuccessfulInsert() {
        coEvery { mockWorkRepository.insert(any()) } just Runs
    }

    private fun setupSuccessfulUpdate() {
        coEvery { mockWorkRepository.update(any()) } just Runs
    }

    private fun setupValidWorkData() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(TEST_ELAPSED_HOUR, TEST_ELAPSED_MINUTE)
    }

    @Test
    fun `新規作成時に初期値が正しく設定される`() {
        val uiState = viewModel.uiState.value
        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals(TEST_START_DAY, uiState.endDay)
        assertEquals("00:00", uiState.startTime)
        assertEquals("00:00", uiState.endTime)
        assertEquals(0, uiState.elapsedHour)
        assertEquals(0, uiState.elapsedMinute)
    }

    @Test
    fun `既存記録編集時にDBから値がロードされる`() {
        val testWork = createTestWork()
        setupMockWorkForEdit(testWork)

        val savedStateHandle = SavedStateHandle().apply { set("id", testWork.id) }
        val viewModel = EditWorkViewModel(savedStateHandle, mockWorkRepository)

        val uiState = viewModel.uiState.value
        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals(TEST_END_DAY, uiState.endDay)
        assertEquals(TEST_START_TIME, uiState.startTime)
        assertEquals(TEST_END_TIME, uiState.endTime)
        assertEquals(TEST_ELAPSED_HOUR, uiState.elapsedHour)
        assertEquals(TEST_ELAPSED_MINUTE, uiState.elapsedMinute)

        coVerify { mockWorkRepository.getWork(TEST_ID) }
    }

    @Test
    fun `updateStartDay実行時に状態が更新される`() {
        val newStartDay = "2025-01-04"

        viewModel.updateStartDay(newStartDay)

        assertEquals(newStartDay, viewModel.uiState.value.startDay)
    }

    @Test
    fun `updateEndDay実行時に状態が更新される`() {
        val newEndDay = "2025-01-04"

        viewModel.updateEndDay(newEndDay)

        assertEquals(newEndDay, viewModel.uiState.value.endDay)
    }

    @Test
    fun `updateStartTime実行時に状態が更新される`() {
        val newStartTime = "10:00"

        viewModel.updateStartTime(newStartTime)

        assertEquals(newStartTime, viewModel.uiState.value.startTime)
    }

    @Test
    fun `updateEndTime実行時に状態が更新される`() {
        val newEndTime = "18:00"

        viewModel.updateEndTime(newEndTime)

        assertEquals(newEndTime, viewModel.uiState.value.endTime)
    }

    @Test
    fun `updateElapsedTime実行時に状態が更新される`() {
        val newHour = 5L
        val newMinute = 30L

        viewModel.updateElapsedTime(newHour, newMinute)

        val uiState = viewModel.uiState.value
        assertEquals(newHour, uiState.elapsedHour)
        assertEquals(newMinute, uiState.elapsedMinute)
    }

    @Test
    fun `新規作成時に正常保存が成功する`() = runTest {
        setupSuccessfulInsert()
        setupValidWorkData()

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0)

        // Then
        coVerify { mockWorkRepository.insert(any()) }
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is UiEvent.SaveSuccess)
    }

    @Test
    fun `既存記録更新時に正常保存が成功する`() = runTest {
        setupSuccessfulUpdate()
        setupValidWorkData()

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = TEST_ID)

        coVerify { mockWorkRepository.update(any()) }
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is UiEvent.SaveSuccess)
    }

    @Test
    fun `無効な日付形式でエラーメッセージが表示される`() = runTest {
        viewModel.updateStartDay(INVALID_DATE)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }

    @Test
    fun `無効な時刻形式でエラーメッセージが表示される`() = runTest {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(INVALID_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }

    @Test
    fun `経過時間が0分でエラー状態が設定される`() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(0, 0)

        viewModel.saveWork(id = 0)

        assertTrue("0分エラーが表示されること", viewModel.uiState.value.showZeroMinutesError)
    }

    @Test
    fun `開始時刻が終了時刻より後でエラー状態が設定される`() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(FUTURE_END_DAY)
        viewModel.updateEndTime(FUTURE_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        viewModel.saveWork(id = 0)

        assertTrue("開始終了時刻エラーが表示されること", viewModel.uiState.value.showStartEndError)
    }

    @Test
    fun `経過時間が実時間を超える場合にエラー状態が設定される`() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE)

        viewModel.saveWork(id = 0, forceSave = false)

        assertTrue("経過時間超過エラーが表示されること", viewModel.uiState.value.showElapsedTimeOver)
    }

    @Test
    fun `強制保存時は経過時間チェックをスキップする`() = runTest {
        setupSuccessfulInsert()
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE)

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0, forceSave = true)

        assertFalse("経過時間超過エラーが表示されないこと", viewModel.uiState.value.showElapsedTimeOver)
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is UiEvent.SaveSuccess)
    }

    @Test
    fun `SQLiteException発生時にDBエラーメッセージが表示される`() = runTest {
        coEvery { mockWorkRepository.insert(any()) } throws SQLiteException("DB Error")
        setupValidWorkData()

        var collectedEvent: UiEvent? = null

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }

    @Test
    fun `clearZeroMinutesError実行時にエラー状態がクリアされる`() {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(0, 0)
        viewModel.saveWork(id = 0)

        assertTrue("初期状態で0分エラーが設定されること", viewModel.uiState.value.showZeroMinutesError)

        viewModel.clearZeroMinutesError()

        assertFalse("0分エラーがクリアされること", viewModel.uiState.value.showZeroMinutesError)
    }

    @Test
    fun `clearStartEndError実行時にエラー状態がクリアされる`() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(FUTURE_END_DAY)
        viewModel.updateEndTime(FUTURE_END_TIME)
        viewModel.updateElapsedTime(1, 0)
        viewModel.saveWork(id = 0)

        assertTrue("初期状態で開始終了時刻エラーが設定されること", viewModel.uiState.value.showStartEndError)

        viewModel.clearStartEndError()

        assertFalse("開始終了時刻エラーがクリアされること", viewModel.uiState.value.showStartEndError)
    }

    @Test
    fun `clearElapsedTimeOver実行時にエラー状態がクリアされる`() {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE)
        viewModel.saveWork(id = 0, forceSave = false)

        assertTrue("初期状態で経過時間超過エラーが設定されること", viewModel.uiState.value.showElapsedTimeOver)

        viewModel.clearElapsedTimeOver()

        assertFalse("経過時間超過エラーがクリアされること", viewModel.uiState.value.showElapsedTimeOver)
    }

    @Test
    fun `初期UI状態は正しいデフォルト値を持つ`() {
        val uiState = viewModel.uiState.value

        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals(TEST_START_DAY, uiState.endDay)
        assertEquals("00:00", uiState.startTime)
        assertEquals("00:00", uiState.endTime)
        assertEquals(0, uiState.elapsedHour)
        assertEquals(0, uiState.elapsedMinute)
        assertFalse(uiState.showZeroMinutesError)
        assertFalse(uiState.showStartEndError)
        assertFalse(uiState.showElapsedTimeOver)
    }

    @Test
    fun `saveWork実行時に正しいWorkオブジェクトがリポジトリに渡される`() = runTest {
        setupSuccessfulInsert()
        setupValidWorkData()

        viewModel.saveWork(id = 0)

        coVerify {
            mockWorkRepository.insert(match { work ->
                work.start_day == TEST_START_DAY &&
                work.start_time == TEST_START_TIME &&
                work.end_day == TEST_END_DAY &&
                work.end_time == TEST_END_TIME &&
                work.elapsed_time == TEST_ELAPSED_TIME
            })
        }
    }

    @Test
    fun `updateWork実行時に正しいWorkオブジェクトがリポジトリに渡される`() = runTest {
        setupSuccessfulUpdate()
        setupValidWorkData()

        viewModel.saveWork(id = TEST_ID)

        coVerify {
            mockWorkRepository.update(match { work ->
                work.id == TEST_ID &&
                work.start_day == TEST_START_DAY &&
                work.start_time == TEST_START_TIME &&
                work.end_day == TEST_END_DAY &&
                work.end_time == TEST_END_TIME &&
                work.elapsed_time == TEST_ELAPSED_TIME
            })
        }
    }

    @Test
    fun `経過時間の計算が正しく行われる`() = runTest {
        setupSuccessfulInsert()
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(SHORT_ELAPSED_HOUR, SHORT_ELAPSED_MINUTE)

        viewModel.saveWork(id = 0)

        val expectedElapsedTime = SHORT_ELAPSED_HOUR * SECOND_IN_HOURS + SHORT_ELAPSED_MINUTE * SECOND_IN_MINUTES
        coVerify {
            mockWorkRepository.insert(match { work ->
                work.elapsed_time == expectedElapsedTime
            })
        }
    }

    @Test
    fun `無効な終了日付形式でエラーメッセージが表示される`() = runTest {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(INVALID_DATE)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)
        testDispatcher.scheduler.runCurrent()

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }

    @Test
    fun `無効な終了時刻形式でエラーメッセージが表示される`() = runTest {
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(INVALID_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)
        testDispatcher.scheduler.runCurrent()

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }

    @Test
    fun `存在しないIDで編集しようとした場合、UIは初期状態のまま`() = runTest {
        val nonExistentId = 999
        coEvery { mockWorkRepository.getWork(nonExistentId) } returns emptyFlow()

        testDispatcher.scheduler.runCurrent()

        val uiState = viewModel.uiState.value
        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals("00:00", uiState.endTime)
        assertEquals(0L, uiState.elapsedHour)
    }

    @Test
    fun `メッセージがない予期しない例外発生時に汎用エラーメッセージが表示される`() = runTest {
        val exceptionWithoutMessage = RuntimeException()
        coEvery { mockWorkRepository.insert(any()) } throws exceptionWithoutMessage
        setupValidWorkData()

        var collectedEvent: UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        viewModel.saveWork(id = 0)
        testDispatcher.scheduler.runCurrent()

        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is UiEvent.ShowSnackbar)
    }
}

