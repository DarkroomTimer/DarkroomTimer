# Connectivité Relais — Architecture Driver

---

## Description

L'application contrôle les relais physiques (agrandisseur et safelight) via une architecture modulaire à deux couches. Le mode **Standalone** (sans matériel) est toujours disponible. Les drivers WiFi (Tasmota, ESPHome) sont supportés en v1. Bluetooth BLE est prévu pour une version future.

---

## Architecture — Deux Couches

```
┌──────────────────────────────────────────────────────────────┐
│  RelaySystem                                                 │
│  (orchestre enlarger + safelight, expose les capabilities)   │
│                                                              │
│   enlarger: RelayController    safelight: RelayController?   │
└─────────────────┬──────────────────────────┬─────────────────┘
                  │                          │
        ┌─────────▼──────────┐    ┌─────────▼──────────┐
        │  RelayController   │    │  RelayController   │
        │  (un relais        │    │  (un relais        │
        │   physique)        │    │   physique)        │
        └────────────────────┘    └────────────────────┘
```

**`RelayController`** : contrôle un seul relais physique via un protocole donné.  
**`RelaySystem`** : compose enlarger + safelight optionnel, expose les capacités combinées.

---

## Interface RelayController

```kotlin
interface RelayController {
    val canPause: Boolean                       // false si timing délégué au firmware
    val state: StateFlow<RelayState>
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun set(on: Boolean): Result<Unit>
    suspend fun startTimed(durationMs: Long): Result<Unit>
}

enum class RelayState { ON, OFF, UNKNOWN }

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting   : ConnectionState()
    object Connected    : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
```

### Implémentations v1

| Classe | Protocole | `canPause` | Notes |
|---|---|---|---|
| `NullRelayController` | — | `true` | Standalone silencieux — PAUSE visuelle toujours possible |
| `DemoRelayController` | — | `true` | Simulation instantanée, pour tests UI |
| `TasmotaRelayController` | HTTP | selon `timingMode` | `TIMED_POWER` → false, `EXPLICIT_ON_OFF` → true |
| `ESPhomeHttpRelayController` | HTTP REST | `false` | Timing délégué au firmware |

### Implémentations futures

| Classe | Protocole | `canPause` |
|---|---|---|
| `ESPhomeNativeRelayController` | Native API port 6053 | `true` (push natif, latence faible) |
| `ESPHomeBleRelayController` | BLE proxy ESPHome | à définir |

---

## RelaySystem

```kotlin
class RelaySystem(
    val enlarger: RelayController,
    val safelight: RelayController? = null
) {
    val capabilities: DriverCapabilities
        get() = DriverCapabilities(
            canPause     = enlarger.canPause,
            hasSafelight = safelight != null
        )

    val relayStates: StateFlow<RelayStates>
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun setEnlarger(on: Boolean): Result<Unit>
    suspend fun setSafelight(on: Boolean): Result<Unit>
    suspend fun startTimedExposure(durationMs: Long): Result<Unit>
}

data class DriverCapabilities(
    val canPause: Boolean,
    val hasSafelight: Boolean
)

data class RelayStates(
    val enlarger: RelayState  = RelayState.UNKNOWN,
    val safelight: RelayState = RelayState.UNKNOWN
)
```

### Exemples de configurations

| Enlarger | Safelight | Cas d'usage |
|---|---|---|
| `NullRelayController` | `null` | Standalone pur |
| `DemoRelayController` | `DemoRelayController` | Démo / tests UI |
| `TasmotaRelayController(ch=1)` | `TasmotaRelayController(ch=2)` | Sonoff Dual R3 — même device |
| `TasmotaRelayController` | `ESPhomeHttpRelayController` | Deux devices, protocoles différents |
| `ESPhomeHttpRelayController` | `null` | ESPHome, 1 relais, sans safelight |

---

## Configuration

### RelayControllerConfig

```kotlin
sealed class RelayControllerConfig {

    object Null : RelayControllerConfig()

    object Demo : RelayControllerConfig()

    data class Tasmota(
        val host: String,
        val port: Int = 80,
        val username: String = "",
        val password: String = "",
        val channel: Int = 1,                           // 1 = Power1, 2 = Power2
        val timingMode: TimingMode = TimingMode.TIMED_POWER
    ) : RelayControllerConfig()

    data class ESPhomeHttp(
        val host: String,
        val port: Int = 80,
        val entityId: String                            // nom de l'entité switch ESPHome
    ) : RelayControllerConfig()

    // Futur
    data class ESPhomeNative(
        val host: String,
        val port: Int = 6053,
        val apiKey: String? = null,
        val entityId: String
    ) : RelayControllerConfig()
}

enum class TimingMode { TIMED_POWER, EXPLICIT_ON_OFF }
```

### RelaySystemConfig

```kotlin
data class RelaySystemConfig(
    val enlarger: RelayControllerConfig = RelayControllerConfig.Null,
    val safelight: RelayControllerConfig? = null
)
```

**Persistance :** sérialisée en JSON dans DataStore (clé `relay_system_config`). Une seule config active à la fois.

### Factory

```kotlin
object RelayControllerFactory {
    fun create(config: RelayControllerConfig, dispatcher: CoroutineDispatcher): RelayController =
        when (config) {
            is Null          -> NullRelayController()
            is Demo          -> DemoRelayController()
            is Tasmota       -> TasmotaRelayController(config, dispatcher)
            is ESPhomeHttp   -> ESPhomeHttpRelayController(config, dispatcher)
            is ESPhomeNative -> ESPhomeNativeRelayController(config, dispatcher)
        }
}
```

---

## Découverte Réseau (mDNS)

| Firmware | Service mDNS | Exemple hostname |
|---|---|---|
| Tasmota | `_http._tcp` | `tasmota-a1b2c3.local` |
| ESPHome | `_esphomelib._tcp` | `sonoff-dual-r3.local` |

Un bouton **"Scanner"** dans l'écran de configuration lance une découverte mDNS et pré-remplit le champ `host`. La saisie manuelle reste toujours disponible.

---

## Interface Utilisateur — Configuration

### Écran Réglages → Relais

1. **Section Agrandisseur** : sélecteur de type de driver + champs de config propres au driver
2. **Section Safelight** :
   - Toggle **"Même appareil que l'agrandisseur"** → copie `host` et `port`, seul le `channel` ou `entityId` est à saisir
   - Ou configuration indépendante (autre device, autre protocole)
3. **Bouton "Scanner"** : découverte mDNS
4. **Bouton "Tester la connexion"** : vérifie la connectivité et affiche l'état

### Dans l'écran principal

Un indicateur discret montre l'état de connexion :

| État | Indicateur |
|---|---|
| Null / Demo | Icône grisée (pas de matériel) |
| Connecting | Icône orange animée |
| Connected | Icône verte |
| Error | Icône rouge |

---

## Intégration Timer

### Cycle de vie de la connexion

| Événement | Action |
|---|---|
| App démarrée | `connect()` si driver ≠ Null/Demo |
| Config driver changée | `disconnect()` → rebuild via factory → `connect()` |
| Exposition active → arrière-plan | Foreground Service maintient timer et connexion |
| Perte réseau | `connectionState` → `Error`, retry en arrière-plan, indicateur rouge UI |
| App fermée | `disconnect()`, relais en sécurité (OPEN) |

### Séquence d'une exposition

```kotlin
fun startExposure(durationMs: Long) {
    relaySystem.setSafelight(on = false)
    if (relaySystem.capabilities.canPause) {
        relaySystem.setEnlarger(on = true)
        timerJob = launchTimer(durationMs)          // app contrôle le timing
    } else {
        // IMPORTANT : Pour le timing délégué, envoyer la durée ajustée via le profil agrandisseur
        relaySystem.startTimedExposure(adjustedDurationMs)  // firmware contrôle le timing
        launchProgressDisplay(durationMs)           // UI seulement
    }
}

fun stopExposure() {
    // ACTION CRITIQUE : Toujours envoyer un OFF explicite
    // Même si le timing est délégué au firmware, l'appel à setEnlarger(on = false)
    // doit envoyer une commande d'ouverture immédiate du relais pour couper la lampe.
    relaySystem.setEnlarger(on = false)
    if (relaySystem.capabilities.hasSafelight)
        relaySystem.setSafelight(on = true)
}

fun onTimerFinished() {
    stopExposure()
}
```

### Orchestration PAUSE / REPRISE

Le bouton PAUSE n'est affiché que si `capabilities.canPause == true`.

```kotlin
fun onPause() {
    timerJob.cancel()
    relaySystem.setEnlarger(on = false)
    if (relaySystem.capabilities.hasSafelight)
        relaySystem.setSafelight(on = true)
}

fun onResume() {
    if (relaySystem.capabilities.hasSafelight)
        relaySystem.setSafelight(on = false)
    relaySystem.setEnlarger(on = true)
    timerJob = launchTimer(remainingMs)
}
```

---

## Injection et Tests

`RelaySystem` est injecté par constructeur dans les ViewModels. En production, fourni par Hilt avec `@ActivityRetainedScoped`. En tests, instancié directement :

```kotlin
val fakeEnlarger  = FakeRelayController(canPause = true)
val fakeSafelight = FakeRelayController()
val system        = RelaySystem(fakeEnlarger, fakeSafelight)
val viewModel     = CountdownViewModel(system, UnconfinedTestDispatcher())
```

Voir `spec/00-overview.md` et le design doc `docs/superpowers/specs/2026-05-22-relay-driver-architecture-design.md` pour les détails sur les tests et la qualité statique.

---

## Futur — Bluetooth BLE

Le BLE via le **proxy BLE d'ESPHome** est prévu pour une version future. L'interface `RelayController` l'accueille sans modification.

Pour référence, les UUIDs BLE de la spec originale (Arduino/ESP32 custom) restent documentés ci-dessous. Ils restent valides pour un futur `BleRelayController` non-ESPHome :

```
Service UUID  : 0000FFE0-0000-1000-8000-00805F9B34FB
CMD_RELAY     : 0000FFE1-0000-1000-8000-00805F9B34FB  (Write sans réponse)
STATUS        : 0000FFE2-0000-1000-8000-00805F9B34FB  (Notify)
```

| Valeur | Commande | Effet |
|---|---|---|
| `0x01` | ENLARGER_ON | Fermer le relais agrandisseur |
| `0x02` | ENLARGER_OFF | Ouvrir le relais agrandisseur |
| `0x03` | SAFELIGHT_ON | Fermer le relais safelight |
| `0x04` | SAFELIGHT_OFF | Ouvrir le relais safelight |
| `0x05` | BOTH_OFF | Ouvrir les deux relais |
| `0x06` | PING | Demande d'état |
