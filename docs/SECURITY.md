<!--
SPDX-FileCopyrightText: 2025 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| latest  | Yes                |
| < latest| No                 |

Only the latest release receives security updates. Users are encouraged to stay on the most recent version.

---

## Reporting a Vulnerability

If you discover a security vulnerability in Yole, please report it responsibly.

**Do NOT open a public GitHub issue for security vulnerabilities.**

Instead, please use one of the following methods:

1. **GitHub Security Advisories**: Use the [GitHub Security Advisory](https://github.com/nicehash/Yole/security/advisories) feature to privately report the vulnerability.
2. **Email**: Send a detailed report to the project maintainer directly.

### What to include in your report

- A description of the vulnerability and its potential impact
- Steps to reproduce the issue
- Affected versions and platforms (Android, Desktop, iOS, Web)
- Any proof-of-concept code or screenshots
- Your suggested fix, if applicable

### Response timeline

- **Acknowledgment**: Within 48 hours of receiving the report
- **Assessment**: Within 7 days, an initial assessment of severity and scope
- **Fix**: Critical vulnerabilities are prioritized and patched as quickly as possible
- **Disclosure**: Coordinated disclosure after a fix is available

---

## Security Architecture Overview

Yole is a Kotlin Multiplatform (KMP) cross-platform text editor with optional cloud storage integration. The security architecture addresses the following domains:

- **Credential storage** via platform-native secure storage mechanisms
- **Authentication** via OAuth2 flows for cloud services and credential-based auth for network protocols
- **Network security** via TLS/SSL enforcement across all supported protocols
- **Input validation** via HTML escaping and content sanitization in the text parsing pipeline
- **Static analysis and scanning** via an automated CI/CD security pipeline

All sensitive operations are confined to the `shared` module under the `digital.vasic.yole.network` package, ensuring consistent security behavior across all platforms.

---

## Credential Storage

### SecureStorage Interface

All credential storage is abstracted behind the `SecureStorage` interface defined in:

```
shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt
```

The interface provides a unified API for securely storing, retrieving, and deleting sensitive data including:

- **Credentials** (username/password pairs) via `storeCredentials()` / `retrieveCredentials()`
- **Authentication tokens** (OAuth2 access/refresh tokens) via `storeToken()` / `retrieveToken()`
- **Private keys** (SSH keys for SFTP/Git) via `storePrivateKey()` / `retrievePrivateKey()`

A `SecureStorageFactory` (`expect`/`actual` pattern) creates the appropriate platform-specific implementation:

```kotlin
expect object SecureStorageFactory {
    suspend fun create(): Result<SecureStorage>
    suspend fun isAvailable(): Boolean
}
```

All SecureStorage operations return `Result<T>` types for safe error handling, and the `isSecure()` method allows runtime verification that the storage backend is functioning correctly.

### Platform Implementations

#### Android: `AndroidSecureStorage`

**File**: `shared/src/androidMain/kotlin/digital/vasic/yole/network/platform/AndroidSecureStorage.kt`

- Uses **AndroidX Security Crypto** library (`EncryptedSharedPreferences`)
- Master key generated via `MasterKey.Builder` with **AES-256-GCM** key scheme
- Key encryption: **AES-256-SIV**
- Value encryption: **AES-256-GCM**
- Keys are backed by the **Android Keystore** hardware security module where available
- Stored under the preferences file name `network_secure_storage`

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

EncryptedSharedPreferences.create(
    context,
    PREFS_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

#### Desktop (JVM): `DesktopSecureStorage`

**File**: `shared/src/desktopMain/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorage.kt`

- Uses **AES-256-GCM** encryption with `javax.crypto` APIs
- Secret key generated via `KeyGenerator.getInstance("AES")` with 256-bit key size
- GCM mode with 12-byte IV (initialization vector) and 128-bit authentication tag
- Encrypted data stored in a file-based storage system (`.secure_storage`)
- Key file (`.storage_key`) has restricted file permissions (`setReadable(false, false)`)
- Data file also has restricted permissions

#### Web (Wasm): `WebSecureStorage`

**File**: `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/WebSecureStorage.kt`

- Uses **localStorage** with XOR obfuscation and Base64 encoding
- All keys are prefixed with `yole_network_secure_` to avoid collisions
- **Important limitation**: Web environments cannot provide true encryption; this implementation provides obfuscation only, not cryptographic security
- The `isSecure()` method returns `true` if the obfuscation/deobfuscation cycle functions correctly, but this does not indicate cryptographic security

#### iOS (Planned)

- Will use **iOS Keychain Services** via `Security.framework`
- Keychain items will be stored with `kSecClassGenericPassword`
- Access control via `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`

---

## Authentication and Token Management

### OAuth2 Flows

**File**: `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/OAuth2Flow.kt`

Yole implements the **OAuth2 Authorization Code Grant** flow for cloud storage services. The base `OAuth2Flow` class provides:

- **Authorization URL generation** with CSRF protection via `state` parameter
- **Authorization code exchange** for access tokens (`grant_type=authorization_code`)
- **Token refresh** using refresh tokens (`grant_type=refresh_token`)
- **Token revocation** for logout/disconnect scenarios
- **Offline access** requested via `access_type=offline` to obtain refresh tokens
- **Forced consent** via `prompt=consent` to ensure refresh token issuance

Service-specific OAuth2 implementations:

| Service       | Authorization Endpoint                                         | Token Endpoint                                          | Scopes                                                          |
|---------------|----------------------------------------------------------------|---------------------------------------------------------|-----------------------------------------------------------------|
| Dropbox       | `https://www.dropbox.com/oauth2/authorize`                     | `https://api.dropboxapi.com/oauth2/token`               | `files.content.write`, `files.content.read`, `files.metadata.read` |
| Google Drive  | `https://accounts.google.com/o/oauth2/v2/auth`                | `https://oauth2.googleapis.com/token`                   | `drive.file`, `drive.readonly`                                  |
| OneDrive      | `https://login.microsoftonline.com/common/oauth2/v2.0/authorize` | `https://login.microsoftonline.com/common/oauth2/v2.0/token` | `Files.ReadWrite`, `Files.ReadWrite.All`, `offline_access`      |

All OAuth2 endpoints use **HTTPS** exclusively. Token responses are deserialized via `kotlinx.serialization` with the `TokenResponse` data class, which includes `access_token`, `token_type`, `expires_in`, `refresh_token`, and `scope`.

### Token Management

**File**: `shared/src/commonMain/kotlin/digital/vasic/yole/network/auth/AuthTokenManager.kt`

The `AuthTokenManager` class manages the full token lifecycle:

- **Thread-safe access**: All public methods use a `Mutex` to prevent concurrent token corruption. Internal methods (suffixed `Internal`) operate without locking and are only called from within locked blocks to prevent deadlocks.
- **Token storage**: Access tokens, refresh tokens, and expiration timestamps are stored separately in `SecureStorage` with service-specific key prefixes.
- **Expiration tracking**: Token expiration is stored as epoch milliseconds and checked against `Clock.System.now()`. Tokens with no expiration data are assumed expired.
- **Token validity checking**: `hasValidToken()` performs a combined check for token existence, non-emptiness, and non-expiration.
- **Complete token info storage**: `storeTokenInfo()` atomically stores access token, optional refresh token, and optional expiration in a single locked operation.
- **Secure cleanup**: `clearTokens()` removes all token data (access, refresh, expiration) for a given service.
- **Debug-safe introspection**: `getTokenInfo()` returns a `TokenInfo` object containing boolean flags (`hasAccessToken`, `hasRefreshToken`, `isExpired`) without exposing actual token values.

### Credential-Based Authentication

For non-OAuth2 protocols (WebDAV, FTP, SFTP, SMB, Git), credentials are stored via `SecureStorage.storeCredentials()` during configuration through `NetworkStorageConfigService`. Each protocol's credentials are namespaced with a protocol prefix (e.g., `webdav_`, `ftp_`, `sftp_`, `smb_`, `git_`) combined with the connection name.

SFTP additionally supports **SSH key-based authentication** via `storePrivateKey()`.

---

## Network Security

### TLS/SSL Enforcement

All cloud storage services (Dropbox, Google Drive, OneDrive) communicate exclusively over HTTPS. OAuth2 authorization and token endpoints are all HTTPS URLs:

- `https://www.dropbox.com/oauth2/authorize`
- `https://api.dropboxapi.com/oauth2/token`
- `https://accounts.google.com/o/oauth2/v2/auth`
- `https://oauth2.googleapis.com/token`
- `https://login.microsoftonline.com/common/oauth2/v2.0/authorize`
- `https://login.microsoftonline.com/common/oauth2/v2.0/token`

Network protocol services (WebDAV, SFTP) support encrypted connections:

- **WebDAV**: Supports both `http://` and `https://` URLs (HTTPS recommended)
- **SFTP**: Encrypted by default via SSH protocol
- **FTP**: Standard FTP (FTPS support depends on server configuration)
- **SMB**: Encryption depends on server/client negotiation

### Certificate Validation

The `NetworkStorageException.ConnectionException.SslError` class provides structured error handling for SSL/TLS failures, including certificate error details. The error handling hierarchy includes:

- `SslError` with optional `certificateError` description
- User-friendly message: "Secure connection failed. Please check certificate settings."
- Classified as a non-retryable error requiring user intervention

### HTTP Client

Network communication uses the **Ktor** HTTP client library (`io.ktor.client`), which delegates TLS/SSL handling to the platform-native HTTP stack:

- **Android**: OkHttp or Android HttpURLConnection (platform certificate store)
- **Desktop (JVM)**: Java's `SSLContext` with system trust store
- **iOS**: URLSession (Apple's certificate infrastructure)

---

## Input Validation and Sanitization

### HTML Escaping

**File**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt`

All text parsers use the `escapeHtml()` extension function to prevent XSS when rendering user content as HTML:

```kotlin
fun String.escapeHtml(): String {
    return this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
```

This function escapes the five standard HTML special characters and is applied in the default `TextParser.toHtml()` implementation. Individual parser implementations (Markdown, LaTeX, Org Mode, etc.) use this function when inserting user-provided content into HTML output.

### Configuration Validation

The `NetworkStorageConfigService.validateConfiguration()` method performs input validation on all storage configurations before they are saved or used:

- **URL validation**: WebDAV URLs must start with `http://` or `https://`; Git URLs must start with `http(s)://` or `git@`
- **Required field validation**: Usernames, passwords, host addresses, and service-specific fields are validated for non-emptiness
- **Port range validation**: FTP and SFTP ports must be within 1-65535
- **Authentication requirements**: SFTP requires either password or private key; OAuth2 services require either access token or refresh token

### Network Error Classification

The `NetworkStorageException` sealed class hierarchy provides structured error handling with:

- Error codes for programmatic handling
- Timestamps for audit logging
- User-friendly messages via `toUserMessage()`
- Retryability classification via `isRetryable()`
- Permanent failure detection via `isPermanentFailure()`
- Suggested actions via `getSuggestedAction()`

---

## Security Scanning Pipeline

Yole employs a multi-layered security scanning pipeline defined in `.github/workflows/security.yml`. The workflow runs on:

- Every push to the `master` branch
- Every pull request
- Weekly schedule (Sunday at midnight UTC)

The workflow uses minimal permissions (`contents: read`, `security-events: write`, `actions: read`) and concurrency controls to cancel redundant runs.

### Gitleaks (Secret Scanning)

- **Purpose**: Detects accidentally committed secrets, API keys, tokens, and passwords
- **Action**: `gitleaks/gitleaks-action@v2`
- **Configuration**: Full history scan (`fetch-depth: 0`)
- **Trigger**: Every push and pull request

### OWASP Dependency Check

- **Purpose**: Identifies known vulnerabilities in third-party dependencies using the National Vulnerability Database (NVD)
- **Configuration**: JDK 17 + Android SDK environment
- **Status**: Workflow infrastructure is in place; the Gradle plugin integration is prepared for activation
- **Current**: Dependency tree is audited via `./gradlew dependencies --configuration runtimeClasspath`

### Snyk Vulnerability Scanning

- **Purpose**: Scans Gradle dependencies for known vulnerabilities
- **Action**: `snyk/actions/gradle@master`
- **Configuration**: Severity threshold set to `high` (`--severity-threshold=high`)
- **Authentication**: Uses `SNYK_TOKEN` secret
- **Failure policy**: `continue-on-error: true` to avoid blocking builds on scan issues

### CodeQL Static Analysis

- **Purpose**: Semantic code analysis to find security vulnerabilities, bugs, and code quality issues
- **Action**: `github/codeql-action/init@v3` and `github/codeql-action/analyze@v3`
- **Languages**: `java-kotlin`
- **Build targets**: `:shared:compileKotlinDesktop` and `:androidApp:compileFlavorDefaultDebugKotlin`
- **Results**: Published to GitHub Security tab as SARIF reports

### SonarQube Continuous Inspection

**File**: `docker-compose.yml` (under `security` profile)

- **Purpose**: Continuous code quality and security inspection
- **Image**: `sonarqube:community`
- **Port**: 9000
- **Activation**: Available via Docker Compose `security` or `full` profiles
- **Data persistence**: Separate volumes for data, logs, and extensions

```bash
# Start SonarQube for local analysis
docker compose --profile security up -d sonarqube
```

### Detekt Kotlin Static Analysis

**File**: `detekt.yml`

- **Purpose**: Kotlin-specific static analysis for code quality and potential bugs
- **Configuration**:
  - `maxIssues: 0` (zero tolerance for detected issues)
  - `DoubleMutabilityForCollection: active` (detects mutable collections in mutable variables)
  - `WildcardImport: active` (prevents wildcard imports that could shadow types)
  - `ComplexCondition: threshold: 4` (limits boolean expression complexity)
  - `CyclomaticComplexMethod: threshold: 15` (limits method complexity)
  - `LongMethod: threshold: 60` (limits method length)
  - `LongParameterList: functionThreshold: 6, constructorThreshold: 7`

---

## Secure Development Practices

### Thread Safety

- `AuthTokenManager` uses Kotlin coroutine `Mutex` for all token operations, with a carefully designed internal/external method pattern to prevent deadlocks from nested lock acquisition
- `ParserRegistry` uses `synchronized` blocks for thread-safe parser registration and lookup
- `ParsedDocument` uses `@Volatile` annotations for thread-safe lazy HTML cache initialization
- `NetworkStorageConfigService` uses `Mutex` for all state mutations

### Error Handling

- All security-sensitive operations return `Result<T>` types instead of throwing exceptions
- `SecureStorage.isSecure()` gracefully returns `false` on any failure rather than exposing error details
- `AuthTokenManager.getTokenInfo()` exposes only boolean flags, never raw token values
- `NetworkStorageException` provides structured error information without leaking sensitive data (no passwords or tokens in error messages)

### Memory Safety

- HTML generation in `ParsedDocument` is lazily evaluated to avoid unnecessary memory allocation
- `clearHtmlCache()` allows explicit memory reclamation
- Credential data in `SecureStorage` uses `Result` wrapping to ensure cleanup on failure paths

### Build Environment Isolation

All builds and tests execute inside Docker containers (see `docker-compose.yml`) to ensure:

- Consistent and reproducible build environment
- Isolation from host system credentials and configuration
- Controlled dependency resolution

---

## Dependencies and Supply Chain Security

### Dependency Management

- All dependency versions are centralized in `gradle/libs.versions.toml` for single-point-of-control updates
- Gradle wrapper is checked into the repository with a verified checksum
- GitHub Actions use pinned action versions (e.g., `actions/checkout@v4`, `actions/setup-java@v4`)

### Scanning Coverage

| Tool                    | Scope                          | Frequency              |
|-------------------------|--------------------------------|------------------------|
| Gitleaks                | Committed secrets              | Every push/PR          |
| OWASP Dependency Check  | Known CVEs in dependencies     | Every push/PR          |
| Snyk                    | Dependency vulnerabilities     | Every push/PR          |
| CodeQL                  | Source code vulnerabilities     | Every push/PR + weekly |
| SonarQube               | Code quality + security        | On-demand (local)      |
| Detekt                  | Kotlin static analysis         | Every build            |

### Third-Party Libraries

Key security-relevant dependencies:

- **AndroidX Security Crypto** (`androidx.security.crypto`): EncryptedSharedPreferences
- **Ktor Client**: HTTP networking with platform-native TLS
- **kotlinx.serialization**: Safe deserialization of OAuth2 responses
- **kotlinx.datetime**: Secure time handling for token expiration
- **kotlinx.coroutines**: Thread-safe concurrency primitives (Mutex)

---

## Platform-Specific Security Summary

| Platform | Secure Storage       | Encryption              | Key Management           |
|----------|----------------------|-------------------------|--------------------------|
| Android  | EncryptedSharedPreferences | AES-256-GCM / AES-256-SIV | Android Keystore (hardware-backed) |
| Desktop  | File-based encrypted store | AES-256-GCM             | Generated AES key with restricted file permissions |
| Web      | localStorage + obfuscation | XOR obfuscation (not cryptographic) | Embedded obfuscation key |
| iOS      | Keychain Services (planned) | AES-256 (system)        | Secure Enclave (planned) |

---

## Security Contacts

For security-related questions or concerns, please use GitHub Security Advisories or contact the project maintainer directly. Do not discuss security vulnerabilities in public issues, discussions, or pull requests until a fix has been released.
