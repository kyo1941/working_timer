package com.example.working_timer.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.working_timer.util.Constants.SECOND_IN_HOURS
import com.example.working_timer.util.SharedPrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreManagerImplTest {

    companion object {
        private const val TEST_START_DATE = "2025-01-04"
        private const val TEST_START_TIME = "09:00"
        private const val TEST_ELAPSED_TIME = SECOND_IN_HOURS
        private const val UPDATED_ELAPSED_TIME = SECOND_IN_HOURS * 2
        private const val DEFAULT_ELAPSED_TIME = 0L
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 各テスト内で呼び出すための共通初期化ヘルパー関数。
     * TestScopeの拡張関数として定義することで、backgroundScopeを利用可能にする。
     */
    private fun TestScope.createDataStoreManager(): DataStoreManagerImpl {
        val testFile = File(tempFolder.root, "test-prefs.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { testFile }
        )
        return DataStoreManagerImpl(dataStore, testDispatcher)
    }

    @Test
    fun `SharedPrefKeysの定数が正しく定義されていることを確認`() {
        assertEquals("TimerPrefs", SharedPrefKeys.PREFS_NAME)
        assertEquals("startDate", SharedPrefKeys.START_DATE_KEY)
        assertEquals("startTimeString", SharedPrefKeys.START_TIME_STRING_KEY)
        assertEquals("elapsedTime", SharedPrefKeys.ELAPSED_TIME_KEY)
    }

    @Test
    fun `Constants定数が正しく定義されていることを確認`() {
        assertEquals(3600L, SECOND_IN_HOURS)
        assertEquals(SECOND_IN_HOURS, TEST_ELAPSED_TIME)
        assertEquals(SECOND_IN_HOURS * 2, UPDATED_ELAPSED_TIME)
    }

    @Test
    fun `saveTimerState実行時にデータが正しく保存される`() = runTest(testDispatcher) {
        // GIVEN: ヘルパー関数を使ってインスタンスを生成
        val dataStoreManager = createDataStoreManager()

        // WHEN
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // THEN
        val savedElapsedTime = dataStoreManager.getElapsedTime().first()
        val savedStartDate = dataStoreManager.getStartDate().first()
        val savedStartTime = dataStoreManager.getStartTime().first()

        assertEquals(TEST_ELAPSED_TIME, savedElapsedTime)
        assertEquals(TEST_START_DATE, savedStartDate)
        assertEquals(TEST_START_TIME, savedStartTime)
    }

    // Windows環境下だと失敗するが、MacやLinux環境では成功すると考えられる
    @Test
    fun `updateElapsedTime実行時に経過時間が正しく更新される`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // WHEN
        dataStoreManager.updateElapsedTime(UPDATED_ELAPSED_TIME)

        // THEN
        val savedElapsedTime = dataStoreManager.getElapsedTime().first()
        val savedStartDate = dataStoreManager.getStartDate().first()
        val savedStartTime = dataStoreManager.getStartTime().first()

        assertEquals(UPDATED_ELAPSED_TIME, savedElapsedTime)
        assertEquals(TEST_START_DATE, savedStartDate)
        assertEquals(TEST_START_TIME, savedStartTime)
    }

    // Windows環境下だと失敗するが、MacやLinux環境では成功すると考えられる
    @Test
    fun `clearTimerState実行時にすべてのデータがクリアされる`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // WHEN
        dataStoreManager.clearTimerState()

        // THEN
        val savedElapsedTime = dataStoreManager.getElapsedTime().first()
        val savedStartDate = dataStoreManager.getStartDate().first()
        val savedStartTime = dataStoreManager.getStartTime().first()

        assertEquals(DEFAULT_ELAPSED_TIME, savedElapsedTime)
        assertNull(savedStartDate)
        assertNull(savedStartTime)
    }

    @Test
    fun `初期状態でデフォルト値が返される`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()

        // WHEN & THEN
        val elapsedTime = dataStoreManager.getElapsedTime().first()
        val startDate = dataStoreManager.getStartDate().first()
        val startTime = dataStoreManager.getStartTime().first()

        assertEquals(DEFAULT_ELAPSED_TIME, elapsedTime)
        assertNull(startDate)
        assertNull(startTime)
    }

    @Test
    fun `getElapsedTimeSyncが正しい値を返す`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // WHEN
        val syncElapsedTime = dataStoreManager.getElapsedTimeSync()

        // THEN
        assertEquals(TEST_ELAPSED_TIME, syncElapsedTime)
    }

    @Test
    fun `getStartDateSyncが正しい値を返す`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // WHEN
        val syncStartDate = dataStoreManager.getStartDateSync()

        // THEN
        assertEquals(TEST_START_DATE, syncStartDate)
    }

    @Test
    fun `getStartTimeSyncが正しい値を返す`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()
        dataStoreManager.saveTimerState(TEST_START_DATE, TEST_START_TIME, TEST_ELAPSED_TIME)

        // WHEN
        val syncStartTime = dataStoreManager.getStartTimeSync()

        // THEN
        assertEquals(TEST_START_TIME, syncStartTime)
    }

    @Test
    fun `未保存状態でgetSyncメソッドがデフォルト値を返す`() = runTest(testDispatcher) {
        // GIVEN
        val dataStoreManager = createDataStoreManager()

        // WHEN & THEN
        val syncElapsedTime = dataStoreManager.getElapsedTimeSync()
        val syncStartDate = dataStoreManager.getStartDateSync()
        val syncStartTime = dataStoreManager.getStartTimeSync()

        assertEquals(DEFAULT_ELAPSED_TIME, syncElapsedTime)
        assertNull(syncStartDate)
        assertNull(syncStartTime)
    }
}

