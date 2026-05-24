# Relay Driver Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a modular relay control system supporting Standalone, Demo, Tasmota, and ESPHome drivers.

**Architecture:**
- `RelayController`: Interface for a single physical relay.
- `RelaySystem`: Orchestrator for Enlarger and optional Safelight relays.
- `RelayControllerFactory`: Produces drivers based on `RelayControllerConfig`.
- HTTP drivers use OkHttp for communication with Tasmota/ESPHome.

**Tech Stack:** Kotlin, Coroutines, StateFlow, OkHttp

---

## File Structure

| File | Action | Responsibility |
|---|---|---|
| `system/RelayDriverModels.kt` | Create | `RelayState` (enum), `ConnectionState` (sealed), `RelayStates` (data) |
| `system/RelayController.kt` | Create | `RelayController` interface |
| `system/RelaySystem.kt` | Create | `RelaySystem` orchestrator and `DriverCapabilities` |
| `system/drivers/NullRelayController.kt` | Create | Standalone driver (no-op) |
| `system/drivers/DemoRelayController.kt` | Create | Simulation driver for UI tests |
| `system/drivers/TasmotaRelayController.kt` | Create | Tasmota HTTP driver |
| `system/drivers/ESPhomeHttpRelayController.kt` | Create | ESPHome HTTP REST driver |
| `system/RelayControllerConfig.kt` | Create | Configuration models for different drivers |
| `system/RelayControllerFactory.kt` | Create | Factory to instantiate drivers |
| `gradle/libs.versions.toml` | Modify | Add `okhttp` |
| `app/build.gradle.kts` | Modify | Add `okhttp` implementation |
| `system/RelayState.kt` | Delete | Replaced by `RelayDriverModels.kt` |

---

## Task 1: Dependencies & Base Models

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayDriverModels.kt`
- Delete: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt`

- [ ] **Step 1.1: Add OkHttp to libs.versions.toml**

```toml
# In [versions]
okhttp = "4.12.0"

# In [libraries]
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
```

- [ ] **Step 1.2: Add OkHttp to app/build.gradle.kts**

```kotlin
implementation(libs.okhttp)
```

- [ ] **Step 1.3: Implement RelayDriverModels**

```kotlin
package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.flow.StateFlow

enum class RelayState { ON, OFF, UNKNOWN }

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting   : ConnectionState()
    object Connected    : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class RelayStates(
    val enlarger: RelayState  = RelayState.UNKNOWN,
    val safelight: RelayState = RelayState.UNKNOWN
)
```

- [ ] **Step 1.4: Delete old RelayState.kt**

Run: `rm app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt`

- [ ] **Step 1.5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/fr/mathgl/darkroomtimer/system/RelayDriverModels.kt
git rm app/src/main/java/fr/mathgl/darkroomtimer/system/RelayState.kt
git commit -m "feat: add relay driver base models and okhttp dependency"
```

---

## Task 2: RelayController Interface & RelaySystem

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayController.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystem.kt`

- [ ] **Step 2.1: Implement RelayController interface**

```kotlin
package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.flow.StateFlow

interface RelayController {
    val canPause: Boolean
    val state: StateFlow<RelayState>
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun set(on: Boolean): Result<Unit>
    suspend fun startTimed(durationMs: Long): Result<Unit>
}
```

- [ ] **Step 2.2: Implement RelaySystem orchestrator**

```kotlin
package fr.mathgl.darkroomtimer.system

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class DriverCapabilities(
    val canPause: Boolean,
    val hasSafelight: Boolean
)

class RelaySystem(
    val enlarger: RelayController,
    val safelight: RelayController? = null
) {
    val capabilities: DriverCapabilities
        get() = DriverCapabilities(
            canPause     = enlarger.canPause,
            hasSafelight = safelight != null
        )

    val relayStates: StateFlow<RelayStates> // Implementation via combine in a real flow
    val connectionState: StateFlow<ConnectionState> // Based on enlarger state

    suspend fun connect() {
        enlarger.connect()
        safelight?.connect()
    }

    suspend fun disconnect() {
        enlarger.disconnect()
        safelight?.disconnect()
    }

    suspend fun setEnlarger(on: Boolean): Result<Unit> = enlarger.set(on)
    suspend fun setSafelight(on: Boolean): Result<Unit> = 
        safelight?.set(on) ?: Result.success(Unit)

    suspend fun startTimedExposure(durationMs: Long): Result<Unit> = enlarger.startTimed(durationMs)
}
```

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/RelayController.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/system/RelaySystem.kt
git commit -m "feat: add RelayController interface and RelaySystem orchestrator"
```

---

## Task 3: Simple Drivers (Null & Demo)

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/NullRelayController.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/DemoRelayController.kt`

- [ ] **Step 3.1: Implement NullRelayController**

```kotlin
package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NullRelayController : RelayController {
    override val canPause = true
    override val state = MutableStateFlow(RelayState.OFF)
    override val connectionState = MutableStateFlow(ConnectionState.Connected)

    override suspend fun connect() {}
    override suspend fun disconnect() {}
    override suspend fun set(on: Boolean): Result<Unit> {
        state.value = if (on) RelayState.ON else RelayState.OFF
        return Result.success(Unit)
    }
    override suspend fun startTimed(durationMs: Long): Result<Unit> = set(true)
}
```

- [ ] **Step 3.2: Implement DemoRelayController**

```kotlin
package fr.mathgl.darkroomtimer.system.drivers

import fr.mathgl.darkroomtimer.system.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DemoRelayController : RelayController {
    override val canPause = true
    override val state = MutableStateFlow(RelayState.OFF)
    override val connectionState = MutableStateFlow(ConnectionState.Disconnected)

    override suspend fun connect(): Result<Unit> {
        connectionState.value = ConnectionState.Connected
        return Result.success(Unit)
    }
    override suspend fun disconnect() {
        connectionState.value = ConnectionState.Disconnected
    }
    override suspend fun set(on: Boolean): Result<Unit> {
        state.value = if (on) RelayState.ON else RelayState.OFF
        return Result.success(Unit)
    }
    override suspend fun startTimed(durationMs: Long): Result<Unit> = set(true)
}
```

- [ ] **Step 3.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/NullRelayController.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/DemoRelayController.kt
git commit -m "feat: add Null and Demo relay controllers"
```

---

## Task 4: WiFi Drivers (Tasmota & ESPHome)

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/TasmotaRelayController.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/ESPhomeHttpRelayController.kt`

- [ ] **Step 4.1: Implement TasmotaRelayController**
(Implementation will use OkHttp to send HTTP requests like `/cm?cmnd=Power1 ON`)

- [ ] **Step 4.2: Implement ESPhomeHttpRelayController**
(Implementation will use OkHttp to send JSON POST requests to the ESPHome REST API)

- [ ] **Step 4.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/TasmotaRelayController.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/system/drivers/ESPhomeHttpRelayController.kt
git commit -m "feat: add Tasmota and ESPHome HTTP relay controllers"
```

---

## Task 5: Factory & Configuration Models

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayControllerConfig.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/system/RelayControllerFactory.kt`

- [ ] **Step 5.1: Implement RelayControllerConfig**

```kotlin
package fr.mathgl.darkroomtimer.system

sealed class RelayControllerConfig {
    object Null : RelayControllerConfig()
    object Demo : RelayControllerConfig()
    data class Tasmota(
        val host: String,
        val port: Int = 80,
        val username: String = "",
        val password: String = "",
        val channel: Int = 1,
        val timingMode: TimingMode = TimingMode.TIMED_POWER
    ) : RelayControllerConfig()
    data class ESPhomeHttp(
        val host: String,
        val port: Int = 80,
        val entityId: String
    ) : RelayControllerConfig()
}

enum class TimingMode { TIMED_POWER, EXPLICIT_ON_OFF }
```

- [ ] **Step 5.2: Implement RelayControllerFactory**

```kotlin
package fr.mathgl.darkroomtimer.system

import fr.mathgl.darkroomtimer.system.drivers.*
import kotlinx.coroutines.CoroutineDispatcher

object RelayControllerFactory {
    fun create(config: RelayControllerConfig, dispatcher: CoroutineDispatcher): RelayController =
        when (config) {
            is RelayControllerConfig.Null -> NullRelayController()
            is RelayControllerConfig.Demo -> DemoRelayController()
            is RelayControllerConfig.Tasmota -> TasmotaRelayController(config, dispatcher)
            is RelayControllerConfig.ESPhomeHttp -> ESPhomeHttpRelayController(config, dispatcher)
        }
}
```

- [ ] **Step 5.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/system/RelayControllerConfig.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/system/RelayControllerFactory.kt
git commit -m "feat: add relay controller config and factory"
```
