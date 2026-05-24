package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade
import org.junit.Assert.*
import org.junit.Test

class BurnDodgeManagerTest {

    private lateinit var manager: BurnDodgeManager

    @Test
    fun `initial state is empty`() {
        manager = BurnDodgeManager()

        assertTrue(manager.isEmpty)
        assertFalse(manager.isFull)
        assertEquals(0, manager.count)
        assertTrue(manager.entriesList.isEmpty())
    }

    @Test
    fun `can add entries up to maximum`() {
        manager = BurnDodgeManager()

        repeat(9) { i ->
            manager.addEntry(
                label = "Zone $i",
                type = BurnDodgeType.BURN,
                numerator = 1,
                denominator = 3,
                contrastGrade = ContrastGrade.GRADE_2
            )
        }

        assertEquals(9, manager.count)
        assertTrue(manager.isFull)
    }

    @Test(expected = IllegalStateException::class)
    fun `throws when adding beyond maximum`() {
        manager = BurnDodgeManager()

        repeat(9) { i ->
            manager.addEntry(
                label = "Zone $i",
                type = BurnDodgeType.BURN,
                numerator = 1,
                denominator = 3,
                contrastGrade = ContrastGrade.GRADE_2
            )
        }

        // Should throw
        manager.addEntry(
            label = "Extra",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 3,
            contrastGrade = ContrastGrade.GRADE_2
        )
    }

    @Test
    fun `entries get sequential IDs`() {
        manager = BurnDodgeManager()

        val entry1 = manager.addEntry("A", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        val entry2 = manager.addEntry("B", BurnDodgeType.DODGE, 1, 2, ContrastGrade.GRADE_1)
        val entry3 = manager.addEntry("C", BurnDodgeType.BURN, 1, 4, ContrastGrade.GRADE_3)

        assertEquals(0, entry1.id)
        assertEquals(1, entry2.id)
        assertEquals(2, entry3.id)
    }

    @Test
    fun `can remove entry by ID`() {
        manager = BurnDodgeManager()

        val entry1 = manager.addEntry("A", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        manager.addEntry("B", BurnDodgeType.DODGE, 1, 2, ContrastGrade.GRADE_1)

        val removed = manager.removeEntry(entry1.id)

        assertTrue(removed)
        assertEquals(1, manager.count)
        assertNull(manager.getEntry(entry1.id))
    }

    @Test
    fun `returns false when removing non-existent entry`() {
        manager = BurnDodgeManager()

        val removed = manager.removeEntry(999)

        assertFalse(removed)
        assertEquals(0, manager.count)
    }

    @Test
    fun `can update entry fields`() {
        manager = BurnDodgeManager()

        val entry = manager.addEntry("Original", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)

        val updated = manager.updateEntry(
            id = entry.id,
            label = "Updated",
            type = BurnDodgeType.DODGE,
            numerator = 1,
            denominator = 2,
            contrastGrade = ContrastGrade.GRADE_4
        )

        assertNotNull(updated)
        assertEquals("Updated", updated!!.label)
        assertEquals(BurnDodgeType.DODGE, updated.type)
        assertEquals(1, updated.numerator)
        assertEquals(2, updated.denominator)
        assertEquals(ContrastGrade.GRADE_4, updated.contrastGrade)
    }

    @Test
    fun `update returns null for non-existent ID`() {
        manager = BurnDodgeManager()

        val updated = manager.updateEntry(
            id = 999,
            label = "Updated",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 3,
            contrastGrade = ContrastGrade.GRADE_2
        )

        assertNull(updated)
    }

    @Test
    fun `clear resets all entries and ID counter`() {
        manager = BurnDodgeManager()

        repeat(5) { i ->
            manager.addEntry("Zone $i", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        }

        manager.clear()

        assertTrue(manager.isEmpty)
        assertEquals(0, manager.count)

        // After clear, new entry should get ID 0 again
        val newEntry = manager.addEntry("New", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        assertEquals(0, newEntry.id)
    }

    @Test
    fun `label is truncated to maximum length`() {
        manager = BurnDodgeManager()

        val entry = manager.addEntry(
            label = "This is a very long label that exceeds the maximum allowed length for zone descriptions",
            type = BurnDodgeType.BURN,
            numerator = 1,
            denominator = 3,
            contrastGrade = ContrastGrade.GRADE_2
        )

        assertEquals(32, entry.label.length)
        assertEquals("This is a very long label that e", entry.label)
    }

    @Test
    fun `getEntry returns entry by ID`() {
        manager = BurnDodgeManager()

        val entry1 = manager.addEntry("A", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        val entry2 = manager.addEntry("B", BurnDodgeType.DODGE, 1, 2, ContrastGrade.GRADE_1)

        assertSame(entry1, manager.getEntry(entry1.id))
        assertSame(entry2, manager.getEntry(entry2.id))
        assertNull(manager.getEntry(999))
    }

    @Test
    fun `adjustmentTimeMsForEntry calculates correctly`() {
        manager = BurnDodgeManager(baseTimeMs = 10000L)

        val entry = manager.addEntry("A", BurnDodgeType.BURN, 1, 1, ContrastGrade.GRADE_2)

        // +1 stop = double time, adjustment = base = 10000ms
        val adjustment = manager.adjustmentTimeMsForEntry(entry.id)
        assertEquals(10000L, adjustment)
    }

    @Test
    fun `adjustmentTimeMsForEntry returns null for non-existent entry`() {
        manager = BurnDodgeManager()

        assertNull(manager.adjustmentTimeMsForEntry(999))
    }

    @Test
    fun `transitions from not full to full when adding last entry`() {
        manager = BurnDodgeManager()

        repeat(8) {
            manager.addEntry("Zone $it", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)
        }

        assertFalse(manager.isFull)

        manager.addEntry("Last", BurnDodgeType.BURN, 1, 3, ContrastGrade.GRADE_2)

        assertTrue(manager.isFull)
    }
}
