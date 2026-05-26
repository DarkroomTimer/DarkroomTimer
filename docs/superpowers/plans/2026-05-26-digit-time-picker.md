# DigitTimePicker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer le sélecteur de temps (BottomSheet + boutons ±) par un afficheur 7-segments interactif réutilisable sur CountdownScreen, TeststripScreen, et DevelopmentProfileEditorScreen.

**Architecture:** Deux nouveaux composables — `SegmentDisplay` (Canvas, un digit) et `DigitTimePicker` (assemblage de digits + gestes). La logique de conversion ms↔digits et d'application de delta est extraite en fonctions `internal` pures testables. Les ViewModels touchés reçoivent `setBaseTime`/`setRemainingTime` en remplacement de `setTimeFromInput`.

**Tech Stack:** Jetpack Compose Canvas, `pointerInput` / `awaitEachGesture`, Kotlin unit tests (JUnit4 + AssertJ), Gradle `./gradlew test`

---

## Fichiers

| Fichier | Statut |
|---------|--------|
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/SegmentDisplay.kt` | Créer |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt` | Créer |
| `app/src/test/java/fr/mathgl/darkroomtimer/ui/DigitTimePickerLogicTest.kt` | Créer |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt` | Modifier |
| `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt` | Modifier |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt` | Modifier |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt` | Modifier |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt` | Modifier |

---

## Task 1 — Logique pure : `DigitTimeFormat`, `msToDigits`, `applyDelta`

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt`
- Create: `app/src/test/java/fr/mathgl/darkroomtimer/ui/DigitTimePickerLogicTest.kt`

- [ ] **Step 1 : Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/ui/DigitTimePickerLogicTest.kt
package fr.mathgl.darkroomtimer.ui

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DigitTimePickerLogicTest {

    // ── msToDigits ───────────────────────────────────────────────────────────

    @Test
    fun `msToDigits MM_SS_T for 8 seconds`() {
        assertArrayEquals(intArrayOf(0, 0, 0, 8, 0), msToDigits(8_000L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits MM_SS_T for 1min 23s 4 tenths`() {
        // 1*60000 + 23*1000 + 4*100 = 83400
        assertArrayEquals(intArrayOf(0, 1, 2, 3, 4), msToDigits(83_400L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits MM_SS_T for max value 999000`() {
        // 999s = 16min 39s
        assertArrayEquals(intArrayOf(1, 6, 3, 9, 0), msToDigits(999_000L, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `msToDigits HH_MM_SS for 1h 30min 45s`() {
        val ms = (1 * 3600 + 30 * 60 + 45) * 1000L // = 5_445_000
        assertArrayEquals(intArrayOf(0, 1, 3, 0, 4, 5), msToDigits(ms, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `msToDigits HH_MM_SS for 99h 59min 59s`() {
        assertArrayEquals(intArrayOf(9, 9, 5, 9, 5, 9), msToDigits(359_999_000L, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    // ── applyDelta MM:SS.T ───────────────────────────────────────────────────

    @Test
    fun `applyDelta T carry into S2 — 00_09_9 + T becomes 00_10_0`() {
        // 9.9s = 9900ms, increment tenths (index 4, weight 100ms)
        assertEquals(10_000L, applyDelta(9_900L, 4, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta S2 carry into S1 — 00_59_0 + S2 becomes 01_00_0`() {
        // 59s = 59000ms, increment S2 (index 3, weight 1000ms)
        assertEquals(60_000L, applyDelta(59_000L, 3, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta S1 carry into M — 00_50_0 + S1 becomes 01_00_0`() {
        // 50s = 50000ms, increment S1 (index 2, weight 10000ms)
        assertEquals(60_000L, applyDelta(50_000L, 2, +1, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta clamp to min 100ms`() {
        assertEquals(100L, applyDelta(200L, 4, -2, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    @Test
    fun `applyDelta clamp to max 999000ms`() {
        assertEquals(999_000L, applyDelta(998_900L, 4, +2, DigitTimeFormat.MINUTES_SECONDS_TENTHS))
    }

    // ── applyDelta HH:MM:SS ──────────────────────────────────────────────────

    @Test
    fun `applyDelta HH_MM_SS S2 carry — 00_00_59 + S2 becomes 00_01_00`() {
        assertEquals(60_000L, applyDelta(59_000L, 5, +1, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS M carry into H — 00_59_00 + M2 becomes 01_00_00`() {
        val ms = 59 * 60 * 1000L // 59 minutes
        assertEquals(3_600_000L, applyDelta(ms, 3, +1, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS clamp to max 359999000`() {
        assertEquals(359_999_000L, applyDelta(359_998_000L, 5, +2, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }

    @Test
    fun `applyDelta HH_MM_SS clamp to min 1000`() {
        assertEquals(1_000L, applyDelta(2_000L, 5, -2, DigitTimeFormat.HOURS_MINUTES_SECONDS))
    }
}
```

- [ ] **Step 2 : Lancer les tests — vérifier qu'ils échouent**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.DigitTimePickerLogicTest"
```

Attendu : erreur de compilation (`DigitTimeFormat`, `msToDigits`, `applyDelta` non définis).

- [ ] **Step 3 : Créer `DigitTimePicker.kt` avec les fonctions logiques**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt
package fr.mathgl.darkroomtimer.ui

enum class DigitTimeFormat {
    MINUTES_SECONDS_TENTHS,  // MM:SS.T — countdown, teststrip
    HOURS_MINUTES_SECONDS    // HH:MM:SS — développement
}

internal fun msToDigits(ms: Long, format: DigitTimeFormat): IntArray = when (format) {
    DigitTimeFormat.MINUTES_SECONDS_TENTHS -> {
        val t = (ms / 100 % 10).toInt()
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val m = (totalS / 60).toInt()
        intArrayOf(m / 10, m % 10, s / 10, s % 10, t)
    }
    DigitTimeFormat.HOURS_MINUTES_SECONDS -> {
        val totalS = ms / 1000
        val s = (totalS % 60).toInt()
        val totalM = totalS / 60
        val m = (totalM % 60).toInt()
        val h = (totalM / 60).toInt()
        intArrayOf(h / 10, h % 10, m / 10, m % 10, s / 10, s % 10)
    }
}

internal fun applyDelta(ms: Long, digitIndex: Int, delta: Int, format: DigitTimeFormat): Long {
    val (min, max) = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS -> 100L to 999_000L
        DigitTimeFormat.HOURS_MINUTES_SECONDS  -> 1_000L to 359_999_000L
    }
    val weight = when (format) {
        DigitTimeFormat.MINUTES_SECONDS_TENTHS ->
            longArrayOf(600_000L, 60_000L, 10_000L, 1_000L, 100L)[digitIndex]
        DigitTimeFormat.HOURS_MINUTES_SECONDS  ->
            longArrayOf(36_000_000L, 3_600_000L, 600_000L, 60_000L, 10_000L, 1_000L)[digitIndex]
    }
    return (ms + delta * weight).coerceIn(min, max)
}
```

- [ ] **Step 4 : Lancer les tests — vérifier qu'ils passent**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.DigitTimePickerLogicTest"
```

Attendu : BUILD SUCCESSFUL, 14 tests passent.

- [ ] **Step 5 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/ui/DigitTimePickerLogicTest.kt
git commit -m "feat: add DigitTimeFormat enum and pure logic functions (msToDigits, applyDelta)"
```

---

## Task 2 — `SegmentDisplay` Canvas composable

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/SegmentDisplay.kt`

- [ ] **Step 1 : Créer `SegmentDisplay.kt`**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/SegmentDisplay.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint

// Bits: 0=a(top) 1=b(top-right) 2=c(bot-right) 3=d(bot) 4=e(bot-left) 5=f(top-left) 6=g(mid)
private val SEGMENT_PATTERNS = intArrayOf(
    0b0111111, // 0
    0b0000110, // 1
    0b1011011, // 2
    0b1001111, // 3
    0b1100110, // 4
    0b1101101, // 5
    0b1111101, // 6
    0b0000111, // 7
    0b1111111, // 8
    0b1101111, // 9
)

@Composable
fun SegmentDisplay(
    digit: Int,
    segOnColor: Color = DarkroomRedBright,
    segOffColor: Color = DarkroomRedFaint,
    modifier: Modifier = Modifier
) {
    val pattern = SEGMENT_PATTERNS[digit.coerceIn(0, 9)]
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = w * 0.14f
        val g = w * 0.04f
        val cr = CornerRadius(t * 0.3f)

        fun seg(bitIndex: Int, left: Float, top: Float, width: Float, height: Float) {
            drawRoundRect(
                color = if ((pattern shr bitIndex) and 1 == 1) segOnColor else segOffColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                cornerRadius = cr
            )
        }

        val hw = w - 2 * (t + g)  // horizontal segment width
        val vh = h / 2f - t - 2 * g  // vertical segment half-height

        seg(0, t + g,       0f,        hw, t)   // a top
        seg(1, w - t,       t + g,     t,  vh)  // b top-right
        seg(2, w - t,       h/2f + g,  t,  vh)  // c bot-right
        seg(3, t + g,       h - t,     hw, t)   // d bottom
        seg(4, 0f,          h/2f + g,  t,  vh)  // e bot-left
        seg(5, 0f,          t + g,     t,  vh)  // f top-left
        seg(6, t + g,       h/2f - t/2f, hw, t) // g middle
    }
}
```

- [ ] **Step 2 : Vérifier la compilation**

```bash
./gradlew assembleDebug
```

Attendu : BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/SegmentDisplay.kt
git commit -m "feat: add SegmentDisplay Canvas composable (7-segment digit renderer)"
```

---

## Task 3 — `DigitTimePicker` composable (interaction)

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt`

- [ ] **Step 1 : Ajouter les imports et les composables à `DigitTimePicker.kt`**

Ajouter ces imports en haut du fichier (après `package`):

```kotlin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedFaint
import kotlinx.coroutines.CancellationException
import kotlin.math.abs
```

Puis ajouter à la fin du fichier :

```kotlin
@Composable
fun DigitTimePicker(
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    enabled: Boolean = true,
    format: DigitTimeFormat = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
    digitHeight: Dp = 80.dp
) {
    val digits = msToDigits(valueMs, format)
    Row(verticalAlignment = Alignment.CenterVertically) {
        digits.forEachIndexed { i, digit ->
            SingleDigitPicker(
                digit = digit,
                enabled = enabled,
                onIncrement = { onValueChange(applyDelta(valueMs, i, +1, format)) },
                onDecrement = { onValueChange(applyDelta(valueMs, i, -1, format)) },
                digitHeight = digitHeight
            )
            val sep: Char? = when (format) {
                DigitTimeFormat.MINUTES_SECONDS_TENTHS -> when (i) { 1 -> ':'; 3 -> '.'; else -> null }
                DigitTimeFormat.HOURS_MINUTES_SECONDS  -> when (i) { 1 -> ':'; 3 -> ':'; else -> null }
            }
            if (sep != null) {
                Text(
                    text = sep.toString(),
                    fontSize = (digitHeight.value * 0.6f).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DarkroomRedBright.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SingleDigitPicker(
    digit: Int,
    enabled: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    digitHeight: Dp,
    segOnColor: Color = DarkroomRedBright,
    segOffColor: Color = DarkroomRedFaint
) {
    val digitWidth = digitHeight / 2
    val arrowZoneHeight = digitHeight * 0.35f
    var activeZone by remember { mutableStateOf(0) } // 0=none, 1=increment, -1=decrement
    val currentOnIncrement by rememberUpdatedState(onIncrement)
    val currentOnDecrement by rememberUpdatedState(onDecrement)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(digitWidth)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val swipeThresholdPx = 40.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    activeZone = if (down.position.y < size.height / 2f) 1 else -1
                    var swipeAcc = 0f
                    var isDragging = false
                    val slop = viewConfiguration.touchSlop
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                if (!isDragging) {
                                    if (down.position.y < size.height / 2f) currentOnIncrement()
                                    else currentOnDecrement()
                                }
                                activeZone = 0
                                break
                            }
                            val dy = change.position.y - change.previousPosition.y
                            swipeAcc += dy
                            if (!isDragging && abs(swipeAcc) > slop) isDragging = true
                            if (isDragging) {
                                while (swipeAcc <= -swipeThresholdPx) {
                                    currentOnIncrement()
                                    swipeAcc += swipeThresholdPx
                                }
                                while (swipeAcc >= swipeThresholdPx) {
                                    currentOnDecrement()
                                    swipeAcc -= swipeThresholdPx
                                }
                            }
                            change.consume()
                        }
                    } catch (e: CancellationException) {
                        activeZone = 0
                        throw e
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier.width(digitWidth).height(arrowZoneHeight),
            contentAlignment = Alignment.Center
        ) {
            if (activeZone > 0) {
                Text("▲", color = segOnColor.copy(alpha = 0.55f), fontSize = (arrowZoneHeight.value * 0.55f).sp)
            }
        }

        Box(modifier = Modifier.width(digitWidth).height(digitHeight)) {
            SegmentDisplay(
                digit = digit,
                segOnColor = segOnColor,
                segOffColor = segOffColor,
                modifier = Modifier.fillMaxSize()
            )
            if (activeZone != 0) {
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 2.dp),
                    color = segOnColor.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
            }
        }

        Box(
            modifier = Modifier.width(digitWidth).height(arrowZoneHeight),
            contentAlignment = Alignment.Center
        ) {
            if (activeZone < 0) {
                Text("▼", color = segOnColor.copy(alpha = 0.4f), fontSize = (arrowZoneHeight.value * 0.55f).sp)
            }
        }
    }
}
```

- [ ] **Step 2 : Vérifier la compilation**

```bash
./gradlew assembleDebug
```

Attendu : BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DigitTimePicker.kt
git commit -m "feat: add DigitTimePicker and SingleDigitPicker composables with tap/swipe gestures"
```

---

## Task 4 — Modifier `CountdownViewModel` + tests

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt`

- [ ] **Step 1 : Écrire les tests qui échouent**

Ajouter à la fin de `CountdownViewModelTest.kt` (dans la classe `CountdownViewModelTest`) :

```kotlin
// ── displayTimeMs ────────────────────────────────────────────────────────────

@Test
fun `displayTimeMs matches configuredTimeMs at init`() {
    assertEquals(viewModel.uiState.value.configuredTimeMs, viewModel.uiState.value.displayTimeMs)
}

// ── setBaseTime ──────────────────────────────────────────────────────────────

@Test
fun `setBaseTime in STOPPED updates displayTimeMs and resets fstop correction`() {
    viewModel.applyFStopDelta(1, 2)
    assertEquals(1, viewModel.uiState.value.fStopCorrectionNumerator)

    viewModel.setBaseTime(10_000L)

    val state = viewModel.uiState.value
    assertEquals(10_000L, state.displayTimeMs)
    assertEquals(0, state.fStopCorrectionNumerator)
}

@Test
fun `setBaseTime clamps to 100ms minimum`() {
    viewModel.setBaseTime(50L)
    assertEquals(100L, viewModel.uiState.value.displayTimeMs)
}

@Test
fun `setBaseTime in PAUSED is no-op`() = runTest {
    viewModel.start()
    viewModel.pause()
    val before = viewModel.uiState.value.displayTimeMs

    viewModel.setBaseTime(5_000L)

    assertEquals(before, viewModel.uiState.value.displayTimeMs)
}

@Test
fun `setBaseTime in RUNNING is no-op`() = runTest {
    viewModel.start()
    val before = viewModel.uiState.value.displayTimeMs

    viewModel.setBaseTime(5_000L)

    assertEquals(before, viewModel.uiState.value.displayTimeMs)
}

// ── setRemainingTime ─────────────────────────────────────────────────────────

@Test
fun `setRemainingTime in PAUSED updates displayTimeMs`() = runTest {
    viewModel.start()
    viewModel.pause()

    viewModel.setRemainingTime(3_000L)

    assertEquals(3_000L, viewModel.uiState.value.displayTimeMs)
}

@Test
fun `setRemainingTime in STOPPED is no-op`() {
    val before = viewModel.uiState.value.displayTimeMs
    viewModel.setRemainingTime(3_000L)
    assertEquals(before, viewModel.uiState.value.displayTimeMs)
}

@Test
fun `setRemainingTime in RUNNING is no-op`() = runTest {
    viewModel.start()
    val before = viewModel.uiState.value.displayTimeMs

    viewModel.setRemainingTime(3_000L)

    assertEquals(before, viewModel.uiState.value.displayTimeMs)
}
```

- [ ] **Step 2 : Lancer les tests — vérifier qu'ils échouent**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest"
```

Attendu : erreur de compilation (`displayTimeMs`, `setBaseTime`, `setRemainingTime` non définis).

- [ ] **Step 3 : Modifier `CountdownUiState`**

Dans `CountdownViewModel.kt`, modifier `CountdownUiState` :

```kotlin
data class CountdownUiState(
    val displayTime: String,
    val displayTimeMs: Long,           // ← NOUVEAU
    val timerState: TimerState,
    val relayState: RelayStates,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long,
    val burnDodgeEntries: List<BurnDodgeEntry>,
    val burnDodgeVisible: Boolean,
    val maxEntriesReached: Boolean,
    // showTimeEditor supprimé
    val enlargerOverride: Boolean = false,
    val safelightOverride: Boolean = false,
    val relayType: String = "NULL",
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
    val fStopCorrectionNumerator: Int = 0,
    val fStopCorrectionDenominator: Int = 1
)
```

- [ ] **Step 4 : Corriger l'initialisation de `_uiState`**

```kotlin
private val _uiState = MutableStateFlow(
    CountdownUiState(
        displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
        displayTimeMs = timer.configuredTimeMs,    // ← NOUVEAU
        timerState = TimerState.STOPPED,
        relayState = RelayStates.INITIAL,
        selectedGrade = ContrastGrade.DEFAULT,
        configuredTimeMs = timer.configuredTimeMs,
        burnDodgeEntries = emptyList(),
        burnDodgeVisible = false,
        maxEntriesReached = false
    )
)
```

- [ ] **Step 5 : Ajouter `displayTimeMs` à chaque `.copy()` qui met à jour `displayTime`**

Dans `applyFStopDelta` — dans le `.copy()` existant, ajouter :
```kotlin
displayTimeMs = calc
```
(où `calc` est déjà calculé par `FStopMath.adjustTime(baseTimeMs, ...)`)

Dans `resetFStopCorrection` — dans le `.copy()` existant, ajouter :
```kotlin
displayTimeMs = baseTimeMs
```

Dans `setFStopCorrectionAsBase` — dans le `.copy()` existant, ajouter :
```kotlin
displayTimeMs = calc
```
(où `calc = calculatedTimeMs()`)

Dans `pause()` :
```kotlin
_uiState.update { it.copy(
    timerState = TimerState.PAUSED,
    displayTime = CountdownTimer.formatTime(timer.remainingMs()),
    displayTimeMs = timer.remainingMs()
)}
```

Dans `stop()` :
```kotlin
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(calculatedTimeMs()),
    displayTimeMs = calculatedTimeMs(),
    timerState = TimerState.STOPPED,
    enlargerOverride = false,
    safelightOverride = false
)}
```

Dans `launchTickJob` — tick loop :
```kotlin
_uiState.update { it.copy(
    displayTime = if (ended) CountdownTimer.formatTime(calculatedTimeMs())
                 else CountdownTimer.formatTime(remaining),
    displayTimeMs = if (ended) calculatedTimeMs() else remaining,
    timerState = timer.state,
    enlargerOverride = if (ended) false else it.enlargerOverride,
    safelightOverride = if (ended) false else it.safelightOverride
)}
```

Dans `adjustTime` — cas STOPPED :
```kotlin
val calc = calculatedTimeMs()
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(calc),
    displayTimeMs = calc,
    configuredTimeMs = newBase
)}
```

Dans `adjustTime` — cas PAUSED :
```kotlin
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(timer.remainingMs()),
    displayTimeMs = timer.remainingMs()
)}
```

Dans le bloc `init` (chargement des préférences) :
```kotlin
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
    displayTimeMs = timer.configuredTimeMs,
    configuredTimeMs = timer.configuredTimeMs,
    selectedGrade = prefs.defaultContrastGrade
)}
```

- [ ] **Step 6 : Supprimer `showTimeEditor`, `openTimeEditor`, `closeTimeEditor`, `setTimeFromInput`**

Supprimer dans `CountdownViewModel.kt` :
- La fonction `openTimeEditor()`
- La fonction `closeTimeEditor()`
- La fonction `setTimeFromInput(minutes: Int, seconds: Int, tenths: Int)`

(Le champ `showTimeEditor` a déjà été supprimé de l'UiState à l'étape 3.)

- [ ] **Step 7 : Ajouter `setBaseTime` et `setRemainingTime`**

```kotlin
fun setBaseTime(ms: Long) {
    if (timer.state != TimerState.STOPPED) return
    val clamped = ms.coerceIn(100L, 999_000L)
    baseTimeMs = clamped
    timer.configuredTimeMs = clamped
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(clamped),
        displayTimeMs = clamped,
        configuredTimeMs = clamped,
        fStopCorrectionNumerator = 0,
        fStopCorrectionDenominator = 1
    )}
}

fun setRemainingTime(ms: Long) {
    if (timer.state != TimerState.PAUSED) return
    val clamped = ms.coerceIn(100L, 999_000L)
    val delta = clamped - timer.remainingMs()
    timer.configuredTimeMs = (timer.configuredTimeMs + delta).coerceIn(100L, 999_000L)
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(timer.remainingMs()),
        displayTimeMs = timer.remainingMs()
    )}
}
```

- [ ] **Step 8 : Supprimer les tests `setTimeFromInput` obsolètes**

Dans `CountdownViewModelTest.kt`, supprimer tout test qui appelle `viewModel.setTimeFromInput(...)`, `viewModel.openTimeEditor()`, ou `viewModel.closeTimeEditor()`, et tout test qui lit `state.showTimeEditor`.

- [ ] **Step 9 : Lancer tous les tests ViewModel — vérifier qu'ils passent**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.ui.CountdownViewModelTest"
```

Attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 10 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/ui/CountdownViewModelTest.kt
git commit -m "feat: add setBaseTime/setRemainingTime/displayTimeMs to CountdownViewModel, remove TimeEditor API"
```

---

## Task 5 — Intégrer `DigitTimePicker` dans `CountdownScreen`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

- [ ] **Step 1 : Remplacer le `Text` cliquable par `DigitTimePicker`**

Supprimer ce bloc (lignes ~69-79) :
```kotlin
// Timer display
Text(
    text = state.displayTime,
    fontSize = 80.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = DarkroomRedBright,
    modifier = Modifier.clickable(
        enabled = state.timerState != TimerState.RUNNING
    ) { viewModel.openTimeEditor() }
)
```

Le remplacer par :
```kotlin
// Timer display
DigitTimePicker(
    valueMs = state.displayTimeMs,
    onValueChange = { newMs ->
        if (state.timerState == TimerState.PAUSED) viewModel.setRemainingTime(newMs)
        else viewModel.setBaseTime(newMs)
    },
    enabled = state.timerState != TimerState.RUNNING,
    format = DigitTimeFormat.MINUTES_SECONDS_TENTHS
)
```

- [ ] **Step 2 : Supprimer le bloc `TimeAdjustRow` conditionnel**

Supprimer ce bloc (lignes ~127-130) :
```kotlin
// Time adjustment (only when not RUNNING)
if (state.timerState != TimerState.RUNNING) {
    TimeAdjustRow(onAdjust = { viewModel.adjustTime(it) })
    Spacer(modifier = Modifier.height(24.dp))
}
```

- [ ] **Step 3 : Supprimer le bloc `TimeEditorSheet` conditionnel**

Supprimer à la fin de la fonction `CountdownScreen` (lignes ~165-171) :
```kotlin
if (state.showTimeEditor) {
    TimeEditorSheet(
        currentMs = state.configuredTimeMs,
        onConfirm = { m, s, t -> viewModel.setTimeFromInput(m, s, t) },
        onDismiss = { viewModel.closeTimeEditor() }
    )
}
```

- [ ] **Step 4 : Supprimer les fonctions privées obsolètes**

Supprimer du fichier les quatre fonctions composable privées :
- `TimeAdjustRow` (lignes ~231-245)
- `TimeAdjustButton` (lignes ~247-258)
- `TimeEditorSheet` (lignes ~260-319)
- `TimeSpinner` (lignes ~321-338)

- [ ] **Step 5 : Nettoyer les imports devenus inutiles**

Supprimer les imports qui ne sont plus utilisés :
```kotlin
import androidx.compose.foundation.clickable          // si plus utilisé
import androidx.compose.material3.ModalBottomSheet     // supprimé avec TimeEditorSheet
// @OptIn(ExperimentalMaterial3Api::class) — supprimer l'annotation si elle est sur TimeEditorSheet
```

- [ ] **Step 6 : Vérifier la compilation**

```bash
./gradlew assembleDebug
```

Attendu : BUILD SUCCESSFUL.

- [ ] **Step 7 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt
git commit -m "feat: replace timer Text+BottomSheet+AdjustRow with DigitTimePicker in CountdownScreen"
```

---

## Task 6 — Intégrer `DigitTimePicker` dans `TeststripScreen`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt`

- [ ] **Step 1 : Remplacer le sélecteur de temps de base dans `ConfigurationSection`**

Dans `ConfigurationSection`, supprimer ce bloc (~lignes 243-273) :
```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text("Base:", fontSize = 14.sp, color = DarkroomRedDim)
    Text("${baseTimeMs / 1000.0}s", fontSize = 16.sp, color = DarkroomRedBright, fontFamily = FontFamily.Monospace)
}

Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
    OutlinedButton(
        onClick = { onBaseTimeChange((baseTimeMs - 1000).coerceIn(100L, 999_000L)) },
        ...
    ) { Text("-1s") }
    OutlinedButton(
        onClick = { onBaseTimeChange((baseTimeMs + 1000).coerceIn(100L, 999_000L)) },
        ...
    ) { Text("+1s") }
}
```

Le remplacer par :
```kotlin
Text("Base:", fontSize = 14.sp, color = DarkroomRedDim)
Spacer(modifier = Modifier.height(4.dp))
DigitTimePicker(
    valueMs = baseTimeMs,
    onValueChange = onBaseTimeChange,
    format = DigitTimeFormat.MINUTES_SECONDS_TENTHS,
    digitHeight = 52.dp
)
```

- [ ] **Step 2 : Vérifier la compilation**

```bash
./gradlew assembleDebug
```

Attendu : BUILD SUCCESSFUL.

- [ ] **Step 3 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt
git commit -m "feat: replace base time selector in TeststripScreen with DigitTimePicker"
```

---

## Task 7 — Intégrer `DigitTimePicker` dans `DevelopmentProfileEditorScreen`

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt`

- [ ] **Step 1 : Remplacer l'état `durationSeconds` par `durationMs` dans `StepEditorDialog`**

Dans `StepEditorDialog`, remplacer :
```kotlin
var durationSeconds by remember { mutableStateOf(step?.durationSeconds?.toString() ?: "") }
```
par :
```kotlin
var durationMs by remember { mutableStateOf((step?.durationSeconds ?: 60) * 1000L) }
```

- [ ] **Step 2 : Remplacer l'`OutlinedTextField` de durée par `DigitTimePicker`**

Supprimer le bloc `OutlinedTextField` pour la durée (~lignes 367-381) :
```kotlin
OutlinedTextField(
    value = durationSeconds,
    onValueChange = { durationSeconds = it },
    label = { Text("Durée (secondes)", color = DarkroomRedBright) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    colors = OutlinedTextFieldDefaults.colors(...)
)
```

Le remplacer par :
```kotlin
Text("Durée", color = DarkroomRedBright, fontSize = 14.sp)
Spacer(modifier = Modifier.height(4.dp))
DigitTimePicker(
    valueMs = durationMs,
    onValueChange = { durationMs = it },
    format = DigitTimeFormat.HOURS_MINUTES_SECONDS,
    digitHeight = 48.dp
)
```

- [ ] **Step 3 : Mettre à jour le handler de sauvegarde**

Dans le bloc `confirmButton` → `onClick`, changer :
```kotlin
durationSeconds = durationSeconds.toIntOrNull() ?: 60,
```
par :
```kotlin
durationSeconds = (durationMs / 1000).toInt(),
```

(Même chose dans les deux branches du `if (stepType == 0)` et `else`.)

- [ ] **Step 4 : Vérifier la compilation**

```bash
./gradlew assembleDebug
```

Attendu : BUILD SUCCESSFUL.

- [ ] **Step 5 : Lancer tous les tests**

```bash
./gradlew test
```

Attendu : BUILD SUCCESSFUL, aucun test en échec.

- [ ] **Step 6 : Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt
git commit -m "feat: replace duration text field in StepEditorDialog with DigitTimePicker (HH:MM:SS)"
```

---

## Vérification finale

```bash
./gradlew test && ./gradlew assembleDebug
```

Tous les tests passent, l'APK compile.
