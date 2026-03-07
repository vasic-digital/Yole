# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 2.15.x  | :white_check_mark: |
| 2.14.x  | :white_check_mark: |
| < 2.14  | :x:                |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please report it responsibly.

### How to Report

1. **DO NOT** create a public GitHub issue for security vulnerabilities
2. Email security concerns to: security@vasic.digital
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

- **Acknowledgment**: Within 48 hours
- **Initial Assessment**: Within 7 days
- **Resolution Timeline**: Depends on severity
  - Critical: 24-72 hours
  - High: 1-2 weeks
  - Medium: 2-4 weeks
  - Low: Next release cycle

### Scope

The following are in scope for security reports:

- Yole Android application
- Yole Desktop application
- Yole iOS application
- Yole Web application
- Shared KMP module
- Network protocol implementations
- File encryption functionality
- Credential storage

### Out of Scope

- Third-party dependencies (report to upstream)
- Social engineering attacks
- Physical attacks
- Denial of service attacks

## Security Measures

### Data Protection

- **Encryption**: AES-256 for file encryption
- **Credentials**: Platform-specific secure storage
  - Android: EncryptedSharedPreferences
  - Desktop: Java Preferences with encryption
  - iOS: Keychain Services
  - Web: Encrypted localStorage

### Network Security

- All cloud connections use HTTPS/TLS 1.2+
- Certificate pinning for cloud services
- No telemetry or data collection
- Offline-first architecture

### Resilience Patterns

The network protocol layer includes these resilience mechanisms to protect against cascading failures and resource exhaustion:

- **CircuitBreaker** (`network/common/CircuitBreaker.kt`): Implements a CLOSED/OPEN/HALF_OPEN state machine with configurable failure threshold and reset timeout. Prevents repeated calls to failing services.
- **ConnectionLimiter** (`network/common/ConnectionLimiter.kt`): Semaphore-based concurrent connection limiting. Non-blocking design prevents resource exhaustion under high load.
- **DocumentCache** (`format/DocumentCache.kt`): LRU cache for `ParsedDocument` instances with hit/miss tracking and configurable maximum size.

### CancellationException Safety

All `catch` blocks in all eight protocol service implementations (FTP, SFTP, SMB, WebDAV, Git, Dropbox, Google Drive, OneDrive) rethrow `kotlin.coroutines.cancellation.CancellationException`. This ensures that structured concurrency is never silently broken when a coroutine is cancelled, preventing coroutine leaks and hung operations.

### Query and JSON Injection Protection

API query strings sent to cloud providers (Google Drive, OneDrive, Dropbox) are sanitized before transmission:

- Single-quote escaping to prevent query injection in search and filter operations
- URL encoding for path and query parameters
- JSON escaping for request bodies containing user-supplied data

### Path Traversal Defense

All protocol services that handle file paths use `normalizePath()` which resolves `..` path segments and enforces root boundary constraints. This prevents directory traversal attacks that could access files outside the configured storage root.

### CoroutineScope Lifecycle

Protocol services that maintain a `serviceScope` cancel it on reconnect and disconnect operations. This prevents coroutine leaks from orphaned background operations when connection state changes.

### Code Security

- **Detekt**: Static analysis configured in `config/detekt/detekt.yml` with security-focused rules
- **Snyk**: Dependency vulnerability scanning in CI/CD and Docker
- **SonarQube**: Code quality and security analysis (Docker-based local instance)
- **CodeQL**: GitHub-native static analysis for Java/Kotlin
- **Gitleaks**: Secret scanning across full git history
- **OWASP Dependency-Check**: Gradle plugin (version 11.1.1, `failBuildOnCVSS = 9.0f`)

For detailed instructions on running security scans locally and in CI, see [docs/SECURITY_SCANNING.md](docs/SECURITY_SCANNING.md).

## Security Checklist for Contributors

Before submitting code:

- [ ] No hardcoded credentials or API keys
- [ ] Input validation for all user data
- [ ] Proper error handling (no stack traces to users)
- [ ] Secure random number generation
- [ ] No SQL injection vulnerabilities
- [ ] No path traversal vulnerabilities
- [ ] Proper permission checks
- [ ] Memory-safe operations

## Vulnerability Disclosure

We follow responsible disclosure:

1. Reporter notifies us privately
2. We acknowledge and investigate
3. We develop and test a fix
4. We release the fix
5. We credit the reporter (if desired)
6. Details published after 90 days or fix release

## Contact

- Security Email: security@vasic.digital
- PGP Key: Available upon request
