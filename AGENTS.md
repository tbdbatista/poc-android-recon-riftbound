# Project Guidelines & Development Workflow (Steering Rules)

This document establishes the official development rules, branch lifecycle, commit conventions, and versioning standards for this project. All contributors and AI agents MUST strictly comply with these rules.

---

## 1. Branch Strategy & Hierarchy

```
[ Development Branches ] (feature/*, config/*, refactor/*, dev-bugfix/*)
         │
         ▼ (PR)
     [ epic/* ] ──(PR)──► [ develop ] ──(PR)──► [ main ] (Release / Tag)
                               ▲                   │
                               │ (Merge Back PR)   ▼
                    [ Production Bugfix ] ◄───────┘ (Branch from main)
                    (bugfix/* for hotfix)
```

### 1.1 Protected Branches (No Direct Commits)

* **`main`**:
  * Represents production-ready, stable releases.
  * **STRICT RULE:** Direct commits are NEVER allowed.
  * Merges into `main` occur exclusively via Pull Requests from:
    1. `develop` (Standard Releases after sealing a version via `bump/`).
    2. `bugfix/<context>-<description>` (Critical Production Hotfixes for already released versions).
  * Every merge into `main` triggers automated CI release tagging.

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

## 2. Working Branches & Bugfix Scenarios

All active development must occur in dedicated working branches.

### 2.1 Branch Types

| Branch Type | Description / When to Use |
| :--- | :--- |
| `feature/` | Development of new features or capabilities. Target: `develop`. |
| `bugfix/` | Resolution of bugs in production releases (hotfix from `main`) or active development. |
| `config/` | Exclusively for project configuration files (e.g., Gradle, build tools, CI/CD, linters, steering rules). Target: `develop`. |
| `refactor/` | Code modifications that do not add new functionality or alter public behavior. Target: `develop`. |
| `bump/` | Exclusively for updating version-control files (`version.properties`, `CHANGELOG.md`) prior to merging into `main`. Target: `develop`. |

### 2.2 Bugfix Workflows: Production vs. Development

#### Scenario A: Production Bug (Bug reported in a released version on `main`)
When a defect is discovered in a closed, production-ready version already merged into `main` (e.g. v0.5.0):
1. **Branching Origin:** Create a dedicated branch `bugfix/<context>-<description>` **directly from `main`**.
2. **Version Bump:** Increment the **PATCH** version in `version.properties` (e.g., `0.5.0` → `0.5.1`) and increment `versionCode`.
3. **Changelog:** Add a dedicated release section in `CHANGELOG.md` (e.g. `## [0.5.1] - YYYY-MM-DD`) with `### Fixed`.
4. **Pull Request to `main`:** Open a PR targeting `main` directly (`gh pr create --base main --head bugfix/...`).
5. **Merge Back to `develop`:** After merging into `main`, immediately merge back (or open a sync PR) from `main` (or the bugfix branch) into `develop` to ensure day-to-day development incorporates the fix.

#### Scenario B: Development Bug (Bug found during ongoing development, not yet released)
When a bug is identified in unreleased code existing only on `develop`:
1. **Branching Origin:** Create a branch from `develop`.
2. **Naming:** May be named `refactor/<description>` or `bugfix/<description>`.
3. **Pull Request Target:** Target `develop` directly.
4. **Versioning:** Follows the normal release cycle; no immediate patch bump is required.

### 2.3 Branch Naming Standards

* **Language:** Branch names must **ALWAYS be written in English**.
* **Pattern:** `<type>/<context>-<description>[-part<N>]`
  * Use hyphen-separated lowercase words (`kebab-case`).
  * If a large feature is divided into sequential parts, suffix with `-part1`, `-part2`, etc.

**Examples:**
* `bugfix/card-mockups-webp-urls` (Production Hotfix from `main`)
* `feature/notification-push-service-setup-part1` (Feature from `develop`)
* `config/steering-production-hotfix-rules` (Config from `develop`)
* `refactor/database-card-dao-queries` (Refactor from `develop`)
* `bump/release-v0-5-0` (Bump from `develop`)

---

## 3. Commit Message Standards

* **Language:** Commit messages must **ALWAYS be written in English**.
* **Structure:**
  ```
  [<Context>] <Type>: <Description in imperative mood>
  ```

### 3.1 Components

* **`[<Context>]`**: The module, feature area, or tool affected, in PascalCase or Title Case enclosed in brackets.
  * Examples: `[Notification]`, `[Scanner]`, `[Compendium]`, `[Database]`, `[Version]`, `[CI]`, `[Steering]`.
* **`<Type>`**: The action type matching the nature of the change:
  * `Feature:` New feature or behavior.
  * `Bugfix:` Bug or defect fix.
  * `Config:` Configuration, tooling, or steering modification.
  * `Refactor:` Code refactoring or restructuring without functional addition.
  * `Bump:` Version bump.
  * `Test:` Adding or modifying unit/instrumented tests.
  * `Docs:` Documentation updates.
* **`<Description>`**: Concise explanation of what was done, starting with lowercase or capitalized imperative verb.

### 3.2 Commit Examples

* `[Assets] Bugfix: point all_cards.json imageUrl to webp extension`
* `[Steering] Config: document production bugfix and hotfix merge-back workflow`
* `[Scanner] Feature: add undo last deletion button and descriptive button labels`
* `[Database] Refactor: optimize Room query for card search filtering`
* `[Version] Bump: increment version to 0.5.1`

---

## 4. Version Management & Changelog

* Version numbering follows Semantic Versioning (`MAJOR.MINOR.PATCH`).
* Version definition is centralized in `version.properties`.
* **Changelog Policy (`CHANGELOG.md`):**
  * All merges into `develop` must have their changes documented under the `## [Unreleased] (develop)` section of `CHANGELOG.md`.
  * Standard releases tag the `[Unreleased]` section with the new release version and date before merging into `main`.
  * Production Hotfixes add their own `## [MAJOR.MINOR.PATCH] - YYYY-MM-DD` section directly to seal the patch.

---

## 5. Pull Request (PR) Standards & Lifecycle

All integrations into `develop` or `main` MUST occur through Pull Requests on GitHub.

### 5.1 PR Target Rules

* **Development PRs** (`feature/*`, `config/*`, `refactor/*`, `bump/*`, dev `bugfix/*`):
  * **Target Base Branch:** `develop` (or active `epic/*` branch).
* **Production Bugfix PRs** (`bugfix/*` created from `main`):
  * **Target Base Branch:** `main`.
* **Release PRs** (`develop` -> `main`):
  * **Target Base Branch:** `main` (opened after `bump/` merges into `develop`).
* **Merge-Back PRs** (`main` -> `develop` or `bugfix/*` -> `develop`):
  * **Target Base Branch:** `develop` (to synchronize hotfix changes).

### 5.2 PR Title Standards

* **Language:** PR titles must **ALWAYS be written in English**.
* **Pattern for Working / Bump / Bugfix Branches:**
  ```
  [<Context>] <Type>: <Description in imperative mood>
  ```
  * Examples:
    * `[Assets] Bugfix: point all_cards.json imageUrl to webp extension`
    * `[Steering] Config: document production hotfix and merge-back workflow`
    * `[Version] Bump: release version 0.5.0`
* **Pattern for Release PRs (`develop` -> `main`):**
  ```
  [Release] Version <MAJOR.MINOR.PATCH>
  ```
  * Example: `[Release] Version 0.5.0`

### 5.3 PR Description Templates

#### 5.3.1 Template for Feature / Config / Refactor PRs:
```markdown
## Summary
<Concise high-level overview explaining the purpose, motivation, and user-facing impact of the PR.>

### Key Changes
1. **<Component/Area>**: <Detailed explanation of what was added, modified, or removed.>
...
n. **Tests & Documentation**:
   - Added / updated unit tests.
   - Updated `CHANGELOG.md` under `## [Unreleased] (develop)`.

### Verification / Testing
- [x] Compilation: Verified `./gradlew assembleDebug` builds cleanly without errors.
- [x] Unit Tests: Verified `./gradlew testDebugUnitTest` passes 100%.
```

#### 5.3.2 Template for Production Bugfix PRs (`bugfix/*` -> `main`):
```markdown
## Summary
Hotfix for production release addressing issue #<IssueNumber>: <Brief description>.
Bumps version to <PATCH_VERSION> (versionCode <codeCount>).

### Root Cause
<Clear explanation of why the defect occurred in production.>

### Key Changes
1. **<Component/Area>**: <Detailed fix applied.>
2. **Version & Changelog**: Bumped version to <PATCH_VERSION> and documented under `## [<PATCH_VERSION>] - YYYY-MM-DD` in `CHANGELOG.md`.

### Verification / Testing
- [x] Compilation: Verified `./gradlew assembleDebug` builds cleanly without errors.
- [x] Unit Tests: Verified `./gradlew testDebugUnitTest` passes 100%.
- [x] Reproduction: Verified defect is resolved on clean install.
```

### 5.4 Pre-PR Checklist (Mandatory for Contributors & Agents)

Before creating a Pull Request, verify:
1. [ ] **Build:** `./gradlew assembleDebug` completes with `BUILD SUCCESSFUL`.
2. [ ] **Tests:** `./gradlew testDebugUnitTest` runs with all tests passing.
3. [ ] **Changelog:** Changes documented in `CHANGELOG.md`.
4. [ ] **Clean Branch:** Working branch is rebased / up-to-date with target base.
5. [ ] **GitHub CLI:** PR created with `gh pr create --base <base> --head <branch> --title "<title>" --body "<body>"`.
