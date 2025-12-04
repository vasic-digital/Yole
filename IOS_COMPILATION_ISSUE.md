# iOS Compilation Issue - Technical Analysis

**Date**: November 19, 2025
**Status**: BLOCKED - Requires Kotlin version downgrade or upstream bug fix
**Priority**: HIGH

---

## Summary

iOS targets cannot be enabled in the Yole project due to a Kotlin Multiplatform 2.1.0 framework export configuration issue.

## Error

```
Could not create task ':shared:linkDebugFrameworkIosX64'.
> Could not create task of type 'KotlinNativeLink'.
   > Configuration with name 'iosX64DebugFrameworkExport' not found.
```

**Location**: `shared/build.gradle.kts:52`

## Root Cause

When Kotlin Multiplatform 2.1.0 creates iOS framework tasks (`linkDebugFrameworkIosX64`), it expects Gradle configurations named `{target}{BuildType}FrameworkExport` to exist. However, these configurations are not being automatically created despite following the standard KMP framework declaration syntax.

### Stacktrace Analysis

```
at org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink.<init>(KotlinNativeLink.kt:183)
```

The `KotlinNativeLink` task constructor is trying to access the export configuration but it doesn't exist.

## Attempted Fixes

### 1. Standard Framework Declaration ❌
```kotlin
iosX64 {
    binaries.framework {
        baseName = "shared"
        isStatic = true
    }
}
```
**Result**: Failed - export configuration not created

### 2. Explicit Export Declarations ❌
```kotlin
binaries.framework {
    baseName = "shared"
    export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    export("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}
```
**Result**: Failed - same error

### 3. Manual Configuration Creation ❌
```kotlin
configurations {
    create("iosX64DebugFrameworkExport")
    create("iosX64ReleaseFrameworkExport")
    // ... etc
}
```
**Result**: Failed - configurations created but still not found by KotlinNativeLink

### 4. API Dependencies with Export ❌
```kotlin
val iosMain by creating {
    dependencies {
        api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        api("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    }
}
```
**Result**: Failed - same error

## Environment

- **Kotlin Version**: 2.1.0
- **Gradle Version**: 8.11.1
- **Compose Multiplatform**: 1.7.3
- **Kotlin Multiplatform Plugin**: 2.1.0
- **Platform**: macOS 15.5 (Apple Silicon)

## Dependencies

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
implementation("com.squareup.okio:okio:3.9.1")
```

## Possible Solutions

### Option 1: Downgrade Kotlin (RECOMMENDED)
Downgrade to Kotlin 2.0.20 which had working iOS support:

**Steps**:
1. Update `gradle/libs.versions.toml`:
   ```toml
   kotlin = "2.0.20"
   ```
2. Sync Gradle
3. Test iOS compilation

**Pros**: Known to work with iOS frameworks
**Cons**: Miss out on Kotlin 2.1.0 features

### Option 2: Wait for Kotlin 2.1.1 Bug Fix
This appears to be a regression in Kotlin 2.1.0. Monitor:
- https://youtrack.jetbrains.com/issues/KT
- Kotlin Slack #multiplatform channel

**Pros**: Stay on latest Kotlin
**Cons**: Unknown timeline for fix

### Option 3: Use CocoaPods Instead of XCFramework
Switch from static framework to CocoaPods integration:

```kotlin
cocoapods {
    summary = "Yole Shared Module"
    homepage = "https://github.com/vasic-digital/Yole"
    ios.deploymentTarget = "15.0"
    framework {
        baseName = "shared"
    }
}
```

**Pros**: Different code path, might avoid the bug
**Cons**: Requires CocoaPods setup, more complex build

### Option 4: Community Workaround
Check if the community has found a workaround:
- Kotlin Slack: https://kotlinlang.slack.com/
- Stack Overflow: kotlin-multiplatform tag
- GitHub Issues for similar projects

## Files Modified (Pending Reversion if Needed)

- `/Volumes/T7/Projects/Yole/shared/build.gradle.kts` (iOS targets enabled, lines 51-68)
- `/Volumes/T7/Projects/Yole/iosApp/build.gradle.kts` (iOS configuration enabled)

## Current Status

- **iOS Targets**: Enabled in config but cannot compile
- **Shared Module**: iOS source sets configured
- **iosApp Module**: iOS framework tasks configured
- **Build Status**: FAILING

## Recommendation

**IMMEDIATE ACTION**: Downgrade to Kotlin 2.0.20 to unblock iOS development.

### Implementation Steps

1. Edit `gradle/libs.versions.toml`:
   ```toml
   kotlin = "2.0.20"  # was 2.1.0
   ```

2. Test iOS compilation:
   ```bash
   gradle :shared:compileKotlinIosArm64
   ```

3. If successful, proceed with iOS app development

4. Monitor Kotlin 2.1.x releases for fix, upgrade when available

## Related Issues

- Kotlin Multiplatform framework export: https://kotl.in/kmp-framework-export
- KMP documentation: https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html

---

**Last Updated**: November 19, 2025
**Investigated By**: Claude Code (AI Assistant)
**Status**: Documented, awaiting decision on solution approach
