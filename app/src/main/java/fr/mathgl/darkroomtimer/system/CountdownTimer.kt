package fr.mathgl.darkroomtimer.system

enum class TimerState { STOPPED, RUNNING, PAUSED }

class CountdownTimer(private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() }) {

    var configuredTimeMs: Long = 8000L
        set(value) {
            require(value in 100L..999_000L) { "configuredTimeMs must be in [100, 999000], was $value" }
            field = value
        }

    var state: TimerState = TimerState.STOPPED
        private set

    private var startAt: Long = 0L
    private var pauseAt: Long = 0L

    val isStarted: Boolean get() = state != TimerState.STOPPED
    val isPaused: Boolean  get() = state == TimerState.PAUSED

    fun start() {
        check(state == TimerState.STOPPED) { "start() called from state $state" }
        startAt = clock()
        state = TimerState.RUNNING
    }

    fun pause() {
        check(state == TimerState.RUNNING) { "pause() called from state $state" }
        pauseAt = clock()
        state = TimerState.PAUSED
    }

    fun resume() {
        check(state == TimerState.PAUSED) { "resume() called from state $state" }
        val now = clock()
        val elapsed = maxOf(0L, pauseAt - startAt)
        startAt = now - elapsed
        state = TimerState.RUNNING
    }

    fun stop() {
        check(state == TimerState.RUNNING || state == TimerState.PAUSED) {
            "stop() called from state $state"
        }
        state = TimerState.STOPPED
    }

    fun remainingMs(): Long = when (state) {
        TimerState.STOPPED -> configuredTimeMs
        TimerState.RUNNING -> configuredTimeMs - (clock() - startAt)
        TimerState.PAUSED  -> configuredTimeMs - (pauseAt - startAt)
    }

    fun tick(): Boolean {
        if (state == TimerState.RUNNING && remainingMs() <= 0) {
            state = TimerState.STOPPED
            return true
        }
        return false
    }

    companion object {
        fun formatTime(ms: Long): String {
            val clamped = maxOf(0L, ms)
            val totalTenths = clamped / 100
            val tenths = totalTenths % 10
            val totalSeconds = totalTenths / 10
            val seconds = totalSeconds % 60
            val minutes = totalSeconds / 60
            return "%02d:%02d.%d".format(minutes, seconds, tenths)
        }
    }
}
