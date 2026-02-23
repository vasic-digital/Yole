# Module 10: Production Deployment (10 videos)

## Video 10.1: CI/CD Pipeline Setup (25 min)

### Timestamps
- 0:00 Introduction to CI/CD for KMP projects
- 2:00 GitHub Actions workflow structure
- 5:00 Matrix builds: Android, Desktop, Web, iOS
- 8:00 Automated testing on every push
- 11:00 Docker-based builds for reproducibility
- 14:00 Artifact caching and build speed optimization
- 17:00 Branch protection rules and required checks
- 20:00 Secrets management for signing keys
- 23:00 Pipeline monitoring and failure notifications
- 24:30 Summary

---

## Video 10.2: Code Signing and Distribution (20 min)

### Timestamps
- 0:00 Why code signing matters
- 2:00 Android: keystore generation and signing configs
- 5:00 Play App Signing: upload key vs. signing key
- 8:00 Desktop: Windows Authenticode signing
- 10:00 Desktop: macOS notarization with Apple Developer ID
- 12:00 Desktop: Linux package signing (GPG)
- 14:00 iOS: Xcode certificates and provisioning profiles
- 16:00 Automated signing in CI/CD
- 18:00 Key rotation and security best practices
- 19:30 Summary

---

## Video 10.3: App Store Deployment (18 min)

### Timestamps
- 0:00 Multi-store deployment overview
- 2:00 Google Play Console: store listing, screenshots, descriptions
- 4:00 Staged rollout: 1%, 10%, 50%, 100%
- 6:00 Play Console policies and review process
- 8:00 Apple App Store Connect: TestFlight beta testing
- 10:00 App Review guidelines and common rejection reasons
- 12:00 F-Droid: metadata format, build recipes, reproducible builds
- 14:00 Direct APK distribution via GitHub Releases
- 16:00 Version management across stores
- 17:30 Summary

---

## Videos 10.4-10.10: Production Operations

### Video 10.4: Crash Reporting (12 min)

#### Timestamps
- 0:00 Crash reporting overview
- 2:00 Firebase Crashlytics setup for Android
- 4:00 Sentry for multiplatform crash reporting
- 6:00 Symbolication and deobfuscation
- 8:00 Crash grouping and prioritization
- 10:00 Alerting thresholds and escalation
- 11:30 Summary

### Video 10.5: Analytics and Usage Tracking (12 min)

#### Timestamps
- 0:00 Privacy-first analytics philosophy
- 2:00 Anonymous usage metrics: format popularity, platform distribution
- 4:00 Event tracking with Firebase Analytics
- 6:00 Custom event design for format usage
- 8:00 GDPR and privacy regulation compliance
- 10:00 Opt-out mechanisms and transparency
- 11:30 Summary

### Video 10.6: Feature Flags and A/B Testing (12 min)

#### Timestamps
- 0:00 Feature flags for gradual rollout
- 2:00 Firebase Remote Config integration
- 4:00 Server-side vs. client-side flags
- 6:00 A/B testing UI variations
- 8:00 Measuring experiment results
- 10:00 Flag cleanup and technical debt
- 11:30 Summary

### Video 10.7: Automated Release Pipelines (12 min)

#### Timestamps
- 0:00 Release automation goals
- 2:00 Semantic versioning strategy
- 4:00 Changelog generation from commit messages
- 6:00 GitHub Releases automation
- 8:00 Multi-platform artifact publishing
- 10:00 Release approval workflows
- 11:30 Summary

### Video 10.8: Monitoring and Alerting (12 min)

#### Timestamps
- 0:00 Production monitoring overview
- 2:00 Health checks and uptime monitoring
- 4:00 Performance monitoring: ANR, startup time, frame rate
- 6:00 Custom metrics and dashboards
- 8:00 Alert rules and notification channels
- 10:00 Incident response runbooks
- 11:30 Summary

### Video 10.9: Database Migrations and Versioning (12 min)

#### Timestamps
- 0:00 Why database migrations matter
- 2:00 SQLDelight migration files
- 4:00 Forward and backward compatibility
- 6:00 Data migration strategies
- 8:00 Testing migrations with real data
- 10:00 Rollback procedures
- 11:30 Summary

### Video 10.10: Disaster Recovery and Rollback (12 min)

#### Timestamps
- 0:00 Disaster recovery planning
- 2:00 Rollback strategies for app updates
- 4:00 Data backup and restore procedures
- 6:00 Cloud storage data recovery
- 8:00 Post-mortem process for incidents
- 10:00 Preventive measures and redundancy
- 11:30 Summary
