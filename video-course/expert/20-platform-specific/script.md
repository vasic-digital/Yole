<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 20: Platform-Specific Development (7 videos)

## Video 20.1: Understanding the expect/actual Pattern (20 min)

### Timestamps
- 0:00 Introduction: the KMP approach to platform abstraction
- 2:00 What expect/actual is and why Yole uses it instead of interfaces
- 4:00 Compiler guarantees: every expect declaration must have an actual on every target
- 6:00 Walking through a real example: `expect fun createHttpClient(): HttpClient` in `HttpClientFactory.kt`
- 8:00 Actual implementations: `HttpClientFactory.android.kt`, `HttpClientFactory.desktop.kt`, `HttpClientFactory.ios.kt`, `HttpClientFactory.wasmJs.kt`
- 10:00 expect classes vs. expect functions vs. expect objects
- 12:00 Yole's expect declarations inventory: `SecureStorageFactory`, `PlatformFileIOFactory`, `SmbProtocolClient`, `SshClient`, `SftpChannel`, `FtpProtocolClient`, `currentTimeMillis()`, `getCurrentDate()`
- 14:00 Limitations: no expect for sealed classes, no expect with default parameters, no typealias for expect classes with nested types
- 16:00 How facade bridges work around these limitations (e.g., `util/RateLimiting.kt`, `util/LazyLoading.kt`)
- 18:00 When to use expect/actual vs. dependency injection vs. interface abstraction
- 19:30 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.kt` -- expect fun declaration
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.wasmJs.kt` -- Wasm actual
- `shared/src/iosMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.ios.kt` -- iOS actual
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` -- expect object SecureStorageFactory
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.kt` -- expect object PlatformFileIOFactory
- `shared/src/commonMain/kotlin/digital/vasic/yole/model/Document.kt` -- expect functions: `currentTimeMillis()`, `getFileModTime()`, `getFileSize()`, `fileExists()`, `createDocument()`

### Key Concept: expect/actual Lifecycle

```kotlin
// commonMain -- the contract
expect fun createHttpClient(): HttpClient

// androidMain -- Android-specific engine
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json() }
}

// desktopMain -- JVM engine
actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
}
```

### Exercises
1. **Trace an expect/actual chain** -- Starting from `SecureStorageFactory` in commonMain, find every actual implementation across all four platform source sets and list the storage mechanism each uses.
2. **Add a new expect function** -- Declare `expect fun getPlatformName(): String` in commonMain and provide actual implementations returning "Android", "Desktop", "iOS", and "Web" respectively.

---

## Video 20.2: iOS Platform Stubs and Limitations (18 min)

### Timestamps
- 0:00 iOS platform status: in development, stubs with Result types
- 2:00 Why FTP, SFTP, and SMB have stub implementations on iOS
- 4:00 Walking through `FtpProtocolClient` iOS actual: stub pattern with `TODO()` markers
- 6:00 Walking through `SshClient` and `SftpChannel` iOS actuals
- 8:00 The `SmbProtocolClient` iOS stub and why SMB is hardest on iOS
- 10:00 `SecureStorageFactory.ios.kt`: iOS Keychain integration via Kotlin/Native interop
- 12:00 `PlatformFileIOFactory.ios.kt`: file system access on iOS (sandboxing, app groups)
- 14:00 Result type pattern for stubs: returning meaningful errors instead of crashing
- 16:00 Roadmap: what needs to happen to make iOS protocols production-ready
- 17:30 Summary

### Code References
- `shared/src/iosMain/kotlin/digital/vasic/yole/network/platform/SecureStorageFactory.ios.kt` -- iOS SecureStorage actual
- `shared/src/iosMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.ios.kt` -- iOS file I/O actual
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/ftp/FtpProtocolClient.kt` -- expect class for FTP
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/sftp/SshClient.kt` -- expect class for SSH/SFTP
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocols/smb/SmbProtocolClient.kt` -- expect class for SMB

### Exercises
1. **Audit iOS stubs** -- List every expect/actual pair where the iOS actual contains `TODO()` or returns a stub value. Classify each by effort required to implement (low/medium/high).
2. **Design an iOS FTP implementation** -- Research available Kotlin/Native or Swift interop libraries for FTP and sketch how you would replace the FTP stub.

---

## Video 20.3: Wasm Browser Security Model Constraints (18 min)

### Timestamps
- 0:00 Introduction: Wasm runs in a browser sandbox
- 2:00 No raw socket access: why FTP, SFTP, and SMB cannot work in Wasm
- 4:00 Walking through `WasmProtocolStubTests.kt`: verifying stubs throw appropriate errors
- 6:00 Secure storage in the browser: `WebSecureStorage` using the Web Crypto API
- 8:00 `SecureStorageFactory.wasmJs.kt`: localStorage vs. sessionStorage vs. IndexedDB
- 10:00 CORS restrictions: how they affect cloud storage API calls from Wasm
- 12:00 `HttpClientFactory.wasmJs.kt`: Ktor Js engine with fetch API underneath
- 14:00 Binary size considerations: Kotlin/Wasm output size and tree shaking
- 16:00 What works well in Wasm: all 17 format parsers, local file editing via File System Access API
- 17:30 Summary

### Code References
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/SecureStorageFactory.wasmJs.kt` -- Wasm secure storage actual
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/WebSecureStorage.kt` -- Web-specific secure storage
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.wasmJs.kt` -- Wasm file I/O actual
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.wasmJs.kt` -- Wasm HTTP client
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WasmProtocolStubTests.kt` -- Protocol stub verification tests
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WebSecureStorageTest.kt` -- Web secure storage tests

### Key Concept: Browser Sandbox Boundaries

```
+---------------------------------------------------+
|  Browser Sandbox (Wasm target)                    |
|                                                   |
|  AVAILABLE:           BLOCKED:                    |
|  - HTTP/HTTPS (fetch) - Raw TCP sockets (FTP)    |
|  - localStorage       - SSH connections (SFTP)    |
|  - IndexedDB          - SMB/CIFS protocol         |
|  - Web Crypto API     - Direct file system*       |
|  - File System Access - Native keychain           |
|    API (limited)                                  |
+---------------------------------------------------+
* File System Access API provides sandboxed access with user permission
```

### Exercises
1. **Test Wasm stubs** -- Run `./gradlew :shared:wasmJsTest` and examine which protocol tests are stub-only vs. fully functional.
2. **CORS investigation** -- Configure a local proxy and demonstrate how a Wasm Yole instance can reach a WebDAV server without CORS issues.

---

## Video 20.4: Android Platform Specifics (20 min)

### Timestamps
- 0:00 Android as Yole's most mature platform: production status
- 2:00 `AndroidSecureStorage`: EncryptedSharedPreferences with AndroidX Security
- 4:00 Key generation: MasterKey with AES256-GCM encryption
- 6:00 `SecureStorageFactory.android.kt`: Context dependency and initialization
- 8:00 `HttpClientFactory` on Android: OkHttp engine with certificate pinning
- 10:00 `PlatformFileIOFactory.android.kt`: Storage Access Framework, content URIs
- 12:00 Android-specific expect/actual for `Document`: file metadata via `java.io.File`
- 14:00 Build variants: `flavorDefault` (dev) vs. `flavorAtest` (testing)
- 16:00 Android-specific test considerations: `androidUnitTest` source set, Robolectric
- 18:00 AGP version constraints and how they affect the build (AGP 8.2.2 compatibility)
- 19:30 Summary

### Code References
- `shared/src/androidMain/kotlin/digital/vasic/yole/network/platform/AndroidSecureStorage.kt` -- Android encrypted storage
- `shared/src/androidMain/kotlin/digital/vasic/yole/network/platform/SecureStorageFactory.android.kt` -- Android factory actual
- `shared/src/androidMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.android.kt` -- Android file I/O actual
- `shared/src/androidTest/kotlin/digital/vasic/yole/network/platform/AndroidSecureStorageTest.kt` -- Android storage tests
- `shared/src/androidTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryAndroidTest.kt` -- Factory integration tests

### Exercises
1. **Trace Android secure storage** -- Follow the call chain from `SecureStorageFactory.create()` on Android through to the underlying `EncryptedSharedPreferences` and identify all encryption parameters.
2. **Build variant comparison** -- Build both `flavorDefault` and `flavorAtest` and compare the APK sizes and included resources.

---

## Video 20.5: Desktop JVM Implementation Patterns (18 min)

### Timestamps
- 0:00 Desktop as Yole's beta platform: Compose for Desktop on JVM
- 2:00 `DesktopSecureStorage`: file-based encrypted storage on JVM
- 4:00 `SecureStorageFactory.desktop.kt`: no OS keychain dependency, portable approach
- 6:00 `HttpClientFactory` on Desktop: CIO engine, full socket support
- 8:00 Why all 8 protocols work on Desktop: JVM has full networking capabilities
- 10:00 FTP via Apache Commons Net, SFTP via JSch, SMB via SMBJ
- 12:00 `PlatformFileIOFactory.desktop.kt`: direct `java.io.File` / `java.nio.file` access
- 14:00 Desktop-specific tests: `desktopTest` source set with JVM-only assertions
- 16:00 JVM target configuration: `jvmTarget = "11"` in shared, JDK 21 for desktopApp
- 17:30 Summary

### Code References
- `shared/src/desktopMain/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorage.kt` -- Desktop encrypted storage
- `shared/src/desktopMain/kotlin/digital/vasic/yole/network/platform/SecureStorageFactory.desktop.kt` -- Desktop factory actual
- `shared/src/desktopMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.desktop.kt` -- Desktop file I/O actual
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorageTest.kt` -- Desktop storage tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorageTests.kt` -- Additional desktop storage tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopHttpClientFactoryTests.kt` -- HTTP client factory tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopPlatformFileIOTests.kt` -- Platform file I/O tests

### Exercises
1. **Compare factory implementations** -- Read `SecureStorageFactory` actuals for all four platforms side by side and document the storage mechanism, encryption approach, and thread-safety strategy each uses.
2. **Desktop protocol test** -- Write a test in `desktopTest` that creates an `HttpClient` via the factory and verifies it can make a request (use a mock server or httpbin).

---

## Video 20.6: Testing Platform-Specific Code (20 min)

### Timestamps
- 0:00 The challenge: testing code that behaves differently per platform
- 2:00 `commonTest` for shared behavior, platform test source sets for platform-specific behavior
- 4:00 MockK is JVM-only: what this means for commonTest mocking strategy
- 6:00 Alternative mocking in commonTest: manual mock implementations (e.g., `MockNetworkStorageService`)
- 8:00 `kotlinx-coroutines-test`: `runTest` vs. `runBlocking` -- the JUnit4 void return issue
- 10:00 The `runBlocking<Unit>` pattern for desktopTest (JUnit4 compatibility)
- 12:00 Platform-specific test examples: `AndroidSecureStorageTest`, `DesktopSecureStorageTests`, `WebSecureStorageTest`, `WasmProtocolStubTests`
- 14:00 `SecureStorageFactoryIntegrationTest` in commonTest: testing the expect object contract
- 16:00 `HttpClientFactoryTest` in commonTest: verifying client creation across platforms
- 18:00 Running platform-specific tests: `./gradlew :shared:desktopTest`, `./gradlew :shared:wasmJsTest`
- 19:30 Summary

### Code References
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageTest.kt` -- Common secure storage tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryIntegrationTest.kt` -- Factory integration tests
- `shared/src/commonTest/kotlin/digital/vasic/yole/network/platform/HttpClientFactoryTest.kt` -- HTTP client factory tests
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorageTests.kt` -- Desktop-specific tests
- `shared/src/wasmJsTest/kotlin/digital/vasic/yole/network/platform/WasmProtocolStubTests.kt` -- Wasm stub tests
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocol/MockNetworkStorageService.kt` -- Manual mock for commonTest

### Key Concept: Testing Without MockK on Non-JVM Targets

```kotlin
// commonTest -- manual mock (works on all platforms)
class TestSecureStorage : SecureStorage {
    private val store = mutableMapOf<String, String>()
    override suspend fun save(key: String, value: String) { store[key] = value }
    override suspend fun load(key: String): String? = store[key]
    override suspend fun delete(key: String) { store.remove(key) }
    override suspend fun clear() { store.clear() }
}

// desktopTest -- MockK available (JVM only)
val mockStorage = mockk<SecureStorage>()
coEvery { mockStorage.save(any(), any()) } just Runs
```

### Exercises
1. **Write a cross-platform test** -- Create a test in `commonTest` that exercises `SecureStorageFactory.create()` and verifies basic save/load/delete operations work regardless of platform.
2. **Mock migration** -- Take a desktopTest that uses MockK and rewrite it as a commonTest using a manual mock implementation. Verify it passes on both desktop and Wasm.

---

## Video 20.7: Platform Factory Patterns (15 min)

### Timestamps
- 0:00 The factory pattern in KMP: why expect objects work well as factories
- 2:00 `SecureStorageFactory`: expect object with `fun create(): SecureStorage`
- 4:00 `PlatformFileIOFactory`: expect object with `fun create(): PlatformFileIO`
- 6:00 `HttpClientFactory`: expect function (simpler pattern for single-method factories)
- 8:00 When to use expect object vs. expect function vs. expect class
- 10:00 Configuration injection: passing platform-specific config through factories
- 12:00 Factory testing strategy: `SecureStorageFactoryDesktopTest`, `SecureStorageFactoryAndroidTest`
- 14:00 Summary

### Code References
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/SecureStorage.kt` -- `expect object SecureStorageFactory`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/platform/PlatformFileIOFactory.kt` -- `expect object PlatformFileIOFactory`
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/protocol/HttpClientFactory.kt` -- `expect fun createHttpClient()`
- `shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryDesktopTest.kt` -- Desktop factory tests
- `shared/src/androidTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryAndroidTest.kt` -- Android factory tests

### Key Concept: expect object as Factory

```kotlin
// commonMain -- contract
expect object SecureStorageFactory {
    fun create(): SecureStorage
}

// androidMain -- Android Keystore backed
actual object SecureStorageFactory {
    actual fun create(): SecureStorage = AndroidSecureStorage(context)
}

// desktopMain -- file-based encryption
actual object SecureStorageFactory {
    actual fun create(): SecureStorage = DesktopSecureStorage()
}

// iosMain -- Keychain backed
actual object SecureStorageFactory {
    actual fun create(): SecureStorage = IosSecureStorage()
}

// wasmJsMain -- Web Crypto API
actual object SecureStorageFactory {
    actual fun create(): SecureStorage = WebSecureStorage()
}
```

### Exercises
1. **Design a new factory** -- Create an `expect object LoggerFactory` that returns a platform-specific logger: Logcat on Android, SLF4J on Desktop, NSLog on iOS, console.log on Wasm.
2. **Factory test coverage** -- Run `./gradlew test koverHtmlReport` and check the coverage for all factory classes. Write tests to cover any untested branches.
3. **Refactor challenge** -- Convert the `HttpClientFactory` expect function into an expect object with a `create()` method. Discuss the tradeoffs with the simpler function-based approach.
