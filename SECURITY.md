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

### Code Security

- Static analysis with Detekt and KtLint
- Dependency scanning with OWASP Dependency-Check
- Secret scanning with Gitleaks
- Code quality analysis with SonarQube

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
