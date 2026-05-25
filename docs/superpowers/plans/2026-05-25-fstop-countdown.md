# F-Stop Correction in CountDown — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add f-stop correction buttons (±1, ±½, ±⅓, ±⅙ stop) to the CountDown screen, with cumulative fraction accumulation, Reset, and Set actions.

**Architecture:** The correction is stored as `(fStopCorrectionNumerator, fStopCorrectionDenominator)` in `CountdownUiState`, accumulated via `FStopMath.simplify()`. The ViewModel keeps a private `baseTimeMs` field. `timer.configuredTimeMs` is set to `calculatedTimeMs()` just before `start()` and restored to `baseTimeMs` on `stop()`. All math reuses existing `FStopMath` utilities unchanged.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Mockito, `kotlinx-coroutines-test`

---

## Files

- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`
- Modify: `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt`

---

## Task 1: Add correction fields to `CountdownUiState` and `baseTimeMs` to ViewModel

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt`

- [ ] **Step 1.1: Write failing tests for initial state**

Add to `CountdownViewModelTest.kt` (inside the class, after existing tests):

```kotlin
@Test
fun `initial fStopCorrectionNumerator should be 0`() = runTest {
    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
}

@Test
fun `initial fStopCorrectionDenominator should be 1`() = runTest {
    assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
}
```

- [ ] **Step 1.2: Run tests to confirm they fail**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.initial fStopCorrectionNumerator should be 0" --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.initial fStopCorrectionDenominator should be 1"
```

Expected: FAILED — `Unresolved reference: fStopCorrectionNumerator`

- [ ] **Step 1.3: Add fields to `CountdownUiState`**

In `CountdownViewModel.kt`, find `data class CountdownUiState(` and add two fields after `val errorMessage: String? = null`:

```kotlin
data class CountdownUiState(
    val displayTime: String,
    val timerState: TimerState,
    val relayState: RelayStates,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long,
    val burnDodgeEntries: List<BurnDodgeEntry>,
    val burnDodgeVisible: Boolean,
    val maxEntriesReached: Boolean,
    val showTimeEditor: Boolean = false,
    val enlargerOverride: Boolean = false,
    val safelightOverride: Boolean = false,
    val relayType: String = "NULL",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
    val fStopCorrectionNumerator: Int = 0,
    val fStopCorrectionDenominator: Int = 1
)
```

- [ ] **Step 1.4: Add `baseTimeMs` private field and update `init{}`**

In `CountdownViewModel`, add the private field after the existing private fields (after `private var tickJob: Job? = null`):

```kotlin
private var baseTimeMs: Long = timer.configuredTimeMs
```

In `init{}`, inside the `try { ... }` block that loads from preferences, add `baseTimeMs = timer.configuredTimeMs` right after `timer.configuredTimeMs = prefs.defaultExposureMs`:

```kotlin
try {
    val context = getApplication<Application>()
    val prefs = PreferenceManager.getInstance(context)
    timer.configuredTimeMs = prefs.defaultExposureMs
    baseTimeMs = timer.configuredTimeMs              // ADD THIS LINE
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
        configuredTimeMs = timer.configuredTimeMs,
        selectedGrade = prefs.defaultContrastGrade
    ) }
} catch (e: Exception) {
    // prefs unavailable in test environment, keep hardcoded defaults
}
```

- [ ] **Step 1.5: Run the two new tests to confirm they pass**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.initial fStopCorrectionNumerator should be 0" --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.initial fStopCorrectionDenominator should be 1"
```

Expected: PASSED

- [ ] **Step 1.6: Run all ViewModel tests to check for regressions**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest"
```

Expected: all PASSED

- [ ] **Step 1.7: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt
git commit -m "$(cat <<'EOF'
feat: add fStopCorrection fraction fields to CountdownUiState and baseTimeMs to ViewModel
EOF
)"
```

---

## Task 2: `calculatedTimeMs()` helper + update `start()`, `stop()`, `adjustTime()`, `setTimeFromInput()`, `launchTickJob()`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt`

- [ ] **Step 2.1: Write failing tests**

Add to `CountdownViewModelTest.kt`:

```kotlin
@Test
fun `start should use calculatedTimeMs when correction is active`() = runTest {
    // Set base to 8000ms (default is 8000 in test, but be explicit)
    val diff = 8000L - viewModel.uiState.value.configuredTimeMs
    if (diff != 0L) viewModel.adjustTime(diff)
    testDispatcher.scheduler.runCurrent()

    // Apply +1 stop → calculatedTimeMs = 16000ms
    viewModel.applyFStopDelta(1, 1)
    testDispatcher.scheduler.runCurrent()

    viewModel.start()
    testDispatcher.scheduler.runCurrent()

    // Advance past base time (8000ms) but before calculated time (16000ms)
    testDispatcher.scheduler.advanceTimeBy(9000L)
    testDispatcher.scheduler.runCurrent()

    // If start() used baseTimeMs (8000ms), the timer would have ended — so STOPPED.
    // If start() used calculatedTimeMs (16000ms), the timer is still running.
    assertEquals(TimerState.RUNNING, viewModel.uiState.value.timerState)
}

@Test
fun `stop should display calculatedTimeMs after stopping`() = runTest {
    val diff = 8000L - viewModel.uiState.value.configuredTimeMs
    if (diff != 0L) viewModel.adjustTime(diff)
    testDispatcher.scheduler.runCurrent()

    // Apply +1 stop → calculatedTimeMs = 16000ms → displayTime = "00:16.0"
    viewModel.applyFStopDelta(1, 1)
    testDispatcher.scheduler.runCurrent()

    viewModel.start()
    testDispatcher.scheduler.runCurrent()
    viewModel.stop()
    testDispatcher.scheduler.runCurrent()

    assertEquals("00:16.0", viewModel.uiState.value.displayTime)
}

@Test
fun `adjustTime when STOPPED should adjust base time while keeping correction`() = runTest {
    val diff = 8000L - viewModel.uiState.value.configuredTimeMs
    if (diff != 0L) viewModel.adjustTime(diff)
    testDispatcher.scheduler.runCurrent()

    // Apply +1/3 stop
    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()

    // Adjust base by +1000ms
    viewModel.adjustTime(1000L)
    testDispatcher.scheduler.runCurrent()

    // Base should be 9000ms
    assertEquals(9000L, viewModel.uiState.value.configuredTimeMs)
    // Correction unchanged
    assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(3, viewModel.uiState.value.fStopCorrectionDenominator)
    // displayTime = formatTime(9000 * 2^(1/3)) = formatTime(FStopMath.adjustTime(9000, 1, 3, 1))
    val expectedMs = fr.mathgl.darkroomtimer.math.FStopMath.adjustTime(9000L, 1, 3, 1)
    assertEquals(CountdownTimer.formatTime(expectedMs), viewModel.uiState.value.displayTime)
}

@Test
fun `setTimeFromInput should reset fstop correction to zero`() = runTest {
    viewModel.applyFStopDelta(1, 2)
    testDispatcher.scheduler.runCurrent()

    viewModel.setTimeFromInput(0, 10, 0)  // 10 seconds
    testDispatcher.scheduler.runCurrent()

    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
    assertEquals(10_000L, viewModel.uiState.value.configuredTimeMs)
    assertEquals(CountdownTimer.formatTime(10_000L), viewModel.uiState.value.displayTime)
}
```

Note: these tests call `applyFStopDelta` which doesn't exist yet — they will fail with `Unresolved reference`.

- [ ] **Step 2.2: Run to confirm they fail**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.start should use calculatedTimeMs when correction is active"
```

Expected: FAILED — compile error `Unresolved reference: applyFStopDelta`

- [ ] **Step 2.3: Add `calculatedTimeMs()` private helper to `CountdownViewModel`**

Add this private function after `private var baseTimeMs: Long = timer.configuredTimeMs`:

```kotlin
private fun calculatedTimeMs(): Long {
    val state = _uiState.value
    if (state.fStopCorrectionNumerator == 0) return baseTimeMs
    return fr.mathgl.darkroomtimer.math.FStopMath
        .adjustTime(baseTimeMs, state.fStopCorrectionNumerator, state.fStopCorrectionDenominator, 1)
        .coerceIn(100L, 999_000L)
}
```

Add the import at the top of the file:
```kotlin
import fr.mathgl.darkroomtimer.math.FStopMath
```

- [ ] **Step 2.4: Add stub `applyFStopDelta` so tests compile**

Add a stub after `calculatedTimeMs()`:

```kotlin
fun applyFStopDelta(numerator: Int, denominator: Int) { /* implemented in Task 3 */ }
```

- [ ] **Step 2.5: Update `start()` to use `calculatedTimeMs()`**

Find the `fun start()` method. Add `timer.configuredTimeMs = calculatedTimeMs()` as the first line inside the method (after the guard):

```kotlin
fun start() {
    if (timer.state != TimerState.STOPPED) return
    timer.configuredTimeMs = calculatedTimeMs()       // ADD: use calculated time
    timer.start()
    // ... rest unchanged
```

- [ ] **Step 2.6: Update `stop()` to restore `baseTimeMs` and display `calculatedTimeMs()`**

Find `fun stop()`. After `timer.stop()`, add `timer.configuredTimeMs = baseTimeMs`. Change the `_uiState.update` to use `calculatedTimeMs()` instead of `timer.configuredTimeMs`:

```kotlin
fun stop() {
    if (timer.state == TimerState.STOPPED) return
    val wasPaused = timer.state == TimerState.PAUSED
    val timerCompletedNaturally = timer.state == TimerState.RUNNING
    tickJob?.cancel(); tickJob = null
    timer.stop()
    timer.configuredTimeMs = baseTimeMs                           // ADD: restore base
    viewModelScope.launch {
        val res1 = relaySystem.setEnlarger(false)
        val res2 = relaySystem.setSafelight(false)
        if (!res1.isSuccess || !res2.isSuccess) {
            _uiState.update { it.copy(errorMessage = "CRITICAL: Failed to shut off relays!") }
        }
    }
    if (timerCompletedNaturally) {
        audioSystem?.stopExposure()
    }
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(calculatedTimeMs()),  // CHANGE: was timer.configuredTimeMs
        timerState = TimerState.STOPPED,
        enlargerOverride = false,
        safelightOverride = false
    ) }
    if (!wasPaused) {
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }
}
```

- [ ] **Step 2.7: Update `adjustTime()` to split STOPPED vs PAUSED paths**

Replace the existing `adjustTime` implementation:

```kotlin
fun adjustTime(deltaMs: Long) {
    if (timer.state == TimerState.RUNNING) return
    if (timer.state == TimerState.STOPPED) {
        val newBase = (baseTimeMs + deltaMs).coerceIn(100L, 999_000L)
        baseTimeMs = newBase
        val calc = calculatedTimeMs()
        timer.configuredTimeMs = calc
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(calc),
            configuredTimeMs = newBase
        ) }
    } else {
        // PAUSED: fine-tune remaining time; does not affect baseTimeMs or correction
        val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
        timer.configuredTimeMs = newTime
        _uiState.update { it.copy(
            displayTime = CountdownTimer.formatTime(timer.remainingMs())
        ) }
    }
}
```

- [ ] **Step 2.8: Update `setTimeFromInput()` to reset correction**

Replace the existing `setTimeFromInput` implementation:

```kotlin
fun setTimeFromInput(minutes: Int, seconds: Int, tenths: Int) {
    val newBase = (minutes * 60_000L + seconds * 1_000L + tenths * 100L).coerceIn(100L, 999_000L)
    baseTimeMs = newBase
    timer.configuredTimeMs = newBase
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(newBase),
        configuredTimeMs = newBase,
        fStopCorrectionNumerator = 0,
        fStopCorrectionDenominator = 1,
        showTimeEditor = false
    ) }
}
```

- [ ] **Step 2.9: Update `launchTickJob()` to show `calculatedTimeMs()` on natural end**

In `launchTickJob()`, change the `_uiState.update` block to handle the end-of-timer case:

```kotlin
private fun launchTickJob(): Job = viewModelScope.launch {
    while (true) {
        val ended = timer.tick()
        val remaining = maxOf(0L, timer.remainingMs())
        if (ended) timer.configuredTimeMs = baseTimeMs           // ADD: restore base on natural end
        _uiState.update { it.copy(
            displayTime = if (ended) CountdownTimer.formatTime(calculatedTimeMs())
                          else CountdownTimer.formatTime(remaining),  // CHANGE
            timerState = timer.state,
            enlargerOverride = if (ended) false else it.enlargerOverride,   // ADD
            safelightOverride = if (ended) false else it.safelightOverride  // ADD
        ) }
        sendServiceIntent(
            if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
            remaining
        )
        if (ended) {
            viewModelScope.launch {
                val res1 = relaySystem.setEnlarger(false)
                val res2 = relaySystem.setSafelight(false)
                if (!res1.isSuccess || !res2.isSuccess) {
                    _uiState.update { it.copy(errorMessage = "CRITICAL: Failed to shut off relays on timer end!") }
                }
            }
            audioSystem?.stopExposure()
            tickJob = null
            break
        }
        delay(50L)
    }
}
```

- [ ] **Step 2.10: Run new tests**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.start should use calculatedTimeMs when correction is active" \
               --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.stop should display calculatedTimeMs after stopping" \
               --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.adjustTime when STOPPED should adjust base time while keeping correction" \
               --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.setTimeFromInput should reset fstop correction to zero"
```

Expected: all PASSED. (The `applyFStopDelta` stub does nothing, so corrections are never active — tests that apply +1 stop will see no correction, hence may fail. That's expected at this intermediate step; they will pass after Task 3.)

Actually: `applyFStopDelta` is a stub so `fStopCorrectionNumerator` stays 0. So:
- "start should use calculatedTimeMs" — `calculatedTimeMs()` = baseTimeMs = 8000ms. After 9000ms timer ends → STOPPED. Test expects RUNNING → **will FAIL** until Task 3. That is expected.
- "stop should display calculatedTimeMs" — no correction, calculated = 8000ms → "00:08.0". But timer already started → may tick past that. Accept partial failure here.
- "adjustTime when STOPPED" — correction stays 0, test will pass since displayTime = formatTime(calculatedTimeMs(9000, 0)) = formatTime(9000). But test expects FStopMath.adjustTime(9000, 1, 3, 1). **Will FAIL** until Task 3.
- "setTimeFromInput should reset correction" — passes since correction is already 0.

Run all existing tests to confirm no regressions:

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest"
```

Expected: existing tests all PASSED. New tests may fail — that's fine at this step.

- [ ] **Step 2.11: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt
git commit -m "$(cat <<'EOF'
feat: add calculatedTimeMs helper, update start/stop/adjustTime/setTimeFromInput for f-stop correction
EOF
)"
```

---

## Task 3: Implement `applyFStopDelta()`, `resetFStopCorrection()`, `setFStopCorrectionAsBase()`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt`

- [ ] **Step 3.1: Write failing tests**

Add to `CountdownViewModelTest.kt`:

```kotlin
@Test
fun `applyFStopDelta should accumulate same-denominator fractions correctly`() = runTest {
    // +1/3 + 1/3 = 2/3 (simplified)
    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()
    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()

    assertEquals(2, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(3, viewModel.uiState.value.fStopCorrectionDenominator)
}

@Test
fun `applyFStopDelta should accumulate different-denominator fractions correctly`() = runTest {
    // +1/2 + 1/3 = 5/6
    viewModel.applyFStopDelta(1, 2)
    testDispatcher.scheduler.runCurrent()
    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()

    assertEquals(5, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(6, viewModel.uiState.value.fStopCorrectionDenominator)
}

@Test
fun `applyFStopDelta should update displayTime to calculated time`() = runTest {
    val diff = 8000L - viewModel.uiState.value.configuredTimeMs
    if (diff != 0L) viewModel.adjustTime(diff)
    testDispatcher.scheduler.runCurrent()

    // +1 stop on 8000ms → 16000ms → "00:16.0"
    viewModel.applyFStopDelta(1, 1)
    testDispatcher.scheduler.runCurrent()

    assertEquals("00:16.0", viewModel.uiState.value.displayTime)
}

@Test
fun `applyFStopDelta should be rejected when result would be below 100ms`() = runTest {
    // Set base to 200ms
    viewModel.adjustTime(200L - viewModel.uiState.value.configuredTimeMs)
    testDispatcher.scheduler.runCurrent()

    // -1 stop: 200 * 2^(-1) = 100ms → accepted (boundary value)
    viewModel.applyFStopDelta(-1, 1)
    testDispatcher.scheduler.runCurrent()
    assertEquals(-1, viewModel.uiState.value.fStopCorrectionNumerator)

    // Another -1 stop: 200 * 2^(-2) = 50ms → rejected
    viewModel.applyFStopDelta(-1, 1)
    testDispatcher.scheduler.runCurrent()
    assertEquals(-1, viewModel.uiState.value.fStopCorrectionNumerator) // unchanged
}

@Test
fun `applyFStopDelta should be rejected when result would exceed 999000ms`() = runTest {
    // Set base to 700000ms (700s)
    viewModel.adjustTime(700_000L - viewModel.uiState.value.configuredTimeMs)
    testDispatcher.scheduler.runCurrent()

    // +1 stop: 700000 * 2 = 1400000ms → rejected (> 999000)
    viewModel.applyFStopDelta(1, 1)
    testDispatcher.scheduler.runCurrent()
    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator) // unchanged
}

@Test
fun `applyFStopDelta should do nothing when timer is RUNNING`() = runTest {
    viewModel.start()
    testDispatcher.scheduler.runCurrent()

    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()

    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
}

@Test
fun `resetFStopCorrection should clear correction and restore base displayTime`() = runTest {
    val baseMs = viewModel.uiState.value.configuredTimeMs

    viewModel.applyFStopDelta(1, 3)
    testDispatcher.scheduler.runCurrent()

    viewModel.resetFStopCorrection()
    testDispatcher.scheduler.runCurrent()

    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
    assertEquals(CountdownTimer.formatTime(baseMs), viewModel.uiState.value.displayTime)
}

@Test
fun `setFStopCorrectionAsBase should promote calculated time to base`() = runTest {
    val diff = 8000L - viewModel.uiState.value.configuredTimeMs
    if (diff != 0L) viewModel.adjustTime(diff)
    testDispatcher.scheduler.runCurrent()

    // +1 stop → 16000ms
    viewModel.applyFStopDelta(1, 1)
    testDispatcher.scheduler.runCurrent()

    viewModel.setFStopCorrectionAsBase()
    testDispatcher.scheduler.runCurrent()

    assertEquals(16_000L, viewModel.uiState.value.configuredTimeMs)
    assertEquals(0, viewModel.uiState.value.fStopCorrectionNumerator)
    assertEquals(1, viewModel.uiState.value.fStopCorrectionDenominator)
    assertEquals(CountdownTimer.formatTime(16_000L), viewModel.uiState.value.displayTime)
}
```

- [ ] **Step 3.2: Run to confirm they fail**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest.applyFStopDelta should accumulate same-denominator fractions correctly"
```

Expected: FAILED — stub does nothing

- [ ] **Step 3.3: Replace stub `applyFStopDelta` with full implementation**

Replace the stub `fun applyFStopDelta(...)` in `CountdownViewModel.kt`:

```kotlin
fun applyFStopDelta(numerator: Int, denominator: Int) {
    if (timer.state != TimerState.STOPPED) return
    val currentN = _uiState.value.fStopCorrectionNumerator
    val currentD = _uiState.value.fStopCorrectionDenominator
    val rawN = currentN * denominator + numerator * currentD
    val rawD = currentD * denominator
    val (simplN, simplD) = FStopMath.simplify(rawN, rawD)
    val calc = FStopMath.adjustTime(baseTimeMs, simplN, simplD, 1)
    if (calc !in 100L..999_000L) return
    timer.configuredTimeMs = calc
    _uiState.update { it.copy(
        fStopCorrectionNumerator = simplN,
        fStopCorrectionDenominator = simplD,
        displayTime = CountdownTimer.formatTime(calc)
    ) }
}
```

- [ ] **Step 3.4: Add `resetFStopCorrection()` after `applyFStopDelta`**

```kotlin
fun resetFStopCorrection() {
    if (timer.state != TimerState.STOPPED) return
    timer.configuredTimeMs = baseTimeMs
    _uiState.update { it.copy(
        fStopCorrectionNumerator = 0,
        fStopCorrectionDenominator = 1,
        displayTime = CountdownTimer.formatTime(baseTimeMs)
    ) }
}
```

- [ ] **Step 3.5: Add `setFStopCorrectionAsBase()` after `resetFStopCorrection`**

```kotlin
fun setFStopCorrectionAsBase() {
    if (timer.state != TimerState.STOPPED) return
    val calc = calculatedTimeMs()
    baseTimeMs = calc
    timer.configuredTimeMs = calc
    _uiState.update { it.copy(
        configuredTimeMs = calc,
        fStopCorrectionNumerator = 0,
        fStopCorrectionDenominator = 1,
        displayTime = CountdownTimer.formatTime(calc)
    ) }
}
```

- [ ] **Step 3.6: Run all ViewModel tests**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest"
```

Expected: all PASSED (including all new tests from Tasks 1–3 and all original tests)

- [ ] **Step 3.7: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt
git commit -m "$(cat <<'EOF'
feat: implement applyFStopDelta, resetFStopCorrection, setFStopCorrectionAsBase
EOF
)"
```

---

## Task 4: UI — `FStopCorrectionSection` composable in `CountdownScreen.kt`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

There are no automated UI tests for this task. Verification is manual (build + visual inspection described in Step 4.5).

- [ ] **Step 4.1: Add `FStopMath` import to `CountdownScreen.kt`**

Add to the existing imports block:

```kotlin
import fr.mathgl.darkroomtimer.math.FStopMath
```

- [ ] **Step 4.2: Add `FStopCorrectionSection` private composable**

Add this composable at the bottom of `CountdownScreen.kt`, after the existing `TimerControlButtons` composable:

```kotlin
@Composable
private fun FStopCorrectionSection(
    correctionNumerator: Int,
    correctionDenominator: Int,
    baseTimeMs: Long,
    onApplyDelta: (numerator: Int, denominator: Int) -> Unit,
    onReset: () -> Unit,
    onSet: () -> Unit
) {
    val hasCorrection = correctionNumerator != 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
            Text(
                text = " F-Stop ",
                fontSize = 11.sp,
                color = DarkroomRedDim
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = DarkroomRedFaint)
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TimeAdjustButton(label = "-1")  { onApplyDelta(-1, 1) }
            TimeAdjustButton(label = "-½")  { onApplyDelta(-1, 2) }
            TimeAdjustButton(label = "-⅓")  { onApplyDelta(-1, 3) }
            TimeAdjustButton(label = "-⅙")  { onApplyDelta(-1, 6) }
            Spacer(modifier = Modifier.width(4.dp))
            TimeAdjustButton(label = "+⅙")  { onApplyDelta(1, 6) }
            TimeAdjustButton(label = "+⅓")  { onApplyDelta(1, 3) }
            TimeAdjustButton(label = "+½")  { onApplyDelta(1, 2) }
            TimeAdjustButton(label = "+1")  { onApplyDelta(1, 1) }
        }

        if (hasCorrection) {
            Spacer(modifier = Modifier.height(8.dp))
            val sign = if (correctionNumerator > 0) "+" else ""
            val correctionLabel = FStopMath.formatStop(correctionNumerator, correctionDenominator)
            val calcMs = FStopMath.adjustTime(baseTimeMs, correctionNumerator, correctionDenominator, 1)
                .coerceIn(100L, 999_000L)
            Text(
                text = "Correction : $sign$correctionLabel stop → ${CountdownTimer.formatTime(calcMs)}",
                fontSize = 12.sp,
                color = DarkroomRedBright
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedDim),
                    border = BorderStroke(1.dp, DarkroomRedFaint)
                ) {
                    Text("Reset", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onSet,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkroomRedBright),
                    border = BorderStroke(1.dp, DarkroomRedBright)
                ) {
                    Text("Set", fontSize = 12.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 4.3: Wire `FStopCorrectionSection` into `CountdownScreen`**

In `CountdownScreen`, find the block:

```kotlin
// Time adjustment (only when not RUNNING)
if (state.timerState != TimerState.RUNNING) {
    TimeAdjustRow(onAdjust = { viewModel.adjustTime(it) })
    Spacer(modifier = Modifier.height(24.dp))
}
```

Replace with:

```kotlin
// Time adjustment (only when not RUNNING)
if (state.timerState != TimerState.RUNNING) {
    TimeAdjustRow(onAdjust = { viewModel.adjustTime(it) })
}

// F-Stop correction (only when STOPPED)
if (state.timerState == TimerState.STOPPED) {
    Spacer(modifier = Modifier.height(8.dp))
    FStopCorrectionSection(
        correctionNumerator = state.fStopCorrectionNumerator,
        correctionDenominator = state.fStopCorrectionDenominator,
        baseTimeMs = state.configuredTimeMs,
        onApplyDelta = { n, d -> viewModel.applyFStopDelta(n, d) },
        onReset = { viewModel.resetFStopCorrection() },
        onSet = { viewModel.setFStopCorrectionAsBase() }
    )
}
Spacer(modifier = Modifier.height(16.dp))
```

- [ ] **Step 4.4: Build to verify compilation**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4.5: Manual verification checklist**

Install on a device or emulator (`./gradlew installDebug`) and verify:

1. **Initial state**: CountDown screen shows the 8 f-stop buttons below the ±time buttons. No correction label visible.
2. **Apply +1 stop**: Tap `+1`. Large timer updates from e.g. "00:08.0" to "00:16.0". Correction label appears: "Correction : +1 stop → 00:16.0". Reset and Set buttons appear.
3. **Apply +1/3 stop twice**: Tap `+⅓` twice starting from base 8s. Correction = +2/3, calculated ≈ "00:12.6". Label shows "+2/3 stop → 00:12.6".
4. **Example from spec**: Base 8s, tap `+⅓` once → "Correction : +1/3 stop → 00:10.0". Then tap `+1` once → "+1 1/3 stop → 00:20.1".
5. **Reset**: With active correction, tap Reset. Timer returns to base, correction label disappears.
6. **Set**: Apply correction, tap Set. Correction disappears, base time becomes the calculated time. Timer shows new base.
7. **Existing ±time buttons**: With correction active, tap `+1s`. Base increases, correction stays, calculated time updates accordingly.
8. **Timer editor**: With correction active, open editor (tap timer display), set a new time. After confirm, correction resets to 0.
9. **PAUSED state**: Start and pause. F-stop section is hidden. ±time buttons still visible.
10. **RUNNING state**: Both f-stop section and ±time buttons hidden.

- [ ] **Step 4.6: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt
git commit -m "$(cat <<'EOF'
feat: add FStopCorrectionSection to CountdownScreen with 8 stop buttons, correction label, Reset and Set
EOF
)"
```
