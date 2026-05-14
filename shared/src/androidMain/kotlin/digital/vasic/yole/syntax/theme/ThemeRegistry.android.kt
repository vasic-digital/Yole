/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: Android actual for `readBuiltinTheme`.
 * Built-in theme JSONs live in `shared/src/commonMain/resources/themes/builtin/`
 * and are packaged into the .aar/.apk classpath at `themes/builtin/`.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * Load a built-in theme JSON via the Android JVM classloader.
 * The Kotlin/Compose Multiplatform Gradle pipeline packages
 * `shared/src/commonMain/resources/` recursively into the consumer module's
 * classpath, so the same `themes/builtin/<filename>` lookup works on Android.
 */
actual fun readBuiltinTheme(filename: String): String {
    val path = "themes/builtin/$filename"
    val stream = object {}.javaClass.classLoader?.getResourceAsStream(path)
        ?: error(
            "Built-in theme resource `$path` not found on the Android classpath. " +
                "Ensure shared/src/commonMain/resources/$path is present."
        )
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
