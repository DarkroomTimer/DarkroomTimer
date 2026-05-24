package fr.mathgl.darkroomtimer.development

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DevelopmentSessionTest {

    private lateinit var session: DevelopmentSession
    private val fakeClock = TestClock()

    @Before
    fun setup() {
        val profile = DevelopmentProfile(
            name = "Test Profile",
            navigationMode = DevelopmentNavigationMode.MANUAL,
            steps = listOf(
                DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10, preEndAlertSeconds = 2),
                DevelopmentStep.PauseStep(name = "Pause 1", durationSeconds = 5),
                DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
            )
        )
        session = DevelopmentSession(profile, fakeClock::elapsed)
    }

    // --- État initial ---

    @Test
    fun `initial state is CONFIGURED`() {
        assertEquals(DevelopmentSessionState.CONFIGURED, session.state)
        assertEquals(-1, session.currentStepIndex)
    }

    @Test
    fun `initially isRunning is false`() {
        assertFalse(session.isRunning)
        assertFalse(session.isPaused)
        assertFalse(session.isCompleted)
    }

    // --- Démarrer la session ---

    @Test
    fun `start sets state to ACTIVE and begins first step`() {
        session.start()
        assertEquals(DevelopmentSessionState.ACTIVE, session.state)
        assertEquals(0, session.currentStepIndex)
        assertTrue(session.isRunning)
    }

    @Test
    fun `start sets correct step elapsed time`() {
        session.start()
        assertEquals(0, session.currentStepElapsedSeconds)
    }

    // --- Tick pendant l'exécution ---

    @Test
    fun `tick increments elapsed time for current step`() {
        session.start()
        fakeClock.advanceSeconds(1)
        session.tick()
        assertEquals(1, session.currentStepElapsedSeconds)
    }

    @Test
    fun `tick with multiple calls advances time correctly`() {
        session.start()
        repeat(5) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        assertEquals(5, session.currentStepElapsedSeconds)
    }

    // --- Alerte de pré-fin ---

    @Test
    fun `tick triggers preEndAlert when threshold reached`() {
        session.start()
        repeat(8) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Bain 1: 10s, preEndAlert à 8s (10 - 2)
        assertTrue(session.isPreEndAlertTriggered)
    }

    @Test
    fun `tick clears preEndAlert after end of step`() {
        session.start()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // À la fin du bain, l'alerte pré-fin ne devrait plus être déclenchée
        assertFalse(session.isPreEndAlertTriggered)
    }

    // --- Transition vers l'étape suivante ---

    @Test
    fun `nextStep moves to next step when in MANUAL mode`() {
        session = DevelopmentSession(
            DevelopmentProfile(
                name = "Test",
                navigationMode = DevelopmentNavigationMode.MANUAL,
                steps = listOf(
                    DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10),
                    DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
                )
            ),
            fakeClock::elapsed
        )
        session.start()
        fakeClock.advanceSeconds(10)
        session.tick()
        session.nextStep()
        assertEquals(1, session.currentStepIndex)
        assertEquals(0, session.currentStepElapsedSeconds)
    }

    @Test
    fun `tick auto-advances to next step in AUTOMATIC mode`() {
        session = DevelopmentSession(
            DevelopmentProfile(
                name = "Test",
                navigationMode = DevelopmentNavigationMode.AUTOMATIC,
                steps = listOf(
                    DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10),
                    DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
                )
            ),
            fakeClock::elapsed
        )
        session.start()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Après 10s, devrait passer automatiquement à l'étape 2
        assertEquals(1, session.currentStepIndex)
    }

    @Test
    fun `tick marks session as COMPLETED after last step finishes`() {
        session.start()
        // Bain 1: 10s
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Pause 1: 5s (MANUAL mode, besoin de nextStep)
        session.nextStep()
        repeat(5) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Bain 2: 10s
        session.nextStep()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        assertTrue(session.isCompleted)
        assertEquals(DevelopmentSessionState.COMPLETED, session.state)
    }

    // --- Pause et reprise ---

    @Test
    fun `pause sets state to PAUSED and preserves elapsed time`() {
        session.start()
        repeat(5) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        session.pause()
        assertEquals(DevelopmentSessionState.PAUSED, session.state)
        assertEquals(5, session.currentStepElapsedSeconds)
    }

    @Test
    fun `resume returns to ACTIVE state`() {
        session.start()
        session.pause()
        session.resume()
        assertEquals(DevelopmentSessionState.ACTIVE, session.state)
        assertTrue(session.isRunning)
    }

    @Test
    fun `tick does not advance time when paused`() {
        session.start()
        repeat(3) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        session.pause()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // elapsed should stay at 3
        assertEquals(3, session.currentStepElapsedSeconds)
    }

    // --- Annuler la session ---

    @Test
    fun `cancel resets session to CONFIGURED state`() {
        session.start()
        session.cancel()
        assertEquals(DevelopmentSessionState.CONFIGURED, session.state)
        assertEquals(-1, session.currentStepIndex)
        assertFalse(session.isRunning)
    }

    // --- Accesseurs ---

    @Test
    fun `totalSteps returns correct count`() {
        assertEquals(3, session.totalSteps)
    }

    @Test
    fun `remainingSteps returns steps left including current`() {
        session.start()
        assertEquals(3, session.remainingSteps)
        // After nextStep, it should be 2
        fakeClock.advanceSeconds(10)
        session.tick()
        session.nextStep()
        assertEquals(2, session.remainingSteps)
    }

    @Test
    fun `progress returns percentage 0-100`() {
        session.start()
        assertEquals(0, session.progress)
        // Avancer à l'étape 1 sur 3 (33%)
        fakeClock.advanceSeconds(10)
        session.tick()
        session.nextStep()
        assertTrue(session.progress in 30..36)
    }
}

/** Fake clock pour tests déterministes */
class TestClock {
    private var seconds: Long = 0

    fun advanceSeconds(s: Long) {
        seconds += s
    }

    fun elapsed(): Long = seconds
}
