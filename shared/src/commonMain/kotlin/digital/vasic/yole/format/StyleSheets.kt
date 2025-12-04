/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared CSS Stylesheets for Format Parsers
 * Centralizes CSS definitions to reduce memory allocation
 *
 *########################################################*/
package digital.vasic.yole.format

/**
 * Centralized CSS stylesheets for all format parsers.
 *
 * This object consolidates CSS definitions that were previously duplicated
 * in every parser's HTML output. By extracting CSS to shared constants,
 * we reduce memory allocation from ~1-2KB per document to a one-time allocation.
 *
 * ## Memory Optimization
 *
 * **Before**: Each parser's toHtml() call created new CSS strings
 * **After**: CSS strings allocated once and reused
 * **Savings**: ~1-2KB per document × number of documents
 *
 * ## Usage
 *
 * ```kotlin
 * class MarkdownParser : TextParser {
 *     override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
 *         return buildString {
 *             append(StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode))
 *             append(generateHtmlContent(document))
 *         }
 *     }
 * }
 * ```
 *
 * @see getStyleSheet
 */
object StyleSheets {

    /**
     * Get the appropriate stylesheet for a given format and theme.
     *
     * @param formatId The format identifier (e.g., TextFormat.ID_MARKDOWN)
     * @param lightMode true for light theme, false for dark theme
     * @return Complete HTML div + style tags with appropriate CSS
     */
    fun getStyleSheet(formatId: String, lightMode: Boolean): String {
        return when (formatId) {
            TextFormat.ID_MARKDOWN -> MARKDOWN_STYLES
            TextFormat.ID_WIKITEXT -> WIKITEXT_STYLES
            TextFormat.ID_RESTRUCTUREDTEXT -> if (lightMode) RST_STYLES_LIGHT else RST_STYLES_DARK
            TextFormat.ID_ORGMODE -> if (lightMode) ORGMODE_STYLES_LIGHT else ORGMODE_STYLES_DARK
            TextFormat.ID_ASCIIDOC -> if (lightMode) ASCIIDOC_STYLES_LIGHT else ASCIIDOC_STYLES_DARK
            TextFormat.ID_LATEX -> if (lightMode) LATEX_STYLES_LIGHT else LATEX_STYLES_DARK
            else -> "" // No predefined styles for this format
        }
    }

    /**
     * Markdown CSS styles.
     * Supports GitHub-flavored markdown with syntax highlighting.
     */
    const val MARKDOWN_STYLES = """<style>
.markdown { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; }
.markdown h1 { font-size: 2em; font-weight: 600; border-bottom: 1px solid #eee; padding-bottom: 0.3em; margin-top: 24px; margin-bottom: 16px; }
.markdown h2 { font-size: 1.5em; font-weight: 600; border-bottom: 1px solid #eee; padding-bottom: 0.3em; margin-top: 24px; margin-bottom: 16px; }
.markdown h3 { font-size: 1.25em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h4 { font-size: 1em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h5 { font-size: 0.875em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h6 { font-size: 0.85em; font-weight: 600; color: #666; margin-top: 24px; margin-bottom: 16px; }
.markdown p { margin-top: 0; margin-bottom: 16px; }
.markdown blockquote { border-left: 4px solid #ddd; padding: 0 1em; color: #666; margin: 0 0 16px 0; }
.markdown ul, .markdown ol { margin-top: 0; margin-bottom: 16px; padding-left: 2em; }
.markdown li { margin-bottom: 0.25em; }
.markdown code { background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; margin: 0; font-size: 85%; font-family: 'SF Mono', Monaco, Consolas, 'Courier New', monospace; border-radius: 3px; }
.markdown pre { background-color: #f6f8fa; padding: 16px; overflow-x: auto; font-size: 85%; line-height: 1.45; border-radius: 6px; margin-bottom: 16px; }
.markdown pre code { background-color: transparent; padding: 0; margin: 0; border-radius: 0; }
.markdown hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }
.markdown table { border-collapse: collapse; border-spacing: 0; margin-bottom: 16px; }
.markdown table th { font-weight: 600; padding: 6px 13px; border: 1px solid #ddd; background-color: #f6f8fa; }
.markdown table td { padding: 6px 13px; border: 1px solid #ddd; }
.markdown a { color: #0366d6; text-decoration: none; }
.markdown a:hover { text-decoration: underline; }
.markdown img { max-width: 100%; }
.markdown input[type='checkbox'] { margin-right: 0.5em; }
</style>"""

    /**
     * WikiText CSS styles.
     * Supports Zim Wiki and MediaWiki markup.
     */
    const val WIKITEXT_STYLES = """<style>
.wikitext { font-family: sans-serif; line-height: 1.6; }
.wikitext h1 { color: #4e9a06; font-size: 2em; }
.wikitext h2 { color: #4e9a06; font-size: 1.8em; }
.wikitext h3 { color: #4e9a06; font-size: 1.6em; }
.wikitext h4 { color: #4e9a06; font-size: 1.4em; }
.wikitext h5 { color: #4e9a06; font-size: 1.2em; }
.wikitext h6 { color: #4e9a06; font-size: 1em; }
.wikitext ul { list-style-type: disc; }
.wikitext ol { list-style-type: decimal; }
.wikitext .checklist { list-style-type: none; }
.wikitext .checklist li::before { content: '☐ '; color: #daa521; }
.wikitext .checklist li.checked::before { content: '☑ '; color: #54a309; }
.wikitext .checklist li.crossed::before { content: '☒ '; color: #a90000; }
.wikitext .highlight { background-color: #ffa062; padding: 2px 4px; }
.wikitext code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; }
.wikitext pre { background-color: #f0f0f0; padding: 10px; overflow-x: auto; }
.wikitext a { color: #1ea3fd; text-decoration: none; }
.wikitext a:hover { text-decoration: underline; }
</style>"""

    /**
     * reStructuredText CSS styles (Light mode).
     * Supports Python documentation format.
     */
    const val RST_STYLES_LIGHT = """<style>
.rst-document { font-family: sans-serif; line-height: 1.6; }
.rst-document.light { background: white; color: black; }
.rst-section { margin: 1rem 0; }
.rst-section-1 { font-size: 2em; color: #4e9a06; font-weight: bold; border-bottom: 2px solid #4e9a06; }
.rst-section-2 { font-size: 1.8em; color: #4e9a06; font-weight: bold; border-bottom: 1px solid #4e9a06; }
.rst-section-3 { font-size: 1.6em; color: #4e9a06; font-weight: bold; }
.rst-section-4 { font-size: 1.4em; color: #4e9a06; font-weight: bold; }
.rst-section-5 { font-size: 1.2em; color: #4e9a06; font-weight: bold; }
.rst-section-6 { font-size: 1em; color: #4e9a06; font-weight: bold; }
.rst-directive { background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-directive-header { font-family: monospace; color: #75507b; margin-bottom: 0.5rem; }
.rst-directive-content { font-family: monospace; white-space: pre-wrap; }
.rst-code { font-family: monospace; background: #f8f8f8; padding: 0.2rem 0.4rem; border-radius: 3px; }
.rst-block { background: #f8f8f8; border: 1px solid #ddd; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-link { color: #1ea3fd; text-decoration: none; }
.rst-link:hover { text-decoration: underline; }
.rst-bold { font-weight: bold; }
.rst-italic { font-style: italic; }
.rst-literal { font-family: monospace; background: #f0f0f0; padding: 0.1rem 0.2rem; }
.rst-admonition { background: #e7f2fa; border: 1px solid #6ab0de; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-admonition-title { font-weight: bold; color: #4e9a06; margin-bottom: 0.5rem; }
.rst-admonition.note { background: #e7f2fa; border-color: #6ab0de; }
.rst-admonition.warning { background: #fff2cc; border-color: #f0b37e; }
.rst-admonition.danger { background: #f2dede; border-color: #d9534f; }
.rst-admonition.tip { background: #dff0d8; border-color: #5cb85c; }
</style>"""

    /**
     * reStructuredText CSS styles (Dark mode).
     * Optimized for dark theme display.
     */
    const val RST_STYLES_DARK = """<style>
.rst-document { font-family: sans-serif; line-height: 1.6; }
.rst-document.dark { background: #1e1e1e; color: #d4d4d4; }
.rst-section { margin: 1rem 0; }
.rst-section-1 { font-size: 2em; color: #4e9a06; font-weight: bold; border-bottom: 2px solid #4e9a06; }
.rst-section-2 { font-size: 1.8em; color: #4e9a06; font-weight: bold; border-bottom: 1px solid #4e9a06; }
.rst-section-3 { font-size: 1.6em; color: #4e9a06; font-weight: bold; }
.rst-section-4 { font-size: 1.4em; color: #4e9a06; font-weight: bold; }
.rst-section-5 { font-size: 1.2em; color: #4e9a06; font-weight: bold; }
.rst-section-6 { font-size: 1em; color: #4e9a06; font-weight: bold; }
.rst-directive { background: #2d2d2d; border: 1px solid #444; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-directive-header { font-family: monospace; color: #ad7fa8; margin-bottom: 0.5rem; }
.rst-directive-content { font-family: monospace; white-space: pre-wrap; }
.rst-code { font-family: monospace; background: #2d2d2d; padding: 0.2rem 0.4rem; border-radius: 3px; }
.rst-block { background: #2d2d2d; border: 1px solid #444; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-link { color: #1ea3fd; text-decoration: none; }
.rst-link:hover { text-decoration: underline; }
.rst-bold { font-weight: bold; }
.rst-italic { font-style: italic; }
.rst-literal { font-family: monospace; background: #3d3d3d; padding: 0.1rem 0.2rem; }
.rst-admonition { background: #1e3a5f; border: 1px solid #4e9a06; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.rst-admonition-title { font-weight: bold; color: #4e9a06; margin-bottom: 0.5rem; }
.rst-admonition.note { background: #1e3a5f; border-color: #6ab0de; }
.rst-admonition.warning { background: #3d3020; border-color: #f0b37e; }
.rst-admonition.danger { background: #3d2020; border-color: #d9534f; }
.rst-admonition.tip { background: #203d20; border-color: #5cb85c; }
</style>"""

    /**
     * Org Mode CSS styles (Light mode).
     * Supports Emacs Org Mode format.
     */
    const val ORGMODE_STYLES_LIGHT = """<style>
.org-mode-document { font-family: sans-serif; line-height: 1.6; }
.org-mode-document.light { background: white; color: black; }
.org-heading { margin: 1rem 0; }
.org-heading-1 { font-size: 2em; color: #4e9a06; font-weight: bold; }
.org-heading-2 { font-size: 1.8em; color: #4e9a06; font-weight: bold; }
.org-heading-3 { font-size: 1.6em; color: #4e9a06; font-weight: bold; }
.org-heading-4 { font-size: 1.4em; color: #4e9a06; font-weight: bold; }
.org-heading-5 { font-size: 1.2em; color: #4e9a06; font-weight: bold; }
.org-heading-6 { font-size: 1em; color: #4e9a06; font-weight: bold; }
.org-todo { font-weight: bold; }
.org-todo-todo { color: #cc0000; }
.org-todo-done { color: #4e9a06; text-decoration: line-through; }
.org-properties { background: #f5f5f5; border: 1px solid #ddd; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.org-property { font-family: monospace; }
.org-property-key { color: #75507b; }
.org-property-value { color: #4e9a06; }
.org-code { font-family: monospace; background: #f8f8f8; padding: 0.2rem 0.4rem; border-radius: 3px; }
.org-block { background: #f8f8f8; border: 1px solid #ddd; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.org-block-header { font-family: monospace; color: #75507b; margin-bottom: 0.5rem; }
.org-block-content { font-family: monospace; white-space: pre-wrap; }
.org-link { color: #1ea3fd; text-decoration: none; }
.org-link:hover { text-decoration: underline; }
.org-bold { font-weight: bold; }
.org-italic { font-style: italic; }
.org-underline { text-decoration: underline; }
.org-strikethrough { text-decoration: line-through; }
.org-verbatim { font-family: monospace; background: #f0f0f0; padding: 0.1rem 0.2rem; }
</style>"""

    /**
     * Org Mode CSS styles (Dark mode).
     * Optimized for dark theme display.
     */
    const val ORGMODE_STYLES_DARK = """<style>
.org-mode-document { font-family: sans-serif; line-height: 1.6; }
.org-mode-document.dark { background: #1e1e1e; color: #d4d4d4; }
.org-heading { margin: 1rem 0; }
.org-heading-1 { font-size: 2em; color: #4e9a06; font-weight: bold; }
.org-heading-2 { font-size: 1.8em; color: #4e9a06; font-weight: bold; }
.org-heading-3 { font-size: 1.6em; color: #4e9a06; font-weight: bold; }
.org-heading-4 { font-size: 1.4em; color: #4e9a06; font-weight: bold; }
.org-heading-5 { font-size: 1.2em; color: #4e9a06; font-weight: bold; }
.org-heading-6 { font-size: 1em; color: #4e9a06; font-weight: bold; }
.org-todo { font-weight: bold; }
.org-todo-todo { color: #cc0000; }
.org-todo-done { color: #73d216; text-decoration: line-through; }
.org-properties { background: #2d2d2d; border: 1px solid #444; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.org-property { font-family: monospace; }
.org-property-key { color: #ad7fa8; }
.org-property-value { color: #73d216; }
.org-code { font-family: monospace; background: #2d2d2d; padding: 0.2rem 0.4rem; border-radius: 3px; }
.org-block { background: #2d2d2d; border: 1px solid #444; border-radius: 4px; padding: 1rem; margin: 1rem 0; }
.org-block-header { font-family: monospace; color: #ad7fa8; margin-bottom: 0.5rem; }
.org-block-content { font-family: monospace; white-space: pre-wrap; }
.org-link { color: #1ea3fd; text-decoration: none; }
.org-link:hover { text-decoration: underline; }
.org-bold { font-weight: bold; }
.org-italic { font-style: italic; }
.org-underline { text-decoration: underline; }
.org-strikethrough { text-decoration: line-through; }
.org-verbatim { font-family: monospace; background: #3d3d3d; padding: 0.1rem 0.2rem; }
</style>"""

    /**
     * AsciiDoc CSS styles (Light mode).
     * Supports AsciiDoc markup format.
     */
    const val ASCIIDOC_STYLES_LIGHT = """<style>
.asciidoc { font-family: sans-serif; line-height: 1.6; }
.asciidoc h1 { color: #4e9a06; font-size: 2em; border-bottom: 2px solid #4e9a06; padding-bottom: 0.3em; }
.asciidoc h2 { color: #4e9a06; font-size: 1.8em; border-bottom: 1px solid #4e9a06; padding-bottom: 0.2em; }
.asciidoc h3 { color: #4e9a06; font-size: 1.6em; }
.asciidoc h4 { color: #4e9a06; font-size: 1.4em; }
.asciidoc h5 { color: #4e9a06; font-size: 1.2em; }
.asciidoc h6 { color: #4e9a06; font-size: 1em; }
.asciidoc blockquote { border-left: 4px solid #4e9a06; padding-left: 1em; margin-left: 0; color: #666; }
.asciidoc code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; border-radius: 3px; }
.asciidoc pre { background-color: #f0f0f0; padding: 1em; border-radius: 5px; overflow-x: auto; }
.asciidoc table { border-collapse: collapse; width: 100%; }
.asciidoc th, .asciidoc td { border: 1px solid #ddd; padding: 8px; text-align: left; }
.asciidoc th { background-color: #4e9a06; color: white; }
.asciidoc a { color: #1ea3fd; text-decoration: none; }
.asciidoc a:hover { text-decoration: underline; }
.asciidoc .admonition { border-left: 4px solid; padding: 1em; margin: 1em 0; }
.asciidoc .admonition-note { border-color: #1ea3fd; background-color: #f0f8ff; }
.asciidoc .admonition-tip { border-color: #4e9a06; background-color: #f8fff0; }
.asciidoc .admonition-warning { border-color: #ffa500; background-color: #fff8f0; }
.asciidoc .admonition-important { border-color: #ff4500; background-color: #fff0f0; }
.asciidoc .admonition-caution { border-color: #dc143c; background-color: #fff0f0; }
.asciidoc ul { list-style-type: disc; }
.asciidoc ol { list-style-type: decimal; }
.asciidoc li { margin: 0.5em 0; }
</style>"""

    /**
     * AsciiDoc CSS styles (Dark mode).
     * Optimized for dark theme display.
     */
    const val ASCIIDOC_STYLES_DARK = """<style>
.asciidoc { font-family: sans-serif; line-height: 1.6; background-color: #2d2d2d; color: #f0f0f0; }
.asciidoc h1 { color: #4e9a06; font-size: 2em; border-bottom: 2px solid #4e9a06; padding-bottom: 0.3em; }
.asciidoc h2 { color: #4e9a06; font-size: 1.8em; border-bottom: 1px solid #4e9a06; padding-bottom: 0.2em; }
.asciidoc h3 { color: #4e9a06; font-size: 1.6em; }
.asciidoc h4 { color: #4e9a06; font-size: 1.4em; }
.asciidoc h5 { color: #4e9a06; font-size: 1.2em; }
.asciidoc h6 { color: #4e9a06; font-size: 1em; }
.asciidoc blockquote { border-left: 4px solid #4e9a06; padding-left: 1em; margin-left: 0; color: #ccc; }
.asciidoc code { background-color: #3d3d3d; color: #f0f0f0; padding: 2px 4px; font-family: monospace; border-radius: 3px; }
.asciidoc pre { background-color: #3d3d3d; color: #f0f0f0; padding: 1em; border-radius: 5px; overflow-x: auto; }
.asciidoc table { border-collapse: collapse; width: 100%; }
.asciidoc th, .asciidoc td { border: 1px solid #555; padding: 8px; text-align: left; }
.asciidoc th { background-color: #4e9a06; color: white; }
.asciidoc a { color: #1ea3fd; text-decoration: none; }
.asciidoc a:hover { text-decoration: underline; }
.asciidoc .admonition { border-left: 4px solid; padding: 1em; margin: 1em 0; }
.asciidoc .admonition-note { border-color: #1ea3fd; background-color: #1e2a3a; }
.asciidoc .admonition-tip { border-color: #4e9a06; background-color: #1e2a1e; }
.asciidoc .admonition-warning { border-color: #ffa500; background-color: #3a2a1e; }
.asciidoc .admonition-important { border-color: #ff4500; background-color: #3a1e1e; }
.asciidoc .admonition-caution { border-color: #dc143c; background-color: #3a1e1e; }
.asciidoc ul { list-style-type: disc; }
.asciidoc ol { list-style-type: decimal; }
.asciidoc li { margin: 0.5em 0; }
</style>"""

    /**
     * LaTeX CSS styles (Light mode).
     * Supports LaTeX document formatting.
     */
    const val LATEX_STYLES_LIGHT = """<style>
.latex { font-family: serif; line-height: 1.6; }
.latex .document-title { font-size: 2em; font-weight: bold; text-align: center; margin: 1em 0; }
.latex .document-author { font-size: 1.2em; text-align: center; margin: 0.5em 0; color: #666; }
.latex .document-date { font-size: 1em; text-align: center; margin: 0.5em 0; color: #999; }
.latex .section { font-size: 1.5em; font-weight: bold; margin: 1em 0 0.5em 0; }
.latex .subsection { font-size: 1.3em; font-weight: bold; margin: 0.8em 0 0.4em 0; }
.latex .paragraph { font-size: 1.1em; font-weight: bold; margin: 0.6em 0 0.3em 0; }
.latex .math { font-family: "Times New Roman", serif; font-style: italic; }
.latex .math-inline { display: inline; }
.latex .math-display { display: block; text-align: center; margin: 1em 0; }
.latex .environment { border: 1px solid #ddd; padding: 1em; margin: 1em 0; background-color: #f9f9f9; }
.latex .environment-title { font-weight: bold; margin-bottom: 0.5em; color: #4e9a06; }
.latex code { background-color: #f0f0f0; padding: 2px 4px; font-family: monospace; }
.latex pre { background-color: #f0f0f0; padding: 1em; overflow-x: auto; }
.latex .itemize { list-style-type: disc; margin-left: 2em; }
.latex .enumerate { list-style-type: decimal; margin-left: 2em; }
.latex .item { margin: 0.5em 0; }
.latex .bold { font-weight: bold; }
.latex .italic { font-style: italic; }
.latex .underline { text-decoration: underline; }
</style>"""

    /**
     * LaTeX CSS styles (Dark mode).
     * Optimized for dark theme display.
     */
    const val LATEX_STYLES_DARK = """<style>
.latex { font-family: serif; line-height: 1.6; background-color: #2d2d2d; color: #f0f0f0; }
.latex .document-title { font-size: 2em; font-weight: bold; text-align: center; margin: 1em 0; }
.latex .document-author { font-size: 1.2em; text-align: center; margin: 0.5em 0; color: #aaa; }
.latex .document-date { font-size: 1em; text-align: center; margin: 0.5em 0; color: #888; }
.latex .section { font-size: 1.5em; font-weight: bold; margin: 1em 0 0.5em 0; }
.latex .subsection { font-size: 1.3em; font-weight: bold; margin: 0.8em 0 0.4em 0; }
.latex .paragraph { font-size: 1.1em; font-weight: bold; margin: 0.6em 0 0.3em 0; }
.latex .math { font-family: "Times New Roman", serif; font-style: italic; }
.latex .math-inline { display: inline; }
.latex .math-display { display: block; text-align: center; margin: 1em 0; }
.latex .environment { border: 1px solid #555; padding: 1em; margin: 1em 0; background-color: #3d3d3d; }
.latex .environment-title { font-weight: bold; margin-bottom: 0.5em; color: #4e9a06; }
.latex code { background-color: #3d3d3d; color: #f0f0f0; padding: 2px 4px; font-family: monospace; }
.latex pre { background-color: #3d3d3d; color: #f0f0f0; padding: 1em; overflow-x: auto; }
.latex .itemize { list-style-type: disc; margin-left: 2em; }
.latex .enumerate { list-style-type: decimal; margin-left: 2em; }
.latex .item { margin: 0.5em 0; }
.latex .bold { font-weight: bold; }
.latex .italic { font-style: italic; }
.latex .underline { text-decoration: underline; }
</style>"""
}
