# TestStrip UI Redesign

## Overview
Redesign the TestStrip interface to fit on a single screen, simplify the workflow, and clarify the user experience.

## Goals
- Interface must fit on a single screen without scrolling
- Remove "Configuration" section from the main view during session
- Separate configuration phase from execution phase
- Clarify "en attente" status message
- Support auto-start of first patch after configuration

## User Flow

### Phase 1: Configuration (Before Session Starts)
User enters configuration values:
- Base time (temps de base)
- Increment type (Fixed / % / f-stop)
- Increment value
- Number of patches (1-9)
- Mode (Test/Sequential)

User clicks "Démarrer" → session initializes and first patch starts immediately.

### Phase 2: Session (During Exposure)
Display elements:
- **Line 1:** Mode badge | Pause/Resume button
- **Line 2:** Increment type badge | +/- buttons (adjust next patch time)
- **Progression:** 9 circles representing patches (active / done / not reached)
- **Timer:** Large display showing remaining time for current patch
- **Cancel button:** To abort the session

Configuration is locked once the session has started (first patch begins).

## State Changes

### Current States
- `CONFIGURED` → "En attente" (waiting for relay connection)
- `EXPOSING` → Active exposure
- `BETWEEN_PATCHES` → Between patches (previously allowed config modification)
- `PAUSED` → Session paused

### New States
- Remove `CONFIGURED` state (replaced by configuration phase before session)
- States start at `EXPOSING` (first patch auto-starts)
- `BETWEEN_PATCHES` → No config modification allowed
- "En attente" → Replaced by clear connection status indicator

## Components to Modify

1. **TeststripScreen.kt**
   - Remove configuration section from main view
   - Add pre-session configuration modal/screen
   - Simplify main view to show only session controls
   - Replace "En attente" with clearer status

2. **TeststripViewModel.kt**
   - Manage configuration phase state separately
   - Lock configuration once session starts
   - Allow +/- to adjust next patch time only

3. **TeststripSession.kt**
   - Auto-start first patch after configuration validation
   - Remove state transitions that allow config modification during session

4. **TeststripEngine.kt**
   - Support dynamic time adjustment for next patch (via -/+ buttons)

## Non-Goals
- Changing the core exposure calculation logic
- Modifying patch display grid (circles stay)
- Adding new increment types
- Changing relay connection logic

## Success Criteria
- All controls visible without scrolling on a standard phone screen
- User can complete a teststrip session without navigating between screens
- "En attente" message is eliminated or clarified
- Configuration cannot be modified after session starts
- First patch auto-starts after clicking "Démarrer"
