package fr.mathgl.darkroomtimer.system

data class RelayState(
    val enlargerOn: Boolean,
    val safelightOn: Boolean
) {
    companion object {
        val INITIAL = RelayState(enlargerOn = false, safelightOn = true)
        val RUNNING = RelayState(enlargerOn = true, safelightOn = false)
        val IDLE    = RelayState(enlargerOn = false, safelightOn = false)
    }
}
