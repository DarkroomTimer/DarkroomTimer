# Remaining Spec Features — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two silent bugs (audio dead code + relay stays ON after natural timer completion), implement the missing CountdownScreen UX from spec (defaults from prefs, tap-to-edit time, relay override), add luminosity settings, and wire the existing StorageService to export/import buttons in SettingsScreen.

**Architecture:** All changes are contained in existing files. No new files needed. WiFi relay configuration UI is intentionally excluded — it is a separate plan.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, SharedPreferences (PreferenceManager), Room, Coroutines, Android ActivityResultContracts (file picker)

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `ui/CountdownViewModel.kt` | Modifier | Bugs audio + relay, defaults depuis prefs, override relais |
| `ui/CountdownScreen.kt` | Modifier | Éditeur de temps (tap), badges relais cliquables |
| `ui/TeststripViewModel.kt` | Modifier | Bug audio init |
| `ui/SettingsScreen.kt` | Modifier | Section luminosité + boutons export/import |
| `storage/PreferenceManager.kt` | Modifier | Clés préférences luminosité |
| `storage/StorageService.kt` | Modifier | Bug validation profil "Idéal" (all-zeros) |
| `MainActivity.kt` | Modifier | Câblage LuminosityManager.Config depuis prefs |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/storage/StorageService.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

---

## Context: État actuel vs spec

**Implémenté :** CountdownTimer, FStopMath, TeststripEngine, ContrastGrade, BurnDodgeManager, AudioSystem/AudioEngine/MetronomeController, tous les drivers relais (Null/Demo/Tasmota/ESPHome), ForegroundTimerService, LuminosityManager, Room DB, PreferenceManager (toutes clés), StorageService (logique export/import), tous les écrans UI, NavigationBar 3 onglets.

**Manquant (ce plan) :**
- Bug : `getAudioSystem()` jamais appelé → audio silencieux partout
- Bug : fin naturelle du timer ne coupe pas le relais ni ne joue le bip
- Bug : `StorageService` refuse les profils avec `riseTimeMs=0` (dont "Idéal")
- CountdownViewModel : temps par défaut et grade initial hardcodés (ignorent les prefs)
- CountdownScreen : tap sur le temps ne fait rien (spec : Bottom Sheet éditeur)
- CountdownScreen : badges relais non cliquables (spec : override manuel)
- SettingsScreen : section "LUMINOSITÉ" absente (LuminosityManager hardcodé)
- SettingsScreen : boutons export/import sont des placeholders "à venir"
- MainActivity : `LuminosityManager` démarre avec config hardcodée, ignorant les prefs

**Non traité dans ce plan (plan séparé) :**
- Configuration UI WiFi relais (Tasmota/ESPHome) — grand feature indépendant
- Indicateur Bluetooth en barre supérieure
- Mode Ne Pas Déranger

---

## Task 1: Fix audio initialization in CountdownViewModel

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`

**Context:** `getAudioSystem()` est défini mais jamais appelé. `audioSystem` reste `null`, donc `audioSystem?.startExposure()` etc. ne font rien. Toutes les fonctions audio (métronome, bips de fin) sont silencieuses. Le fix : supprimer `getAudioSystem()` et initialiser `audioSystem` dans `init {}`.

- [ ] **Step 1.1: Supprimer getAudioSystem() et ajouter initAudio() dans init {}**

Remplacer dans `CountdownViewModel.kt` le bloc suivant :
```kotlin
private fun getAudioSystem(): AudioSystem {
    if (audioSystem == null) {
        try {
            val context = getApplication<Application>()
            val preferenceManager = PreferenceManager.getInstance(context)
            val audioPreferences = AudioPreferences(preferenceManager.prefs)
            val audioEngine = ToneGeneratorAudioEngine(audioPreferences.buzzerVolume)
            audioSystem = AudioSystem(audioEngine, audioPreferences, audioPreferences.buzzerVolume)
        } catch (e: Exception) {
            // In test environments or when prefs are unavailable, audioSystem remains null
            // Audio operations will be silently skipped
        }
    }
    return audioSystem!!
}
```

par (supprimer complètement — ne garder que rien à cet endroit), et dans le bloc `init {}` ajouter l'initialisation audio **avant** la collection de `relayStates` :

```kotlin
init {
    // Initialize audio from preferences
    try {
        val context = getApplication<Application>()
        val preferenceManager = PreferenceManager.getInstance(context)
        val audioPreferences = AudioPreferences(preferenceManager.prefs)
        val audioEngine = ToneGeneratorAudioEngine(audioPreferences.buzzerVolume)
        audioSystem = AudioSystem(audioEngine, audioPreferences, audioPreferences.buzzerVolume)
    } catch (e: Exception) {
        // audio unavailable in test environment
    }

    viewModelScope.launch {
        relaySystem.relayStates.collect { relayState ->
            _uiState.update { it.copy(relayState = relayState) }
        }
    }
}
```

- [ ] **Step 1.2: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 1.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "fix: initialize AudioSystem in CountdownViewModel init (was dead code)"
```

---

## Task 2: Fix audio initialization in TeststripViewModel

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`

**Context:** Même bug que Task 1 — `getAudioSystem()` existe mais n'est jamais appelé dans TeststripViewModel.

- [ ] **Step 2.1: Supprimer getAudioSystem() et initialiser l'audio dans init {}**

Lire la méthode `getAudioSystem()` dans TeststripViewModel (vers la ligne 50) et la supprimer. Puis dans le bloc `init {}` (qui doit déjà exister pour les LiveData/StateFlow), ajouter en premier :

```kotlin
init {
    // Initialize audio from preferences
    try {
        val context = getApplication<Application>()
        val preferenceManager = PreferenceManager.getInstance(context)
        val audioPreferences = AudioPreferences(preferenceManager.prefs)
        val audioEngine = ToneGeneratorAudioEngine(audioPreferences.buzzerVolume)
        audioSystem = AudioSystem(audioEngine, audioPreferences, audioPreferences.buzzerVolume)
    } catch (e: Exception) {
        // audio unavailable in test environment
    }
    // ... reste du init existant
}
```

- [ ] **Step 2.2: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt
git commit -m "fix: initialize AudioSystem in TeststripViewModel init (was dead code)"
```

---

## Task 3: Fix natural timer completion (relay + audio)

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`

**Context:** Quand `timer.tick()` retourne `true` (expostion terminée), le tick loop `break`s sans : (a) couper le relais agrandisseur, (b) jouer le bip de fin. Le relais reste ON visuellement. Ce bug est présent dans `start()` ET dans `resume()` (deux boucles de tick identiques).

La boucle actuelle dans `start()` se termine par :
```kotlin
if (ended) { tickJob = null; break }
```

- [ ] **Step 3.1: Ajouter le cleanup relais + audio à la fin naturelle dans start()**

Remplacer :
```kotlin
if (ended) { tickJob = null; break }
```
par (dans la boucle de `start()`) :
```kotlin
if (ended) {
    viewModelScope.launch {
        relaySystem.setEnlarger(false)
        relaySystem.setSafelight(false)
    }
    audioSystem?.stopExposure()
    tickJob = null
    break
}
```

- [ ] **Step 3.2: Même fix dans resume()**

La boucle de `resume()` a le même pattern. Même remplacement :
```kotlin
if (ended) {
    viewModelScope.launch {
        relaySystem.setEnlarger(false)
        relaySystem.setSafelight(false)
    }
    audioSystem?.stopExposure()
    tickJob = null
    break
}
```

- [ ] **Step 3.3: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 3.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "fix: turn off relay and play stop beep on natural timer completion"
```

---

## Task 4: Load countdown defaults from PreferenceManager

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`

**Context:** `timer.configuredTimeMs` démarre à 8000ms (hardcodé dans `CountdownTimer`). Le grade affiché est `ContrastGrade.DEFAULT`. Les deux doivent être lus depuis `PreferenceManager` au démarrage du ViewModel. La mise à jour doit se faire dans `init {}` avant la collecte de `relayStates`, en modifiant `timer.configuredTimeMs` puis en mettant à jour `_uiState`.

- [ ] **Step 4.1: Lire les préférences dans init {} et mettre à jour l'état initial**

Dans `init {}`, juste **après** l'initialisation audio (Task 1) et **avant** le `viewModelScope.launch`, ajouter :

```kotlin
// Load defaults from preferences
val context = getApplication<Application>()
val prefs = PreferenceManager.getInstance(context)
timer.configuredTimeMs = prefs.defaultExposureMs
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
    configuredTimeMs = timer.configuredTimeMs,
    selectedGrade = prefs.defaultContrastGrade
) }
```

Le bloc `init {}` complet doit être dans cet ordre :
1. Init audio (Task 1)
2. Load defaults from prefs (ce step)
3. `viewModelScope.launch { relaySystem.relayStates.collect { ... } }`

- [ ] **Step 4.2: Compiler et vérifier**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL. Vérifier que `CountdownTimer.configuredTimeMs` supporte la valeur lue depuis les prefs (doit être dans `[100, 999000]` — la valeur par défaut 8000 l'est).

- [ ] **Step 4.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "feat: load default exposure time and contrast grade from preferences"
```

---

## Task 5: Tap-on-time → Bottom Sheet éditeur

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

**Context:** Spec : "Tap sur le temps → Bottom Sheet Dialog avec champ de saisie numérique (MM:SS.d)". Actuellement le temps affiché n'est pas cliquable. Le dialog doit être accessible uniquement en état STOPPED ou PAUSED.

- [ ] **Step 5.1: Ajouter showTimeEditor à CountdownUiState**

Dans `CountdownViewModel.kt`, modifier `CountdownUiState` pour ajouter :
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
    val showTimeEditor: Boolean = false   // ← ajouter
)
```

- [ ] **Step 5.2: Ajouter openTimeEditor(), closeTimeEditor(), setTimeFromInput() au VM**

Ajouter après `adjustTime()` dans `CountdownViewModel.kt` :

```kotlin
fun openTimeEditor() {
    if (_uiState.value.timerState == TimerState.RUNNING) return
    _uiState.update { it.copy(showTimeEditor = true) }
}

fun closeTimeEditor() {
    _uiState.update { it.copy(showTimeEditor = false) }
}

fun setTimeFromInput(minutes: Int, seconds: Int, tenths: Int) {
    val newTime = (minutes * 60_000L + seconds * 1_000L + tenths * 100L).coerceIn(100L, 999_000L)
    timer.configuredTimeMs = newTime
    _uiState.update { it.copy(
        displayTime = CountdownTimer.formatTime(newTime),
        configuredTimeMs = newTime,
        showTimeEditor = false
    ) }
}
```

- [ ] **Step 5.3: Rendre le temps cliquable dans CountdownScreen.kt**

Remplacer le `Text` du temps :
```kotlin
Text(
    text = state.displayTime,
    fontSize = 80.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = Color.White
)
```

par :
```kotlin
Text(
    text = state.displayTime,
    fontSize = 80.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    color = Color.White,
    modifier = Modifier.clickable(
        enabled = state.timerState != TimerState.RUNNING
    ) { viewModel.openTimeEditor() }
)
```

Ajouter l'import `import androidx.compose.foundation.clickable` si absent.

- [ ] **Step 5.4: Ajouter le Bottom Sheet dialog TimeEditorSheet**

Dans `CountdownScreen.kt`, ajouter en bas du fichier (avant la dernière accolade) :

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeEditorSheet(
    currentMs: Long,
    onConfirm: (minutes: Int, seconds: Int, tenths: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val totalTenths = currentMs / 100
    val initTenths = (totalTenths % 10).toInt()
    val totalSeconds = totalTenths / 10
    val initSeconds = (totalSeconds % 60).toInt()
    val initMinutes = (totalSeconds / 60).toInt()

    var minutes by remember { mutableStateOf(initMinutes) }
    var seconds by remember { mutableStateOf(initSeconds) }
    var tenths  by remember { mutableStateOf(initTenths) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF111111)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Régler le temps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeSpinner(label = "min", value = minutes, range = 0..16, onValueChange = { minutes = it })
                Text(":", color = Color.White, fontSize = 32.sp)
                TimeSpinner(label = "s", value = seconds, range = 0..59, onValueChange = { seconds = it })
                Text(".", color = Color.White, fontSize = 32.sp)
                TimeSpinner(label = "1/10", value = tenths, range = 0..9, onValueChange = { tenths = it })
            }

            Text(
                text = CountdownTimer.formatTime(minutes * 60_000L + seconds * 1_000L + tenths * 100L),
                color = Color(0xFFCC2200),
                fontSize = 48.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onDismiss) {
                    Text("Annuler", color = Color.Gray)
                }
                Button(
                    onClick = { onConfirm(minutes, seconds, tenths) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                ) {
                    Text("Valider")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimeSpinner(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
            Text("▲", color = Color(0xFFCC2200), fontSize = 16.sp)
        }
        Text(
            text = "%02d".format(value),
            color = Color.White,
            fontSize = 32.sp,
            fontFamily = FontFamily.Monospace
        )
        IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
            Text("▼", color = Color(0xFFCC2200), fontSize = 16.sp)
        }
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}
```

- [ ] **Step 5.5: Câbler le Bottom Sheet dans CountdownScreen()**

Dans le composable `CountdownScreen()`, après le `if (showBurnDodgeDialog)` bloc, ajouter :

```kotlin
if (state.showTimeEditor) {
    TimeEditorSheet(
        currentMs = state.configuredTimeMs,
        onConfirm = { m, s, t -> viewModel.setTimeFromInput(m, s, t) },
        onDismiss = { viewModel.closeTimeEditor() }
    )
}
```

Ajouter les imports nécessaires :
```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
```

- [ ] **Step 5.6: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 5.7: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "feat: add tap-on-time bottom sheet editor for countdown time input"
```

---

## Task 6: Relay indicator override toggle

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

**Context:** Spec : "Tap sur l'indicateur agrandisseur → override relais (toggle forcé)" et "Les overrides sont actifs uniquement en état STOPPED ou PAUSED". Au démarrage du timer, les overrides sont annulés. L'indicateur montre un badge "override" si actif.

- [ ] **Step 6.1: Ajouter overrideEnlarger et overrideSafelight à CountdownUiState**

Dans `CountdownViewModel.kt`, ajouter dans `CountdownUiState` :
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
    val enlargerOverride: Boolean = false,    // ← ajouter
    val safelightOverride: Boolean = false    // ← ajouter
)
```

- [ ] **Step 6.2: Ajouter toggleEnlargerOverride() et toggleSafelightOverride() au VM**

Ajouter dans `CountdownViewModel.kt` après `selectGrade()` :

```kotlin
fun toggleEnlargerOverride() {
    if (_uiState.value.timerState == TimerState.RUNNING) return
    val newOverride = !_uiState.value.enlargerOverride
    viewModelScope.launch { relaySystem.setEnlarger(newOverride) }
    _uiState.update { it.copy(enlargerOverride = newOverride) }
}

fun toggleSafelightOverride() {
    if (_uiState.value.timerState == TimerState.RUNNING) return
    val newOverride = !_uiState.value.safelightOverride
    viewModelScope.launch { relaySystem.setSafelight(newOverride) }
    _uiState.update { it.copy(safelightOverride = newOverride) }
}
```

- [ ] **Step 6.3: Annuler les overrides au start() et stop()**

Dans `start()`, modifier le `_uiState.update` :
```kotlin
_uiState.update { it.copy(
    timerState = TimerState.RUNNING,
    enlargerOverride = false,
    safelightOverride = false
) }
```

Dans `stop()`, modifier le `_uiState.update` :
```kotlin
_uiState.update { it.copy(
    displayTime = CountdownTimer.formatTime(timer.configuredTimeMs),
    timerState = TimerState.STOPPED,
    enlargerOverride = false,
    safelightOverride = false
) }
```

- [ ] **Step 6.4: Mettre à jour RelayIndicators et RelayBadge dans CountdownScreen.kt**

Remplacer la fonction `RelayIndicators` :
```kotlin
@Composable
private fun RelayIndicators(
    enlargerOn: Boolean,
    safelightOn: Boolean,
    enlargerOverride: Boolean,
    safelightOverride: Boolean,
    overrideEnabled: Boolean,
    onToggleEnlarger: () -> Unit,
    onToggleSafelight: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        RelayBadge(
            label = "Agrandisseur",
            isOn = enlargerOn,
            hasOverride = enlargerOverride,
            clickEnabled = overrideEnabled,
            onClick = onToggleEnlarger
        )
        RelayBadge(
            label = "Safelight",
            isOn = safelightOn,
            hasOverride = safelightOverride,
            clickEnabled = overrideEnabled,
            onClick = onToggleSafelight
        )
    }
}
```

Remplacer la fonction `RelayBadge` :
```kotlin
@Composable
private fun RelayBadge(
    label: String,
    isOn: Boolean,
    hasOverride: Boolean,
    clickEnabled: Boolean,
    onClick: () -> Unit
) {
    val dotColor = if (isOn) Color(0xFFCC2200) else Color(0xFF444444)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = clickEnabled, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(dotColor, shape = RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color(0xFFAAAAAA))
        if (hasOverride) {
            Text(text = "override", fontSize = 9.sp, color = Color(0xFFCC2200))
        }
    }
}
```

Mettre à jour l'appel dans `CountdownScreen()` :
```kotlin
RelayIndicators(
    enlargerOn = state.relayState.enlarger == RelayState.ON,
    safelightOn = state.relayState.safelight == RelayState.ON,
    enlargerOverride = state.enlargerOverride,
    safelightOverride = state.safelightOverride,
    overrideEnabled = state.timerState != TimerState.RUNNING,
    onToggleEnlarger = { viewModel.toggleEnlargerOverride() },
    onToggleSafelight = { viewModel.toggleSafelightOverride() }
)
```

Ajouter l'import `import androidx.compose.foundation.clickable` si absent.

- [ ] **Step 6.5: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 6.6: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "feat: add relay indicator override toggle in CountdownScreen"
```

---

## Task 7: Luminosity settings in PreferenceManager + SettingsScreen

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

**Context:** `LuminosityManager` existe et est câblé dans `MainActivity`, mais sa `Config` est hardcodée (`minBrightness=0.01f, maxBrightness=0.1f, fixedBrightness=0.05f`). Il faut : (1) stocker ces valeurs dans les prefs, (2) les exposer dans SettingsScreen, (3) les lire depuis les prefs dans MainActivity au démarrage.

- [ ] **Step 7.1: Ajouter les clés luminosité dans PreferenceManager.kt**

Ajouter dans le bloc `companion object` de `PreferenceManager` :
```kotlin
private const val KEY_LUMINOSITY_MODE = "pref_luminosity_mode"
private const val KEY_LUMINOSITY_MIN = "pref_luminosity_min"
private const val KEY_LUMINOSITY_MAX = "pref_luminosity_max"
private const val KEY_LUMINOSITY_FIXED = "pref_luminosity_fixed"
```

Ajouter les propriétés dans le corps de la classe (après `enlargerProfileId`) :
```kotlin
// Luminosity Settings
var luminosityMode: String
    get() = prefs.getString(KEY_LUMINOSITY_MODE, "ADAPTIVE") ?: "ADAPTIVE"
    set(value) = prefs.edit().putString(KEY_LUMINOSITY_MODE, value).apply()

var luminosityMin: Float
    get() = prefs.getFloat(KEY_LUMINOSITY_MIN, 0.01f)
    set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_MIN, value).apply()

var luminosityMax: Float
    get() = prefs.getFloat(KEY_LUMINOSITY_MAX, 0.10f)
    set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_MAX, value).apply()

var luminosityFixed: Float
    get() = prefs.getFloat(KEY_LUMINOSITY_FIXED, 0.05f)
    set(value) = prefs.edit().putFloat(KEY_LUMINOSITY_FIXED, value).apply()
```

- [ ] **Step 7.2: Ajouter la section LUMINOSITÉ dans SettingsScreen.kt**

Dans `SettingsScreen()`, ajouter les variables d'état pour la luminosité après les autres variables :
```kotlin
var luminosityMode by remember { mutableStateOf(prefs.luminosityMode) }
var luminosityMin by remember { mutableStateOf(prefs.luminosityMin) }
var luminosityMax by remember { mutableStateOf(prefs.luminosityMax) }
var luminosityFixed by remember { mutableStateOf(prefs.luminosityFixed) }
```

Ajouter la section après `// AGRANDISSEUR` et avant `// BLUETOOTH` :
```kotlin
// LUMINOSITÉ ÉCRAN
SettingsSectionHeader("LUMINOSITÉ ÉCRAN")
SettingsDropdown(
    label = "Mode",
    options = listOf("ADAPTIVE", "FIXED"),
    selected = luminosityMode,
    onSelect = { luminosityMode = it; prefs.luminosityMode = it }
)
if (luminosityMode == "ADAPTIVE") {
    LuminositySlider(
        label = "Minimum : ${(luminosityMin * 100).toInt()}%",
        value = luminosityMin,
        onValueChange = { luminosityMin = it; prefs.luminosityMin = it }
    )
    LuminositySlider(
        label = "Maximum : ${(luminosityMax * 100).toInt()}%",
        value = luminosityMax,
        onValueChange = { luminosityMax = it; prefs.luminosityMax = it }
    )
} else {
    LuminositySlider(
        label = "Luminosité fixe : ${(luminosityFixed * 100).toInt()}%",
        value = luminosityFixed,
        onValueChange = { luminosityFixed = it; prefs.luminosityFixed = it }
    )
}
```

Ajouter en bas du fichier `SettingsScreen.kt` le composable `LuminositySlider` :
```kotlin
@Composable
private fun LuminositySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFCC2200),
                activeTrackColor = Color(0xFFCC2200)
            )
        )
    }
}
```

- [ ] **Step 7.3: Lire les prefs luminosité dans MainActivity.onStart()**

Dans `MainActivity.kt`, modifier `onStart()` :
```kotlin
override fun onStart() {
    super.onStart()
    val prefs = fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(this)
    luminosityManager.setConfig(
        fr.mathgl.darkroomtimer.system.LuminosityManager.Config(
            mode = if (prefs.luminosityMode == "FIXED")
                fr.mathgl.darkroomtimer.system.LuminosityManager.Mode.FIXED
            else
                fr.mathgl.darkroomtimer.system.LuminosityManager.Mode.ADAPTIVE,
            minBrightness = prefs.luminosityMin,
            maxBrightness = prefs.luminosityMax,
            fixedBrightness = prefs.luminosityFixed
        )
    )
    luminosityManager.start()
}
```

Si `LuminosityManager` et `PreferenceManager` sont déjà importés en haut du fichier, utiliser les noms courts. Sinon ajouter les imports.

- [ ] **Step 7.4: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 7.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "feat: add luminosity settings in SettingsScreen wired to LuminosityManager"
```

---

## Task 8: Fix StorageService validation bug

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/storage/StorageService.kt`

**Context:** `validateBackupData()` throw pour les profils avec `riseTimeMs=0` ET `riseTimeEquivMs=0` (condition `0 >= 0` → true). Cela rejette le profil "Idéal" (tous les champs à 0), rendant l'import impossible dès le premier export/import. La règle s'applique uniquement si `riseTimeMs > 0`.

- [ ] **Step 8.1: Corriger la validation dans validateBackupData()**

Remplacer dans `StorageService.validateBackupData()` :
```kotlin
if (profile.riseTimeEquivMs >= profile.riseTimeMs) {
    throw IllegalArgumentException("Profile ${profile.name}: riseTimeEquivMs must be less than riseTimeMs")
}
if (profile.fallTimeEquivMs >= profile.fallTimeMs) {
    throw IllegalArgumentException("Profile ${profile.name}: fallTimeEquivMs must be less than fallTimeMs")
}
```

par :
```kotlin
if (profile.riseTimeMs > 0 && profile.riseTimeEquivMs >= profile.riseTimeMs) {
    throw IllegalArgumentException("Profile ${profile.name}: riseTimeEquivMs must be less than riseTimeMs")
}
if (profile.fallTimeMs > 0 && profile.fallTimeEquivMs >= profile.fallTimeMs) {
    throw IllegalArgumentException("Profile ${profile.name}: fallTimeEquivMs must be less than fallTimeMs")
}
```

- [ ] **Step 8.2: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 8.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/storage/StorageService.kt
git commit -m "fix: allow zero riseTimeMs/fallTimeMs in StorageService validation (fixes Idéal profile import)"
```

---

## Task 9: Wire Export/Import buttons in SettingsScreen

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`

**Context:** La section DONNÉES de `SettingsScreen` a un placeholder "à venir". `StorageService` avec `exportBackup()` et `importBackup()` est entièrement implémenté. Il faut : export → `Intent.ACTION_SEND` (partage Android), import → `ActivityResultContracts.GetContent()` (sélecteur de fichier) + dialog de confirmation.

- [ ] **Step 9.1: Ajouter les imports nécessaires dans SettingsScreen.kt**

S'assurer que ces imports sont présents en haut du fichier :
```kotlin
import android.app.Application
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import fr.mathgl.darkroomtimer.storage.StorageService
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

- [ ] **Step 9.2: Remplacer le placeholder DONNÉES dans SettingsScreen()**

Remplacer le bloc DONNÉES actuel :
```kotlin
// DONNÉES
SettingsSectionHeader("DONNÉES")
Text(
    text = "Export / Import JSON — à venir.",
    color = Color.Gray,
    fontSize = 12.sp,
    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
)
```

par :
```kotlin
// DONNÉES
SettingsSectionHeader("DONNÉES")
val exportImportScope = rememberCoroutineScope()
var showImportConfirm by remember { mutableStateOf(false) }
var pendingImportJson by remember { mutableStateOf("") }
var exportError by remember { mutableStateOf("") }
var importError by remember { mutableStateOf("") }

val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    if (uri != null) {
        exportImportScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                }.getOrNull()
            } ?: ""
            if (json.isNotBlank()) {
                pendingImportJson = json
                showImportConfirm = true
            } else {
                importError = "Fichier invalide ou vide."
            }
        }
    }
}

if (exportError.isNotBlank()) {
    Text(exportError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
}
if (importError.isNotBlank()) {
    Text(importError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
}

Button(
    onClick = {
        exportImportScope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching {
                    val db = AppDatabase.getDatabase(
                        context.applicationContext as Application,
                        CoroutineScope(Dispatchers.Default)
                    )
                    StorageService(prefs, db.enlargerProfileDao()).exportBackup()
                }.getOrNull()
            }
            if (json != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                    putExtra(Intent.EXTRA_SUBJECT, "DarkroomTimer Backup")
                }
                context.startActivity(Intent.createChooser(intent, "Exporter"))
                exportError = ""
            } else {
                exportError = "Erreur lors de l'export."
            }
        }
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
) {
    Text("EXPORTER LES DONNÉES", color = Color.White, fontSize = 14.sp)
}

Spacer(modifier = Modifier.height(8.dp))

Button(
    onClick = { importError = ""; importLauncher.launch("application/json") },
    modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222))
) {
    Text("IMPORTER LES DONNÉES", color = Color.White, fontSize = 14.sp)
}

if (showImportConfirm) {
    AlertDialog(
        onDismissRequest = { showImportConfirm = false },
        title = { Text("Importer les données ?", color = Color.White) },
        text = {
            Text(
                "Cette opération va remplacer vos réglages actuels. Continuer ?",
                color = Color.White
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val json = pendingImportJson
                    showImportConfirm = false
                    exportImportScope.launch {
                        val error = withContext(Dispatchers.IO) {
                            runCatching {
                                val db = AppDatabase.getDatabase(
                                    context.applicationContext as Application,
                                    CoroutineScope(Dispatchers.Default)
                                )
                                StorageService(prefs, db.enlargerProfileDao()).importBackup(json)
                            }.exceptionOrNull()?.message
                        }
                        importError = if (error != null) "Erreur : $error" else ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
            ) {
                Text("IMPORTER")
            }
        },
        dismissButton = {
            TextButton(onClick = { showImportConfirm = false }) { Text("ANNULER") }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}
```

- [ ] **Step 9.3: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL. Si erreur de type sur `AppDatabase.getDatabase()` (signature avec `Application` vs `Context`), adapter le cast selon la signature existante dans `AppDatabase.kt`.

- [ ] **Step 9.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt
git commit -m "feat: wire export/import JSON buttons in SettingsScreen to StorageService"
```

---

## Vérification Finale

1. **Compiler** : `./gradlew assembleDebug` — BUILD SUCCESSFUL
2. **Tests** : `./gradlew test` — tous PASS
3. **Vérification visuelle** (installer l'APK) :
   - Lancer l'app → le timer affiche le temps par défaut depuis les prefs (8s par défaut)
   - Tap sur `00:08.0` → Bottom Sheet éditeur s'ouvre, permet de saisir un temps
   - Démarrer le timer → bip de démarrage (métronome si activé)
   - Laisser le timer se terminer → bip de fin joue (3 bips), relais OFF, UI passe à STOPPED
   - Tap sur badge "Agrandisseur" en état STOPPED → état ON, badge "override" visible
   - Démarrer le timer → override annulé
   - Onglet Réglages → section "LUMINOSITÉ ÉCRAN" présente avec mode et sliders
   - Onglet Réglages → section "DONNÉES" avec boutons "EXPORTER" et "IMPORTER"
   - Tap "EXPORTER" → partage Android s'ouvre avec JSON
   - Tap "IMPORTER" → sélecteur de fichier, puis dialog de confirmation

---

## Plan Suivant

**WiFi Relay Configuration** — Le plan suivant implémente :
- Section "RELAIS" dans SettingsScreen avec sélecteur de driver (Null / Demo / Tasmota / ESPHome)
- Champs de configuration dynamiques selon le driver (host, port, channel, entityId)
- Persistance de `RelaySystemConfig` en JSON dans les prefs
- Câblage de `CountdownViewModel` et `TeststripViewModel` pour utiliser le système relais configuré
- Indicateur de connexion dans la barre supérieure de `CountdownScreen`
