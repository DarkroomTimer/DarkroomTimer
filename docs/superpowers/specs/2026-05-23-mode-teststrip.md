# Mode Teststrip — Bande de Test Photographique

Source : `fStop/src/teststrip.cpp`, `fStop/include/fstop/teststrip.h`, `printalyzer/exposure_state.c`

---

## Description

Le mode Teststrip permet de réaliser une bande de test : une série d'expositions progressives sur des sections d'un même papier photographique. Cela permet de déterminer visuellement le temps d'exposition optimal avant le tirage final.

---

## Modes de Teststrip

### Mode INCREMENTAL (Printalyzer)
Chaque patch reçoit une exposition **cumulée** : le patch 1 reçoit le temps de base, le patch 2 reçoit 2× le temps de base, etc.

En pratique : le photographe couvre les patches déjà exposés, puis expose le suivant.

```
Patch 1 : exposé pour t₁
Patch 2 : exposé pour t₂ − t₁ (diff avec le précédent)
Patch n : exposé pour tₙ − tₙ₋₁
```

Chaque patch vu individuellement a reçu une exposition totale de `tₙ`.

### Mode SEPARATE (fStop)
Chaque patch est exposé indépendamment pour son propre temps calculé.

```
Patch 1 : exposé pour t₁
Patch 2 : exposé pour t₂ (indépendant)
Patch n : exposé pour tₙ (indépendant)
```

Mode plus simple à réaliser, mais moins courant.

---

## Paramètres de Configuration

| Paramètre | Défaut | Min | Max | Source |
|---|---|---|---|---|
| Temps de base (`base_time`) | 8 000 ms | 100 ms | 999 000 ms | `fStop/teststrip.cpp` |
| Incrément f-stop (`stop_fraction`) | 1/3 | 1/33 | 33/1 | `fStop/teststrip.cpp` |
| Nombre de patches (`patch_count`) | 6 | 3 | 7 | fStop=6, Printalyzer=5 ou 7 |
| Mode (`teststrip_mode`) | INCREMENTAL | — | — | Printalyzer |

---

## Calcul des Temps par Patch

### Formule (identique pour les deux modes)
```
time[n] = round(base_time × 2^(stop_numerator/stop_denominator × n))
```

Avec `n` = index du patch (commence à 0).

Source : `fStop/src/stop.cpp` → `Stop::adjust(base, step)`

### Exemple : base=8000 ms, stop=1/3, 6 patches

| Patch | n | Calcul | Temps (ms) | Affichage |
|---|---|---|---|---|
| 1 | 0 | 8000 × 2^0 | 8 000 | 8.0 s |
| 2 | 1 | 8000 × 2^(1/3) | 10 079 | 10.1 s |
| 3 | 2 | 8000 × 2^(2/3) | 12 699 | 12.7 s |
| 4 | 3 | 8000 × 2^1 | 16 000 | 16.0 s |
| 5 | 4 | 8000 × 2^(4/3) | 20 159 | 20.2 s |
| 6 | 5 | 8000 × 2^(5/3) | 25 398 | 25.4 s |

### Mode INCREMENTAL — Temps de chaque exposition physique

| Patch | Temps total (ms) | Durée de l'exposition physique (ms) |
|---|---|---|
| 1 | 8 000 | 8 000 |
| 2 | 10 079 | 2 079 (= 10 079 − 8 000) |
| 3 | 12 699 | 2 620 |
| 4 | 16 000 | 3 301 |
| 5 | 20 159 | 4 159 |
| 6 | 25 398 | 5 239 |

---

## Déroulement d'une Session Teststrip

### Séquence utilisateur (Mode INCREMENTAL)

1. L'utilisateur configure le temps de base et l'incrément f-stop
2. L'app affiche la liste des patches avec leurs temps calculés
3. L'utilisateur tape "Démarrer"
4. **Patch 1** : exposition pour la durée calculée (physique)
   - Agrandisseur ON, safelight OFF
   - Compte à rebours affiché
   - À la fin : agrandisseur OFF, safelight OFF, son de fin
5. L'utilisateur couvre le patch 1, tape "Patch suivant"
6. **Patch 2** : exposition pour la durée différentielle
   - Répétition jusqu'au dernier patch
7. Fin : tous les patches exposés, session terminée

### Contrôles UI pendant le teststrip

| Action | Effet |
|---|---|
| Tap "Démarrer" | Lance le patch courant (compte à rebours) |
| Tap "Pause" | Pause le compte (si besoin) |
| Tap "Patch suivant" (état STOPPED entre patches) | Passe au patch suivant |
| Tap "Recommencer" | Remet le patch courant à 0 (sans avancer) |
| Tap "Abandonner" | Quitte le mode teststrip, retour à l'écran principal |
| Modifier stop pendant session | Recalcule les patches restants (non encore exposés) |

---

## Wraparound des Patches

Source : `fStop/src/teststrip.cpp`

Quand le dernier patch est atteint, l'index revient à 0 (mode boucle).

```
Si step < patch_count - 1 → step++
Sinon → step = 0
```

Le retour à l'index 0 représente le **début d'une nouvelle session** avec les mêmes paramètres. L'état cumulatif est réinitialisé — l'utilisateur est censé utiliser un nouveau papier. L'application ne conserve aucune mémoire des expositions précédentes. Cela permet d'enchaîner plusieurs bandes de test consécutives sans reconfigurer les paramètres.

---

## Comportement des Relais

Identique au mode Countdown (voir `02-mode-countdown.md`) :
- **Début patch** : agrandisseur CLOSED (ON), safelight OPEN (OFF)
- **Pause / Entre patches** : agrandisseur OPEN (OFF), safelight CLOSED (ON) — aligné avec 02-mode-countdown.md
- **Fin session** : agrandisseur OPEN (OFF), safelight CLOSED (ON)

---

## Affichage

### Écran de configuration (avant démarrage)
```
Base: [08.0 s]        Incrément: [1/3]
Mode: [INCREMENTAL]   Patches: [6]

Patch 1:  8.0 s  ──────────────────────
Patch 2: 10.1 s  ──────────────────────
Patch 3: 12.7 s  ──────────────────────
Patch 4: 16.0 s  ──────────────────────
Patch 5: 20.2 s  ──────────────────────
Patch 6: 25.4 s  ──────────────────────
```

### Écran pendant une exposition
```
Patch 3 / 6          RUNNING
       12.7 s

      [00:07.3]    ◄── temps restant

[Pause]                    [Patch suivant →]
```

### Écran entre patches (après fin d'un patch)
```
Patch 3 / 6          DONE ✓

Prochaine exposition: 12.7 s (diff: 3.3 s)

[Recommencer patch]    [Patch suivant →]
```

---

## Modification des Paramètres en Cours de Session

L'utilisateur peut modifier le temps de base et l'incrément entre les patches (pas pendant une exposition). Les patches déjà réalisés ne peuvent pas être refaits automatiquement. L'application recalcule les temps des patches restants.

Un avertissement est affiché si la modification affecte la cohérence de la bande de test.
