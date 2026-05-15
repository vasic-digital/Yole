/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: LspServerRegistry tests.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub LspServerRegistry.forLanguage to always return null.
 *   2. ./gradlew :shared:desktopTest --tests "digital.vasic.yole.lsp.LspServerRegistryTest"
 *   3. Expect: at least 2 tests FAIL.
 *   4. Revert; confirm 4/4 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LspServerRegistryTest {

    @AfterTest
    fun clearCache() {
        LspServerRegistry.clearCacheForTest()
    }

    @Test
    fun all_15_servers_registered() {
        val registry = LspServerRegistry.default()
        assertEquals(15, registry.allSpecs().size)
    }

    @Test
    fun rust_returns_rustAnalyzer_spec() {
        val spec = LspServerRegistry.default().forLanguage("rust")
        assertNotNull(spec)
        assertEquals("rust-analyzer", spec.executable)
        assertEquals(listOf("Cargo.toml"), spec.projectMarkers)
    }

    @Test
    fun clangd_serves_both_c_and_cpp() {
        val cSpec = LspServerRegistry.default().forLanguage("c")
        val cppSpec = LspServerRegistry.default().forLanguage("cpp")
        assertNotNull(cSpec)
        assertNotNull(cppSpec)
        assertEquals(cSpec.executable, cppSpec.executable)
        assertEquals("clangd", cSpec.executable)
    }

    @Test
    fun unsupported_lang_returns_null() {
        assertNull(LspServerRegistry.default().forLanguage("nonexistent-lang-xyz"))
    }
}
