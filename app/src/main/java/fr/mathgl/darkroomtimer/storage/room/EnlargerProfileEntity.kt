package fr.mathgl.darkroomtimer.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "enlarger_profiles")
data class EnlargerProfileEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val turnOnDelayMs: Int,
    val riseTimeMs: Int,
    val riseTimeEquivMs: Int,
    val turnOffDelayMs: Int,
    val fallTimeMs: Int,
    val fallTimeEquivMs: Int
)
