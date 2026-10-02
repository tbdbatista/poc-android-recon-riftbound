# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] (develop)
*Tracks changes and PR merges integrated into `develop` awaiting release to `main`.*

### Added
- Dedicated Settings Screen (`SettingsScreen.kt`) featuring:
  - Theme mode selection (Dark Mode, Light Mode, and System Default) with real-time application theme updates.
  - Automatic synchronization of the device home screen launcher icon (`AppIconHelper.syncIconWithTheme`) based on active theme mode (Dark icon for Dark theme, Light icon for Light theme).
  - Preference toggle for skipping deletion confirmation dialogs during card scanning and collection management.
  - Application and TCC research metadata section.
- Settings gear button in `CollectionsScreen.kt` header replacing legacy palette button.
- `ThemeMode` enum and preference persistence in `AppPreferences.kt` and `MainViewModel.kt`.
- Real-time visual and tactile feedback for optical scanning in `ScanScreen.kt`:
  - Floating status feedback banner (`ScanFeedbackBanner`) powered by `AnimatedContent`, sliding down with a fresh transition on every card scan (including rapid consecutive scans).
  - High-contrast visual cards displaying card thumbnail, name, set, and code on successful match, or helpful alignment guidance on failure.
  - Haptic feedback confirmation on card recognition and error events via `LocalHapticFeedback`.
  - `ScanFeedback` sealed class state flow in `MainViewModel`.
- Animated Splash Screen (`SplashScreen.kt`) featuring the "Arcane Aperture" motif with rotating rune ring, expanding camera shutter blades, and teal optical laser sweep ray, adapting to both Dark and Light system themes.
- Adaptive vector app launcher icons for both Dark Theme (`ic_launcher` - Obsidian & Arcane Neon) and Light Theme (`ic_launcher_light` - Ice-White & Royal Purple) with Android 13+ monochrome themed icon support.

### Removed
- Removed manual app icon customization dialog and palette button from `CollectionsScreen.kt`, now fully replaced by automated theme-based icon synchronization in Settings.
- Removed the rigid card alignment guide frame (`CardGuideFrame`) from the camera viewfinder, enabling a 100% clean, unobstructed full-screen viewfinder experience.
- Removed the debug console / text logs terminal overlay from the camera viewfinder in `ScanScreen.kt`.
- Removed unused cooldown / delay timestamps (`lastScannedTime` and `lastScannedCardId`) from `MainViewModel`.

---

## [0.5.2] - 2026-10-01
### Fixed
- Fixed duplicated Vendetta (`VEN`) cards in Compendium and `all_cards.json` by removing 131 duplicate legacy entries and ensuring all 227 unique collector numbers are correctly represented, resolving Issue #32.

---

## [0.5.1] - 2026-10-01
### Fixed
- Fixed missing card artwork/mockups on clean app installs by updating `all_cards.json` `imageUrl` references from `.png` to `.webp` format, resolving Issue #28.

---

## [0.5.0] - 2026-10-01
### Added
- Table-style header row ("Posição", "Carta", "Nome", "Coleção / Cód.") with minimum 40dp height on the Collection Detail screen.
- Bold card position text ("#1", "#2", etc.) replacing the old circular badge.
- Card artwork thumbnails using asynchronous decoding and caching (`AsyncImage`).
- Exact card code format without `#` prefix (e.g. "Origins • 066a/298", "Vendetta • SP1/006", "Vendetta • R04").
- Support for special subcollections with distinct counts (e.g. Vendetta Crystal collection reprints `SP1/006` to `SP6/006`) and uppercase prefix normalization (`SP`, `R`, `T`).
- Multi-criteria sorting dropdown in Collection Detail screen (1ª para última, Última para 1ª, Nome A-Z, Coleção, and Numeração da carta).
- Enlarged card preview dialog when tapping any card row in the collection details.
- Comprehensive Design System steering specification in `DESIGN_SYSTEM.md` detailing color palette tokens, typography scales, component specs, and UI rules.
- Comprehensive Pull Request (PR) standards, naming conventions, templates, and lifecycle rules in `AGENTS.md`.

### Changed
- Streamlined collection card rows to display card name, set, and collector code, removing energy/might stats and individual delete icons in preparation for list edit mode and swipe-to-delete.
- Refined set total assignment so main set totals are applied only to main numerical sequences, preserving reprints and subcollection numbering.
- Adjusted `SecondaryTeal` from `#00E5FF` to `#00BFA6` for improved contrast and readability on both dark and light themes.
- Light theme now uses dedicated color adjustments (darker purple `#7B3BDB`, teal `#009B86`, gold `#E6A800`) instead of sharing the same values as the dark theme.
- Added custom `RiftboundTypography` with a fully defined Material 3 type scale (`titleLarge` through `labelSmall`) to `Theme.kt`.
- Extracted all hardcoded colors from UI screens into centralized `Theme.kt` constants: scanner console log colors (`LogSuccess`, `LogError`, `LogMetadata`, `LogBoundary`), set gradient colors (`SetGradientColors`), and energy cost badge (`EnergyCostBadge`).
- Replaced all hardcoded `Color.Red` references across `CollectionDetailScreen`, `SearchScreen`, and `ScanScreen` with semantic `MaterialTheme.colorScheme.error`.
- Added explicit `error` and `outlineVariant` tokens to `darkColorScheme`.
- Redesigned `CollectionsScreen` with an app branding header, summary statistics card (total collections and cards), icon boxes, metadata chips, and an extended floating action button.
- Overhauled `SearchScreen` with a dedicated physical reverse-lookup card design displaying box name and glowing `Posição: #N` location badges.
- Enhanced `CompendiumScreen` with clearable search, glowing active set filter pills, standard TCG aspect ratio cards with bottom scrim gradients, gold ownership badges (`xN`), and rich card details modal.
- Modernized `CollectionDetailScreen` with search clear button, sort indicator chips, and refined table card rows.
- Updated `ScanScreen` with high-tech viewfinder corner reticles, color-coded console logs, and thumbnails with `#N` position overlays.
- Redesigned `MainActivity` navigation bar with modern icons (`CenterFocusStrong`, `AutoStories`, `CollectionsBookmark`), active tab indicator, and subtle top border.
- Converted all 1451 card images from PNG to WebP (quality 80), reducing `assets/images` from 1.3 GB to 130 MB (~90% reduction) and APK size from 1.3 GB to 178 MB.
- Updated `sync_cards.py` to download images as temporary PNG, convert to WebP via `cwebp`, and skip re-download when WebP already exists locally.
- Updated `all_cards.json` `imageUrl` references from `.png` to `.webp`.

---

## [0.4.0] - 2026-09-21
### Added
- Scanner screen displays newly scanned cards first (left-to-right reverse order: newest card at position 1) in the bottom shelf with automatic scroll to the newest item upon capture.
- Card preview modal when tapping scanned thumbnails on the scanner screen, offering options to delete the card or close the modal.
- Deletion confirmation dialog with a "Do not ask again" checkbox for removing cards from the scanning session, backed by persistent preferences (`AppPreferences`).
- Updated bottom scanner left action button to delete the last captured card with deletion confirmation prompt support ("Não perguntar novamente").
- Conclude session options dialog on the check button offering choices to save the card list, continue capturing, or discard all captured cards with a confirmation dialog.
- Confirmation dialog when choosing to discard all captured cards from the scanning session.
- Added "Desfazer última exclusão" button on the scanner screen to restore recently deleted cards, enabled only after a deletion occurs.
- Added descriptive text labels beneath all scanner action buttons ("Desfazer exclusão", "Excluir última", "Capturar carta", and "Concluir captura") for improved usability.

---

## [0.3.0] - 2026-09-20
### Added
- Automated GitHub Actions CI workflow (`.github/workflows/auto-release.yml`) to automatically create Git tags and publish GitHub Releases with notes from `CHANGELOG.md` whenever changes merge into `main`.
- Missing in-game token cards for Spiritforged (SFD) and Unleashed (UNL) sets, bringing total cards to 1.472.

### Fixed
- Updated OCR collector code regular expression in `CardScannerMatcher` and `MainViewModel` to support alphanumeric prefixes (e.g., `t01`, `r01`, `sp3`), enabling camera recognition for all tokens and runes.

---

## [0.2.0] - 2026-09-20
### Added
- Card collection sharing feature via native Android Share Sheet (`CollectionShareHelper`), formatting collection name, card quantity, collector number, set code (e.g., OGN, SPF), and card name for export to WhatsApp, Google Drive, and other apps.
- Synchronized full card database (1.464 cards, including 358 cards from the "Vendetta" (VEN) expansion and promotional sets) from Riftcodex API.
- Added automated synchronization script `scripts/sync_cards.py` for fetching future sets and downloading artwork.
- Added "Vendetta" filter tab with dedicated visual theme to Compendium screen.

### Fixed
- Updated Room database seed logic in `CardRepositoryImpl` to incrementally synchronize newly added cards from assets json instead of only executing on an empty database.

---

## [0.1.1] - 2026-09-20
### Added
- Project development steering rules in `AGENTS.md`.
- Centralized version management system in `version.properties`.
- Project changelog in `CHANGELOG.md`.
- Comprehensive technical specification and architectural documentation in `README.md` for TCC (PUC-PR).

### Changed
- `app/build.gradle.kts` dynamically resolves `versionCode` and `versionName` from `version.properties`.

### Fixed
- Explicitly configured Kotlin JVM toolchain to Java 21 in `app/build.gradle.kts` to guarantee Gradle 8.5 compatibility and resolve Android Studio JVM 25 selection issues.

---

## [0.01.0] - 2026-07-14
### Added
- Initial project foundation with OCR card scanner POC, Compendium, Room local database, and Jetpack Compose UI.
