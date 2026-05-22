# Burn & Dodge — Ajustements Locaux d'Exposition

Source : `printalyzer/exposure_state.c`, `printalyzer/exposure_state.h`, `printalyzer/keypad_action.c`

---

## Description

Burn et Dodge sont des techniques de tirage permettant de modifier localement l'exposition d'une zone du tirage :

- **Burn (surexposer)** : exposer une zone plus longtemps que le reste → zone plus sombre
- **Dodge (sous-exposer)** : réduire l'exposition d'une zone → zone plus claire

Dans l'application, ces ajustements sont des **notes de session** : l'utilisateur crée une liste d'entrées décrivant les zones et les ajustements à appliquer, consultable pendant l'exposition pour guider son geste manuel.

---

## Structure d'une Entrée Burn/Dodge

Source : `printalyzer/exposure_state.h`

```kotlin
data class BurnDodgeEntry(
    val label: String,              // Description libre de la zone ("ciel", "visage")
    val contrastGrade: ContrastGrade, // Grade de contraste pour cette zone (voir 06-contrast-grades.md)
    val numerator: Int,             // Numérateur de la fraction de stop
    val denominator: Int,           // Dénominateur — l'une des 6 valeurs fixes : {12, 6, 4, 3, 2, 1}
    val isBurn: Boolean             // true = burn, false = dodge
)
```

### Fractions de Stop Disponibles

Source : `printalyzer/exposure_state.h`

| Fraction | Valeur | Label UI |
|---|---|---|
| 1/12 | 0.083 | `+1/12` |
| 1/6 | 0.167 | `+1/6` |
| 1/4 | 0.25 | `+1/4` |
| 1/3 | 0.333 | `+1/3` |
| 1/2 | 0.5 | `+1/2` |
| 1/1 | 1.0 | `+1 stop` |

Les mêmes fractions existent en négatif pour le dodge :

| Fraction | Label UI |
|---|---|
| −1/12 | `−1/12` |
| −1/6 | `−1/6` |
| ... | ... |
| −1/1 | `−1 stop` |

---

## Limites

| Contrainte | Valeur | Source |
|---|---|---|
| Nombre maximum d'entrées par session | 9 | `printalyzer/exposure_state.h` |
| Fractions valides | Flexible (voir `01-fstop-math.md`) | — |
| Grade de contraste | 0.0 à 5.0 (pas de 0.5) | voir `06-contrast-grades.md` |

---

## Calcul du Temps d'Ajustement

Pour chaque entrée burn/dodge, le temps d'ajustement par rapport au temps de base est :

```
adjustment_time = base_exposure_time × (2^(fraction × sign) − 1)

où sign = +1 pour burn, −1 pour dodge
```

Exemples avec `base = 16 s` :

| Type | Fraction | Calcul | Temps ajouté/soustrait |
|---|---|---|---|
| Burn +1/3 | +1/3 | 16 × (2^(1/3) − 1) | +4.2 s |
| Burn +1 stop | +1 | 16 × (2^1 − 1) | +16.0 s |
| Dodge −1/3 | −1/3 | 16 × (2^(−1/3) − 1) | −3.3 s |
| Dodge −1/2 | −1/2 | 16 × (2^(−1/2) − 1) | −4.7 s |

Ce calcul est affiché à titre informatif. L'utilisateur applique l'ajustement manuellement.

---

## Workflow Utilisateur

### Création d'une entrée

1. Pendant la configuration du countdown, taper "Ajouter ajustement"
2. Saisir un label optionnel pour la zone (ex : "ciel haut")
3. Sélectionner burn ou dodge
4. Choisir la fraction de stop
5. Optionnel : sélectionner le grade de contraste pour cette zone
6. Confirmer → entrée ajoutée à la liste

### Consultation pendant l'exposition

Pendant l'exposition principale (état RUNNING), un panneau latéral ou inférieur affiche la liste des entrées burn/dodge :
- Zone, type (B/D), fraction, temps calculé
- L'utilisateur peut les consulter pour planifier ses gestes

### Édition et suppression

- Tap sur une entrée → ouvrir l'éditeur
- Swipe gauche sur une entrée → supprimer
- L'édition est disponible avant et entre les expositions, pas pendant une exposition active

---

## Affichage

### Liste des entrées (panneau dédié)

```
Burn & Dodge  [Ajouter +]
─────────────────────────────
BURN  "ciel"      +1/3  ≈ +4.2 s  Grade 3
DODGE "visage"   −1/6   ≈ −2.1 s  Grade 2
BURN  "premiers" +1/2   ≈ +6.6 s  Grade 2.5
─────────────────────────────
3 ajustements / 9 maximum
```

### Éditeur d'une entrée

```
Type :      [BURN ●]    [DODGE ○]
Zone :      [ciel haut________]
Fraction :  [−]  +1/3  [+]
Grade :     [Grade 2 ▼]

Temps base : 16.0 s
Ajustement : +4.2 s (totale: 20.2 s)

[Annuler]              [Confirmer]
```

---

## Persistance

Les entrées burn/dodge sont attachées à une **session d'exposition** et non persistées globalement. Elles sont perdues à la fermeture de l'écran de countdown, sauf si l'utilisateur les sauvegarde explicitement dans un "profil de tirage" (fonctionnalité future non dans le scope initial).

---

## Interaction avec le Mode Countdown

La liste burn/dodge est visible comme un panneau escamotable dans l'écran du mode Countdown. Elle n'affecte pas le fonctionnement automatique du timer — c'est un outil d'aide à la mémoire pour le photographe qui effectue les gestes de masquage manuellement.

---

## Validation

- Si 9 entrées existent déjà, le bouton "Ajouter" est désactivé
- La fraction suit la logique flexible de `01-fstop-math.md`
- Le grade de contraste doit être dans la liste des grades valides (voir `06-contrast-grades.md`)
- Un label vide est accepté (l'entrée s'affiche alors sans label)
