# Design : Écran d'édition de profil de développement

**Date :** 2026-05-25  
**Statut :** Approuvé

## Contexte

L'éditeur de profil de développement existe en plein écran (`DevelopmentProfileEditorScreen.kt`) mais n'est pas câblé à la navigation. Il est actuellement exposé via un `AlertDialog` imbriqué dans `DevelopmentProfileListScreen.kt` (~170 lignes de code dupliqué). L'objectif est de câbler l'écran existant comme destination propre dans le nested graph `development`, en utilisant la navigation Jetpack Compose déjà en place.

## Architecture

### Flux de navigation

```
DevelopmentProfileListScreen
  ├── clic Éditer  → devVM.setEditingProfile(profile) → navigate("development/editor")
  └── clic +       → devVM.setEditingProfile(null)    → navigate("development/editor")

DevelopmentProfileEditorScreen
  ├── lit editingProfile depuis DevelopmentFlowViewModel (partagé sur le graph)
  ├── Enregistrer  → listVM.saveProfile() → clearEditingProfile() → popBackStack()
  └── ← Retour     → clearEditingProfile() → popBackStack()
```

### Passage du profil entre écrans

Le profil à éditer transite via `DevelopmentFlowViewModel` (déjà scopé au nested graph `development`). Évite la sérialisation d'un objet complexe dans un nav argument.

### Bottom bar

Cachée pour la route `development/editor`. Même mécanisme que `ENLARGER_PROFILES` : ajouter la route à l'exclusion dans `showBottomBar` dans `AppNavGraph.kt`.

## Fichiers modifiés

### 1. `ui/navigation/AppRoutes.kt`
Ajouter :
```kotlin
const val DEVELOPMENT_PROFILE_EDITOR = "development/editor"
```

### 2. `ui/DevelopmentFlowViewModel.kt`
Ajouter :
```kotlin
private val _editingProfile = MutableStateFlow<DevelopmentProfile?>(null)
val editingProfile: StateFlow<DevelopmentProfile?> = _editingProfile.asStateFlow()

fun setEditingProfile(profile: DevelopmentProfile?) { _editingProfile.value = profile }
fun clearEditingProfile() { _editingProfile.value = null }
```

### 3. `ui/navigation/AppNavGraph.kt`

**showBottomBar** — exclure la nouvelle route :
```kotlin
val showBottomBar = currentRoute != AppRoutes.ENLARGER_PROFILES
    && currentRoute != AppRoutes.DEVELOPMENT_PROFILE_EDITOR
```

**Composable DEVELOPMENT_LIST** — ajouter les callbacks :
```kotlin
DevelopmentProfileListScreen(
    onSelectProfile = { ... },
    onEditProfile = { profile ->
        devVM.setEditingProfile(profile)
        navController.navigate(AppRoutes.DEVELOPMENT_PROFILE_EDITOR)
    },
    onNewProfile = {
        devVM.setEditingProfile(null)
        navController.navigate(AppRoutes.DEVELOPMENT_PROFILE_EDITOR)
    },
    onBack = { navController.popBackStack() }
)
```

**Nouveau composable** dans le nested graph `development` :
```kotlin
composable(AppRoutes.DEVELOPMENT_PROFILE_EDITOR) { backStackEntry ->
    val devGraphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(AppRoutes.DEVELOPMENT_GRAPH)
    }
    val devVM: DevelopmentFlowViewModel = viewModel(devGraphEntry)
    val editingProfile by devVM.editingProfile.collectAsState()

    val context = LocalContext.current
    var listVM by remember { mutableStateOf<DevelopmentListViewModel?>(null) }
    LaunchedEffect(Unit) {
        val app = context.applicationContext as Application
        val db = AppDatabase.getDatabase(app, CoroutineScope(Dispatchers.Default))
        listVM = DevelopmentListViewModel(app, db.developmentDao())
    }

    DevelopmentProfileEditorScreen(
        profile = editingProfile,
        onSave = { profile ->
            listVM?.saveProfile(profile)
            devVM.clearEditingProfile()
            navController.popBackStack()
        },
        onCancel = {
            devVM.clearEditingProfile()
            navController.popBackStack()
        }
    )
}
```

### 4. `ui/DevelopmentProfileListScreen.kt`

**Signature étendue :**
```kotlin
fun DevelopmentProfileListScreen(
    onSelectProfile: (DevelopmentProfile) -> Unit,
    onEditProfile: (DevelopmentProfile) -> Unit,
    onNewProfile: () -> Unit,
    onBack: () -> Unit
)
```

**Supprimer :**
- Les états `showProfileEditor`, `editingProfile` (et le `LaunchedEffect` qui les gère)
- La fonction composable `DevelopmentProfileEditorDialog` (~lignes 264–436)
- Le `StepEditorDialog` dupliqué dans ce fichier (déjà présent dans `DevelopmentProfileEditorScreen.kt`)

**Remplacer :**
- Bouton `+ Nouveau Profil` → appelle `onNewProfile()`
- Boutons `Éditer` des cartes → appellent `onEditProfile(profile)`

La VM locale reste pour le chargement des profils et la suppression.

**Nettoyer dans `DevelopmentListViewModel.kt`** — supprimer les champs et méthodes devenus code mort :
- `_showEditor`, `showEditor`, `openEditor()`, `closeEditor()`
- Le `LaunchedEffect(showEditor, selectedProfile)` correspondant dans `DevelopmentProfileListScreen.kt`

### 5. `ui/DevelopmentProfileEditorScreen.kt`

Aucun changement fonctionnel. Corriger le bug mineur dans `NavigationModeButton` :
```kotlin
// ligne ~215 : remplacer
color = if (selected) DarkroomRedBright else DarkroomRedDim
// par
color = if (selected) Color.White else DarkroomRedDim
```

## Fichiers non modifiés
- Modèle de données (`DevelopmentProfile.kt`, `DevelopmentStep.kt`)
- Persistence (`DevelopmentDao.kt`, `AppDatabase.kt`)
- Tous les autres écrans

Aucune migration Room requise.

## Vérification

1. **Build** : `./gradlew assembleDebug` — doit compiler sans erreur
2. **Tests unitaires** : `./gradlew test` — aucun test existant ne doit régresser
3. **Scénarios manuels à valider :**
   - Naviguer vers Développement → Gérer les profils → `+` → écran éditeur s'ouvre, bottom bar cachée
   - Créer un profil avec des étapes → Enregistrer → retour liste, profil visible
   - Cliquer Éditer sur un profil existant → champs pré-remplis → modifier → Enregistrer → changements visibles
   - Cliquer ← Retour depuis l'éditeur → retour liste, aucune modification
   - Naviguer vers un autre onglet depuis la liste → revenir → état de liste intact
