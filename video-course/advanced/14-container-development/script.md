# Module 14: Container-Based Development (6 videos)

## Video 14.1: Docker and Podman Setup (15 min)

### Timestamps
- 0:00 Introduction: why Yole mandates container-based builds and tests
- 1:30 Docker vs. Podman: rootless containers on Linux
- 3:00 Installing Docker Desktop (macOS, Windows) or Podman (Linux)
- 5:00 Verifying the installation: `docker --version` or `podman --version`
- 7:00 Container basics: images, containers, volumes, networks
- 9:00 Why containers matter for Yole: reproducible builds, consistent JDK, Android SDK
- 11:00 ALT Linux specifics: `podman compose` instead of `docker compose`
- 13:00 Troubleshooting: permission errors, storage drivers, SELinux
- 14:30 Summary

### Key Concepts

Yole's `CLAUDE.md` mandates container-based builds:

```
ALL builds and tests MUST be executed inside Docker/Podman containers,
NOT directly on the host machine!
```

This ensures:
- Consistent JDK version (JDK 11 in container; desktop app needs JDK 21)
- Android SDK availability without local installation
- Reproducible builds across developer machines and CI
- Isolation from host system configuration

### Exercises
1. **Install and verify** -- Install Docker or Podman on your machine and run `docker run hello-world` to verify the installation works.
2. **Explore images** -- Pull the `thyrlian/android-sdk:latest` image used by Yole and inspect its contents with `docker run -it thyrlian/android-sdk:latest bash`.

---

## Video 14.2: docker-compose.yml Deep Dive (18 min)

### Timestamps
- 0:00 Overview of Yole's `docker-compose.yml`
- 2:00 The `build` service: Android SDK image, volume mounts, environment variables
- 4:00 Volume mounts explained: workspace, Gradle cache, Maven cache, Android SDK
- 6:00 The `build-alt` service: alternative without host caches
- 8:00 The `sonarqube` service: SonarQube Community Edition on port 9000
- 10:00 The `snyk` service: vulnerability scanning with `SNYK_TOKEN`
- 12:00 The `detekt` service: Kotlin static analysis runner
- 14:00 Docker Compose profiles: `security`, `full`
- 15:30 Named volumes for persistent SonarQube data
- 17:00 Summary

### Code References
- `docker-compose.yml` -- Complete container orchestration configuration

### Key Code Walkthrough

The build service mounts the project workspace and host caches:

```yaml
services:
  build:
    image: docker.io/thyrlian/android-sdk:latest
    container_name: yole-build
    privileged: true
    volumes:
      - .:/workspace                        # Project source
      - ./releases:/workspace/releases      # Build artifacts
      - ${HOME}/.gradle:/root/.gradle       # Gradle cache
      - ${HOME}/.m2:/root/.m2              # Maven cache
      - ${HOME}/Android/Sdk:/opt/android-sdk  # Android SDK
    environment:
      - ANDROID_HOME=/opt/android-sdk
      - ANDROID_SDK_ROOT=/opt/android-sdk
    working_dir: /workspace
```

Profiles let you selectively start security services:

```bash
# Start only the build container
docker compose up build

# Start build + all security scanners
docker compose --profile security up

# Start everything
docker compose --profile full up
```

### Exercises
1. **Customize the compose file** -- Add a new service for running Gradle with a specific JDK 21 image for desktop builds. Mount the same volumes as the `build` service.
2. **Profile exploration** -- Start the `security` profile and verify SonarQube is accessible at `http://localhost:9000`.

---

## Video 14.3: Building in Containers (15 min)

### Timestamps
- 0:00 The build workflow: `docker compose build build` then `docker compose run`
- 2:00 Building the Android app: `./gradlew :androidApp:assembleDebug`
- 4:00 Building the shared module: `./gradlew :shared:compileKotlinDesktop`
- 6:00 Build scripts: `docker/scripts/build.sh`
- 8:00 Gradle cache management: sharing host cache via volume mounts
- 10:00 Handling OOM kills: exit code 137 and Gradle lock file cleanup
- 12:00 Optimizing build times: Gradle daemon, configuration cache, parallel builds
- 14:00 Summary

### Key Commands

```bash
# Build the container image
docker compose build build

# Run Android debug build
docker compose run --rm build ./gradlew :androidApp:assembleDebug

# Run full build script
docker compose run --rm build ./docker/scripts/build.sh

# Clean up after OOM kill (exit code 137)
docker compose run --rm build bash -c \
  "find /root/.gradle/caches -name '*.lock' -delete && ./gradlew clean"
```

### Exercises
1. **First container build** -- Run `docker compose run --rm build ./gradlew :shared:compileKotlinDesktop` and observe the build output. Compare build time with a host build.
2. **Cache analysis** -- Run a build twice and compare the times. The second build should be significantly faster due to Gradle cache hits from the mounted volume.

---

## Video 14.4: Running Tests in Containers (18 min)

### Timestamps
- 0:00 Why tests must run in containers (from CLAUDE.md)
- 2:00 The test runner script: `docker/scripts/test-all.sh`
- 4:00 Running the full test suite: 4,750+ tests across all platforms
- 6:00 Running specific test classes in containers
- 8:00 Desktop tests: `./gradlew :shared:desktopTest` (works with JDK 11)
- 10:00 AGP version mismatch: why androidApp tests use the container's Android SDK
- 12:00 Coverage reports: `./gradlew test koverHtmlReport`
- 14:00 Extracting reports from the container to the host
- 16:00 Debugging test failures in containers: logs, interactive shell
- 17:30 Summary

### Key Commands

```bash
# Run all tests in container
docker compose run --rm build ./docker/scripts/test-all.sh

# Run specific test class
docker compose run --rm build ./gradlew test \
  --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Run desktop tests (avoids AGP version mismatch)
docker compose run --rm build ./gradlew :shared:desktopTest

# Generate coverage report
docker compose run --rm build ./gradlew test koverHtmlReport

# Interactive debugging shell
docker compose run --rm build bash
```

### Exercises
1. **Run the full suite** -- Execute `docker compose run --rm build ./docker/scripts/test-all.sh` and record the total test count and execution time.
2. **Debug a test** -- Start an interactive container shell, run a single failing test with `--info` flag, and examine the full stack trace.
3. **Coverage report** -- Generate a Kover HTML report in the container and copy it to the host for viewing in a browser.

---

## Video 14.5: SonarQube in Containers (15 min)

### Timestamps
- 0:00 What is SonarQube and why use it for code quality
- 2:00 Starting SonarQube: `docker compose --profile security up sonarqube`
- 4:00 Initial setup: default credentials, creating a project
- 6:00 Configuring the Gradle SonarQube plugin
- 8:00 Running analysis: `./gradlew sonar` in the build container
- 10:00 Interpreting results: bugs, vulnerabilities, code smells, coverage
- 12:00 Quality gates: setting thresholds for code quality
- 13:30 Persistent data: SonarQube volumes survive container restarts
- 14:30 Summary

### Key Commands

```bash
# Start SonarQube
docker compose --profile security up -d sonarqube

# Wait for startup (takes ~60 seconds)
# Access at http://localhost:9000 (admin/admin)

# Run analysis
docker compose run --rm build ./gradlew sonar \
  -Dsonar.host.url=http://sonarqube:9000 \
  -Dsonar.token=YOUR_TOKEN
```

### Exercises
1. **First analysis** -- Start SonarQube, create a project, and run a full analysis of the Yole codebase. Review the dashboard for any critical findings.
2. **Quality gate** -- Configure a quality gate that requires at least 60% code coverage and zero critical vulnerabilities. Verify Yole passes.

---

## Video 14.6: Security Scanning Workflow in Containers (15 min)

### Timestamps
- 0:00 The security scanning pipeline overview
- 2:00 Running Snyk in container: `docker compose --profile security run snyk`
- 4:00 Running Detekt: `docker compose --profile security run detekt`
- 6:00 Combining scanners: the `full` profile
- 8:00 Interpreting scanner output: severity levels, false positives
- 10:00 Fixing findings: dependency upgrades, code changes
- 12:00 Automating scans in CI/CD pipelines
- 13:30 Summary

### Key Commands

```bash
# Run all security tools
docker compose --profile security up

# Run Snyk vulnerability scan
docker compose --profile security run --rm snyk test --all-projects

# Run Detekt static analysis
docker compose --profile security run --rm detekt

# Run everything together
docker compose --profile full up
```

### Exercises
1. **Vulnerability scan** -- Run Snyk against the Yole project and categorize findings by severity (critical, high, medium, low).
2. **Static analysis** -- Run Detekt and review the report. Identify the top 3 most common code smell categories.
