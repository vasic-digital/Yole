# Yole - Development Guide

## Build Commands
- **Android**: `./gradlew :androidApp:assembleDebug` or `make build`
- **Desktop**: `./gradlew :desktopApp:run` or `make desktop`
- **Web**: `./gradlew :webApp:wasmJsBrowserRun` or `make web`
- **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode
- **Lint**: `./gradlew lint` or `make lint`
- **Test**: `./gradlew test` or `make test`
- **Single test**: `./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"`
- **Clean**: `./gradlew clean` or `make clean`

## Code Coverage
- **HTML Report**: `./gradlew koverHtmlReport` → `build/reports/kover/html/index.html`
- **XML Report**: `./gradlew koverXmlReport` → `build/reports/kover/report.xml`
- **Run with Coverage**: `./gradlew test koverHtmlReport`
- **Target**: 100% coverage across all modules

## API Documentation
- **Generate**: `./gradlew :shared:dokkaHtml` → `shared/build/dokka/html/`
- **Publish**: `mkdir -p docs/api && cp -r shared/build/dokka/html/* docs/api/`

## Test Generation
- **Generate Tests**: `./scripts/generate_format_tests.sh <format-name> <extension>`
- **Example**: `./scripts/generate_format_tests.sh Markdown .md`
- **Templates**: ParserTest, IntegrationTest, MockKExample, KotestPropertyTest, UITest, SnapshotTest

## Code Style Guidelines
- **Language**: Kotlin with Java 8+ compatibility
- **Packages**: `digital.vasic.yole.*` for app code, `net.gsantner.opoc.*` for utilities
- **Naming**: CamelCase (classes), lowerCamelCase (methods/vars), UPPER_SNAKE_CASE (constants)
- **Imports**: Group standard, third-party, then project imports
- **Error Handling**: try-catch blocks, appropriate logging, null checks
- **Testing**: JUnit 4/5 + AssertJ, test classes end with `Tests` or `Test`
- **Headers**: SPDX license header + maintainer info (Apache 2.0, CC0-1.0, or Unlicense)
- **Dependencies**: Use version catalog (`libs.versions.toml`)