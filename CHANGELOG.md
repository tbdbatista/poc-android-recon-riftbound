# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] (develop)
*Tracks changes and PR merges integrated into `develop` awaiting release to `main`.*

### Added
- Project development steering rules in `AGENTS.md`.
- Centralized version management system in `version.properties`.
- Project changelog in `CHANGELOG.md`.
- Comprehensive technical specification and architectural documentation in `README.md` for TCC (PUC-PR).

### Changed
- `app/build.gradle.kts` dynamically resolves `versionCode` and `versionName` from `version.properties`.

---

## [0.01.0] - 2026-07-14
### Added
- Initial project foundation with OCR card scanner POC, Compendium, Room local database, and Jetpack Compose UI.
