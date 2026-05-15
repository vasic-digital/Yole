/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: LspServerRegistry — central authority
 * mapping Yole langId → LspServerSpec. Specs deserialized from
 * shared/src/commonMain/resources/lsp-servers/<langId>/server.json.
 *
 * Resource access uses `expect fun readLspServerResource` mirroring
 * iter-58 ScmQueryLoader + iter-60 SnippetRegistry patterns.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.serialization.json.Json

/**
 * Read a bundled LSP server.json resource, or null if absent.
 * Mirrors iter-60 SnippetRegistry.readSnippetResource.
 */
expect fun readLspServerResource(path: String): String?

/**
 * The 15 langIds Yole bundles LSP support for in v1. Order does not matter.
 */
private val BUNDLED_LANG_IDS = listOf(
    "rust", "go", "c", "cpp", "markdown", "lua", "zig", "python",
    "elixir", "haskell", "typescript", "bash", "yaml", "java", "kotlin",
)

/**
 * Authority for resolving langId → LspServerSpec. Specs are loaded
 * lazily on first access and cached for the process lifetime.
 *
 * Note: `c` and `cpp` share the same underlying spec (both point to
 * clangd). The shared spec lives at lsp-servers/c/server.json and is
 * resolved twice via two entries in the spec cache.
 */
class LspServerRegistry private constructor(
    private val specs: Map<String, LspServerSpec>,
) {
    fun forLanguage(langId: String): LspServerSpec? = specs[langId]
    fun allSpecs(): Collection<LspServerSpec> = specs.values.toSet()

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = false }

        private var cached: LspServerRegistry? = null

        fun default(): LspServerRegistry {
            cached?.let { return it }
            val loaded = mutableMapOf<String, LspServerSpec>()
            for (langId in BUNDLED_LANG_IDS) {
                val path = "lsp-servers/$langId/server.json"
                val raw = readLspServerResource(path) ?: continue
                val spec = runCatching {
                    json.decodeFromString(LspServerSpec.serializer(), raw)
                }.getOrNull() ?: continue
                // A spec may declare multiple langIds (clangd → c + cpp).
                // Register the spec under each declared langId.
                for (declaredLang in spec.langIds) {
                    loaded[declaredLang] = spec
                }
                // Also register under the directory name in case it differs
                // from the spec's declared langIds (defensive).
                loaded[langId] = spec
            }
            val registry = LspServerRegistry(loaded.toMap())
            cached = registry
            return registry
        }

        internal fun clearCacheForTest() {
            cached = null
        }
    }
}
