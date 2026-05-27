# Design Spec: Teststrip Feature Enhancements
Date: 2026-05-27
Status: Draft

## 1. Introduction
The Teststrip feature allows photographers to create a series of exposures on a photographic paper to determine the ideal exposure time. The current implementation is limited to a fixed number of patches (6) and only supports f-stop based exponential increments. This design extends the feature to support flexible patch counts, linear increments (seconds), and different exposure modes (Incremental vs Separate).

## 2. Requirements
- **Flexible Patch Count**: User can choose the number of patches (Default: 6, Range: 3-12).
- **Exposure Mode**: 
    - `INCREMENTAL`: Each patch adds to the previous exposure (Relay runs for the difference).
    - `SEPARATE`: Each patch is independent (Relay runs for the total time).
- **Increment Type**:
    - `F_STOP`: Exponential increments based on f-stop fractions (e.g., 1/3 stop).
    - `SECONDS`: Linear increments based on a fixed time value.
- **UI/UX**: Settings must be directly accessible and editable on the `TeststripScreen`.
- **Safety**: The "DÉMARRER" button must be disabled if the relay system is not connected.

## 3. Architecture

### 3.1 TeststripEngine (The Brain)
The `TeststripEngine` is the single source of truth for exposure calculations.

**Updated Properties:**
- `baseTimeMs`: Long (The duration of the first patch)
- `patchCount`: Int (Number of patches)
- `mode`: `TeststripMode` (`INCREMENTAL`, `SEPARATE`)
- `incrementType`: `IncrementType` (`F_STOP`, `SECONDS`)
- `incrementValue`: 
    - For `F_STOP`: `numerator` and `denominator` (fraction of a stop).
    - For `SECONDS`: `incrementMs` (time in milliseconds).

**Math Logic:**
1. **Absolute Time calculation ($T_n$):**
    - If `F_STOP`: $T_n = baseTimeMs \times 2^{(\frac{numerator}{denominator} \times n)}$
    - If `SECONDS`: $T_n = baseTimeMs + (incrementMs \times n)$
2. **Relay Duration calculation ($D_n$):**
    - If `mode == SEPARATE`: $D_n = T_n$
    - If `mode == INCREMENTAL`: $D_n = T_n - T_{n-1}$ (with $T_{-1} = 0$)

### 3.2 TeststripSession (The Coordinator)
The session manages the timing and state machine.

- **Timing**: Instead of calculating durations internally, it calls `engine.getRelayDuration(currentPatchIndex)`.
- **State Machine**: Remains largely the same (`CONFIGURED` $\rightarrow$ `EXPOSING` $\rightarrow$ `BETWEEN_PATCHES`).

### 3.3 TeststripViewModel (The State Manager)
- **UI State**: Expanded `TeststripUiState` to include all new configuration parameters.
- **Controls**: Provides methods to update engine parameters and triggers `updateUiState()` to refresh the UI in real-time.
- **Start Guard**: The `startSession()` method and the UI button will check `relaySystem.isConnected()` before allowing start.

## 4. UI/UX Design

### 4.1 Settings Panel (ConfigurationSection)
The settings will be grouped into three logical areas on the `TeststripScreen`:

**A. Basic Setup**
- **Base Time**: `DigitTimePicker` (existing).
- **Patch Count**: Increment/Decrement selector `[-] 6 [+]`.

**B. Mode & Strategy**
- **Exposure Mode**: Segmented control: `[ Incremental | Separate ]`.
- **Increment Type**: Segmented control: `[ f-stop | Seconds ]`.

**C. Value Configuration (Dynamic)**
- **If f-stop**: Show `+1/n` and `-1/n` buttons for stop fractions.
- **If Seconds**: Show `DigitTimePicker` for the increment duration.

### 4.2 Interaction & Feedback
- **Real-time Grid**: The `LazyVerticalGrid` of patches will update its displayed times instantly as any setting is changed.
- **Locking**: Settings are disabled while `state == EXPOSING`.
- **Connection Status**: "DÉMARRER" button disabled if relay is not connected.

## 5. Verification Plan
- **Unit Tests**: 
    - Verify $T_n$ for both Linear and Exponential types.
    - Verify $D_n$ for both Separate and Incremental modes.
- **UI Test**: 
    - Verify that changing "Seconds" mode switches the input from fractions to a time picker.
    - Verify that the relay duration changes correctly when switching between Incremental and Separate.
- **Integration Test**: 
    - Ensure the "DÉMARRER" button is disabled when WiFi is off.
