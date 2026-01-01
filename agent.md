# AI Agent Brief: MyMoney Project

This document provides a comprehensive overview of the **MyMoney** Android project for future AI agents. Read this first to understand the context, architecture, and current state.

## 1. Project Context
**MyMoney** is a modern Android application built to migrate and enhance the legacy **moneywallet** app.
- **Goal**: Provide a premium, performant, and modern financial tracking experience.
- **Key Feature**: Ability to import `.mwbx` backup files (which are ZIP files containing a `database.json` and a `metadata.json`).

## 2. Tech Stack & Architecture
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (100%)
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **Preferences**: Jetpack DataStore
- **Architecture**: Domain-driven layered architecture:
    - `core.data`: Contains Local (Room), Preferences (DataStore), and Import (Backup Parsing) logic.
    - `core.util`: Central utilities like `MoneyFormatter` and `DateUtils`.
    - `feature.*`: Feature-based modules (Home, Wallet, Settings) containing ViewModels and Screens.

## 3. Core Logic & Conventions

### Money Formatting (`MoneyFormatter.kt`)
Always use `MoneyFormatter.format()` for displaying balances. It handles:
- Decimal precision (legacy data often uses `Long` to represent minor units).
- Currency symbols.
- User-defined formatting preferences (Decimals, Grouping, Plus/Minus).

### Date Handling (`DateUtils.kt`)
Supports multiple date formats (patterns 0-8) inherited from the legacy app. Use this for parsing and consistent UI display.

### Data Import (`BackupImporter.kt`)
Responsible for unzipping backups and mapping JSON to Room Entities. It also includes an "Analysis" mode to detect "Zombie" (corrupted) records before import.

## 4. UI Design System (Premium Look)
We avoid standard Material 3 defaults in favor of a custom "Premium" aesthetic:
- **Glassmorphism**: Headers use dynamic gradients (`generateColor(name)`), semi-transparent surfaces, and `Canvas` drawings.
- **Timeline View**: The Transaction list uses a sticky month header, followed by daily sub-headers, and a vertical timeline line connecting transaction icons.
- **Dynamic Action Bar**: TopAppBars are often transparent at the top, with titles fading in only after the user scrolls past the hero header.

## 5. Key Constants
- `TOTAL_WALLET_ID`: A reserved ID used to represent the "Global" or "Total" wallet view which aggregates transactions from all non-archived wallets.

## 6. Current Progress (as of Dec 2025)
- [x] Full Backup Import parity.
- [x] Implement App Shortcuts (Quick Actions) <!-- id: 8 -->
- [/] Implement M3 Design Refinements <!-- id: 9 -->
    - [ ] Enable true Edge-to-Edge in `MainActivity` <!-- id: 10 -->
    - [ ] Modernize `HomeScreen` (M3 FAB, Centered Header, Tonal Depth) <!-- id: 11 -->
    - [ ] Refine `TransactionDetailsScreen` (Immersive Header, M3 FAB) <!-- id: 12 -->
- [x] Verify implementation in `AndroidManifest.xml` and resource folders <!-- id: 4 -->
- [x] Settings management (Formatting & Default Wallet).
- [x] Home Screen Redesign (Grand Total hero card).
- [x] Dynamic TopBar implementation.

## 7. Immediate Next Steps
- Finalize the "Total Wallet" dedicated view logic (ensure it handles all transactions correctly).
- Comprehensive testing of the new UI transitions.
- Accessibility and Localization review.

---
**Advice for Agents**: When editing UI, prioritize aesthetics (gradients, spacing, typography). When editing Data logic, ensure parity with the `Long` based money representation to avoid precision errors.
