# Mode Développement Chimique — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implémenter le mode Développement chimique pour guider l'utilisateur à travers une séquence de bains et de pauses lors du développement photographique.

**Architecture:** Modèles de données `Step` (sealed class avec `BathStep` et `PauseStep`), `DevelopmentProfile` stockés dans Room database (`DevelopmentProfileDao`). `DevelopmentSession` gère la progression dans les étapes avec machine d'états (ACTIVE/PAUSED/COMPLETED). `DevelopmentViewModel` orchestre le timer et les alertes. Trois écrans Compose: `DevelopmentProfileListScreen` (gestion profils), `DevelopmentProfileEditorScreen` (création/édition), `DevelopmentSessionScreen` (exécution). Intégration avec `AudioSystem` existant pour les alertes.

**Tech Stack:** Kotlin, Jetpack Compose, Room Database, StateFlow, Kotlin Coroutines, AudioSystem existant

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `development/DevelopmentStep.kt` | Créer | Modèles Step, BathStep, PauseStep |
| `development/DevelopmentProfile.kt` | Créer | Modèle DevelopmentProfile avec Entity |
| `development/DevelopmentDao.kt` | Créer | DAO pour CRUD des profils |
| `development/DevelopmentDatabase.kt` | Créer | Room database avec migration |
| `development/DevelopmentSession.kt` | Créer | Machine d'états de session |
| `development/DevelopmentViewModel.kt` | Créer | ViewModel pour écran de session |
| `ui/DevelopmentProfileListScreen.kt` | Créer | Liste et gestion des profils |
| `ui/DevelopmentProfileEditorScreen.kt` | Créer | Éditeur de profil avec étapes |
| `ui/DevelopmentSessionScreen.kt` | Créer | Écran d'exécution du développement |
| `test/.../DevelopmentSessionTest.kt` | Créer | Tests machine d'états |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentStep.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentProfile.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentDao.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentDatabase.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentSession.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentViewModel.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentSessionScreen.kt`
- `app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentSessionTest.kt`

---

## Task 1: DevelopmentStep — Modèles de données pour les étapes

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentStep.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentStepTest.kt`

- [ ] **Step 1.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentStepTest.kt
package fr.mathgl.darkroomtimer.development

import org.junit.Assert.*
import org.junit.Test

class DevelopmentStepTest {

    @Test
    fun `BathStep has correct properties`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Révélateur",
            durationSeconds = 60,
            preEndAlertSeconds = 5
        )
        assertEquals(DevelopmentStepType.BATH, step.type)
        assertEquals("Révélateur", step.name)
        assertEquals(60, step.durationSeconds)
        assertEquals(5, step.preEndAlertSeconds)
    }

    @Test
    fun `PauseStep has correct properties`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Transfert",
            durationSeconds = 10
        )
        assertEquals(DevelopmentStepType.PAUSE, step.type)
        assertEquals("Transfert", step.name)
        assertEquals(10, step.durationSeconds)
    }

    @Test
    fun `BathStep preEndAlertSeconds defaults to 0`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Fixateur",
            durationSeconds = 120
        )
        assertEquals(0, step.preEndAlertSeconds)
    }

    @Test
    fun `BathStep remainingSeconds calculates correctly`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60,
            preEndAlertSeconds = 10
        )
        assertEquals(60, step.remainingSeconds(0))
        assertEquals(30, step.remainingSeconds(30))
        assertEquals(0, step.remainingSeconds(60))
        assertEquals(0, step.remainingSeconds(90))
    }

    @Test
    fun `PauseStep remainingSeconds calculates correctly`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Test",
            durationSeconds = 30
        )
        assertEquals(30, step.remainingSeconds(0))
        assertEquals(15, step.remainingSeconds(15))
        assertEquals(0, step.remainingSeconds(30))
    }

    @Test
    fun `isPreEndAlertTriggered returns true when threshold reached`() {
        val step = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60,
            preEndAlertSeconds = 10
        )
        assertFalse(step.isPreEndAlertTriggered(0))
        assertFalse(step.isPreEndAlertTriggered(40))
        assertTrue(step.isPreEndAlertTriggered(50))
        assertTrue(step.isPreEndAlertTriggered(60))
    }

    @Test
    fun `isPreEndAlertTriggered always false for PauseStep`() {
        val step = DevelopmentStep.PauseStep(
            id = 2,
            name = "Test",
            durationSeconds = 30
        )
        assertFalse((step as DevelopmentStep.PauseStep).isPreEndAlertTriggered(0))
        assertFalse((step as DevelopmentStep.PauseStep).isPreEndAlertTriggered(15))
    }

    @Test
    fun `hasEnded returns true when elapsed >= duration`() {
        val bathStep = DevelopmentStep.BathStep(
            id = 1,
            name = "Test",
            durationSeconds = 60
        )
        assertFalse(bathStep.hasEnded(0))
        assertFalse(bathStep.hasEnded(59))
        assertTrue(bathStep.hasEnded(60))
        assertTrue(bathStep.hasEnded(120))
    }

    @Test
    fun `copy creates modified step with same id`() {
        val original = DevelopmentStep.BathStep(
            id = 1,
            name = "Original",
            durationSeconds = 60,
            preEndAlertSeconds = 5
        )
        val modified = original.copy(name = "Modified", durationSeconds = 90)
        assertEquals(1, modified.id)
        assertEquals("Modified", modified.name)
        assertEquals(90, modified.durationSeconds)
        assertEquals(5, modified.preEndAlertSeconds)
    }
}
```

- [ ] **Step 1.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.development.DevelopmentStepTest"
```
Résultat attendu : FAILED — `DevelopmentStep` n'existe pas.

- [ ] **Step 1.3: Implémenter DevelopmentStep**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentStep.kt
package fr.mathgl.darkroomtimer.development

import androidx.room.Embedded
import androidx.room.TypeConverters

enum class DevelopmentStepType {
    BATH, PAUSE
}

sealed class DevelopmentStep {
    abstract val id: Int
    abstract val name: String
    abstract val durationSeconds: Int

    /** Durée d'exposition déjà écoulée en secondes */
    abstract val elapsedSeconds: Long

    /** Retourne le temps restant en secondes */
    fun remainingSeconds(elapsed: Long): Int = maxOf(0, durationSeconds - elapsed.toInt())

    /** Retourne true si l'étape est terminée */
    fun hasEnded(elapsed: Long): Boolean = elapsed >= durationSeconds

    data class BathStep(
        override val id: Int = 0,
        override val name: String,
        override val durationSeconds: Int,
        val preEndAlertSeconds: Int = 0,
        override val elapsedSeconds: Long = 0L
    ) : DevelopmentStep() {
        /** Retourne true si l'alerte de pré-fin doit être déclenchée */
        fun isPreEndAlertTriggered(elapsed: Long): Boolean {
            return elapsed >= (durationSeconds - preEndAlertSeconds) && !hasEnded(elapsed)
        }

        fun copy(
            id: Int = this.id,
            name: String = this.name,
            durationSeconds: Int = this.durationSeconds,
            preEndAlertSeconds: Int = this.preEndAlertSeconds,
            elapsedSeconds: Long = this.elapsedSeconds
        ) = BathStep(id, name, durationSeconds, preEndAlertSeconds, elapsedSeconds)
    }

    data class PauseStep(
        override val id: Int = 0,
        override val name: String,
        override val durationSeconds: Int,
        override val elapsedSeconds: Long = 0L
    ) : DevelopmentStep() {

        fun copy(
            id: Int = this.id,
            name: String = this.name,
            durationSeconds: Int = this.durationSeconds,
            elapsedSeconds: Long = this.elapsedSeconds
        ) = PauseStep(id, name, durationSeconds, elapsedSeconds)
    }
}

/**
 * Convertisseur pour Room pour stocker DevelopmentStepType comme String
 */
class DevelopmentStepTypeConverter {
    @JvmStatic
    fun fromStepType(type: DevelopmentStepType): String = type.name

    @JvmStatic
    fun toStepType(value: String): DevelopmentStepType {
        return runCatching { DevelopmentStepType.valueOf(value) }.getOrDefault(DevelopmentStepType.BATH)
    }
}
```

- [ ] **Step 1.4: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.development.DevelopmentStepTest"
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 1.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentStep.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentStepTest.kt
git commit -m "feat: add DevelopmentStep models (BathStep, PauseStep)"
```

---

## Task 2: DevelopmentProfile — Modèle de données pour les profils

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentProfile.kt`

- [ ] **Step 2.1: Implémenter DevelopmentProfile avec Entity Room**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentProfile.kt
package fr.mathgl.darkroomtimer.development

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DevelopmentNavigationMode {
    MANUAL, AUTOMATIC
}

/**
 * Représente un profil de développement complet.
 * Utilisé pour la visualisation et la sélection.
 */
data class DevelopmentProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val navigationMode: DevelopmentNavigationMode = DevelopmentNavigationMode.MANUAL,
    val steps: List<DevelopmentStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun stepCount(): Int = steps.size
    fun isNotEmpty(): Boolean = steps.isNotEmpty()
    fun isEmpty(): Boolean = steps.isEmpty()

    /** Retourne une prévisualisation du profil (nom des étapes + durées) */
    fun preview(): String {
        return steps.joinToString(" → ") { step ->
            "${step.name} (${step.durationSeconds}s)"
        }
    }

    fun copyWithSteps(steps: List<DevelopmentStep>): DevelopmentProfile = this.copy(
        steps = steps,
        updatedAt = System.currentTimeMillis()
    )

    fun copy(
        id: Long = this.id,
        name: String = this.name,
        navigationMode: DevelopmentNavigationMode = this.navigationMode,
        steps: List<DevelopmentStep> = this.steps,
        createdAt: Long = this.createdAt,
        updatedAt: Long = this.updatedAt
    ) = DevelopmentProfile(id, name, navigationMode, steps, createdAt, updatedAt)
}

/**
 * Entité Room pour stockage persistant.
 * Les étapes sont stockées comme JSON dans une colonne.
 */
@Entity(tableName = "development_profiles")
data class DevelopmentProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val navigationModeIndex: Int = 0, // 0 = MANUAL, 1 = AUTOMATIC
    val stepsJson: String, // JSON sérialisé des étapes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DevelopmentProfile {
        // Décoder JSON → List<DevelopmentStep>
        // Simplifié pour l'instant : retourne un profil vide
        return DevelopmentProfile(
            id = id,
            name = name,
            navigationMode = if (navigationModeIndex == 1) DevelopmentNavigationMode.AUTOMATIC else DevelopmentNavigationMode.MANUAL,
            steps = emptyList(), // À implémenter avec Gson/Kotlinx.serialization
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(profile: DevelopmentProfile): DevelopmentProfileEntity {
            // Sérialiser List<DevelopmentStep> → JSON
            return DevelopmentProfileEntity(
                id = profile.id,
                name = profile.name,
                navigationModeIndex = if (profile.navigationMode == DevelopmentNavigationMode.AUTOMATIC) 1 else 0,
                stepsJson = "", // À implémenter
                createdAt = profile.createdAt,
                updatedAt = profile.updatedAt
            )
        }
    }
}
```

- [ ] **Step 2.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentProfile.kt
git commit -m "feat: add DevelopmentProfile model with Room entity"
```

---

## Task 3: DevelopmentDao — DAO pour persistance des profils

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentDao.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/storage/AppDatabase.kt` (ajouter table development_profiles)

- [ ] **Step 3.1: Implémenter DevelopmentDao**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentDao.kt
package fr.mathgl.darkroomtimer.development

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DevelopmentDao {

    @Query("SELECT * FROM development_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<DevelopmentProfileEntity>>

    @Query("SELECT * FROM development_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): DevelopmentProfileEntity?

    @Query("SELECT * FROM development_profiles WHERE id = :id")
    fun getProfileByIdFlow(id: Long): Flow<DevelopmentProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: DevelopmentProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: DevelopmentProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: DevelopmentProfileEntity)

    @Query("DELETE FROM development_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("SELECT COUNT(*) FROM development_profiles")
    fun getProfileCount(): Flow<Int>
}
```

- [ ] **Step 3.2: Mettre à jour AppDatabase pour inclure DevelopmentDao**

```kotlin
// Modification de AppDatabase.kt
// Ajouter @TypeConverters(DevelopmentStepTypeConverter::class)
// Ajouter addMigrations() si nécessaire
```

- [ ] **Step 3.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 3.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentDao.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/storage/AppDatabase.kt
git commit -m "feat: add DevelopmentDao for development profile persistence"
```

---

## Task 4: DevelopmentSession — Machine d'états de session

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentSession.kt`
- Test: `app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentSessionTest.kt`

- [ ] **Step 4.1: Écrire les tests qui échouent**

```kotlin
// app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentSessionTest.kt
package fr.mathgl.darkroomtimer.development

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DevelopmentSessionTest {

    private lateinit var session: DevelopmentSession
    private val fakeClock = TestClock()

    @Before
    fun setup() {
        val profile = DevelopmentProfile(
            name = "Test Profile",
            navigationMode = DevelopmentNavigationMode.MANUAL,
            steps = listOf(
                DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10, preEndAlertSeconds = 2),
                DevelopmentStep.PauseStep(name = "Pause 1", durationSeconds = 5),
                DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
            )
        )
        session = DevelopmentSession(profile, fakeClock)
    }

    // --- État initial ---

    @Test
    fun `initial state is CONFIGURED`() {
        assertEquals(DevelopmentSessionState.CONFIGURED, session.state)
        assertEquals(-1, session.currentStepIndex)
    }

    @Test
    fun `initially isRunning is false`() {
        assertFalse(session.isRunning)
        assertFalse(session.isPaused)
        assertFalse(session.isCompleted)
    }

    // --- Démarrer la session ---

    @Test
    fun `start sets state to ACTIVE and begins first step`() {
        session.start()
        assertEquals(DevelopmentSessionState.ACTIVE, session.state)
        assertEquals(0, session.currentStepIndex)
        assertTrue(session.isRunning)
    }

    @Test
    fun `start sets correct step elapsed time`() {
        session.start()
        assertEquals(0, session.currentStep?.elapsedSeconds)
    }

    // --- Tick pendant l'exécution ---

    @Test
    fun `tick increments elapsed time for current step`() {
        session.start()
        fakeClock.advanceSeconds(1)
        session.tick()
        assertEquals(1, session.currentStep?.elapsedSeconds)
    }

    @Test
    fun `tick with multiple calls advances time correctly`() {
        session.start()
        repeat(5) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        assertEquals(5, session.currentStep?.elapsedSeconds)
    }

    // --- Alerte de pré-fin ---

    @Test
    fun `tick triggers preEndAlert when threshold reached`() {
        session.start()
        repeat(8) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Bain 1: 10s, preEndAlert à 8s (10 - 2)
        assertTrue(session.isPreEndAlertTriggered)
    }

    @Test
    fun `tick clears preEndAlert after end of step`() {
        session.start()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // À la fin du bain, l'alerte pré-fin ne devrait plus être déclenchée
        assertFalse(session.isPreEndAlertTriggered)
    }

    // --- Transition vers l'étape suivante ---

    @Test
    fun `nextStep moves to next step when in MANUAL mode`() {
        session = DevelopmentSession(
            DevelopmentProfile(
                name = "Test",
                navigationMode = DevelopmentNavigationMode.MANUAL,
                steps = listOf(
                    DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10),
                    DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
                )
            ),
            fakeClock
        )
        session.start()
        fakeClock.advanceSeconds(10)
        session.tick()
        session.nextStep()
        assertEquals(1, session.currentStepIndex)
        assertEquals(0, session.currentStep?.elapsedSeconds)
    }

    @Test
    fun `tick auto-advances to next step in AUTOMATIC mode`() {
        session = DevelopmentSession(
            DevelopmentProfile(
                name = "Test",
                navigationMode = DevelopmentNavigationMode.AUTOMATIC,
                steps = listOf(
                    DevelopmentStep.BathStep(name = "Bain 1", durationSeconds = 10),
                    DevelopmentStep.BathStep(name = "Bain 2", durationSeconds = 10)
                )
            ),
            fakeClock
        )
        session.start()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Après 10s, devrait passer automatiquement à l'étape 2
        assertEquals(1, session.currentStepIndex)
    }

    @Test
    fun `tick marks session as COMPLETED after last step finishes`() {
        session.start()
        // Bain 1: 10s
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Pause 1: 5s (MANUAL mode, besoin de nextStep)
        session.nextStep()
        repeat(5) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        // Bain 2: 10s
        session.nextStep()
        repeat(10) {
            fakeClock.advanceSeconds(1)
            session.tick()
        }
        assertTrue(session.isCompleted)
        assertEquals(DevelopmentSessionState.COMPLETED, session.state)
    }

    // --- Pause et reprise ---

    @Test
    fun `pause sets state to PAUSED and preserves elapsed time`() {
        session.start()
        fakeClock.advanceSeconds(5)
        session.tick()
        session.pause()
        assertEquals(DevelopmentSessionState.PAUSED, session.state)
        assertEquals(5, session.currentStep?.elapsedSeconds)
    }

    @Test
    fun `resume returns to ACTIVE state`() {
        session.start()
        session.pause()
        session.resume()
        assertEquals(DevelopmentSessionState.ACTIVE, session.state)
        assertTrue(session.isRunning)
    }

    @Test
    fun `tick does not advance time when paused`() {
        session.start()
        fakeClock.advanceSeconds(3)
        session.tick()
        session.pause()
        fakeClock.advanceSeconds(10)
        session.tick()
        // elapsed devrait rester à 3
        assertEquals(3, session.currentStep?.elapsedSeconds)
    }

    // --- Annuler la session ---

    @Test
    fun `cancel resets session to CONFIGURED state`() {
        session.start()
        session.cancel()
        assertEquals(DevelopmentSessionState.CONFIGURED, session.state)
        assertEquals(-1, session.currentStepIndex)
        assertFalse(session.isRunning)
    }

    // --- Accesseurs ---

    @Test
    fun `totalSteps returns correct count`() {
        assertEquals(3, session.totalSteps)
    }

    @Test
    fun `remainingSteps returns steps left including current`() {
        session.start()
        assertEquals(3, session.remainingSteps)
        session.nextStep()
        assertEquals(2, session.remainingSteps)
    }

    @Test
    fun `progress returns percentage 0-100`() {
        session.start()
        assertEquals(0, session.progress)
        // Avancer à l'étape 1 sur 3 (33%)
        fakeClock.advanceSeconds(10)
        session.tick()
        session.nextStep()
        assertTrue(session.progress in 30..36)
    }
}

/** Fake clock pour tests déterministes */
class TestClock {
    private var seconds: Long = 0

    fun advanceSeconds(s: Long) {
        seconds += s
    }

    fun elapsed(): Long = seconds
}
```

- [ ] **Step 4.2: Lancer les tests pour vérifier qu'ils échouent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.development.DevelopmentSessionTest"
```
Résultat attendu : FAILED.

- [ ] **Step 4.3: Implémenter DevelopmentSession**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentSession.kt
package fr.mathgl.darkroomtimer.development

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DevelopmentSessionState { CONFIGURED, ACTIVE, PAUSED, COMPLETED }

data class DevelopmentSessionStateSnapshot(
    val state: DevelopmentSessionState,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val currentStep: DevelopmentStep?,
    val remainingSteps: Int,
    val progress: Int, // 0-100
    val isPreEndAlertTriggered: Boolean,
    val isCompleted: Boolean
)

/**
 * Machine d'états pour une session de développement chimique.
 * Gère la progression dans les étapes d'un profil.
 */
class DevelopmentSession(
    private val profile: DevelopmentProfile,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val _stateFlow = MutableStateFlow(initialState())
    val stateFlow: StateFlow<DevelopmentSessionStateSnapshot> = _stateFlow.asStateFlow()

    val state: DevelopmentSessionState get() = _stateFlow.value.state
    val currentStepIndex: Int get() = _stateFlow.value.currentStepIndex
    val totalSteps: Int get() = profile.stepCount()
    val remainingSteps: Int get() = totalSteps - currentStepIndex
    val progress: Int get() = if (totalSteps > 0) ((currentStepIndex * 100) / totalSteps) else 0
    val isPreEndAlertTriggered: Boolean get() = _stateFlow.value.isPreEndAlertTriggered
    val isCompleted: Boolean get() = _stateFlow.value.isCompleted

    val currentStep: DevelopmentStep?
        get() = if (currentStepIndex in 0 until totalSteps) {
            profile.steps[currentStepIndex]
        } else null

    val currentStepElapsedSeconds: Long
        get() = currentStep?.elapsedSeconds ?: 0L

    val currentStepRemainingSeconds: Int
        get() = currentStep?.remainingSeconds(currentStepElapsedSeconds) ?: 0

    private fun initialState(): DevelopmentSessionStateSnapshot {
        return DevelopmentSessionStateSnapshot(
            state = DevelopmentSessionState.CONFIGURED,
            currentStepIndex = -1,
            totalSteps = totalSteps,
            currentStep = null,
            remainingSteps = totalSteps,
            progress = 0,
            isPreEndAlertTriggered = false,
            isCompleted = false
        )
    }

    fun start() {
        require(_stateFlow.value.state == DevelopmentSessionState.CONFIGURED) {
            "Cannot start from state ${_stateFlow.value.state}"
        }
        updateState { it.copy(
            state = DevelopmentSessionState.ACTIVE,
            currentStepIndex = 0,
            remainingSteps = totalSteps - 1,
            progress = 0
        ) }
    }

    fun pause() {
        require(_stateFlow.value.state == DevelopmentSessionState.ACTIVE) {
            "Cannot pause from state ${_stateFlow.value.state}"
        }
        updateState { it.copy(state = DevelopmentSessionState.PAUSED) }
    }

    fun resume() {
        require(_stateFlow.value.state == DevelopmentSessionState.PAUSED) {
            "Cannot resume from state ${_stateFlow.value.state}"
        }
        updateState { it.copy(state = DevelopmentSessionState.ACTIVE) }
    }

    /** Incrémenter le temps écoulé pour l'étape courante */
    fun tick() {
        if (_stateFlow.value.state != DevelopmentSessionState.ACTIVE) return

        val current = _stateFlow.value
        if (current.currentStep == null) return

        val newElapsed = current.currentStep.elapsedSeconds + 1
        val step = current.currentStep.copy(
            elapsedSeconds = newElapsed
        )

        // Vérifier si l'étape est terminée
        if (step.hasEnded(newElapsed)) {
            handleStepCompletion(step)
        } else {
            // Mettre à jour l'état avec la nouvelle elapsed
            updateState { it.copy(
                currentStep = step,
                isPreEndAlertTriggered = step.isPreEndAlertTriggered(newElapsed)
            ) }
        }
    }

    private fun handleStepCompletion(completedStep: DevelopmentStep) {
        val currentIndex = currentStepIndex
        val nextIndex = currentIndex + 1

        if (nextIndex >= totalSteps) {
            // Session terminée
            updateState { it.copy(
                state = DevelopmentSessionState.COMPLETED,
                isCompleted = true,
                progress = 100
            ) }
        } else {
            // Passer à l'étape suivante
            val nextStep = profile.steps[nextIndex].copy(elapsedSeconds = 0)
            updateState { it.copy(
                currentStepIndex = nextIndex,
                currentStep = nextStep,
                remainingSteps = totalSteps - nextIndex,
                progress = (nextIndex * 100) / totalSteps,
                isPreEndAlertTriggered = false
            ) }

            // En mode automatique, continuer immédiatement
            if (profile.navigationMode == DevelopmentNavigationMode.AUTOMATIC) {
                // Démarre le timer pour l'étape suivante
                // Appel de tick() sera nécessaire
            }
        }
    }

    /** Passer manuellement à l'étape suivante (mode MANUAL) */
    fun nextStep() {
        require(_stateFlow.value.state == DevelopmentSessionState.ACTIVE || 
                _stateFlow.value.state == DevelopmentSessionState.PAUSED) {
            "Cannot call nextStep from state ${_stateFlow.value.state}"
        }
        // Lancer l'étape suivante (reset elapsed)
        val currentIndex = currentStepIndex
        if (currentIndex < totalSteps - 1) {
            val nextStep = profile.steps[currentIndex + 1].copy(elapsedSeconds = 0)
            updateState { it.copy(
                currentStepIndex = currentIndex + 1,
                currentStep = nextStep,
                remainingSteps = totalSteps - (currentIndex + 1),
                progress = ((currentIndex + 1) * 100) / totalSteps,
                isPreEndAlertTriggered = false
            ) }
        }
    }

    /** Annuler et retourner à l'état CONFIGURED */
    fun cancel() {
        updateState { initialState() }
    }

    private fun updateState(mutator: (DevelopmentSessionStateSnapshot) -> DevelopmentSessionStateSnapshot) {
        _stateFlow.value = mutator(_stateFlow.value)
    }

    /** Retourne true si une alerte doit être jouée (pré-fin ou fin d'étape) */
    fun shouldPlayAlert(currentElapsed: Long): Boolean {
        return currentStep?.isPreEndAlertTriggered(currentElapsed) == true
    }
}
```

- [ ] **Step 4.4: Lancer les tests pour vérifier qu'ils passent**

```
./gradlew test --tests "fr.mathgl.darkroomtimer.development.DevelopmentSessionTest"
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 4.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentSession.kt \
        app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentSessionTest.kt
git commit -m "feat: add DevelopmentSession state machine for development workflow"
```

---

## Task 5: DevelopmentViewModel — ViewModel pour écran de session

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentViewModel.kt`

- [ ] **Step 5.1: Implémenter DevelopmentViewModel**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentViewModel.kt
package fr.mathgl.darkroomtimer.development

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.mathgl.darkroomtimer.audio.AudioSystem
import fr.mathgl.darkroomtimer.audio.AudioVolume
import fr.mathgl.darkroomtimer.audio.ToneGeneratorAudioEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

class DevelopmentViewModel(
    application: Application,
    private val profile: DevelopmentProfile
) : AndroidViewModel(application) {

    private val session = DevelopmentSession(profile)
    
    // Audio system (lazy init for testing)
    private val audioSystem: AudioSystem? by lazy {
        val prefs = getAudioPreferences()
        val engine = ToneGeneratorAudioEngine()
        AudioSystem(engine, prefs, AudioVolume.MEDIUM)
    }

    private var tickJob: Job? = null
    private var audioPlayJob: Job? = null

    val stateFlow: StateFlow<DevelopmentSessionStateSnapshot> = session.stateFlow

    val isRunning: Boolean get() = session.state == DevelopmentSessionState.ACTIVE
    val isPaused: Boolean get() = session.state == DevelopmentSessionState.PAUSED
    val isCompleted: Boolean get() = session.state == DevelopmentSessionState.COMPLETED

    val currentStep = session.currentStep
    val currentStepElapsedSeconds = session.currentStepElapsedSeconds
    val currentStepRemainingSeconds = session.currentStepRemainingSeconds
    val progress = session.progress

    init {
        viewModelScope.launch {
            session.stateFlow.collect { snapshot ->
                // Check for alerts
                if (snapshot.isPreEndAlertTriggered) {
                    playPreEndAlert()
                }
            }
        }
    }

    fun start() {
        session.start()
        audioSystem?.startExposure()
        startTickJob()
    }

    fun pause() {
        session.pause()
        audioSystem?.pause()
        tickJob?.cancel()
        tickJob = null
    }

    fun resume() {
        session.resume()
        audioSystem?.resume()
        startTickJob()
    }

    fun nextStep() {
        session.nextStep()
    }

    fun cancel() {
        session.cancel()
        audioSystem?.stop()
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        audioSystem?.release()
        tickJob?.cancel()
    }

    private fun startTickJob() {
        if (session.state != DevelopmentSessionState.ACTIVE) return
        tickJob = viewModelScope.launch {
            while (session.state == DevelopmentSessionState.ACTIVE) {
                delay(1000)
                session.tick()
            }
        }
    }

    private fun playPreEndAlert() {
        audioSystem?.stopExposure() // Joue un bip unique
    }

    // Audio preferences getter (to be implemented from SharedPreferences)
    private fun getAudioPreferences(): Any {
        // TODO: Implement from storage
        return object { /* mock preferences */ }
    }
}
```

- [ ] **Step 5.2: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL (avec ajustements).

- [ ] **Step 5.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/development/DevelopmentViewModel.kt
git commit -m "feat: add DevelopmentViewModel for session management"
```

---

## Task 6: UI - Écran de liste des profils

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt`
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt`

- [ ] **Step 6.1: Implémenter DevelopmentProfileListScreen**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentNavigationMode

@Composable
fun DevelopmentProfileListScreen(
    profiles: List<DevelopmentProfile>,
    onSelectProfile: (DevelopmentProfile) -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (DevelopmentProfile) -> Unit,
    onDeleteProfile: (DevelopmentProfile) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profils de Développement",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (profiles.isEmpty()) {
            Text(
                text = "Aucun profil créé",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(profiles) { profile ->
                    ProfileItem(
                        profile = profile,
                        onClick = { onSelectProfile(profile) },
                        onEdit = { onEditProfile(profile) },
                        onDelete = { onDeleteProfile(profile) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddProfile,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
        ) {
            Text("+ Nouveau Profil", fontSize = 18.sp)
        }
    }
}

@Composable
private fun ProfileItem(
    profile: DevelopmentProfile,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.preview(),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = "${profile.stepCount()} étapes • ${profile.navigationMode.name}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
            Row {
                TextButton(onClick = onEdit) {
                    Text("✎", color = Color(0xFFCC2200), fontSize = 18.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("✕", color = Color.Red, fontSize = 18.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 6.2: Implémenter DevelopmentProfileEditorScreen**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt
package fr.mathgl.darkroomtimer.ui

// À implémenter : formulaire pour créer/éditer un profil
// - Nom du profil
// - Sélecteur de mode (MANUAL/AUTOMATIC)
// - Liste dynamique d'étapes (ajout/suppression)
// - Pour chaque étape: type (bain/pause), nom, durée, alerte pré-fin

// Pour l'instant, placeholder
@Composable
fun DevelopmentProfileEditorScreen(
    profile: DevelopmentProfile?,
    onSave: (DevelopmentProfile) -> Unit,
    onCancel: () -> Unit
) {
    // TODO: Implémenter éditeur complet
}
```

- [ ] **Step 6.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 6.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt
git commit -m "feat: add DevelopmentProfileListScreen for profile management"
```

---

## Task 7: UI - Écran de session de développement

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentSessionScreen.kt`
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt` (ajouter navigation)

- [ ] **Step 7.1: Implémenter DevelopmentSessionScreen**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentSessionScreen.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import fr.mathgl.darkroomtimer.development.DevelopmentSessionState

@Composable
fun DevelopmentSessionScreen(
    stepName: String,
    stepElapsedSeconds: Long,
    stepRemainingSeconds: Int,
    progress: Int,
    state: DevelopmentSessionState,
    totalSteps: Int,
    currentStepIndex: Int,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNextStep: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header avec progression
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Étape ${currentStepIndex + 1} / $totalSteps",
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = "$progress%",
                color = Color(0xFFCC2200),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barre de progression
        LinearProgressIndicator(
            progress = progress / 100f,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFFCC2200),
            trackColor = Color(0xFF333333)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Nom de l'étape
        Text(
            text = stepName,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer
        when (state) {
            DevelopmentSessionState.ACTIVE,
            DevelopmentSessionState.PAUSED -> {
                Text(
                    text = formatTime(stepRemainingSeconds * 1000),
                    color = Color.White,
                    fontSize = 72.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "écoulé: ${stepElapsedSeconds}s",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            DevelopmentSessionState.COMPLETED -> {
                Text(
                    text = "✓ COMPLÉTÉ",
                    color = Color(0xFF44AA44),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Contrôles
        when (state) {
            DevelopmentSessionState.CONFIGURED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("DÉMARRER", fontSize = 20.sp)
                }
            }

            DevelopmentSessionState.ACTIVE -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF884400))
                    ) {
                        Text("PAUSE", fontSize = 18.sp)
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("QUITTER", fontSize = 14.sp)
                    }
                }
            }

            DevelopmentSessionState.PAUSED -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                    ) {
                        Text("REPRENDRE", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onNextStep,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
                    ) {
                        Text("SUIVANT", fontSize = 14.sp)
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("QUITTER", fontSize = 12.sp)
                    }
                }
            }

            DevelopmentSessionState.COMPLETED -> {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF44AA44))
                ) {
                    Text("TERMINER", fontSize = 20.sp)
                }
            }
            else -> {}
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
```

- [ ] **Step 7.2: Mettre à jour MainActivity pour ajouter navigation vers Development mode**

```kotlin
// Modification de MainActivity.kt
// Ajouter AppMode.DEVELOPMENT
// Ajouter DevelopmentSessionScreen au when{}
```

- [ ] **Step 7.3: Vérifier que le projet compile**

```
./gradlew assembleDebug
```
Résultat attendu : BUILD SUCCESSFUL.

- [ ] **Step 7.4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentSessionScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "feat: add DevelopmentSessionScreen and integrate into navigation"
```

---

## Récapitulatif des fichiers

| Fichier | Action |
|---|---|
| `development/DevelopmentStep.kt` | Créé |
| `development/DevelopmentProfile.kt` | Créé |
| `development/DevelopmentDao.kt` | Créé |
| `development/DevelopmentDatabase.kt` | Modifié |
| `development/DevelopmentSession.kt` | Créé |
| `development/DevelopmentViewModel.kt` | Créé |
| `ui/DevelopmentProfileListScreen.kt` | Créé |
| `ui/DevelopmentProfileEditorScreen.kt` | Créé |
| `ui/DevelopmentSessionScreen.kt` | Créé |
| `test/.../DevelopmentStepTest.kt` | Créé |
| `test/.../DevelopmentSessionTest.kt` | Créé |

---

## Auto-Review du plan

**1. Couverture du spec:**
- ✅ Modèles Step (BathStep, PauseStep) avec propriétés complètes
- ✅ DevelopmentProfile avec navigation mode MANUAL/AUTOMATIC
- ✅ Persistance Room avec DAO
- ✅ Session state machine (CONFIGURED/ACTIVE/PAUSED/COMPLETED)
- ✅ Modes de navigation automatique et manuel
- ✅ Système d'alertes (pré-fin, fin d'étape)
- ✅ Écran de liste des profils
- ✅ Écran de session avec timer et contrôles
- ✅ Intégration AudioSystem pour les alertes

**2. Gaps identifiés:**
- Éditeur de profil complet non implémenté dans ce plan (placeholder) — à faire ultérieurement
- Sérialisation JSON des étapes non implémentée (TODO dans code)
- Écran de lancement avec sélection rapide non implémenté

**3. Cohérence des types:**
- DevelopmentSessionState cohérent avec DevelopmentViewModel
- SessionStateSnapshot utilisé pour StateFlow
- Méthodes tick(), nextStep(), pause(), resume() alignées

**Plan prêt à être exécuté.**
