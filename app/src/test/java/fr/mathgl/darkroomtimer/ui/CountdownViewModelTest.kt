package fr.mathgl.darkroomtimer.ui

import android.app.Application
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.MockRelayController
import fr.mathgl.darkroomtimer.system.MockRelaySystem
import fr.mathgl.darkroomtimer.system.MockRelaySystemNoPause
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class CountdownViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var relaySystem: MockRelaySystem
    private lateinit var viewModel: TestCountdownViewModel

    class TestCountdownViewModel(app: Application, rs: RelaySystem) : CountdownViewModel(app, rs) {
        override fun sendServiceIntent(action: String, remainingMs: Long) {
            // Do nothing in tests
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        relaySystem = MockRelaySystem(TestScope(testDispatcher))

        viewModel = TestCountdownViewModel(application, relaySystem)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start should trigger timed exposure on relay system`() = runTest {
        viewModel.start()
        // Advance time to let the startTimedExposure coroutine execute
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, relaySystem.startTimedExposureCalled)
    }

    @Test
    fun `stop should turn off relays`() = runTest {
        // First start the timer (which calls startTimedExposure)
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Reset flags to test stop separately
        relaySystem.setEnlargerCalled = false
        relaySystem.setSafelightCalled = false

        // Call stop which should turn off the enlarger and safelight
        viewModel.stop()
        testDispatcher.scheduler.runCurrent()

        // Stop should call setEnlarger(false) and setSafelight(false)
        assertEquals(true, relaySystem.setEnlargerCalled)
        assertEquals(true, relaySystem.setSafelightCalled)
    }

    @Test
    fun `uiState relayState should reflect relaySystem state`() = runTest {
        assertEquals(RelayStates.INITIAL, viewModel.uiState.value.relayState)
    }

    @Test
    fun `pause should stop timer and set PAUSED state`() = runTest {
        // First start the timer
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Reset the tick job so pause can cancel it
        // Call pause
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerState.PAUSED, viewModel.uiState.value.timerState)
    }

    @Test
    fun `resume should restart timer from PAUSED state`() = runTest {
        // Start then pause to get into PAUSED state
        viewModel.start()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerState.PAUSED, viewModel.uiState.value.timerState)

        // Resume
        viewModel.resume()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TimerState.RUNNING, viewModel.uiState.value.timerState)
    }

    @Test
    fun `adjustTime should increase time when timer is STOPPED`() = runTest {
        val initialTime = viewModel.uiState.value.configuredTimeMs

        viewModel.adjustTime(1000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(initialTime + 1000L, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `adjustTime should decrease time when timer is STOPPED`() = runTest {
        val initialTime = viewModel.uiState.value.configuredTimeMs

        viewModel.adjustTime(-500L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(initialTime - 500L, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `adjustTime should respect minimum time of 100ms`() = runTest {
        // Set time to near minimum
        viewModel.adjustTime(-viewModel.uiState.value.configuredTimeMs + 150L)
        testDispatcher.scheduler.runCurrent()

        // Try to go below minimum
        viewModel.adjustTime(-1000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(100L, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `adjustTime should respect maximum time of 999000ms`() = runTest {
        // Set time to near maximum
        viewModel.adjustTime(999_000L - viewModel.uiState.value.configuredTimeMs + 100L)
        testDispatcher.scheduler.runCurrent()

        // Try to go above maximum
        viewModel.adjustTime(10000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(999_000L, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `adjustTime should not change time when timer is RUNNING`() = runTest {
        val initialTime = viewModel.uiState.value.configuredTimeMs

        // Start the timer
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Try to adjust time while running
        viewModel.adjustTime(1000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(initialTime, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `selectGrade should update selectedGrade in uiState`() = runTest {
        viewModel.selectGrade(ContrastGrade.GRADE_3)
        testDispatcher.scheduler.runCurrent()

        assertEquals(ContrastGrade.GRADE_3, viewModel.uiState.value.selectedGrade)
    }

    @Test
    fun `start should return early when timer is already RUNNING`() = runTest {
        // Start the timer
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        val startCount = relaySystem.startTimedExposureCalled

        // Try to start again
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Should only have called startTimedExposure once
        assertEquals(startCount, relaySystem.startTimedExposureCalled)
    }

    @Test
    fun `stop should return early when timer is already STOPPED`() = runTest {
        // Reset flags
        relaySystem.setEnlargerCalled = false
        relaySystem.setSafelightCalled = false

        // Try to stop when already stopped
        viewModel.stop()
        testDispatcher.scheduler.runCurrent()

        // Should not have called set methods
        assertEquals(false, relaySystem.setEnlargerCalled)
        assertEquals(false, relaySystem.setSafelightCalled)
    }

    @Test
    fun `start with canPause false should use setEnlarger and setSafelight`() = runTest {
        // Create a relay system with canPause = false
        val mockController = MockRelayController(canPause = false)
        val relaySystemNoPause = MockRelaySystemNoPause(
            scope = TestScope(testDispatcher),
            controller = mockController
        )

        val vm = TestCountdownViewModel(application, relaySystemNoPause)
        vm.start()
        testDispatcher.scheduler.runCurrent()

        assertEquals(true, relaySystemNoPause.setEnlargerCalled)
        assertEquals(true, relaySystemNoPause.setSafelightCalled)
        assertEquals(false, relaySystemNoPause.startTimedExposureCalled)
    }
}
