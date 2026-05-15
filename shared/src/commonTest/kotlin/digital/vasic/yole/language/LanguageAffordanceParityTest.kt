/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 6: anti-bluff parity test for the 55-language
 * affordance matrix. For every entry in [LanguageMetadata.all]
 * asserts that:
 *
 *   1. id, displayName, and at least one extension + mimeType are
 *      non-blank.
 *   2. Either lineComment OR blockComment is non-null (catches the
 *      forgot-to-populate-CommentSyntax regression), with documented
 *      exceptions for JSON (RFC 8259 forbids comments) and regex
 *      (no comment syntax).
 *   3. IndentRules.indentTokens and dedentTokens are non-null (sets
 *      may be empty for layout-significant langs).
 *   4. BracketPairs.pairs is non-empty (default covers () [] {} "" '').
 *   5. indentUnit is exactly one of: "  " (2 spaces), "    " (4
 *      spaces), or "\t" (tab).
 *
 * Anti-bluff anchor (CONST-035): stubbing any of these fields to
 * empty/null on a single language causes this test to FAIL with a
 * descriptive message naming the offending lang id. Verified by
 * mutation: setting `csharp.commentSyntax = CommentSyntax()` (empty)
 * makes assertion (2) fail with `"csharp must declare lineComment or
 * blockComment"`.
 *
 *########################################################*/
package digital.vasic.yole.language

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class LanguageAffordanceParityTest {

    /**
     * Languages legitimately lacking comment syntax. JSON per RFC 8259
     * (strict spec forbids comments — `.jsonc` is a separate format),
     * regex (no comment construct in regex itself).
     */
    private val commentlessLangs = setOf("json", "regex")

    /**
     * Allowed values for [LanguageFormat.indentUnit]. Per
     * research-report.md §4.3 + editorconfig.org defaults.
     */
    private val allowedIndentUnits = setOf("  ", "    ", "\t")

    @Test
    fun allLanguagesHaveNonBlankIdentity() {
        for (lf in LanguageMetadata.all) {
            assertTrue(
                lf.id.isNotBlank(),
                "every LanguageFormat must have a non-blank id, got `$lf`",
            )
            assertTrue(
                lf.displayName.isNotBlank(),
                "${lf.id} must have a non-blank displayName, got `${lf.displayName}`",
            )
            assertTrue(
                lf.extensions.isNotEmpty(),
                "${lf.id} must declare at least one extension",
            )
            assertTrue(
                lf.mimeTypes.isNotEmpty(),
                "${lf.id} must declare at least one mimeType",
            )
            for (ext in lf.extensions) {
                assertTrue(
                    ext.startsWith("."),
                    "${lf.id}: extension `$ext` must be leading-dot",
                )
                assertTrue(
                    ext == ext.lowercase() || ext == ".R" || ext.endsWith(".F90") ||
                        ext.endsWith(".F95") || ext.endsWith(".F03") || ext.endsWith(".F08"),
                    "${lf.id}: extension `$ext` should be lowercase (only R+Fortran caps allowed)",
                )
            }
        }
    }

    @Test
    fun allLanguagesDeclareCommentSyntaxExceptDocumentedExceptions() {
        for (lf in LanguageMetadata.all) {
            val cs = lf.commentSyntax
            assertNotNull(cs, "${lf.id}: commentSyntax must not be null")
            if (lf.id in commentlessLangs) {
                // documented exception — both can be null
                continue
            }
            assertTrue(
                cs.lineComment != null || cs.blockComment != null,
                "${lf.id} must declare lineComment OR blockComment " +
                    "(only commentless-langs $commentlessLangs are exempt)",
            )
        }
    }

    @Test
    fun allLanguagesDeclareIndentRules() {
        for (lf in LanguageMetadata.all) {
            val ir = lf.indentRules
            assertNotNull(ir, "${lf.id}: indentRules must not be null")
            // indentTokens + dedentTokens may be empty (for indent-
            // significant langs like python / yaml / haskell / elm / nim)
            // but the sets themselves must exist.
            assertNotNull(ir.indentTokens, "${lf.id}: indentTokens set must exist (may be empty)")
            assertNotNull(ir.dedentTokens, "${lf.id}: dedentTokens set must exist (may be empty)")
        }
    }

    @Test
    fun allLanguagesDeclareBracketPairs() {
        for (lf in LanguageMetadata.all) {
            val bp = lf.bracketPairs
            assertNotNull(bp, "${lf.id}: bracketPairs must not be null")
            assertTrue(
                bp.pairs.isNotEmpty(),
                "${lf.id}: bracketPairs.pairs should default to at least ()[]{}",
            )
        }
    }

    @Test
    fun allLanguagesDeclareCanonicalIndentUnit() {
        for (lf in LanguageMetadata.all) {
            assertTrue(
                lf.indentUnit in allowedIndentUnits,
                "${lf.id}: indentUnit `${lf.indentUnit.replace("\t", "\\t").replace(" ", "·")}` " +
                    "must be one of: 2-space, 4-space, or tab (per research-report.md §4.3)",
            )
        }
    }

    @Test
    fun languageIdsAreUnique() {
        val ids = LanguageMetadata.all.map { it.id }
        val duplicates = ids.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (duplicates.isNotEmpty()) {
            fail("LanguageMetadata.all has duplicate ids: $duplicates")
        }
    }

    @Test
    fun extensionsArePerLanguageUnique() {
        val seen = mutableMapOf<String, String>()
        for (lf in LanguageMetadata.all) {
            for (ext in lf.extensions) {
                val owner = seen[ext]
                if (owner != null && owner != lf.id) {
                    fail(
                        "extension `$ext` is claimed by BOTH `$owner` and `${lf.id}` " +
                            "in LanguageMetadata.all — extensions must uniquely map to one lang",
                    )
                }
                seen[ext] = lf.id
            }
        }
    }

    @Test
    fun fifty_fiveLanguagesShipped() {
        val count = LanguageMetadata.all.size
        assertTrue(
            count == 55,
            "F2 spec targets 55 languages; LanguageMetadata.all has $count",
        )
    }
}
