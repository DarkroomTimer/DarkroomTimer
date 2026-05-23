package fr.mathgl.darkroomtimer.math

enum class ContrastGrade(val floatValue: Float, val label: String) {
    GRADE_00  (0.00f, "00"),
    GRADE_0   (0.00f, "0"),
    GRADE_HALF(0.50f, "½"),
    GRADE_1   (1.00f, "1"),
    GRADE_1H  (1.50f, "1½"),
    GRADE_2   (2.00f, "2"),
    GRADE_2H  (2.50f, "2½"),
    GRADE_3   (3.00f, "3"),
    GRADE_3H  (3.50f, "3½"),
    GRADE_4   (4.00f, "4"),
    GRADE_4H  (4.50f, "4½"),
    GRADE_5   (5.00f, "5");

    val index: Int get() = ordinal

    companion object {
        val DEFAULT = GRADE_2

        fun fromIndex(index: Int): ContrastGrade {
            val entries = ContrastGrade.entries
            return entries[index.coerceIn(0, entries.lastIndex)]
        }
    }
}
