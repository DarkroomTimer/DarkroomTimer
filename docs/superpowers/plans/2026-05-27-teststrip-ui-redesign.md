# TestStrip UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign TestStrip interface to fit on one screen, separate configuration from execution, auto-start first patch, and clarify status messages.

**Architecture:**
- Add pre-session configuration modal that appears when entering TestStrip screen
- Once "Démarrer" is clicked, session starts immediately (no CONFIGURED state visible to user)
- Main screen shows only session controls (Mode badge, Pause button, Increment type, +/- buttons, patch progression, timer)
- Configuration locked after session starts; +/- buttons adjust next patch time only
- Replace "En attente" with clear connection status

**Tech Stack:** Kotlin, Jetpack Compose, Android ViewModel

---

## Files to Modify

| File | Changes |
|------|---------|
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt` | Add pre-session config modal, simplify main view, remove "En attente" |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt` | Manage config phase state, lock config after start, allow +/- adjustment |
| `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt` | Remove CONFIGURED state handling, auto-start on validation |
| `app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt` | Support dynamic increment adjustment for next patch |

---

### Task 1: Update TeststripState to remove CONFIGURED

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripState.kt:6`

- [ ] Remove CONFIGURED from the enum
- [ ] Update state transitions to start at EXPOSING instead
- [ ] Update `abandon()` to reset to a new `INIT` or `IDLE` state instead of CONFIGURED

```kotlin
enum class TeststripState { INIT, EXPOSING, BETWEEN_PATCHES, PAUSED }
```

- [ ] Commit: `refactor: remove CONFIGURED state, replace with INIT`

---

### Task 2: Update TeststripSession to auto-start on validation

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt:12, 50-70, 116`

- [ ] Change initial state from `CONFIGURED` to `INIT`
- [ ] Remove `start()` logic that handles CONFIGURED state
- [ ] When transitioning from INIT to EXPOSING, set patch index to 0 and start immediately
- [ ] Update `abandon()` to reset to `INIT` state with patchIndex = -1

```kotlin
var state: TeststripState = INIT  // was CONFIGURED
```

- [ ] Commit: `feat: auto-start first patch on configuration validation`

---

### Task 3: Add pre-session configuration modal to TeststripScreen

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt:60-141, 241-258`
- Create: new `ConfigurationModal` composable component

- [ ] Create `ConfigurationModal` composable with:
  - Base time picker
  - Patch count selector (- / number / +)
  - Mode segmented control (Incremental / Séparé)
  - Increment type segmented control (f-stop / Seconds)
  - Increment value picker (fraction for f-stop, time for seconds)
  - "Démarrer" button (disabled if relay not connected)

```kotlin
@Composable
private fun ConfigurationModal(
    baseTimeMs: Long,
    patchCount: Int,
    mode: TeststripMode,
    incrementType: IncrementType,
    numerator: Int,
    denominator: Int,
    incrementMs: Long,
    isRelayConnected: Boolean,
    onConfirm: () -> Unit,
    onBaseTimeChange: (Long) -> Unit,
    onPatchCountChange: (Int) -> Unit,
    onModeChange: (TeststripMode) -> Unit,
    onIncrementTypeChange: (IncrementType) -> Unit,
    onStopFractionChange: (Int, Int) -> Unit,
    onIncrementMsChange: (Long) -> Unit
) {
    // Modal content with all config controls
}
```

- [ ] Show modal when `sessionState == INIT`
- [ ] On "Démarrer" click, call `viewModel.startSession()` which transitions directly to EXPOSING
- [ ] Commit: `feat: add pre-session configuration modal`

---

### Task 4: Simplify main TeststripScreen view

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt:63-120, 166-237`

- [ ] Remove ConfigurationSection from main view
- [ ] Remove "En attente" text (line 265)
- [ ] Line 1: Show Mode badge + Pause/Resume button
- [ ] Line 2: Show Increment type badge + +/- buttons for time adjustment
- [ ] Keep patch progression grid (circles)
- [ ] Keep large timer display during EXPOSING
- [ ] Keep "Annuler" button (replace "ABANDONNER")
- [ ] Update "En attente" message on button (line 253) - remove relay wait text

```kotlin
// Line 1 during session:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text("MODE: ${state.mode.name}", ...)  // Mode badge
    Button(onClick = { viewModel.pause() }) { Text("PAUSE") }
}

// Line 2 during session:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Text("INC: ${state.incrementType.name}", ...)  // Increment type badge
    Row {
        OutlinedButton(onClick = { viewModel.adjustIncrement(-1) }) { Text("-") }
        OutlinedButton(onClick = { viewModel.adjustIncrement(+1) }) { Text("+") }
    }
}
```

- [ ] Commit: `feat: simplify main view, remove ConfigurationSection`

---

### Task 5: Update TeststripViewModel to lock config and allow increment adjustment

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt:58-76, 141-150, 232-267`

- [ ] Update initial state: `sessionState = INIT` (was CONFIGURED)
- [ ] Update `startSession()` to transition directly to EXPOSING (no CONFIGURED check)
- [ ] Lock config updates after session starts:
  - `updateBaseTime()`: only allow if `state != TeststripState.EXPOSING`
  - `updateStopFraction()`: only allow if `state != TeststripState.EXPOSING`
  - `updatePatchCount()`: only allow if `state != TeststripState.EXPOSING`
  - `updateMode()`: only allow if `state != TeststripState.EXPOSING`
  - `updateIncrementType()`: only allow if `state != TeststripState.EXPOSING`
  - `updateIncrementMs()`: only allow if `state != TeststripState.EXPOSING`

- [ ] Add `adjustIncrement(delta: Int)` function to modify next patch time:
```kotlin
fun adjustIncrement(delta: Int) {
    if (session.state != TeststripState.EXPOSING && session.state != TeststripState.BETWEEN_PATCHES) return
    // Adjust the increment value, affecting NEXT patch only
    engine.incrementMs = (engine.incrementMs + delta * 1000).coerceAtLeast(1000)
    updateUiState()
}
```

- [ ] Commit: `feat: lock config after start, add increment adjustment`

---

### Task 6: Update TeststripEngine to support dynamic adjustment

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt:20-27`

- [ ] Keep current structure - already supports mutable `incrementMs`
- [ ] Ensure `getRelayDuration(index)` uses current values for calculation
- [ ] Verify patch times update when increment changes

- [ ] Commit: `refactor: verify dynamic increment support`

---

### Task 7: Run tests and verify

**Files:**
- Test: existing TeststripEngine tests
- Test: existing TeststripSession tests

- [ ] Run: `./gradlew test --tests "*TeststripEngine*"`
- [ ] Run: `./gradlew test --tests "*TeststripSession*"`
- [ ] Fix any broken tests due to state enum changes
- [ ] Commit: `fix: update tests for new state model`

---

## Self-Review Checklist

After completing all tasks:

1. **Spec coverage:** Each requirement from the spec has a task
2. **Type consistency:** `TeststripState` enum changes propagated everywhere
3. **Placeholder scan:** No "TBD", "TODO", or vague instructions in plan
4. **Scope check:** Plan focused on UI redesign only, no unrelated refactoring

---

## Success Criteria (from spec)

- [ ] All controls visible without scrolling on a standard phone screen
- [ ] User can complete a teststrip session without navigating between screens
- [ ] "En attente" message is eliminated or clarified
- [ ] Configuration cannot be modified after session starts
- [ ] First patch auto-starts after clicking "Démarrer"
