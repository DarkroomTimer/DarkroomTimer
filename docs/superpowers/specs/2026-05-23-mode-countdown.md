# Mode Countdown — Compte à Rebours d'Exposition

Source : `fStop/src/countdown.cpp`, `fStop/src/counter.cpp`, `fStop/src/relay.cpp`, `fStop/include/fstop/counter.h`

---

## Description

Le mode Countdown est le mode principal de l'application. Il permet de programmer un temps d'exposition et de le décompter, en contrôlant simultanément le relais de l'agrandisseur et le safelight.

---

## Machine d'États du Compteur

```
           start()
[STOPPED] ──────────► [RUNNING]
    ▲                     │
    │      stop()         │ pause()
    │◄────────────────────┤
    │                     ▼
    │                 [PAUSED]
    │                     │
    └─────────────────────┘
                     stop() / resume()→[RUNNING]
```

Transition supplémentaire : `RUNNING ──(remaining ≤ 0)──► STOPPED` (fin automatique)

### Règles de transition
- `start()` : uniquement depuis STOPPED
- `pause()` : uniquement depuis RUNNING
- `resume()` : uniquement depuis PAUSED → RUNNING
- `stop()` : depuis RUNNING ou PAUSED → STOPPED
- fin automatique (`remaining ≤ 0`) : depuis RUNNING → STOPPED

### Propriétés d'état
| État | `isStarted` | `isPaused` |
|---|---|---|
| STOPPED | false | false |
| RUNNING | true | false |
| PAUSED | true | true |

---

## Calcul du Temps Restant

Source : `fStop/src/counter.cpp`, méthode `Counter::update()`

Le temps restant est recalculé à chaque frame d'affichage (toutes les ~50 ms).

```
Si état = PAUSED :
    remaining = configured_time - ((now - start_at) - (now - pause_at))
               = configured_time - (elapsed_before_pause)

Si état = RUNNING :
    remaining = configured_time - (now - start_at)
```

- `configured_time` : temps programmé en ms
- `start_at` : timestamp du dernier `start()` ou `resume()` corrigé
- `pause_at` : timestamp du `pause()`
- `remaining` peut devenir **négatif** (le compte est terminé)

**Détection de fin** : si `remaining <= 0` et état = RUNNING → déclencher la fin d'exposition.

---

## Limites du Temps

| Paramètre | Valeur | Source |
|---|---|---|
| Temps minimum | 100 ms | Résolution d'affichage |
| Temps maximum | 999 000 ms (999 s) | `fStop/config.h : COUNTER_MAX` |
| Temps par défaut | 8 000 ms (8 s) | `fStop/src/teststrip.cpp` |
| Résolution d'affichage | 100 ms (1 dixième) | `fStop/src/countdown.cpp` |

---

## Format d'Affichage du Temps

Format : `MM:SS.d`

| Valeur ms | Affichage |
|---|---|
| 8 000 | `00:08.0` |
| 65 400 | `01:05.4` |
| 999 000 | `16:39.0` |
| 100 | `00:00.1` |

La partie dixième est toujours affichée (1 chiffre après le point).
Si le temps est supérieur à 10 minutes, les minutes sont affichées avec 2 chiffres minimum.

---

## Ajustement du Temps

Le temps configuré est modifiable avant le start et pendant la pause.

### Positions d'édition (inspiré fStop)
Le temps est divisé en positions éditables indépendantes :

| Position | Incrément | Exemple (+1) |
|---|---|---|
| Centaines de secondes | ±100 000 ms | `100 s` |
| Dizaines de secondes | ±10 000 ms | `10 s` |
| Unités de secondes | ±1 000 ms | `1 s` |
| Dixièmes de secondes | ±100 ms | `0.1 s` |

Sur Android, ces ajustements sont accessibles par :
- **Boutons +/−** à l'écran
- **Swipe vertical** sur le chiffre concerné
- **Dialogue de saisie** directe (tap sur le temps)

---

## Comportement des Relais

### Mode Standalone
| Événement | Agrandisseur (visuel) | Safelight (visuel) |
|---|---|---|
| Au démarrage de l'app | OFF | ON |
| `start()` | **ON** | OFF |
| `pause()` | OFF | OFF |
| `resume()` | **ON** | OFF |
| Fin de compte (remaining ≤ 0) | OFF | OFF |
| `stop()` | OFF | OFF |

En mode standalone, les indicateurs visuels reflètent ces états sans envoyer aucune commande physique.

### Mode Bluetooth
Les mêmes événements envoient des commandes BLE à l'appareil périphérique (voir `09-bluetooth-relay.md`) :
- `start()` → `ENLARGER_ON` + `SAFELIGHT_OFF`
- `pause()` / `stop()` → `ENLARGER_OFF` + `SAFELIGHT_OFF`
- `resume()` → `ENLARGER_ON` + `SAFELIGHT_OFF`

### Contrôle Manuel et Priorités
L'utilisateur peut forcer l'état des relais indépendamment du timer :
- **Override agrandisseur** : bascule On/Off manuellement (utile pour la mise au point)
- **Override safelight** : bascule On/Off manuellement

**Matrice de Priorité :**
Le Timer est prioritaire sur tout contrôle manuel. 
- **Nettoyage à la transition** : Dès qu'une transition d'état du timer a lieu (ex: `start()` ou `stop()`), tout override manuel en cours est automatiquement annulé et écrasé par l'état requis par le timer.
- **Blocage en RUNNING** : Pendant que le timer est en état RUNNING, les contrôles d'override manuel sont désactivés pour éviter de fausser l'exposition.
- **Disponibilité** : Les overrides sont actifs uniquement en état STOPPED ou PAUSED.

---

## Comportement du Safelight

Le safelight s'allume automatiquement :
- Quand le timer est en état STOPPED (entre les expositions)
- Quand le timer est en état PAUSED

Le safelight s'éteint :
- Juste avant que l'agrandisseur s'allume (état RUNNING)

En mode standalone, ces changements sont purement visuels.

---

## Intégration avec Burn & Dodge

Si des entrées Burn & Dodge sont configurées pour la session en cours (voir `05-burn-dodge.md`), le timer n'est pas affecté dans ce mode. Les entrées burn/dodge sont des notes pour l'utilisateur, pas des temps automatiques dans le mode Countdown de base.

> Note : Un mode avancé pourrait séquencer automatiquement les zones burn/dodge, mais cela n'est pas dans le scope initial.

---

## Métronome

Le métronome audio (voir `08-audio-metronome.md`) est actif pendant l'état RUNNING si l'option est activée dans les paramètres. Il émet un bip périodique pour indiquer le défilement du temps.

---

## Foreground Service

Pendant qu'une exposition est en cours (état RUNNING), l'application doit maintenir un **Foreground Service Android** avec une notification persistante indiquant :
- Temps restant (mis à jour chaque seconde)
- Bouton "Pause" dans la notification
- Bouton "Stop" dans la notification

Cela garantit que le timer continue si l'écran s'éteint ou si l'utilisateur bascule vers une autre app.

---

## Résumé des Interactions UI

| Action utilisateur | Effet |
|---|---|
| Tap "Start" (état STOPPED) | → état RUNNING, relais ON |
| Tap "Pause" (état RUNNING) | → état PAUSED, relais OFF |
| Tap "Resume" (état PAUSED) | → état RUNNING, relais ON |
| Tap "Stop" (état PAUSED) | → état STOPPED, relais OFF |
| Tap temps (état STOPPED/PAUSED) | Ouvrir dialogue de saisie |
| Swipe +/− sur chiffre | Incrémenter/décrémenter la position |
| Tap "Agrandisseur" | Override relais agrandisseur |
| Tap "Safelight" | Override relais safelight |
| Fin automatique du compte | → état STOPPED, son de fin, relais OFF |
