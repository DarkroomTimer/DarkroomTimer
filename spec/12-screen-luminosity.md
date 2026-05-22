# Contrôle de la Luminosité de l'Écran

Date : 2026-05-22
Statut : Approuvé

## 1. Objectif
L'objectif est d'empêcher que la luminosité de l'écran du téléphone ne provoque un voile sur le papier photographique en chambre noire, tout en garantissant que l'interface reste lisible selon la lumière ambiante.

## 2. Architecture Technique

### Flux de Données
Le système suit une chaîne de traitement linéaire :
`Capteur de Lumière (Lux)` $\rightarrow$ `Filtre de Lissage` $\rightarrow$ `Mappage de Luminosité` $\rightarrow$ `Application Window Override`.

### Détails des Composants
- **Capteur** : Utilisation de `SensorManager` avec le capteur `TYPE_LIGHT`.
- **Filtre de Lissage** : Implémentation d'une moyenne glissante (moving average) sur les données reçues durant les 3 dernières secondes. Cela permet d'éviter le scintillement (flicker) lors de micro-variations de lumière.
- **Mappage** : Conversion de la valeur lissée (Lux) en un pourcentage de luminosité (0.0 à 1.0) compris entre les bornes `Min` et `Max` définies par l'utilisateur.
- **Application** : Utilisation de `WindowManager.LayoutParams.screenBrightness` pour appliquer l'override de luminosité uniquement à la fenêtre de l'application.

## 3. Configuration et Paramètres

L'utilisateur peut configurer le comportement via l'écran des réglages :

### Modes de fonctionnement
- **Mode Adaptatif** : La luminosité varie selon le capteur de lumière.
    - `Luminosité Minimale` (0-100%) : Seuil bas pour éviter l'extinction complète.
    - `Luminosité Maximale` (0-100%) : Seuil haut pour limiter l'éblouissement.
- **Mode Fixe** : La luminosité reste constante.
    - `Valeur Fixe` (0-100%) : Luminosité appliquée sans tenir compte du capteur.

### Logique d'application
- Si `Mode == Adaptatif` $\implies$ $\text{Luminosité} = \text{clamp}(\text{f}(\text{Lux}_{\text{lissée}}), \text{Min}, \text{Max})$.
- Si `Mode == Fixe` $\implies$ $\text{Luminosité} = \text{Valeur Fixe}$.

## 4. Contraintes et Gestion d'Erreurs

### Cas d'erreur
- **Capteur indisponible** : Si le capteur `TYPE_LIGHT` est absent ou désactivé, l'application bascule automatiquement en mode **Fixe** et informe l'utilisateur via un message dans les réglages.
- **Cycle de vie** : L'override de luminosité est actif uniquement quand l'application est au premier plan. L'effet est levé dès que l'application passe en arrière-plan.

### Stratégie de Test
- **Mode Simulation** : Ajout d'un curseur de debug permettant d'injecter des valeurs de Lux fictives pour valider le lissage et le mappage sans manipulation physique.
- **Test de transition** : Vérification que les changements brusques de lumière ne provoquent pas de sauts visuels désagréables.
