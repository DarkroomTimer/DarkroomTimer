package fr.mathgl.darkroomtimer.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PreferenceManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var manager: PreferenceManager

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).thenAnswer { }

        manager = PreferenceManager(mockContext)
    }

    @Test
    fun `default exposure is 8 seconds`() {
        `when`(mockPrefs.getLong("pref_default_exposure_ms", 8000L)).thenReturn(8000L)
        assertEquals(8000L, manager.defaultExposureMs)
    }

    @Test
    fun `default contrast grade index is 5`() {
        `when`(mockPrefs.getInt("pref_default_contrast_grade_index", 5)).thenReturn(5)
        assertEquals(5, manager.defaultContrastGradeIndex)
        assertEquals(ContrastGrade.fromIndex(5), manager.defaultContrastGrade)
    }

    @Test
    fun `default stop fraction is 1 over 3`() {
        `when`(mockPrefs.getInt("pref_default_stop_numerator", 1)).thenReturn(1)
        `when`(mockPrefs.getInt("pref_default_stop_denominator", 3)).thenReturn(3)
        assertEquals(1, manager.defaultStopNumerator)
        assertEquals(3, manager.defaultStopDenominator)
    }

    @Test
    fun `setting exposure stores value in shared preferences`() {
        manager.defaultExposureMs = 12000L
        verify(mockEditor).putLong("pref_default_exposure_ms", 12000L)
        verify(mockEditor).apply()
    }

    @Test
    fun `setting contrast grade stores its index`() {
        manager.defaultContrastGrade = ContrastGrade.fromIndex(2)
        verify(mockEditor).putInt("pref_default_contrast_grade_index", 2)
    }

    @Test
    fun `relay system config defaults when nothing stored`() {
        `when`(mockPrefs.getString("pref_relay_system_config", null)).thenReturn(null)
        assertEquals(RelaySystemConfigFlat(), manager.relaySystemConfig)
    }

    @Test
    fun `relay system config round-trips through json`() {
        val config = RelaySystemConfigFlat(
            enlargerType = "TASMOTA",
            enlargerHost = "192.168.1.50",
            enlargerChannel = 2,
            safelightEnabled = true
        )
        val json = Gson().toJson(config)

        `when`(mockPrefs.getString("pref_relay_system_config", null)).thenReturn(json)
        assertEquals(config, manager.relaySystemConfig)

        manager.relaySystemConfig = config
        verify(mockEditor).putString("pref_relay_system_config", json)
    }

    @Test
    fun `corrupt relay system config json falls back to defaults`() {
        `when`(mockPrefs.getString("pref_relay_system_config", null)).thenReturn("{not valid json!!")
        assertEquals(RelaySystemConfigFlat(), manager.relaySystemConfig)
    }
}
