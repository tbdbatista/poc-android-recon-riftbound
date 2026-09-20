# Project Guidelines & Development Workflow (Steering Rules)

This document establishes the official development rules, branch lifecycle, commit conventions, and versioning standards for this project. All contributors and AI agents MUST strictly comply with these rules.

---

## 1. Branch Strategy & Hierarchy

```
[ working branch ] (feature/*, bugfix/*, config/*, refactor/*, bump/*)
         │
         ▼ (PR)
     [ epic/* ] ──(PR)──► [ develop ] ──(PR)──► [ main ]
                                                      │
                                                      ▼
                                              [ Release / Tag ]
```

### 1.1 Protected Branches (No Direct Commits)

* **`main`**:
  * Represents production-ready, stable releases.
  * **STRICT RULE:** Direct commits are NEVER allowed.
  * **STRICT RULE:** Only `develop` may be merged into `main`. No feature, bugfix, or epic branch can ever be merged directly into `main`.
  * Every merge of `develop` into `main` constitutes a new release and MUST bump the version number.

* **`develop`**:
  * The central integration branch for day-to-day development.
  * **STRICT RULE:** Direct commits are NEVER allowed.
  * Changes are integrated solely via Pull Requests (PR).
  * Merges into `main` when a milestone or release is ready.

* **`epic/<context>-<description>`**:
  * Operates as a "parallel develop" branch for large initiatives, multi-agent tasks, or extensive features.
  * **STRICT RULE:** Direct commits are NEVER allowed on an epic branch; work must be merged via PRs from working branches.
  * **STRICT RULE:** An `epic/*` branch CANNOT be merged directly into `main`. It MUST be merged into `develop` via PR.

---

## 2. Working Branches

All active development must occur in dedicated working branches created from and merged back into `develop` (or into an `epic/*` branch if contributing to an ongoing epic).

### 2.1 Branch Types

| Branch Type | Description / When to Use |
| :--- | :--- |
| `feature/` | Development of new features or capabilities. |
| `bugfix/` | Resolution of bugs, regressions, or unexpected behaviors. |
| `config/` | Exclusively for project configuration files (e.g., Gradle, build tools, CI/CD, linters). |
| `refactor/` | Code modifications that do not add new functionality or alter public behavior (e.g., architectural cleanup, renaming, performance optimizations). |
| `bump/` | Exclusively for updating version-control files (e.g., `version.properties`, `build.gradle.kts`) prior to merging into `main`. |

### 2.2 Branch Naming Standards

* **Language:** Branch names must **ALWAYS be written in English**.
* **Pattern:** `<type>/<context>-<description>[-part<N>]`
  * Use hyphen-separated lowercase words (`kebab-case`).
  * If a large feature is divided into sequential parts, suffix with `-part1`, `-part2`, etc.

**Examples:**
* `feature/notification-push-service-setup-part1`
* `config/tuist-config-tuist-at-project`
* `bugfix/card-scanner-bounding-box-alignment`
* `refactor/database-card-dao-queries`
* `bump/release-v1-0-1`

---

## 3. Commit Message Standards

* **Language:** Commit messages must **ALWAYS be written in English**.
* **Structure:**
  ```
  [<Context>] <Type>: <Description in imperative mood>
  ```

### 3.1 Components

* **`[<Context>]`**: The module, feature area, or tool affected, in PascalCase or Title Case enclosed in brackets.
  * Examples: `[Notification]`, `[Scanner]`, `[Compendium]`, `[Tuist]`, `[Database]`, `[Version]`, `[CI]`.
* **`<Type>`**: The action type matching the nature of the change:
  * `Feature:` New feature or behavior.
  * `Bugfix:` Bug or defect fix.
  * `Config:` Configuration or tooling modification.
  * `Refactor:` Code refactoring or restructuring without functional addition.
  * `Bump:` Version bump.
  * `Test:` Adding or modifying unit/instrumented tests.
  * `Docs:` Documentation updates.
* **`<Description>`**: Concise explanation of what was done, starting with lowercase or capitalized imperative verb.

### 3.2 Commit Examples

* `[Notification] Feature: implement FCM background receiver service`
* `[Tuist] Config: create tuist directive files`
* `[Scanner] Bugfix: correct text recognition box orientation on rotated frames`
* `[Database] Refactor: optimize Room query for card search filtering`
* `[Version] Bump: increment version to 1.0.1`

---

## 4. Version Management & Changelog

* Version numbering follows Semantic Versioning (`MAJOR.MINOR.PATCH`).
* Version definition is centralized in `version.properties`.
* **Changelog Policy (`CHANGELOG.md`):**
  * All merges into `develop` must have their changes documented under the `## [Unreleased] (develop)` section of `CHANGELOG.md`.
  * When preparing a release from `develop` to `main`, the `[Unreleased]` section is tagged with the new release version and date.
* Before merging `develop` into `main`:
  1. A dedicated `bump/<version>` branch is created from `develop`.
  2. `version.properties` is updated (incrementing `versionCode` and `versionName`).
  3. `CHANGELOG.md` is updated to seal the new release version.
  4. The `bump/` branch is merged into `develop` via PR.
  5. `develop` is then merged into `main` via PR.
