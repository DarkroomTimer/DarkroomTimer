# F-Stop Correction in CountDown Screen

**Date:** 2026-05-25  
**Status:** Approved

## Context

The CountDown screen lets the user set a base exposure time and start the timer. Currently, fine-tuning is limited to ±1m/±10s/±1s/±0.1s buttons. Darkroom printing frequently requires f-stop-based corrections (e.g. "+1/3 stop" or "-1 stop") to maintain perceptual linearity across different base times. This feature adds f-stop correction buttons directly to the CountDown screen so the user can apply, accumulate, reset, and commit corrections without leaving the screen.

## State Model

### `CountdownUiState` additions

```kotlin
val fStopCorrectionNumerator: Int = 0
val fStopCorrectionDenominator: Int = 1   // represents 0/1 = no correction
// configuredTimeMs remains the BASE time (semantics unchanged)
```

The correction is stored as a reduced fraction using the same `(numerator, denominator)` convention as `BurnDodgeEntry` and `TeststripEngine`. `FStopMath.simplify()` keeps the fraction reduced after each accumulation. Positive numerator = more exposure, negative = less.

Accumulation formula: `(n1/d1) + (n2/d2) = (n1×d2 + n2×d1) / (d1×d2)`, then `FStopMath.simplify()`.

### `CountdownViewModel` additions

```kotlin
private var baseTimeMs: Long   // mirrors configuredTimeMs; updated by base-time operations only
```

Derived helper (private):
```kotlin
private fun calculatedTimeMs(): Long =
    if (_uiState.value.fStopCorrectionNumerator == 0) baseTimeMs
    else FStopMath.adjustTime(
            baseTimeMs,
            _uiState.value.fStopCorrectionNumerator,
            _uiState.value.fStopCorrectionDenominator,
            1
        ).coerceIn(100L, 999_000L)
```

`FStopMath.adjustTime(base, n, d, 1)` computes `base × 2^(n/d)`, reusing the existing utility without modification.

## ViewModel Logic

### New methods

| Method | Behaviour |
|---|---|
| `applyFStopDelta(numerator: Int, denominator: Int)` | Only when STOPPED. Adds the fraction to the current correction using fraction addition + `FStopMath.simplify()`. Silently ignored if the resulting calculated time falls outside [100 ms, 999 000 ms]. Updates `displayTime` and `timer.configuredTimeMs`. |
| `resetFStopCorrection()` | Only when STOPPED. Sets correction to `0/1`. Restores `displayTime` and `timer.configuredTimeMs` to base. |
| `setFStopCorrectionAsBase()` | Only when STOPPED. Sets `baseTimeMs = calculatedTimeMs()`, correction to `0/1`, `configuredTimeMs = baseTimeMs`. The calculated time becomes the new base. |

### Modified methods

| Method | Change |
|---|---|
| `start()` | Sets `timer.configuredTimeMs = calculatedTimeMs()` before `timer.start()`. This ensures the timer and relay both use the corrected time. |
| `stop()` | Restores `timer.configuredTimeMs = baseTimeMs` after stopping. Sets `displayTime = formatTime(calculatedTimeMs())` so the display reflects the correction for the next run. |
| `adjustTime(deltaMs)` when STOPPED | Adjusts `baseTimeMs` (not `timer.configuredTimeMs` directly), then recomputes and updates display. Correction stays applied. |
| `adjustTime(deltaMs)` when PAUSED | Adjusts `timer.configuredTimeMs` directly (fine-tune remaining time during pause). Does **not** change `baseTimeMs` or `configuredTimeMs` in UiState. The paused adjustment is lost if the user stops and restarts — the timer returns to `calculatedTimeMs()` on the next `start()`. |
| `setTimeFromInput()` | Updates `baseTimeMs`, resets correction to `0/1`. A manual time entry is treated as a fresh base. |
| `init{}` | Initialises `baseTimeMs` from preferences alongside `timer.configuredTimeMs`. |

## UI

### New composable: `FStopCorrectionSection`

Displayed below the ±time adjustment buttons, only when `timerState == STOPPED`.

**Layout:**
```
─────── F-Stop ───────
[-1][-½][-⅓][-⅙]  [+⅙][+⅓][+½][+1]

// shown only when fStopCorrectionNumerator != 0:
Correction : +1 1/3 stop → 00:20.1
[    Reset    ]  [    Set    ]
```

**Button deltas `(numerator, denominator)`:**

| Button | Label | Delta |
|---|---|---|
| -1 stop | `-1` | (-1, 1) |
| -1/2 stop | `-½` | (-1, 2) |
| -1/3 stop | `-⅓` | (-1, 3) |
| -1/6 stop | `-⅙` | (-1, 6) |
| +1/6 stop | `+⅙` | (1, 6) |
| +1/3 stop | `+⅓` | (1, 3) |
| +1/2 stop | `+½` | (1, 2) |
| +1 stop | `+1` | (1, 1) |

**Correction label** (when `fStopCorrectionNumerator != 0`):
```
"${if (n > 0) "+" else ""}${FStopMath.formatStop(n, d)} stop → ${CountdownTimer.formatTime(calculatedTimeMs)}"
```

**Styling:** Buttons follow the same style as existing `TimeAdjustButton`. Reset and Set use an outlined secondary style.

### Timer display (`displayTime`)

When STOPPED: shows `formatTime(calculatedTimeMs())`. The large timer always reflects what the next countdown will be, including any active correction.

When RUNNING or PAUSED: unchanged (shows remaining time).

## Test Coverage

New unit tests in `CountdownViewModelTest`:

- `applyFStopDelta` accumulates correctly across multiple calls (e.g. +1/3 then +1/3 → +2/3, simplified)
- `applyFStopDelta` accumulates fractions with different denominators correctly (e.g. +1/2 then +1/3 → +5/6)
- `applyFStopDelta` is rejected when result < 100 ms
- `applyFStopDelta` is rejected when result > 999 000 ms
- `applyFStopDelta` does nothing when timer is not STOPPED
- `resetFStopCorrection` returns display to base time, correction is 0/1
- `setFStopCorrectionAsBase` sets base = calculated, clears correction to 0/1
- `start()` passes `calculatedTimeMs` to `timer.configuredTimeMs`
- `stop()` restores `timer.configuredTimeMs` to base
- `adjustTime()` when STOPPED adjusts base, correction stays applied, display updates
- `setTimeFromInput()` resets correction to 0/1

## Out of Scope

- Saving/restoring the active correction across app sessions
- Applying f-stop correction in Teststrip or Development modes
- Disabling buttons when correction would go out of range (silent rejection is sufficient)
