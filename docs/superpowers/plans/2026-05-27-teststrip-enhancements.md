# Teststrip Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance the Teststrip feature to support flexible patch counts, linear increments (seconds), and different exposure modes (Incremental vs Separate).

**Architecture:** Move all timing logic into `TeststripEngine`. `TeststripSession` delegates relay duration to the engine. `TeststripViewModel` manages the configuration state and guards the session start based on relay connectivity.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4.

---

### Task 1: TeststripEngine Math & Logic

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt`
- Create: `app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt`

- [ ] **Step 1: Define Enums for Mode and Increment Type**
Add `TeststripMode { INCREMENTAL, SEPARATE }` and `IncrementType { F_STOP, SECONDS }` to `TeststripEngine.kt` (or as top-level in the package).

- [ ] **Step 2: Update TeststripEngine properties**
Modify the constructor to include:
- `var patchCount: Int` (range 3-12)
- `var mode: TeststripMode`
- `var incrementType: IncrementType`
- `var incrementMs: Long` (used when type is `SECONDS`)
- Keep `numerator` and `denominator` for `F_STOP`.

- [ ] **Step 3: Implement Absolute Time Calculation**
Create a private method `calculateAbsoluteTime(index: Int): Long`:
- If `F_STOP`: `(baseTimeMs * 2.0.pow((numerator.toDouble() / denominator) * index)).roundToLong()`
- If `SECONDS`: `baseTimeMs + (incrementMs * index)`

- [ ] **Step 4: Implement `getRelayDuration(index: Int)`**
```kotlin
fun getRelayDuration(index: Int): Long {
    val current = calculateAbsoluteTime(index)
    return if (mode == TeststripMode.SEPARATE) {
        current
    } else {
        if (index == 0) current else current - calculateAbsoluteTime(index - 1)
    }
}
```

- [ ] **Step 5: Write failing tests for all 4 combinations**
Create `TeststripEngineTest.kt` and add tests for:
1. F-Stop + Separate
2. F-Stop + Incremental
3. Seconds + Separate
4. Seconds + Incremental
Example:
```kotlin
@Test
fun testSecondsIncremental() {
    val engine = TeststripEngine(baseTimeMs = 8000, patchCount = 3, 
                                 mode = TeststripMode.INCREMENTAL, 
                                 incrementType = IncrementType.SECONDS, 
                                 incrementMs = 2000)
    assertEquals(8000L, engine.getRelayDuration(0))
    assertEquals(2000L, engine.getRelayDuration(1))
    assertEquals(2000L, engine.getRelayDuration(2))
}
```

- [ ] **Step 6: Run tests to verify they fail**
Run: `./gradlew test --tests "fr.mathgl.darkroomtimer.math.TeststripEngineTest"`

- [ ] **Step 7: Implement remaining logic and verify tests pass**
Run: `./gradlew test --tests "fr.mathgl.darkroomtimer.math.TeststripEngineTest"`
Expected: PASS

- [ ] **Step 8: Commit**
```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt
git commit -m "feat: implement flexible timing logic in TeststripEngine"
```

### Task 2: TeststripSession Integration

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt`

- [ ] **Step 1: Update `currentExposureTimeMs`**
Change the getter to:
```kotlin
val currentExposureTimeMs: Long
    get() = if (currentPatchIndex < 0) 0L else engine.getRelayDuration(currentPatchIndex)
```

- [ ] **Step 2: Verify consistency**
Ensure that `remainingTimeMs` still works correctly as it depends on `currentExposureTimeMs`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt
git commit -m "refactor: delegate relay duration to TeststripEngine"
```

### Task 3: ViewModel State & Guard

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`

- [ ] **Step 1: Expand `TeststripUiState`**
Add:
- `patchCount: Int`
- `mode: TeststripMode`
- `incrementType: IncrementType`
- `incrementMs: Long`

- [ ] **Step 2: Add configuration update methods**
Add methods for:
- `updatePatchCount(count: Int)`
- `updateMode(mode: TeststripMode)`
- `updateIncrementType(type: IncrementType)`
- `updateIncrementMs(ms: Long)`
Ensure each method has the guard: `if (session.state == TeststripState.EXPOSING) return`.

- [ ] **Step 3: Implement Start Guard**
Modify `startSession()` to check relay connection:
```kotlin
fun startSession() {
    if (!relaySystem.isConnected()) {
        _uiState.update { it.copy(errorMessage = "Relais déconnecté") }
        return
    }
    // ... existing logic
}
```
(Note: I will need to verify if `RelaySystem` has an `isConnected()` method or equivalent).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt
git commit -m "feat: add configuration state and connection guard to TeststripViewModel"
```

### Task 4: UI Implementation

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt`

- [ ] **Step 1: Create Segmented Control Component**
Implement a simple row of buttons to switch between two options (Mode and Increment Type).

- [ ] **Step 2: Update `ConfigurationSection`**
Replace current layout with:
1. Base Time (`DigitTimePicker`)
2. Patch Count (`[-] 6 [+]`)
3. Mode Selector (`Incremental` | `Separate`)
4. Type Selector (`f-stop` | `Seconds`)
5. Dynamic Value Input:
    - If f-stop: existing fraction buttons.
    - If Seconds: `DigitTimePicker` for increment.

- [ ] **Step 3: Connect UI to ViewModel**
Wire all new selectors to the new ViewModel methods.

- [ ] **Step 4: Update "DÉMARRER" Button State**
Set `enabled = state.sessionState == TeststripState.CONFIGURED && relayConnected`.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt
git commit -m "feat: implement flexible teststrip configuration UI"
```

### Task 5: Final Verification

- [ ] **Step 1: Manual test of all combinations**
Verify that the patch grid updates instantly when switching modes/types.
- [ ] **Step 2: Verify relay duration in both modes**
(Use a mock relay or log outputs).
- [ ] **Step 3: Verify "DÉMARRER" is disabled when disconnected**
- [ ] **Step 4: Final commit/cleanup**
```bash
git commit -m "test: verify teststrip enhancements"
```
