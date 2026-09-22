# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] (develop)
*Tracks changes and PR merges integrated into `develop` awaiting release to `main`.*

### Added
- Scanner screen now displays newly scanned cards first (left-to-right reverse order: newest card at position 1) in the bottom shelf with automatic scroll to the newest item upon capture.

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
