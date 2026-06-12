package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.math.BurnDodgeEntry
import fr.mathgl.darkroomtimer.math.BurnDodgeType
import fr.mathgl.darkroomtimer.math.ContrastGrade

/**
 * Manages a list of burn/dodge entries for a countdown session.
 * Maximum of 9 entries as per spec.
 */
class BurnDodgeManager(
    private var baseTimeMs: Long = 8000L
) {
    private val entries = mutableListOf<BurnDodgeEntry>()
    private var nextId = 0

    val count: Int get() = entries.size
    val isEmpty: Boolean get() = entries.isEmpty()
    val isFull: Boolean get() = entries.size >= MAX_ENTRIES

    val entriesList: List<BurnDodgeEntry> get() = entries.toList()

    /**
     * Add a new burn/dodge entry.
     * @throws IllegalStateException if maximum entries reached
     */
    fun addEntry(
        label: String,
        type: BurnDodgeType,
        numerator: Int,
        denominator: Int,
        contrastGrade: ContrastGrade
    ): BurnDodgeEntry {
        check(entries.size < MAX_ENTRIES) { "Maximum $MAX_ENTRIES entries reached" }
        require(numerator > 0) { "numerator must be > 0" }
        require(denominator > 0) { "denominator must be > 0" }
        val entry = BurnDodgeEntry(
            id = nextId++,
            label = label.take(MAX_LABEL_LENGTH),
            type = type,
            numerator = numerator,
            denominator = denominator,
            contrastGrade = contrastGrade
        )
        entries.add(entry)
        return entry
    }

    /**
     * Update an existing entry by ID.
     * @return the updated entry, or null if not found
     */
    fun updateEntry(
        id: Int,
        label: String? = null,
        type: BurnDodgeType? = null,
        numerator: Int? = null,
        denominator: Int? = null,
        contrastGrade: ContrastGrade? = null
    ): BurnDodgeEntry? {
        val index = entries.indexOfFirst { it.id == id }
        if (index == -1) return null
        val original = entries[index]
        val updated = original.copy(
            label = (label ?: original.label).take(MAX_LABEL_LENGTH),
            type = type ?: original.type,
            numerator = numerator ?: original.numerator,
            denominator = denominator ?: original.denominator,
            contrastGrade = contrastGrade ?: original.contrastGrade
        )
        entries[index] = updated
        return updated
    }

    /**
     * Remove an entry by ID.
     * @return true if removed, false if not found
     */
    fun removeEntry(id: Int): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        if (index == -1) return false
        entries.removeAt(index)
        return true
    }

    /**
     * Move an entry one position up (towards index 0).
     * @return true if moved, false if not found or already first
     */
    fun moveUp(id: Int): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        if (index <= 0) return false
        val tmp = entries[index]
        entries[index] = entries[index - 1]
        entries[index - 1] = tmp
        return true
    }

    /**
     * Move an entry one position down (towards last index).
     * @return true if moved, false if not found or already last
     */
    fun moveDown(id: Int): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        if (index < 0 || index >= entries.size - 1) return false
        val tmp = entries[index]
        entries[index] = entries[index + 1]
        entries[index + 1] = tmp
        return true
    }

    /**
     * Clear all entries and reset ID counter.
     */
    fun clear() {
        entries.clear()
        nextId = 0
    }

    /**
     * Get an entry by ID.
     */
    fun getEntry(id: Int): BurnDodgeEntry? = entries.find { it.id == id }

    /**
     * Get adjustment time for a specific entry using current base time.
     */
    fun adjustmentTimeMsForEntry(id: Int): Long? {
        val entry = getEntry(id) ?: return null
        return entry.adjustmentTimeMs(baseTimeMs)
    }

    companion object {
        const val MAX_ENTRIES = 9
        const val MAX_LABEL_LENGTH = 32
    }
}
