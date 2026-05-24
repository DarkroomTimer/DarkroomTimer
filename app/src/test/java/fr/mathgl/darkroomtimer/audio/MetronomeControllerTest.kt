package fr.mathgl.darkroomtimer.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeControllerTest {

    @Test
    fun `start starts the metronome and plays clicks`() = runTest {
        var clickCount = 0
        val audioEngine = object : AudioEngine {
            override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) {
                clickCount++
            }
            override fun playBeepSequence(
                frequencyHz: Int,
                beepCount: Int,
                beepDurationMs: Int,
                silenceBetweenMs: Int,
                volume: Float
            ) {}
            override fun stop() {}
            override fun release() {}
        }

        val controller = MetronomeController(audioEngine, cadenceMs = 500)
        controller.start()

        delay(1200) // Attendre ~2 clicks
        assertTrue(clickCount >= 1)
        controller.stop()
    }

    @Test
    fun `stop stops the metronome`() = runTest {
        var clickCount = 0
        val audioEngine = object : AudioEngine {
            override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) {
                clickCount++
            }
            override fun playBeepSequence(
                frequencyHz: Int,
                beepCount: Int,
                beepDurationMs: Int,
                silenceBetweenMs: Int,
                volume: Float
            ) {}
            override fun stop() {}
            override fun release() {}
        }

        val controller = MetronomeController(audioEngine, cadenceMs = 300)
        controller.start()
        delay(800)
        val clicksBeforeStop = clickCount
        controller.stop()
        delay(400)
        assertEquals(clicksBeforeStop, clickCount) // Pas de nouveaux clicks après stop
    }

    @Test
    fun `isRunning returns correct state`() = runTest {
        val audioEngine = object : AudioEngine {
            override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) {}
            override fun playBeepSequence(
                frequencyHz: Int,
                beepCount: Int,
                beepDurationMs: Int,
                silenceBetweenMs: Int,
                volume: Float
            ) {}
            override fun stop() {}
            override fun release() {}
        }

        val controller = MetronomeController(audioEngine, cadenceMs = 1000)
        assertFalse(controller.isRunning)
        controller.start()
        assertTrue(controller.isRunning)
        controller.stop()
        assertFalse(controller.isRunning)
    }
}
