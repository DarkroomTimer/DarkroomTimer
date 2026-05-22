# Stockage des Données

Source : `fStop/src/params.cpp`, `fStop/include/fstop/params.h`, `printalyzer/settings.c`

---

## Vue d'Ensemble

L'application utilise deux mécanismes de persistance :
- **SharedPreferences** : paramètres simples (scalaires, booléens, énumérations)
- **Room Database** : données structurées (profils agrandisseur)

Aucun réseau ou synchronisation cloud n'est prévu dans le scope initial.

---

## SharedPreferences

Fichier : `darkroom_timer_prefs`

### Paramètres Généraux

| Clé | Type | Défaut | Description | Source |
|---|---|---|---|---|
| `pref_metronome_enabled` | Boolean | false | Métronome actif | `fStop/params.cpp` |
| `pref_metronome_cadence_ms` | Int | 1 000 | Cadence métronome (ms) | `fStop/config.h` |
| `pref_buzzer_volume` | String (enum) | "MEDIUM" | Volume audio | Printalyzer |
| `pref_start_beep_enabled` | Boolean | true | Bip au démarrage d'expo | — |

### Paramètres d'Exposition

| Clé | Type | Défaut | Description | Source |
|---|---|---|---|---|
| `pref_default_exposure_ms` | Long | 8 000 | Temps d'exposition par défaut | `fStop/teststrip.cpp` |
| `pref_default_contrast_grade_index` | Int | 5 (= grade 2) | Grade de contraste par défaut | Printalyzer |
| `pref_default_stop_numerator` | Int | 1 | Numérateur incrément f-stop | — |
| `pref_default_stop_denominator` | Int | 3 | Dénominateur incrément f-stop | fStop |
| `pref_default_timer_ms` | Long | 120 000 | Temps minuterie par défaut | `fStop/timer.cpp` |

### Paramètres Teststrip

| Clé | Type | Défaut | Description | Source |
|---|---|---|---|---|
| `pref_teststrip_mode` | String (enum) | "INCREMENTAL" | Mode teststrip | Printalyzer |
| `pref_teststrip_patch_count` | Int | 6 | Nombre de patches | `fStop/config.h` |
| `pref_teststrip_base_ms` | Long | 8 000 | Temps de base teststrip | fStop |
| `pref_teststrip_stop_numerator` | Int | 1 | Incrément f-stop teststrip | — |
| `pref_teststrip_stop_denominator` | Int | 3 | — | fStop |

### Paramètres Bluetooth

| Clé | Type | Défaut | Description |
|---|---|---|---|
| `pref_bluetooth_enabled` | Boolean | false | Mode compagnon BT activé |
| `pref_bluetooth_device_address` | String | "" | Adresse MAC du dernier périphérique |
| `pref_bluetooth_device_name` | String | "" | Nom du dernier périphérique |

### Profil Agrandisseur Actif

| Clé | Type | Défaut | Description | Source |
|---|---|---|---|---|
| `pref_enlarger_profile_id` | Int | 0 | ID du profil actif (0 = Idéal) | Printalyzer |

---

## Room Database

### Entité : `EnlargerProfileEntity`

Table : `enlarger_profiles`

```kotlin
@Entity(tableName = "enlarger_profiles")
data class EnlargerProfileEntity(
    @PrimaryKey val id: Int,               // 0-15
    val name: String,                      // Max 32 chars
    val turnOnDelayMs: Int,                // ≥ 0
    val riseTimeMs: Int,                   // ≥ 0
    val riseTimeEquivMs: Int,              // ≥ 0, < riseTimeMs
    val turnOffDelayMs: Int,               // ≥ 0
    val fallTimeMs: Int,                   // ≥ 0
    val fallTimeEquivMs: Int               // ≥ 0, < fallTimeMs
)
```

**Contraintes** : max 16 enregistrements (id 0 à 15), id=0 réservé au profil "Idéal".

### Seed initial
À la création de la base (première installation), le profil Idéal est inséré automatiquement :
```
id=0, name="Idéal", tous les délais à 0
```

---

## Migration de Schema

La base de données est versionnée. La version initiale est **1**.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Future migrations ici
    }
}
```

Les SharedPreferences sont migrées manuellement si nécessaire lors des mises à jour.

---

## Export / Import (JSON)

L'utilisateur peut exporter et importer ses données via le partage Android standard (Intent.ACTION_SEND / Intent.ACTION_GET_CONTENT).

### Format JSON d'export

```json
{
  "version": 1,
  "exported_at": "2024-01-15T10:30:00Z",
  "settings": {
    "default_exposure_ms": 8000,
    "default_contrast_grade_index": 5,
    "default_stop_numerator": 1,
    "default_stop_denominator": 3,
    "metronome_enabled": false,
    "metronome_cadence_ms": 1000,
    "buzzer_volume": "MEDIUM",
    "teststrip_mode": "INCREMENTAL",
    "teststrip_patch_count": 6
  },
  "enlarger_profiles": [
    {
      "id": 0,
      "name": "Idéal",
      "turn_on_delay_ms": 0,
      "rise_time_ms": 0,
      "rise_time_equiv_ms": 0,
      "turn_off_delay_ms": 0,
      "fall_time_ms": 0,
      "fall_time_equiv_ms": 0
    },
    {
      "id": 1,
      "name": "Leitz Focomat",
      "turn_on_delay_ms": 150,
      "rise_time_ms": 800,
      "rise_time_equiv_ms": 350,
      "turn_off_delay_ms": 50,
      "fall_time_ms": 500,
      "fall_time_equiv_ms": 200
    }
  ]
}
```

### Import
- Vérifier le champ `version` pour la compatibilité
- Afficher un **dialogue de confirmation** avant tout import : "Cette opération va remplacer vos réglages actuels. Continuer ?" avec boutons Annuler / Importer
- Écraser les préférences existantes avec les valeurs importées (uniquement après confirmation)
- Merger les profils agrandisseur : si l'id existe déjà, demander confirmation (remplacer / garder / annuler)

---

## Données Non Persistées

Les éléments suivants sont **éphémères** (perdus à la fermeture de l'écran) :

- **Liste Burn & Dodge** d'une session : attachée à l'exposition en cours, pas sauvegardée automatiquement
- **État du timer** (running/paused) : l'exposition est arrêtée si l'app est tuée par le système (le Foreground Service prévient de cette situation)
- **Temps restant** d'une exposition en cours

---

## Validation à l'Import

| Champ | Règle |
|---|---|
| `version` | Doit être ≤ version actuelle |
| Profils agrandisseur | `id` entre 0 et 15, `name` non vide |
| Délais timing | Valeurs ≥ 0 |
| `rise_time_equiv_ms` | < `rise_time_ms` |
| `fall_time_equiv_ms` | < `fall_time_ms` |

En cas d'erreur de validation, l'import est refusé avec un message descriptif.
