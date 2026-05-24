package fr.mathgl.darkroomtimer.audio

import android.content.SharedPreferences

/**
 * Wraps SharedPreferences for audio-related settings.
 */
class AudioPreferences(private val prefs: SharedPreferences) {

    /**
     * Whether the metronome is enabled during exposures.
     */
    var isMetronomeEnabled: Boolean
        get() = prefs.getBoolean(KEY_METRONOME_ENABLED, false)
        set(value) = prefs.edit().putString(KEY_METRONOME_ENABLED, value.toString()).apply()

    /**
     * Metronome cadence in milliseconds (time between clicks).
     */
    var metronomeCadenceMs: Int
        get() = prefs.getInt(KEY_METRONOME_CADENCE_MS, DEFAULT_METRONOME_CADENCE_MS)
        set(value) = prefs.edit().putInt(KEY_METRONOME_CADENCE_MS, value).apply()

    /**
     * Metronome frequency in Hz (read-only, fixed at 250Hz).
     */
    val metronomeFrequencyHz: Int = DEFAULT_METRONOME_FREQUENCY_HZ

    /**
     * Metronome click duration in milliseconds (read-only, fixed at 25ms).
     */
    val metronomeDurationMs: Int = DEFAULT_METRONOME_DURATION_MS

    /**
     * Whether the start beep is enabled at the beginning of exposures.
     */
    var isStartBeepEnabled: Boolean
        get() = prefs.getBoolean(KEY_START_BEEP_ENABLED, true)
        set(value) = prefs.edit().putString(KEY_START_BEEP_ENABLED, value.toString()).apply()

    /**
     * Volume level for the buzzer/beeps.
     */
    var buzzerVolume: AudioVolume
        get() = AudioVolume.fromString(prefs.getString(KEY_BUZZER_VOLUME, "MEDIUM"))
        set(value) = prefs.edit().putString(KEY_BUZZER_VOLUME, value.name).apply()

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
