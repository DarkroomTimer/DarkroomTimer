package fr.mathgl.darkroomtimer.storage.room

import androidx.room.*

@Dao
interface EnlargerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: EnlargerProfileEntity)

    @Update
    suspend fun update(profile: EnlargerProfileEntity)

    @Delete
    suspend fun delete(profile: EnlargerProfileEntity)

    @Query("SELECT * FROM enlarger_profiles WHERE id = :id")
    suspend fun getById(id: Int): EnlargerProfileEntity?

    @Query("SELECT * FROM enlarger_profiles")
    suspend fun getAll(): List<EnlargerProfileEntity>
}
