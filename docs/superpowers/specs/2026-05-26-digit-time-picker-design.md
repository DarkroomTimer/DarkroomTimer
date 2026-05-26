# DigitTimePicker — Design Spec

**Date:** 2026-05-26  
**Scope:** Remplacement du sélecteur de temps par un afficheur 7-segments interactif

---

## Contexte

L'écran principal (`CountdownScreen`) utilise actuellement un `Text` cliquable qui ouvre un `TimeEditorSheet` (ModalBottomSheet avec des `TimeSpinner`), complété par une `TimeAdjustRow` de boutons ±1m/10s/1s/0.1s. Ce flux est lent et peu adapté à une utilisation en chambre noire avec les mains occupées.

L'objectif est de rendre le compteur lui-même directement éditable par tap (moitié haute/basse) et swipe, avec un rendu authentique type afficheur 7-segments. Le composant doit être réutilisable sur trois écrans : compte à rebours, teststrip, et développement.

---

## Composants

### `SegmentDisplay` (nouveau fichier : `ui/SegmentDisplay.kt`)

Composable Canvas qui dessine un seul chiffre (0–9) via 7 segments rectangulaires (`drawRoundRect`). Les segments éteints sont visibles en couleur sombre, comme un vrai afficheur.

**Paramètres :**
```kotlin
@Composable
fun SegmentDisplay(
    digit: Int,                            // 0–9
    segOnColor: Color = DarkroomRedBright,
    segOffColor: Color = DarkroomRedFaint,
    modifier: Modifier = Modifier
)
```

**Table des segments** (a=haut, b=haut-droit, c=bas-droit, d=bas, e=bas-gauche, f=haut-gauche, g=milieu) :

| Digit | a | b | c | d | e | f | g |
|-------|---|---|---|---|---|---|---|
| 0     | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |   |
| 1     |   | ✓ | ✓ |   |   |   |   |
| 2     | ✓ | ✓ |   | ✓ | ✓ |   | ✓ |
| 3     | ✓ | ✓ | ✓ | ✓ |   |   | ✓ |
| 4     |   | ✓ | ✓ |   |   | ✓ | ✓ |
| 5     | ✓ |   | ✓ | ✓ |   | ✓ | ✓ |
| 6     | ✓ |   | ✓ | ✓ | ✓ | ✓ | ✓ |
| 7     | ✓ | ✓ | ✓ |   |   |   |   |
| 8     | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| 9     | ✓ | ✓ | ✓ | ✓ |   | ✓ | ✓ |

---

### `DigitTimePicker` (nouveau fichier : `ui/DigitTimePicker.kt`)

Composable interactif qui assemble N `SegmentDisplay` + séparateurs et gère les gestes.

**Format :**
```kotlin
enum class DigitTimeFormat {
    MINUTES_SECONDS_TENTHS,  // MM:SS.T  — countdown, teststrip
    HOURS_MINUTES_SECONDS    // HH:MM:SS — développement
}
```

**Paramètres :**
```kotlin
@Composable
fun DigitTimePicker(
    valueMs: Long,
    onValueChange: (Long) -> Unit,
    enabled: Boolean = true,
    format: DigitTimeFormat = DigitTimeFormat.MINUTES_SECONDS_TENTHS
)
```

**Poids par position :**

| Format | Position | Poids (ms) |
|--------|----------|------------|
| MM:SS.T | T  | 100 |
| MM:SS.T | S2 | 1 000 |
| MM:SS.T | S1 | 10 000 |
| MM:SS.T | M2 | 60 000 |
| MM:SS.T | M1 | 600 000 |
| HH:MM:SS | S2 | 1 000 |
| HH:MM:SS | S1 | 10 000 |
| HH:MM:SS | M2 | 60 000 |
| HH:MM:SS | M1 | 600 000 |
| HH:MM:SS | H2 | 3 600 000 |
| HH:MM:SS | H1 | 36 000 000 |

**Plages :**
- `MINUTES_SECONDS_TENTHS` : 100ms – 999 000ms
- `HOURS_MINUTES_SECONDS` : 1 000ms – 359 999 000ms (99:59:59)

---

## Interaction

Chaque digit est entouré d'une zone ▲ (au-dessus) et ▼ (en-dessous), invisibles au repos. La zone interactive totale est divisée en deux moitiés par la position Y du tap :

- **Y < 50 %** (zone ▲ + moitié haute du digit) → `valueMs += poids`
- **Y ≥ 50 %** (moitié basse du digit + zone ▼) → `valueMs -= poids`

Les flèches ▲/▼ apparaissent sur la zone correspondante uniquement au contact (`pointerInput` avec `awaitPointerEvent`).

**Swipe vertical :** chaque 40dp de déplacement = 1 incrément/décrément. La retenue est automatique (l'opération porte sur `valueMs` directement, pas sur les digits individuels) — débordement d'un digit = carry/borrow sur le suivant.

**Implémentation :** un seul `pointerInput` sur la bounding box totale du widget (flèche + digit + flèche). Position Y relative détermine la zone.

Résultat clampé à la plage du format avant d'appeler `onValueChange`.

---

## Intégration par écran

### `CountdownScreen` + `CountdownViewModel`

- Remplace : `Text` cliquable + `TimeEditorSheet` + `TimeAdjustRow` + `TimeSpinner`
- Ajoute : `DigitTimePicker(valueMs = state.displayTimeMs, onValueChange, enabled = state.timerState != TimerState.RUNNING, format = MINUTES_SECONDS_TENTHS)`
- `onValueChange` branch sur l'état dans `CountdownScreen` :
  - STOPPED → `viewModel.setBaseTime(ms)`
  - PAUSED  → `viewModel.setRemainingTime(ms)`
- En état **STOPPED** : `setBaseTime(ms)` met à jour `baseTimeMs`, réinitialise la correction f-stop si active (comportement identique à l'actuel `setTimeFromInput`)
- En état **PAUSED** : `setRemainingTime(ms)` ajuste `timer.configuredTimeMs` pour que `remainingMs()` vaille `ms`, sans toucher à `baseTimeMs`
- En état **RUNNING** : `enabled = false`
- Ajouté à `CountdownUiState` : `displayTimeMs: Long` (ms affiché — base time en STOPPED, temps restant en PAUSED/RUNNING)
- Supprimé de `CountdownUiState` : `showTimeEditor`
- Supprimé du ViewModel : `openTimeEditor()`, `closeTimeEditor()`, `setTimeFromInput(m, s, t)`
- Ajouté au ViewModel : `fun setBaseTime(ms: Long)`, `fun setRemainingTime(ms: Long)`

### `TeststripScreen`

- Dans `ConfigurationSection` : remplace l'affichage `${baseTimeMs/1000.0}s` + boutons ±1s par `DigitTimePicker(valueMs = baseTimeMs, onValueChange = { viewModel.updateBaseTime(it) }, enabled = sessionState != EXPOSING, format = MINUTES_SECONDS_TENTHS)`

### `DevelopmentProfileEditorScreen`

- Dans `StepEditorDialog` : remplace l'`OutlinedTextField` de durée par `DigitTimePicker(valueMs = durationSeconds * 1000L, onValueChange = { durationSeconds = (it / 1000).toInt() }, format = HOURS_MINUTES_SECONDS)`

---

## Fichiers modifiés

| Fichier | Statut |
|---------|--------|
| `ui/SegmentDisplay.kt` | Nouveau |
| `ui/DigitTimePicker.kt` | Nouveau (inclut `DigitTimeFormat`) |
| `ui/CountdownScreen.kt` | Modifié |
| `ui/CountdownViewModel.kt` | Modifié |
| `ui/TeststripScreen.kt` | Modifié |
| `ui/DevelopmentProfileEditorScreen.kt` | Modifié |

---

## Tests

**`DigitTimePickerLogicTest`** (nouveau) :
- Conversion ms → digits aller-retour pour les deux formats
- Retenue `MM:SS.T` : 00:09.9 + T → 00:10.0
- Retenue `HH:MM:SS` : 00:59:59 + S2 → 01:00:00
- Clamp min/max pour chaque format

**`CountdownViewModelTest`** (existant, étendu) :
- `setBaseTime(ms)` en état STOPPED : met à jour `baseTimeMs`, reset f-stop si actif
- `setBaseTime(ms)` en état PAUSED → guard (no-op)
- `setBaseTime(ms)` en état RUNNING → guard (no-op)
- `setRemainingTime(ms)` en état PAUSED : ajuste `timer.configuredTimeMs` correctement
- `setRemainingTime(ms)` en état STOPPED/RUNNING → guard (no-op)
- `displayTimeMs` dans l'UiState = base time en STOPPED, temps restant en PAUSED/RUNNING
- Suppression des tests `setTimeFromInput`
