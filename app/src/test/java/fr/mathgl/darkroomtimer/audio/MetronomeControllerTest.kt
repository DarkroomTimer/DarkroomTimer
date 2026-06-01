package fr.mathgl.darkroomtimer.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MetronomeControllerTest {

    private fun fakeEngine(onPlay: () -> Unit) = object : AudioEngine {
        override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) { onPlay() }
        override fun playBeepSequence(frequencyHz: Int, beepCount: Int, beepDurationMs: Int, silenceBetweenMs: Int, volume: Float) {}
        override fun stop() {}
        override fun release() {}
    }

    @Test
    fun `start starts the metronome and plays clicks`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)

        var clickCount = 0
        val controller = MetronomeController(fakeEngine { clickCount++ }, cadenceMs = 500, externalScope = scope)

        scope.runTest {
            controller.start()
            testScheduler.advanceTimeBy(1200) // 2 clicks at t=0ms and t=500ms
            testScheduler.runCurrent()
            assertTrue("Expected at least 1 click, got $clickCount", clickCount >= 1)
            controller.stop()
        }
    }

    @Test
    fun `stop stops the metronome`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)

        var clickCount = 0
        val controller = MetronomeController(fakeEngine { clickCount++ }, cadenceMs = 300, externalScope = scope)

        scope.runTest {
            controller.start()
            testScheduler.advanceTimeBy(800) // 2-3 clicks
            testScheduler.runCurrent()
            val clicksBeforeStop = clickCount
            controller.stop()
            testScheduler.advanceTimeBy(400)
            testScheduler.runCurrent()
            assertEquals(clicksBeforeStop, clickCount)
        }
    }

    @Test
    fun `isRunning returns correct state`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val controller = MetronomeController(fakeEngine {}, cadenceMs = 1000, externalScope = scope)

        scope.runTest {
            assertFalse(controller.isRunning)
            controller.start()
            assertTrue(controller.isRunning)
            controller.stop()
            assertFalse(controller.isRunning)
        }
    }
}
