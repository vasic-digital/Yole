/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: Android JVM actual for `readSnippetResource`.
 * Bundled snippet JSON files live in
 * `shared/src/commonMain/resources/snippets/<lang>/snippets.json`
 * and are packaged into the .aar/.apk classpath at the same path by
 * the Kotlin/Compose Multiplatform Gradle pipeline.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * Load a bundled snippets.json via the Android JVM classloader.
 * The Kotlin/Compose Multiplatform Gradle pipeline packages
 * `shared/src/commonMain/resources/` recursively into the consumer
 * module's classpath, so the same `snippets/<lang>/snippets.json`
 * lookup works on Android.
 *
 * Returns `null` if the resource is not found (missing snippet bundle
 * is benign — [SnippetRegistry] degrades to an empty list).
 */
actual fun readSnippetResource(path: String): String? =
    object {}.javaClass.classLoader
        ?.getResourceAsStream(path)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
