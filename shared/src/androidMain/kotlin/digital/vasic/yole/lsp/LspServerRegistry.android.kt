/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: Android JVM actual for readLspServerResource.
 * Bundled server.json files live in
 * `shared/src/commonMain/resources/lsp-servers/<langId>/server.json`
 * and are packaged into the .aar/.apk classpath at the same path by
 * the Kotlin/Compose Multiplatform Gradle pipeline.
 *
 * iter-60 Phase 9 pattern: Robolectric's sandboxed classloader does not
 * always expose resources through `object {}.javaClass.classLoader`
 * because KMP commonMain resources are placed on the test classpath via
 * a separate class-path entry that Robolectric's SandboxClassLoader
 * delegates to the system (parent) classloader rather than the sandbox.
 * Fix: probe three classloaders in order —
 *   1. Thread context classloader (Robolectric sets this to the
 *      sandboxed loader that sees merged test resources).
 *   2. The anonymous-object classloader (standard JVM production path).
 *   3. ClassLoader.getSystemClassLoader() (last-resort fallback).
 * The first non-null stream wins. Returns null if none finds the file.
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Load a bundled server.json via the Android JVM classloader.
 * The Kotlin/Compose Multiplatform Gradle pipeline packages
 * `shared/src/commonMain/resources/` recursively into the consumer
 * module's classpath, so the same `lsp-servers/<langId>/server.json`
 * lookup works on Android in both production and Robolectric tests.
 *
 * Returns `null` if the resource is not found (missing server spec
 * is benign — [LspServerRegistry] silently skips the langId).
 */
actual fun readLspServerResource(path: String): String? {
    val loaders = listOf(
        Thread.currentThread().contextClassLoader,
        object {}.javaClass.classLoader,
        ClassLoader.getSystemClassLoader(),
    )
    for (loader in loaders) {
        val stream = loader?.getResourceAsStream(path) ?: continue
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
    return null
}
