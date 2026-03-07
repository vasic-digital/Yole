<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Troubleshooting Guide

Common issues and solutions for Yole, organized by category. If your issue is not listed here, check the [GitHub Issues](https://github.com/vasic-digital/Yole/issues) page or open a new issue.

---

## Build Issues

### Gradle sync fails with "Could not resolve all dependencies"

**Cause**: Missing or unreachable dependency repositories, or network issues.

**Fix**:
1. Verify your internet connection
2. Check that `settings.gradle.kts` includes all required `includeBuild()` directives for the 10 extracted KMP modules
3. If behind a corporate proxy, configure Gradle proxy settings in `~/.gradle/gradle.properties`:
   ```properties
   systemProp.http.proxyHost=proxy.example.com
   systemProp.http.proxyPort=8080
   systemProp.https.proxyHost=proxy.example.com
   systemProp.https.proxyPort=8080
   ```
4. Run `./gradlew --refresh-dependencies` to clear the dependency cache

### AGP version mismatch (8.2.2 vs 8.9.0)

**Cause**: The extracted KMP modules may target a different AGP version than the main Yole project.

**Fix**: This is a known issue. Use `:shared:desktopTest` instead of `./gradlew test` when running tests for the Android app module. The shared module tests cover the same code paths.

### JDK version errors ("Unsupported class file major version")

**Cause**: Yole's shared module targets JVM 11, but the desktop app requires JDK 21.

**Fix**:
- For container builds: the Docker container uses JDK 11 and is configured correctly
- For local builds: install JDK 21 and set `JAVA_HOME` to point to it
- Verify: `java -version` should show 21.x
- If you see `class file has wrong version 65.0, should be 55.0`, you are compiling with JDK 21 but running on JDK 11

### Out of memory during build (exit code 137)

**Cause**: The Gradle daemon or container ran out of memory. Exit code 137 means OOM kill.

**Fix**:
1. Increase Gradle heap size in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:+HeapDumpOnOutOfMemoryError
   ```
2. In Docker, increase the container memory limit:
   ```bash
   docker compose run --rm -m 8g build ./gradlew test
   ```
3. Clean Gradle lock files after an OOM kill:
   ```bash
   find ~/.gradle/caches -name "*.lock" -delete
   ```

### "Cannot find symbol" for extracted module types

**Cause**: The composite build modules are not properly wired or have not been built.

**Fix**:
1. Verify all 10 `includeBuild()` directives exist in `settings.gradle.kts`
2. Build the extracted modules first:
   ```bash
   cd /path/to/RateLimiter-KMP && ./gradlew build
   cd /path/to/Concurrency-KMP && ./gradlew build
   # ... repeat for all modules
   ```
3. In Yole, run `./gradlew --refresh-dependencies`

### iOS build fails in Xcode

**Cause**: iOS support is in development. Kotlin/Native compilation for iOS targets may fail due to missing expect/actual declarations or unsupported APIs.

**Fix**: Open `iosApp/iosApp.xcodeproj` in Xcode and check the build log for specific errors. Ensure the Kotlin Multiplatform iOS framework is built first:
```bash
./gradlew :shared:linkDebugFrameworkIosArm64
```

---

## Connection Issues

### "Connection timed out" for all providers

**Cause**: Network connectivity issue, DNS resolution failure, or firewall blocking outbound connections.

**Fix**:
1. Test basic connectivity: `ping api.dropboxapi.com` (or your server's hostname)
2. Check DNS resolution: `nslookup your-server.com`
3. Verify the port is reachable: `nc -zv host port`
4. If behind a firewall, ensure ports 443 (HTTPS), 22 (SSH/SFTP), 21 (FTP), and 445 (SMB) are open for outbound traffic
5. Increase the `connectionTimeout` in the provider's configuration

### OAuth sign-in opens browser but never returns

**Cause**: The redirect URI in the OAuth configuration does not match the registered redirect URI, or the browser cannot redirect back to Yole.

**Fix**:
1. Verify the redirect URI registered with the provider matches Yole's expected URI:
   - Android: `yole://oauth/callback`
   - Desktop: `http://localhost:8080/callback`
2. On Android, ensure no other app is registered to handle the `yole://` scheme
3. On Desktop, ensure port 8080 is not in use by another process

### "SSL handshake failed" or "Certificate not trusted"

**Cause**: The server uses a self-signed certificate, an expired certificate, or an untrusted CA.

**Fix**:
1. For self-signed certificates on WebDAV: disable **Verify Certificate** in the connection settings (reduces security)
2. For expired certificates: contact the server administrator
3. For corporate CAs: install the CA root certificate on your device
4. Test SSL manually: `openssl s_client -connect host:443`

### Token refresh fails ("invalid_grant")

**Cause**: The refresh token has been revoked, expired (for providers with expiring refresh tokens), or the app credentials have changed.

**Fix**:
1. Go to **Settings** > **Cloud Storage** and tap **Re-authenticate** on the affected provider
2. Complete a fresh OAuth flow
3. If using custom OAuth credentials, verify the client ID and secret have not changed
4. Check that the app has not been removed from the provider's authorized apps list

### "Rate limit exceeded" (HTTP 429)

**Cause**: Too many API requests in a short period. Common with Dropbox development apps (~100 requests/minute).

**Fix**:
1. Yole's built-in `AdaptiveRateLimiter` automatically backs off on 429 responses
2. Wait a few minutes and retry
3. Reduce sync frequency for the affected provider
4. If using a Dropbox development app, consider applying for production approval for higher limits

---

## Format Issues

### Format not detected for a file

**Cause**: The file extension is not recognized, or the content does not match any detection pattern.

**Fix**:
1. Check the file extension matches a supported format (e.g., `.md` for Markdown, `.tex` for LaTeX)
2. Use **Manual Format Selection** in the editor menu to override detection
3. If the extension should be supported, file a bug report with the file name and first few lines of content

### Detection returns the wrong format

**Cause**: Multiple formats share the same extension (e.g., `.txt` could be plain text or Todo.txt), or the content matches a more general pattern first.

**Fix**:
1. Rename the file with a more specific extension if possible
2. Use manual format selection to override
3. Format detection priority is defined in `FormatRegistry.formats` -- more specific formats are checked before general ones

### HTML preview shows raw markup

**Cause**: The parser failed to produce HTML output, or the format does not support preview.

**Fix**:
1. Check for parsing errors in the editor's status bar
2. Some formats (Binary, Key-Value) have limited preview support
3. For Markdown, ensure the Flexmark library is available (it is bundled with all platform builds)
4. Clear the parsed document cache and try again

### Markdown tables or extensions not rendering

**Cause**: The Markdown extension (tables, strikethrough, task lists, etc.) may not be enabled.

**Fix**: Yole enables all 16+ Flexmark extensions by default, including tables, task lists, strikethrough, footnotes, and math. If a specific extension is not rendering:
1. Verify the syntax matches the extension's expected format
2. For GFM tables, ensure the header row has the `---` separator
3. For math, use `$...$` for inline and `$$...$$` for display math

### RST parser does not report errors

**Cause**: The reStructuredText parser's `parse()` method does not populate the `errors` field in `ParsedDocument`. Validation errors are only available through `validate()`.

**Fix**: Call `validate()` separately on the parsed document to get error information. This is by design -- parsing and validation are separate steps for RST.

---

## Platform-Specific Issues

### Android: "Permission denied" when accessing files

**Cause**: Storage permissions have not been granted, or the app is targeting scoped storage (Android 11+).

**Fix**:
1. Go to Android Settings > Apps > Yole > Permissions > Storage and grant access
2. On Android 11+, use the in-app file picker (SAF) instead of direct file paths
3. For the Notebook folder, select a directory within the app's accessible scope

### Android: App crashes on startup

**Cause**: Corrupted preferences, incompatible upgrade, or out-of-memory condition.

**Fix**:
1. Clear app data: Android Settings > Apps > Yole > Storage > Clear data
2. If the issue persists, uninstall and reinstall
3. Check logcat for the specific crash: `adb logcat -s "digital.vasic.yole"`

### Desktop: "No suitable JRE found" on startup

**Cause**: JDK 21 is not installed or not on the PATH.

**Fix**:
1. Install JDK 21 (Eclipse Temurin recommended)
2. Set `JAVA_HOME` to the JDK 21 directory
3. Verify: `java -version` should show 21.x

### Desktop: High memory usage

**Cause**: Multiple large documents open, or the JVM heap is not being garbage collected.

**Fix**:
1. Close documents that are not in use
2. Clear the document cache: **Settings** > **Advanced** > **Clear Cache**
3. Increase JVM heap if needed (see [Performance Tuning](PERFORMANCE_TUNING.md))

### Web: CORS errors when connecting to cloud storage

**Cause**: Browser security prevents direct API calls to cloud storage servers from a different origin.

**Fix**:
1. Cloud providers (Dropbox, Google Drive, OneDrive) support CORS for their APIs -- ensure your OAuth client is configured for web use
2. WebDAV, FTP, and SMB require a proxy server for web access
3. For development, use a CORS proxy or configure the target server to include appropriate `Access-Control-Allow-Origin` headers

### Web: Large Wasm binary causes slow initial load

**Cause**: The Kotlin/Wasm binary can be several MB.

**Fix**:
1. Use production builds with optimization (`./gradlew :webApp:wasmJsBrowserProductionRun`)
2. Enable gzip or brotli compression on your web server
3. The PWA service worker caches the binary for subsequent loads

---

## Database Issues

### "Database locked" error

**Cause**: Multiple concurrent write operations to the SQLite database.

**Fix**:
1. Yole serializes database access through `NetworkStorageDatabase` methods -- this error should be rare
2. If it occurs, restart the app to release the lock
3. Run `vacuum()` on the database to compact it

### Stale sync status after a crash

**Cause**: The app crashed during a sync operation, leaving the status as SYNCING.

**Fix**:
1. Go to **Settings** > **Cloud Storage** and select the affected provider
2. Tap **Force Sync** to reset sync status and re-sync
3. Alternatively, call `clearAll()` on the database to reset all metadata

---

## Testing Issues

### Tests fail with "Unresolved reference"

**Cause**: Missing imports, or the test references a class from an extracted module that is not properly wired.

**Fix**:
1. Verify `includeBuild()` directives in `settings.gradle.kts`
2. Run `./gradlew :shared:desktopTest` first to verify shared module tests compile
3. Check that facade bridges (typealias files) exist for the referenced types

### Test timeouts

**Cause**: Stress tests or integration tests may take longer than the default timeout.

**Fix**:
1. Increase the test timeout in the test class:
   ```kotlin
   @Test
   fun mySlowTest() = runTest(timeout = 60.seconds) { ... }
   ```
2. Run tests with more memory: see "Out of memory during build" above
3. Disable stress tests for quick iteration: `./gradlew test --tests "!*stress*"`

### Mock HTTP tests fail with CancellationException

**Cause**: Flow transparency requirement -- `CancellationException` must be rethrown, not caught.

**Fix**: All service files should include:
```kotlin
catch (e: Exception) {
    if (e is kotlin.coroutines.cancellation.CancellationException) throw e
    // handle other exceptions
}
```
This pattern is already applied to all 5 protocol service files that use Ktor.

---

## Getting More Help

1. **Search existing issues**: [GitHub Issues](https://github.com/vasic-digital/Yole/issues)
2. **Ask a question**: [GitHub Discussions](https://github.com/vasic-digital/Yole/discussions)
3. **Report a bug**: [New Issue](https://github.com/vasic-digital/Yole/issues/new)
4. **Check logs**: Enable debug logging in Settings for detailed error output

When reporting an issue, include:
- Yole version and platform (Android/Desktop/iOS/Web)
- Steps to reproduce
- Expected vs. actual behavior
- Relevant log output or error messages

---

*Last updated: March 7, 2026*
