# Profils Agrandisseur

Source : `printalyzer/enlarger_config.c`, `printalyzer/enlarger_config.h`, `printalyzer/settings.c`

---

## Description

Un profil agrandisseur capture les caractéristiques de montée et descente de la lampe d'un agrandisseur spécifique. La plupart des agrandisseurs ont un délai entre la commande d'allumage et le moment où la lumière est stabilisée à pleine intensité. Ignorer ces délais crée des expositions imprécises.

Ce module est **optionnel** : un profil "Idéal" (tous délais à 0) est disponible par défaut et convient aux agrandisseurs simples.

---

## Structure d'un Profil Agrandisseur

Source : `printalyzer/enlarger_config.h` (struct `enlarger_timing_t`)

```kotlin
data class EnlargerProfile(
    val id: Int,                   // 0-15
    val name: String,              // Nom libre, max 32 caractères
    val turnOnDelayMs: Int,        // Délai avant début de montée (ms)
    val riseTimeMs: Int,           // Durée de montée 0% → 100% (ms)
    val riseTimeEquivMs: Int,      // Exposition équivalente de la montée (ms)
    val turnOffDelayMs: Int,       // Délai avant début de descente (ms)
    val fallTimeMs: Int,           // Durée de descente 100% → 0% (ms)
    val fallTimeEquivMs: Int       // Exposition équivalente de la descente (ms)
)
```

---

## Définitions des Paramètres de Timing

```
Commande ON
     │
     │◄──────── turnOnDelayMs ──────────►│
     │                                   │
     │                                   │◄──── riseTimeMs ────►│
     │                                   │                       │
     │                                   0%                    100% (pleine puissance)
     │
     └─── Commande OFF
                │
                │◄──── turnOffDelayMs ───►│
                │                         │
                │                         │◄── fallTimeMs ──►│
                │                         │                   │
                │                       100%                  0%
```

- **turnOnDelayMs** : temps entre la commande CLOSE du relais et le début de la montée de lumière
- **riseTimeMs** : durée de la montée de 0% à 100%
- **riseTimeEquivMs** : quantité d'exposition (en ms à pleine puissance) équivalente à toute la phase de montée
- **turnOffDelayMs** : temps entre la commande OPEN du relais et le début de la descente
- **fallTimeMs** : durée de la descente de 100% à 0%
- **fallTimeEquivMs** : quantité d'exposition équivalente à la phase de descente

---

## Calcul du Temps Effectif de Relais

Source : `printalyzer/enlarger_control.c`

Pour atteindre un temps d'exposition cible `target_ms` :

```
relay_on_duration = target_ms - rise_time_equiv_ms - fall_time_equiv_ms

**Note d'intégration matérielle :** Dans le cas d'un driver où le timing est délégué au firmware (`canPause = false` dans `spec/09-relay-driver.md`), la valeur `relay_on_duration` doit être passée comme durée d'exposition au relais, car le firmware ne connaît pas le profil de l'agrandisseur.

Séquence complète :
  t=0               : commande ON
  t=turnOnDelay     : lampe commence à monter
  t=turnOnDelay+rise: lampe à 100%
  t=relay_on_duration: commande OFF
  t=relay_on_duration+turnOffDelay: lampe commence à descendre
  t=relay_on_duration+turnOffDelay+fall: lampe à 0%

Exposition totale reçue ≈ target_ms
```

### Contrainte de validité
```
rise_time_equiv_ms < rise_time_ms   (sinon erreur de profil)
fall_time_equiv_ms < fall_time_ms   (sinon erreur de profil)
```
L'exposition cible `target_ms` doit être strictement supérieure à la somme des équivalents de montée et descente (`target_ms > rise_time_equiv_ms + fall_time_equiv_ms`) pour être réalisable avec le profil actuel.

### Profil "Idéal" (tous délais à 0)
```
turnOnDelayMs = 0
riseTimeMs = 0
riseTimeEquivMs = 0
turnOffDelayMs = 0
fallTimeMs = 0
fallTimeEquivMs = 0
```
Avec ce profil, `relay_on_duration = target_ms`. C'est le comportement par défaut.

---

## Capacité de Stockage

| Paramètre | Valeur | Source |
|---|---|---|
| Nombre de profils | 16 (index 0-15) | `printalyzer/settings.h` |
| Profil actif | 1 à la fois | Sélectionné par l'utilisateur |
| Profil "Idéal" | Index 0 (créé automatiquement) | Valeurs à 0 |

---

## Gestion des Profils

### Créer un profil
1. Aller dans Réglages → Profils Agrandisseur → "Nouveau"
2. Saisir un nom
3. Saisir les valeurs de timing (en ms)
4. Valider

### Sélectionner le profil actif
- Dans Réglages → Profils Agrandisseur → Sélectionner dans la liste
- Le profil actif est appliqué à toutes les expositions suivantes

### Éditer / Supprimer
- Tap sur un profil existant → édition
- Swipe gauche → supprimer (sauf le profil Idéal qui ne peut être supprimé)

---

## Affichage dans l'Écran Principal

Le nom du profil agrandisseur actif est affiché discrètement dans l'écran de countdown (ex : sous le temps, en petit). Si le profil "Idéal" est sélectionné, l'information peut être omise.

---

## Validation des Données

| Champ | Validation |
|---|---|
| `name` | Non vide, max 32 caractères |
| `turnOnDelayMs` | ≥ 0, ≤ 10 000 ms |
| `riseTimeMs` | ≥ 0, ≤ 10 000 ms |
| `riseTimeEquivMs` | ≥ 0 et < `riseTimeMs` |
| `turnOffDelayMs` | ≥ 0, ≤ 10 000 ms |
| `fallTimeMs` | ≥ 0, ≤ 10 000 ms |
| `fallTimeEquivMs` | ≥ 0 et < `fallTimeMs` |

Si `riseTimeMs = 0` alors `riseTimeEquivMs` doit également être 0 (pas de rampe).

---

## Persistance

Les profils sont stockés dans la base de données Room (voir `10-data-storage.md`), table `enlarger_profiles`. Le profil actif est mémorisé dans SharedPreferences (index du profil sélectionné).
