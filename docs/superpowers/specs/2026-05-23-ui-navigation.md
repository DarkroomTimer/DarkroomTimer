# UI et Navigation

---

## Architecture de Navigation

```
MainActivity (setContent)
    └── NavHost (Compose Navigation)
            ├── CountdownScreen        (Timer principal)
            ├── TeststripScreen        (Bande de test)
            ├── DevelopmentScreen       (Assistant développement)
            └── SettingsScreen
                    ├── AudioSettingsScreen
                    ├── BluetoothSettingsScreen
                    └── EnlargerProfilesScreen
                            └── EnlargerProfileEditScreen
```

Navigation via **Navigation Compose** (`androidx.navigation:navigation-compose`) avec **`NavigationBar`** (3 onglets principaux). Pas de Fragment ni de XML layout — tout en Compose.

---

## Navigation Principale (Bottom Bar)

| Onglet | Icône | Screen |
|---|---|---|
| Exposition | chronomètre | `CountdownScreen` |
| Test | grille | `TeststripScreen` |
| Réglages | engrenage | `SettingsScreen` |

Le mode Développement est accessible depuis l'écran Exposition (bouton ou menu).

---

## Écran Exposition (CountdownScreen)

### Disposition

```
┌────────────────────────────────────┐
│ [BT●]              Grade: [2 ▼]   │  ← Barre de contrôle supérieure
│                                    │
│                                    │
│           00:08.0                  │  ← Temps (très grand, tap = édition)
│                                    │
│    [▲  Agrandisseur: OFF ●]        │  ← Indicateur relais (tap = override)
│    [▲  Safelight:    OFF ●]        │
│                                    │
│  ┌────────────────────────────┐   │
│  │ BURN & DODGE (2)    [+]   │   │  ← Panneau escamotable
│  │ BURN "ciel"    +1/3  4.2s │   │
│  │ DODGE "visage" -1/6  2.1s │   │
│  └────────────────────────────┘   │
│                                    │
│     [STOP]    [START]    [⟳]      │  ← Boutons de contrôle
└────────────────────────────────────┘
```

### États des boutons de contrôle

| État timer | Bouton gauche | Bouton centre | Bouton droit |
|---|---|---|---|
| STOPPED | — | **START** (vert) | — |
| RUNNING | **PAUSE** (orange) | Chrono actif | — |
| PAUSED | **STOP** (rouge) | **RESUME** (vert) | Reset |

### Édition du temps

Tap sur le temps → **Bottom Sheet Dialog** avec :
- Champ de saisie numérique (MM:SS.d)
- 4 boutons ± pour centaines/dizaines/unités/dixièmes
- Bouton "Valider" + "Annuler"

### Indicateurs de relais

Chaque indicateur affiche :
- Nom (`Agrandisseur` / `Safelight`)
- État (`ON` en vert / `OFF` en rouge)
- Long press → override manuel (toggle forcé, visible par badge "override")

En mode standalone, une icône "simulation" est affichée à côté.

### Sélecteur de grade

Tap sur le badge `Grade: [2 ▼]` → **Bottom Sheet** avec sélecteur :
```
◄  00  0  ½  1  1½  [2]  2½  3  3½  4  4½  5  ►
```
Sélecteur horizontal scrollable avec highlight du grade actuel.

### Indicateur Bluetooth

Icône BT en haut à gauche :
- Grise (pas d'anneau) = standalone
- Bleue (avec anneau) = connecté
- Orange (clignotant) = connexion en cours / reconnexion

Tap → ouvrir le fragment réglages Bluetooth.

---

## Écran Teststrip (TeststripScreen)

### Phase 1 : Configuration

```
┌────────────────────────────────────┐
│         BANDE DE TEST              │
│                                    │
│  Base :  [08.0 s]  Incr.: [1/3 ▼] │
│  Mode :  [INCRÉMENTAL ▼]           │
│  Patches: [◄  6  ►]               │
│                                    │
│  ┌──────────────────────────────┐  │
│  │ Patch 1 :   8.0 s            │  │
│  │ Patch 2 :  10.1 s            │  │
│  │ Patch 3 :  12.7 s            │  │
│  │ Patch 4 :  16.0 s            │  │
│  │ Patch 5 :  20.2 s  ← Total  │  │
│  │ Patch 6 :  25.4 s  ← Total  │  │
│  └──────────────────────────────┘  │
│                                    │
│           [DÉMARRER ►]             │
└────────────────────────────────────┘
```

### Phase 2 : Exposition en cours

```
┌────────────────────────────────────┐
│  Patch  3 / 6    ████████░░░░░░    │  ← Barre de progression
│                                    │
│           00:05.3                  │  ← Temps restant pour CE patch
│                                    │
│  Durée physique : 2.6 s            │  ← Mode incrémental
│  Exposition totale : 12.7 s        │
│                                    │
│     [PAUSE]       [ABANDONNER]     │
└────────────────────────────────────┘
```

### Phase 3 : Entre deux patches

```
┌────────────────────────────────────┐
│  Patch  3 / 6  ✓  TERMINÉ         │
│                                    │
│  Prochain : Patch 4  →  16.0 s    │
│  (exposition: 3.3 s supplémentaires│
│   en mode incrémental)             │
│                                    │
│  Couvrir le patch 3 avant de       │
│  continuer.                        │
│                                    │
│  [← Recommencer]  [Patch 4 →]     │
└────────────────────────────────────┘
```

---

## Écran Développement (DevelopmentScreen)

L'interface du mode développement est détaillée dans `spec/13-mode-development.md`. Elle comprend la gestion des profils, l'écran de lancement et l'écran de session.

---

## Écran Réglages (SettingsScreen)

Écran de réglages entièrement en Compose (pas de Jetpack Preference Library, qui est View-based). Les sections sont des `Column` avec des composants Compose standard (`Switch`, `Slider`, `DropdownMenu`, etc.).

```
GÉNÉRAL
  Volume audio           [Moyen ▼]
  Bip au démarrage       [ON/OFF]

MÉTRONOME
  Métronome              [ON/OFF]
  Cadence                [1 000 ms] (visible si ON)

EXPOSITION
  Temps par défaut       [8.0 s]
  Grade par défaut       [2]
  Incrément f-stop       [1/3 ▼]

TESTSTRIP
  Mode                   [Incrémental ▼]
  Nombre de patches      [6]

AGRANDISSEUR
  Profils agrandisseur   ►
  Profil actif           [Idéal ▼]

BLUETOOTH
  Mode compagnon         [ON/OFF]
  Périphérique           [Non connecté]   (visible si ON)
  Scanner                [SCANNER]        (bouton si ON)

DONNÉES
  Exporter les données   [EXPORTER]
  Importer les données   [IMPORTER]
```

---

## Écran Profils Agrandisseur (EnlargerProfilesScreen)

```
┌────────────────────────────────────┐
│  Profils Agrandisseur      [+ Nouveau] │
│                                    │
│  ● Idéal                           │  ← Sélectionné (point vert)
│    Leitz Focomat II                │
│    Durst M605                      │
│                                    │
│  (swipe gauche pour supprimer)     │
└────────────────────────────────────┘
```

### Éditeur de profil (EnlargerProfileEditScreen)

```
Nom :         [Leitz Focomat II    ]

Allumage
  Délai ON :      [150 ms]
  Temps montée :  [800 ms]
  Montée equiv. : [350 ms]

Extinction
  Délai OFF :     [ 50 ms]
  Temps descente: [500 ms]
  Descente equiv.:[200 ms]

[Annuler]                  [Enregistrer]
```

---

## Dialog Éditeur Burn & Dodge

Accessible via le bouton [+] dans le panneau Burn & Dodge de CountdownScreen.

```
┌──────────────────────────────┐
│  Ajouter un ajustement       │
│                              │
│  Type :  [BURN ●] [DODGE ○]  │
│                              │
│  Zone :  [ciel haut_______]  │
│                              │
│  Fraction :                  │
│  [1/12][1/6][1/4][1/3][1/2][1] │
│                   ^^^         │
│                (sélectionné) │
│                              │
│  Grade : [Grade 2 ▼]         │
│                              │
│  Effet : +4.2 s (base 16 s)  │
│                              │
│  [Annuler]      [Confirmer]  │
└──────────────────────────────┘
```

---

## Foreground Service — Notification

Pendant une exposition active, la notification Android affiche :

```
DarkroomTimer                          [icône]
Exposition en cours — 00:05.3 restant
[Pause]  [Stop]
```

La notification est mise à jour toutes les secondes. Les actions Pause/Stop sont fonctionnelles depuis la notification.

---

## Thème et Style

- **Thème** : sombre (DayNight avec mode NIGHT par défaut) — adapté à l'usage en chambre noire
- **Couleurs** :
  - Fond : noir (`#000000`) ou très sombre (`#0D0D0D`)
  - Texte principal : blanc (`#FFFFFF`)
  - Accent : rouge sombre (`#CC2200`) — inactinique, minimal impact sur la sensibilité du papier
  - Agrandisseur ON : rouge (`#FF4400`)
  - Safelight ON : orange (`#FF6600`)
  - État OK / connecté : vert (`#33AA44`)
- **Taille du temps** : police monospace, minimum 72sp (lisible à distance)
- **Contraste élevé** : l'écran doit être lisible avec très peu de lumière ambiante

---

## Accessibilité

- Toutes les zones interactives ont un `contentDescription` en français
- La taille de police respecte le scaling système (accessibilité visuelle)
- Les boutons de contrôle principaux (Start/Pause/Stop) ont une taille minimum de 56dp pour permettre la manipulation avec des gants de laboratoire

---

## Orientation

L'application est conçue principalement en **portrait**. Le mode paysage est supporté mais non optimisé dans le scope initial.
