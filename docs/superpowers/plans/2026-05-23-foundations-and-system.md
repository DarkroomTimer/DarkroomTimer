# Foundations & System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the core mathematical engine, data persistence layer, and ambient light screen management.

**Architecture:** 
- **FStopMath**: A stateless utility for exponential exposure calculations.
- **Storage**: A hybrid approach using SharedPreferences for simple settings and Room for structured enlarger profiles.
- **Luminosity**: A sensor-driven service that overrides window brightness based on a smoothed lux input.

**Tech Stack:** Kotlin, Room DB, Android Sensor API, Android WindowManager.

---

## File Mapping

### 1. FStop Math
- Create: `app/src/main/java/com/darkroomtimer/math/FStopMath.kt`
- Test: `app/src/test/java/com/darkroomtimer/math/FStopMathTest.kt`

### 2. Data Storage
- Create: `app/src/main/java/com/darkroomtimer/storage/PreferenceManager.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/room/AppDatabase.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/room/EnlargerProfileEntity.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/room/EnlargerProfileDao.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/StorageService.kt`
- Test: `app/src/test/java/com/darkroomtimer/storage/StorageServiceTest.kt`

### 3. Screen Luminosity
- Create: `app/src/main/java/com/darkroomtimer/system/LuminosityManager.kt`
- Test: `app/src/test/java/com/darkroomtimer/system/LuminosityManagerTest.kt`

---

## Implementation Tasks

### Task 1: FStop Math Engine

**Files:**
- Create: `app/src/main/java/com/darkroomtimer/math/FStopMath.kt`
- Test: `app/src/test/java/com/darkroomtimer/math/FStopMathTest.kt`

- [ ] **Step 1: Write tests for `adjustTime`**
Verify the validation table from `01-fstop-math.md` (base=8000ms, stop=1/3).

```kotlin
@Test
fun testAdjustTime() {
    assertEquals(8000L, FStopMath.adjustTime(8000L, 1, 3, 0))
    assertEquals(10079L, FStopMath.adjustTime(8000L, 1, 3, 1))
    assertEquals(12699L, FStopMath.adjustTime(8000L, 1, 3, 2))
    assertEquals(16000L, FStopMath.adjustTime(8000L, 1, 3, 3))
    assertEquals(6349L, FStopMath.adjustTime(8000L, 1, 3, -1))
    assertEquals(4000L, FStopMath.adjustTime(8000L, 1, 3, -3))
}
```

- [ ] **Step 2: Run tests to verify they fail**
Run: `./gradlew testDebugUnitTest --tests "com.darkroomtimer.math.FStopMathTest"`

- [ ] **Step 3: Implement `adjustTime`, `simplify`, and `gcd`**
Ensure denominator 0 protection and result $\ge 0$.

- [ ] **Step 4: Write tests for `formatStop`**
Verify notation: `0`, `1`, `1/3`, `1 1/3`, `-2/3`, `-2 1/3`.

- [ ] **Step 5: Implement `formatStop`**

- [ ] **Step 6: Run all math tests to verify they pass**

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/darkroomtimer/math/FStopMath.kt app/src/test/java/com/darkroomtimer/math/FStopMathTest.kt
git commit -S -m "feat: implement FStop exponential math engine"
```

### Task 2: Preference Storage (SharedPreferences)

**Files:**
- Create: `app/src/main/java/com/darkroomtimer/storage/PreferenceManager.kt`

- [ ] **Step 1: Implement `PreferenceManager` for scalar values**
Define keys and default values for:
- Metronome (enabled, cadence)
- Exposure (default_ms, default_grade, stop_num, stop_denom)
- Teststrip (mode, patch_count, base_ms, stop_num, stop_denom)
- Audio (volume, start_beep)

- [ ] **Step 2: Implement getter/setter methods**
Ensure types match `10-data-storage.md`.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/darkroomtimer/storage/PreferenceManager.kt
git commit -S -m "feat: implement SharedPreferences manager for basic settings"
```

### Task 3: Enlarger Profile Storage (Room)

**Files:**
- Create: `app/src/main/java/com/darkroomtimer/storage/room/EnlargerProfileEntity.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/room/EnlargerProfileDao.kt`
- Create: `app/src/main/java/com/darkroomtimer/storage/room/AppDatabase.kt`

- [ ] **Step 1: Define `EnlargerProfileEntity`**
Include all timing fields (turnOnDelayMs, riseTimeMs, etc.).

- [ ] **Step 2: Define `EnlargerProfileDao`**
Implement `insert`, `update`, `delete`, `getById`, `getAll`.

- [ ] **Step 3: Implement `AppDatabase` with Seed**
Create a `RoomDatabase.Callback` to insert the "Idéal" profile (id=0) on first creation.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/darkroomtimer/storage/room/
git commit -S -m "feat: implement Room DB for enlarger profiles with seed"
```

### Task 4: Storage Service & JSON I/O

**Files:**
- Create: `app/src/main/java/com/darkroomtimer/storage/StorageService.kt`
- Test: `app/src/test/java/com/darkroomtimer/storage/StorageServiceTest.kt`

- [ ] **Step 1: Implement `StorageService` facade**
Combine `PreferenceManager` and `EnlargerProfileDao` into a single service.

- [ ] **Step 2: Implement JSON Export**
Serialize preferences and profiles into the JSON format defined in `10-data-storage.md`.

- [ ] **Step 3: Implement JSON Import with Validation**
Verify version, profile IDs (0-15), and timing constraints.

- [ ] **Step 4: Write and run tests for Import/Export**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/darkroomtimer/storage/ StorageService.kt
git commit -S -m "feat: implement storage service with JSON import/export"
```

### Task 5: Screen Luminosity Management

**Files:**
- Create: `app/src/main/java/com/darkroomtimer/system/LuminosityManager.kt`
- Test: `app/src/test/java/com/darkroomtimer/system/LuminosityManagerTest.kt`

- [ ] **Step 1: Implement Smoothing Filter**
Create a moving average filter for Lux values (3-second window).

- [ ] **Step 2: Implement Luminosity Mapping**
Implement the logic for Adaptive (clamp(f(lux), min, max)) vs Fixed modes.

- [ ] **Step 3: Implement Window Brightness Override**
Use `WindowManager.LayoutParams.screenBrightness`.

- [ ] **Step 4: Implement Sensor Integration**
Register `Sensor.TYPE_LIGHT` listener and link it to the mapping logic.

- [ ] **Step 5: Write and run tests for mapping and smoothing**

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/darkroomtimer/system/LuminosityManager.kt
git commit -S -m "feat: implement ambient light screen brightness management"
```
