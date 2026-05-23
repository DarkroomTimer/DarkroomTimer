# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands
- Build Debug APK: `./gradlew assembleDebug`
- Run Unit Tests: `./gradlew test`
- Run Single Unit Test: `./gradlew test --tests "fr.mathgl.darkroomtimer.package.ClassName"`
- Run Instrumented Tests: `./gradlew connectedAndroidTest`
- Run Lint: `./gradlew lint`
- Clean Project: `./gradlew clean`

## Project Architecture
DarkroomTimer is an Android application designed to control darkroom enlargers, supporting both standalone visual simulation and physical relay control via WiFi.

### Core Modules
- `fr.mathgl.darkroomtimer.ui`: Jetpack Compose based user interface.
- `fr.mathgl.darkroomtimer.math`: Photography and exposure calculations (f-stop math, teststrips).
- `fr.mathgl.darkroomtimer.storage`: Persistence layer using Room database and Gson.
- `fr.mathgl.darkroomtimer.system`: System-level integrations, including relay drivers (Tasmota/ESPHome) and Foreground Services for timer stability.

### Key Architectural Patterns
- **Mode Switching**: The app operates in either Standalone mode (UI only) or Companion mode (WiFi relay control).
- **Hardware Stability**: Uses Foreground Services, WakeLocks (`PARTIAL_WAKE_LOCK`), and WifiLocks to ensure timers are not interrupted by Android's power management.
- **Domain Logic**: Complex exposure logic (Burn & Dodge, Contrast Grades) is decoupled from the UI in the `math` and `system` packages.

## Technical Constraints
- **Min SDK**: 24
- **Target SDK**: 36
- **Language**: Kotlin with Jetpack Compose
- **Database**: Room
