/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: Desktop JVM actual for readLspServerResource.
 * Bundled server.json files live in
 * `shared/src/commonMain/resources/lsp-servers/<langId>/server.json`
 * and end up on the runtime classpath under the same path.
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Load a bundled server.json via the desktop JVM classloader.
 * Resources from `shared/src/commonMain/resources` are merged into the
 * desktop compilation's main resources, so the same path works.
 *
 * Returns `null` if the resource is not found (missing server spec
 * is benign — [LspServerRegistry] silently skips the langId).
 */
actual fun readLspServerResource(path: String): String? =
    object {}.javaClass.classLoader
        ?.getResourceAsStream(path)
        ?.bufferedReader(Charsets.UTF_8)
        ?.use { it.readText() }
