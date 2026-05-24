# Contrast Grades + Countdown Timer — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implémenter l'enum `ContrastGrade` et la machine d'états du timer countdown avec son écran de base.

**Architecture:** `ContrastGrade` est un enum pur Kotlin dans le package `math`. `CountdownTimer` est un moteur pur Kotlin (sans dépendance Android) dans `system`, wrappé par un `CountdownViewModel` Compose dans `ui`. L'écran `CountdownScreen` affiche le timer et le sélecteur de grade. Un `ForegroundTimerService` maintient le timer actif en arrière-plan.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX ViewModel + viewModelScope, Kotlin Coroutines, Android Foreground Service

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `math/ContrastGrade.kt` | Créer | Enum des 12 grades de contraste |
| `storage/PreferenceManager.kt` | Modifier | Ajouter `defaultContrastGrade` computed property |
| `system/CountdownTimer.kt` | Créer | Machine d'états pur Kotlin (STOPPED/RUNNING/PAUSED) |
| `system/RelayState.kt` | Créer | Modèle état visuel des relais (agrandisseur + safelight) |
| `system/ForegroundTimerService.kt` | Créer | Foreground Service pour stabilité pendant RUNNING |
| `ui/CountdownViewModel.kt` | Créer | ViewModel coroutine-based, expose StateFlow |
| `ui/GradeSelector.kt` | Créer | Composable sélecteur horizontal 12 grades |
| `ui/CountdownScreen.kt` | Créer | Écran principal: timer + contrôles + grade |
| `gradle/libs.versions.toml` | Modifier | Ajouter `lifecycle-viewmodel-ktx` et `lifecycle-viewmodel-compose` |
| `app/build.gradle.kts` | Modifier | Ajouter dépendances ViewModel |
| `AndroidManifest.xml` | Modifier | Déclarer ForegroundService + permission FOREGROUND_SERVICE |
| `MainActivity.kt` | Modifier | Afficher CountdownScreen, démarrer/stopper le service |
| `test/.../ContrastGradeTest.kt` | Créer | Tests unitaires ContrastGrade |
| `test/.../CountdownTimerTest.kt` | Créer | Tests unitaires CountdownTimer |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/math/ContrastGrade.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/system/CountdownTimer.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/system/ForegroundTimerService.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/GradeSelector.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`
- `app/src/test/java/fr/mathgl/darkroomtimer/math/ContrastGradeTest.kt`
- `app/src/test/java/fr/mathgl/darkroomtimer/system/CountdownTimerTest.kt`

---

## Task 1: ContrastGrade enum

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/math/ContrastGrade.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/math/ContrastGradeTest.kt`

- [ ] **Step 1.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/math/ContrastGradeTest.kt
package fr.mathgl.darkroomtimer.math

import org.junit.Assert.*
import org.junit.Test

class ContrastGradeTest {

    @Test
    fun `should have 12 grades`() {
        assertEquals(12, ContrastGrade.entries.size)
    }

    @Test
    fun `GRADE_00 and GRADE_0 both have floatValue 0,00 but are distinct`() {
        assertEquals(0.00f, ContrastGrade.GRADE_00.floatValue)
        assertEquals(0.00f, ContrastGrade.GRADE_0.floatValue)
        assertNotEquals(ContrastGrade.GRADE_00, ContrastGrade.GRADE_0)
    }

    @Test
    fun `GRADE_2 is the default grade`() {
        assertEquals(ContrastGrade.GRADE_2, ContrastGrade.DEFAULT)
    }

    @Test
    fun `labels match spec`() {
        assertEquals("00",  ContrastGrade.GRADE_00.label)
        assertEquals("0",   ContrastGrade.GRADE_0.label)
        assertEquals("½",   ContrastGrade.GRADE_HALF.label)
        assertEquals("1",   ContrastGrade.GRADE_1.label)
        assertEquals("1½",  ContrastGrade.GRADE_1H.label)
        assertEquals("2",   ContrastGrade.GRADE_2.label)
        assertEquals("2½",  ContrastGrade.GRADE_2H.label)
        assertEquals("3",   ContrastGrade.GRADE_3.label)
        assertEquals("3½",  ContrastGrade.GRADE_3H.label)
        assertEquals("4",   ContrastGrade.GRADE_4.label)
        assertEquals("4½",  ContrastGrade.GRADE_4H.label)
        assertEquals("5",   ContrastGrade.GRADE_5.label)
    }

    @Test
    fun `fromIndex returns correct grade`() {
        assertEquals(ContrastGrade.GRADE_00,   ContrastGrade.fromIndex(0))
        assertEquals(ContrastGrade.GRADE_0,    ContrastGrade.fromIndex(1))
        assertEquals(ContrastGrade.GRADE_HALF, ContrastGrade.fromIndex(2))
        assertEquals(ContrastGrade.GRADE_2,    ContrastGrade.fromIndex(5))
        assertEquals(ContrastGrade.GRADE_5,    ContrastGrade.fromIndex(11))
    }

    @Test
    fun `fromIndex clamps out-of-bounds values`() {
        assertEquals(ContrastGrade.GRADE_00, ContrastGrade.fromIndex(-1))
        assertEquals(ContrastGrade.GRADE_5,  ContrastGrade.fromIndex(100))
    }

    @Test
    fun `index property matches ordinal`() {
        ContrastGrade.entries.forEachIndexed { i, grade ->
            assertEquals(i, grade.index)
        }
    }
}
```

- [ ] **Step 1.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.math.ContrastGradeTest"
```
Résultat attendu : FAILED — `ContrastGrade` n'existe pas encore.

- [ ] **Step 1.3: Implémenter ContrastGrade**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/math/ContrastGrade.kt
package fr.mathgl.darkroomtimer.math

enum class ContrastGrade(val floatValue: Float, val label: String) {
    GRADE_00  (0.00f, "00"),
    GRADE_0   (0.00f, "0"),
    GRADE_HALF(0.50f, "½"),
    GRADE_1   (1.00f, "1"),
    GRADE_1H  (1.50f, "1½"),
    GRADE_2   (2.00f, "2"),
    GRADE_2H  (2.50f, "2½"),
    GRADE_3   (3.00f, "3"),
    GRADE_3H  (3.50f, "3½"),
    GRADE_4   (4.00f, "4"),
    GRADE_4H  (4.50f, "4½"),
    GRADE_5   (5.00f, "5");

    val index: Int get() = ordinal

    companion object {
        val DEFAULT = GRADE_2

        fun fromIndex(index: Int): ContrastGrade {
            val entries = ContrastGrade.entries
            return entries[index.coerceIn(0, entries.lastIndex)]
        }
    }
}
```

- [ ] **Step 1.4: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.math.ContrastGradeTest"
```
Résultat attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 1.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/math/ContrastGrade.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/math/ContrastGradeTest.kt
git commit -m "feat: add ContrastGrade enum with 12 grades and fromIndex helper"
```

---

## Task 2: PreferenceManager — propriété ContrastGrade

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt`

La propriété `defaultContrastGradeIndex: Int` existe déjà. Ajouter une propriété computed `defaultContrastGrade` qui lit/écrit via l'index existant.

- [ ] **Step 2.1: Modifier PreferenceManager**

Ajouter après la propriété `defaultContrastGradeIndex` (ligne ~36) :

```kotlin
    var defaultContrastGrade: fr.mathgl.darkroomtimer.math.ContrastGrade
        get() = fr.mathgl.darkroomtimer.math.ContrastGrade.fromIndex(defaultContrastGradeIndex)
        set(value) { defaultContrastGradeIndex = value.index }
```

- [ ] **Step 2.2: Vérifier que les tests existants passent toujours**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.storage.StorageServiceTest"
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt
git commit -m "feat: add defaultContrastGrade computed property to PreferenceManager"
```

---

## Task 3: CountdownTimer — moteur pur Kotlin

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/CountdownTimer.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/system/CountdownTimerTest.kt`

- [ ] **Step 3.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/system/CountdownTimerTest.kt
package fr.mathgl.darkroomtimer.system

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CountdownTimerTest {

    private var fakeNow = 0L
    private lateinit var timer: CountdownTimer

    @Before
    fun setup() {
        fakeNow = 0L
        timer = CountdownTimer(clock = { fakeNow })
    }

    // --- État initial ---

    @Test
    fun `initial state is STOPPED`() {
        assertEquals(TimerState.STOPPED, timer.state)
        assertFalse(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `initial remaining equals configuredTime`() {
        timer.configuredTimeMs = 8000L
        assertEquals(8000L, timer.remainingMs())
    }

    // --- Transitions d'état ---

    @Test
    fun `start transitions from STOPPED to RUNNING`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        assertEquals(TimerState.RUNNING, timer.state)
        assertTrue(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `pause transitions from RUNNING to PAUSED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        assertEquals(TimerState.PAUSED, timer.state)
        assertTrue(timer.isStarted)
        assertTrue(timer.isPaused)
    }

    @Test
    fun `resume transitions from PAUSED to RUNNING`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        timer.resume()
        assertEquals(TimerState.RUNNING, timer.state)
        assertTrue(timer.isStarted)
        assertFalse(timer.isPaused)
    }

    @Test
    fun `stop from RUNNING transitions to STOPPED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.stop()
        assertEquals(TimerState.STOPPED, timer.state)
        assertFalse(timer.isStarted)
    }

    @Test
    fun `stop from PAUSED transitions to STOPPED`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.pause()
        timer.stop()
        assertEquals(TimerState.STOPPED, timer.state)
    }

    @Test(expected = IllegalStateException::class)
    fun `start from RUNNING throws`() {
        timer.configuredTimeMs = 8000L
        timer.start()
        timer.start()
    }

    @Test(expected = IllegalStateException::class)
    fun `pause from STOPPED throws`() {
        timer.pause()
    }

    @Test(expected = IllegalStateException::class)
    fun `resume from STOPPED throws`() {
        timer.resume()
    }

    // --- Calcul du temps restant ---

    @Test
    fun `remaining decreases while RUNNING`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        assertEquals(5000L, timer.remainingMs())
    }

    @Test
    fun `remaining is frozen while PAUSED`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        timer.pause()
        fakeNow = 6000L  // le temps passe mais le timer est pausé
        assertEquals(5000L, timer.remainingMs())
    }

    @Test
    fun `elapsed before pause is preserved after resume`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 2000L
        timer.pause()      // 2s écoulées, 6s restantes
        fakeNow = 5000L
        timer.resume()     // la pause dure 3s, non comptabilisée
        fakeNow = 7000L    // 2s de plus depuis resume
        assertEquals(4000L, timer.remainingMs())  // 8000 - 2000 - 2000 = 4000
    }

    @Test
    fun `remaining can be negative when timer overruns`() {
        timer.configuredTimeMs = 1000L
        fakeNow = 0L
        timer.start()
        fakeNow = 2000L
        assertTrue(timer.remainingMs() < 0)
    }

    // --- Détection de fin ---

    @Test
    fun `tick returns true and transitions to STOPPED when remaining reaches zero`() {
        timer.configuredTimeMs = 1000L
        fakeNow = 0L
        timer.start()
        fakeNow = 1000L
        val ended = timer.tick()
        assertTrue(ended)
        assertEquals(TimerState.STOPPED, timer.state)
    }

    @Test
    fun `tick returns false when still running`() {
        timer.configuredTimeMs = 8000L
        fakeNow = 0L
        timer.start()
        fakeNow = 3000L
        assertFalse(timer.tick())
        assertEquals(TimerState.RUNNING, timer.state)
    }

    @Test
    fun `tick does nothing when STOPPED`() {
        assertFalse(timer.tick())
        assertEquals(TimerState.STOPPED, timer.state)
    }

    // --- Limites du temps configuré ---

    @Test(expected = IllegalArgumentException::class)
    fun `configuredTime below 100ms throws`() {
        timer.configuredTimeMs = 99L
    }

    @Test(expected = IllegalArgumentException::class)
    fun `configuredTime above 999000ms throws`() {
        timer.configuredTimeMs = 999_001L
    }

    @Test
    fun `configuredTime accepts boundary values`() {
        timer.configuredTimeMs = 100L
        timer.configuredTimeMs = 999_000L
    }

    // --- Affichage MM:SS.d ---

    @Test
    fun `formatTime returns MM_SS_d format`() {
        assertEquals("00:08.0", CountdownTimer.formatTime(8000L))
        assertEquals("01:05.4", CountdownTimer.formatTime(65400L))
        assertEquals("16:39.0", CountdownTimer.formatTime(999_000L))
        assertEquals("00:00.1", CountdownTimer.formatTime(100L))
    }

    @Test
    fun `formatTime clamps negative values to zero`() {
        assertEquals("00:00.0", CountdownTimer.formatTime(-500L))
    }
}
```

- [ ] **Step 3.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.system.CountdownTimerTest"
```
Résultat attendu : FAILED — `CountdownTimer`, `TimerState` n'existent pas.

- [ ] **Step 3.3: Implémenter RelayState**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt
package fr.mathgl.darkroomtimer.system

data class RelayState(
    val enlargerOn: Boolean,
    val safelightOn: Boolean
) {
    companion object {
        val INITIAL = RelayState(enlargerOn = false, safelightOn = true)
        val RUNNING = RelayState(enlargerOn = true, safelightOn = false)
        val IDLE    = RelayState(enlargerOn = false, safelightOn = false)
    }
}
```

- [ ] **Step 3.4: Implémenter CountdownTimer**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/system/CountdownTimer.kt
package fr.mathgl.darkroomtimer.system

enum class TimerState { STOPPED, RUNNING, PAUSED }

class CountdownTimer(private val clock: () -> Long = { System.currentTimeMillis() }) {

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
        startAt = now - (pauseAt - startAt)
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
```

- [ ] **Step 3.5: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.system.CountdownTimerTest"
```
Résultat attendu : BUILD SUCCESSFUL, tous les tests passent.

- [ ] **Step 3.6: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/CountdownTimer.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/system/CountdownTimerTest.kt
git commit -m "feat: add CountdownTimer state machine and RelayState model"
```

---

## Task 4: Dépendances ViewModel

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

`viewModelScope` vient de `lifecycle-viewmodel-ktx`. `viewModel()` dans Compose vient de `lifecycle-viewmodel-compose`.

- [ ] **Step 4.1: Ajouter les entrées dans libs.versions.toml**

Dans la section `[libraries]`, ajouter après `androidx-lifecycle-runtime-ktx` :

```toml
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```

Note : `lifecycleRuntimeKtx = "2.10.0"` — la même version s'applique aux trois artifacts lifecycle.

- [ ] **Step 4.2: Ajouter les dépendances dans app/build.gradle.kts**

Dans le bloc `dependencies`, après `implementation(libs.androidx.lifecycle.runtime.ktx)` :

```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
```

- [ ] **Step 4.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 4.4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add lifecycle-viewmodel-ktx and lifecycle-viewmodel-compose dependencies"
```

---

## Task 5: CountdownViewModel

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`

Le ViewModel orchestre le `CountdownTimer` via une coroutine de tick à ~50 ms, et expose un `StateFlow<CountdownUiState>` pour l'écran.

- [ ] **Step 5.1: Implémenter CountdownViewModel**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
package fr.mathgl.darkroomtimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CountdownUiState(
    val displayTime: String,
    val timerState: TimerState,
    val relayState: RelayState,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long
)

class CountdownViewModel : ViewModel() {

    private val timer = CountdownTimer()
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.start()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(maxOf(0L, timer.remainingMs())),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                if (ended) {
                    tickJob = null
                    break
                }
            }
        }
    }

    fun pause() {
        if (timer.state != TimerState.RUNNING) return
        timer.pause()
        tickJob?.cancel()
        tickJob = null
        _uiState.value = currentState().copy(
            timerState = TimerState.PAUSED,
            relayState = RelayState.IDLE
        )
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(maxOf(0L, timer.remainingMs())),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                if (ended) {
                    tickJob = null
                    break
                }
            }
        }
    }

    fun stop() {
        if (timer.state == TimerState.STOPPED) return
        tickJob?.cancel()
        tickJob = null
        timer.stop()
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.IDLE
        )
    }

    fun adjustTime(deltaMs: Long) {
        if (timer.state == TimerState.RUNNING) return
        val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
        timer.configuredTimeMs = newTime
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(newTime),
            configuredTimeMs = newTime
        )
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.value = currentState().copy(selectedGrade = grade)
    }

    private fun currentState() = _uiState.value

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
```

- [ ] **Step 5.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 5.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "feat: add CountdownViewModel with StateFlow-based UI state"
```

---

## Task 6: GradeSelector Composable

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/GradeSelector.kt`

Sélecteur horizontal scrollable avec 12 grades. Le grade sélectionné est mis en surbrillance.

- [ ] **Step 6.1: Implémenter GradeSelector**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/GradeSelector.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.ContrastGrade

@Composable
fun GradeSelector(
    selectedGrade: ContrastGrade,
    onGradeSelected: (ContrastGrade) -> Unit,
    modifier: Modifier = Modifier
) {
    val grades = ContrastGrade.entries
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedGrade.index)

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(grades) { grade ->
            GradeItem(
                grade = grade,
                isSelected = grade == selectedGrade,
                onClick = { onGradeSelected(grade) }
            )
        }
    }
}

@Composable
private fun GradeItem(
    grade: ContrastGrade,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFCC2200) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFFCC2200)
    val borderColor = Color(0xFFCC2200)

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade.label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
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
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/GradeSelector.kt
git commit -m "feat: add GradeSelector composable with 12-grade horizontal carousel"
```

---

## Task 7: CountdownScreen

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

Écran complet : affichage timer MM:SS.d, boutons de contrôle, indicateurs relais visuels, sélecteur de grade.

- [ ] **Step 7.1: Implémenter CountdownScreen**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.mathgl.darkroomtimer.system.TimerState

@Composable
fun CountdownScreen(
    viewModel: CountdownViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // --- Affichage du temps ---
        Text(
            text = state.displayTime,
            fontSize = 80.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Grade sélectionné (affiché en grand) ---
        Text(
            text = "Grade ${state.selectedGrade.label}",
            fontSize = 24.sp,
            color = Color(0xFFCC2200)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Sélecteur de grade ---
        GradeSelector(
            selectedGrade = state.selectedGrade,
            onGradeSelected = { viewModel.selectGrade(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Indicateurs relais ---
        RelayIndicators(
            enlargerOn = state.relayState.enlargerOn,
            safelightOn = state.relayState.safelightOn
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Ajustement du temps (disponible si STOPPED ou PAUSED) ---
        if (state.timerState != TimerState.RUNNING) {
            TimeAdjustRow(
                onAdjust = { viewModel.adjustTime(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- Boutons de contrôle ---
        TimerControlButtons(
            timerState = state.timerState,
            onStart = { viewModel.start() },
            onPause = { viewModel.pause() },
            onResume = { viewModel.resume() },
            onStop = { viewModel.stop() }
        )
    }
}

@Composable
private fun RelayIndicators(enlargerOn: Boolean, safelightOn: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        RelayBadge(label = "Agrandisseur", isOn = enlargerOn)
        RelayBadge(label = "Safelight", isOn = safelightOn)
    }
}

@Composable
private fun RelayBadge(label: String, isOn: Boolean) {
    val color = if (isOn) Color(0xFFCC2200) else Color(0xFF444444)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, shape = RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFFAAAAAA))
    }
}

@Composable
private fun TimeAdjustRow(onAdjust: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeAdjustButton(label = "+100s") { onAdjust(100_000L) }
        TimeAdjustButton(label = "+10s")  { onAdjust(10_000L) }
        TimeAdjustButton(label = "+1s")   { onAdjust(1_000L) }
        TimeAdjustButton(label = "+0.1s") { onAdjust(100L) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeAdjustButton(label = "-100s") { onAdjust(-100_000L) }
        TimeAdjustButton(label = "-10s")  { onAdjust(-10_000L) }
        TimeAdjustButton(label = "-1s")   { onAdjust(-1_000L) }
        TimeAdjustButton(label = "-0.1s") { onAdjust(-100L) }
    }
}

@Composable
private fun TimeAdjustButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCC2200)),
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp)
    }
}

@Composable
private fun TimerControlButtons(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when (timerState) {
            TimerState.STOPPED -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200)),
                    modifier = Modifier.height(56.dp).width(160.dp)
                ) {
                    Text("START", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            TimerState.RUNNING -> {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400)),
                    modifier = Modifier.height(56.dp).width(120.dp)
                ) {
                    Text("PAUSE", fontSize = 18.sp)
                }
            }
            TimerState.PAUSED -> {
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200)),
                    modifier = Modifier.height(56.dp).width(120.dp)
                ) {
                    Text("RESUME", fontSize = 18.sp)
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444)),
                    modifier = Modifier.height(56.dp).width(80.dp)
                ) {
                    Text("STOP", fontSize = 16.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 7.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 7.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt
git commit -m "feat: add CountdownScreen with timer display, grade selector, and relay indicators"
```

---

## Task 8: ForegroundTimerService

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/ForegroundTimerService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

Le service maintient une notification persistante avec le temps restant pendant RUNNING. Il reçoit des Intents pour démarrer/stopper.

- [ ] **Step 8.1: Implémenter ForegroundTimerService**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/system/ForegroundTimerService.kt
package fr.mathgl.darkroomtimer.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fr.mathgl.darkroomtimer.MainActivity
import fr.mathgl.darkroomtimer.R

class ForegroundTimerService : Service() {

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING_MS, 0L)
                startForeground(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_UPDATE -> {
                val remaining = intent.getLongExtra(EXTRA_REMAINING_MS, 0L)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(remaining))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(remainingMs: Long) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Exposition en cours")
            .setContentText(CountdownTimer.formatTime(maxOf(0L, remainingMs)))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer exposition",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START  = "fr.mathgl.darkroomtimer.TIMER_START"
        const val ACTION_UPDATE = "fr.mathgl.darkroomtimer.TIMER_UPDATE"
        const val ACTION_STOP   = "fr.mathgl.darkroomtimer.TIMER_STOP"
        const val EXTRA_REMAINING_MS = "remaining_ms"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "darkroom_timer_channel"
    }
}
```

- [ ] **Step 8.2: Déclarer le service dans AndroidManifest.xml**

Ajouter à l'intérieur du bloc `<application>`, avant `</application>` :

```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
```

Et dans `<application>` :

```xml
        <service
            android:name=".system.ForegroundTimerService"
            android:foregroundServiceType="mediaProjection"
            android:exported="false" />
```

Le résultat complet du manifeste doit être :

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.DarkroomTimer">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.DarkroomTimer">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <service
            android:name=".system.ForegroundTimerService"
            android:foregroundServiceType="mediaProjection"
            android:exported="false" />
    </application>

</manifest>
```

- [ ] **Step 8.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 8.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/ForegroundTimerService.kt \
        app/src/main/AndroidManifest.xml
git commit -m "feat: add ForegroundTimerService for background timer stability"
```

---

## Task 9: Intégration ForegroundService dans ViewModel + MainActivity

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

Le ViewModel doit notifier le Service au démarrage, à chaque tick et à l'arrêt. Le ViewModel a besoin d'un `Context` pour lancer le service — on l'injecte via une `ViewModelFactory` ou `AndroidViewModel`.

- [ ] **Step 9.1: Remplacer CountdownViewModel par AndroidViewModel**

Remplacer tout le contenu de `CountdownViewModel.kt` par :

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
package fr.mathgl.darkroomtimer.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.system.CountdownTimer
import fr.mathgl.darkroomtimer.system.ForegroundTimerService
import fr.mathgl.darkroomtimer.system.RelayState
import fr.mathgl.darkroomtimer.system.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CountdownUiState(
    val displayTime: String,
    val timerState: TimerState,
    val relayState: RelayState,
    val selectedGrade: ContrastGrade,
    val configuredTimeMs: Long
)

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val timer = CountdownTimer()
    private var tickJob: Job? = null

    private val _uiState = MutableStateFlow(
        CountdownUiState(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.INITIAL,
            selectedGrade = ContrastGrade.DEFAULT,
            configuredTimeMs = timer.configuredTimeMs
        )
    )
    val uiState: StateFlow<CountdownUiState> = _uiState.asStateFlow()

    fun start() {
        if (timer.state != TimerState.STOPPED) return
        timer.start()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                sendServiceIntent(
                    if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                    remaining
                )
                if (ended) { tickJob = null; break }
            }
        }
    }

    fun pause() {
        if (timer.state != TimerState.RUNNING) return
        timer.pause()
        tickJob?.cancel(); tickJob = null
        _uiState.value = currentState().copy(
            timerState = TimerState.PAUSED,
            relayState = RelayState.IDLE
        )
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun resume() {
        if (timer.state != TimerState.PAUSED) return
        timer.resume()
        _uiState.value = currentState().copy(
            timerState = TimerState.RUNNING,
            relayState = RelayState.RUNNING
        )
        sendServiceIntent(ForegroundTimerService.ACTION_START, timer.remainingMs())
        tickJob = viewModelScope.launch {
            while (true) {
                delay(50L)
                val ended = timer.tick()
                val remaining = maxOf(0L, timer.remainingMs())
                _uiState.value = currentState().copy(
                    displayTime = CountdownTimer.formatTime(remaining),
                    timerState = timer.state,
                    relayState = if (timer.state == TimerState.RUNNING) RelayState.RUNNING else RelayState.IDLE
                )
                sendServiceIntent(
                    if (ended) ForegroundTimerService.ACTION_STOP else ForegroundTimerService.ACTION_UPDATE,
                    remaining
                )
                if (ended) { tickJob = null; break }
            }
        }
    }

    fun stop() {
        if (timer.state == TimerState.STOPPED) return
        tickJob?.cancel(); tickJob = null
        timer.stop()
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
            timerState = TimerState.STOPPED,
            relayState = RelayState.IDLE
        )
        sendServiceIntent(ForegroundTimerService.ACTION_STOP, 0L)
    }

    fun adjustTime(deltaMs: Long) {
        if (timer.state == TimerState.RUNNING) return
        val newTime = (timer.configuredTimeMs + deltaMs).coerceIn(100L, 999_000L)
        timer.configuredTimeMs = newTime
        _uiState.value = currentState().copy(
            displayTime = CountdownTimer.formatTime(newTime),
            configuredTimeMs = newTime
        )
    }

    fun selectGrade(grade: ContrastGrade) {
        _uiState.value = currentState().copy(selectedGrade = grade)
    }

    private fun currentState() = _uiState.value

    private fun sendServiceIntent(action: String, remainingMs: Long) {
        val intent = Intent(getApplication(), ForegroundTimerService::class.java).apply {
            this.action = action
            putExtra(ForegroundTimerService.EXTRA_REMAINING_MS, remainingMs)
        }
        getApplication<Application>().startForegroundService(intent)
    }

    override fun onCleared() {
        super.onCleared()
        tickJob?.cancel()
    }
}
```

- [ ] **Step 9.2: Mettre à jour MainActivity pour afficher CountdownScreen**

Remplacer tout le contenu de `MainActivity.kt` par :

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.CountdownScreen
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)

        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    CountdownScreen()
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
```

- [ ] **Step 9.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 9.4: Lancer tous les tests unitaires**

```
./gradlew test
```
Résultat attendu : BUILD SUCCESSFUL, tous les tests passent (ContrastGradeTest, CountdownTimerTest, FStopMathTest, StorageServiceTest, LuminosityManagerTest).

- [ ] **Step 9.5: Commit final**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "feat: wire CountdownScreen and ForegroundTimerService into MainActivity"
```

---

## Récapitulatif des fichiers

| Fichier | Action |
|---|---|
| `math/ContrastGrade.kt` | Créé |
| `system/CountdownTimer.kt` | Créé |
| `system/RelayState.kt` | Créé |
| `system/ForegroundTimerService.kt` | Créé |
| `ui/CountdownViewModel.kt` | Créé |
| `ui/GradeSelector.kt` | Créé |
| `ui/CountdownScreen.kt` | Créé |
| `storage/PreferenceManager.kt` | Modifié (+`defaultContrastGrade`) |
| `gradle/libs.versions.toml` | Modifié (+viewmodel) |
| `app/build.gradle.kts` | Modifié (+viewmodel) |
| `AndroidManifest.xml` | Modifié (+service +permissions) |
| `MainActivity.kt` | Modifié (→ CountdownScreen) |
| `test/.../ContrastGradeTest.kt` | Créé |
| `test/.../CountdownTimerTest.kt` | Créé |

---

## Vérification manuelle post-implémentation

Installer l'APK sur un émulateur ou appareil physique :

```
./gradlew installDebug
```

Vérifier :
1. L'écran affiche `00:08.0` au démarrage (temps par défaut)
2. Le grade sélectionné par défaut est `2`
3. Les boutons +/− ajustent le temps correctement
4. `START` lance le décompte, le timer défile
5. `PAUSE` fige le timer, `RESUME` reprend
6. `STOP` remet le timer à `00:08.0`
7. Le timer s'arrête automatiquement à `00:00.0`
8. L'indicateur Agrandisseur est rouge pendant RUNNING
9. La notification apparaît pendant RUNNING et disparaît à l'arrêt
10. Le timer continue de fonctionner en arrière-plan (basculer sur une autre app)
