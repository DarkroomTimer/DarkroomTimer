# WiFi Relay Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the SettingsScreen "BLUETOOTH" placeholder with a functional "RELAIS" section that lets the user select and configure the enlarger and safelight relay drivers (Null / Demo / Tasmota / ESPHome), persist the config as JSON in SharedPreferences, wire CountdownViewModel and TeststripViewModel to build their relay systems from that config, and show a connection state indicator in CountdownScreen.

**Architecture:** `RelaySystemConfig` is serialized to JSON (Gson, already in project) and stored in a single SharedPreferences key. CountdownViewModel and TeststripViewModel Factories read the config at ViewModel creation time and build the `RelaySystem` via `RelayControllerFactory`. The ViewModel calls `relaySystem.connect()` in `init` and `relaySystem.disconnect()` in `onCleared()`. Connection state is exposed in `CountdownUiState` and displayed as a small indicator in `CountdownScreen`. No dependency injection required.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, SharedPreferences, Gson, OkHttp (already in project), RelayControllerFactory (already built), Coroutines

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `storage/PreferenceManager.kt` | Modifier | Persistance de `RelaySystemConfig` comme JSON string |
| `system/RelaySystemConfigStore.kt` | Créer | Data class `RelaySystemConfig` plat + helpers `toRelaySystem()` |
| `ui/SettingsScreen.kt` | Modifier | Remplacer section BLUETOOTH par section RELAIS |
| `ui/CountdownViewModel.kt` | Modifier | Factory lit config, init connecte, onCleared déconnecte, expose connectionState |
| `ui/CountdownScreen.kt` | Modifier | Indicateur de connexion en haut |
| `ui/TeststripViewModel.kt` | Modifier | Factory lit config, init connecte, onCleared déconnecte |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystemConfigStore.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`

---

## Context: État actuel vs spec

**Implémenté :** Tous les drivers (NullRelayController, DemoRelayController, TasmotaRelayController, ESPhomeHttpRelayController), RelaySystem, RelayControllerFactory, RelayControllerConfig (sealed class). CountdownViewModel et TeststripViewModel utilisent `StandaloneRelaySystem()` (hardcodé). SettingsScreen a "BLUETOOTH → Mode compagnon WiFi — configuration relay à venir." comme placeholder.

**Manquant (ce plan) :**
- `RelaySystemConfig` : data class plat sérialisable en JSON, avec helpers pour construire `RelaySystem`
- `PreferenceManager` : clé JSON pour stocker la config
- Section RELAIS dans SettingsScreen avec sélection driver + champs dynamiques
- `CountdownViewModel` : Factory lit config depuis prefs, `init` appelle `connect()`, `onCleared` appelle `disconnect()`, `connectionState` exposé dans `CountdownUiState`
- `CountdownScreen` : indicateur de connexion (dot coloré + label)
- `TeststripViewModel` : Factory lit config depuis prefs, `init` appelle `connect()`, `onCleared` appelle `disconnect()`

**Non traité dans ce plan :**
- Découverte mDNS (bouton "Scanner") — complexité réseau, plan séparé
- Mode Ne Pas Déranger — feature séparée
- Sauvegarde de l'exposition active en cas de perte réseau mid-timer (hors scope v1)

---

## Task 1: RelaySystemConfigStore — data class et helpers

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystemConfigStore.kt`

**Context:** La `RelayControllerConfig` existante est une sealed class qui ne se sérialise pas facilement en JSON sans adaptateur custom. On crée `RelaySystemConfigFlat`, une data class JSON-friendly avec des champs String/Int/Boolean pour toute la configuration agrandisseur + safelight. Le helper `buildRelaySystem()` convertit vers l'architecture existante.

- [ ] **Step 1.1: Créer RelaySystemConfigStore.kt**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystemConfigStore.kt
package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.DemoRelayController
import fr.mathgl.darkroomtimer.system.drivers.ESPhomeHttpRelayController
import fr.mathgl.darkroomtimer.system.drivers.NullRelayController
import fr.mathgl.darkroomtimer.system.drivers.TasmotaRelayController
import kotlinx.coroutines.MainScope

/**
 * Flat, JSON-serializable representation of the full relay system configuration.
 * Stored as a single JSON string in SharedPreferences.
 */
data class RelaySystemConfigFlat(
    // Enlarger
    val enlargerType: String = "NULL",        // "NULL", "DEMO", "TASMOTA", "ESPHOME_HTTP"
    val enlargerHost: String = "",
    val enlargerPort: Int = 80,
    val enlargerChannel: Int = 1,             // Tasmota: 1 or 2
    val enlargerUsername: String = "",
    val enlargerPassword: String = "",
    val enlargerEntityId: String = "",        // ESPHome
    val enlargerTimingMode: String = "TIMED_POWER", // Tasmota: "TIMED_POWER" or "EXPLICIT_ON_OFF"
    // Safelight
    val safelightEnabled: Boolean = false,
    val safelightSameDevice: Boolean = true,  // true = same host/port as enlarger, only channel differs
    val safelightType: String = "NULL",       // only used when safelightSameDevice = false
    val safelightHost: String = "",
    val safelightPort: Int = 80,
    val safelightChannel: Int = 2,            // Tasmota: channel 2 when same device
    val safelightEntityId: String = "",       // ESPHome when independent
    val safelightUsername: String = "",
    val safelightPassword: String = ""
) {
    fun buildRelaySystem(): RelaySystem {
        val enlarger = buildEnlarger()
        val safelight = if (safelightEnabled) buildSafelight() else null
        return RelaySystem(enlarger = enlarger, safelight = safelight, scope = MainScope())
    }

    private fun buildEnlarger(): RelayController = when (enlargerType) {
        "DEMO"         -> DemoRelayController()
        "TASMOTA"      -> TasmotaRelayController(
            host       = enlargerHost,
            port       = enlargerPort,
            channel    = enlargerChannel,
            username   = enlargerUsername.ifBlank { null },
            password   = enlargerPassword.ifBlank { null },
            timingMode = if (enlargerTimingMode == "EXPLICIT_ON_OFF") TimingMode.EXPLICIT_ON_OFF
                         else TimingMode.TIMED_POWER
        )
        "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
            host     = enlargerHost,
            port     = enlargerPort,
            entityId = enlargerEntityId
        )
        else           -> NullRelayController()  // "NULL" and fallback
    }

    private fun buildSafelight(): RelayController {
        if (safelightSameDevice) {
            // Same device as enlarger: inherit host/port, but different channel
            return when (enlargerType) {
                "TASMOTA" -> TasmotaRelayController(
                    host       = enlargerHost,
                    port       = enlargerPort,
                    channel    = safelightChannel,
                    username   = enlargerUsername.ifBlank { null },
                    password   = enlargerPassword.ifBlank { null },
                    timingMode = TimingMode.EXPLICIT_ON_OFF  // safelight always app-controlled
                )
                "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
                    host     = enlargerHost,
                    port     = enlargerPort,
                    entityId = safelightEntityId
                )
                else -> NullRelayController()
            }
        }
        return when (safelightType) {
            "DEMO"         -> DemoRelayController()
            "TASMOTA"      -> TasmotaRelayController(
                host       = safelightHost,
                port       = safelightPort,
                channel    = safelightChannel,
                username   = safelightUsername.ifBlank { null },
                password   = safelightPassword.ifBlank { null },
                timingMode = TimingMode.EXPLICIT_ON_OFF
            )
            "ESPHOME_HTTP" -> ESPhomeHttpRelayController(
                host     = safelightHost,
                port     = safelightPort,
                entityId = safelightEntityId
            )
            else           -> NullRelayController()
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
git add app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystemConfigStore.kt
git commit -m "feat: add RelaySystemConfigFlat with buildRelaySystem() helper"
```

---

## Task 2: Persistance de RelaySystemConfigFlat dans PreferenceManager

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt`

**Context:** Gson est déjà dans le projet (utilisé par StorageService). `relaySystemConfig` est lue/écrite comme JSON string dans une clé dédiée. La valeur par défaut (JSON absent) retourne un `RelaySystemConfigFlat()` par défaut (driver Null, pas de safelight).

- [ ] **Step 2.1: Ajouter les imports Gson et la propriété relaySystemConfig dans PreferenceManager**

Ajouter en haut du fichier, après les imports existants :
```kotlin
import com.google.gson.Gson
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat
```

Ajouter dans `companion object` de `PreferenceManager` :
```kotlin
private const val KEY_RELAY_SYSTEM_CONFIG = "pref_relay_system_config"
```

Ajouter dans le corps de la classe, après `luminosityFixed` :
```kotlin
// Relay System Config (stored as JSON)
private val gson = Gson()

var relaySystemConfig: RelaySystemConfigFlat
    get() {
        val json = prefs.getString(KEY_RELAY_SYSTEM_CONFIG, null)
        return if (json != null) {
            try { gson.fromJson(json, RelaySystemConfigFlat::class.java) }
            catch (e: Exception) { RelaySystemConfigFlat() }
        } else {
            RelaySystemConfigFlat()
        }
    }
    set(value) = prefs.edit().putString(KEY_RELAY_SYSTEM_CONFIG, gson.toJson(value)).apply()
```

- [ ] **Step 2.2: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/storage/PreferenceManager.kt
git commit -m "feat: add relaySystemConfig JSON persistence in PreferenceManager"
```

---

## Task 3: Section RELAIS dans SettingsScreen

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`

**Context:** Remplacer la section "BLUETOOTH" placeholder par une section "RELAIS" complète. La section montre : sélecteur de driver agrandisseur, champs dynamiques selon le driver, toggle safelight, champs safelight dynamiques, et un bouton "Tester la connexion" qui tente un `connect()` et affiche le résultat.

Les champs n'apparaissent que si le driver le nécessite :
- NULL : aucun champ
- DEMO : aucun champ
- TASMOTA : Host, Port, Canal (1 ou 2), Mode (TIMED_POWER / EXPLICIT_ON_OFF), Utilisateur (optionnel), Mot de passe (optionnel)
- ESPHOME_HTTP : Host, Port, Entity ID

La safelight est masquée si `enlargerType == "NULL"` ou `"DEMO"` (pas de sens physique). Quand safelight activée + même appareil : seul le canal (Tasmota) ou Entity ID (ESPHome) est demandé.

Sauvegarde : chaque modification de champ écrit immédiatement dans `prefs.relaySystemConfig` (pattern identique aux autres settings).

- [ ] **Step 3.1: Remplacer la section BLUETOOTH par la section RELAIS dans SettingsScreen()**

Localiser dans `SettingsScreen()` :
```kotlin
// BLUETOOTH
SettingsSectionHeader("BLUETOOTH")
Text(
    text = "Mode compagnon WiFi — configuration relay à venir.",
    color = DarkroomRedDim,
    fontSize = 12.sp,
    modifier = Modifier.padding(vertical = 8.dp)
)
```

Remplacer par :
```kotlin
// RELAIS
SettingsSectionHeader("RELAIS")
var relayCfg by remember { mutableStateOf(prefs.relaySystemConfig) }

fun saveRelayCfg(newCfg: RelaySystemConfigFlat) {
    relayCfg = newCfg
    prefs.relaySystemConfig = newCfg
}

// Enlarger driver
SettingsDropdown(
    label = "Agrandisseur",
    options = listOf("NULL", "DEMO", "TASMOTA", "ESPHOME_HTTP"),
    selected = relayCfg.enlargerType,
    onSelect = { saveRelayCfg(relayCfg.copy(enlargerType = it)) }
)

if (relayCfg.enlargerType == "TASMOTA" || relayCfg.enlargerType == "ESPHOME_HTTP") {
    RelayTextField(
        label = "Hôte (IP ou hostname)",
        value = relayCfg.enlargerHost,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerHost = it)) }
    )
    RelayNumberField(
        label = "Port",
        value = relayCfg.enlargerPort,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerPort = it)) }
    )
}
if (relayCfg.enlargerType == "TASMOTA") {
    RelayNumberField(
        label = "Canal (1 ou 2)",
        value = relayCfg.enlargerChannel,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerChannel = it.coerceIn(1, 2))) }
    )
    SettingsDropdown(
        label = "Mode timing",
        options = listOf("TIMED_POWER", "EXPLICIT_ON_OFF"),
        selected = relayCfg.enlargerTimingMode,
        onSelect = { saveRelayCfg(relayCfg.copy(enlargerTimingMode = it)) }
    )
    RelayTextField(
        label = "Utilisateur (optionnel)",
        value = relayCfg.enlargerUsername,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerUsername = it)) }
    )
    RelayTextField(
        label = "Mot de passe (optionnel)",
        value = relayCfg.enlargerPassword,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerPassword = it)) }
    )
}
if (relayCfg.enlargerType == "ESPHOME_HTTP") {
    RelayTextField(
        label = "Entity ID",
        value = relayCfg.enlargerEntityId,
        onValueChange = { saveRelayCfg(relayCfg.copy(enlargerEntityId = it)) }
    )
}

// Safelight (only for TASMOTA or ESPHOME_HTTP)
if (relayCfg.enlargerType == "TASMOTA" || relayCfg.enlargerType == "ESPHOME_HTTP") {
    Spacer(modifier = Modifier.height(8.dp))
    SettingsSwitch(
        label = "Safelight activé",
        checked = relayCfg.safelightEnabled,
        onCheckedChange = { saveRelayCfg(relayCfg.copy(safelightEnabled = it)) }
    )
    if (relayCfg.safelightEnabled) {
        SettingsSwitch(
            label = "Même appareil que l'agrandisseur",
            checked = relayCfg.safelightSameDevice,
            onCheckedChange = { saveRelayCfg(relayCfg.copy(safelightSameDevice = it)) }
        )
        if (relayCfg.safelightSameDevice) {
            if (relayCfg.enlargerType == "TASMOTA") {
                RelayNumberField(
                    label = "Canal safelight (1 ou 2)",
                    value = relayCfg.safelightChannel,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightChannel = it.coerceIn(1, 2))) }
                )
            } else {
                RelayTextField(
                    label = "Entity ID safelight",
                    value = relayCfg.safelightEntityId,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightEntityId = it)) }
                )
            }
        } else {
            // Independent safelight
            SettingsDropdown(
                label = "Driver safelight",
                options = listOf("NULL", "TASMOTA", "ESPHOME_HTTP"),
                selected = relayCfg.safelightType,
                onSelect = { saveRelayCfg(relayCfg.copy(safelightType = it)) }
            )
            if (relayCfg.safelightType == "TASMOTA" || relayCfg.safelightType == "ESPHOME_HTTP") {
                RelayTextField(
                    label = "Hôte safelight",
                    value = relayCfg.safelightHost,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightHost = it)) }
                )
                RelayNumberField(
                    label = "Port safelight",
                    value = relayCfg.safelightPort,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightPort = it)) }
                )
            }
            if (relayCfg.safelightType == "TASMOTA") {
                RelayNumberField(
                    label = "Canal safelight",
                    value = relayCfg.safelightChannel,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightChannel = it.coerceIn(1, 2))) }
                )
            }
            if (relayCfg.safelightType == "ESPHOME_HTTP") {
                RelayTextField(
                    label = "Entity ID safelight",
                    value = relayCfg.safelightEntityId,
                    onValueChange = { saveRelayCfg(relayCfg.copy(safelightEntityId = it)) }
                )
            }
        }
    }
}

// Test connection
var relayTestResult by remember { mutableStateOf("") }
val relayTestScope = rememberCoroutineScope()
Spacer(modifier = Modifier.height(8.dp))
Button(
    onClick = {
        relayTestResult = "Connexion en cours…"
        relayTestScope.launch {
            val cfg = prefs.relaySystemConfig
            if (cfg.enlargerType == "NULL" || cfg.enlargerType == "DEMO") {
                relayTestResult = "Mode ${cfg.enlargerType} — pas de connexion réseau."
                return@launch
            }
            val testSystem = cfg.buildRelaySystem()
            val result = withContext(Dispatchers.IO) {
                runCatching { testSystem.connect() }.getOrNull()
            }
            relayTestResult = if (result != null) "✓ Connexion réussie." else "✗ Échec de connexion."
            withContext(Dispatchers.IO) { testSystem.disconnect() }
        }
    },
    modifier = Modifier.fillMaxWidth().height(48.dp),
    colors = ButtonDefaults.buttonColors(containerColor = DarkroomSurface)
) {
    Text("TESTER LA CONNEXION", color = DarkroomRedBright, fontSize = 14.sp)
}
if (relayTestResult.isNotBlank()) {
    Text(
        text = relayTestResult,
        color = if (relayTestResult.startsWith("✓")) DarkroomRedBright else Color.Red,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp, start = 8.dp)
    )
}
Text(
    text = "Les changements s'appliquent au prochain démarrage de l'app.",
    color = DarkroomRedDim,
    fontSize = 11.sp,
    modifier = Modifier.padding(top = 8.dp, start = 8.dp)
)
```

- [ ] **Step 3.2: Ajouter les imports RelaySystemConfigFlat, withContext, Dispatchers**

S'assurer que ces imports sont présents en haut de `SettingsScreen.kt` :
```kotlin
import fr.mathgl.darkroomtimer.system.RelaySystemConfigFlat
import kotlinx.coroutines.withContext
```
(Les autres imports `Dispatchers`, `rememberCoroutineScope`, `launch` sont déjà présents.)

- [ ] **Step 3.3: Ajouter RelayTextField et RelayNumberField en bas du fichier**

Ajouter après `LuminositySlider` :
```kotlin
@Composable
private fun RelayTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkroomRedBright,
            unfocusedBorderColor = DarkroomRedFaint,
            focusedTextColor = DarkroomRedBright,
            unfocusedTextColor = DarkroomRedBright,
            cursorColor = DarkroomRedBright
        )
    )
}

@Composable
private fun RelayNumberField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onValueChange(it.toIntOrNull() ?: value) },
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DarkroomRedBright,
            unfocusedBorderColor = DarkroomRedFaint,
            focusedTextColor = DarkroomRedBright,
            unfocusedTextColor = DarkroomRedBright,
            cursorColor = DarkroomRedBright
        )
    )
}
```

- [ ] **Step 3.4: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL. En cas d'erreur sur `fun saveRelayCfg` (Composable scope), envelopper la lambda dans une `remember { }` ou la transformer en variable locale de `SettingsScreen()`.

- [ ] **Step 3.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt
git commit -m "feat: add RELAIS section in SettingsScreen with dynamic relay driver config"
```

---

## Task 4: CountdownViewModel — relay depuis prefs + connection state

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt`

**Context:** La Factory crée actuellement `StandaloneRelaySystem()` hardcodé. Il faut : (1) lire `prefs.relaySystemConfig` dans la Factory et construire le `RelaySystem` via `buildRelaySystem()`, (2) appeler `relaySystem.connect()` dans `init {}` si le driver n'est pas Null/Demo, (3) appeler `relaySystem.disconnect()` dans `onCleared()`, (4) exposer `connectionState` dans `CountdownUiState`.

- [ ] **Step 4.1: Ajouter connectionState à CountdownUiState**

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
    val enlargerOverride: Boolean = false,
    val safelightOverride: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected  // ← ajouter
)
```

- [ ] **Step 4.2: Collecter connectionState dans init {}**

Dans `init {}`, après la collecte de `relayStates`, ajouter :
```kotlin
viewModelScope.launch {
    relaySystem.connectionState.collect { connState ->
        _uiState.update { it.copy(connectionState = connState) }
    }
}
```

- [ ] **Step 4.3: Appeler connect() dans init {} pour les drivers réseau**

Dans `init {}`, après les collectes de flows, ajouter :
```kotlin
// Connect relay if not Null/Demo (network drivers need connection)
viewModelScope.launch {
    try { relaySystem.connect() } catch (e: Exception) { /* ignore — connectionState Flow reflects error */ }
}
```

- [ ] **Step 4.4: Appeler disconnect() dans onCleared()**

Dans `onCleared()`, ajouter avant `audioSystem?.release()` :
```kotlin
viewModelScope.launch {
    try { relaySystem.disconnect() } catch (e: Exception) { /* ignore */ }
}
```

- [ ] **Step 4.5: Mettre à jour la Factory pour lire la config depuis prefs**

Remplacer le bloc `override fun <T : ViewModel> create(modelClass, extras)` dans `companion object` :
```kotlin
override fun <T : androidx.lifecycle.ViewModel> create(
    modelClass: Class<T>,
    extras: androidx.lifecycle.viewmodel.CreationExtras
): T {
    val application = extras[
        ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
    ] as? Application
        ?: throw IllegalStateException("Application not available")
    val prefs = fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(application)
    val relaySystem = prefs.relaySystemConfig.buildRelaySystem()
    return CountdownViewModel(application, relaySystem) as T
}
```

- [ ] **Step 4.6: Ajouter l'import ConnectionState**

S'assurer que l'import est présent en haut du fichier :
```kotlin
import fr.mathgl.darkroomtimer.system.ConnectionState
```

- [ ] **Step 4.7: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 4.8: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownViewModel.kt
git commit -m "feat: CountdownViewModel reads relay config from prefs and manages connection lifecycle"
```

---

## Task 5: Indicateur de connexion dans CountdownScreen

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`

**Context:** Spec : un indicateur discret en haut de l'écran montre l'état de connexion. Null/Demo → icône grisée (pas de matériel). Connecting → orange. Connected → vert. Error → rouge. L'indicateur est un petit dot coloré + label texte, placé juste sous le Spacer initial du CountdownScreen.

- [ ] **Step 5.1: Ajouter l'import ConnectionState dans CountdownScreen.kt**

```kotlin
import fr.mathgl.darkroomtimer.system.ConnectionState
```

- [ ] **Step 5.2: Ajouter le composable ConnectionIndicator en bas du fichier**

Ajouter avant la dernière accolade du fichier :
```kotlin
@Composable
private fun ConnectionIndicator(connectionState: ConnectionState, relayType: String) {
    val (dotColor, label) = when {
        relayType == "NULL" || relayType == "DEMO" ->
            DarkroomRedDim to relayType.lowercase()
        connectionState is ConnectionState.Connected ->
            Color(0xFF44AA44) to "connecté"
        connectionState is ConnectionState.Connecting ->
            Color(0xFFAA8800) to "connexion…"
        connectionState is ConnectionState.Error ->
            Color.Red to "erreur"
        else ->
            DarkroomRedDim to "déconnecté"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, shape = RoundedCornerShape(4.dp))
        )
        Text(text = label, fontSize = 11.sp, color = dotColor)
    }
}
```

- [ ] **Step 5.3: Câbler l'indicateur dans CountdownScreen()**

Dans le composable `CountdownScreen()`, le state contient `state.connectionState`. Il faut aussi connaître le type de relay pour distinguer Null/Demo des drivers réseau. Passer le type via `PreferenceManager` lu directement dans le composable (lecture unique au démarrage — pas de recomposition sur changement car les settings ne changent pas à chaud).

Ajouter dans `CountdownScreen()` après `val state by viewModel.uiState.collectAsState()` :
```kotlin
val context = androidx.compose.ui.platform.LocalContext.current
val relayType = remember {
    fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(context).relaySystemConfig.enlargerType
}
```

Ajouter dans le `Column` de `CountdownScreen()`, juste après le premier `Spacer(modifier = Modifier.height(32.dp))` :
```kotlin
ConnectionIndicator(
    connectionState = state.connectionState,
    relayType = relayType
)
Spacer(modifier = Modifier.height(8.dp))
```

- [ ] **Step 5.4: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 5.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt
git commit -m "feat: add connection indicator to CountdownScreen top bar"
```

---

## Task 6: TeststripViewModel — relay depuis prefs + connection lifecycle

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt`

**Context:** Même fix que Task 4, sans le `connectionState` dans l'UI (TeststripScreen n'affiche pas d'indicateur réseau dans cette version). Il faut : Factory lit config depuis prefs, `init` connecte, `onCleared` déconnecte.

- [ ] **Step 6.1: Mettre à jour la Factory de TeststripViewModel**

Remplacer dans `companion object` de `TeststripViewModel` le `create(modelClass, extras)` :
```kotlin
override fun <T : androidx.lifecycle.ViewModel> create(
    modelClass: Class<T>,
    extras: androidx.lifecycle.viewmodel.CreationExtras
): T {
    val application = extras[
        ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY
    ] as? Application
        ?: throw IllegalStateException("Application not available")
    val prefs = fr.mathgl.darkroomtimer.storage.PreferenceManager.getInstance(application)
    val relaySystem = prefs.relaySystemConfig.buildRelaySystem()
    return TeststripViewModel(application, relaySystem) as T
}
```

- [ ] **Step 6.2: Appeler connect() dans init {} de TeststripViewModel**

Dans `init {}`, après `updateUiState()`, ajouter :
```kotlin
// Connect relay if network driver
viewModelScope.launch {
    try { relaySystem.connect() } catch (e: Exception) { /* ignore */ }
}
```

- [ ] **Step 6.3: Appeler disconnect() dans onCleared()**

Dans `onCleared()` de `TeststripViewModel`, ajouter avant `audioSystem?.release()` :
```kotlin
viewModelScope.launch {
    try { relaySystem.disconnect() } catch (e: Exception) { /* ignore */ }
}
```

- [ ] **Step 6.4: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 6.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripViewModel.kt
git commit -m "feat: TeststripViewModel reads relay config from prefs and manages connection lifecycle"
```

---

## Vérification Finale

1. **Compiler** : `./gradlew assembleDebug` — BUILD SUCCESSFUL
2. **Tests** : `./gradlew test` — tous PASS
3. **Vérification visuelle** (installer l'APK) :
   - Onglet Réglages → section RELAIS visible sous LUMINOSITÉ ÉCRAN
   - Sélectionner "NULL" → aucun champ additionnel
   - Sélectionner "DEMO" → aucun champ, CountdownScreen affiche dot gris + "demo"
   - Sélectionner "TASMOTA" → champs Host, Port, Canal, Mode, Utilisateur, Mot de passe apparaissent
   - Saisir un hôte inexistant → "TESTER LA CONNEXION" → "✗ Échec de connexion."
   - Avec un vrai Sonoff Dual R3 Tasmota : host valide → "✓ Connexion réussie."
   - CountdownScreen : dot orange pendant Connecting, vert si Connected
   - Démarrer le timer → agrandisseur s'allume sur le relais physique
   - Laisser le timer se terminer → relais OFF automatiquement
   - PAUSE → relais OFF ; RESUME → relais ON
   - Note texte dans SettingsScreen : "Les changements s'appliquent au prochain démarrage de l'app."

---

## Plan Suivant

**Fonctionnalités restantes identifiées (hors scope de ce plan) :**
- Découverte mDNS automatique (bouton "Scanner") — nécessite Android NSD API
- Mode Ne Pas Déranger — `ACCESS_NOTIFICATION_POLICY`, section séparée dans Settings
- Persistance des réglages teststrip entre sessions (baseMs, stop fraction vers prefs)
