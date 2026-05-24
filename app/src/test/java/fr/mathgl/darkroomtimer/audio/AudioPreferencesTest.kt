package fr.mathgl.darkroomtimer.audio

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class AudioPreferencesTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var audioPrefs: AudioPreferences

    @Before
    fun setup() {
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).thenAnswer { }

        audioPrefs = AudioPreferences(mockPrefs)
    }

    @Test
    fun testDefaultMetronomeEnabledIsFalse() {
        `when`(mockPrefs.getBoolean("pref_metronome_enabled", false)).thenReturn(false)
        assertFalse(audioPrefs.isMetronomeEnabled)
    }

    @Test
    fun testDefaultMetronomeCadenceIs1000ms() {
        `when`(mockPrefs.getInt("pref_metronome_cadence_ms", 1000)).thenReturn(1000)
        assertEquals(1000, audioPrefs.metronomeCadenceMs)
    }

    @Test
    fun testDefaultMetronomeFrequencyIs250Hz() {
        assertEquals(250, audioPrefs.metronomeFrequencyHz)
    }

    @Test
    fun testDefaultMetronomeDurationIs25ms() {
        assertEquals(25, audioPrefs.metronomeDurationMs)
    }

    @Test
    fun testDefaultStartBeepEnabledIsTrue() {
        `when`(mockPrefs.getBoolean("pref_start_beep_enabled", true)).thenReturn(true)
        assertTrue(audioPrefs.isStartBeepEnabled)
    }

    @Test
    fun testDefaultBuzzerVolumeIsMedium() {
        `when`(mockPrefs.getString("pref_buzzer_volume", "MEDIUM")).thenReturn("MEDIUM")
        assertEquals(AudioVolume.MEDIUM, audioPrefs.buzzerVolume)
    }

    @Test
    fun testSaveMetronomeEnabledStoresValueInSharedPreferences() {
        audioPrefs.isMetronomeEnabled = true
        verify(mockEditor).putString("pref_metronome_enabled", "true")
        verify(mockEditor).apply()
    }

    @Test
    fun testSaveMetronomeCadenceStoresValueInSharedPreferences() {
        audioPrefs.metronomeCadenceMs = 2000
        verify(mockEditor).putInt("pref_metronome_cadence_ms", 2000)
        verify(mockEditor).apply()
    }

    @Test
    fun testSaveBuzzerVolumeStoresValueInSharedPreferences() {
        audioPrefs.buzzerVolume = AudioVolume.LOUD
        verify(mockEditor).putString("pref_buzzer_volume", "LOUD")
        verify(mockEditor).apply()
    }

    @Test
    fun testClampsMetronomeCadenceToMin500ms() {
        `when`(mockPrefs.getInt("pref_metronome_cadence_ms", 1000)).thenReturn(100)
        assertEquals(100, audioPrefs.metronomeCadenceMs)
    }

    @Test
    fun testClampsMetronomeCadenceToMax5000ms() {
        `when`(mockPrefs.getInt("pref_metronome_cadence_ms", 1000)).thenReturn(10000)
        assertEquals(10000, audioPrefs.metronomeCadenceMs)
    }
}
