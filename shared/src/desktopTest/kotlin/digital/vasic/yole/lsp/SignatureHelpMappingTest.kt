/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 3: SignatureHelpMappingTest — unit tests for
 * the mapLspSignatureHelpToYole internal helper.
 *
 * Per Phase 0 §1 research, SignatureInformation.documentation is
 * Either<String, MarkupContent> in LSP4J. Two cases must be verified:
 *   1. Plain-string documentation (left side of Either).
 *   2. MarkupContent documentation (right side of Either).
 *
 * LSP4J 1.0.0 constructors:
 *   SignatureInformation(String label, String doc, List<ParameterInformation>)
 *   SignatureInformation(String label, MarkupContent doc, List<ParameterInformation>)
 *   ParameterInformation(String label, String doc)
 *   SignatureHelp(List<SignatureInformation>, Integer, Integer)
 *
 * Mutation procedure (CONST-035):
 *   1. Stub mapLspSignatureHelpToYole to always return null.
 *      → stringDocumentation_isExtracted FAILS (assertNotNull).
 *      → markupContentDocumentation_isExtracted FAILS (assertNotNull).
 *   2. Revert; confirm both PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): JVM actual helper under test; lives here.
 *   - Android: mapLspSignatureHelpToYoleAndroid mirrors logic; covered by androidUnitTest.
 *   - iOS/Wasm: signatureHelp() returns null stubs; no mapping occurs.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MarkupKind
import org.eclipse.lsp4j.ParameterInformation as LspParameterInformation
import org.eclipse.lsp4j.SignatureInformation as LspSignatureInformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for [mapLspSignatureHelpToYole].
 *
 * Tests call the production helper directly (internal visibility, same package).
 * No mock server needed — LSP4J data classes are constructed inline using
 * LSP4J 1.0.0 constructors that set documentation directly.
 */
class SignatureHelpMappingTest {

    /**
     * When SignatureInformation.documentation is a plain string (set via the
     * String-doc constructor → stored as Either.forLeft internally), the mapper
     * must extract the raw string verbatim into [SignatureInformation.documentation].
     *
     * Mutation: stub returns null → assertNotNull FAILS.
     */
    @Test
    fun stringDocumentation_isExtracted() {
        // LSP4J 1.0.0: SignatureInformation(label, String doc, List<ParameterInformation>)
        // The String-doc constructor stores documentation as Either.forLeft(doc).
        val param = LspParameterInformation("name: String", "The name to greet.")
        val lspSig = LspSignatureInformation(
            "fun greet(name: String): String",
            "Greets the user by name.",
            listOf(param),
        )
        val lspHelp = org.eclipse.lsp4j.SignatureHelp(
            listOf(lspSig),
            /* activeSignature = */ 0,
            /* activeParameter = */ 0,
        )

        val result = mapLspSignatureHelpToYole(lspHelp)

        assertNotNull(result, "Expected non-null SignatureHelp")
        assertEquals(1, result.signatures.size, "Expected 1 signature")
        val sig = result.signatures[0]
        assertEquals("fun greet(name: String): String", sig.label)
        assertEquals("Greets the user by name.", sig.documentation, "Plain-string doc must be preserved verbatim")
        assertEquals(1, sig.parameters.size, "Expected 1 parameter")
        assertEquals("name: String", sig.parameters[0].label)
        assertEquals("The name to greet.", sig.parameters[0].documentation)
        assertEquals(0, result.activeSignature)
        assertEquals(0, result.activeParameter)
    }

    /**
     * When SignatureInformation.documentation is a MarkupContent (set via the
     * MarkupContent-doc constructor → stored as Either.forRight internally), the
     * mapper must extract [MarkupContent.value] into [SignatureInformation.documentation].
     *
     * Mutation: stub returns null → assertNotNull FAILS.
     */
    @Test
    fun markupContentDocumentation_isExtracted() {
        val doc = MarkupContent(MarkupKind.MARKDOWN, "## greet\n\nReturns a greeting string.")
        // LSP4J 1.0.0: SignatureInformation(label, MarkupContent doc, List<ParameterInformation>)
        val lspSig = LspSignatureInformation(
            "fun greet(name: String): String",
            doc,
            emptyList(),
        )
        val lspHelp = org.eclipse.lsp4j.SignatureHelp(
            listOf(lspSig),
            /* activeSignature = */ 0,
            /* activeParameter = */ 0,
        )

        val result = mapLspSignatureHelpToYole(lspHelp)

        assertNotNull(result, "Expected non-null SignatureHelp")
        val sig = result.signatures[0]
        assertEquals(
            "## greet\n\nReturns a greeting string.",
            sig.documentation,
            "MarkupContent.value must be extracted as documentation",
        )
        // No parameters in this sig.
        assertEquals(0, sig.parameters.size)
    }
}
