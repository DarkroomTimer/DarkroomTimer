package fr.mathgl.darkroomtimer.development

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DevelopmentNavigationMode {
    MANUAL, AUTOMATIC
}

/**
 * Represente un profil de developpement complet.
 * Utilise pour la visualisation et la sélection.
 */
data class DevelopmentProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val navigationMode: DevelopmentNavigationMode = DevelopmentNavigationMode.MANUAL,
    val steps: List<DevelopmentStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun stepCount(): Int = steps.size
    fun isNotEmpty(): Boolean = steps.isNotEmpty()
    fun isEmpty(): Boolean = steps.isEmpty()

    /** Retourne une previsualisation du profil (nom des etapes + durees) */
    fun preview(): String {
        return steps.joinToString(" -> ") { step ->
            "${step.name} (${step.durationSeconds}s)"
        }
    }

    fun copyWithSteps(newSteps: List<DevelopmentStep>): DevelopmentProfile = this.copy(
        steps = newSteps,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Entite Room pour stockage persistant.
 * Les etapes sont stockees comme JSON dans une colonne.
 */
@Entity(tableName = "development_profiles")
data class DevelopmentProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val navigationModeIndex: Int = 0, // 0 = MANUAL, 1 = AUTOMATIC
    val stepsJson: String, // JSON serialise des etapes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DevelopmentProfile {
        return DevelopmentProfile(
            id = id,
            name = name,
            navigationMode = if (navigationModeIndex == 1) DevelopmentNavigationMode.AUTOMATIC else DevelopmentNavigationMode.MANUAL,
            steps = DevelopmentStepSerializer.deserializeSteps(stepsJson),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(profile: DevelopmentProfile): DevelopmentProfileEntity {
            return DevelopmentProfileEntity(
                id = profile.id,
                name = profile.name,
                navigationModeIndex = if (profile.navigationMode == DevelopmentNavigationMode.AUTOMATIC) 1 else 0,
                stepsJson = DevelopmentStepSerializer.serializeSteps(profile.steps),
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}
