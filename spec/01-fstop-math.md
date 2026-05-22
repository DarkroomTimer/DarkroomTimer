# F-Stop Math — Calculs d'Exposition Photographique

Source : `fStop/include/fstop/stop.h`, `fStop/src/stop.cpp`, `fStop/test/stop_test.cpp`

---

## Principe

En photographie argentique, l'exposition varie de façon **exponentielle** base 2 :
- +1 stop = doubler le temps d'exposition (facteur ×2)
- −1 stop = halver le temps d'exposition (facteur ×0.5)
- Chaque stop = un facteur 2

Les fractions de stop permettent des ajustements fins :
- 1/3 stop = facteur ×2^(1/3) ≈ 1.2599
- 1/2 stop = facteur ×2^(1/2) ≈ 1.4142
- 1/4 stop = facteur ×2^(1/4) ≈ 1.1892

---

## Formule Centrale

```
adjusted_time = round(base_time × 2^(numerator/denominator × step))
```

**Paramètres :**
- `base_time` : temps de base en millisecondes
- `numerator` / `denominator` : fraction de stop (ex : 1/3)
- `step` : nombre de pas appliqués (entier, peut être 0, positif ou négatif)

**Implémentation de référence** (`fStop/src/stop.cpp`) :
```cpp
int32_t Stop::adjust(int32_t base, uint16_t step) {
    return round(base * pow(2.0, ((double)numerator / (double)denominator) * step));
}
```

---

## Représentation des Fractions de Stop

Une fraction de stop est représentée par deux entiers : `numerator` et `denominator`.

### Contraintes
| Contrainte | Valeur | Source |
|---|---|---|
| Dénominateur minimum | 1 | Interdit le dénominateur 0 |
| Dénominateur maximum | 33 | `fStop/src/teststrip.cpp` |
| Numérateur | Tout entier signé | Fractions négatives possibles |

### Protections obligatoires
- Si `denominator == 0` → forcer `numerator = 0, denominator = 1` (représente 0 stop)
- Résultat `adjusted_time` doit être ≥ 0

### Simplification (réduction par PGCD)
Après toute opération arithmétique, réduire la fraction par le PGCD :
```
gcd = pgcd(|numerator|, denominator)
numerator /= gcd
denominator /= gcd
```

---

## Incréments Prédéfinis

Source : `printalyzer/exposure_state.h` (type `exposure_adjustment_increment_t`)

| Label UI | Fraction | Valeur décimale | Facteur ×2^x |
|---|---|---|---|
| `/12` | 1/12 | 0.0833 | ≈ 1.0595 |
| `/6` | 1/6 | 0.1667 | ≈ 1.1225 |
| `/4` | 1/4 | 0.25 | ≈ 1.1892 |
| **`/3`** | **1/3** | **0.3333** | **≈ 1.2599** ← défaut |
| `/2` | 1/2 | 0.5 | ≈ 1.4142 |
| `×1` | 1/1 | 1.0 | = 2.0000 |

L'incrément par défaut est **1/3 stop** (valeur la plus utilisée en chambre noire).

---

## Format d'Affichage

Source : `fStop/src/stop.cpp` méthode `Stop::str()`

La fraction est affichée en notation mixte :

| Valeur interne | Affichage |
|---|---|
| 0/1 | `0` |
| 1/1 | `1` |
| 1/3 | `1/3` |
| 4/3 | `1 1/3` |
| −2/3 | `−2/3` |
| −7/3 | `−2 1/3` |
| 6/2 (= 3/1 simplifié) | `3` |

Règle :
- Si `|numerator| >= denominator` : afficher `partie_entière + reste/dénominateur`
- Si `remainder == 0` : afficher uniquement la partie entière
- Si `partie_entière == 0` : afficher uniquement `num/denom`

---

## Table de Validation

Référence extraite de `fStop/test/stop_test.cpp`, `test_stop_adjust()` :

**Paramètres :** `base = 8000 ms`, `stop = 1/3`

| Step | Calcul | Résultat (ms) |
|---|---|---|
| 0 | 8000 × 2^(1/3 × 0) = 8000 × 1.000 | **8000** |
| 1 | 8000 × 2^(1/3 × 1) = 8000 × 1.2599 | **10079** |
| 2 | 8000 × 2^(1/3 × 2) = 8000 × 1.5874 | **12699** |
| 3 | 8000 × 2^(1/3 × 3) = 8000 × 2.000 | **16000** |
| 4 | 8000 × 2^(1/3 × 4) = 8000 × 2.5198 | **20159** |
| 5 | 8000 × 2^(1/3 × 5) = 8000 × 3.1748 | **25398** |
| 6 | 8000 × 2^(1/3 × 6) = 8000 × 4.000 | **32000** |

**Stops négatifs** (réduction d'exposition) :

| Step | Calcul | Résultat (ms) |
|---|---|---|
| −1 | 8000 × 2^(1/3 × −1) = 8000 × 0.7937 | **6349** |
| −3 | 8000 × 2^(1/3 × −3) = 8000 × 0.5 | **4000** |

---

## Opérations Arithmétiques sur les Fractions

### Addition de deux fractions de stop
```
(a/b) + (c/d) = (a×d + c×b) / (b×d)
puis simplifier par PGCD
```

### Soustraction
```
(a/b) − (c/d) = (a×d − c×b) / (b×d)
puis simplifier par PGCD
```

Ces opérations sont utilisées dans le mode Burn & Dodge pour accumuler des ajustements (voir `05-burn-dodge.md`).

---

## Conversion Temps ↔ Stops

Pour calculer le **nombre de stops** entre deux temps :
```
stops = log2(time_b / time_a)
      = ln(time_b / time_a) / ln(2)
```

Pour convertir un **PEV** (Printalyzer) en facteur de temps :
```
ΔEV = delta_PEV / 100 / log10(2)     (≈ delta_PEV × 0.03322)
time_factor = 2^ΔEV
```

Cette conversion n'est utilisée qu'en interne si un futur module profil papier est ajouté.

---

## Implémentation Android Recommandée

```kotlin
object FStopMath {

    fun adjustTime(baseMs: Long, numerator: Int, denominator: Int, step: Int): Long {
        require(denominator != 0) { "Denominator cannot be zero" }
        val exponent = (numerator.toDouble() / denominator.toDouble()) * step
        return Math.round(baseMs * Math.pow(2.0, exponent))
    }

    fun simplify(numerator: Int, denominator: Int): Pair<Int, Int> {
        val g = gcd(Math.abs(numerator), denominator)
        return Pair(numerator / g, denominator / g)
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    fun formatStop(numerator: Int, denominator: Int): String {
        if (numerator == 0) return "0"
        val wholeAbs = Math.abs(numerator) / denominator
        val remainder = Math.abs(numerator) % denominator
        val sign = if (numerator < 0) "-" else ""
        return when {
            remainder == 0 -> "$sign$wholeAbs"
            wholeAbs == 0 -> "$sign$remainder/$denominator"
            else -> "$sign$wholeAbs $remainder/$denominator"
        }
    }
}
```
