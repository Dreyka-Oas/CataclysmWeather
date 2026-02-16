# Cataclysm Weather - Minecraft Fabric Mod

## Project Overview
Cataclysm Weather is a high-performance Minecraft mod for **Fabric 1.21.11**, providing extreme weather events like hyper-realistic hail and apocalyptic lightning, optimized for performance and immersion.

## Side Projects Context
- **Metric-Hub** (`/media/OasWork/Minecraft/Autres/metric-hub`) : 
    - **Stack** : Rust Fullstack (Leptos + Tauri).
    - **Data** : SQLite (via SQLx).
    - **UI** : Composants Leptos.
    - **Scripts** : Utilise des migrations SQL.

## Build & Run Instructions
Commands must be run from `fabric/1.21.11/`.

- **Build Mod**: `./gradlew build`
- **Run Client**: `export JAVA_HOME=/home/dreykaoas/.sdkman/candidates/java/21.0.2-graalce && ./gradlew runClient`
- **Clean**: `./gradlew clean`

> **Note de Test** : Le client **doit être lancé dans le terminal** après chaque modification pour valider les changements.

## Custom Tools & Skills
- **Skill: codebase-context** : Installé localement.
    - Permet de scanner un projet avec `node skills/codebase-context/scripts/map_project.cjs <path>`.
    - Identifie les stacks Java/Fabric, Rust/Tauri et Shaders.

## Important Directories
- **Runtime Shaders (Photon)**: `fabric/1.21.11/run/shaderpacks/photon_v1.2a/shaders/`
    - C'est ici que le shaderpack Photon est lu par Iris. Les modifications de paramètres (`settings.glsl`) et les inclusions se trouvent ici.
- **Reference & Temp**: `/media/OasWork/Minecraft/Mods/CataclysmWeather/tmp/`
    - Contient les archives de Photon et d'autres mods de météo pour comparaison.

## Technical Architecture
- **`WeatherState`**: Central logic for weather events (hail, lightning).
    - **Hail System**: Manages hail levels (1-13) and distributes impacts over time.
    - **Lightning System**: Handles extreme lightning probability (up to 12 bolts/sec at level 13).
    - **Thread Safety**: Uses `ConcurrentHashMap` and snapshots to avoid crashes during multi-threaded simulation.
- **`HailManager` (Client)**: Visual simulation of hail stones on the Main Thread for smooth rendering.
- **`WeatherSimulationThread`**: Dedicated background thread for heavy server-side calculations (lightning logic, impact planning).
- **Mixins**: 
    - **Dry Weather**: Intercepts rain particles and sound volume at low level to ensure a perfectly silent "dry" storm while keeping cloudy sky shaders.
    - **Commands**: Unified `/weather thunder <level> <dry> <duration>` command.

## Known Issues & Solutions
1.  **Issue:** Rain sound/particles during dry weather.
    *   **Solution:** Intercept `ParticleManager` and `AbstractSoundInstance` to force zero volume/count when `isDry` is true.
2.  **Issue:** Server lag during storms.
    *   **Solution:** All lightning logic and impact planning moved to `WeatherSimulationThread`.
3.  **Issue:** Redundant commands.
    *   **Solution:** Replaced vanilla `thunder` command branch with custom unified logic.

## Task Tracking
See `TASKS.md` for detailed progress.
