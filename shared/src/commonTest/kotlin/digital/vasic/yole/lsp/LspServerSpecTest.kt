/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: LspServerSpec round-trip tests.
 *
 * Mutation procedure (CONST-035):
 *   1. Remove `@Serializable` from LspServerSpec.
 *   2. ./gradlew :shared:desktopTest --tests "digital.vasic.yole.lsp.LspServerSpecTest"
 *   3. Expect: compile failure (no auto-generated serializer).
 *   4. Revert; re-run; confirm 3/3 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LspServerSpecTest {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @Test
    fun parses_minimal_serverJson() {
        val raw = """
            {
              "langIds": ["rust"],
              "executable": "rust-analyzer",
              "args": [],
              "projectMarkers": ["Cargo.toml"],
              "initOptions": {}
            }
        """.trimIndent()
        val spec = json.decodeFromString(LspServerSpec.serializer(), raw)
        assertEquals(listOf("rust"), spec.langIds)
        assertEquals("rust-analyzer", spec.executable)
        assertEquals(listOf("Cargo.toml"), spec.projectMarkers)
    }

    @Test
    fun parses_multiLangId_serverJson() {
        val raw = """
            {
              "langIds": ["c", "cpp"],
              "executable": "clangd",
              "args": ["--background-index"],
              "projectMarkers": ["compile_commands.json", "CMakeLists.txt"],
              "initOptions": {}
            }
        """.trimIndent()
        val spec = json.decodeFromString(LspServerSpec.serializer(), raw)
        assertEquals(listOf("c", "cpp"), spec.langIds)
        assertEquals(listOf("--background-index"), spec.args)
    }

    @Test
    fun roundtrip_preservesFields() {
        val original = LspServerSpec(
            langIds = listOf("go"),
            executable = "gopls",
            args = listOf("serve"),
            projectMarkers = listOf("go.mod"),
            initOptions = kotlinx.serialization.json.JsonObject(emptyMap()),
        )
        val raw = json.encodeToString(LspServerSpec.serializer(), original)
        val parsed = json.decodeFromString(LspServerSpec.serializer(), raw)
        assertEquals(original, parsed)
    }

    @Test
    fun parses_args_correctly() {
        val raw = """
            {
              "langIds": ["typescript"],
              "executable": "typescript-language-server",
              "args": ["--stdio"],
              "projectMarkers": ["tsconfig.json", "package.json"],
              "initOptions": {}
            }
        """.trimIndent()
        val spec = json.decodeFromString(LspServerSpec.serializer(), raw)
        assertTrue(spec.args.contains("--stdio"))
        assertEquals(listOf("tsconfig.json", "package.json"), spec.projectMarkers)
    }
}
