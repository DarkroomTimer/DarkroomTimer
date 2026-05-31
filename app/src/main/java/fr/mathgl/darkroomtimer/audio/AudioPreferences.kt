package fr.mathgl.darkroomtimer.audio

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Interface for audio-related settings, enabling testability.
 */
interface AudioSettingsProvider {
    var isMetronomeEnabled: Boolean
    var metronomeCadenceMs: Int
    val metronomeFrequencyHz: Int
    val metronomeDurationMs: Int
    var isStartBeepEnabled: Boolean
    var buzzerVolume: AudioVolume
}

/**
 * Wraps SharedPreferences for audio-related settings.
 */
class AudioPreferences(private val prefs: SharedPreferences) : AudioSettingsProvider {

    override var isMetronomeEnabled: Boolean
        get() = prefs.getBoolean(KEY_METRONOME_ENABLED, false)
        set(value) = prefs.edit {putBoolean(KEY_METRONOME_ENABLED, value)}

    override var metronomeCadenceMs: Int
        get() = prefs.getInt(KEY_METRONOME_CADENCE_MS, DEFAULT_METRONOME_CADENCE_MS)
        set(value) = prefs.edit {putInt(KEY_METRONOME_CADENCE_MS, value)}

    override val metronomeFrequencyHz: Int = DEFAULT_METRONOME_FREQUENCY_HZ

    override val metronomeDurationMs: Int = DEFAULT_METRONOME_DURATION_MS

    override var isStartBeepEnabled: Boolean
        get() = prefs.getBoolean(KEY_START_BEEP_ENABLED, true)
        set(value) = prefs.edit {putBoolean(KEY_START_BEEP_ENABLED, value)}

    override var buzzerVolume: AudioVolume
        get() = AudioVolume.fromString(prefs.getString(KEY_BUZZER_VOLUME, "MEDIUM"))
        set(value) = prefs.edit {putString(KEY_BUZZER_VOLUME, value.name)}

    companion object {
        private const val KEY_METRONOME_ENABLED = "pref_metronome_enabled"
        private const val KEY_METRONOME_CADENCE_MS = "pref_metronome_cadence_ms"
        private const val KEY_START_BEEP_ENABLED = "pref_start_beep_enabled"
        private const val KEY_BUZZER_VOLUME = "pref_buzzer_volume"

        private const val DEFAULT_METRONOME_CADENCE_MS = 1000
        private const val DEFAULT_METRONOME_FREQUENCY_HZ = 250
        private const val DEFAULT_METRONOME_DURATION_MS = 25
    }
}
