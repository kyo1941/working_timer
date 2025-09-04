package com.example.working_timer.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.working_timer.data.db.Work
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkItemComposableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    companion object {
        // 時間定数（秒単位）
        private const val THREE_HOURS_THIRTY_MINUTES_SECONDS = 12600L // 3時間30分
        private const val THREE_HOURS_SECONDS = 10800L // 3時間
        private const val FORTY_FIVE_MINUTES_SECONDS = 2700L // 45分
        private const val TEN_HOURS_FIFTEEN_MINUTES_SECONDS = 36900L // 10時間15分
        private const val TWO_HOURS_SECONDS = 7200L // 2時間

        // テストデータ
        private const val TEST_START_DAY = "2025-01-05"
        private const val TEST_END_DAY = "2025-01-05"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_END_TIME_1 = "12:30"
        private const val TEST_END_TIME_2 = "12:00"
        private const val TEST_END_TIME_3 = "09:45"
        private const val TEST_END_TIME_4 = "18:15"

        private const val CROSS_DAY_START_DAY = "2025-01-04"
        private const val CROSS_DAY_END_DAY = "2025-01-05"
        private const val CROSS_DAY_START_TIME = "23:30"
        private const val CROSS_DAY_END_TIME = "01:30"

        private const val LONG_WORK_START_TIME = "08:00"

        // 表示テキスト
        private const val ACTIVITY_TIME_LABEL = "活動時間    "
        private const val TIME_UNIT_HOUR = "時間 "
        private const val TIME_UNIT_MINUTE = "分"
        private const val START_LABEL = "開始  "
        private const val END_LABEL = "終了  "
        private const val EDIT_DESCRIPTION = "編集"
        private const val DELETE_DESCRIPTION = "削除"

        // ID定数
        private const val TEST_WORK_ID = 1
        private const val MINUTES_ONLY_WORK_ID = 2
        private const val EXACT_HOURS_WORK_ID = 3
        private const val LONG_WORK_ID = 4
        private const val CROSS_DAY_WORK_ID = 5
    }

    // テストデータファクトリーメソッド
    private fun createTestWork(
        id: Int = TEST_WORK_ID,
        startDay: String = TEST_START_DAY,
        endDay: String = TEST_END_DAY,
        startTime: String = TEST_START_TIME,
        endTime: String = TEST_END_TIME_1,
        elapsedTime: Long = THREE_HOURS_THIRTY_MINUTES_SECONDS
    ) = Work(
        id = id,
        start_day = startDay,
        end_day = endDay,
        start_time = startTime,
        end_time = endTime,
        elapsed_time = elapsedTime
    )

    // 共通アサーションメソッド
    private fun assertWorkItemDisplaysCorrectly(work: Work) {
        composeTestRule.onNodeWithText(ACTIVITY_TIME_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText("$START_LABEL${work.start_time}").assertIsDisplayed()
        composeTestRule.onNodeWithText("$END_LABEL${work.end_time}").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(EDIT_DESCRIPTION).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(DELETE_DESCRIPTION).assertIsDisplayed()
    }

    private fun assertTimeDisplayCorrectly(hours: Long, minutes: Long) {
        if (hours > 0) {
            composeTestRule.onNodeWithText(hours.toString()).assertIsDisplayed()
            composeTestRule.onNodeWithText(TIME_UNIT_HOUR).assertIsDisplayed()
        } else {
            composeTestRule.onNodeWithText(TIME_UNIT_HOUR).assertDoesNotExist()
        }
        composeTestRule.onNodeWithText(minutes.toString()).assertIsDisplayed()
        composeTestRule.onNodeWithText(TIME_UNIT_MINUTE).assertIsDisplayed()
    }

    private fun setupWorkItemComposable(
        work: Work,
        onEdit: () -> Unit = { },
        onDelete: () -> Unit = { }
    ) {
        composeTestRule.setContent {
            WorkItemComposable(
                work = work,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }

    @Test
    fun `WorkItemComposableが正しい情報を表示する`() {
        // Given
        val testWork = createTestWork()

        // When
        setupWorkItemComposable(testWork)

        // Then
        assertWorkItemDisplaysCorrectly(testWork)
        assertTimeDisplayCorrectly(hours = 3, minutes = 30)
    }

    @Test
    fun `編集ボタンをクリックするとコールバックが呼ばれる`() {
        // Given
        val testWork = createTestWork()
        var editClicked = false

        setupWorkItemComposable(
            work = testWork,
            onEdit = { editClicked = true }
        )

        // When
        composeTestRule.onNodeWithContentDescription(EDIT_DESCRIPTION).performClick()

        // Then
        assert(editClicked)
    }

    @Test
    fun `削除ボタンをクリックするとコールバックが呼ばれる`() {
        // Given
        val testWork = createTestWork()
        var deleteClicked = false

        setupWorkItemComposable(
            work = testWork,
            onDelete = { deleteClicked = true }
        )

        // When
        composeTestRule.onNodeWithContentDescription(DELETE_DESCRIPTION).performClick()

        // Then
        assert(deleteClicked)
    }

    @Test
    fun `時間が0時間の場合時間表示が非表示になる`() {
        // Given - 1時間未満（45分）のテストデータ
        val minutesOnlyWork = createTestWork(
            id = MINUTES_ONLY_WORK_ID,
            startTime = TEST_START_TIME,
            endTime = TEST_END_TIME_3,
            elapsedTime = FORTY_FIVE_MINUTES_SECONDS
        )

        // When
        setupWorkItemComposable(minutesOnlyWork)

        // Then
        assertTimeDisplayCorrectly(hours = 0, minutes = 45)
    }

    @Test
    fun `正確な時間の場合時間と分両方が表示される`() {
        // Given - 正確に3時間（分は0）のテストデータ
        val exactHoursWork = createTestWork(
            id = EXACT_HOURS_WORK_ID,
            endTime = TEST_END_TIME_2,
            elapsedTime = THREE_HOURS_SECONDS
        )

        // When
        setupWorkItemComposable(exactHoursWork)

        // Then
        assertTimeDisplayCorrectly(hours = 3, minutes = 0)
    }

    @Test
    fun `長時間作業の場合正しく表示される`() {
        // Given - 長時間（10時間15分）のテストデータ
        val longWork = createTestWork(
            id = LONG_WORK_ID,
            startTime = LONG_WORK_START_TIME,
            endTime = TEST_END_TIME_4,
            elapsedTime = TEN_HOURS_FIFTEEN_MINUTES_SECONDS
        )

        // When
        setupWorkItemComposable(longWork)

        // Then
        assertTimeDisplayCorrectly(hours = 10, minutes = 15)
    }

    @Test
    fun `開始時間と終了時間が正しく表示される`() {
        // Given - 異なる時間のテストデータ
        val workWithDifferentTimes = createTestWork(
            id = CROSS_DAY_WORK_ID,
            startDay = CROSS_DAY_START_DAY,
            endDay = CROSS_DAY_END_DAY,
            startTime = CROSS_DAY_START_TIME,
            endTime = CROSS_DAY_END_TIME,
            elapsedTime = TWO_HOURS_SECONDS
        )

        // When
        setupWorkItemComposable(workWithDifferentTimes)

        // Then
        composeTestRule.onNodeWithText("$START_LABEL$CROSS_DAY_START_TIME").assertIsDisplayed()
        composeTestRule.onNodeWithText("$END_LABEL$CROSS_DAY_END_TIME").assertIsDisplayed()
        assertTimeDisplayCorrectly(hours = 2, minutes = 0)
    }

    @Test
    fun `アイコンボタンが正しく動作し複数回クリックにも対応する`() {
        // Given
        val testWork = createTestWork()
        var editCallbackCount = 0
        var deleteCallbackCount = 0

        setupWorkItemComposable(
            work = testWork,
            onEdit = { editCallbackCount++ },
            onDelete = { deleteCallbackCount++ }
        )

        // When
        composeTestRule.onNodeWithContentDescription(EDIT_DESCRIPTION).performClick()
        composeTestRule.onNodeWithContentDescription(DELETE_DESCRIPTION).performClick()

        // 複数回クリックをテスト
        composeTestRule.onNodeWithContentDescription(EDIT_DESCRIPTION).performClick()

        // Then
        assert(editCallbackCount == 2)
        assert(deleteCallbackCount == 1)
    }
}
