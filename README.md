# Cataclysm Weather

Un mod Minecraft haute performance conçu pour simuler des phénomènes météorologiques extrêmes et immersifs.

## Systèmes de Simulation

### 🧊 Grêle Hyper-Réaliste
Le système de grêle est le cœur visuel du mod, offrant une expérience physique et dynamique.
- **Intensité Dynamique** : Supporte 13 niveaux d'intensité, allant d'une légère chute de glace à des bombardements cataclysmiques.
- **Simulation Physique** : Les grêlons sont rendus comme des entités 3D avec des rotations aléatoires et une échelle variant selon l'intensité (jusqu'à 0.8 blocs au niveau max).
- **Logique Hybride** : Les calculs de collision au sol et la planification des impacts sont déportés sur un thread dédié (`WeatherSimulationThread`) pour éviter le lag serveur, tandis que le rendu visuel est synchronisé sur le thread principal pour une fluidité parfaite.

### ⚡ Éclairs Apocalyptiques
La foudre dans Cataclysm Weather ne suit plus les règles vanilla aléatoires et espacées.
- **Fréquence Scalaire** : La probabilité d'impact augmente exponentiellement avec le niveau de tempête, pouvant atteindre jusqu'à 12 impacts par seconde au niveau 13.
- **Intégration Shaders** : Conçu pour fonctionner avec Iris et le shaderpack Photon, garantissant que chaque éclair illumine l'atmosphère de manière cohérente avec l'intensité de la tempête.

### 🌍 Activité Sismique (Quake)
Les tempêtes extrêmes peuvent provoquer des secousses terrestres.
- **Tremblements de Caméra** : Vibrations réalistes de la vue basées sur une échelle d'intensité de 1 à 15.
- **Synchronisation Persistante** : Les états de séisme sont synchronisés périodiquement et lors de la connexion des joueurs pour maintenir l'immersion.

### ☁️ Tempêtes Sèches (Dry Storms)
Une fonctionnalité permettant de maintenir l'ambiance visuelle sans les inconvénients du rendu vanilla.
- **Sky-only Effects** : Possibilité de déclencher des orages sans particules de pluie ni bruits de pluie, tout en conservant les shaders de nuages et de ciel sombre.

## Fonctionnement des Commandes
Le mod remplace la branche standard de la commande `/weather thunder` par un système unifié :
` /weather thunder <level> <isDry> <duration> `
- **level** : 1 à 13 (intensité de la grêle et de la foudre).
- **isDry** : Boolean (true pour des effets de ciel uniquement).
- **duration** : Durée en secondes.

## Architecture Technique
- **Thread Safety** : Utilisation de snapshots immuables et de `ConcurrentHashMap` pour permettre une simulation lourde sans bloquer le tick principal du serveur.
- **Interpolation de Précision** : Logique de mouvement personnalisée pour les entités météo afin d'assurer des visuels fluides même en cas de variation des FPS ou des TPS.
