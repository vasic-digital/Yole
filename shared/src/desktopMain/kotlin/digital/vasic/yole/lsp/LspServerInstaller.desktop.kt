/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 3: Desktop JVM actual for LspServerInstaller.
 *
 * User-data layout:
 *   macOS:   ~/Library/Application Support/yole/lsp-servers/<lang>/
 *   Linux:   ~/.local/share/yole/lsp-servers/<lang>/
 *   Windows: %LOCALAPPDATA%/yole/lsp-servers/<lang>/
 *
 * Binary source on classpath: lsp-bundles/<langId>/<executable>
 * (Phase 7 populates these resources; Phase 3 wires the extraction logic.)
 *
 * Idempotent: if the target file already exists and is executable,
 * extraction is skipped and the existing path is returned immediately.
 *
 * Detekt compliance:
 *   - CancellationException is always rethrown (SwallowedException rule).
 *   - No GlobalScope usage.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.CancellationException
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

/**
 * Desktop JVM actual for [LspServerInstaller].
 *
 * Extracts the bundled LSP binary (from classpath path
 * `lsp-bundles/<langId>/<executable>`) to the OS-appropriate user data
 * directory, sets the executable bit, and returns the target [Path].
 *
 * Returns [Result.failure] wrapping [LspInstallError.ExtractionFailed]
 * when the bundled binary resource is absent from the classpath (expected
 * behaviour until Phase 7 acquires the binaries).
 */
actual class LspServerInstaller actual constructor(private val spec: LspServerSpec) {

    actual suspend fun ensureInstalled(): Result<Path> {
        val langId = spec.langIds.firstOrNull() ?: return Result.failure(
            LspInstallError.ExtractionFailed(langId = "unknown")
        )
        return try {
            val targetDir = resolveUserDataDir(langId)
            val targetFile = File(targetDir, spec.executable)
            val target = targetFile.absolutePath.toPath()

            // Idempotent: skip extraction if already present and executable.
            if (targetFile.exists() && targetFile.canExecute()) {
                return Result.success(target)
            }

            val resourcePath = "lsp-bundles/$langId/${spec.executable}"
            val stream = findResourceStream(resourcePath)
                ?: return Result.failure(LspInstallError.ExtractionFailed(langId = langId))

            targetDir.mkdirs()
            stream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // chmod +x — owner + group + others (false = not owner-only)
            targetFile.setExecutable(true, false)

            Result.success(target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(LspInstallError.ExtractionFailed(langId = spec.langIds.firstOrNull() ?: "unknown", rootCause = e))
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the OS-appropriate user-data directory for this langId.
     * Returns a [File] (not yet guaranteed to exist — caller calls mkdirs()).
     */
    private fun resolveUserDataDir(langId: String): File {
        val os = System.getProperty("os.name", "").lowercase()
        val base: String = when {
            os.contains("mac") -> {
                val home = System.getProperty("user.home", "")
                "$home/Library/Application Support/yole/lsp-servers/$langId"
            }
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                    ?: System.getProperty("user.home", "") + "/AppData/Local"
                "$localAppData/yole/lsp-servers/$langId"
            }
            else -> {
                // Linux / BSD / unknown Unix
                val xdgData = System.getenv("XDG_DATA_HOME")
                    ?: (System.getProperty("user.home", "") + "/.local/share")
                "$xdgData/yole/lsp-servers/$langId"
            }
        }
        return File(base)
    }

    /**
     * Probes three classloaders for [resourcePath], returning the first
     * non-null [java.io.InputStream] found.  Mirrors the Android classloader
     * probe pattern established in LspServerRegistry.android.kt so that
     * resources are visible under both production and desktopTest classpaths.
     */
    private fun findResourceStream(resourcePath: String): java.io.InputStream? {
        val loaders = listOf(
            Thread.currentThread().contextClassLoader,
            LspServerInstaller::class.java.classLoader,
            ClassLoader.getSystemClassLoader(),
        )
        for (loader in loaders) {
            val stream = loader?.getResourceAsStream(resourcePath)
            if (stream != null) return stream
        }
        return null
    }
}
