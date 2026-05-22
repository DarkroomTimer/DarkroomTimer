# Mode Développement Chimique

Ce mode transforme l'application en assistant de développement pour les procédés photographiques (CP41, E-6, Noir & Blanc, etc.), en guidant l'utilisateur à travers une séquence de bains et de pauses.

---

## Description

Le mode Développement permet de créer des profils de développement personnalisés. Un profil est une suite ordonnée d'étapes (bains et pauses) avec des timers associés et des alertes sonores pour minimiser les erreurs de manipulation et l'incertitude temporelle.

---

## Modèle de Données

### 1. Étape (`Step`)
Une étape est l'unité de base d'une séquence. Il existe deux types d'étapes :

**A. Bain (`BathStep`)**
- `name`: Nom du bain (ex: "Révélateur", "Fixateur 1").
- `durationSeconds`: Temps d'immersion requis.
- `preEndAlertSeconds`: Délai avant la fin pour déclencher l'alerte de pré-fin (optionnel).

**B. Pause (`PauseStep`)**
- `name`: Nom de la pause (ex: "Transfert", "Lavage rapide").
- `durationSeconds`: Durée de la pause.

### 2. Profil de Développement (`DevelopmentProfile`)
- `id`: Identifiant unique.
- `name`: Nom du profil (ex: "Film N&B Standard").
- `steps`: Liste ordonnée d'étapes (`List<Step>`).
- `defaultMode`: Mode de navigation par défaut (`MANUAL` ou `AUTOMATIC`).

### 3. Persistance
Les profils sont stockés dans la base de données Room (table `development_profiles`). Les préférences de session (dernier profil utilisé, dernier mode choisi) sont mémorisées dans `SharedPreferences`.

---

## Logique d'Exécution

### Machine d'États de la Session
Une session de développement suit l'index de l'étape actuelle (`currentStepIndex`).

- **ACTIF** : Le timer de l'étape en cours décompte.
- **PAUSE** : Le décompte est suspendu.
- **TERMINÉ** : Toutes les étapes du profil ont été validées.

### Modes de Navigation
Le passage à l'étape suivante (`currentStepIndex++`) dépend du mode sélectionné :
- **Mode Automatique** : Dès que le timer d'une étape atteint `0`, l'étape suivante est lancée automatiquement sans intervention.
- **Mode Manuel** : Dès que le timer atteint `0`, la session attend l'action de l'utilisateur (bouton "Suivant") pour lancer l'étape suivante.

### Système d'Alertes Sonores
Toutes les alertes utilisent un **bip unique et discret**.

| Événement | Condition | Signal |
|---|---|---|
| **Alerte Pré-fin** | $t = \text{duration} - \text{preEndAlertSeconds}$ (pour les bains) | 1 bip |
| **Fin de Bain** | $t = 0$ (pour les bains) | 1 bip |
| **Fin de Pause** | $t = 0$ (pour les pauses) | 1 bip |

---

## Interface Utilisateur (UI)

L'expérience utilisateur est divisée en trois écrans pour garantir fluidité et clarté.

### 1. Gestionnaire de Profils (Configuration)
Écran dédié à la maintenance de la bibliothèque de profils :
- **Liste des profils** : Affichage, création et suppression.
- **Éditeur de profil** :
    - Saisie du nom du profil.
    - Liste dynamique d'étapes (ajout, suppression, réordonnancement via Drag & Drop).
    - Pour chaque étape : type (Bain/Pause), nom (avec suggestions), durée et délai d'alerte.
    - Sélection du mode par défaut (`MANUAL` / `AUTOMATIC`).

### 2. Écran de Lancement (Confirmation rapide)
Écran de transition optimisé pour un démarrage en un seul clic :
- **Sélection Rapide** : Le dernier profil utilisé et le dernier mode sont pré-sélectionnés.
- **Prévisualisation** : Affichage d'un résumé succinct de la séquence (ex: *Révélateur (60s) $\rightarrow$ Transfert (10s) $\rightarrow$ ...*).
- **Action** : Un bouton unique **"DÉMARRER"**.

### 3. L'Écran de Session (Exécution)
Interface haute visibilité adaptée à l'usage en chambre noire :
- **Affichage Central** : 
    - Nom de l'étape actuelle (police large).
    - Timer géant (`MM:SS`).
- **Commandes** : 
    - **Pause / Reprise**.
    - **Suivant** (permet de passer à l'étape suivante, même en mode automatique).
    - **Quitter** (retour à l'écran de lancement).
- **Progression** : Indicateur visuel de l'avancement dans la séquence (ex: "Étape 2/5").
