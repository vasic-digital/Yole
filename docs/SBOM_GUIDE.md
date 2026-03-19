<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# SBOM Guide

This document describes how to generate, validate, and use a Software Bill of Materials (SBOM) for the Yole project.

---

## What is an SBOM?

A Software Bill of Materials (SBOM) is a formal, machine-readable inventory of all components (direct and transitive dependencies) in a software project. It lists each dependency's name, version, license, and known vulnerabilities.

SBOMs serve three primary purposes:

1. **Vulnerability monitoring** -- feed the SBOM into tools like OWASP Dependency-Track to receive continuous alerts when new CVEs affect your dependencies.
2. **License compliance** -- verify that all transitive dependencies use licenses compatible with your project's distribution model.
3. **Supply chain transparency** -- provide auditors, customers, or compliance teams with a complete inventory of third-party code.

---

## CycloneDX Plugin

Yole uses the [CycloneDX Gradle plugin](https://github.com/CycloneDX/cyclonedx-gradle-plugin) to generate SBOMs in the CycloneDX 1.5 JSON format.

The plugin is declared in the root `build.gradle.kts`:

```kotlin
plugins {
    id("org.cyclonedx.bom") version "1.10.0"
}
```

### KMP Compatibility Note

The CycloneDX plugin resolves dependency configurations to build the SBOM. When applied to the KMP root project, it fails because the root project does not have standard JVM configurations (`runtimeClasspath`, `compileClasspath`) that the plugin expects.

**The plugin must be applied to JVM-targeting subprojects** (`desktopApp`, `androidApp`) rather than the root project. This is because those subprojects produce JVM artifacts with resolvable dependency graphs.

---

## How to Generate an SBOM

### Step 1: Apply the Plugin to a JVM Subproject

Add the CycloneDX plugin to the target subproject's `build.gradle.kts`:

```kotlin
// desktopApp/build.gradle.kts
plugins {
    id("org.cyclonedx.bom") version "1.10.0"
}
```

Or for Android:

```kotlin
// androidApp/build.gradle.kts
plugins {
    id("org.cyclonedx.bom") version "1.10.0"
}
```

### Step 2: Run the SBOM Generation Task

```bash
# Desktop SBOM
./gradlew :desktopApp:cyclonedxBom

# Android SBOM
./gradlew :androidApp:cyclonedxBom
```

Inside a container:

```bash
docker compose run --rm build ./gradlew :desktopApp:cyclonedxBom
```

### Step 3: Locate the Output

The SBOM is generated at:

```
desktopApp/build/reports/bom.json
```

or

```
androidApp/build/reports/bom.json
```

The output is a CycloneDX 1.5 JSON file containing:

- **Metadata** -- project name, version, timestamp
- **Components** -- every direct and transitive dependency with group, name, version, type, and license
- **Dependencies** -- the dependency graph showing which components depend on which

---

## Validating the SBOM

### Using the CycloneDX CLI

Install the [CycloneDX CLI](https://github.com/CycloneDX/cyclonedx-cli):

```bash
# Validate the SBOM against the CycloneDX schema
cyclonedx validate --input-file desktopApp/build/reports/bom.json --input-format json
```

### Manual Inspection

The JSON file can be inspected directly. Key sections:

```json
{
  "bomFormat": "CycloneDX",
  "specVersion": "1.5",
  "metadata": { ... },
  "components": [
    {
      "type": "library",
      "group": "org.jetbrains.kotlin",
      "name": "kotlin-stdlib",
      "version": "2.0.20",
      "licenses": [ { "license": { "id": "Apache-2.0" } } ]
    }
  ],
  "dependencies": [ ... ]
}
```

---

## Integration with Supply Chain Security

### OWASP Dependency-Track

[Dependency-Track](https://dependencytrack.org/) is an open-source platform that consumes SBOMs and continuously monitors them for vulnerabilities.

```bash
# Upload SBOM to Dependency-Track via API
curl -X POST "http://localhost:8081/api/v1/bom" \
    -H "X-Api-Key: YOUR_API_KEY" \
    -H "Content-Type: multipart/form-data" \
    -F "project=YOLE_PROJECT_UUID" \
    -F "bom=@desktopApp/build/reports/bom.json"
```

### Automated Monitoring Workflow

1. Generate the SBOM after each release build.
2. Upload to Dependency-Track (or a similar tool).
3. Dependency-Track cross-references all components against NVD, GitHub Advisory Database, and OSS Index.
4. When a new vulnerability is disclosed that affects a listed component, a notification is sent.

### License Auditing

Use the SBOM to verify all dependencies use approved licenses:

```bash
# Using cyclonedx-cli to list licenses
cyclonedx convert --input-file desktopApp/build/reports/bom.json --output-format csv \
    | grep -v "Apache-2.0\|MIT\|BSD"
```

Any output indicates dependencies with licenses that may require review.

---

## Key Dependencies Tracked

The Yole SBOM captures all dependencies listed in `gradle/libs.versions.toml`, including:

| Dependency | Version | License |
|-----------|---------|---------|
| Kotlin Stdlib | 2.0.20 | Apache-2.0 |
| Compose Multiplatform | 1.7.3 | Apache-2.0 |
| Kotlinx Coroutines | 1.9.0 | Apache-2.0 |
| Kotlinx Serialization | 1.7.3 | Apache-2.0 |
| Ktor Client | 3.0.2 | Apache-2.0 |
| Flexmark | 0.64.8 | BSD-2-Clause |
| Okio | 3.9.1 | Apache-2.0 |

Plus all transitive dependencies pulled in by these libraries.

---

## Related Documentation

- [Security Scanning Guide](SECURITY_SCANNING.md) -- Vulnerability scanning tools
- [Build System Guide](BUILD_SYSTEM.md) -- Build commands and Gradle configuration
- [Security Event Logging](SECURITY_EVENT_LOGGING.md) -- Runtime security audit trail

---

*Last updated: March 19, 2026*
