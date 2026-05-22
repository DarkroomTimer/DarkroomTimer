# DarkroomTimer — Vue d'ensemble

## Objectif

DarkroomTimer est une application Android destinée aux photographes pratiquant le tirage argentique en chambre noire. Elle remplace (ou complète) les minuteries électroniques physiques dédiées, en offrant les modes essentiels du tirage : compte à rebours d'exposition, bande de test, et assistant de développement chimique.

L'application s'inspire de deux firmwares open-source :
- **fStop** (Arduino/PlatformIO) : minuterie simple avec calculs f-stop et bande de test
- **Printalyzer** (STM32/FreeRTOS) : minuterie professionnelle avec grades de contraste, burn/dodge, profils agrandisseur

---

## Modes de fonctionnement

### Mode Standalone
L'application fonctionne seule sur le téléphone. Les relais sont simulés visuellement : un indicateur à l'écran montre si l'agrandisseur serait allumé ou éteint. Le photographe actionne manuellement l'agrandisseur en suivant les indications visuelles et sonores.

### Mode Compagnon Relais (WiFi)
L'application se connecte via WiFi à un ou deux périphériques relais pour contrôler physiquement :
- Le relais de l'agrandisseur (enlarger relay)
- Le relais de la lampe inactinique (safelight relay, optionnel)

Le matériel supporté en v1 sont des relais **Sonoff Dual R3** avec firmware **Tasmota** ou **ESPHome**. L'agrandisseur et le safelight peuvent être sur le même appareil (2 canaux) ou sur deux appareils distincts, y compris de protocoles différents.

En cas de coupure réseau, l'app affiche un indicateur d'erreur. Le Bluetooth BLE est prévu pour une version future via le proxy BLE d'ESPHome.

Voir `09-relay-driver.md` pour l'architecture complète.

---

## Fonctionnalités incluses

| Fonctionnalité | Source |
|---|---|
| Compte à rebours avec contrôle relais | fStop/countdown |
| Calcul f-stop (expositions exponentielles) | fStop/stop.cpp |
| Bande de test (teststrip) | fStop + Printalyzer |
| Assistant développement chimique | Printalyzer / Custom |
| Grades de contraste (00 à 5) | Printalyzer/contrast |
| Burn & Dodge (ajustements par zones) | Printalyzer/exposure_state |
| Profils agrandisseur (délais rampe) | Printalyzer/enlarger_config |
| Métronome audio | fStop/metronome |
| Contrôle WiFi des relais (Tasmota, ESPHome) | — |
| Persistance des paramètres | fStop/params + Printalyzer/settings |

---

## Fonctionnalités explicitement exclues

Ces fonctionnalités sont spécifiques au hardware du Printalyzer et ne s'appliquent pas à une app Android :

- **Contrôle DMX512** : protocole d'éclairage scénique, aucun équivalent Android
- **Densitomètre** (Densistick) : capteur physique USB
- **Sonde de mesure lumière** (Meter Probe) : capteur TSL2585 externe
- **Profils papier / courbes PEV** : nécessitent un densitomètre pour être utiles
- **Interface LCD 16×2** : remplacée par l'écran Android
- **Clavier ADC** : remplacé par les boutons tactiles

---

## Contraintes Android

### Permissions requises

**Mode standalone et mode compagnon WiFi :**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
```

Ces permissions sont dans la catégorie "normale" — aucune demande à l'utilisateur nécessaire.

**Mode compagnon Bluetooth (futur) :**
```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />  <!-- API 31+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />                      <!-- API 31+ -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /> <!-- BLE scan < API 31 -->
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

Les permissions Bluetooth ne seront demandées que lors de l'implémentation du driver BLE.

### API minimale
- **minSdk** : 26 (Android 8.0) — pour AudioTrack moderne et coroutines stables
- **targetSdk** : 36 (Android 16)

### Comportement en arrière-plan et Énergie
Le timer doit continuer à fonctionner si l'écran s'éteint ou si l'app passe en arrière-plan.
- **Foreground Service** : Utiliser un service avec notification persistante pendant qu'une exposition est active.
- **WakeLocks** : Utiliser `PowerManager.WakeLock` (PARTIAL_WAKE_LOCK) pour empêcher le CPU de s'endormir.
- **WifiLocks** : Utiliser `WifiLock` pour empêcher la radio WiFi de passer en mode économie d'énergie, garantissant une latence minimale pour les commandes relais.
- **Écran et Affichage** : 
    - Forcer l'écran à rester allumé (`FLAG_KEEP_SCREEN_ON`) pendant l'utilisation du timer.
    - Utiliser le mode **Immersif (Plein Écran)** pour masquer les barres système et éviter les interactions accidentelles.

### Gestion des interruptions (Mode Focus)
Pour éviter que des notifications, appels ou SMS n'illuminent l'écran ou ne perturbent le photographe en chambre noire :
- L'application propose un réglage global pour activer le mode **"Ne pas déranger" (Do Not Disturb)** du système Android.
- Ce mode peut être activé en permanence via les réglages de l'app pour garantir un environnement de travail sans interruptions.
- Cela nécessite la permission `android.permission.ACCESS_NOTIFICATION_POLICY`.


---

## Glossaire

| Terme | Définition |
|---|---|
| **f-stop** | Unité photographique d'exposition. 1 stop = doublement ou halvage du temps d'exposition. |
| **Fraction de stop** | Subdivision d'un stop : 1/3, 1/2, etc. Permet des ajustements fins. |
| **Agrandisseur (Enlarger)** | Appareil projetant le négatif sur le papier. Contrôlé par un relais. |
| **Safelight** | Lampe inactinique rouge/orange allumée en chambre noire. Doit s'éteindre pendant l'exposition. |
| **Teststrip (bande de test)** | Série d'expositions progressives sur un même papier pour trouver le temps optimal. |
| **Grade de contraste** | Paramètre des filtres de l'agrandisseur (00 à 5). Grade 2 = contraste normal. |
| **Burn** | Surexposer localement une zone du tirage (plus sombre). |
| **Dodge** | Sous-exposer localement une zone du tirage (plus clair). |
| **PEV** | Print Exposure Value. Logarithme de l'exposition en lux-secondes × 100. Non utilisé dans cette version. |
| **Relais** | Interrupteur électromagnétique contrôlant le courant 220V de l'agrandisseur. |
| **CLOSED / OPEN** | État du relais : CLOSED = circuit fermé = courant passe = lampe allumée. |
