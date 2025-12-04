# GitHub Actions CI/CD Workflows

This directory contains all CI/CD workflows for the Yole project. These workflows automate testing, building, linting, and documentation generation.

## Workflow Overview

### 1. `build-android-project.yml` - Main Build Workflow

**Trigger**: Push, Pull Request
**Purpose**: Build Android APK and verify project compiles
**Duration**: ~10-15 minutes

**Jobs**:
- Build Android debug APK using `make clean all`
- Upload APK artifacts (retained for 5 days)
- Upload all dist files

**Artifacts**:
- `all` - Complete dist directory
- `android-apk` - Android APK files only

**Usage**: Legacy build workflow, primarily for APK generation.

---

### 2. `test-and-coverage.yml` - Test Execution & Coverage

**Trigger**: Push (master/main/develop), Pull Request, Manual
**Purpose**: Run comprehensive test suite and generate coverage reports
**Duration**: ~20-30 minutes

**Jobs**:
1. **test-shared** - Test shared Kotlin Multiplatform module
   - Runs: `./gradlew :shared:testDebugUnitTest`
   - Generates Kover coverage report
   - Uploads to Codecov with `shared` flag

2. **test-android** - Test Android-specific code
   - Runs: `./gradlew :androidApp:testDebugUnitTest`
   - Uploads test reports

3. **test-desktop** - Test Desktop module
   - Runs: `./gradlew :desktopApp:test`
   - Uploads test reports

4. **coverage-report** - Combined coverage analysis
   - Generates combined Kover report
   - Parses coverage percentage
   - Uploads to Codecov with `combined` flag

5. **test-summary** - Test execution summary
   - Creates summary table in GitHub Actions UI
   - Fails if any test job failed

**Artifacts**:
- `test-reports-shared` - Shared module test results (14 days)
- `test-reports-android` - Android test results (14 days)
- `test-reports-desktop` - Desktop test results (14 days)
- `coverage-reports` - Combined coverage HTML/XML (30 days)

**Concurrency**: Cancels previous runs on same branch

---

### 3. `lint-and-docs.yml` - Lint & Documentation

**Trigger**: Push (master/main/develop), Pull Request, Manual
**Purpose**: Run code quality checks and generate API documentation
**Duration**: ~15-20 minutes

**Jobs**:
1. **lint-kotlin** - Detekt (Kotlin static analysis)
   - Runs: `./gradlew detekt`
   - Uploads Detekt reports

2. **lint-android** - Android Lint
   - Runs: `./gradlew :androidApp:lintDebug`
   - Parses error/warning counts
   - Uploads lint HTML/XML reports

3. **generate-docs** - Dokka API documentation
   - Runs: `./gradlew :shared:dokkaHtml`
   - Publishes to GitHub Pages (master branch only)
   - URL: `https://<username>.github.io/Yole/api/`

4. **check-formatting** - KtLint code style
   - Runs: `./gradlew ktlintCheck`
   - Uploads formatting reports

5. **dependency-check** - Dependency updates
   - Runs: `./gradlew dependencyUpdates`
   - Identifies outdated dependencies

6. **lint-summary** - Combined lint results
   - Creates summary table
   - Fails if documentation generation failed

**Artifacts**:
- `lint-reports-detekt` - Detekt static analysis (14 days)
- `lint-reports-android` - Android Lint results (14 days)
- `api-documentation` - Dokka HTML docs (30 days)
- `formatting-reports` - KtLint results (7 days)
- `dependency-reports` - Dependency update report (7 days)

**GitHub Pages**: Automatically publishes API docs on master branch pushes.

---

### 4. `pr-validation.yml` - Pull Request Validation

**Trigger**: Pull Request (opened, synchronized, reopened, ready_for_review)
**Purpose**: Comprehensive validation before merging PRs
**Duration**: ~25-35 minutes

**Jobs**:
1. **pr-info** - Display PR information
   - Shows title, author, commits, file changes, additions/deletions

2. **build-check** - Verify builds
   - Builds Android Debug APK
   - Builds shared module
   - Attempts Desktop build (non-blocking)

3. **test-check** - Run all tests
   - Executes full test suite
   - Generates coverage report
   - Uploads to Codecov with PR-specific flag

4. **lint-check** - Code quality
   - Runs Android Lint
   - Runs Detekt
   - Runs KtLint formatting check

5. **size-check** - APK size comparison
   - Builds base branch APK
   - Builds PR branch APK
   - Compares sizes and calculates difference
   - Warns if size increases >1 MB

6. **security-check** - Dependency vulnerability scan
   - Runs OWASP dependency check
   - Uploads security reports

7. **validation-summary** - Overall PR status
   - Creates summary table
   - Posts comment to PR with results
   - Fails if build or tests failed

**Artifacts**:
- `pr-test-reports` - Test results (7 days)
- `pr-lint-reports` - Lint results (7 days)
- `pr-security-reports` - Security scan (7 days)

**PR Comments**: Automatically posts validation status to PR.

**Draft PRs**: Skipped for draft pull requests.

---

## Configuration Files

### `codecov.yml`

Codecov configuration for coverage reporting:

- **Target Coverage**: 80% project, 70% patch
- **Precision**: 2 decimal places
- **Flags**: `shared`, `android`, `desktop`, `combined`
- **Ignore Patterns**: Test files, build artifacts, templates, docs

**Key Settings**:
```yaml
coverage:
  status:
    project:
      target: 80%
    patch:
      target: 70%
```

---

## Secrets Required

The following secrets must be configured in GitHub repository settings:

| Secret | Purpose | Required For |
|--------|---------|--------------|
| `CODECOV_TOKEN` | Upload coverage to Codecov | test-and-coverage.yml, pr-validation.yml |
| `GITHUB_TOKEN` | Publish to GitHub Pages | lint-and-docs.yml (auto-provided) |

**Setup**: Settings → Secrets and variables → Actions → New repository secret

---

## Badges

Add these badges to your README.md:

```markdown
![CI Build](https://github.com/<username>/Yole/workflows/CI/badge.svg)
![Tests](https://github.com/<username>/Yole/workflows/Tests%20%26%20Coverage/badge.svg)
![Lint](https://github.com/<username>/Yole/workflows/Lint%20%26%20Docs/badge.svg)
[![codecov](https://codecov.io/gh/<username>/Yole/branch/master/graph/badge.svg)](https://codecov.io/gh/<username>/Yole)
![Code Coverage](https://img.shields.io/badge/coverage-15%25-red.svg)
```

---

## Running Workflows Manually

All workflows except `build-android-project.yml` support manual triggering via `workflow_dispatch`.

**To run manually**:
1. Go to **Actions** tab in GitHub
2. Select workflow from left sidebar
3. Click **Run workflow** button
4. Choose branch and click **Run workflow**

---

## Workflow Optimization

### Caching Strategy

All workflows use Gradle dependency caching:

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
```

**Benefits**: ~3-5 minutes faster builds after first run.

### Concurrency Control

Workflows use concurrency groups to cancel redundant runs:

```yaml
concurrency:
  group: test-${{ github.ref }}
  cancel-in-progress: true
```

**Benefits**: Saves CI minutes, faster feedback on latest commits.

### Conditional Execution

- **Path Ignoring**: `test-and-coverage.yml` skips on documentation-only changes
- **Draft PRs**: `pr-validation.yml` skips draft pull requests
- **Branch-Specific**: `lint-and-docs.yml` only publishes docs on master

---

## Troubleshooting

### Test Failures

1. Check test report artifacts
2. Run locally: `./gradlew test --info --stacktrace`
3. Review specific test logs in Actions UI

### Coverage Upload Failures

- Verify `CODECOV_TOKEN` is set correctly
- Check `codecov.yml` syntax
- Review Codecov dashboard for errors

### Build Failures

1. Check Gradle build logs
2. Verify Java version (21 required)
3. Run locally: `./gradlew clean build --info`
4. Check for dependency resolution issues

### Lint Failures

- Review lint report artifacts
- Run locally: `./gradlew lint detekt ktlintCheck`
- Fix issues or add suppressions if justified

### APK Size Increase

- Review size-check job output
- Identify large dependencies added
- Consider ProGuard/R8 optimization
- Check for accidentally included resources

---

## Adding New Workflows

When creating new workflows:

1. **Naming**: Use descriptive names (e.g., `performance-tests.yml`)
2. **Triggers**: Be specific with paths and branches
3. **Timeouts**: Set reasonable timeouts (default 360 min)
4. **Artifacts**: Retain only necessary files
5. **Concurrency**: Add concurrency groups
6. **Summary**: Use `$GITHUB_STEP_SUMMARY` for readability
7. **Documentation**: Update this README

**Template**:
```yaml
name: "New Workflow"

on:
  push:
    branches: [master, main]
    paths-ignore: ['docs/**', '**.md']

concurrency:
  group: new-workflow-${{ github.ref }}
  cancel-in-progress: true

jobs:
  example:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    steps:
      # ... workflow steps
```

---

## Performance Metrics

Typical workflow execution times:

| Workflow | Duration | Runs On |
|----------|----------|---------|
| build-android-project | ~10-15 min | Every push/PR |
| test-and-coverage | ~20-30 min | Push to main branches, PRs |
| lint-and-docs | ~15-20 min | Push to main branches, PRs |
| pr-validation | ~25-35 min | Pull requests only |

**Total CI time per PR**: ~60-85 minutes (parallelized to ~35 min wall time)

---

## Future Enhancements

Planned improvements:

- [ ] iOS build workflow (when iOS module is active)
- [ ] Web build workflow (when Web module is ready)
- [ ] Performance regression testing
- [ ] Snapshot testing for UI components
- [ ] Automated release workflow with changelogs
- [ ] Nightly builds for bleeding-edge testing
- [ ] Matrix testing across multiple Java versions
- [ ] Integration tests on real devices (Firebase Test Lab)

---

## Support

For issues with CI workflows:

1. Check workflow logs in Actions tab
2. Review this documentation
3. Open issue: [GitHub Issues](https://github.com/vasic-digital/Yole/issues)
4. Discussion: [GitHub Discussions](https://github.com/vasic-digital/Yole/discussions)

---

*Last Updated: November 2025*
*Yole CI/CD Infrastructure v1.0*
