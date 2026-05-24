package fr.mathgl.darkroomtimer.audio

import org.junit.Assert.*
import org.junit.Test

class AudioVolumeTest {

    @Test
    fun testMuteMapsToVolumeZero() {
        assertEquals(0f, AudioVolume.MUTE.toFloat())
    }

    @Test
    fun testQuietMapsToVolume025() {
        assertEquals(0.25f, AudioVolume.QUIET.toFloat())
    }

    @Test
    fun testMediumMapsToVolume06() {
        assertEquals(0.6f, AudioVolume.MEDIUM.toFloat())
    }

    @Test
    fun testLoudMapsToVolumeOne() {
        assertEquals(1.0f, AudioVolume.LOUD.toFloat())
    }

    @Test
    fun testFromStringParsesValidVolumeNamesCorrectly() {
        assertEquals(AudioVolume.MUTE, AudioVolume.fromString("MUTE"))
        assertEquals(AudioVolume.QUIET, AudioVolume.fromString("QUIET"))
        assertEquals(AudioVolume.MEDIUM, AudioVolume.fromString("MEDIUM"))
        assertEquals(AudioVolume.LOUD, AudioVolume.fromString("LOUD"))
    }

    @Test
    fun testFromStringReturnsMediumForInvalidVolumeName() {
        assertEquals(AudioVolume.MEDIUM, AudioVolume.fromString("INVALID"))
        assertEquals(AudioVolume.MEDIUM, AudioVolume.fromString(""))
        assertEquals(AudioVolume.MEDIUM, AudioVolume.fromString(null))
    }

    @Test
    fun testOrdinalValuesAreSequentialFromMute() {
        assertEquals(0, AudioVolume.MUTE.ordinal)
        assertEquals(1, AudioVolume.QUIET.ordinal)
        assertEquals(2, AudioVolume.MEDIUM.ordinal)
        assertEquals(3, AudioVolume.LOUD.ordinal)
    }
}
