package fr.mathgl.darkroomtimer.development

import org.junit.Assert.*
import org.junit.Test

class DevelopmentStepSerializerTest {

    @Test
    fun `BathStep round-trip preserves all fields`() {
        val step = DevelopmentStep.BathStep(
            id = 3,
            name = "Révélateur",
            durationSeconds = 90,
            preEndAlertSeconds = 10,
            elapsedSeconds = 42L
        )

        val json = DevelopmentStepSerializer.serializeSteps(listOf(step))
        val result = DevelopmentStepSerializer.deserializeSteps(json)

        assertEquals(1, result.size)
        val deserialized = result[0] as DevelopmentStep.BathStep
        assertEquals(step.id, deserialized.id)
        assertEquals(step.name, deserialized.name)
        assertEquals(step.durationSeconds, deserialized.durationSeconds)
        assertEquals(step.preEndAlertSeconds, deserialized.preEndAlertSeconds)
        assertEquals(step.elapsedSeconds, deserialized.elapsedSeconds)
    }

    @Test
    fun `PauseStep round-trip preserves all fields`() {
        val step = DevelopmentStep.PauseStep(
            id = 7,
            name = "Pause agitation",
            durationSeconds = 30,
            elapsedSeconds = 5L
        )

        val json = DevelopmentStepSerializer.serializeSteps(listOf(step))
        val result = DevelopmentStepSerializer.deserializeSteps(json)

        assertEquals(1, result.size)
        val deserialized = result[0] as DevelopmentStep.PauseStep
        assertEquals(step.id, deserialized.id)
        assertEquals(step.name, deserialized.name)
        assertEquals(step.durationSeconds, deserialized.durationSeconds)
        assertEquals(step.elapsedSeconds, deserialized.elapsedSeconds)
    }

    @Test
    fun `mixed list round-trip preserves order and types`() {
        val steps = listOf(
            DevelopmentStep.BathStep(id = 0, name = "Révélateur", durationSeconds = 60),
            DevelopmentStep.PauseStep(id = 1, name = "Pause", durationSeconds = 10),
            DevelopmentStep.BathStep(id = 2, name = "Fixateur", durationSeconds = 120, preEndAlertSeconds = 5)
        )

        val json = DevelopmentStepSerializer.serializeSteps(steps)
        val result = DevelopmentStepSerializer.deserializeSteps(json)

        assertEquals(3, result.size)
        assertTrue(result[0] is DevelopmentStep.BathStep)
        assertTrue(result[1] is DevelopmentStep.PauseStep)
        assertTrue(result[2] is DevelopmentStep.BathStep)
        assertEquals("Fixateur", (result[2] as DevelopmentStep.BathStep).name)
    }

    @Test
    fun `corrupted JSON returns empty list`() {
        val result = DevelopmentStepSerializer.deserializeSteps("not valid json {{{{")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty JSON array returns empty list`() {
        val result = DevelopmentStepSerializer.deserializeSteps("[]")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `unknown step type falls back to BathStep`() {
        val json = """[{"type":"UNKNOWN","id":1,"name":"Mystère","durationSeconds":60,"elapsedSeconds":0}]"""

        val result = DevelopmentStepSerializer.deserializeSteps(json)

        assertEquals(1, result.size)
        assertTrue(result[0] is DevelopmentStep.BathStep)
        assertEquals("Mystère", result[0].name)
    }

    @Test
    fun `missing elapsedSeconds field defaults to zero`() {
        val json = """[{"type":"BATH","id":0,"name":"Révélateur","durationSeconds":60,"preEndAlertSeconds":5}]"""

        val result = DevelopmentStepSerializer.deserializeSteps(json)

        assertEquals(1, result.size)
        assertEquals(0L, result[0].elapsedSeconds)
    }

    @Test
    fun `missing preEndAlertSeconds defaults to zero`() {
        val json = """[{"type":"BATH","id":0,"name":"Révélateur","durationSeconds":60,"elapsedSeconds":0}]"""

        val result = DevelopmentStepSerializer.deserializeSteps(json)

        val bath = result[0] as DevelopmentStep.BathStep
        assertEquals(0, bath.preEndAlertSeconds)
    }
}
