package fr.mathgl.darkroomtimer.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AudioSystemTest {

    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var audioSettingsProvider: FakeAudioSettingsProvider
    private lateinit var audioSystem: AudioSystem

    @Before
    fun setup() {
        audioEngine = FakeAudioEngine()
        audioSettingsProvider = FakeAudioSettingsProvider()
        audioSystem = AudioSystem(audioEngine, audioSettingsProvider, AudioVolume.MEDIUM)
    }

    @Test
    fun `initial state should have metronome stopped`() {
        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `startExposure should play start beep at 440Hz when enabled`() {
        audioSettingsProvider.isStartBeepEnabled = true

        audioSystem.startExposure()

        assertEquals(1, audioEngine.tonePlaybackHistory.size)
        val (freq, duration, vol) = audioEngine.tonePlaybackHistory[0]
        assertEquals(440, freq)
        assertTrue(duration > 0)
        assertEquals(0.6f, vol)
    }

    @Test
    fun `startExposure should not play beep when disabled`() {
        audioSettingsProvider.isStartBeepEnabled = false

        audioSystem.startExposure()

        assertTrue(audioEngine.tonePlaybackHistory.isEmpty())
    }

    @Test
    fun `startExposure should start metronome when enabled`() {
        audioSettingsProvider.isMetronomeEnabled = true

        audioSystem.startExposure()

        assertTrue(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `startExposure should not start metronome when disabled`() {
        audioSettingsProvider.isMetronomeEnabled = false

        audioSystem.startExposure()

        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `stopExposure should stop metronome`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = true
        audioSystem.startExposure()
        assertTrue(audioSystem.isMetronomeRunning)

        audioSystem.stopExposure()

        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `stopExposure should play 3 beeps at 880Hz with 100ms gaps`() {
        audioSystem.stopExposure()

        assertEquals(1, audioEngine.beepSequenceHistory.size)
        val (freq, count, _, silence, vol) = audioEngine.beepSequenceHistory[0]
        assertEquals(880, freq)
        assertEquals(3, count)
        assertEquals(100, silence)
        assertEquals(0.6f, vol)
    }

    @Test
    fun `stopTeststripPatch should play 2 beeps at 660Hz with 100ms gaps`() {
        audioSystem.stopTeststripPatch()

        assertEquals(1, audioEngine.beepSequenceHistory.size)
        val (freq, count, _, silence, vol) = audioEngine.beepSequenceHistory[0]
        assertEquals(660, freq)
        assertEquals(2, count)
        assertEquals(100, silence)
        assertEquals(0.6f, vol)
    }

    @Test
    fun `stopTeststripSession should play 4 ascending beeps`() {
        audioSystem.stopTeststripSession()

        assertEquals(4, audioEngine.beepSequenceHistory.size)
        assertEquals(440, audioEngine.beepSequenceHistory[0].freq)
        assertEquals(1, audioEngine.beepSequenceHistory[0].count)
        assertEquals(550, audioEngine.beepSequenceHistory[1].freq)
        assertEquals(1, audioEngine.beepSequenceHistory[1].count)
        assertEquals(660, audioEngine.beepSequenceHistory[2].freq)
        assertEquals(1, audioEngine.beepSequenceHistory[2].count)
        assertEquals(880, audioEngine.beepSequenceHistory[3].freq)
        assertEquals(1, audioEngine.beepSequenceHistory[3].count)
    }

    @Test
    fun `pause should stop metronome`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = true
        audioSystem.startExposure()
        assertTrue(audioSystem.isMetronomeRunning)

        audioSystem.pause()

        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `resume should restart metronome when enabled`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = true
        audioSystem.startExposure()
        audioSystem.pause()

        audioSystem.resume()

        assertTrue(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `resume should not start metronome when disabled`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = false
        audioSystem.startExposure()
        audioSystem.pause()

        audioSystem.resume()

        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `stop should stop metronome`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = true
        audioSystem.startExposure()
        assertTrue(audioSystem.isMetronomeRunning)

        audioSystem.stop()

        assertFalse(audioSystem.isMetronomeRunning)
    }

    @Test
    fun `stop should stop any ongoing tone`() {
        audioSystem.stop()
        assertTrue(audioEngine.stopCalled)
    }

    @Test
    fun `setVolume should update internal volume`() {
        audioSystem.setVolume(AudioVolume.LOUD)

        audioSystem.stopExposure()
        val (_, _, _, _, vol) = audioEngine.beepSequenceHistory[0]
        assertEquals(1.0f, vol)
    }

    @Test
    fun `setMetronomeCadence should update settings provider`() {
        audioSystem.setMetronomeCadence(500)

        assertEquals(500, audioSettingsProvider.metronomeCadenceMs)
    }

    @Test
    fun `release should stop metronome and release engine`() = runTest {
        audioSettingsProvider.isMetronomeEnabled = true
        audioSystem.startExposure()

        audioSystem.release()

        assertFalse(audioSystem.isMetronomeRunning)
        assertTrue(audioEngine.released)
    }
}

/**
 * Fake AudioEngine for testing that records all interactions.
 */
class FakeAudioEngine : AudioEngine {
    data class TonePlayback(val frequencyHz: Int, val durationMs: Int, val volume: Float)
    data class BeepSequence(
        val freq: Int,
        val count: Int,
        val durationMs: Int,
        val silence: Int,
        val volume: Float
    )

    val tonePlaybackHistory = mutableListOf<TonePlayback>()
    val beepSequenceHistory = mutableListOf<BeepSequence>()
    var stopCalled = false
    var released = false

    override fun playTone(frequencyHz: Int, durationMs: Int, volume: Float) {
        tonePlaybackHistory.add(TonePlayback(frequencyHz, durationMs, volume))
    }

    override fun playBeepSequence(
        frequencyHz: Int,
        beepCount: Int,
        beepDurationMs: Int,
        silenceBetweenMs: Int,
        volume: Float
    ) {
        beepSequenceHistory.add(BeepSequence(frequencyHz, beepCount, beepDurationMs, silenceBetweenMs, volume))
    }

    override fun stop() {
        stopCalled = true
    }

    override fun release() {
        released = true
    }
}

/**
 * Fake AudioSettingsProvider for testing that stores values in memory.
 */
class FakeAudioSettingsProvider : AudioSettingsProvider {
    override var isMetronomeEnabled: Boolean = false
    override var metronomeCadenceMs: Int = 1000
    override val metronomeFrequencyHz: Int = 250
    override val metronomeDurationMs: Int = 25
    override var isStartBeepEnabled: Boolean = true
    override var buzzerVolume: AudioVolume = AudioVolume.MEDIUM
}
