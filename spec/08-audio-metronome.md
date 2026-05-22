# Audio et Métronome

Source : `fStop/src/metronome.cpp`, `fStop/include/fstop/metronome.h`, `printalyzer/buzzer.c`

---

## Description

L'audio sert deux fonctions :
1. **Métronome** : bip périodique pendant l'exposition pour informer le photographe du défilement du temps sans regarder l'écran
2. **Sons d'événements** : signaux sonores à la fin d'une exposition, au démarrage, en cas d'alerte

---

## Métronome

Source : `fStop/src/metronome.cpp`

### Comportement
- Émet un bip à intervalle régulier pendant qu'une exposition est en cours (état RUNNING)
- S'arrête automatiquement quand l'exposition est en pause ou terminée
- Configurable : activé ou désactivé globalement

### Paramètres

| Paramètre | Défaut | Min | Max | Source |
|---|---|---|---|---|
| Activé | false (désactivé) | — | — | `fStop/src/params.cpp` |
| Cadence | 1 000 ms | 500 ms | 5 000 ms | `fStop/include/config.h : METRONOME_CADENCE_MS` |
| Fréquence du bip | 250 Hz | — | — | `fStop/include/config.h : METRONOME_TONE` |
| Durée du bip | 25 ms | — | — | `fStop/include/config.h : METRONOME_DURATION_MS` |

### Logique

```kotlin
// Pseudo-code métronome
if (metronome.isEnabled && timer.isRunning && !timer.isPaused) {
    val now = System.currentTimeMillis()
    if (now - lastClickAt >= cadenceMs) {
        playTone(frequency = 250, durationMs = 25)
        lastClickAt = now
    }
}
```

Le premier bip est émis dès le démarrage de l'exposition (sans attendre une cadence complète).

---

## Niveaux de Volume

Source : `printalyzer/buzzer.c` (type `buzzer_volume_t`)

| Niveau | Label UI | Comportement Android |
|---|---|---|
| MUTE | Silencieux | Aucun son |
| QUIET | Faible | Volume 25% |
| MEDIUM | Moyen ← défaut | Volume 60% |
| LOUD | Fort | Volume 100% |

Le volume s'applique à **tous** les sons de l'application (métronome + sons d'événements).

Le volume système du téléphone n'est pas modifié : les sons sont joués via le flux `STREAM_MUSIC` ou `STREAM_ALARM` en respectant le volume utilisateur.

---

## Sons d'Événements

### Fin d'exposition (countdown terminé)
- **Pattern** : 3 bips courts, distinctifs du métronome
- **Fréquence** : 880 Hz (plus aigu que le métronome)
- **Durée chaque bip** : 100 ms, avec 100 ms de silence entre chaque
- Ce son est joué même si le métronome est désactivé (sauf si volume = MUTE)

### Fin d'un patch teststrip
- **Pattern** : 2 bips courts
- **Fréquence** : 660 Hz

### Fin de la session teststrip complète
- **Pattern** : 4 bips progressivement plus aigus
- Signale que tous les patches sont terminés

### Démarrage d'une exposition
- **Pattern** : 1 bip court
- **Fréquence** : 440 Hz
- Optionnel (activable séparément dans les réglages)

---

## Implémentation Android

### API recommandée

**ToneGenerator** (API simple, fréquences fixes) :
```kotlin
val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, volume)
toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
```

**AudioTrack** (pour le métronome précis avec contrôle de fréquence) :
```kotlin
// Génération PCM d'une sinusoïde à 250 Hz, durée 25ms
val sampleRate = 44100
val numSamples = (sampleRate * durationMs / 1000.0).toInt()
val buffer = ShortArray(numSamples) { i ->
    (Short.MAX_VALUE * sin(2 * PI * frequency * i / sampleRate)).toInt().toShort()
}
val audioTrack = AudioTrack.Builder()
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .build())
    .setAudioFormat(AudioFormat.Builder()
        .setSampleRate(sampleRate)
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build())
    .setBufferSizeInBytes(numSamples * 2)
    .build()
audioTrack.play()
audioTrack.write(buffer, 0, numSamples)
```

Le métronome utilise un thread dédié pour garantir la précision temporelle.

### Thread et synchronisation
Le métronome doit tourner dans un thread séparé (ou coroutine avec dispatcher IO) pour ne pas bloquer le thread principal. La logique de timing du compteur est également dans un thread séparé.

---

## Réglages

Dans l'écran Settings :
- **Volume** : sélecteur à 4 niveaux (MUTE / Faible / Moyen / Fort)
- **Métronome** : interrupteur On/Off
- **Cadence métronome** : slider 500ms à 5000ms (visible seulement si métronome activé)
- **Son de démarrage** : interrupteur On/Off

---

## Comportement en Mode Silencieux

Si le téléphone est en mode "Ne pas déranger" ou si le volume système est à 0 :
- Respecter le mode silencieux du système
- Afficher une notification visuelle (flash de l'écran ou indicateur clignotant) en compensation optionnelle

---

## Persistance

Les préférences audio sont sauvegardées dans SharedPreferences (voir `10-data-storage.md`) :
- `pref_metronome_enabled` : Boolean
- `pref_metronome_cadence_ms` : Int
- `pref_buzzer_volume` : Enum (MUTE/QUIET/MEDIUM/LOUD)
- `pref_start_beep_enabled` : Boolean
