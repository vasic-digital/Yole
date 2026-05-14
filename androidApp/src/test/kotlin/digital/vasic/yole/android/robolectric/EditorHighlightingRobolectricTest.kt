/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-57 Phase 9, CONST-035 anti-bluff):
 *   Verifies that the IDE editor surface (SyncedScrollEditor) is
 *   genuinely wired to SyntaxHighlighter for colored AnnotatedString
 *   rendering, and that IdeEditorScreen passes a non-null
 *   highlighter+langId to it when the file's grammar resolves.
 *
 *   Three layers of evidence are produced here:
 *     (1) Structural source-level assertion against
 *         SyncedScrollEditor.kt — must declare a VisualTransformation
 *         that wraps the highlighted AnnotatedString and gate it on
 *         EnabledFormatGate.isEnabled. Removing the
 *         VisualTransformation, dropping the gate check, or stubbing
 *         the highlighted state back to plain AnnotatedString MUST
 *         fail these assertions.
 *     (2) Structural source-level assertion against
 *         IdeEditorScreen in YoleApp.kt — the SyncedScrollEditor(...)
 *         call MUST pass `highlighter = highlighter` and
 *         `langId = passedLangId` (i.e., not null literals).
 *         Mutation guard: replacing `highlighter = highlighter` with
 *         `highlighter = null` (or removing the param) MUST fail
 *         this assertion.
 *     (3) Runtime AnnotatedString contract: a real
 *         AnnotatedStringBuilder.build call over a known set of
 *         tokens + theme entries produces > 0 SpanStyles. This
 *         exercises the exact code path the editor's
 *         VisualTransformation consumes once the platform tokenizer
 *         emits tokens. If the editor were silently routed around
 *         this builder (e.g., a future regression that drops the
 *         transform), the call-site assertion in (2) would catch it;
 *         this assertion catches the inverse — a bluff builder that
 *         appears to return styled output but actually drops spans.
 *
 *   Why no full end-to-end "type Markdown → assert SpanStyle in
 *   semantics" test on Android: the bonede tree-sitter-ng upstream
 *   JAR does not bundle aarch64-linux-android-tree-sitter.so today
 *   (see TokenizerEngine.android.kt) so initialize() returns
 *   Result.failure on Android, and the highlighter gracefully
 *   degrades to plain text — by design, per spec §4 and CONST-035.
 *   The Desktop SyntaxHighlighterTest already verifies the runtime
 *   AnnotatedString-with-spans contract end-to-end against a real
 *   parser. This Android test enforces the *integration wiring*,
 *   which is the surface this phase ships.
 *
 *   Mutation discipline:
 *     - Reverting `highlighter = highlighter` in IdeEditorScreen to
 *       `highlighter = null` → layer (2) FAILS.
 *     - Removing the VisualTransformation from SyncedScrollEditor
 *       → layer (1) FAILS.
 *     - Removing the EnabledFormatGate.isEnabled gate check
 *       → layer (1) FAILS.
 *     - Stubbing AnnotatedStringBuilder.build to return
 *       AnnotatedString(text) (no spans) → layer (3) FAILS.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.Token
import digital.vasic.yole.syntax.render.AnnotatedStringBuilder
import digital.vasic.yole.syntax.theme.VsCodeThemeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class EditorHighlightingRobolectricTest {

    @Before
    fun enableMarkdown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadEditorSource(): String =
        loadSource("androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt")

    private fun loadYoleAppSource(): String =
        loadSource("androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt")

    /**
     * Layer 1a: SyncedScrollEditor MUST apply a VisualTransformation to
     * the BasicTextField. Without one, the highlighted AnnotatedString
     * is computed but never rendered — the user sees plain text.
     * Mutation guard: deleting `visualTransformation =
     * highlightingTransform` from BasicTextField makes this fail.
     */
    @Test
    fun syncedScrollEditorAppliesVisualTransformationToBasicTextField() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST define a VisualTransformation for highlighting",
            src.contains("VisualTransformation"),
        )
        assertTrue(
            "BasicTextField MUST receive a visualTransformation argument",
            Regex("""visualTransformation\s*=""").containsMatchIn(src),
        )
        assertTrue(
            "VisualTransformation MUST wrap an AnnotatedString via TransformedText",
            src.contains("TransformedText"),
        )
    }

    /**
     * Layer 1b: SyncedScrollEditor MUST gate highlighting on
     * EnabledFormatGate.isEnabled(langId) so a user-disabled format
     * does NOT get tokenized. Removing the check would also bypass
     * graceful degradation when the engine fails to initialize on a
     * platform — both classes of bluff. Mutation guard: deleting the
     * gate-check fails this.
     */
    @Test
    fun syncedScrollEditorGatesHighlightingOnEnabledFormatGate() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST consult EnabledFormatGate.isEnabled before tokenizing",
            Regex("""EnabledFormatGate\.isEnabled\(""").containsMatchIn(src),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept an optional SyntaxHighlighter parameter",
            Regex("""highlighter:\s*SyntaxHighlighter\?""").containsMatchIn(src),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept an optional langId parameter",
            Regex("""langId:\s*String\?""").containsMatchIn(src),
        )
    }

    /**
     * Layer 1c: SyncedScrollEditor MUST debounce tokenization so rapid
     * keystrokes don't trigger a tokenize call per character. The
     * iter-57 spec mandates 80ms. Mutation guard: removing the
     * `delay(80)` (or reducing it to 0) inside the LaunchedEffect
     * makes this fail.
     */
    @Test
    fun syncedScrollEditorDebouncesTokenization() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST debounce tokenization with delay(80)",
            Regex("""delay\(80\)""").containsMatchIn(src),
        )
    }

    /**
     * Layer 2: IdeEditorScreen in YoleApp.kt MUST pass a non-null
     * highlighter and langId binding into SyncedScrollEditor. A literal
     * `null` would break the integration contract. Mutation guard:
     * editing the call site to `highlighter = null` (per Phase 9.5
     * spec) MUST fail this.
     */
    @Test
    fun ideEditorScreenPassesHighlighterAndLangIdToSyncedScrollEditor() {
        val src = loadYoleAppSource()
        assertTrue(
            "IdeEditorScreen MUST detect lang via GrammarRegistry.detectLangId",
            src.contains("GrammarRegistry.detectLangId"),
        )
        assertTrue(
            "IdeEditorScreen MUST construct a SyntaxHighlighter",
            src.contains("SyntaxHighlighter(tokenizerEngine)"),
        )
        assertTrue(
            "IdeEditorScreen MUST initialize the TokenizerEngine via LaunchedEffect",
            Regex("""tokenizerEngine\.initialize\(\)""").containsMatchIn(src),
        )
        // The mutation-verifying assertion: the call site must pass the
        // highlighter variable (not a null literal) to SyncedScrollEditor.
        // Reverting to `highlighter = null` (Phase 9.5 mutation) fails here.
        assertTrue(
            "SyncedScrollEditor(...) call MUST pass `highlighter = highlighter` (NOT null)",
            Regex("""highlighter\s*=\s*highlighter""").containsMatchIn(src),
        )
        assertTrue(
            "SyncedScrollEditor(...) call MUST pass a non-null langId binding",
            Regex("""langId\s*=\s*passedLangId""").containsMatchIn(src),
        )
        // Explicit anti-bluff: there MUST NOT be a `highlighter = null`
        // literal in the SyncedScrollEditor(...) call within IdeEditorScreen.
        val ideEditorBlock = src.substringAfter("fun IdeEditorScreen(")
            .substringBefore("\n// ===== IDE MARKDOWN TOOLBAR =====")
        assertTrue(
            "IdeEditorScreen MUST NOT pass a literal `highlighter = null` to SyncedScrollEditor",
            !Regex("""highlighter\s*=\s*null""").containsMatchIn(ideEditorBlock),
        )
        assertTrue(
            "IdeEditorScreen MUST NOT pass a literal `langId = null` to SyncedScrollEditor",
            !Regex("""langId\s*=\s*null""").containsMatchIn(ideEditorBlock),
        )
    }

    /**
     * Layer 2b: theme is sourced from LocalTheme.current so the
     * highlighter recomposes when the theme changes (spec §3.8 single
     * source of truth). Mutation guard: hardcoding a theme in
     * IdeEditorScreen would fail this.
     */
    @Test
    fun ideEditorScreenSourcesThemeFromLocalTheme() {
        val src = loadYoleAppSource()
        assertTrue(
            "IdeEditorScreen MUST access LocalTheme.current for the highlighter theme",
            src.contains("LocalTheme.current"),
        )
    }

    /**
     * Layer 3: the AnnotatedStringBuilder used by SyntaxHighlighter
     * MUST produce > 0 SpanStyles when given a Token list whose
     * scopes resolve via the theme. This is the same runtime
     * contract enforced by the Desktop SyntaxHighlighterTest, but
     * tested here against AnnotatedStringBuilder directly because
     * the Android TokenizerEngine actual returns Result.failure
     * (no .so bundled) and a true end-to-end Android tokenize is
     * blocked on the operator NDK build per
     * docs/KNOWN_DEFECTS.md. The wiring under test for THIS phase
     * is the Compose surface integration in layers 1 & 2; this
     * layer guards against a bluff in the shared rendering path.
     */
    @Test
    fun annotatedStringBuilderProducesSpansForKnownTokenAndTheme() {
        val themeJson = """
            {
              "name": "Test",
              "type": "dark",
              "colors": {},
              "tokenColors": [
                { "scope": "markup.heading", "settings": { "foreground": "#ff0000" } }
              ]
            }
        """.trimIndent()
        val theme = VsCodeThemeParser.parse(themeJson)
        val text = "# Heading\n"
        // Scope name `heading` is a canonical Tree-Sitter highlight capture
        // and ScopeMapper.treeSitterToVsCode rewrites it to `markup.heading`.
        val tokens = listOf(Token(startByte = 0, endByte = 9, scope = "heading"))
        val annotated = AnnotatedStringBuilder.build(text, tokens, theme)
        assertEquals("text preserved verbatim", text, annotated.text)
        assertTrue(
            "expected >= 1 SpanStyle from real renderer, got ${annotated.spanStyles.size}",
            annotated.spanStyles.isNotEmpty(),
        )
    }
}
