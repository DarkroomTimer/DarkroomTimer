package fr.mathgl.darkroomtimer.development

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DevelopmentDao {

    @Query("SELECT * FROM development_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<DevelopmentProfileEntity>>

    @Query("SELECT * FROM development_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): DevelopmentProfileEntity?

    @Query("SELECT * FROM development_profiles WHERE id = :id")
    fun getProfileByIdFlow(id: Long): Flow<DevelopmentProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DevelopmentProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: DevelopmentProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: DevelopmentProfileEntity)

    @Query("DELETE FROM development_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("SELECT COUNT(*) FROM development_profiles")
    fun getProfileCount(): Flow<Int>
}
