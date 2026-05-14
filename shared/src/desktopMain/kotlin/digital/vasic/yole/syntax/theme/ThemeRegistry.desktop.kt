/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: Desktop JVM actual for `readBuiltinTheme`.
 * Built-in theme JSONs live in `shared/src/commonMain/resources/themes/builtin/`
 * and end up on the runtime classpath under `themes/builtin/`.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * Load a built-in theme JSON via the desktop JVM classloader.
 * Resources from `shared/src/commonMain/resources` are merged into the
 * `desktop` compilation's main resources, so the same path works.
 */
actual fun readBuiltinTheme(filename: String): String {
    val path = "themes/builtin/$filename"
    val stream = object {}.javaClass.classLoader?.getResourceAsStream(path)
        ?: error(
            "Built-in theme resource `$path` not found on the desktop classpath. " +
                "Ensure shared/src/commonMain/resources/$path is present."
        )
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
