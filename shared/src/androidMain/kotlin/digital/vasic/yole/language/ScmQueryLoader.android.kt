/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: Android actual for `readScmResource`.
 * Vendored `.scm` query files live in
 * `shared/src/commonMain/resources/grammars/<lang>/<queryName>.scm`
 * and are packaged into the .aar/.apk classpath at the same path.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * Load a bundled `.scm` query via the Android JVM classloader.
 * The Kotlin/Compose Multiplatform Gradle pipeline packages
 * `shared/src/commonMain/resources/` recursively into the consumer
 * module's classpath, so the same `grammars/<lang>/<queryName>.scm`
 * lookup works on Android.
 */
actual fun readScmResource(path: String): String {
    val stream = object {}.javaClass.classLoader?.getResourceAsStream(path)
        ?: error(
            "Tree-Sitter query resource `$path` not found on the Android classpath. " +
                "Ensure shared/src/commonMain/resources/$path is present.",
        )
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
