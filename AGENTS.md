# Instructions

This file provides guidance to coding agents when working with code in this repository.

## General Approach

- Analyze assumptions and provide counterpoints — prioritize truth over agreement.
- Treat me as an expert Android developer. Give overviews, not tutorials.
- Always give an overview of the solution before diving into implementation (unless explicitly asked to implement right away).
- Do not add comments for trivial logic or when the name is descriptive enough.
- **Skills are mandatory workflows** — when a skill is triggered (via `/command` or trigger phrases), follow its SKILL.md phases strictly. NEVER skip approval gates. Present plan and STOP before writing code.

## Architecture Overview

**Simplenote for Android** is a note-taking app using Simperium for real-time sync. Built with Kotlin/Java, Dagger Hilt for DI, and Android Architecture Components (ViewModel, LiveData, Coroutines).

**Stack**: Kotlin + Java, Android SDK (min 23 / target 35 / compile 36), Dagger Hilt 2.57, Simperium sync, Sentry crash reporting

### Module Structure

```
:Simplenote       — Main application (com.automattic.simplenote)
:Wear             — Android Wear companion app
:PasscodeLock     — Passcode lock library (org.wordpress.passcodelock)
```

### Code Navigation (package: `com.automattic.simplenote`)

| To find...                        | Look in...                                                       |
|-----------------------------------|------------------------------------------------------------------|
| Activities & Fragments            | `Simplenote/src/main/java/.../simplenote/` (root package)        |
| Authentication / login flow       | `authentication/` (SignInFragment, MagicLink*, NewCredentials*)   |
| Dependency injection (Hilt)       | `di/` (AppModule, etc.)                                          |
| Data models                       | `models/` (Note, Tag, etc.)                                      |
| API / networking                  | `networking/` (SimpleHttp, etc.)                                 |
| Repository layer                  | `repositories/` (Simperium-backed repos)                         |
| Business logic                    | `usecases/`                                                      |
| ViewModels                        | `viewmodels/`                                                    |
| Utility functions                 | `utils/`                                                         |
| Analytics / tracking              | `analytics/`                                                     |
| Custom views / widgets            | `widgets/`                                                       |
| Unit tests                        | `Simplenote/src/test/java/.../simplenote/`                       |
| Instrumented tests                | `Simplenote/src/androidTest/java/.../simplenote/`                |
| Release config / versioning       | `version.properties` (root)                                      |
| Fastlane / release automation     | `fastlane/`                                                      |
| CI pipelines                      | `.buildkite/pipeline.yml`, `.buildkite/release-pipelines/`       |

### Key Architectural Patterns

1. **MVVM with Use Cases** — ViewModels consume use cases and repositories. LiveData for UI observation.
2. **Repository pattern** — Simperium buckets wrapped in repository classes for data access.
3. **Dagger Hilt DI** — Constructor injection throughout; modules in `di/`.
4. **Simperium sync** — Real-time sync via Simperium SDK. Notes and Tags stored as Simperium `Bucket` objects.
5. **Magic link authentication** — Passwordless auth flow alongside traditional email/password.

### Common Pitfalls

Based on recent merged PRs:
- **Android version-specific crashes** — Widget and activity lifecycle issues on newer Android versions (e.g., Android 16). Test across API levels.
- **Release branch merge conflicts** — `release/X.Y` branches must be merged back to `trunk` carefully. Changelog files often conflict.
- **Fastlane variable scoping** — Ruby variable references in Fastlane lanes can silently be nil if mistyped.

## Git Operations (CRITICAL)
- **NEVER commit without explicit permission**
- **NEVER push without explicit permission**
- **NEVER create a PR without explicit permission**
- When asked to "fix" or "update" something, that does NOT imply permission to commit/push
- Always wait for explicit "commit", "push", or "create PR" commands

## Build Commands

```bash
# Build all modules
./gradlew assembleDebug

# Build individual modules
./gradlew :Simplenote:assembleDebug
./gradlew :Wear:assembleDebug
./gradlew :PasscodeLock:assembleDebug

# Install on device
./gradlew :Simplenote:installDebug

# Lint (all modules)
./gradlew lintDebug

# Lint (individual modules)
./gradlew :Simplenote:lintDebug
./gradlew :Wear:lintDebug

# Unit tests (all modules)
./gradlew --stacktrace testDebugUnitTest

# Unit tests (Simplenote only — where most tests live)
./gradlew :Simplenote:testDebugUnitTest

# Specific test class
./gradlew :Simplenote:testDebugUnitTest --tests "com.automattic.simplenote.viewmodels.TagsViewModelTest" --info

# Specific test method
./gradlew :Simplenote:testDebugUnitTest --tests "com.automattic.simplenote.viewmodels.TagsViewModelTest.testMethodName" --info
```

### Build Configuration

- Versions defined in root `build.gradle` (Kotlin, AGP, Hilt, Sentry versions) and `version.properties` (app version)
- Java 11 target (`kotlinOptions.jvmTarget = 11`)
- ViewBinding enabled
- Max line length: 120 characters (`.editorconfig`)
- No ktlint or detekt Gradle plugins — lint checking via Android Lint only

## GitHub Commands

```bash
PAGER=cat gh pr list
PAGER=cat gh pr view <NUMBER>
PAGER=cat gh pr view <NUMBER> --comments
PAGER=cat gh pr diff <NUMBER>
```

## PR Conventions

- Default branch: **`trunk`**
- Branch naming: `username/kebab-case-description`
- Title format: `[Category] Description` for categorized work (e.g., `[Tooling]`, `[Analysis]`); plain description for app changes
- Labels: `[Type] Bug`, `[Type] UX`, `[Type] Debt`, `tooling`, `Core`, `crash`, `[priority] high`
- Issue tracking: Linear (`SIMPL-`, `AINFRA-`) and GitHub Issues
- PRs target `trunk` (except hotfixes targeting `release/X.Y`)
- Small, focused PRs preferred (1-7 files, under 50 lines)

## PR Template

```markdown
### Fix
<description of what was fixed/added, with links/screenshots if applicable>

### Test
1. Step-by-step testing instructions

### Review
<instructions for reviewers>

### Release
<release notes statement, or note that release notes are not needed>
```

## Skills

| Skill       | Trigger phrases                                                                                  |
|-------------|--------------------------------------------------------------------------------------------------|
| `implement` | "implement", "fix this", "work on this", "build this", "add feature", any implementation request |

### Skill execution rules (CRITICAL)
- When a skill is triggered (via `/command` or matching trigger phrases), you MUST follow the skill's phases **sequentially and completely**
- **NEVER skip a phase that requires user approval** — if a phase says "STOP and wait for approval", you must STOP
- The skill's SKILL.md is your instruction set — treat each phase gate as a hard blocker, not a suggestion
- Do NOT write production code before the plan phase is explicitly approved by the user
