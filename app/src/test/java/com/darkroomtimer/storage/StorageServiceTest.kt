package com.darkroomtimer.storage

import com.darkroomtimer.storage.room.EnlargerProfileDao
import com.darkroomtimer.storage.room.EnlargerProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.*

class StorageServiceTest {
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var profileDao: EnlargerProfileDao
    private lateinit var storageService: StorageService

    @Before
    fun setup() {
        preferenceManager = mock()
        profileDao = mock()
        storageService = StorageService(preferenceManager, profileDao)
    }

    @Test
    fun `exportBackup should produce correct JSON structure`() = runBlocking {
        // Arrange
        whenever(preferenceManager.defaultExposureMs).thenReturn(10000L)
        whenever(preferenceManager.defaultContrastGradeIndex).thenReturn(3)
        whenever(preferenceManager.defaultStopNumerator).thenReturn(1)
        whenever(preferenceManager.defaultStopDenominator).thenReturn(2)
        whenever(preferenceManager.metronomeEnabled).thenReturn(true)
        whenever(preferenceManager.metronomeCadenceMs).thenReturn(500)
        whenever(preferenceManager.buzzerVolume).thenReturn("HIGH")
        whenever(preferenceManager.teststripMode).thenReturn("FIXED")
        whenever(preferenceManager.teststripPatchCount).thenReturn(4)

        val profiles = listOf(
            EnlargerProfileEntity(0, "Test Profile", 100, 200, 150, 100, 200, 150)
        )
        whenever(profileDao.getAll()).thenReturn(profiles)

        // Act
        val json = storageService.exportBackup()

        // Assert
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"default_exposure_ms\": 10000"))
        assertTrue(json.contains("\"default_contrast_grade_index\": 3"))
        assertTrue(json.contains("\"metronome_enabled\": true"))
        assertTrue(json.contains("\"name\": \"Test Profile\""))
        assertTrue(json.contains("\"riseTimeMs\": 200"))
        assertTrue(json.contains("\"riseTimeEquivMs\": 150"))
    }

    @Test
    fun `importBackup should succeed with valid JSON`() = runBlocking {
        // Arrange
        val validJson = """
            {
                "version": 1,
                "exported_at": "2023-10-27T10:00:00Z",
                "settings": {
                    "default_exposure_ms": 12000,
                    "default_contrast_grade_index": 2,
                    "default_stop_numerator": 1,
                    "default_stop_denominator": 4,
                    "metronome_enabled": false,
                    "metronome_cadence_ms": 800,
                    "buzzer_volume": "LOW",
                    "teststrip_mode": "INCREMENTAL",
                    "teststrip_patch_count": 5
                },
                "enlarger_profiles": [
                    {
                        "id": 0,
                        "name": "Valid Profile",
                        "turnOnDelayMs": 100,
                        "riseTimeMs": 500,
                        "riseTimeEquivMs": 400,
                        "turnOffDelayMs": 100,
                        "fallTimeMs": 500,
                        "fallTimeEquivMs": 400
                    }
                ]
            }
        """.trimIndent()

        // Act
        storageService.importBackup(validJson)

        // Assert
        verify(preferenceManager).defaultExposureMs = 12000L
        verify(preferenceManager).defaultContrastGradeIndex = 2
        verify(preferenceManager).metronomeEnabled = false
        verify(profileDao).insert(argThat { this.name == "Valid Profile" })
    }

    @Test
    fun `importBackup should reject invalid version`() = runBlocking {
        val invalidVersionJson = """
            {
                "version": 2,
                "exported_at": "2023-10-27T10:00:00Z",
                "settings": {},
                "enlarger_profiles": []
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup(invalidVersionJson) }
        }
        assertTrue(exception.message!!.contains("Unsupported backup version"))
    }

    @Test
    fun `importBackup should reject profile with negative delays`() = runBlocking {
        val negativeDelayJson = """
            {
                "version": 1,
                "exported_at": "2023-10-27T10:00:00Z",
                "settings": {},
                "enlarger_profiles": [
                    {
                        "id": 0,
                        "name": "Bad Profile",
                        "turnOnDelayMs": -100,
                        "riseTimeMs": 500,
                        "riseTimeEquivMs": 400,
                        "turnOffDelayMs": 100,
                        "fallTimeMs": 500,
                        "fallTimeEquivMs": 400
                    }
                ]
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup(negativeDelayJson) }
        }
        assertTrue(exception.message!!.contains("contains negative delays"))
    }

    @Test
    fun `importBackup should reject profile with riseTimeEquivMs greater than or equal to riseTimeMs`() = runBlocking {
        val invalidRiseTimeJson = """
            {
                "version": 1,
                "exported_at": "2023-10-27T10:00:00Z",
                "settings": {},
                "enlarger_profiles": [
                    {
                        "id": 0,
                        "name": "Bad Profile",
                        "turnOnDelayMs": 100,
                        "riseTimeMs": 400,
                        "riseTimeEquivMs": 500,
                        "turnOffDelayMs": 100,
                        "fallTimeMs": 500,
                        "fallTimeEquivMs": 400
                    }
                ]
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup(invalidRiseTimeJson) }
        }
        assertTrue(exception.message!!.contains("riseTimeEquivMs must be less than riseTimeMs"))
    }

    @Test
    fun `importBackup should reject profile with empty name`() = runBlocking {
        val emptyNameJson = """
            {
                "version": 1,
                "exported_at": "2023-10-27T10:00:00Z",
                "settings": {},
                "enlarger_profiles": [
                    {
                        "id": 0,
                        "name": " ",
                        "turnOnDelayMs": 100,
                        "riseTimeMs": 500,
                        "riseTimeEquivMs": 400,
                        "turnOffDelayMs": 100,
                        "fallTimeMs": 500,
                        "fallTimeEquivMs": 400
                    }
                ]
            }
        """.trimIndent()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup(emptyNameJson) }
        }
        assertTrue(exception.message!!.contains("name cannot be empty"))
    }

    @Test
    fun `importBackup should reject syntactically invalid JSON`() = runBlocking {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup("{") }
        }
        assertTrue(exception.message!!.contains("Malformed JSON format"))
    }

    @Test
    fun `importBackup should reject empty or non-JSON string`() = runBlocking {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { storageService.importBackup("") }
        }
        assertTrue(exception.message!!.contains("Malformed JSON format"))
    }
}
