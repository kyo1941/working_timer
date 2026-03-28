# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew assembleDebug

# Unit tests
./gradlew testDebugUnitTest

# Single test class
./gradlew testDebugUnitTest --tests "com.example.working_timer.ui.main.MainViewModelTest"

# Single test method
./gradlew testDebugUnitTest --tests "com.example.working_timer.ui.main.MainViewModelTest.testMethodName"

# Test with coverage (JaCoCo report at app/build/reports/jacoco/testCoverage/html/)
./gradlew testCoverage

# Lint (Detekt with autoCorrect)
./gradlew detekt
```

## Architecture

MVVM + Repository pattern with Dagger Hilt DI. Jetpack Compose UI.

**Layers:**
- `ui/` — Screens and ViewModels. Each screen has its own subdirectory with a ViewModel (StateFlow-based state), a Screen composable, and section composables.
- `domain/repository/` — Repository and manager interfaces.
- `data/repository/` — Implementations of domain interfaces (Room, DataStore, TimerService binding).
- `data/db/` — Room database, `Work` entity, DAO.
- `service/` — `TimerService` (foreground service), `TimerActionReceiver` (notification actions). The timer runs here and exposes `TimerState` via StateFlow.
- `di/` — Hilt modules binding interfaces to implementations and providing DB/DataStore instances.
- `navigation/` — Compose Navigation host and route definitions.

**Timer flow:** `TimerService` (foreground service) → `TimerManagerImpl` binds to the service and exposes its StateFlow → `MainViewModel` collects it for UI state.

**Key data:** `Work` entity has `id`, `start_day`, `start_time`, `elapsed_time`. Period-based salary and hour summaries are computed in `LogViewViewModel`.

## Testing

Tests use MockK, Turbine (Flow testing), and `kotlinx-coroutines-test`. Pattern:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `description`() = runTest {
        viewModel.uiState.test {
            // assert emissions with Turbine
        }
    }
}
```

Tests live in `app/src/test/java/com/example/working_timer/`.
