# Yole Build & Release Process

This document describes the complete build process using Docker/Podman.

## Prerequisites

- Docker or Podman installed
- At least 8GB RAM available
- 20GB disk space

## Quick Start

### 1. Start Build Environment

```bash
# Build and start containers
docker compose up -d build

# Or with Podman
podman-compose up -d build
```

### 2. Run Complete Build

```bash
# Inside container or locally
./docker/scripts/build.sh
```

This will:
- Clean previous builds
- Run all tests (100% pass required)
- Generate coverage reports (100% coverage required)
- Build Android APK (signed)
- Build Desktop JAR
- Build Web/WASM
- Run security scans

### 3. Run Tests Only

```bash
./docker/scripts/test-all.sh
```

## Docker Services

### Build Container
The main build environment includes:
- Ubuntu 22.04
- Java 17
- Gradle 8.11.1
- Android SDK 35
- Python 3

### Android Emulator
- Pre-configured Android 35 emulator
- Automatic startup on test runs

### Optional Services (--profile full)
```bash
docker compose up -d --profile full
```

- **SonarQube** (port 9000): Code quality analysis
- **Dependency-Track** (port 8080): Vulnerability management
- **OWASP ZAP** (port 8090): Security testing

## Signing Keys

Keys are generated in `docker/keys/`:
- `yole.keystore` - Android signing
- `signing.properties` - Build configuration

Default credentials (for development):
```
storePassword: yole123
keyPassword: yole123
```

## Build Outputs

All outputs go to `releases/`:
- `*.apk` - Android apps
- `*.jar` - Desktop apps
- `*.js`, `*.wasm` - Web builds
- `test-results/` - Test reports
- `coverage/` - Coverage reports

## Test Coverage Requirements

| Metric | Required |
|--------|----------|
| Line Coverage | 100% |
| Branch Coverage | 100% |
| Instruction Coverage | 100% |
| Test Pass Rate | 100% |

## Running Specific Tests

```bash
# Single test class
./gradlew test --tests "digital.vasic.yole.format.MarkdownParserTest"

# Format tests only
./gradlew test --tests "digital.vasic.yole.format.*"

# Network tests
./gradlew test --tests "digital.vasic.yole.network.*"

# With Robolectric
./gradlew :androidApp:testDebugUnitTest
```

## CI/CD Alternative

For GitHub Actions (if re-enabled):
```yaml
./gradlew test
./gradlew koverXmlReport
./gradlew :androidApp:assembleRelease
```

## Troubleshooting

### Emulator not starting
```bash
# Manual start
docker compose up -d android-emulator
```

### Coverage too low
- Check test reports in `releases/test-results/`
- Add missing tests
- Verify no untested code paths

### Build failures
- Check `releases/logs/` for detailed logs
- Verify all environment variables set
- Ensure sufficient disk space

## Security Scanning

### Local SonarQube
```bash
docker compose up -d sonarqube
# Access at http://localhost:9000
# Default: admin/admin
```

### OWASP Dependency Check
```bash
./gradlew dependencyCheckAnalyze
# Report at build/reports/dependency-check-report.html
```

## Release Process

1. Update version in `gradle.properties`
2. Run `./docker/scripts/build.sh`
3. Verify all artifacts in `releases/`
4. Create GitHub release
5. Upload artifacts
6. Create release notes

## Support

- Issues: GitHub Issues
- Discussions: GitHub Discussions
- Docs: docs/
