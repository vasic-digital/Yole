/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: anti-bluff registry tests.
 *#######################################################*/
package digital.vasic.yole.language

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LanguageRegistryTest {
    @Test
    fun markdownIsRegistered() = runBlocking<Unit> {
        val lf = LanguageRegistry.get("markdown")
        assertNotNull(lf, "markdown LanguageFormat must be present")
        assertEquals("Markdown", lf.displayName)
        assertEquals(true, lf.extensions.contains(".md"))
    }

    @Test
    fun detectByFilename_handlesKotlin() = runBlocking<Unit> {
        val lf = LanguageRegistry.detectByFilename("HelloWorld.kt")
        assertNotNull(lf)
        assertEquals("kotlin", lf.id)
    }

    @Test
    fun detectByFilename_returnsNullOnUnknownExtension() = runBlocking<Unit> {
        assertNull(LanguageRegistry.detectByFilename("strange.xyznotreal"))
    }

    @Test
    fun all_listsAtLeastMarkdown() = runBlocking<Unit> {
        val ids = LanguageRegistry.all().map { it.id }
        assertEquals(true, "markdown" in ids,
            "LanguageMetadata.all must contain markdown (the v1 anchor)")
    }
}
