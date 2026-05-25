package fr.mathgl.darkroomtimer.ui

import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSessionState
import fr.mathgl.darkroomtimer.development.DevelopmentStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevelopmentFlowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DevelopmentFlowViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DevelopmentFlowViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile() = DevelopmentProfile(
        id = 1L,
        name = "Test Profile",
        steps = listOf(DevelopmentStep.BathStep(name = "Developer", durationSeconds = 60))
    )

    @Test
    fun `selectedProfile is null initially`() {
        assertNull(viewModel.selectedProfile.value)
    }

    @Test
    fun `setSelectedProfile updates selectedProfile`() {
        val p = profile()
        viewModel.setSelectedProfile(p)
        assertEquals(p, viewModel.selectedProfile.value)
    }

    @Test
    fun `startSession sets sessionSnapshot to CONFIGURED`() = runTest {
        viewModel.startSession(profile())
        // runCurrent() processes the collect coroutine without advancing virtual time,
        // so the tick delay(1000) never fires.
        testScheduler.runCurrent()
        assertNotNull(viewModel.sessionSnapshot.value)
        assertEquals(DevelopmentSessionState.CONFIGURED, viewModel.sessionSnapshot.value!!.state)
    }

    @Test
    fun `cancelSession clears selectedProfile and sessionSnapshot`() = runTest {
        viewModel.startSession(profile())
        testScheduler.runCurrent()
        viewModel.cancelSession()
        assertNull(viewModel.selectedProfile.value)
        assertNull(viewModel.sessionSnapshot.value)
    }

    @Test
    fun `sessionStart transitions session to ACTIVE`() = runTest {
        viewModel.startSession(profile())
        testScheduler.runCurrent()
        viewModel.sessionStart()
        testScheduler.runCurrent()
        assertEquals(DevelopmentSessionState.ACTIVE, viewModel.sessionSnapshot.value!!.state)
    }

    @Test
    fun `sessionPause transitions session to PAUSED`() = runTest {
        viewModel.startSession(profile())
        testScheduler.runCurrent()
        viewModel.sessionStart()
        testScheduler.runCurrent()
        viewModel.sessionPause()
        testScheduler.runCurrent()
        assertEquals(DevelopmentSessionState.PAUSED, viewModel.sessionSnapshot.value!!.state)
    }

    @Test
    fun `sessionResume transitions session back to ACTIVE`() = runTest {
        viewModel.startSession(profile())
        testScheduler.runCurrent()
        viewModel.sessionStart()
        testScheduler.runCurrent()
        viewModel.sessionPause()
        testScheduler.runCurrent()
        viewModel.sessionResume()
        testScheduler.runCurrent()
        assertEquals(DevelopmentSessionState.ACTIVE, viewModel.sessionSnapshot.value!!.state)
    }

    @Test
    fun `editingProfile is null initially`() {
        assertNull(viewModel.editingProfile.value)
    }

    @Test
    fun `setEditingProfile updates editingProfile`() {
        val p = profile()
        viewModel.setEditingProfile(p)
        assertEquals(p, viewModel.editingProfile.value)
    }

    @Test
    fun `setEditingProfile with null clears editingProfile`() {
        viewModel.setEditingProfile(profile())
        viewModel.setEditingProfile(null)
        assertNull(viewModel.editingProfile.value)
    }

    @Test
    fun `clearEditingProfile resets to null`() {
        viewModel.setEditingProfile(profile())
        viewModel.clearEditingProfile()
        assertNull(viewModel.editingProfile.value)
    }
}
