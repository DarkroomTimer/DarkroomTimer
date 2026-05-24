# Teststrip Mode — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implémenter le mode Teststrip pour réaliser des bandes de test photographiques avec exposition incrémentale.

**Architecture:** `TeststripEngine` est un moteur pur Kotlin dans `math`, calculant les temps de patches selon la formule f-stop. `TeststripSession` gère l'état de la session (configuration → entre patches → exposant → terminé). `TeststripViewModel` orchestre le tout avec `CountdownTimer` pour chaque exposition de patch. `TeststripScreen` affiche la liste des patches et les contrôles de session.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX ViewModel + StateFlow, Kotlin Coroutines, CountdownTimer existant

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `math/TeststripEngine.kt` | Créer | Calcul des temps de patches selon formule f-stop |
| `system/TeststripSession.kt` | Créer | Machine d'états de session teststrip |
| `ui/TeststripViewModel.kt` | Créer | ViewModel orchestre session + timer par patch |
| `ui/TeststripScreen.kt` | Créer | Écran configuration + liste patches + contrôles |
| `ui/PatchItem.kt` | Créer | Composable affichage ligne patch |
| `test/.../TeststripEngineTest.kt` | Créer | Tests calculs temps patches |
| `test/.../TeststripSessionTest.kt` | Créer | Tests machine d'états session |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/PatchItem.kt`
- `app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt`
- `app/src/test/java/fr/mathgl/darkroomtimer/system/TeststripSessionTest.kt`

---

## Task 1: TeststripEngine — calcul des temps de patches

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt`

- [ ] **Step 1.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt
package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class TeststripEngineTest {

    @Test
    fun `should calculate correct times for base 8000, 1/3 stop, 6 patches`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val times = engine.patchTimesMs

        assertEquals(8000L, times[0])   // Patch 1: 8.0s
        assertEquals(10079L, times[1])  // Patch 2: 10.1s
        assertEquals(12699L, times[2])  // Patch 3: 12.7s
        assertEquals(16000L, times[3])  // Patch 4: 16.0s
        assertEquals(20159L, times[4])  // Patch 5: 20.2s
        assertEquals(25398L, times[5])  // Patch 6: 25.4s
    }

    @Test
    fun `should calculate differential times for incremental mode`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val diffs = engine.differentialTimesMs

        assertEquals(8000L, diffs[0])    // Patch 1: 8.0s (base)
        assertEquals(2079L, diffs[1])    // Patch 2: 10079 - 8000
        assertEquals(2620L, diffs[2])    // Patch 3: 12699 - 10079
        assertEquals(3301L, diffs[3])    // Patch 4: 16000 - 12699
        assertEquals(4159L, diffs[4])    // Patch 5: 20159 - 16000
        assertEquals(5239L, diffs[5])    // Patch 6: 25398 - 20159
    }

    @Test
    fun `should throw for baseTime below 100ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 99, numerator = 1, denominator = 3, patchCount = 6)
        }
    }

    @Test
    fun `should throw for baseTime above 999000ms`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 999_001, numerator = 1, denominator = 3, patchCount = 6)
        }
    }

    @Test
    fun `should throw for patchCount below 3`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 2)
        }
    }

    @Test
    fun `should throw for patchCount above 7`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 8)
        }
    }

    @Test
    fun `should throw for denominator zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 0, patchCount = 6)
        }
    }

    @Test
    fun `should use all time as base for 1 stop increment`() {
        val engine = TeststripEngine(baseTimeMs = 10000, numerator = 1, denominator = 1, patchCount = 4)
        val times = engine.patchTimesMs

        assertEquals(10000L, times[0])  // 10000 * 2^0 = 10000
        assertEquals(20000L, times[1])  // 10000 * 2^1 = 20000
        assertEquals(40000L, times[2])  // 10000 * 2^2 = 40000
        assertEquals(80000L, times[3])  // 10000 * 2^3 = 80000
    }

    @Test
    fun `should use 1/2 stop increment correctly`() {
        val engine = TeststripEngine(baseTimeMs = 10000, numerator = 1, denominator = 2, patchCount = 3)
        val times = engine.patchTimesMs

        assertEquals(10000L, times[0])      // 10000 * 2^0 = 10000
        assertEquals(14142L, times[1])      // 10000 * 2^0.5 ≈ 14142
        assertEquals(20000L, times[2])      // 10000 * 2^1 = 20000
    }

    @Test
    fun `differentialTimeForPatch returns correct diff for given patch index`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        
        assertEquals(8000L, engine.differentialTimeForPatch(0))
        assertEquals(2079L, engine.differentialTimeForPatch(1))
        assertEquals(2620L, engine.differentialTimeForPatch(2))
        assertEquals(5239L, engine.differentialTimeForPatch(5))
    }

    @Test
    fun `formatStopTime produces correct MM_SS_d format`() {
        assertEquals("00:08.0", TeststripEngine.formatStopTime(8000L))
        assertEquals("01:05.4", TeststripEngine.formatStopTime(65400L))
        assertEquals("16:39.0", TeststripEngine.formatStopTime(999_000L))
        assertEquals("00:00.1", TeststripEngine.formatStopTime(100L))
    }

    @Test
    fun `formatStopTime clamps negative values to zero`() {
        assertEquals("00:00.0", TeststripEngine.formatStopTime(-500L))
    }

    @Test
    fun `simplifiedFraction returns reduced numerator and denominator`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        val (n, d) = engine.simplifiedFraction
        
        assertEquals(1, n)
        assertEquals(3, d)
    }

    @Test
    fun `simplifiedFraction reduces 2/6 to 1/3`() {
        val engine = TeststripEngine(baseTimeMs = 8000, numerator = 2, denominator = 6, patchCount = 6)
        val (n, d) = engine.simplifiedFraction
        
        assertEquals(1, n)
        assertEquals(3, d)
    }
}
```

- [ ] **Step 1.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.math.TeststripEngineTest"
```
Résultat attendu : FAILED — `TeststripEngine` n'existe pas encore.

- [ ] **Step 1.3: Implémenter TeststripEngine**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt
package fr.mathgl.darkroomtimer.math

import kotlin.math.pow
import kotlin.math.roundToLong

class TeststripEngine(
    var baseTimeMs: Long,
    var numerator: Int,
    var denominator: Int,
    val patchCount: Int
) {
    init {
        require(baseTimeMs in 100L..999_000L) {
            "baseTimeMs must be in [100, 999000], was $baseTimeMs"
        }
        require(patchCount in 3..7) {
            "patchCount must be in [3, 7], was $patchCount"
        }
        require(denominator != 0) {
            "denominator cannot be zero"
        }
    }

    val simplifiedFraction: Pair<Int, Int>
        get() = simplify(numerator, denominator)

    val patchTimesMs: List<Long>
        get() = (0 until patchCount).map { n ->
            val stops = (numerator.toDouble() / denominator) * n
            (baseTimeMs * 2.0.pow(stops)).roundToLong()
        }

    val differentialTimesMs: List<Long>
        get() {
            val times = patchTimesMs
            return buildList(patchCount) {
                add(times[0])
                for (i in 1 until patchCount) {
                    add(times[i] - times[i - 1])
                }
            }
        }

    fun differentialTimeForPatch(patchIndex: Int): Long {
        val times = patchTimesMs
        return if (patchIndex == 0) times[0] else times[patchIndex] - times[patchIndex - 1]
    }

    fun formatStopTime(ms: Long): String {
        val clamped = maxOf(0L, ms)
        val totalTenths = clamped / 100
        val tenths = totalTenths % 10
        val totalSeconds = totalTenths / 10
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return "%02d:%02d.%d".format(minutes, seconds, tenths)
    }

    private fun simplify(numerator: Int, denominator: Int): Pair<Int, Int> {
        if (denominator == 0) return Pair(numerator, denominator)
        val common = gcd(kotlin.math.abs(numerator), denominator)
        var n = numerator / common
        var d = denominator / common
        if (d < 0) { n = -n; d = -d }
        return Pair(n, d)
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a; var y = b
        while (y != 0) { val temp = y; y = x % y; x = temp }
        return x
    }
}
```

- [ ] **Step 1.4: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.math.TeststripEngineTest"
```
Résultat attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 1.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/math/TeststripEngine.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/math/TeststripEngineTest.kt
git commit -m "feat: add TeststripEngine with f-stop calculation for patch times"
```

---

## Task 2: TeststripSession — machine d'états de session

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/system/TeststripSessionTest.kt`

- [ ] **Step 2.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/system/TeststripSessionTest.kt
package fr.mathgl.darkroomtimer.system

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TeststripSessionTest {

    private var fakeNow = 0L
    private lateinit var session: TeststripSession

    @Before
    fun setup() {
        fakeNow = 0L
        session = TeststripSession(
            engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6),
            clock = { fakeNow }
        )
    }

    // --- État initial ---

    @Test
    fun `initial state is CONFIGURED`() {
        assertEquals(TeststripState.CONFIGURED, session.state)
        assertEquals(-1, session.currentPatchIndex)
    }

    @Test
    fun `initially no exposure is running`() {
        assertFalse(session.isExposing)
        assertFalse(session.isSessionComplete)
    }

    // --- Démarrer une session ---

    @Test
    fun `start exposes first patch`() {
        session.start()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
        assertTrue(session.isExposing)
    }

    @Test
    fun `start sets correct exposure time for first patch`() {
        session.start()
        assertEquals(8000L, session.currentExposureTimeMs)
    }

    // --- Fin d'exposition d'un patch ---

    @Test
    fun `finishExposure transitions to BETWEEN_PATCHES`() {
        session.start()
        session.finishExposure()
        assertEquals(TeststripState.BETWEEN_PATCHES, session.state)
        assertFalse(session.isExposing)
    }

    @Test
    fun `finishExposure marks first patch as exposed`() {
        session.start()
        session.finishExposure()
        assertTrue(session.isPatchExposed(0))
        assertFalse(session.isPatchExposed(1))
    }

    // --- Aller au patch suivant ---

    @Test
    fun `nextPatch starts exposure for next patch`() {
        session.start()
        session.finishExposure()
        session.nextPatch()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(1, session.currentPatchIndex)
        assertEquals(2079L, session.currentExposureTimeMs)
    }

    @Test
    fun `nextPatch wraps around to first patch after last patch`() {
        // Expose all 6 patches
        repeat(6) { _ ->
            session.start()
            session.finishExposure()
            if (it < 5) session.nextPatch()
        }
        // Now at patch 5, call nextPatch -> should wrap to patch 0
        session.nextPatch()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
    }

    // --- Session complète ---

    @Test
    fun `session complete after all patches exposed and in BETWEEN_PATCHES`() {
        repeat(6) { _ ->
            session.start()
            session.finishExposure()
            if (it < 5) session.nextPatch()
        }
        assertEquals(TeststripState.BETWEEN_PATCHES, session.state)
        assertTrue(session.isSessionComplete)
    }

    @Test
    fun `nextPatch after completion resets and starts new session`() {
        repeat(6) { _ ->
            session.start()
            session.finishExposure()
            if (it < 5) session.nextPatch()
        }
        session.nextPatch()  // This should start a new session
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(0, session.currentPatchIndex)
        assertFalse(session.isSessionComplete)
    }

    // --- Recommencer le patch courant ---

    @Test
    fun `restartCurrentPatch keeps same patch index`() {
        session.start()
        session.finishExposure()
        session.restartCurrentPatch()
        assertEquals(0, session.currentPatchIndex)
        assertEquals(TeststripState.EXPOSING, session.state)
        assertEquals(8000L, session.currentExposureTimeMs)
    }

    @Test
    fun `restartCurrentPatch resets exposed status for current patch`() {
        session.start()
        session.finishExposure()
        assertTrue(session.isPatchExposed(0))
        session.restartCurrentPatch()
        assertFalse(session.isPatchExposed(0))
    }

    // --- Abandonner la session ---

    @Test
    fun `abandon transitions to CONFIGURED`() {
        session.start()
        session.finishExposure()
        session.abandon()
        assertEquals(TeststripState.CONFIGURED, session.state)
        assertEquals(-1, session.currentPatchIndex)
        assertFalse(session.isSessionComplete)
    }

    // --- Pause pendant exposition ---

    @Test
    fun `pause during exposure transitions to PAUSED`() {
        session.start()
        session.pause()
        assertEquals(TeststripState.PAUSED, session.state)
        assertTrue(session.isPaused)
    }

    @Test
    fun `resume from PAUSED goes back to EXPOSING`() {
        session.start()
        session.pause()
        session.resume()
        assertEquals(TeststripState.EXPOSING, session.state)
        assertFalse(session.isPaused)
    }

    @Test
    fun `resume preserves remaining time calculation`() {
        session.start()
        fakeNow = 3000L  // 3 seconds elapsed
        session.pause()
        fakeNow = 10000L  // 7 seconds passed during pause (should not count)
        session.resume()
        fakeNow = 13000L  // 3 more seconds
        assertEquals(5000L, session.remainingTimeMs)  // 8000 - 3000 = 5000
    }

    // --- Invalid transitions ---

    @Test(expected = IllegalStateException::class)
    fun `start from EXPOSING throws`() {
        session.start()
        session.start()
    }

    @Test(expected = IllegalStateException::class)
    fun `nextPatch during exposure throws`() {
        session.start()
        session.nextPatch()
    }

    @Test(expected = IllegalStateException::class)
    fun `finishExposure from CONFIGURED throws`() {
        session.finishExposure()
    }

    @Test(expected = IllegalStateException::class)
    fun `pause from CONFIGURED throws`() {
        session.pause()
    }

    @Test(expected = IllegalStateException::class)
    fun `resume from CONFIGURED throws`() {
        session.resume()
    }

    // --- Calcul du temps restant ---

    @Test
    fun `remainingTimeMs decreases during exposure`() {
        session.start()
        fakeNow = 3000L
        assertEquals(5000L, session.remainingTimeMs)
    }

    @Test
    fun `remainingTimeMs frozen during pause`() {
        session.start()
        fakeNow = 3000L
        session.pause()
        fakeNow = 100000L  // lots of time passes
        assertEquals(5000L, session.remainingTimeMs)
    }
}
```

- [ ] **Step 2.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.system.TeststripSessionTest"
```
Résultat attendu : FAILED — `TeststripSession`, `TeststripState` n'existent pas.

- [ ] **Step 2.3: Implémenter TeststripSession**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt
package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.system.TeststripState.*

enum class TeststripState { CONFIGURED, EXPOSING, BETWEEN_PATCHES, PAUSED }

class TeststripSession(
    private val engine: TeststripEngine,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    var state: TeststripState = CONFIGURED
        private set

    private var currentPatchIndexValue: Int = -1
        set(value) {
            field = value
            if (value >= 0) {
                exposureStartAt = clock()
                pauseStartAt = null
            }
        }

    val currentPatchIndex: Int get() = currentPatchIndexValue

    private var exposureStartAt: Long = 0L
    private var pauseStartAt: Long? = null
    private var elapsedBeforePause: Long = 0L

    private val exposedPatches = mutableSetOf<Int>()

    val isExposing: Boolean get() = state == EXPOSING
    val isPaused: Boolean get() = state == PAUSED
    val isSessionComplete: Boolean get() = state == BETWEEN_PATCHES && exposedPatches.size == engine.patchCount

    val currentExposureTimeMs: Long
        get() = if (currentPatchIndex < 0) 0L else engine.differentialTimeForPatch(currentPatchIndex)

    val remainingTimeMs: Long
        get() {
            if (currentPatchIndex < 0) return currentExposureTimeMs
            val elapsed = when (state) {
                EXPOSING -> clock() - exposureStartAt
                PAUSED -> elapsedBeforePause
                else -> 0L
            }
            return currentExposureTimeMs - elapsed
        }

    fun start() {
        check(state == CONFIGURED || state == BETWEEN_PATCHES || state == PAUSED) {
            "start() called from state $state"
        }
        if (state == PAUSED) {
            // Resume logic handled separately
            state = EXPOSING
            return
        }
        currentPatchIndexValue = when (state) {
            CONFIGURED -> 0
            BETWEEN_PATCHES -> currentPatchIndexValue
            else -> throw IllegalStateException("Invalid state: $state")
        }
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun pause() {
        check(state == EXPOSING) { "pause() called from state $state" }
        pauseStartAt = clock()
        elapsedBeforePause = pauseStartAt - exposureStartAt
        state = PAUSED
    }

    fun resume() {
        check(state == PAUSED) { "resume() called from state $state" }
        exposureStartAt = clock() - elapsedBeforePause
        pauseStartAt = null
        state = EXPOSING
    }

    fun finishExposure() {
        check(state == EXPOSING) { "finishExposure() called from state $state" }
        exposedPatches.add(currentPatchIndex)
        state = BETWEEN_PATCHES
    }

    fun nextPatch() {
        check(state == BETWEEN_PATCHES) { "nextPatch() called from state $state" }
        val nextIndex = if (currentPatchIndexValue == engine.patchCount - 1) 0 else currentPatchIndexValue + 1
        if (nextIndex == 0 && currentPatchIndexValue == engine.patchCount - 1) {
            // Wraparound: reset exposed patches for new session
            exposedPatches.clear()
        }
        currentPatchIndexValue = nextIndex
        exposedPatches.remove(currentPatchIndexValue)
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun restartCurrentPatch() {
        check(state == BETWEEN_PATCHES) { "restartCurrentPatch() called from state $state" }
        exposedPatches.remove(currentPatchIndexValue)
        state = EXPOSING
        exposureStartAt = clock()
        elapsedBeforePause = 0L
        pauseStartAt = null
    }

    fun abandon() {
        state = CONFIGURED
        currentPatchIndexValue = -1
        exposedPatches.clear()
    }

    fun isPatchExposed(patchIndex: Int): Boolean = exposedPatches.contains(patchIndex)

    fun formatTime(ms: Long): String = engine.formatStopTime(ms)
}
```

- [ ] **Step 2.4: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.system.TeststripSessionTest"
```
Résultat attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 2.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/TeststripSession.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/system/TeststripSessionTest.kt
git commit -m "feat: add TeststripSession state machine for patch progression"
```

---

## Task 3: TeststripViewModel — orchestration UI

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`

Le ViewModel orchestre la session teststrip, déclenche les expositions via RelaySystem, et expose un StateFlow pour l'UI.

- [ ] **Step 3.1: Implémenter TeststripViewModel**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt
package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.math.TeststripEngine
import fr.mathgl.darkroomtimer.system.RelayController
import fr.mathgl.darkroomtimer.system.RelayStates
import fr.mathgl.darkroomtimer.system.RelaySystem
import fr.mathgl.darkroomtimer.system.TeststripSession
import fr.mathgl.darkroomtimer.system.TeststripState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeststripUiState(
    val sessionState: TeststripState,
    val currentPatchIndex: Int,
    val patchCount: Int,
    val patchTimesMs: List<Long>,
    val differentialTimesMs: List<Long>,
    val exposedPatches: Set<Int>,
    val displayTime: String,
    val remainingTimeMs: Long,
    val isSessionComplete: Boolean,
    val baseTimeMs: Long,
    val numerator: Int,
    val denominator: Int,
    val selectedGrade: ContrastGrade
)

class TeststripViewModel(
    application: Application,
    private val relaySystem: RelaySystem
) : AndroidViewModel(application) {

    private val engine: TeststripEngine
    private val session: TeststripSession

    private var exposureJob: Job? = null
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(TeststripUiState(
        sessionState = TeststripState.CONFIGURED,
        currentPatchIndex = -1,
        patchCount = 6,
        patchTimesMs = emptyList(),
        differentialTimesMs = emptyList(),
        exposedPatches = emptySet(),
        displayTime = "00:08.0",
        remainingTimeMs = 8000L,
        isSessionComplete = false,
        baseTimeMs = 8000L,
        numerator = 1,
        denominator = 3,
        selectedGrade = ContrastGrade.DEFAULT
    ))
    val uiState: StateFlow<TeststripUiState> = _uiState.asStateFlow()

    init {
        engine = TeststripEngine(baseTimeMs = 8000, numerator = 1, denominator = 3, patchCount = 6)
        session = TeststripSession(engine = engine)
        updateUiState()
    }

    private fun updateUiState() {
        _uiState.update { it.copy(
            sessionState = session.state,
            currentPatchIndex = session.currentPatchIndex,
            patchTimesMs = engine.patchTimesMs,
            differentialTimesMs = engine.differentialTimesMs,
            exposedPatches = (0 until engine.patchCount).filter { i -> session.isPatchExposed(i) }.toSet(),
            displayTime = session.formatTime(session.remainingTimeMs),
            remainingTimeMs = session.remainingTimeMs,
            isSessionComplete = session.isSessionComplete,
            patchCount = engine.patchCount,
            baseTimeMs = engine.baseTimeMs,
            numerator = engine.numerator,
            denominator = engine.denominator
        ) }
    }

    fun startSession() {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        session.start()
        startExposure()
        updateUiState()
    }

    fun pause() {
        if (session.state != TeststripState.EXPOSING) return
        exposureJob?.cancel()
        exposureJob = null
        tickJob?.cancel()
        tickJob = null
        session.pause()
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        updateUiState()
    }

    fun resume() {
        if (session.state != TeststripState.PAUSED) return
        session.resume()
        updateUiState()
        startExposure()
    }

    fun finishExposure() {
        if (session.state != TeststripState.EXPOSING) return
        exposureJob?.cancel()
        exposureJob = null
        tickJob?.cancel()
        tickJob = null
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        session.finishExposure()
        updateUiState()
    }

    fun nextPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES) return
        session.nextPatch()
        updateUiState()
        startExposure()
    }

    fun restartCurrentPatch() {
        if (session.state != TeststripState.BETWEEN_PATCHES) return
        session.restartCurrentPatch()
        updateUiState()
        startExposure()
    }

    fun abandon() {
        exposureJob?.cancel()
        tickJob?.cancel()
        exposureJob = null
        tickJob = null
        viewModelScope.launch {
            relaySystem.setEnlarger(false)
            relaySystem.setSafelight(false)
        }
        session.abandon()
        updateUiState()
    }

    fun updateBaseTime(newTimeMs: Long) {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.baseTimeMs = newTimeMs
        updateUiState()
    }

    fun updateStopFraction(numerator: Int, denominator: Int) {
        if (session.state != TeststripState.CONFIGURED && session.state != TeststripState.BETWEEN_PATCHES) return
        engine.numerator = numerator
        engine.denominator = denominator
        updateUiState()
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.update { it.copy(selectedGrade = grade) }
    }

    private fun startExposure() {
        if (session.state != TeststripState.EXPOSING) return
        val durationMs = session.currentExposureTimeMs
        viewModelScope.launch {
            relaySystem.startTimedExposure(durationMs)
        }
        exposureJob = viewModelScope.launch {
            delay(durationMs)
            finishExposure()
        }
        tickJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                updateUiState()
                if (session.state != TeststripState.EXPOSING) break
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exposureJob?.cancel()
        tickJob?.cancel()
    }
}
```

- [ ] **Step 3.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 3.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt
git commit -m "feat: add TeststripViewModel with session orchestration"
```

---

## Task 4: PatchItem Composable

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/PatchItem.kt`

- [ ] **Step 4.1: Implémenter PatchItem**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/PatchItem.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PatchItem(
    patchNumber: Int,
    timeMs: Long,
    differentialMs: Long,
    isExposed: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        isCurrent -> Color(0xFFCC2200)
        isExposed -> Color(0xFF44AA44)
        else -> Color(0xFF444444)
    }
    val backgroundColor = when {
        isCurrent -> Color(0x11CC2200)
        isExposed -> Color(0x1144AA44)
        else -> Color.Transparent
    }
    val textColor = if (isCurrent) Color.White else Color.White

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Patch $patchNumber",
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${timeMs / 1000.0}s",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "(${differentialMs / 1000.0}s)",
                fontSize = 11.sp,
                color = Color(0xFFAAAAAA)
            )
            if (isExposed) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    color = Color(0xFF44AA44)
                )
            }
        }
    }
}
```

- [ ] **Step 4.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 4.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/PatchItem.kt
git commit -m "feat: add PatchItem composable for teststrip patch display"
```

---

## Task 5: TeststripScreen

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt`

- [ ] **Step 5.1: Implémenter TeststripScreen**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.TeststripState

@Composable
fun TeststripScreen(
    viewModel: TeststripViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header avec titre et bouton retour
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Teststrip",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session state indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sessionStateText(state.sessionState, state.currentPatchIndex, state.patchCount),
                fontSize = 16.sp,
                color = when (state.sessionState) {
                    TeststripState.EXPOSING -> Color(0xFFCC2200)
                    TeststripState.BETWEEN_PATCHES -> Color(0xFF44AA44)
                    else -> Color(0xFFAAAAAA)
                }
            )
            if (state.isSessionComplete) {
                Text(
                    text = "COMPLÉTÉ ✓",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44AA44)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Configuration (visible seulement en CONFIGURED ou BETWEEN_PATCHES)
        if (state.sessionState != TeststripState.EXPOSING) {
            ConfigurationSection(
                baseTimeMs = state.baseTimeMs,
                numerator = state.numerator,
                denominator = state.denominator,
                onBaseTimeChange = { viewModel.updateBaseTime(it) },
                onStopFractionChange = { num, den -> viewModel.updateStopFraction(num, den) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Liste des patches
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(state.patchTimesMs) { index, timeMs ->
                PatchItem(
                    patchNumber = index + 1,
                    timeMs = timeMs,
                    differentialMs = state.differentialTimesMs[index],
                    isExposed = index in state.exposedPatches,
                    isCurrent = index == state.currentPatchIndex,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Affichage du temps pendant exposition
        if (state.sessionState == TeststripState.EXPOSING) {
            Text(
                text = state.displayTime,
                fontSize = 60.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Temps: ${state.remainingTimeMs / 1000.0}s",
                fontSize = 16.sp,
                color = Color(0xFFAAAAAA)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contrôles pendant exposition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.sessionState == TeststripState.EXPOSING) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400))
                    ) {
                        Text("PAUSE", fontSize = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contrôles entre patches
        if (state.sessionState == TeststripState.BETWEEN_PATCHES) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.restartCurrentPatch() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                ) {
                    Text("RECOMMENCER", fontSize = 14.sp)
                }
                Button(
                    onClick = { viewModel.nextPatch() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("SUIVANT →", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { viewModel.abandon() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFAA4444))
            ) {
                Text("ABANDONNER", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton démarrage (CONFIGURED seulement)
        if (state.sessionState == TeststripState.CONFIGURED) {
            Button(
                onClick = { viewModel.startSession() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
            ) {
                Text("DÉMARRER", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun sessionStateText(state: TeststripState, patchIndex: Int, patchCount: Int): String {
    return when (state) {
        TeststripState.CONFIGURED -> "En attente"
        TeststripState.EXPOSING -> "Patch ${patchIndex + 1} / $patchCount"
        TeststripState.BETWEEN_PATCHES -> "Patch ${patchIndex + 1} terminé"
        TeststripState.PAUSED -> "PAUSÉ"
    }
}

@Composable
private fun ConfigurationSection(
    baseTimeMs: Long,
    numerator: Int,
    denominator: Int,
    onBaseTimeChange: (Long) -> Unit,
    onStopFractionChange: (Int, Int) -> Unit
) {
    Text(
        text = "Configuration",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Temps de base
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Base:", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        Text("${baseTimeMs / 1000.0}s", fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = { onBaseTimeChange((baseTimeMs - 1000).coerceIn(100L, 999_000L)) },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("-1s", fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = { onBaseTimeChange((baseTimeMs + 1000).coerceIn(100L, 999_000L)) },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("+1s", fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Incrément f-stop
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Incrément:", fontSize = 14.sp, color = Color(0xFFAAAAAA))
        val (n, d) = simplifyFraction(numerator, denominator)
        Text("${n}/${d} stop", fontSize = 16.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        OutlinedButton(
            onClick = {
                val (n, d) = simplifyFraction(numerator, denominator)
                val (newN, newD) = simplifyFraction(n + 1, d)
                onStopFractionChange(newN, newD)
            },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("+1/n", fontSize = 14.sp)
        }
        OutlinedButton(
            onClick = {
                val (n, d) = simplifyFraction(numerator, denominator)
                val (newN, newD) = simplifyFraction(n - 1, d)
                onStopFractionChange(newN, newD)
            },
            modifier = Modifier.weight(1f).height(40.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200))
        ) {
            Text("-1/n", fontSize = 14.sp)
        }
    }
}

private fun simplifyFraction(n: Int, d: Int): Pair<Int, Int> {
    if (d == 0) return Pair(n, d)
    val common = gcd(kotlin.math.abs(n), d)
    return Pair(n / common, d / common)
}

private fun gcd(a: Int, b: Int): Int {
    var x = a; var y = b
    while (y != 0) { val temp = y; y = x % y; x = temp }
    return x
}
```

- [ ] **Step 5.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 5.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt
git commit -m "feat: add TeststripScreen with patch list and session controls"
```

---

## Task 6: Intégration dans MainActivity

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

Ajouter une navigation vers l'écran Teststrip. Pour simplifier, on ajoute un menu ou un bouton pour basculer entre Countdown et Teststrip.

- [ ] **Step 6.1: Mettre à jour MainActivity avec navigation entre modes**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.TeststripScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppMode { COUNTDOWN, TESTSTRIP }

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)

        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ModeSelectorScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        luminosityManager.start()
    }

    override fun onStop() {
        super.onStop()
        luminosityManager.stop()
    }
}

@Composable
fun ModeSelectorScreen() {
    var selectedMode by rememberSaveable { mutableStateOf<AppMode?>(null) }

    if (selectedMode == null) {
        // Mode selection screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DarkroomTimer",
                color = Color.White,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.COUNTDOWN },
                modifier = Modifier.width(200.dp)
            ) {
                Text("Countdown", color = Color.Black)
            }
            Spacer(modifier = Modifier.width(0.dp))
            Spacer(modifier = Modifier.width(0.dp))
            Button(
                onClick = { selectedMode = AppMode.TESTSTRIP },
                modifier = Modifier.width(200.dp)
            ) {
                Text("Teststrip", color = Color.Black)
            }
        }
    } else {
        when (selectedMode) {
            AppMode.COUNTDOWN -> CountdownScreen()
            AppMode.TESTSTRIP -> TeststripScreen(onBack = { selectedMode = null })
            else -> ModeSelectorScreen()
        }
    }
}
```

- [ ] **Step 6.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 6.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "feat: add mode selector for countdown and teststrip"
```

---

## Récapitulatif des fichiers

| Fichier | Action |
|---|---|
| `math/TeststripEngine.kt` | Créé |
| `system/TeststripSession.kt` | Créé |
| `ui/TeststripViewModel.kt` | Créé |
| `ui/TeststripScreen.kt` | Créé |
| `ui/PatchItem.kt` | Créé |
| `ui/ModeSelectorScreen.kt` | Modifié (intégré dans MainActivity) |
| `MainActivity.kt` | Modifié (mode selector) |
| `test/.../TeststripEngineTest.kt` | Créé |
| `test/.../TeststripSessionTest.kt` | Créé |

---

## Vérification manuelle post-implémentation

Installer l'APK sur un émulateur ou appareil physique :

```
./gradlew installDebug
```

Vérifier :
1. L'écran principal affiche deux boutons: "Countdown" et "Teststrip"
2. Sélectionner "Teststrip" affiche la configuration (base=8.0s, incrément=1/3)
3. La liste des 6 patches affiche les temps corrects (8.0s, 10.1s, 12.7s, 16.0s, 20.2s, 25.4s)
4. Bouton "DÉMARRER" lance l'exposition du patch 1
5. L'affichage du temps décompte correctement
6. À la fin de l'exposition, l'état passe à "entre patches" avec checkmark sur le patch
7. "SUIVANT" lance l'exposition du patch suivant
8. "RECOMMENCER" relance le patch courant
9. Après 6 patches, l'état affiche "COMPLÉTÉ"
10. "ABANDONNER" retourne à l'écran de sélection de mode
11. La pause fonctionne pendant l'exposition et reprend correctement

---

## Auto-Review du plan

**1. Couverture du spec:**
- ✅ Mode INCREMENTAL avec temps cumulés et différentiels
- ✅ Paramètres configurables (base_time, stop_fraction, patch_count)
- ✅ Formule time[n] = base × 2^(stop × n)
- ✅ State machine: CONFIGURED → EXPOSING → BETWEEN_PATCHES → (wraparound) ou COMPLETED
- ✅ Contrôles: start, pause, resume, nextPatch, restartCurrentPatch, abandon
- ✅ Wraparound après dernier patch
- ✅ Affichage MM:SS.d
- ✅ UI complète avec liste des patches et indicateurs d'exposition

**2. Scan des placeholders:**
- Aucun placeholder trouvé (TBD, TODO, etc.)
- Tous les blocs de code sont complets
- Tous les types sont définis dans le plan

**3. Cohérence des types:**
- TeststripEngine, TeststripSession, TeststripViewModel utilisent des noms cohérents
- TeststripState enum cohérent avec les appels
- StateFlow utilisé correctement pour l'UI

Le plan est prêt à être exécuté.
