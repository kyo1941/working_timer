package com.example.working_timer.ui.edit_work

import com.example.working_timer.data.db.Work
import com.example.working_timer.domain.repository.WorkRepository
import com.example.working_timer.ui.edit_work.EditWorkViewModel.Companion.ERROR_MSG_DATE_TIME_PATTERN
import com.example.working_timer.ui.edit_work.EditWorkViewModel.Companion.ERROR_MSG_DB_FAILED
import com.example.working_timer.ui.edit_work.EditWorkViewModel.Companion.ERROR_MSG_UNKNOWN
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditWorkViewModelTest {

    // 定数定義
    companion object {
        private const val TEST_ID = 1
        private const val TEST_START_DAY = "2025-01-03"
        private const val TEST_END_DAY = "2025-01-03"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_END_TIME = "17:00"
        private const val TEST_ELAPSED_TIME = 28800 // 8時間
        private const val TEST_ELAPSED_HOUR = 8
        private const val TEST_ELAPSED_MINUTE = 0

        private const val INVALID_DATE = "invalid-date"
        private const val INVALID_TIME = "invalid-time"

        private const val FUTURE_END_DAY = "2025-01-02"
        private const val FUTURE_END_TIME = "08:00"

        private const val LONG_ELAPSED_HOUR = 10
        private const val LONG_ELAPSED_MINUTE = 0

        private const val SHORT_ELAPSED_HOUR = 1
        private const val SHORT_ELAPSED_MINUTE = 30
    }

    private lateinit var viewModel: EditWorkViewModel
    private lateinit var mockWorkRepository: WorkRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockWorkRepository = mockk(relaxed = true)

        viewModel = EditWorkViewModel(mockWorkRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // 共通セットアップメソッド群
    private fun createTestWork(
        id: Int = TEST_ID,
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

    private suspend fun collectUiEvent(): EditWorkViewModel.UiEvent? {
        return try {
            viewModel.uiEvent.first()
        } catch (e: Exception) {
            null
        }
    }

    @Test
    fun `新規作成時に初期値が正しく設定される`() = runTest {
        // When
        viewModel.init(id = 0, isNew = true, startDay = TEST_START_DAY)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals(TEST_START_DAY, uiState.endDay)
        assertEquals("00:00", uiState.startTime)
        assertEquals("00:00", uiState.endTime)
        assertEquals(0, uiState.elapsedHour)
        assertEquals(0, uiState.elapsedMinute)
    }

    @Test
    fun `既存記録編集時にDBから値がロードされる`() = runTest {
        // Given
        val testWork = createTestWork()
        setupMockWorkForEdit(testWork)

        // When
        viewModel.init(id = TEST_ID, isNew = false, startDay = "")

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(TEST_START_DAY, uiState.startDay)
        assertEquals(TEST_END_DAY, uiState.endDay)
        assertEquals(TEST_START_TIME, uiState.startTime)
        assertEquals(TEST_END_TIME, uiState.endTime)
        assertEquals(TEST_ELAPSED_HOUR, uiState.elapsedHour)
        assertEquals(TEST_ELAPSED_MINUTE, uiState.elapsedMinute)

        coVerify { mockWorkRepository.getWork(TEST_ID) }
    }

    @Test
    fun `updateStartDay実行時に状態が更新される`() = runTest {
        // Given
        val newStartDay = "2025-01-04"

        // When
        viewModel.updateStartDay(newStartDay)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(newStartDay, uiState.startDay)
    }

    @Test
    fun `updateEndDay実行時に状態が更新される`() = runTest {
        // Given
        val newEndDay = "2025-01-04"

        // When
        viewModel.updateEndDay(newEndDay)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(newEndDay, uiState.endDay)
    }

    @Test
    fun `updateStartTime実行時に状態が更新される`() = runTest {
        // Given
        val newStartTime = "10:00"

        // When
        viewModel.updateStartTime(newStartTime)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(newStartTime, uiState.startTime)
    }

    @Test
    fun `updateEndTime実行時に状態が更新される`() = runTest {
        // Given
        val newEndTime = "18:00"

        // When
        viewModel.updateEndTime(newEndTime)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(newEndTime, uiState.endTime)
    }

    @Test
    fun `updateElapsedTime実行時に状態が更新される`() = runTest {
        // Given
        val newHour = 5
        val newMinute = 30

        // When
        viewModel.updateElapsedTime(newHour, newMinute)

        // Then
        val uiState = viewModel.uiState.first()
        assertEquals(newHour, uiState.elapsedHour)
        assertEquals(newMinute, uiState.elapsedMinute)
    }

    @Test
    fun `新規作成時に正常保存が成功する`() = runTest {
        // Given
        setupSuccessfulInsert()
        setupValidWorkData()

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        coVerify { mockWorkRepository.insert(any()) }
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.SaveSuccess)
    }

    @Test
    fun `既存記録更新時に正常保存が成功する`() = runTest {
        // Given
        setupSuccessfulUpdate()
        setupValidWorkData()

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = TEST_ID, isNew = false)

        // Then
        coVerify { mockWorkRepository.update(any()) }
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.SaveSuccess)
    }

    @Test
    fun `無効な日付形式でエラーメッセージが表示される`() = runTest {
        // Given
        viewModel.updateStartDay(INVALID_DATE)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        assertEquals(ERROR_MSG_DATE_TIME_PATTERN, (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message)
    }

    @Test
    fun `無効な時刻形式でエラーメッセージが表示される`() = runTest {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(INVALID_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        assertEquals(ERROR_MSG_DATE_TIME_PATTERN, (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message)
    }

    @Test
    fun `経過時間が0分でエラー状態が設定される`() = runTest {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(0, 0)

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("0分エラーが表示されること", uiState.showZeroMinutesError)
    }

    @Test
    fun `開始時刻が終了時刻より後でエラー状態が設定される`() = runTest {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(FUTURE_END_DAY)
        viewModel.updateEndTime(FUTURE_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("開始終了時刻エラーが表示されること", uiState.showStartEndError)
    }

    @Test
    fun `経過時間が実時間を超える場合にエラー状態が設定される`() = runTest {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE) // 10時間（実際は8時間）

        // When
        viewModel.saveWork(id = 0, isNew = true, forceSave = false)

        // Then
        val uiState = viewModel.uiState.first()
        assertTrue("経過時間超過エラーが表示されること", uiState.showElapsedTimeOver)
    }

    @Test
    fun `強制保存時は経過時間チェックをスキップする`() = runTest {
        // Given
        setupSuccessfulInsert()
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE)

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true, forceSave = true)

        // Then
        val uiState = viewModel.uiState.first()
        assertFalse("経過時間超過エラーが表示されないこと", uiState.showElapsedTimeOver)
        assertTrue("SaveSuccessイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.SaveSuccess)
    }

    @Test
    fun `SQLiteException発生時にDBエラーメッセージが表示される`() = runTest {
        // Given
        coEvery { mockWorkRepository.insert(any()) } throws SQLiteException("DB Error")
        setupValidWorkData()

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        assertEquals(ERROR_MSG_DB_FAILED, (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message)
    }

    @Test
    fun `予期しない例外発生時に汎用エラーメッセージが表示される`() = runTest {
        // Given
        val testException = RuntimeException("Test error")
        coEvery { mockWorkRepository.insert(any()) } throws testException
        setupValidWorkData()

        var collectedEvent: EditWorkViewModel.UiEvent? = null

        // backgroundScopeを使い、イベント受信をバックグラウンドで開始する
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        val message = (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message
        assertTrue("汎用エラーメッセージが含まれること", message.startsWith(ERROR_MSG_UNKNOWN))
    }

    @Test
    fun `clearZeroMinutesError実行時にエラー状態がクリアされる`() = runTest {
        // Given
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(0, 0)
        viewModel.saveWork(id = 0, isNew = true)

        val initialState = viewModel.uiState.first()
        assertTrue("初期状態で0分エラーが設定されること", initialState.showZeroMinutesError)

        // When
        viewModel.clearZeroMinutesError()

        // Then
        val finalState = viewModel.uiState.first()
        assertFalse("0分エラーがクリアされること", finalState.showZeroMinutesError)
    }

    @Test
    fun `clearStartEndError実行時にエラー状態がクリアされる`() = runTest {
        // Given (開始時刻 > 終了時刻の状態を作成)
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(FUTURE_END_DAY)
        viewModel.updateEndTime(FUTURE_END_TIME)
        viewModel.updateElapsedTime(1, 0)
        viewModel.saveWork(id = 0, isNew = true) // エラー状態を設定

        val initialState = viewModel.uiState.first()
        assertTrue("初期状態で開始終了時刻エラーが設定されること", initialState.showStartEndError)

        // When
        viewModel.clearStartEndError()

        // Then
        val finalState = viewModel.uiState.first()
        assertFalse("開始終了時刻エラーがクリアされること", finalState.showStartEndError)
    }

    @Test
    fun `clearElapsedTimeOver実行時にエラー状態がクリアされる`() = runTest {
        // Given (経過時間 > 実時間の状態を作成)
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(LONG_ELAPSED_HOUR, LONG_ELAPSED_MINUTE)
        viewModel.saveWork(id = 0, isNew = true, forceSave = false) // エラー状態を設定

        val initialState = viewModel.uiState.first()
        assertTrue("初期状態で経過時間超過エラーが設定されること", initialState.showElapsedTimeOver)

        // When
        viewModel.clearElapsedTimeOver()

        // Then
        val finalState = viewModel.uiState.first()
        assertFalse("経過時間超過エラーがクリアされること", finalState.showElapsedTimeOver)
    }

    @Test
    fun `初期UI状態は正しいデフォルト値を持つ`() = runTest {
        val uiState = viewModel.uiState.first()

        assertEquals("", uiState.startDay)
        assertEquals("", uiState.endDay)
        assertEquals("", uiState.startTime)
        assertEquals("", uiState.endTime)
        assertEquals(0, uiState.elapsedHour)
        assertEquals(0, uiState.elapsedMinute)
        assertFalse(uiState.showZeroMinutesError)
        assertFalse(uiState.showStartEndError)
        assertFalse(uiState.showElapsedTimeOver)
    }

    @Test
    fun `saveWork実行時に正しいWorkオブジェクトがリポジトリに渡される`() = runTest {
        // Given
        setupSuccessfulInsert()
        setupValidWorkData()

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
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
        // Given
        setupSuccessfulUpdate()
        setupValidWorkData()

        // When
        viewModel.saveWork(id = TEST_ID, isNew = false)

        // Then
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
        // Given
        setupSuccessfulInsert()
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(SHORT_ELAPSED_HOUR, SHORT_ELAPSED_MINUTE) // 1時間30分

        // When
        viewModel.saveWork(id = 0, isNew = true)

        // Then
        val expectedElapsedTime = SHORT_ELAPSED_HOUR * SECOND_IN_HOURS.toInt() + SHORT_ELAPSED_MINUTE * SECOND_IN_MINUTES.toInt()
        coVerify {
            mockWorkRepository.insert(match { work ->
                work.elapsed_time == expectedElapsedTime // 1.5時間
            })
        }
    }

    @Test
    fun `無効な終了日付形式でエラーメッセージが表示される`() = runTest {
        // Given: 開始日時は有効だが、終了日付が無効な状態
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(INVALID_DATE) // 終了日付を無効にする
        viewModel.updateEndTime(TEST_END_TIME)
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: EditWorkViewModel.UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)
        testDispatcher.scheduler.runCurrent() // 非同期処理を待機

        // Then: 正しいエラーメッセージが表示されることを確認
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        assertEquals(ERROR_MSG_DATE_TIME_PATTERN, (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message)
    }

    @Test
    fun `無効な終了時刻形式でエラーメッセージが表示される`() = runTest {
        // Given: 開始日時は有効だが、終了時刻が無効な状態
        viewModel.updateStartDay(TEST_START_DAY)
        viewModel.updateStartTime(TEST_START_TIME)
        viewModel.updateEndDay(TEST_END_DAY)
        viewModel.updateEndTime(INVALID_TIME) // 終了時刻を無効にする
        viewModel.updateElapsedTime(1, 0)

        var collectedEvent: EditWorkViewModel.UiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            collectedEvent = viewModel.uiEvent.first()
        }

        // When
        viewModel.saveWork(id = 0, isNew = true)
        testDispatcher.scheduler.runCurrent() // 非同期処理を待機

        // Then: 正しいエラーメッセージが表示されることを確認
        assertTrue("ShowSnackbarイベントが発生すること", collectedEvent is EditWorkViewModel.UiEvent.ShowSnackbar)
        assertEquals(ERROR_MSG_DATE_TIME_PATTERN, (collectedEvent as EditWorkViewModel.UiEvent.ShowSnackbar).message)
    }
}
