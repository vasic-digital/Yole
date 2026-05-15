/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: Desktop JVM actual for `readSnippetResource`.
 * Bundled snippet JSON files live in
 * `shared/src/commonMain/resources/snippets/<lang>/snippets.json`
 * and end up on the runtime classpath under the same path.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * Load a bundled snippets.json via the desktop JVM classloader.
 * Resources from `shared/src/commonMain/resources` are merged into the
 * desktop compilation's main resources, so the same path works.
 *
 * Returns `null` if the resource is not found (missing snippet bundle
 * is benign — [SnippetRegistry] degrades to an empty list).
 */
actual fun readSnippetResource(path: String): String? =
    object {}.javaClass.classLoader
        ?.getResourceAsStream(path)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
