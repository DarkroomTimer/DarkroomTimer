package fr.mathgl.darkroomtimer.ui

import android.app.Application
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.IncrementType
import fr.mathgl.darkroomtimer.math.TeststripMode
import fr.mathgl.darkroomtimer.system.MockRelaySystem
import fr.mathgl.darkroomtimer.system.TeststripState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TeststripViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var relaySystem: MockRelaySystem
    private lateinit var viewModel: TeststripViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mock()
        relaySystem = MockRelaySystem(TestScope(testDispatcher))
        viewModel = TeststripViewModel(application, { relaySystem })
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // Runs the scheduler enough ticks to let connect() complete and connectionState propagate.
    private fun connectRelay() {
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.runCurrent()
    }

    // ── Initial state ────────────────────────────────────────────────────────────

    @Test
    fun `initial state is INIT`() {
        assertEquals(TeststripState.INIT, viewModel.uiState.value.sessionState)
    }

    @Test
    fun `initial patchCount is 6`() {
        assertEquals(6, viewModel.uiState.value.patchCount)
    }

    // ── Configuration guards ─────────────────────────────────────────────────────

    @Test
    fun `updateBaseTime accepted in INIT`() {
        viewModel.updateBaseTime(12_000L)
        assertEquals(12_000L, viewModel.uiState.value.baseTimeMs)
    }

    @Test
    fun `updateBaseTime rejected when EXPOSING`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TeststripState.EXPOSING, viewModel.uiState.value.sessionState)

        val before = viewModel.uiState.value.baseTimeMs
        viewModel.updateBaseTime(20_000L)
        assertEquals(before, viewModel.uiState.value.baseTimeMs)
    }

    @Test
    fun `updateStopFraction accepted in INIT`() {
        viewModel.updateStopFraction(1, 2)
        assertEquals(1, viewModel.uiState.value.numerator)
        assertEquals(2, viewModel.uiState.value.denominator)
    }

    @Test
    fun `updatePatchCount accepted in INIT`() {
        viewModel.updatePatchCount(8)
        assertEquals(8, viewModel.uiState.value.patchCount)
    }

    @Test
    fun `updatePatchCount rejected when EXPOSING`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        val before = viewModel.uiState.value.patchCount
        viewModel.updatePatchCount(3)
        assertEquals(before, viewModel.uiState.value.patchCount)
    }

    @Test
    fun `updateMode accepted in INIT`() {
        viewModel.updateMode(TeststripMode.INCREMENTAL)
        assertEquals(TeststripMode.INCREMENTAL, viewModel.uiState.value.mode)
    }

    @Test
    fun `updateIncrementType accepted in INIT`() {
        viewModel.updateIncrementType(IncrementType.SECONDS)
        assertEquals(IncrementType.SECONDS, viewModel.uiState.value.incrementType)
    }

    // ── startSession guards ──────────────────────────────────────────────────────

    @Test
    fun `startSession with disconnected relay sets errorMessage`() {
        // Don't run scheduler — relay stays Disconnected (stateIn initialValue)
        viewModel.startSession()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(TeststripState.INIT, viewModel.uiState.value.sessionState)
    }

    @Test
    fun `startSession in INIT with connected relay transitions to EXPOSING`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TeststripState.EXPOSING, viewModel.uiState.value.sessionState)
    }

    @Test
    fun `startSession when already EXPOSING is no-op`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()
        val callsBefore = relaySystem.startTimedExposureCallCount

        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        assertEquals(callsBefore, relaySystem.startTimedExposureCallCount)
        assertEquals(TeststripState.EXPOSING, viewModel.uiState.value.sessionState)
    }

    // ── Pause / Resume ───────────────────────────────────────────────────────────

    @Test
    fun `pause in EXPOSING transitions to PAUSED and shuts off relays`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TeststripState.PAUSED, viewModel.uiState.value.sessionState)
        assertEquals(false, relaySystem.setEnlargerCalls.lastOrNull())
        assertEquals(false, relaySystem.setSafelightCalls.lastOrNull())
    }

    @Test
    fun `resume in PAUSED transitions back to EXPOSING`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        viewModel.resume()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TeststripState.EXPOSING, viewModel.uiState.value.sessionState)
    }

    @Test
    fun `pause when not EXPOSING is no-op`() = runTest {
        // INIT state — pause should do nothing
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()
        assertEquals(TeststripState.INIT, viewModel.uiState.value.sessionState)
        assertEquals(true, relaySystem.setEnlargerCalls.isEmpty())
    }

    // ── finishExposure ───────────────────────────────────────────────────────────

    @Test
    fun `finishExposure transitions to BETWEEN_PATCHES and shuts off relays`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        viewModel.finishExposure()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TeststripState.BETWEEN_PATCHES, viewModel.uiState.value.sessionState)
        assertEquals(false, relaySystem.setEnlargerCalls.lastOrNull())
        assertEquals(false, relaySystem.setSafelightCalls.lastOrNull())
    }

    @Test
    fun `finishing the last patch marks session complete`() = runTest {
        viewModel.updatePatchCount(1)
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        viewModel.finishExposure()
        testDispatcher.scheduler.runCurrent()

        assertEquals(true, viewModel.uiState.value.isSessionComplete)
    }

    // ── Relay failure ────────────────────────────────────────────────────────────

    @Test
    fun `relay failure during exposure sets errorMessage and pauses session`() = runTest {
        connectRelay()
        relaySystem.shouldFailRelays = true

        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(TeststripState.PAUSED, viewModel.uiState.value.sessionState)
    }

    // ── abandon ──────────────────────────────────────────────────────────────────

    @Test
    fun `abandon from EXPOSING resets session to INIT and shuts off relays`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()

        viewModel.abandon()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TeststripState.INIT, viewModel.uiState.value.sessionState)
        assertEquals(false, relaySystem.setEnlargerCalls.lastOrNull())
        assertEquals(false, relaySystem.setSafelightCalls.lastOrNull())
    }

    @Test
    fun `abandon from PAUSED resets session to INIT`() = runTest {
        connectRelay()
        viewModel.startSession()
        testDispatcher.scheduler.runCurrent()
        viewModel.pause()
        testDispatcher.scheduler.runCurrent()

        viewModel.abandon()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TeststripState.INIT, viewModel.uiState.value.sessionState)
    }

    // ── selectGrade ──────────────────────────────────────────────────────────────

    @Test
    fun `selectGrade updates selectedGrade in uiState`() {
        viewModel.selectGrade(ContrastGrade.GRADE_3)
        assertEquals(ContrastGrade.GRADE_3, viewModel.uiState.value.selectedGrade)
    }
}
