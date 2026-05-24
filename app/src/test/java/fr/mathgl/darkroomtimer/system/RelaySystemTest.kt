package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class RelaySystemTest {
    private lateinit var testScope: TestScope
    private lateinit var enlargerMock: RelayController
    private lateinit var safelightMock: RelayController
    private lateinit var relaySystem: RelaySystem

    @Before
    fun setup() {
        testScope = kotlinx.coroutines.test.TestScope()
        enlargerMock = mock()
        safelightMock = mock()

        // Default states
        whenever(enlargerMock.state).thenReturn(MutableStateFlow(RelayState.UNKNOWN))
        whenever(safelightMock.state).thenReturn(MutableStateFlow(RelayState.UNKNOWN))
        whenever(enlargerMock.connectionState).thenReturn(MutableStateFlow(ConnectionState.Disconnected))
        whenever(safelightMock.connectionState).thenReturn(MutableStateFlow(ConnectionState.Disconnected))
        whenever(enlargerMock.canPause).thenReturn(true)

        relaySystem = RelaySystem(
            enlarger = enlargerMock,
            safelight = safelightMock,
            scope = testScope
        )
    }

    @Test
    fun `connect should call connect on all controllers`() = runTest {
        relaySystem.connect()
        verify(enlargerMock).connect()
        verify(safelightMock).connect()
    }

    @Test
    fun `disconnect should call disconnect on all controllers`() = runTest {
        relaySystem.disconnect()
        verify(enlargerMock).disconnect()
        verify(safelightMock).disconnect()
    }

    @Test
    fun `relayStates should combine states of enlarger and safelight`() = runTest {
        val enlargerState = MutableStateFlow(RelayState.OFF)
        val safelightState = MutableStateFlow(RelayState.ON)

        whenever(enlargerMock.state).thenReturn(enlargerState)
        whenever(safelightMock.state).thenReturn(safelightState)

        // Need to recreate system to pick up new mocks/flows
        val system = RelaySystem(enlargerMock, safelightMock, testScope)

        val states = system.relayStates.first()
        assertEquals(RelayState.OFF, states.enlarger)
        assertEquals(RelayState.ON, states.safelight)
    }

    @Test
    fun `startTimedExposure should only trigger enlarger`() = runTest {
        relaySystem.startTimedExposure(2000L)
        verify(enlargerMock).startTimed(2000L)
        verify(safelightMock, never()).startTimed(any())
    }

    @Test
    fun `setSafelight should only trigger safelight`() = runTest {
        relaySystem.setSafelight(true)
        verify(safelightMock).set(true)
        verify(enlargerMock, never()).set(any())
    }

    @Test
    fun `capabilities should correctly report canPause and hasSafelight`() {
        assertEquals(true, relaySystem.capabilities.canPause)
        assertEquals(true, relaySystem.capabilities.hasSafelight)
    }

    @Test
    fun `capabilities should report no safelight when null`() {
        val systemNoSafelight = RelaySystem(enlargerMock, null, testScope)
        assertEquals(false, systemNoSafelight.capabilities.hasSafelight)
    }

    @Test
    fun `setEnlarger should return success when enlarger set succeeds`() = runTest {
        whenever(enlargerMock.set(true)).thenReturn(Result.success(Unit))

        val result = relaySystem.setEnlarger(true)

        assertEquals(true, result.isSuccess)
        verify(enlargerMock).set(true)
    }

    @Test
    fun `setEnlarger should return failure when enlarger set fails`() = runTest {
        val exception = IllegalStateException("Connection lost")
        whenever(enlargerMock.set(true)).thenReturn(Result.failure(exception))

        val result = relaySystem.setEnlarger(true)

        assertEquals(false, result.isSuccess)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `setSafelight with null safelight should return success`() = runTest {
        val systemNoSafelight = RelaySystem(enlargerMock, null, testScope)

        val result = systemNoSafelight.setSafelight(true)

        assertEquals(true, result.isSuccess)
    }

    @Test
    fun `setSafelight should call safelight set when safelight exists`() = runTest {
        whenever(safelightMock.set(true)).thenReturn(Result.success(Unit))

        val result = relaySystem.setSafelight(true)

        assertEquals(true, result.isSuccess)
        verify(safelightMock).set(true)
        verify(enlargerMock, never()).set(any())
    }
}
