package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.math.IncrementType
import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.math.TeststripMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TeststripSessionTest {

    private var fakeNow = 0L
    private lateinit var session: TeststripSession

    @Before
    fun setup() {
        fakeNow = 0L
        session = TeststripSession(
            engine = TeststripEngine(
                baseTimeMs = 8000,
                numerator = 1,
                denominator = 3,
                patchCount = 6,
                mode = TeststripMode.SEPARATE,
                incrementType = IncrementType.F_STOP
            ),
            clock = { fakeNow }
        )
    }

    // --- État initial ---

    @Test
    fun `initial state is INIT`() {
        assertEquals(TeststripState.INIT, session.state)
        assertEquals(-1, session.currentPatchIndex)
    }

    @Test
    fun `initially no exposure is running`() {
        assertFalse(session.isExposing)
        assertFalse(session.isSessionComplete)
    }

    // --- Démarrer une session ---

    @Test
    fun `start exposes first patch`() {
        session.start()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
        assertTrue(session.isExposing)
    }

    @Test
    fun `start sets correct exposure time for first patch`() {
        session.start()
        assertEquals(8000L, session.currentExposureTimeMs)
    }

    // --- Fin d'exposition d'un patch ---

    @Test
    fun `finishExposure transitions to BETWEEN_PATCHES`() {
        session.start()
        session.finishExposure()
        assertEquals(TeststripState.BETWEEN_PATCHES, session.state)
        assertFalse(session.isExposing)
    }

    @Test
    fun `finishExposure marks first patch as exposed`() {
        session.start()
        session.finishExposure()
        assertTrue(session.isPatchExposed(0))
        assertFalse(session.isPatchExposed(1))
    }

    // --- Aller au patch suivant ---

    @Test
    fun `nextPatch starts exposure for next patch`() {
        session.start()
        session.finishExposure()
        session.nextPatch()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(1, session.currentPatchIndex)
        assertEquals(2079L, session.currentExposureTimeMs)
    }

    @Test
    fun `nextPatch wraps around to first patch after last patch`() {
        // Expose first patch
        session.start()
        session.finishExposure()
        // Expose remaining 5 patches using nextPatch
        repeat(5) {
            session.nextPatch()
            session.finishExposure()
        }
        // Now at patch 5 (last), call nextPatch -> should wrap to patch 0
        session.nextPatch()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
    }

    // --- Session complète ---

    @Test
    fun `session complete after all patches exposed and in BETWEEN_PATCHES`() {
        // Expose first patch
        session.start()
        session.finishExposure()
        // Expose remaining 5 patches using nextPatch
        repeat(5) {
            session.nextPatch()
            session.finishExposure()
        }
        assertEquals(TeststripState.BETWEEN_PATCHES, session.state)
        assertTrue(session.isSessionComplete)
    }

    @Test
    fun `nextPatch after completion resets and starts new session`() {
        // Expose first patch
        session.start()
        session.finishExposure()
        // Expose remaining 5 patches using nextPatch
        repeat(5) {
            session.nextPatch()
            session.finishExposure()
        }
        session.nextPatch()  // This should start a new session, wrapping to patch 0
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
        assertFalse(session.isSessionComplete)
    }

    // --- Recommencer le patch courant ---

    @Test
    fun `restartCurrentPatch keeps same patch index`() {
        session.start()
        session.finishExposure()
        session.restartCurrentPatch()
        assertEquals(0, session.currentPatchIndex)
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(8000L, session.currentExposureTimeMs)
    }

    @Test
    fun `restartCurrentPatch resets exposed status for current patch`() {
        session.start()
        session.finishExposure()
        assertTrue(session.isPatchExposed(0))
        session.restartCurrentPatch()
        assertFalse(session.isPatchExposed(0))
    }

    // --- Abandonner la session ---

    @Test
    fun `abandon transitions to INIT`() {
        session.start()
        session.finishExposure()
        session.abandon()
        assertEquals(TeststripState.INIT, session.state)
        assertEquals(-1, session.currentPatchIndex)
        assertFalse(session.isSessionComplete)
    }

    // --- Pause pendant exposition ---

    @Test
    fun `pause during exposure transitions to PAUSED`() {
        session.start()
        session.pause()
        assertEquals(TeststripState.PAUSED, session.state)
        assertTrue(session.isPaused)
    }

    @Test
    fun `resume from PAUSED goes back to EXPOSING`() {
        session.start()
        session.pause()
        session.resume()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertFalse(session.isPaused)
    }

    @Test
    fun `resume preserves remaining time calculation`() {
        session.start()
        fakeNow = 3000L  // 3 seconds elapsed
        session.pause()
        fakeNow = 10000L  // 7 seconds passed during pause (should not count)
        session.resume()
        assertEquals(5000L, session.remainingTimeMs)  // 8000 - 3000 = 5000 immediately after resume
        fakeNow = 13000L  // 3 more seconds
        assertEquals(2000L, session.remainingTimeMs)  // 8000 - 6000 = 2000 after 3 more seconds
    }

    // --- Invalid transitions ---

    @Test(expected = IllegalStateException::class)
    fun `start from EXPOSING throws`() {
        session.start()
        session.start()
    }

    @Test(expected = IllegalStateException::class)
    fun `nextPatch during exposure throws`() {
        session.start()
        session.nextPatch()
    }

    @Test(expected = IllegalStateException::class)
    fun `finishExposure from INIT throws`() {
        session.finishExposure()
    }

    @Test(expected = IllegalStateException::class)
    fun `pause from INIT throws`() {
        session.pause()
    }

    @Test(expected = IllegalStateException::class)
    fun `resume from INIT throws`() {
        session.resume()
    }

    // --- Calcul du temps restant ---

    @Test
    fun `remainingTimeMs decreases during exposure`() {
        session.start()
        fakeNow = 3000L
        assertEquals(5000L, session.remainingTimeMs)
    }

    @Test
    fun `remainingTimeMs frozen during pause`() {
        session.start()
        fakeNow = 3000L
        session.pause()
        fakeNow = 100000L  // lots of time passes
        assertEquals(5000L, session.remainingTimeMs)
    }
}
