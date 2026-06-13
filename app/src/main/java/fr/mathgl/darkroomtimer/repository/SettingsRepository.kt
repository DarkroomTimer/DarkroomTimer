package fr.mathgl.darkroomtimer.repository

import android.content.Context
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.storage.PreferenceManager
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat

class SettingsRepository(context: Context) {
    private val prefs = PreferenceManager.getInstance(context)

    var defaultExposureMs: Long
        get() = prefs.defaultExposureMs
        set(value) { prefs.defaultExposureMs = value }

    var defaultContrastGrade: ContrastGrade
        get() = prefs.defaultContrastGrade
        set(value) { prefs.defaultContrastGrade = value }

    var metronomeEnabled: Boolean
        get() = prefs.metronomeEnabled
        set(value) { prefs.metronomeEnabled = value }

    var relaySystemConfig: RelaySystemConfigFlat
        get() = prefs.relaySystemConfig
        set(value) { prefs.relaySystemConfig = value }

    // Teststrip settings
    var teststripBaseMs: Long
        get() = prefs.teststripBaseMs
        set(value) { prefs.teststripBaseMs = value }

    var teststripStopNumerator: Int
        get() = prefs.teststripStopNumerator
        set(value) { prefs.teststripStopNumerator = value }

    var teststripStopDenominator: Int
        get() = prefs.teststripStopDenominator
        set(value) { prefs.teststripStopDenominator = value }

    var teststripPatchCount: Int
        get() = prefs.teststripPatchCount
        set(value) { prefs.teststripPatchCount = value }

    var teststripMode: String
        get() = prefs.teststripMode
        set(value) { prefs.teststripMode = value }

    var teststripIncrementType: String
        get() = prefs.teststripIncrementType
        set(value) { prefs.teststripIncrementType = value }

    var teststripIncrementMs: Long
        get() = prefs.teststripIncrementMs
        set(value) { prefs.teststripIncrementMs = value }
}
