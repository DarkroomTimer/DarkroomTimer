# Settings Screen & Bottom Navigation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement SettingsScreen (all preferences sections), EnlargerProfilesScreen (CRUD for enlarger profiles in Room), and restructure MainActivity to use Material3 NavigationBar with 3 tabs (Exposition, Teststrip, Réglages) — Development mode stays accessible from the Exposition tab as a full-screen overlay.

**Architecture:** SettingsScreen reads/writes `PreferenceManager` directly (no ViewModel — scalar prefs only). EnlargerProfilesScreen follows the DevelopmentProfileListScreen pattern: creates its ViewModel inline via `LaunchedEffect` with `AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.Default))`. MainActivity replaces the simple mode-selector with `Scaffold { NavigationBar(...) }`. When Development is active it covers the full content area (NavigationBar is hidden).

**Tech Stack:** Kotlin, Jetpack Compose, Material3 NavigationBar, Room Database, PreferenceManager (SharedPreferences), StateFlow, Coroutines

---

## File Structure

| Fichier | Action | Responsabilité |
|---|---|---|
| `ui/SettingsScreen.kt` | Créer | Sections GÉNÉRAL, MÉTRONOME, AGRANDISSEUR, BLUETOOTH, DONNÉES |
| `ui/EnlargerProfilesScreen.kt` | Créer | Liste et éditeur inline des profils agrandisseur |
| `MainActivity.kt` | Modifier | Scaffold + NavigationBar 3 onglets + Development overlay |

**Chemins complets** (package = `fr.mathgl.darkroomtimer`) :
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/ui/EnlargerProfilesScreen.kt`
- `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

---

## Context: État actuel vs spec

**Implémenté :** CountdownScreen, TeststripScreen, Development flow complet (LIST→LAUNCH→SESSION), AudioSystem, RelaySystem, FStopMath, Room DB (EnlargerProfile + DevelopmentProfile), PreferenceManager avec toutes les clés.

**Manquant :**
- `SettingsScreen` — aucun fichier existant
- `EnlargerProfilesScreen` — aucune UI pour gérer les profils agrandisseur
- Bottom Navigation — l'écran actuel est un sélecteur de mode basique avec 3 boutons

---

## Task 1: SettingsScreen

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`

**Context:** `PreferenceManager.getInstance(context)` gère tous les scalaires. `AudioVolume` a 4 valeurs (MUTE/QUIET/MEDIUM/LOUD). `ContrastGrade` a 12 valeurs avec `.label` (ex: "2", "½"). Les fractions f-stop stockées en numérateur+dénominateur (ex: 1/3 → numerator=1, denominator=3). Pas de test unitaire possible pour un composable pur — vérification visuelle dans l'app.

- [ ] **Step 1.1: Créer SettingsScreen.kt**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt
package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.audio.AudioVolume
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.storage.PreferenceManager

@Composable
fun SettingsScreen(
    onNavigateToEnlargerProfiles: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getInstance(context) }

    var buzzerVolume by remember { mutableStateOf(AudioVolume.fromString(prefs.buzzerVolume)) }
    var startBeepEnabled by remember { mutableStateOf(prefs.startBeepEnabled) }
    var metronomeEnabled by remember { mutableStateOf(prefs.metronomeEnabled) }
    var metronomeCadenceMs by remember { mutableStateOf(prefs.metronomeCadenceMs) }
    var defaultGrade by remember { mutableStateOf(prefs.defaultContrastGrade) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // GÉNÉRAL
        SettingsSectionHeader("GÉNÉRAL")
        SettingsDropdown(
            label = "Volume audio",
            options = AudioVolume.values().map { it.name },
            selected = buzzerVolume.name,
            onSelect = { name ->
                val v = AudioVolume.fromString(name)
                buzzerVolume = v
                prefs.buzzerVolume = v.name
            }
        )
        SettingsSwitch(
            label = "Bip au démarrage",
            checked = startBeepEnabled,
            onCheckedChange = { startBeepEnabled = it; prefs.startBeepEnabled = it }
        )

        // MÉTRONOME
        SettingsSectionHeader("MÉTRONOME")
        SettingsSwitch(
            label = "Métronome",
            checked = metronomeEnabled,
            onCheckedChange = { metronomeEnabled = it; prefs.metronomeEnabled = it }
        )
        if (metronomeEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Cadence : ${metronomeCadenceMs} ms",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = metronomeCadenceMs.toFloat(),
                    onValueChange = { v ->
                        metronomeCadenceMs = v.toInt()
                        prefs.metronomeCadenceMs = v.toInt()
                    },
                    valueRange = 500f..3000f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFCC2200),
                        activeTrackColor = Color(0xFFCC2200)
                    )
                )
            }
        }

        // EXPOSITION
        SettingsSectionHeader("EXPOSITION")
        SettingsDropdown(
            label = "Grade par défaut",
            options = ContrastGrade.entries.map { it.label },
            selected = defaultGrade.label,
            onSelect = { label ->
                val grade = ContrastGrade.entries.first { it.label == label }
                defaultGrade = grade
                prefs.defaultContrastGrade = grade
            }
        )

        // TESTSTRIP
        SettingsSectionHeader("TESTSTRIP")
        Text(
            text = "Réglages configurables directement dans l'écran Teststrip.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // AGRANDISSEUR
        SettingsSectionHeader("AGRANDISSEUR")
        TextButton(
            onClick = onNavigateToEnlargerProfiles,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profils agrandisseur", color = Color.White, fontSize = 16.sp)
                Text("▶", color = Color(0xFFCC2200), fontSize = 16.sp)
            }
        }

        // BLUETOOTH
        SettingsSectionHeader("BLUETOOTH")
        Text(
            text = "Mode compagnon WiFi — configuration relay à venir.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // DONNÉES
        SettingsSectionHeader("DONNÉES")
        Text(
            text = "Export / Import JSON — à venir.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFFCC2200),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
    HorizontalDivider(color = Color(0xFF222222))
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFCC2200)
            )
        )
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 16.sp)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected, color = Color(0xFFCC2200), fontSize = 16.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 1.2: Vérifier que ça compile**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 1.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt
git commit -m "feat: add SettingsScreen with preferences bindings"
```

---

## Task 2: EnlargerProfilesScreen

**Files:**
- Create: `app/src/main/java/fr/mathgl/darkroomtimer/ui/EnlargerProfilesScreen.kt`

**Context:** `EnlargerProfileDao` a `getAll(): List<EnlargerProfileEntity>` (suspend, pas Flow), `insert(onConflict=REPLACE)`, `update`, `delete`. `EnlargerProfileEntity(id: Int, name: String, turnOnDelayMs, riseTimeMs, riseTimeEquivMs, turnOffDelayMs, fallTimeMs, fallTimeEquivMs: Int)`. id=0 est réservé pour "Idéal" (ne peut pas être supprimé). Nouveaux profils utilisent ids 1–15. `AppDatabase.getDatabase(context, CoroutineScope(Dispatchers.Default))` pour obtenir la DB.

- [ ] **Step 2.1: Créer EnlargerProfilesScreen.kt**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/ui/EnlargerProfilesScreen.kt
package fr.mathgl.darkroomtimer.ui

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.storage.room.AppDatabase
import fr.mathgl.darkroomtimer.storage.room.EnlargerProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun EnlargerProfilesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf<List<EnlargerProfileEntity>>(emptyList()) }
    var editingProfile by remember { mutableStateOf<EnlargerProfileEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val db = remember { AppDatabase.getDatabase(context.applicationContext as Application, CoroutineScope(Dispatchers.Default)) }
    val dao = remember { db.enlargerProfileDao() }

    LaunchedEffect(Unit) {
        profiles = dao.getAll().sortedBy { it.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profils Agrandisseur",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            TextButton(onClick = onBack) {
                Text("← Retour", color = Color(0xFFCC2200))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Allumage: ${profile.turnOnDelayMs}ms / Extinction: ${profile.turnOffDelayMs}ms",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            TextButton(onClick = {
                                editingProfile = profile
                                showEditor = true
                            }) {
                                Text("✎", color = Color(0xFFCC2200), fontSize = 18.sp)
                            }
                            if (profile.id != 0) {
                                TextButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        dao.delete(profile)
                                        profiles = dao.getAll().sortedBy { it.id }
                                    }
                                }) {
                                    Text("✕", color = Color.Red, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val nextId = (profiles.maxOfOrNull { it.id } ?: 0) + 1
                if (nextId <= 15) {
                    editingProfile = EnlargerProfileEntity(
                        id = nextId, name = "", turnOnDelayMs = 0, riseTimeMs = 0,
                        riseTimeEquivMs = 0, turnOffDelayMs = 0, fallTimeMs = 0, fallTimeEquivMs = 0
                    )
                    showEditor = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200)),
            enabled = profiles.size < 16
        ) {
            Text("+ Nouveau Profil", fontSize = 16.sp)
        }
    }

    if (showEditor) {
        val profile = editingProfile ?: return@Column
        EnlargerProfileEditorDialog(
            profile = profile,
            onSave = { saved ->
                scope.launch(Dispatchers.IO) {
                    dao.insert(saved)
                    profiles = dao.getAll().sortedBy { it.id }
                }
                showEditor = false
                editingProfile = null
            },
            onDismiss = {
                showEditor = false
                editingProfile = null
            }
        )
    }
}

@Composable
private fun EnlargerProfileEditorDialog(
    profile: EnlargerProfileEntity,
    onSave: (EnlargerProfileEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var turnOnDelayMs by remember { mutableStateOf(profile.turnOnDelayMs.toString()) }
    var riseTimeMs by remember { mutableStateOf(profile.riseTimeMs.toString()) }
    var riseTimeEquivMs by remember { mutableStateOf(profile.riseTimeEquivMs.toString()) }
    var turnOffDelayMs by remember { mutableStateOf(profile.turnOffDelayMs.toString()) }
    var fallTimeMs by remember { mutableStateOf(profile.fallTimeMs.toString()) }
    var fallTimeEquivMs by remember { mutableStateOf(profile.fallTimeEquivMs.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (profile.name.isEmpty()) "Nouveau Profil" else profile.name,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileTextField("Nom", name, { name = it }, KeyboardType.Text)
                Text("Allumage", color = Color(0xFFCC2200), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ProfileTextField("Délai ON (ms)", turnOnDelayMs, { turnOnDelayMs = it }, KeyboardType.Number)
                ProfileTextField("Temps montée (ms)", riseTimeMs, { riseTimeMs = it }, KeyboardType.Number)
                ProfileTextField("Montée équiv. (ms)", riseTimeEquivMs, { riseTimeEquivMs = it }, KeyboardType.Number)
                Text("Extinction", color = Color(0xFFCC2200), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ProfileTextField("Délai OFF (ms)", turnOffDelayMs, { turnOffDelayMs = it }, KeyboardType.Number)
                ProfileTextField("Temps descente (ms)", fallTimeMs, { fallTimeMs = it }, KeyboardType.Number)
                ProfileTextField("Descente équiv. (ms)", fallTimeEquivMs, { fallTimeEquivMs = it }, KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            profile.copy(
                                name = name,
                                turnOnDelayMs = turnOnDelayMs.toIntOrNull() ?: 0,
                                riseTimeMs = riseTimeMs.toIntOrNull() ?: 0,
                                riseTimeEquivMs = riseTimeEquivMs.toIntOrNull() ?: 0,
                                turnOffDelayMs = turnOffDelayMs.toIntOrNull() ?: 0,
                                fallTimeMs = fallTimeMs.toIntOrNull() ?: 0,
                                fallTimeEquivMs = fallTimeEquivMs.toIntOrNull() ?: 0
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC2200))
            ) {
                Text("ENREGISTRER")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ANNULER") }
        },
        containerColor = Color(0xFF1A1A1A)
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFCC2200),
            unfocusedBorderColor = Color(0xFF444444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        )
    )
}
```

- [ ] **Step 2.2: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 2.3: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/ui/EnlargerProfilesScreen.kt
git commit -m "feat: add EnlargerProfilesScreen with CRUD for enlarger profiles"
```

---

## Task 3: Bottom Navigation in MainActivity

**Files:**
- Modify: `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`

**Context:** `CountdownScreen()` prend 0 paramètres. `TeststripScreen(onBack)` prend un callback. Le flux Development (LIST → LAUNCH → SESSION) doit rester intact. La NavigationBar disparaît quand Development est actif (mode plein écran). Icônes: `Icons.Default.Timer`, `Icons.Default.GridOn`, `Icons.Default.Settings` — ces icônes viennent du module `material-icons-core` déjà inclus via `material3`.

- [ ] **Step 3.1: Vérifier que les icônes Material sont disponibles**

Ouvrir `app/build.gradle.kts` et vérifier que cette ligne est présente dans `dependencies`:
```kotlin
implementation("androidx.compose.material:material-icons-extended")
```
Si absente, l'ajouter.

- [ ] **Step 3.2: Réécrire MainActivity.kt**

```kotlin
// app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
package fr.mathgl.darkroomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.mathgl.darkroomtimer.development.DevelopmentProfile
import fr.mathgl.darkroomtimer.development.DevelopmentSession
import fr.mathgl.darkroomtimer.system.LuminosityManager
import fr.mathgl.darkroomtimer.ui.*
import fr.mathgl.darkroomtimer.ui.theme.DarkroomTimerTheme

enum class AppTab { EXPOSITION, TESTSTRIP, SETTINGS }
enum class DevelopmentFlowState { LIST, LAUNCH, SESSION }

class MainActivity : ComponentActivity() {
    private lateinit var luminosityManager: LuminosityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        luminosityManager = LuminosityManager(this)
        luminosityManager.setWindow(window)
        enableEdgeToEdge()
        setContent {
            DarkroomTimerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    MainScreen()
                }
            }
        }
    }

    override fun onStart() { super.onStart(); luminosityManager.start() }
    override fun onStop() { super.onStop(); luminosityManager.stop() }
}

@Composable
fun MainScreen() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.EXPOSITION) }

    // Development flow state (overlay, hides nav bar)
    var developmentActive by rememberSaveable { mutableStateOf(false) }
    var devFlowState by rememberSaveable { mutableStateOf(DevelopmentFlowState.LIST) }
    var selectedProfile by rememberSaveable { mutableStateOf<DevelopmentProfile?>(null) }
    var developmentSession by remember { mutableStateOf<DevelopmentSession?>(null) }

    // Settings sub-navigation
    var showEnlargerProfiles by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(developmentSession) {
        while (developmentSession?.isRunning == true) {
            kotlinx.coroutines.delay(1000)
            developmentSession?.tick()
        }
    }

    if (developmentActive) {
        // Full-screen development flow, nav bar hidden
        DevelopmentOverlay(
            devFlowState = devFlowState,
            selectedProfile = selectedProfile,
            developmentSession = developmentSession,
            onDevFlowStateChange = { devFlowState = it },
            onSelectedProfileChange = { selectedProfile = it },
            onDevelopmentSessionChange = { developmentSession = it },
            onExit = {
                developmentActive = false
                developmentSession = null
                selectedProfile = null
                devFlowState = DevelopmentFlowState.LIST
            }
        )
        return
    }

    if (showEnlargerProfiles) {
        EnlargerProfilesScreen(onBack = { showEnlargerProfiles = false })
        return
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0D0D0D)) {
                NavigationBarItem(
                    selected = selectedTab == AppTab.EXPOSITION,
                    onClick = { selectedTab = AppTab.EXPOSITION },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Exposition") },
                    label = { Text("Exposition") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.TESTSTRIP,
                    onClick = { selectedTab = AppTab.TESTSTRIP },
                    icon = { Icon(Icons.Default.GridOn, contentDescription = "Teststrip") },
                    label = { Text("Teststrip") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.SETTINGS,
                    onClick = { selectedTab = AppTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Réglages") },
                    label = { Text("Réglages") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFCC2200),
                        selectedTextColor = Color(0xFFCC2200),
                        indicatorColor = Color(0xFF1A0A0A),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.EXPOSITION -> ExpositionTab(
                    onOpenDevelopment = { developmentActive = true }
                )
                AppTab.TESTSTRIP -> TeststripScreen(onBack = { selectedTab = AppTab.EXPOSITION })
                AppTab.SETTINGS -> SettingsScreen(
                    onNavigateToEnlargerProfiles = { showEnlargerProfiles = true }
                )
            }
        }
    }
}

@Composable
private fun ExpositionTab(onOpenDevelopment: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        CountdownScreen()
        // Development access button (bottom-right FAB)
        androidx.compose.material3.FloatingActionButton(
            onClick = onOpenDevelopment,
            modifier = androidx.compose.ui.Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF222222),
            contentColor = Color.White
        ) {
            Text("Dev", fontSize = 12.sp, color = Color.White)
        }
    }
}

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
        DevelopmentFlowState.LAUNCH -> {
            DevelopmentLaunchScreen(
                onLaunchSession = { profile ->
                    onSelectedProfileChange(profile)
                    onDevelopmentSessionChange(DevelopmentSession(profile))
                    onDevFlowStateChange(DevelopmentFlowState.SESSION)
                },
                onNavigateToProfiles = { onDevFlowStateChange(DevelopmentFlowState.LIST) },
                onBack = { onDevFlowStateChange(DevelopmentFlowState.LIST) }
            )
        }
        DevelopmentFlowState.SESSION -> {
            val session = developmentSession
            if (session != null) {
                DevelopmentSessionScreen(
                    stepName = session.currentStep?.name ?: "Étape",
                    stepElapsedSeconds = session.currentStepElapsedSeconds,
                    stepRemainingSeconds = session.currentStepRemainingSeconds,
                    progress = session.progress,
                    state = session.state,
                    totalSteps = session.totalSteps,
                    currentStepIndex = if (session.currentStepIndex >= 0) session.currentStepIndex + 1 else 0,
                    onStart = { session.start() },
                    onPause = { session.pause() },
                    onResume = { session.resume() },
                    onNextStep = { session.nextStep() },
                    onCancel = onExit
                )
            } else {
                onDevFlowStateChange(DevelopmentFlowState.LIST)
            }
        }
    }
}
```

- [ ] **Step 3.3: Supprimer AppMode et ModeSelectorScreen** (ces enums/composables ne sont plus utilisés)

Retirer de `MainActivity.kt` (si encore présents après le remplacement complet) :
- `enum class AppMode`
- `fun ModeSelectorScreen()`

- [ ] **Step 3.4: Compiler**

```
./gradlew assembleDebug
```
Résultat attendu: BUILD SUCCESSFUL.

- [ ] **Step 3.5: Commit**

```bash
git add app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt
git commit -m "feat: replace mode selector with NavigationBar (3 tabs) and Development overlay"
```

---

## Vérification

1. **Compiler** : `./gradlew assembleDebug` — BUILD SUCCESSFUL
2. **Tests unitaires** : `./gradlew test` — tous PASS
3. **Vérification visuelle** (installer l'APK sur un device/émulateur) :
   - Onglet Exposition → CountdownScreen s'affiche avec bouton FAB "Dev"
   - Tap FAB "Dev" → DevelopmentProfileListScreen plein écran
   - Onglet Teststrip → TeststripScreen s'affiche
   - Onglet Réglages → SettingsScreen avec sections GÉNÉRAL/MÉTRONOME/AGRANDISSEUR
   - "Profils agrandisseur" ▶ → EnlargerProfilesScreen avec profil "Idéal" pré-existant
   - Basculer entre onglets conserve l'état (rememberSaveable)
