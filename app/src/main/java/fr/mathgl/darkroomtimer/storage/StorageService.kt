package fr.mathgl.darkroomtimer.storage

import android.content.Context
import fr.mathgl.darkroomtimer.storage.room.EnlargerProfileDao
import fr.mathgl.darkroomtimer.storage.room.EnlargerProfileEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * StorageService acts as a facade coordinating between PreferenceManager and EnlargerProfileDao.
 * It also provides functionality for exporting and importing application data via JSON.
 */
class StorageService(
    private val preferenceManager: PreferenceManager,
    private val profileDao: EnlargerProfileDao
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val currentVersion = 1

    /**
     * Data transfer object for backup/restore.
     */
    data class BackupData(
        val version: Int,
        val exported_at: String,
        val settings: Map<String, Any>,
        val enlarger_profiles: List<EnlargerProfileEntity>
    )

    /**
     * Exports current settings and profiles to a JSON string.
     */
    suspend fun exportBackup(): String {
        val settings = mapOf(
            "default_exposure_ms" to preferenceManager.defaultExposureMs,
            "default_contrast_grade_index" to preferenceManager.defaultContrastGradeIndex,
            "default_stop_numerator" to preferenceManager.defaultStopNumerator,
            "default_stop_denominator" to preferenceManager.defaultStopDenominator,
            "metronome_enabled" to preferenceManager.metronomeEnabled,
            "metronome_cadence_ms" to preferenceManager.metronomeCadenceMs,
            "buzzer_volume" to preferenceManager.buzzerVolume,
            "teststrip_mode" to preferenceManager.teststripMode,
            "teststrip_patch_count" to preferenceManager.teststripPatchCount
        )

        val profiles = profileDao.getAll()
        val timestamp = DateTimeFormatter.ISO_INSTANT
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

        val backupData = BackupData(
            version = currentVersion,
            exported_at = timestamp,
            settings = settings,
            enlarger_profiles = profiles
        )

        return gson.toJson(backupData)
    }

    /**
     * Imports settings and profiles from a JSON string.
     * Throws [IllegalArgumentException] if validation fails.
     */
    suspend fun importBackup(json: String) {
        val backupData = try {
            gson.fromJson(json, BackupData::class.java)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Malformed JSON format", e)
        } ?: throw IllegalArgumentException("Malformed JSON format")

        validateBackupData(backupData)

        // Apply settings
        val s = backupData.settings
        preferenceManager.defaultExposureMs = (s["default_exposure_ms"] as? Number)?.toLong() ?: preferenceManager.defaultExposureMs
        preferenceManager.defaultContrastGradeIndex = (s["default_contrast_grade_index"] as? Number)?.toInt() ?: preferenceManager.defaultContrastGradeIndex
        preferenceManager.defaultStopNumerator = (s["default_stop_numerator"] as? Number)?.toInt() ?: preferenceManager.defaultStopNumerator
        preferenceManager.defaultStopDenominator = (s["default_stop_denominator"] as? Number)?.toInt() ?: preferenceManager.defaultStopDenominator
        preferenceManager.metronomeEnabled = s["metronome_enabled"] as? Boolean ?: preferenceManager.metronomeEnabled
        preferenceManager.metronomeCadenceMs = (s["metronome_cadence_ms"] as? Number)?.toInt() ?: preferenceManager.metronomeCadenceMs
        preferenceManager.buzzerVolume = s["buzzer_volume"] as? String ?: preferenceManager.buzzerVolume
        preferenceManager.teststripMode = s["teststrip_mode"] as? String ?: preferenceManager.teststripMode
        preferenceManager.teststripPatchCount = (s["teststrip_patch_count"] as? Number)?.toInt() ?: preferenceManager.teststripPatchCount

        // Apply profiles
        backupData.enlarger_profiles.forEach { profile ->
            profileDao.insert(profile)
        }
    }

    private fun validateBackupData(data: BackupData) {
        if (data.version > currentVersion) {
            throw IllegalArgumentException("Unsupported backup version: ${data.version}. Max supported is $currentVersion")
        }

        data.enlarger_profiles.forEach { profile ->
            if (profile.id !in 0..15) {
                throw IllegalArgumentException("Profile ID ${profile.id} out of range (0-15)")
            }
            if (profile.name.isBlank()) {
                throw IllegalArgumentException("Profile name cannot be empty")
            }
            if (profile.turnOnDelayMs < 0 || profile.riseTimeMs < 0 || profile.riseTimeEquivMs < 0 ||
                profile.turnOffDelayMs < 0 || profile.fallTimeMs < 0 || profile.fallTimeEquivMs < 0) {
                throw IllegalArgumentException("Profile ${profile.name} contains negative delays")
            }
            if (profile.riseTimeMs > 0 && profile.riseTimeEquivMs >= profile.riseTimeMs) {
                throw IllegalArgumentException("Profile ${profile.name}: riseTimeEquivMs must be less than riseTimeMs")
            }
            if (profile.fallTimeMs > 0 && profile.fallTimeEquivMs >= profile.fallTimeMs) {
                throw IllegalArgumentException("Profile ${profile.name}: fallTimeEquivMs must be less than fallTimeMs")
            }
        }
    }
}
