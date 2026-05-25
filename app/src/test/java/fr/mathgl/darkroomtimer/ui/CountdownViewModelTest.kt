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
import kotlinx.coroutines.CoroutineScope
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

    class TestCountdownViewModel(
        app: Application,
        factory: (CoroutineScope) -> RelaySystem,
        timer: CountdownTimer
    ) : CountdownViewModel(app, factory, timer = timer) {
        val serviceIntents = mutableListOf<Pair<String, Long>>()
        override fun sendServiceIntent(action: String, remainingMs: Long) {
            serviceIntents.add(action to remainingMs)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        relaySystem = MockRelaySystem(TestScope(testDispatcher))

        val timer = CountdownTimer(clock = { testDispatcher.scheduler.currentTime })

        viewModel = TestCountdownViewModel(application, { relaySystem }, timer)
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

        val vm = TestCountdownViewModel(application, { relaySystemNoPause }, CountdownTimer(clock = { testDispatcher.scheduler.currentTime }))
        vm.start()
        testDispatcher.scheduler.runCurrent()

        assertEquals(true, relaySystemNoPause.setEnlargerCalled)
        assertEquals(true, relaySystemNoPause.setSafelightCalled)
        assertEquals(false, relaySystemNoPause.startTimedExposureCalled)
    }

    @Test
    fun `adjustTime should update displayTime based on remaining time when PAUSED`() = runTest {
        // Base time = 8000
        val initialBaseTime = 8000L
        // I can't easily set configuredTimeMs on the timer inside the VM because it's private.
        // But I can use adjustTime to set it.
        val diff = initialBaseTime - viewModel.uiState.value.configuredTimeMs
        viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // Start timer
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Advance scheduler by 2 seconds so the timer clock reads 2000ms elapsed
        testDispatcher.scheduler.advanceTimeBy(2000L)
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        // Remaining time is 6000. UI should show 00:06.0
        assertEquals("00:06.0", viewModel.uiState.value.displayTime)

        // Add 10 seconds
        viewModel.adjustTime(10000L)
        testDispatcher.scheduler.runCurrent()

        // New base time = 18000.
        // Remaining time = 6000 + 10000 = 16000.
        // UI should show 00:16.0
        assertEquals("00:16.0", viewModel.uiState.value.displayTime)
    }

    @Test
    fun `initial fStopCorrectionNumerator should be 0`() = runTest {
        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    }

    @Test
    fun `initial fStopCorrectionDenominator should be 1`() = runTest {
        assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
    }

    @Test
    fun `start should use calculatedTimeMs when correction is active`() = runTest {
        // Set base to 8000ms (default is 8000 in test, but be explicit)
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // Apply +1 stop → calculatedTimeMs = 16000ms
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        // Advance past base time (8000ms) but before calculated time (16000ms)
        testDispatcher.scheduler.advanceTimeBy(9000L)
        testDispatcher.scheduler.runCurrent()

        // If start() used baseTimeMs (8000ms), the timer would have ended — so STOPPED.
        // If start() used calculatedTimeMs (16000ms), the timer is still running.
        assertEquals(TimerState.RUNNING, viewModel.uiState.value.timerState)
    }

    @Test
    fun `stop should display calculatedTimeMs after stopping`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // Apply +1 stop → calculatedTimeMs = 16000ms → displayTime = "00:16.0"
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()
        viewModel.stop()
        testDispatcher.scheduler.runCurrent()

        assertEquals("00:16.0", viewModel.uiState.value.displayTime)
    }

    @Test
    fun `adjustTime when STOPPED should adjust base time while keeping correction`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // Apply +1/3 stop
        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        // Adjust base by +1000ms
        viewModel.adjustTime(1000L)
        testDispatcher.scheduler.runCurrent()

        // Base should be 9000ms
        assertEquals(9000L, viewModel.uiState.value.configuredTimeMs)
        // Correction unchanged
        assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(3, viewModel.uiState.value.fStopCorrectionDenominator)
        // displayTime = formatTime(9000 * 2^(1/3)) = formatTime(FStopMath.adjustTime(9000, 1, 3, 1))
        val expectedMs = fr.mathgl.darkroomtimer.math.FStopMath.adjustTime(9000L, 1, 3, 1)
        assertEquals(CountdownTimer.formatTime(expectedMs), viewModel.uiState.value.displayTime)
    }

    @Test
    fun `setTimeFromInput should reset fstop correction to zero`() = runTest {
        viewModel.applyFStopDelta(1, 2)
        testDispatcher.scheduler.runCurrent()

        viewModel.setTimeFromInput(0, 10, 0)  // 10 seconds
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
        assertEquals(10_000L, viewModel.uiState.value.configuredTimeMs)
        assertEquals(CountdownTimer.formatTime(10_000L), viewModel.uiState.value.displayTime)
    }

    @Test
    fun `applyFStopDelta should accumulate same-denominator fractions correctly`() = runTest {
        // +1/3 + 1/3 = 2/3 (simplified)
        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()
        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        assertEquals(2, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(3, viewModel.uiState.value.fStopCorrectionDenominator)
    }

    @Test
    fun `applyFStopDelta should accumulate different-denominator fractions correctly`() = runTest {
        // +1/2 + 1/3 = 5/6
        viewModel.applyFStopDelta(1, 2)
        testDispatcher.scheduler.runCurrent()
        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        assertEquals(5, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(6, viewModel.uiState.value.fStopCorrectionDenominator)
    }

    @Test
    fun `applyFStopDelta should update displayTime to calculated time`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // +1 stop on 8000ms → 16000ms → "00:16.0"
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        assertEquals("00:16.0", viewModel.uiState.value.displayTime)
    }

    @Test
    fun `applyFStopDelta should be rejected when result would be below 100ms`() = runTest {
        // Set base to 200ms
        viewModel.adjustTime(200L - viewModel.uiState.value.configuredTimeMs)
        testDispatcher.scheduler.runCurrent()

        // -1 stop: 200 * 2^(-1) = 100ms → accepted (boundary value)
        viewModel.applyFStopDelta(-1, 1)
        testDispatcher.scheduler.runCurrent()
        assertEquals(-1, viewModel.uiState.value.fStopCorrectionNumerator)

        // Another -1 stop: 200 * 2^(-2) = 50ms → rejected
        viewModel.applyFStopDelta(-1, 1)
        testDispatcher.scheduler.runCurrent()
        assertEquals(-1, viewModel.uiState.value.fStopCorrectionNumerator) // unchanged
    }

    @Test
    fun `applyFStopDelta should be rejected when result would exceed 999000ms`() = runTest {
        // Set base to 700000ms (700s)
        viewModel.adjustTime(700_000L - viewModel.uiState.value.configuredTimeMs)
        testDispatcher.scheduler.runCurrent()

        // +1 stop: 700000 * 2 = 1400000ms → rejected (> 999000)
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator) // unchanged
    }

    @Test
    fun `applyFStopDelta should do nothing when timer is RUNNING`() = runTest {
        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    }

    @Test
    fun `resetFStopCorrection should clear correction and restore base displayTime`() = runTest {
        val baseMs = viewModel.uiState.value.configuredTimeMs

        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        viewModel.resetFStopCorrection()
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
        assertEquals(CountdownTimer.formatTime(baseMs), viewModel.uiState.value.displayTime)
    }

    @Test
    fun `setFStopCorrectionAsBase should promote calculated time to base`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        // +1 stop → 16000ms
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.setFStopCorrectionAsBase()
        testDispatcher.scheduler.runCurrent()

        assertEquals(16_000L, viewModel.uiState.value.configuredTimeMs)
        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
        assertEquals(CountdownTimer.formatTime(16_000L), viewModel.uiState.value.displayTime)
    }

    @Test
    fun `applyFStopDelta should do nothing when timer is PAUSED`() = runTest {
        viewModel.start()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        viewModel.applyFStopDelta(1, 3)
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    }

    @Test
    fun `resetFStopCorrection should do nothing when timer is RUNNING`() = runTest {
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        viewModel.resetFStopCorrection()
        testDispatcher.scheduler.runCurrent()

        // Correction unchanged — still 1/1
        assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(TimerState.RUNNING, viewModel.uiState.value.timerState)
    }

    @Test
    fun `setFStopCorrectionAsBase should do nothing when timer is RUNNING`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()

        viewModel.setFStopCorrectionAsBase()
        testDispatcher.scheduler.runCurrent()

        // Correction unchanged — still 1/1, base still 8000
        assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(8000L, viewModel.uiState.value.configuredTimeMs)
    }

    @Test
    fun `resetFStopCorrection should do nothing when timer is PAUSED`() = runTest {
        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        viewModel.resetFStopCorrection()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(TimerState.PAUSED, viewModel.uiState.value.timerState)
    }

    @Test
    fun `setFStopCorrectionAsBase should do nothing when timer is PAUSED`() = runTest {
        val diff = 8000L - viewModel.uiState.value.configuredTimeMs
        if (diff != 0L) viewModel.adjustTime(diff)
        testDispatcher.scheduler.runCurrent()

        viewModel.applyFStopDelta(1, 1)
        testDispatcher.scheduler.runCurrent()

        viewModel.start()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        viewModel.setFStopCorrectionAsBase()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
        assertEquals(8000L, viewModel.uiState.value.configuredTimeMs)
    }
}
