package fr.mathgl.darkroomtimer.development

import org.junit.Assert.*
import org.junit.Test

class DevelopmentStepTest {

    @Test
    fun `BathStep has correct properties`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Revélateur",
            durationSeconds = 60,
            preEndAlertSeconds = 5
        )
        assertEquals(DevelopmentStepType.BATH, step.type)
        assertEquals("Revélateur", step.name)
        assertEquals(60, step.durationSeconds)
        assertEquals(5, step.preEndAlertSeconds)
    }

    @Test
    fun `PauseStep has correct properties`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Transfert",
            durationSeconds = 10
        )
        assertEquals(DevelopmentStepType.PAUSE, step.type)
        assertEquals("Transfert", step.name)
        assertEquals(10, step.durationSeconds)
    }

    @Test
    fun `BathStep preEndAlertSeconds defaults to 0`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Fixateur",
            durationSeconds = 120
        )
        assertEquals(0, step.preEndAlertSeconds)
    }

    @Test
    fun `BathStep remainingSeconds calculates correctly`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60,
            preEndAlertSeconds = 10
        )
        assertEquals(60, step.remainingSeconds(0))
        assertEquals(30, step.remainingSeconds(30))
        assertEquals(0, step.remainingSeconds(60))
        assertEquals(0, step.remainingSeconds(90))
    }

    @Test
    fun `PauseStep remainingSeconds calculates correctly`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Test",
            durationSeconds = 30
        )
        assertEquals(30, step.remainingSeconds(0))
        assertEquals(15, step.remainingSeconds(15))
        assertEquals(0, step.remainingSeconds(30))
    }

    @Test
    fun `isPreEndAlertTriggered returns true when threshold reached`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60,
            preEndAlertSeconds = 10
        )
        assertFalse(step.isPreEndAlertTriggered(0))
        assertFalse(step.isPreEndAlertTriggered(40))
        assertTrue(step.isPreEndAlertTriggered(50))
        assertTrue(step.isPreEndAlertTriggered(60))
    }

    @Test
    fun `isPreEndAlertTriggered always false for PauseStep`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Test",
            durationSeconds = 30
        )
        assertFalse((step as DevelopmentStep.PauseStep).isPreEndAlertTriggered(0))
        assertFalse((step as DevelopmentStep.PauseStep).isPreEndAlertTriggered(15))
    }

    @Test
    fun `hasEnded returns true when elapsed greater than or equal to duration`() {
        val bathStep = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60
        )
        assertFalse(bathStep.hasEnded(0))
        assertFalse(bathStep.hasEnded(59))
        assertTrue(bathStep.hasEnded(60))
        assertTrue(bathStep.hasEnded(120))
    }

    @Test
    fun `copy creates modified step with same id`() {
        val original = DevelopmentStep.BathStep(
            id = 1,
            name = "Original",
            durationSeconds = 60,
            preEndAlertSeconds = 5
        )
        val modified = original.copy(name = "Modified", durationSeconds = 90)
        assertEquals(1, modified.id)
        assertEquals("Modified", modified.name)
        assertEquals(90, modified.durationSeconds)
        assertEquals(5, modified.preEndAlertSeconds)
    }
}
