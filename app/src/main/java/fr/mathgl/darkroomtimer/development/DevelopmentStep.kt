package fr.mathgl.darkroomtimer.development

import androidx.room.TypeConverters

enum class DevelopmentStepType {
    BATH, PAUSE
}

sealed class DevelopmentStep {
    abstract val id: Int
    abstract val name: String
    abstract val durationSeconds: Int

    /** Durée d'exposition déjà écoulée en secondes */
    abstract val elapsedSeconds: Long

    /** Retourne le temps restant en secondes */
    fun remainingSeconds(elapsed: Long): Int = maxOf(0, durationSeconds - elapsed.toInt())

    /** Retourne true si l'étape est terminée */
    fun hasEnded(elapsed: Long): Boolean = elapsed >= durationSeconds

    /** Retourne le type d'étape */
    abstract val type: DevelopmentStepType

    /** Copy method returning DevelopmentStep for polymorphic use */
    abstract fun copyWithElapsed(newElapsed: Long): DevelopmentStep

    data class BathStep(
        override val id: Int = 0,
        override val name: String,
        override val durationSeconds: Int,
        val preEndAlertSeconds: Int = 0,
        override val elapsedSeconds: Long = 0L
    ) : DevelopmentStep() {
        override val type: DevelopmentStepType = DevelopmentStepType.BATH

        /** Retourne true si l'alerte de pré-fin doit être déclenchée */
        fun isPreEndAlertTriggered(elapsed: Long): Boolean {
            return elapsed >= (durationSeconds - preEndAlertSeconds)
        }

        override fun copyWithElapsed(newElapsed: Long): DevelopmentStep {
            return this.copy(elapsedSeconds = newElapsed)
        }
    }

    data class PauseStep(
        override val id: Int = 0,
        override val name: String,
        override val durationSeconds: Int,
        override val elapsedSeconds: Long = 0L
    ) : DevelopmentStep() {
        override val type: DevelopmentStepType = DevelopmentStepType.PAUSE

        /** Always returns false for PauseStep */
        fun isPreEndAlertTriggered(elapsed: Long): Boolean = false

        override fun copyWithElapsed(newElapsed: Long): DevelopmentStep {
            return this.copy(elapsedSeconds = newElapsed)
        }
    }
}

/**
 * Convertisseur pour Room pour stocker DevelopmentStepType comme String
 */
class DevelopmentStepTypeConverter {
    companion object {
        @JvmStatic
        fun fromStepType(type: DevelopmentStepType): String = type.name

        @JvmStatic
        fun toStepType(value: String): DevelopmentStepType {
            return runCatching { DevelopmentStepType.valueOf(value) }.getOrDefault(DevelopmentStepType.BATH)
        }
    }
}
