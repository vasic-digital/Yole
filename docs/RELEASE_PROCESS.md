# Release Process

<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

## Version Numbering

Yole follows [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Breaking API changes, major architecture changes
- **MINOR** (0.X.0): New features, format additions, platform enhancements
- **PATCH** (0.0.X): Bug fixes, security patches, documentation updates

Pre-release suffixes: `-alpha.N`, `-beta.N`, `-rc.N`

## Pre-Release Checklist

Before creating a release tag, verify ALL of the following:

### 1. Tests Pass

```bash
# Primary test suite (6,695+ tests)
./gradlew :shared:desktopTest

# Android tests (if Android SDK available)
./gradlew :androidApp:testDebugUnitTest

# Challenge framework
./gradlew runChallenges
```

### 2. Security Scans Clean

```bash
# Run full security scan
./scripts/run_security_scan.sh

# Detekt static analysis
./gradlew detekt

# OWASP dependency check
./gradlew dependencyCheckAnalyze
```

### 3. Coverage Meets Threshold

```bash
# Generate coverage report (must be >= 70%)
./gradlew koverXmlReport koverHtmlReport
```

### 4. Documentation Current

- [ ] CHANGELOG.md updated with release notes
- [ ] Version constants updated in source
- [ ] Website pages reflect new version
- [ ] Video course scripts reference accurate counts

## Creating a Release

### Step 1: Update Version

Update version in these files:
- `sonar-project.properties` (`sonar.projectVersion`)
- `androidApp/build.gradle.kts` (`versionName`, `versionCode`)
- `README.md` (`Current Version`)

### Step 2: Update CHANGELOG

Add release section to `CHANGELOG.md`:

```markdown
## [vX.Y.Z] - YYYY-MM-DD

### Added
- ...

### Changed
- ...

### Fixed
- ...
```

### Step 3: Commit and Tag

```bash
git add -A
git commit -m "release: prepare vX.Y.Z"
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin master --tags
```

### Step 4: GitHub Release

The release workflow triggers automatically on `v*` tags:

1. Builds Android release APK
2. Builds Desktop JAR
3. Creates GitHub Release with auto-generated notes
4. Attaches build artifacts

For manual trigger: Actions > Release > Run workflow

### Step 5: Post-Release

- [ ] Verify GitHub Release artifacts are downloadable
- [ ] Update F-Droid metadata if needed
- [ ] Announce in GitHub Discussions
- [ ] Start next development cycle (bump version to next `-SNAPSHOT`)

## Container-Based Release Build

For reproducible release builds:

```bash
# Build release artifacts in container
docker compose run --rm build ./docker/scripts/build.sh

# Artifacts in releases/ directory
ls -la releases/android/ releases/desktop/ releases/web/
```

## Hotfix Process

For critical bug fixes on a released version:

1. Create branch from tag: `git checkout -b hotfix/vX.Y.Z+1 vX.Y.Z`
2. Apply fix with tests
3. Follow standard release process from Step 1
4. Merge hotfix back to master
