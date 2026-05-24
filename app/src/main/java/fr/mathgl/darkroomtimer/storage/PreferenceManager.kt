package fr.mathgl.darkroomtimer.storage

import android.content.Context
import android.content.SharedPreferences
import fr.mathgl.darkroomtimer.math.ContrastGrade

/**
 * Manager for handling basic scalar settings for the application using SharedPreferences.
 */
class PreferenceManager private constructor(context: Context) {
    internal val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // General Settings
    var metronomeEnabled: Boolean
        get() = prefs.getBoolean(KEY_METRONOME_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_METRONOME_ENABLED, value).apply()

    var metronomeCadenceMs: Int
        get() = prefs.getInt(KEY_METRONOME_CADENCE_MS, 1000)
        set(value) = prefs.edit().putInt(KEY_METRONOME_CADENCE_MS, value).apply()

    var buzzerVolume: String
        get() = prefs.getString(KEY_BUZZER_VOLUME, "MEDIUM") ?: "MEDIUM"
        set(value) = prefs.edit().putString(KEY_BUZZER_VOLUME, value).apply()

    var startBeepEnabled: Boolean
        get() = prefs.getBoolean(KEY_START_BEEP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_START_BEEP_ENABLED, value).apply()

    // Exposure Settings
    var defaultExposureMs: Long
        get() = prefs.getLong(KEY_DEFAULT_EXPOSURE_MS, 8000L)
        set(value) = prefs.edit().putLong(KEY_DEFAULT_EXPOSURE_MS, value).apply()

    var defaultContrastGradeIndex: Int
        get() = prefs.getInt(KEY_DEFAULT_CONTRAST_GRADE_INDEX, 5)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_CONTRAST_GRADE_INDEX, value).apply()

    var defaultContrastGrade: ContrastGrade
        get() = ContrastGrade.fromIndex(defaultContrastGradeIndex)
        set(value) { defaultContrastGradeIndex = value.index }

    var defaultStopNumerator: Int
        get() = prefs.getInt(KEY_DEFAULT_STOP_NUMERATOR, 1)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_STOP_NUMERATOR, value).apply()

    var defaultStopDenominator: Int
        get() = prefs.getInt(KEY_DEFAULT_STOP_DENOMINATOR, 3)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_STOP_DENOMINATOR, value).apply()

    var defaultTimerMs: Long
        get() = prefs.getLong(KEY_DEFAULT_TIMER_MS, 120000L)
        set(value) = prefs.edit().putLong(KEY_DEFAULT_TIMER_MS, value).apply()

    // Teststrip Settings
    var teststripMode: String
        get() = prefs.getString(KEY_TESTSTRIP_MODE, "INCREMENTAL") ?: "INCREMENTAL"
        set(value) = prefs.edit().putString(KEY_TESTSTRIP_MODE, value).apply()

    var teststripPatchCount: Int
        get() = prefs.getInt(KEY_TESTSTRIP_PATCH_COUNT, 6)
        set(value) = prefs.edit().putInt(KEY_TESTSTRIP_PATCH_COUNT, value).apply()

    var teststripBaseMs: Long
        get() = prefs.getLong(KEY_TESTSTRIP_BASE_MS, 8000L)
        set(value) = prefs.edit().putLong(KEY_TESTSTRIP_BASE_MS, value).apply()

    var teststripStopNumerator: Int
        get() = prefs.getInt(KEY_TESTSTRIP_STOP_NUMERATOR, 1)
        set(value) = prefs.edit().putInt(KEY_TESTSTRIP_STOP_NUMERATOR, value).apply()

    var teststripStopDenominator: Int
        get() = prefs.getInt(KEY_TESTSTRIP_STOP_DENOMINATOR, 3)
        set(value) = prefs.edit().putInt(KEY_TESTSTRIP_STOP_DENOMINATOR, value).apply()

    // Bluetooth Settings
    var bluetoothEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLUETOOTH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLUETOOTH_ENABLED, value).apply()

    var bluetoothDeviceAddress: String
        get() = prefs.getString(KEY_BLUETOOTH_DEVICE_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_DEVICE_ADDRESS, value).apply()

    var bluetoothDeviceName: String
        get() = prefs.getString(KEY_BLUETOOTH_DEVICE_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_DEVICE_NAME, value).apply()

    // Active Profile
    var enlargerProfileId: Int
        get() = prefs.getInt(KEY_ENLARGER_PROFILE_ID, 0)
        set(value) = prefs.edit().putInt(KEY_ENLARGER_PROFILE_ID, value).apply()

    // Luminosity Settings
    var luminosityMode: String
        get() = prefs.getString(KEY_LUMINOSITY_MODE, "ADAPTIVE") ?: "ADAPTIVE"
        set(value) = prefs.edit().putString(KEY_LUMINOSITY_MODE, value).apply()

    var luminosityMin: Float
        get() = prefs.getFloat(KEY_LUMINOSITY_MIN, 0.01f)
        set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_MIN, value).apply()

    var luminosityMax: Float
        get() = prefs.getFloat(KEY_LUMINOSITY_MAX, 0.10f)
        set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_MAX, value).apply()

    var luminosityFixed: Float
        get() = prefs.getFloat(KEY_LUMINOSITY_FIXED, 0.05f)
        set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_FIXED, value).apply()

    companion object {
        private const val PREFS_NAME = "darkroom_timer_prefs"

        private const val KEY_METRONOME_ENABLED = "pref_metronome_enabled"
        private const val KEY_METRONOME_CADENCE_MS = "pref_metronome_cadence_ms"
        private const val KEY_BUZZER_VOLUME = "pref_buzzer_volume"
        private const val KEY_START_BEEP_ENABLED = "pref_start_beep_enabled"

        private const val KEY_DEFAULT_EXPOSURE_MS = "pref_default_exposure_ms"
        private const val KEY_DEFAULT_CONTRAST_GRADE_INDEX = "pref_default_contrast_grade_index"
        private const val KEY_DEFAULT_STOP_NUMERATOR = "pref_default_stop_numerator"
        private const val KEY_DEFAULT_STOP_DENOMINATOR = "pref_default_stop_denominator"
        private const val KEY_DEFAULT_TIMER_MS = "pref_default_timer_ms"

        private const val KEY_TESTSTRIP_MODE = "pref_teststrip_mode"
        private const val KEY_TESTSTRIP_PATCH_COUNT = "pref_teststrip_patch_count"
        private const val KEY_TESTSTRIP_BASE_MS = "pref_teststrip_base_ms"
        private const val KEY_TESTSTRIP_STOP_NUMERATOR = "pref_teststrip_stop_numerator"
        private const val KEY_TESTSTRIP_STOP_DENOMINATOR = "pref_teststrip_stop_denominator"

        private const val KEY_BLUETOOTH_ENABLED = "pref_bluetooth_enabled"
        private const val KEY_BLUETOOTH_DEVICE_ADDRESS = "pref_bluetooth_device_address"
        private const val KEY_BLUETOOTH_DEVICE_NAME = "pref_bluetooth_device_name"

        private const val KEY_ENLARGER_PROFILE_ID = "pref_enlarger_profile_id"

        private const val KEY_LUMINOSITY_MODE = "pref_luminosity_mode"
        private const val KEY_LUMINOSITY_MIN = "pref_luminosity_min"
        private const val KEY_LUMINOSITY_MAX = "pref_luminosity_max"
        private const val KEY_LUMINOSITY_FIXED = "pref_luminosity_fixed"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        /**
         * Returns the singleton instance of PreferenceManager.
         */
        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
