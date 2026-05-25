# Development Navigation Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the development mode navigation so the main screen shows the default profile directly, with delete/edit working correctly and a back button to profile selection.

**Architecture:** The current flow enters development mode at the profile list screen (LIST). The new flow enters at a launch screen (LAUNCH) showing the default/selected profile. From LAUNCH, a back button goes to LIST for profile management (edit/delete). Selecting a profile in LIST returns to LAUNCH with that profile.

**Tech Stack:** Kotlin, Jetpack Compose, Room (AppDatabase/DevelopmentDao), Coroutines

---

## Current vs Target Navigation

**Current:**
```
Dev FAB → LIST (CRUD list) → SESSION
          ↓ back
         EXIT
```

**Target:**
```
Dev FAB → LAUNCH (default profile shown)
                → [back] → LIST (select/edit/delete)
                                 → [select] → LAUNCH (with selected profile)
                                 → [back] → EXIT
                → [launch] → SESSION
```

## Root Causes Found

1. `DevelopmentFlowState.LAUNCH` is never used — initial state is always `LIST`
2. `DevelopmentLaunchScreen` shows a full profile list instead of one profile
3. In `DevelopmentProfileListScreen.ProfileItem`, the `Column` uses `fillMaxWidth()` inside a `Row` — this pushes the edit/delete buttons off screen (they are invisible). This is why delete/edit don't appear to work.
4. Navigation: clicking a profile in LIST goes directly to SESSION, bypassing LAUNCH

---

## File Structure

| File | Action | What changes |
|------|--------|--------------|
| `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt` | Modify | Initial devFlowState = LAUNCH; DevelopmentOverlay navigation wiring |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentLaunchScreen.kt` | Modify | New signature accepting `initialProfile`; shows single profile; `onSelectProfile` button |
| `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt` | Modify | Fix `ProfileItem` layout bug (`fillMaxWidth` → `weight(1f)`); rename `onNavigateToSession` → `onSelectProfile` |
| `app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentListViewModelTest.kt` | Verify | Existing tests must still pass after changes |

---

## Task 1: Fix ProfileItem layout bug (edit/delete invisible)

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt:228`

The `ProfileItem` composable has a `Column(modifier = Modifier.then(Modifier.fillMaxWidth()))` inside a `Row`. `fillMaxWidth()` inside a `Row` consumes all horizontal space, leaving zero width for the edit/delete `Row` beside it. The fix is `weight(1f)`.

- [ ] **Step 1: Write failing test for ProfileItem layout**

Since this is Compose UI, we verify the fix by inspecting the code and building. First confirm the bug exists:

```bash
grep -n "fillMaxWidth" app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt
```

Expected output: line 228 shows `Column(modifier = Modifier.then(Modifier.fillMaxWidth()))` inside `ProfileItem`.

- [ ] **Step 2: Fix the layout bug in ProfileItem**

In `DevelopmentProfileListScreen.kt`, in `ProfileItem`, change:

```kotlin
// Before (line ~228):
Column(modifier = Modifier.then(Modifier.fillMaxWidth())) {
```

To:

```kotlin
Column(modifier = Modifier.weight(1f)) {
```

Also fix the same pattern in `StepDialogItem` (line ~477):

```kotlin
// Before:
Column(modifier = Modifier.then(Modifier.fillMaxWidth())) {
```

To:

```kotlin
Column(modifier = Modifier.weight(1f)) {
```

- [ ] **Step 3: Build to verify fix compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run existing tests**

```bash
./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no test failures.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt
git commit -m "$(cat <<'EOF'
fix: make edit/delete buttons visible in DevelopmentProfileListScreen

ProfileItem used fillMaxWidth() on Column inside a Row, which consumed
all horizontal space and hid the edit/delete buttons. Changed to weight(1f).
EOF
)"
```

---

## Task 2: Rename ProfileListScreen callback and update profile selection

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt`

`onNavigateToSession` currently triggers a direct jump to SESSION. In the new flow, clicking a profile should return to LAUNCH with that profile selected. Renaming the callback to `onSelectProfile` makes the intent clear. The actual navigation logic lives in `DevelopmentOverlay` (MainActivity), not here.

- [ ] **Step 1: Rename the callback in DevelopmentProfileListScreen**

In `DevelopmentProfileListScreen.kt`, change the function signature at line ~39:

```kotlin
// Before:
fun DevelopmentProfileListScreen(
    onNavigateToSession: (DevelopmentProfile) -> Unit,
    onBack: () -> Unit
)
```

To:

```kotlin
fun DevelopmentProfileListScreen(
    onSelectProfile: (DevelopmentProfile) -> Unit,
    onBack: () -> Unit
)
```

In `ProfileItem` call at line ~127, change:

```kotlin
// Before:
onClick = { viewModel?.selectProfile(profile) },
```

The `LaunchedEffect(selectedProfile)` at lines 72-76 currently calls `onNavigateToSession`. Change it to call `onSelectProfile`:

```kotlin
// Before (lines 72-76):
LaunchedEffect(selectedProfile) {
    if (selectedProfile != null && !showEditor) {
        onNavigateToSession(selectedProfile!!)
        viewModel?.deselectProfile()
    }
}
```

To:

```kotlin
LaunchedEffect(selectedProfile) {
    if (selectedProfile != null && !showEditor) {
        onSelectProfile(selectedProfile!!)
        viewModel?.deselectProfile()
    }
}
```

- [ ] **Step 2: Build to verify rename compiles (will fail at MainActivity usage)**

```bash
./gradlew assembleDebug 2>&1 | grep -E "error:|warning:" | head -20
```

Expected: compile error at `MainActivity.kt` because `onNavigateToSession` is now `onSelectProfile`. This is expected — Task 4 will fix it.

- [ ] **Step 3: Temporarily fix MainActivity to use new name (to unblock builds)**

In `MainActivity.kt`, in `DevelopmentOverlay`, find the `DevelopmentFlowState.LIST` branch (~line 195-205):

```kotlin
// Before:
DevelopmentFlowState.LIST -> {
    DevelopmentProfileListScreen(
        onNavigateToSession = { profile ->
            onSelectedProfileChange(profile)
            onDevelopmentSessionChange(DevelopmentSession(profile))
            onDevFlowStateChange(DevelopmentFlowState.SESSION)
        },
        onBack = onExit
    )
}
```

Change to:

```kotlin
DevelopmentFlowState.LIST -> {
    DevelopmentProfileListScreen(
        onSelectProfile = { profile ->
            onSelectedProfileChange(profile)
            onDevFlowStateChange(DevelopmentFlowState.LAUNCH)
        },
        onBack = onExit
    )
}
```

- [ ] **Step 4: Build again to verify it compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run existing tests**

```bash
./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no failures.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt \
        app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "$(cat <<'EOF'
refactor: rename onNavigateToSession to onSelectProfile in profile list

Selecting a profile now returns to LAUNCH with that profile loaded
instead of jumping directly to SESSION.
EOF
)"
```

---

## Task 3: Redesign DevelopmentLaunchScreen to show a single profile

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentLaunchScreen.kt`

Currently `DevelopmentLaunchScreen` loads ALL profiles and shows a list, which duplicates `DevelopmentProfileListScreen`. The redesign makes it show ONE profile (the one passed in via `initialProfile`, or the first from DB if null). It no longer needs `onNavigateToProfiles` — a single `onSelectProfile` callback covers going to LIST.

- [ ] **Step 1: Rewrite DevelopmentLaunchScreen.kt**

Replace the entire content of `DevelopmentLaunchScreen.kt` with:

```kotlin
package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.development.DevelopmentListViewModel
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedDim
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurface
import fr.mathgl.darkroomtimer.ui.theme.DarkroomSurfaceElevated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf

/**
 * Écran principal du mode Développement.
 * Affiche le profil par défaut (ou le profil sélectionné depuis la liste).
 * Le bouton "← Sélectionner un profil" revient à la liste de sélection.
 */
@Composable
fun DevelopmentLaunchScreen(
    initialProfile: DevelopmentProfile?,
    onLaunchSession: (DevelopmentProfile) -> Unit,
    onSelectProfile: () -> Unit
) {
    val context = LocalContext.current
    var resolvedProfile by remember { mutableStateOf(initialProfile) }

    // If no profile was passed in, load the first one from DB as the default
    LaunchedEffect(initialProfile) {
        if (initialProfile == null) {
            val db = AppDatabase.getDatabase(
                context.applicationContext as Application,
                CoroutineScope(Dispatchers.Default)
            )
            val viewModel = DevelopmentListViewModel(context.applicationContext as Application, db.developmentDao())
            viewModel.loadProfiles()
            viewModel.profiles.collect { profiles ->
                if (profiles.isNotEmpty()) {
                    resolvedProfile = profiles.first()
                }
            }
        } else {
            resolvedProfile = initialProfile
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header with back-to-selection button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Développement",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkroomRedBright
            )
            TextButton(onClick = onSelectProfile) {
                Text("← Sélectionner un profil", color = DarkroomRedBright)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val profile = resolvedProfile
        if (profile == null) {
            // Empty state: no profiles in DB
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DarkroomRedBright)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Aucun profil disponible",
                        color = DarkroomRedDim,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onSelectProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomRedBright)
                    ) {
                        Text("Créer un profil", fontSize = 14.sp)
                    }
                }
            }
        } else {
            // Profile card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkroomSurfaceElevated)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = profile.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkroomRedBright
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${profile.stepCount()} étapes • ${profile.navigationMode.name}",
                        fontSize = 14.sp,
                        color = DarkroomRedDim
                    )
                    if (profile.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = profile.preview(),
                            fontSize = 12.sp,
                            color = DarkroomRedDim,
                            maxLines = 3
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Launch button
            Button(
                onClick = { onLaunchSession(profile) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = profile.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkroomRedBright,
                    disabledContainerColor = DarkroomRedDim
                )
            ) {
                Text(
                    text = if (profile.isEmpty()) "Profil vide — ajoutez des étapes" else "LANCER LA SESSION",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run existing tests**

```bash
./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no failures.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentLaunchScreen.kt
git commit -m "$(cat <<'EOF'
feat: redesign DevelopmentLaunchScreen to show single default profile

Replaced the full profile list with a focused view of one profile.
Accepts initialProfile param; falls back to first DB profile if null.
Added "← Sélectionner un profil" button to navigate to profile list.
EOF
)"
```

---

## Task 4: Update MainActivity to use new navigation flow

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

This is the central wiring task. Changes:
1. Initial `devFlowState` = `LAUNCH` (not `LIST`)
2. `DevelopmentOverlay.LAUNCH` case: pass `selectedProfile`, wire `onSelectProfile` → LIST, wire `onLaunchSession` → SESSION
3. `DevelopmentOverlay.LIST` case: wire `onSelectProfile` → sets profile + goes to LAUNCH; `onBack` → EXIT
4. Remove unused `DevelopmentFlowState.LAUNCH` handler that was `onBack → LIST` (now LAUNCH is primary)

- [ ] **Step 1: Update DevelopmentOverlay in MainActivity.kt**

In `MainActivity.kt`, change the `MainScreen` composable's initial state declaration (line ~73):

```kotlin
// Before:
var devFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LIST) }
```

To:

```kotlin
var devFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LAUNCH) }
```

Then replace the entire `DevelopmentOverlay` private composable (lines ~184-246):

```kotlin
@Composable
private fun DevelopmentOverlay(
    devFlowState: DevelopmentFlowState,
    selectedProfile: DevelopmentProfile?,
    developmentSession: DevelopmentSession?,
    onDevFlowStateChange: (DevelopmentFlowState) -> Unit,
    onSelectedProfileChange: (DevelopmentProfile?) -> Unit,
    onDevelopmentSessionChange: (DevelopmentSession?) -> Unit,
    onExit: () -> Unit
) {
    when (devFlowState) {
        DevelopmentFlowState.LAUNCH -> {
            DevelopmentLaunchScreen(
                initialProfile = selectedProfile,
                onLaunchSession = { profile ->
                    onSelectedProfileChange(profile)
                    onDevelopmentSessionChange(DevelopmentSession(profile))
                    onDevFlowStateChange(DevelopmentFlowState.SESSION)
                },
                onSelectProfile = { onDevFlowStateChange(DevelopmentFlowState.LIST) }
            )
        }
        DevelopmentFlowState.LIST -> {
            DevelopmentProfileListScreen(
                onSelectProfile = { profile ->
                    onSelectedProfileChange(profile)
                    onDevFlowStateChange(DevelopmentFlowState.LAUNCH)
                },
                onBack = onExit
            )
        }
        DevelopmentFlowState.SESSION -> {
            val session = developmentSession
            if (session != null) {
                val snapshot by session.stateFlow.collectAsState()

                LaunchedEffect(snapshot.state) {
                    while (session.isRunning) {
                        kotlinx.coroutines.delay(1000)
                        session.tick()
                    }
                }

                DevelopmentSessionScreen(
                    stepName = snapshot.currentStep?.name ?: "Étape",
                    stepElapsedSeconds = snapshot.currentStep?.elapsedSeconds ?: 0L,
                    stepRemainingSeconds = snapshot.currentStep?.let { it.remainingSeconds(it.elapsedSeconds) } ?: 0,
                    progress = snapshot.progress,
                    state = snapshot.state,
                    totalSteps = snapshot.totalSteps,
                    currentStepIndex = if (snapshot.currentStepIndex >= 0) snapshot.currentStepIndex + 1 else 0,
                    onStart = { session.start() },
                    onPause = { session.pause() },
                    onResume = { session.resume() },
                    onNextStep = { session.nextStep() },
                    onCancel = onExit
                )
            } else {
                onDevFlowStateChange(DevelopmentFlowState.LAUNCH)
            }
        }
    }
}
```

Also update the `onExit` lambda in `MainScreen` (lines ~87-94) to reset `devFlowState` back to `LAUNCH`:

```kotlin
// Before:
onExit = {
    developmentActive = false
    developmentSession = null
    selectedProfile = null
    devFlowState = DevelopmentFlowState.LIST
}
```

To:

```kotlin
onExit = {
    developmentActive = false
    developmentSession = null
    selectedProfile = null
    devFlowState = DevelopmentFlowState.LAUNCH
}
```

- [ ] **Step 2: Build to verify it compiles**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Run existing tests**

```bash
./gradlew test 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with no test failures.

- [ ] **Step 4: Run lint**

```bash
./gradlew lint 2>&1 | grep -E "error|Error" | head -20
```

Expected: no new errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "$(cat <<'EOF'
feat: make LAUNCH the entry screen for development mode

Changed initial devFlowState from LIST to LAUNCH so the app shows
the default profile directly when entering development mode.
Back button on LAUNCH navigates to LIST for profile selection/management.
Selecting a profile from LIST returns to LAUNCH with that profile.
EOF
)"
```

---

## Task 5: Verify delete/edit work end-to-end

**Files:**
- No code changes — this is a verification task

- [ ] **Step 1: Run all tests**

```bash
./gradlew test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 2: Verify DevelopmentListViewModel delete test exists**

```bash
grep -n "deleteProfile\|delete" app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentListViewModelTest.kt
```

Expected: No existing test for `deleteProfile`. If none exists, add one:

```kotlin
@Test
fun `deleteProfile calls dao deleteProfileById`() = runTest(testDispatcher) {
    val profile = DevelopmentProfile(id = 1L, name = "Test", steps = emptyList())
    whenever(mockDao.getAllProfiles()).thenReturn(flowOf(emptyList()))
    val viewModel = DevelopmentListViewModel(mockApplication, mockDao)

    viewModel.deleteProfile(profile)
    runCurrent()

    verify(mockDao).deleteProfileById(1L)
}
```

Add this test to `DevelopmentListViewModelTest.kt`.

- [ ] **Step 3: Verify saveProfile (edit) test exists**

```bash
grep -n "saveProfile\|updateProfile" app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentListViewModelTest.kt
```

If no test for `saveProfile` with an existing profile (edit path), add:

```kotlin
@Test
fun `saveProfile updates existing profile in dao`() = runTest(testDispatcher) {
    val profile = DevelopmentProfile(id = 5L, name = "Existing", steps = emptyList())
    whenever(mockDao.getAllProfiles()).thenReturn(flowOf(emptyList()))
    val viewModel = DevelopmentListViewModel(mockApplication, mockDao)

    viewModel.saveProfile(profile)
    runCurrent()

    verify(mockDao).updateProfile(DevelopmentProfileEntity.fromDomain(profile))
}
```

- [ ] **Step 4: Run tests again to confirm new tests pass**

```bash
./gradlew test --tests "fr.mathgl.darkroomtimer.development.DevelopmentListViewModelTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with all tests in that class passing.

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/fr/mathgl/darkroomtimer/development/DevelopmentListViewModelTest.kt
git commit -m "$(cat <<'EOF'
test: add delete and edit ViewModel tests for development profiles
EOF
)"
```

---

## Self-Review Checklist

- [x] **Spec coverage:**
  - "L'écran principale doit être celui du profile par défaut" → Task 3 + Task 4 (LAUNCH is initial screen, shows default/first profile)
  - "On doit pouvoir supprimer un profile" → Task 1 (layout bug fix makes delete button visible) + Task 5 (verification)
  - "On doit pouvoir editer un profile" → Task 1 (layout bug fix makes edit button visible) + Task 5 (verification)
  - "Bouton de retour sur la selection du profile" → Task 3 (← Sélectionner un profil button in LAUNCH) + Task 4 (wires it to LIST)

- [x] **Placeholder scan:** No TBD/TODO in code blocks. All code is complete.

- [x] **Type consistency:**
  - `DevelopmentLaunchScreen(initialProfile: DevelopmentProfile?, onLaunchSession: (DevelopmentProfile) -> Unit, onSelectProfile: () -> Unit)` — consistent with Task 4 usage
  - `DevelopmentProfileListScreen(onSelectProfile: (DevelopmentProfile) -> Unit, onBack: () -> Unit)` — consistent with Task 2 and Task 4 usage
  - `DevelopmentProfile` type is used consistently throughout
