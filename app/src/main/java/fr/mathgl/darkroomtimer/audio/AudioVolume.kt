package fr.mathgl.darkroomtimer.audio

/**
 * Enum representing audio volume levels for the buzzer and metronome.
 */
enum class AudioVolume(val floatValue: Float) {
    MUTE(0f),
    QUIET(0.25f),
    MEDIUM(0.6f),
    LOUD(1.0f);

    /**
     * Returns the volume as a Float between 0.0 and 1.0.
     */
    fun toFloat(): Float = floatValue

    companion object {
        /**
         * Parses a volume from a string representation.
         * Returns MEDIUM as default for invalid/empty/null values.
         */
        fun fromString(value: String?): AudioVolume {
            return values().find { it.name == value } ?: MEDIUM
        }
    }
}
