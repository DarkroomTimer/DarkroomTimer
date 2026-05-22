# Grades de Contraste

Source : `printalyzer/contrast.c`, `printalyzer/enlarger_control.c`, `printalyzer/exposure_state.h`

---

## Description

Le grade de contraste détermine le rendu tonal du tirage sur papier variable-contraste (Multigrade, Ilford, etc.). Il correspond aux filtres de l'agrandisseur :
- Grades bas (0, 00) → faible contraste, tons doux
- Grade 2 → contraste standard (référence)
- Grades élevés (4, 5) → fort contraste, noir profond et blanc pur

L'application affiche et mémorise le grade de contraste sélectionné. En mode Bluetooth avec un agrandisseur DMX, le grade pourrait contrôler les canaux RGB (hors scope initial).

---

## Grades Supportés

Source : `printalyzer/contrast.h` (type `contrast_grade_t`)

| Index | Valeur float | Label UI | Description |
|---|---|---|---|
| 0 | 0.00 | `00` | Très faible contraste |
| 1 | 0.00 | `0` | Faible contraste |
| 2 | 0.50 | `½` | |
| 3 | 1.00 | `1` | |
| 4 | 1.50 | `1½` | |
| 5 | 2.00 | `2` | Contraste standard ← défaut |
| 6 | 2.50 | `2½` | |
| 7 | 3.00 | `3` | |
| 8 | 3.50 | `3½` | |
| 9 | 4.00 | `4` | Fort contraste |
| 10 | 4.50 | `4½` | |
| 11 | 5.00 | `5` | Contraste maximal |

**12 grades** au total (grades entiers de 0 à 5 plus les demi-grades).

Le grade `00` est un grade spécial (très basse densité) distinct du grade `0`.

---

## Grade par Défaut

**Grade 2** — contraste standard, adapté au tirage normal.

Source : `printalyzer/settings.c` `settings_get_default_contrast_grade()`

---

## Interpolation des Demi-Grades

Source : `printalyzer/contrast.c`

Les valeurs DMX des demi-grades (0.5, 1.5, 2.5, etc.) sont calculées par **interpolation linéaire** entre les grades entiers voisins :

```
grade_half_value = (grade_low_value + grade_high_value) / 2
```

Dans l'application Android (sans contrôle DMX dans le scope initial), l'interpolation n'est pas nécessaire : les demi-grades sont simplement sélectionnables comme les grades entiers.

---

## Affichage et Notation

### Notation recommandée
| Grade | Notation affichée |
|---|---|
| 00 | `00` |
| 0 | `0` |
| 0.5 | `½` ou `0.5` |
| 1 | `1` |
| 1.5 | `1½` ou `1.5` |
| 2 | `2` |
| ... | ... |
| 5 | `5` |

### Sélecteur UI
Un sélecteur discret (liste horizontale scrollable avec 12 positions) permet de choisir parmi les 12 grades. En Compose : `LazyRow` ou `Row` scrollable avec `SelectableItem` par grade.

Le grade sélectionné est affiché en grand dans l'écran de countdown.

---

## Utilisation dans l'Application

### Mode Countdown
Le grade est affiché à titre informatif et mémorisé. Il rappelle à l'utilisateur quel filtre est en place dans l'agrandisseur.

### Mode Teststrip
Le grade s'applique à l'ensemble de la bande de test (tous les patches utilisent le même grade).

### Burn & Dodge
Chaque entrée burn/dodge peut avoir **son propre grade de contraste**, différent du grade de base. Cela permet de changer de filtre pour certaines zones (technique avancée de tirage).

---

## Sélection du Grade

| Action | Effet |
|---|---|
| Glisser le sélecteur | Naviguer parmi les 12 grades |
| Tap sur un grade | Sélectionner directement |
| Boutons +/− | Grade +1 ou −1 dans la liste |

Le grade sélectionné est persisté dans les préférences de l'application (voir `10-data-storage.md`).

---

## Représentation Interne

Les grades **ne peuvent pas** être représentés par un `Float` : les grades `00` et `0` ont tous les deux la valeur `0.00f`, ce qui les rend indistinguables par valeur numérique.

La représentation interne obligatoire est un **enum `ContrastGrade`** avec 12 valeurs nommées :

```kotlin
enum class ContrastGrade(val floatValue: Float, val label: String) {
    GRADE_00  (0.00f, "00"),
    GRADE_0   (0.00f, "0"),
    GRADE_HALF(0.50f, "½"),
    GRADE_1   (1.00f, "1"),
    GRADE_1H  (1.50f, "1½"),
    GRADE_2   (2.00f, "2"),
    GRADE_2H  (2.50f, "2½"),
    GRADE_3   (3.00f, "3"),
    GRADE_3H  (3.50f, "3½"),
    GRADE_4   (4.00f, "4"),
    GRADE_4H  (4.50f, "4½"),
    GRADE_5   (5.00f, "5")
}
```

Le grade par défaut est `ContrastGrade.GRADE_2`.

---

## Contraintes

- Le grade ne peut pas dépasser `GRADE_5` ni être inférieur à `GRADE_00`
- La liste des grades est **fixe** (pas de grade personnalisé)
- Le grade `GRADE_00` est le premier de la liste (index 0), distinct de `GRADE_0`
