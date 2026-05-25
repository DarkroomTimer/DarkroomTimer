# Palette darkroom — conformité couleurs

## Contexte

L'application DarkroomTimer est utilisée en chambre noire, où toute lumière autre que rouge profond (≥ 680 nm) peut voiler le papier photosensible. L'interface contenait des couleurs interdites : gris (émettent du vert et du bleu), blanc, vert, bleu, violet. L'objectif est de restreindre l'interface à du noir pur et des nuances de rouge pur (composantes verte et bleue à zéro).

## Décisions de design

- **Approche palette** : fond très sombre teinté rouge (`#0D0000`), opacité pour les éléments inactifs, rouge vif pour l'état actif.
- **Burn vs Dodge** : même apparence visuelle, distinction par label uniquement.
- **États patches teststrip** : 3 niveaux de rouge (à venir = noir, en cours = rouge max, exposé = rouge moyen).

## Palette (`Color.kt`)

```kotlin
val DarkroomBlack           = Color(0xFF000000)  // fond principal
val DarkroomSurface         = Color(0xFF0D0000)  // cartes, panels
val DarkroomSurfaceElevated = Color(0xFF1A0000)  // surfaces élevées
val DarkroomRedBright       = Color(0xFFCC0000)  // actif, texte principal
val DarkroomRedMedium       = Color(0xFF880000)  // exposé / secondaire
val DarkroomRedDim          = Color(0xFF440000)  // inactif, tertiaire
val DarkroomRedFaint        = Color(0xFF1A0000)  // bordures, séparateurs
```

Supprimer : `Purple80`, `PurpleGrey80`, `Pink80`, `Purple40`, `PurpleGrey40`, `Pink40`.

## Thème (`Theme.kt`)

```kotlin
private val DarkroomColorScheme = darkColorScheme(
    primary        = DarkroomRedBright,
    onPrimary      = DarkroomBlack,
    background     = DarkroomBlack,
    onBackground   = DarkroomRedBright,
    surface        = DarkroomSurface,
    onSurface      = DarkroomRedBright,
    secondary      = DarkroomRedMedium,
    onSecondary    = DarkroomBlack,
    tertiary       = DarkroomRedDim,
    onTertiary     = DarkroomBlack,
    surfaceVariant = DarkroomSurfaceElevated,
    outline        = DarkroomRedFaint,
    error          = DarkroomRedBright,
    onError        = DarkroomBlack,
)
```

Supprimer le `LightColorScheme`. Forcer le thème sombre uniquement.

## Table de substitution (fichiers UI)

| Couleur actuelle | Remplacée par | Fichiers concernés |
|---|---|---|
| Gris `#0D0D0D`→`#666666` | `DarkroomBlack` pour fonds pleins, `DarkroomSurface` pour cards/panels | `MainActivity`, `CountdownScreen`, `BurnDodgeDialog`, `BurnDodgePanel`, `SettingsScreen` |
| Gris clair `#888888`→`#CCCCCC` | `DarkroomRedDim` ou `DarkroomRedMedium` | `BurnDodgeDialog`, `BurnDodgePanel` |
| `Color.White` (texte) | `DarkroomRedBright` | Partout |
| `Color.Gray` | `DarkroomRedDim` | `MainActivity` (nav items) |
| Vert `#44AA44` (patch exposé) | `DarkroomRedMedium` | `PatchItem`, `TeststripScreen`, `DevelopmentSessionScreen`, `DevelopmentLaunchScreen` |
| Vert `#224422` (bouton +) | `DarkroomSurface` | `DevelopmentProfileEditorScreen`, `DevelopmentProfileListScreen` |
| Bleu `#4488FF` (Dodge) | `DarkroomRedBright` | `BurnDodgeDialog`, `BurnDodgePanel` |
| `#884400`, `#AA4444` | `DarkroomRedDim` | `CountdownScreen` |
| `#FFFBFE` (background Theme) | `DarkroomBlack` | `Theme.kt` |
| `#CC2200` (G=34, non conforme) | `DarkroomRedBright` (#CC0000) | Partout (ex-couleur "principale") |
| `colors.xml` : purple/teal/white | Équivalents rouges ou supprimés | `colors.xml` |

## États patches teststrip

| État | Fond | Texte | Bordure |
|---|---|---|---|
| À venir | `DarkroomBlack` | `DarkroomRedFaint` | — |
| En cours | `DarkroomRedDim` | `DarkroomRedBright` | `DarkroomRedBright` |
| Exposé | `DarkroomSurface` | `DarkroomRedMedium` | — |

## Fichiers à modifier

1. `app/src/main/java/fr/mathgl/darkroomtimer/ui/theme/Color.kt`
2. `app/src/main/java/fr/mathgl/darkroomtimer/ui/theme/Theme.kt`
3. `app/src/main/res/values/colors.xml`
4. `app/src/main/java/fr/mathgl/darkroomtimer/MainActivity.kt`
5. `app/src/main/java/fr/mathgl/darkroomtimer/ui/CountdownScreen.kt`
6. `app/src/main/java/fr/mathgl/darkroomtimer/ui/BurnDodgeDialog.kt`
7. `app/src/main/java/fr/mathgl/darkroomtimer/ui/BurnDodgePanel.kt`
8. `app/src/main/java/fr/mathgl/darkroomtimer/ui/PatchItem.kt`
9. `app/src/main/java/fr/mathgl/darkroomtimer/ui/TeststripScreen.kt`
10. `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileEditorScreen.kt`
11. `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentProfileListScreen.kt`
12. `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentSessionScreen.kt`
13. `app/src/main/java/fr/mathgl/darkroomtimer/ui/DevelopmentLaunchScreen.kt`
14. `app/src/main/java/fr/mathgl/darkroomtimer/ui/SettingsScreen.kt`

## Vérification

1. `./gradlew assembleDebug` — build sans erreur de compilation
2. `./gradlew lint` — pas de warning couleur
3. Inspection visuelle sur émulateur/device : aucun pixel vert, bleu, blanc ou gris visible
