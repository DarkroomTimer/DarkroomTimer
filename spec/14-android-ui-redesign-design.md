# Android UI Redesign — Approche "Centre de Contrôle"

Date : 2026-05-22
Statut : Approuvé (Brainstorming)

## 1. Philosophie de Design

L'interface est optimisée pour un téléphone posé sur une table à environ 50 cm de l'utilisateur. Elle privilégie la visibilité et la réduction de la charge mentale pendant le travail en chambre noire.

### Principes Clés
- **Visibilité à distance :** Éléments critiques (timer) en taille massive.
- **Hiérarchie d'interaction :**
    - *Actions Principales :* Cibles larges, commandes simples (Tap/Toggle).
    - *Configuration :* Saisie précise via Bottom Sheets ou dialogues (accepté pour les réglages/profils).
- **Flux de Travail :** Navigation linéaire et intuitive, minimisant les changements de fenêtres.
- **Thème :** Sombre profond (Noir `#000000`) avec accents inactiniques (Rouge sombre `#CC2200`) pour éviter le voile sur le papier.

---

## 2. Écran d'Exposition (`CountdownScreen`)

### Organisation Visuelle
L'écran est divisé en trois zones verticales :

#### A. Zone de Monitoring et Édition (Haut/Centre)
- **Timer Interactif :** L'élément central. Police monospace géante. 
    - *Action :* Un tap direct sur le temps ouvre une **Bottom Sheet** d'édition (champ numérique + boutons $\pm$).
- **Indicateurs d'État :** Tuiles compactes entourant le timer :
    - *Relais Agrandisseur :* État ON/OFF + Icône simulation.
    - *Relais Safelight :* État ON/OFF.
- **Badge de Grade :** Affichage du grade actuel. Tap pour accès rapide au sélecteur.
- **Indicateur BT :** Icône d'état de connexion en haut à gauche.

#### B. Zone de Pilotage (Centre/Bas)
- **Contrôles Timer :** Boutons massifs `[STOP / PAUSE]`, `[START / RESUME]`, `[RESET]`.
- **Toggle Métronome :** Interrupteur direct et visible pour activer/désactiver le métronome sans menu.
- **Panneau Burn & Dodge :** Accordéon rétractable affichant la liste des zones ajustées et leurs temps.

#### C. Zone de Navigation et Ajustements (Bas)
- **Carrousel de Grade :** Ruban horizontal permettant de changer le grade par glissement latéral (swipe) sans ouvrir de dialogue.
- **Point d'entrée Développement :** Bouton large et distinct **"Lancer l'Assistant Dev"**, placé logiquement après les contrôles de timer.
- **Barre de Navigation :** Onglets pour basculer vers la Bande de Test et les Réglages.

---

## 3. Écran de Bande de Test (`TeststripScreen`)

L'interface suit un flux en trois phases pour guider le photographe.

### Phase 1 : Configuration
- **Cartes de Paramètres :** Cartes interactives pour le temps de base et l'incrément.
- **Prévisualisation Dynamique :** Liste verticale des patches (bulles) qui s'actualise en temps réel lors des modifications de base/incrément.
- **Lancement :** Bouton `[DÉMARRER LA BANDE ►]` massif.

### Phase 2 : Exécution (Mode Focus)
- **Focus Visuel :** L'écran se simplifie pour ne montrer que l'essentiel.
- **Progression :** Barre de progression large (circulaire ou linéaire) entourant le temps restant du patch courant.
- **Infos :** Affichage clair du patch actuel (`Patch X / N`) et du temps cumulé.
- **Sécurité :** Boutons `[PAUSE]` et `[ABANDONNER]` bien distincts.

### Phase 3 : Transition (Inter-Patch)
- **Instruction Explicite :** Message d'alerte massif : **"COUVRIR LE PATCH X"**.
- **Validation :** Bouton `[PATCH SUIVANT →]` pour confirmer que le papier est prêt pour l'exposition suivante.

---

## 4. Mode Développement et Réglages

### Mode Développement (`DevelopmentScreen`)
- **Alignement Visuel :** Utilisation des mêmes codes couleurs et tailles de police que le `CountdownScreen`.
- **Flux :** Maintien de la structure (Gestionnaire $\rightarrow$ Lancement $\rightarrow$ Session) avec des boutons de commande massifs en phase de session.

### Réglages (`SettingsScreen`)
- **Saisie Classique :** Utilisation de composants Compose standards (Switch, Slider, Dropdown).
- **Organisation :** Sections claires (Général, Métronome, Exposition, Teststrip, Agrandisseur, Bluetooth, Données).
