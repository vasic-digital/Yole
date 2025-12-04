# Yole - Development Guide

## Build Commands
- **Android Build**: `./gradlew :androidApp:assembleDebug` or `make build`
- **Desktop Build**: `./gradlew :desktopApp:run` or `make desktop`
- **Web Build**: `./gradlew :webApp:wasmJsBrowserRun` or `make web`
- **iOS Build**: Open `iosApp/iosApp.xcodeproj` in Xcode
- **Lint**: `./gradlew lint` or `make lint`
- **Test**: `./gradlew test` or `make test`
- **Single test**: `./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"`
- **Clean**: `./gradlew clean` or `make clean`

## Code Coverage (Kover)
- **HTML Report**: `./gradlew koverHtmlReport` → View: `open build/reports/kover/html/index.html`
- **XML Report**: `./gradlew koverXmlReport` → Output: `build/reports/kover/report.xml`
- **Run Tests with Coverage**: `./gradlew test koverHtmlReport`
- **Current Baseline**: ~15% estimated (goal: >80%)
- **Note**: Reports are generated per module. Use `koverHtmlReport` for combined coverage.

## API Documentation (Dokka)
- **Generate Docs**: `./gradlew :shared:dokkaHtml` → Output: `shared/build/dokka/html/`
- **View Docs**: `open shared/build/dokka/html/index.html`
- **Publish to Website**: `mkdir -p docs/api && cp -r shared/build/dokka/html/* docs/api/`
- **Key Classes**: FormatRegistry, TextFormat, Document (all in `shared` module)
- **Note**: FormatRegistry and TextFormat already have comprehensive KDoc documentation

## Test Generation
- **Generate Tests**: `./scripts/generate_format_tests.sh <format-name> <extension>`
- **Example**: `./scripts/generate_format_tests.sh Markdown .md`
- **Dry Run**: `./scripts/generate_format_tests.sh "Org Mode" .org --dry-run`
- **Custom Options**: `--package <name>`, `--class <name>`, `--templates <list>`
- **Available Templates**: ParserTest, IntegrationTest, MockKExample, KotestPropertyTest
- **Output**: `shared/src/commonTest/kotlin/digital/vasic/yole/format/<package>/`
- **Documentation**: See `docs/TESTING_GUIDE.md` for complete testing documentation
- **Note**: Generated tests use placeholder content - customize with format-specific samples

## CI/CD Workflows

### GitHub Actions
- **Main Build**: `.github/workflows/build-android-project.yml` - Builds Android APK on every push/PR
- **Tests & Coverage**: `.github/workflows/test-and-coverage.yml` - Runs all tests + Kover coverage
- **Lint & Docs**: `.github/workflows/lint-and-docs.yml` - Code quality checks + Dokka generation
- **PR Validation**: `.github/workflows/pr-validation.yml` - Comprehensive PR checks before merge

### Workflow Triggers
- **Push to master/main/develop**: Triggers tests, coverage, lint, and docs workflows
- **Pull Requests**: Triggers all workflows including PR validation (build, test, lint, size check, security)
- **Manual**: All workflows except main build support manual triggering via Actions tab

### CI Commands (Run Locally)
```bash
# Simulate CI build
./gradlew clean build

# Run CI test suite
./gradlew test koverXmlReport

# Run CI lint checks
./gradlew lintDebug detekt ktlintCheck

# Generate CI documentation
./gradlew :shared:dokkaHtml
```

### Artifacts & Reports
- **Test Reports**: Available in Actions → Workflow → Artifacts (14-30 days retention)
- **Coverage Reports**: Uploaded to Codecov + workflow artifacts
- **Lint Reports**: Android Lint, Detekt, KtLint results in artifacts
- **API Docs**: Auto-published to GitHub Pages on master branch

### Configuration Files
- **codecov.yml**: Coverage reporting configuration (target: 80% project, 70% patch)
- **.github/workflows/README.md**: Complete CI/CD documentation

### Required Secrets
- `CODECOV_TOKEN` - For uploading coverage reports (get from codecov.io)
- `GITHUB_TOKEN` - Auto-provided for GitHub Pages publishing

### CI Status Badges
```markdown
![CI Build](https://github.com/vasic-digital/Yole/workflows/CI/badge.svg)
![Tests](https://github.com/vasic-digital/Yole/workflows/Tests%20%26%20Coverage/badge.svg)
![Lint](https://github.com/vasic-digital/Yole/workflows/Lint%20%26%20Docs/badge.svg)
```

## Code Style Guidelines
- **Language**: Kotlin with Java 8+ compatibility, platform-specific languages
- **Package structure**: `digital.vasic.yole.*` for app code, `net.gsantner.opoc.*` for shared utilities
- **Naming**: CamelCase for classes, lowerCamelCase for methods/variables, UPPER_SNAKE_CASE for constants
- **Imports**: Group standard libraries, third-party, then project imports
- **Error handling**: Use try-catch blocks, log errors appropriately, handle null checks
- **Testing**: Use JUnit 4/5 with AssertJ assertions, test classes end with `Tests` or `Test`
- **File headers**: Include SPDX license header and maintainer info (Apache 2.0, CC0-1.0, or Unlicense)
- **Version**: Use version catalog (libs.versions.toml) for dependency management