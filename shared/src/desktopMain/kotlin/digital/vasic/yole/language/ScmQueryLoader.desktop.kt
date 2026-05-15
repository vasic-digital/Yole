/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: Desktop JVM actual for `readScmResource`.
 * Vendored `.scm` query files live in
 * `shared/src/commonMain/resources/grammars/<lang>/<queryName>.scm`
 * and end up on the runtime classpath under the same path.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * Load a bundled `.scm` query via the desktop JVM classloader.
 * Resources from `shared/src/commonMain/resources` are merged into the
 * `desktop` compilation's main resources, so the same path works.
 */
actual fun readScmResource(path: String): String {
    val stream = object {}.javaClass.classLoader?.getResourceAsStream(path)
        ?: error(
            "Tree-Sitter query resource `$path` not found on the desktop classpath. " +
                "Ensure shared/src/commonMain/resources/$path is present.",
        )
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
