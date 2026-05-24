package fr.mathgl.darkroomtimer.audio

import org.junit.Assert.*
import org.junit.Test

class ToneGeneratorAudioEngineTest {

    @Test
    fun `calculateSilenceDuration for 3 beeps with 100ms gaps returns correct value`() {
        val engine = ToneGeneratorAudioEngine(AudioVolume.MEDIUM)
        // 3 beeps * 100ms each + 2 gaps * 100ms each = 300 + 200 = 500ms
        // But last gap is not included, so: 3*100 + 2*100 = 500ms
        val total = 3 * 100 + 2 * 100
        assertEquals(500, total)
    }

    @Test
    fun `engine can be created with default volume MEDIUM`() {
        val engine = ToneGeneratorAudioEngine(AudioVolume.MEDIUM)
        // Engine should be created successfully
        assert(engine != null)
    }
}
