package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class ContrastGradeTest {

    @Test
    fun `should have 12 grades`() {
        assertEquals(12, ContrastGrade.entries.size)
    }

    @Test
    fun `GRADE_00 and GRADE_0 both have floatValue 0,00 but are distinct`() {
        assertEquals(0.00f, ContrastGrade.GRADE_00.floatValue)
        assertEquals(0.00f, ContrastGrade.GRADE_0.floatValue)
        assertNotEquals(ContrastGrade.GRADE_00, ContrastGrade.GRADE_0)
    }

    @Test
    fun `GRADE_2 is the default grade`() {
        assertEquals(ContrastGrade.GRADE_2, ContrastGrade.DEFAULT)
    }

    @Test
    fun `labels match spec`() {
        assertEquals("00",  ContrastGrade.GRADE_00.label)
        assertEquals("0",   ContrastGrade.GRADE_0.label)
        assertEquals("½",   ContrastGrade.GRADE_HALF.label)
        assertEquals("1",   ContrastGrade.GRADE_1.label)
        assertEquals("1½",  ContrastGrade.GRADE_1H.label)
        assertEquals("2",   ContrastGrade.GRADE_2.label)
        assertEquals("2½",  ContrastGrade.GRADE_2H.label)
        assertEquals("3",   ContrastGrade.GRADE_3.label)
        assertEquals("3½",  ContrastGrade.GRADE_3H.label)
        assertEquals("4",   ContrastGrade.GRADE_4.label)
        assertEquals("4½",  ContrastGrade.GRADE_4H.label)
        assertEquals("5",   ContrastGrade.GRADE_5.label)
    }

    @Test
    fun `fromIndex returns correct grade`() {
        assertEquals(ContrastGrade.GRADE_00,   ContrastGrade.fromIndex(0))
        assertEquals(ContrastGrade.GRADE_0,    ContrastGrade.fromIndex(1))
        assertEquals(ContrastGrade.GRADE_HALF, ContrastGrade.fromIndex(2))
        assertEquals(ContrastGrade.GRADE_2,    ContrastGrade.fromIndex(5))
        assertEquals(ContrastGrade.GRADE_5,    ContrastGrade.fromIndex(11))
    }

    @Test
    fun `fromIndex clamps out-of-bounds values`() {
        assertEquals(ContrastGrade.GRADE_00, ContrastGrade.fromIndex(-1))
        assertEquals(ContrastGrade.GRADE_5,  ContrastGrade.fromIndex(100))
    }

    @Test
    fun `index property matches ordinal`() {
        ContrastGrade.entries.forEachIndexed { i, grade ->
            assertEquals(i, grade.index)
        }
    }
}
