package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeRelaySystem(
    override val enlarger: RelayController,
    override val safelight: RelayController? = null,
    scope: CoroutineScope
) : RelaySystem(enlarger, safelight, scope) {
    // This is just a wrapper for now to avoid Mockito suspend issues
    // In a real fake, we would track calls here.
}
