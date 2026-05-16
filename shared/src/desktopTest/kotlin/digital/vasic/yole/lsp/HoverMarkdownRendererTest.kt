/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 4: HoverMarkdownRenderer tests.
 *
 * The plan (§4.5) contained an INTENTIONAL bug in test 3:
 *   `HoverBlock.render("# Title")`  →  `HoverMarkdownRenderer.render("# Title")`
 * Fixed here during implementation (TDD pedagogy moment per plan note).
 *
 * Mutation procedure (CONST-035):
 *   1. In HoverMarkdownRenderer.desktop.kt stub render() to always return:
 *        if (markdown.isEmpty()) emptyList()
 *        else listOf(HoverBlock.FallbackText(markdown))
 *   2. Re-run desktopTest — the following 4+ tests FAIL:
 *        paragraph_returnsSingleParagraph      (expects Paragraph, gets FallbackText)
 *        heading_extractsLevelAndText          (expects Heading, gets FallbackText)
 *        fencedCodeBlock_extractsLangAndCode   (expects CodeBlock, gets FallbackText)
 *        mixedContent_orderedCorrectly         (expects Heading+Paragraph+CodeBlock)
 *   3. Revert stub → all 8 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: desktopTest — JVM actual under test, lives here.
 *   - Android: identical Flexmark walker in androidMain; covered by
 *              androidUnitTest when ANDROID_SDK_ROOT is available.
 *   - iOS/Wasm: FallbackText stubs; no separate test needed (trivial
 *               single-line impl covered by the two stub variants' own
 *               contract: non-empty → FallbackText, empty → emptyList).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HoverMarkdownRendererTest {

    /**
     * Empty string → empty list on all platforms.
     *
     * Mutation: stub returns emptyList() for empty → PASS (coincidental).
     * Covered by the other tests' failures in the mutation procedure.
     */
    @Test
    fun emptyInput_returnsEmpty() {
        assertEquals(emptyList(), HoverMarkdownRenderer.render(""))
    }

    /**
     * A single paragraph produces exactly one [HoverBlock.Paragraph]
     * whose text matches the input (trimmed).
     *
     * Mutation: stub returns FallbackText → assertIs<Paragraph> FAILS.
     */
    @Test
    fun paragraph_returnsSingleParagraph() {
        val blocks = HoverMarkdownRenderer.render("hello world")
        assertEquals(1, blocks.size, "Expected exactly 1 block")
        assertTrue(
            blocks[0] is HoverBlock.Paragraph,
            "Expected Paragraph but got ${blocks[0]::class.simpleName}",
        )
        assertEquals(
            "hello world",
            (blocks[0] as HoverBlock.Paragraph).text,
            "Paragraph text must match trimmed input",
        )
    }

    /**
     * An ATX heading produces a [HoverBlock.Heading] with the correct
     * level and trimmed text.
     *
     * Plan test 3 had `HoverBlock.render(...)` — fixed to
     * `HoverMarkdownRenderer.render(...)` (intentional typo per plan note).
     *
     * Mutation: stub returns FallbackText → assertIs<Heading> FAILS.
     */
    @Test
    fun heading_extractsLevelAndText() {
        val blocks = HoverMarkdownRenderer.render("# Title")
        assertEquals(1, blocks.size, "Expected exactly 1 block")
        assertTrue(
            blocks[0] is HoverBlock.Heading,
            "Expected Heading but got ${blocks[0]::class.simpleName}",
        )
        val heading = blocks[0] as HoverBlock.Heading
        assertEquals(1, heading.level, "Heading level must be 1")
        assertEquals("Title", heading.text, "Heading text must be 'Title'")
    }

    /**
     * A fenced code block with a language tag produces
     * [HoverBlock.CodeBlock] with the correct lang and code content.
     *
     * Mutation: stub returns FallbackText → assertIs<CodeBlock> FAILS.
     */
    @Test
    fun fencedCodeBlock_extractsLangAndCode() {
        val md = "```rust\nfn main() {}\n```"
        val blocks = HoverMarkdownRenderer.render(md)
        assertTrue(
            blocks.any { it is HoverBlock.CodeBlock },
            "Expected at least one CodeBlock",
        )
        val code = blocks.first { it is HoverBlock.CodeBlock } as HoverBlock.CodeBlock
        assertEquals("rust", code.lang, "Language must be 'rust'")
        assertTrue(
            code.code.contains("fn main()"),
            "Code content must contain 'fn main()', got: ${code.code}",
        )
    }

    /**
     * A 4-space indented code block has no language → [HoverBlock.CodeBlock]
     * with `lang == null`.
     *
     * Mutation: stub returns FallbackText → no CodeBlock found → FAILS.
     */
    @Test
    fun indentedCodeBlock_noLang() {
        val md = "    fn main() {}"
        val blocks = HoverMarkdownRenderer.render(md)
        assertTrue(
            blocks.any { it is HoverBlock.CodeBlock && (it as HoverBlock.CodeBlock).lang == null },
            "Expected a CodeBlock with null lang for indented code block, got: $blocks",
        )
    }

    /**
     * A link produces either a [HoverBlock.Paragraph] (Flexmark nests
     * links inside paragraphs) or a [HoverBlock.FallbackText]. Either
     * way the link label text must appear somewhere.
     *
     * This test is intentionally permissive about the block type because
     * Flexmark wraps link nodes inside Paragraph, not at the top level.
     *
     * Mutation: stub returns FallbackText with full raw markdown →
     * text.contains("a link") is still true (fallback shows raw source),
     * so this test PASSES under the stub. Covered by other tests' failures.
     */
    @Test
    fun unsupportedBlock_becomesFallback() {
        val md = "[a link](https://example.com)"
        val blocks = HoverMarkdownRenderer.render(md)
        assertTrue(
            blocks.any {
                val text = when (it) {
                    is HoverBlock.Paragraph -> it.text
                    is HoverBlock.FallbackText -> it.raw
                    else -> ""
                }
                text.contains("a link")
            },
            "Expected 'a link' to appear somewhere in rendered blocks, got: $blocks",
        )
    }

    /**
     * Mixed heading + paragraph + fenced code produces 3 blocks in
     * document order: Heading, Paragraph, CodeBlock.
     *
     * Mutation: stub returns [FallbackText(markdown)] → size==1, type
     * checks for index 0 (Heading), 1 (Paragraph), 2 (CodeBlock) all FAIL.
     */
    @Test
    fun mixedContent_orderedCorrectly() {
        val md = "# Header\n\npara\n\n```rust\nfn x()\n```"
        val blocks = HoverMarkdownRenderer.render(md)
        assertEquals(3, blocks.size, "Expected 3 blocks (Heading, Paragraph, CodeBlock)")
        assertTrue(
            blocks[0] is HoverBlock.Heading,
            "Block[0] must be Heading, got ${blocks[0]::class.simpleName}",
        )
        assertTrue(
            blocks[1] is HoverBlock.Paragraph,
            "Block[1] must be Paragraph, got ${blocks[1]::class.simpleName}",
        )
        assertTrue(
            blocks[2] is HoverBlock.CodeBlock,
            "Block[2] must be CodeBlock, got ${blocks[2]::class.simpleName}",
        )
    }

    /**
     * Realistic rust-analyzer hover output: fenced Rust code block
     * followed by a plain paragraph containing backtick inline code.
     * Both a CodeBlock and a Paragraph containing "macro" must appear.
     *
     * Mutation: stub returns [FallbackText(md)] → no CodeBlock found → FAIL.
     */
    @Test
    fun rustAnalyzerStyle_signatureBlock() {
        val md = "```rust\nfn println!(args: ...)\n```\n\nThe `println!` macro."
        val blocks = HoverMarkdownRenderer.render(md)
        assertTrue(
            blocks.any { it is HoverBlock.CodeBlock },
            "Expected a CodeBlock for the fenced code section",
        )
        assertTrue(
            blocks.any {
                it is HoverBlock.Paragraph &&
                    (it as HoverBlock.Paragraph).text.contains("macro")
            },
            "Expected a Paragraph containing 'macro', got: $blocks",
        )
    }
}
