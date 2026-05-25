package fr.mathgl.darkroomtimer.system

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CountdownTimerTest {

    private var fakeNow = 0L
    private lateinit var timer: CountdownTimer

    @Before
    fun setup() {
        fakeNow = 0L
        timer = CountdownTimer(clock = { fakeNow })
    }

    // --- État initial ---

    @Test
    fun `initial state is STOPPED`() {
        assertEquals(TimerState.STOPPED, timer.state)
        assertFalse(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `initial remaining equals configuredTime`() {
        timer.configuredTimeMs = 8000L
        assertEquals(8000L, timer.remainingMs())
    }

    // --- Transitions d'état ---

    @Test
    fun `start transitions from STOPPED to RUNNING`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        assertEquals(TimerState.RUNNING, timer.state)
        assertTrue(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `pause transitions from RUNNING to PAUSED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        assertEquals(TimerState.PAUSED, timer.state)
        assertTrue(timer.isStarted)
        assertTrue(timer.isPaused)
    }

    @Test
    fun `resume transitions from PAUSED to RUNNING`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        timer.resume()
        assertEquals(TimerState.RUNNING, timer.state)
        assertTrue(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `stop from RUNNING transitions to STOPPED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.stop()
        assertEquals(TimerState.STOPPED, timer.state)
        assertFalse(timer.isStarted)
    }

    @Test
    fun `stop from PAUSED transitions to STOPPED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        timer.stop()
        assertEquals(TimerState.STOPPED, timer.state)
    }

    @Test(expected = IllegalStateException::class)
    fun `start from RUNNING throws`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.start()
    }

    @Test(expected = IllegalStateException::class)
    fun `pause from STOPPED throws`() {
        timer.pause()
    }

    @Test(expected = IllegalStateException::class)
    fun `resume from STOPPED throws`() {
        timer.resume()
    }

    // --- Calcul du temps restant ---

    @Test
    fun `remaining decreases while RUNNING`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        assertEquals(5000L, timer.remainingMs())
    }

    @Test
    fun `remaining is frozen while PAUSED`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        timer.pause()
        fakeNow = 6000L  // le temps passe mais le timer est pausé
        assertEquals(5000L, timer.remainingMs())
    }

    @Test
    fun `elapsed before pause is preserved after resume`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 2000L
        timer.pause()      // 2s écoulées, 6s restantes
        fakeNow = 5000L
        timer.resume()     // la pause dure 3s, non comptabilisée
        fakeNow = 7000L    // 2s de plus depuis resume
        assertEquals(4000L, timer.remainingMs())  // 8000 - 2000 - 2000 = 4000
    }

    @Test
    fun `remaining can be negative when timer overruns`() {
        timer.configuredTimeMs = 1000L
        fakeNow = 0L
        timer.start()
        fakeNow = 2000L
        assertTrue(timer.remainingMs() < 0)
    }

    // --- Détection de fin ---

    @Test
    fun `tick returns true and transitions to STOPPED when remaining reaches zero`() {
        timer.configuredTimeMs = 1000L
        fakeNow = 0L
        timer.start()
        fakeNow = 1000L
        val ended = timer.tick()
        assertTrue(ended)
        assertEquals(TimerState.STOPPED, timer.state)
    }

    @Test
    fun `tick returns false when still running`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        assertFalse(timer.tick())
        assertEquals(TimerState.RUNNING, timer.state)
    }

    @Test
    fun `tick does nothing when STOPPED`() {
        assertFalse(timer.tick())
        assertEquals(TimerState.STOPPED, timer.state)
    }

    // --- Limites du temps configuré ---

    @Test(expected = IllegalArgumentException::class)
    fun `configuredTime below 100ms throws`() {
        timer.configuredTimeMs = 99L
    }

    @Test(expected = IllegalArgumentException::class)
    fun `configuredTime above 999000ms throws`() {
        timer.configuredTimeMs = 999_001L
    }

    @Test
    fun `configuredTime accepts boundary values`() {
        timer.configuredTimeMs = 100L
        timer.configuredTimeMs = 999_000L
    }

    // --- Affichage MM:SS.d ---

    @Test
    fun `formatTime returns MM_SS_d format`() {
        assertEquals("00:08.0", CountdownTimer.formatTime(8000L))
        assertEquals("01:05.4", CountdownTimer.formatTime(65400L))
        assertEquals("16:39.0", CountdownTimer.formatTime(999_000L))
        assertEquals("00:00.1", CountdownTimer.formatTime(100L))
    }

    @Test
    fun `formatTime clamps negative values to zero`() {
        assertEquals("00:00.0", CountdownTimer.formatTime(-500L))
    }

    @Test
    fun `remaining time is consistent after pause, increment, and resume`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()

        fakeNow = 2000L
        timer.pause()
        assertEquals(6000L, timer.remainingMs())

        timer.configuredTimeMs = 18000L
        assertEquals(16000L, timer.remainingMs())

        fakeNow = 5000L
        timer.resume()

        // Immediately after resume, it should be exactly 16000
        assertEquals(16000L, timer.remainingMs())

        fakeNow = 6000L
        assertEquals(15000L, timer.remainingMs())
    }
}
