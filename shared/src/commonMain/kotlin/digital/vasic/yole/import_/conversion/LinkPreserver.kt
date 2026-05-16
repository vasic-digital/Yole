/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: LinkPreserver — text + URL → Markdown inline link.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

/**
 * Converts a hyperlink (text label + URL) into a Markdown inline link.
 *
 * **Escaping rules:**
 * - In the link text: `]` → `\]`, `\` → `\\` (applied left-to-right,
 *   backslash first to avoid double-escaping).
 * - In the URL: `)` → `%29` (prevents premature closing of the Markdown
 *   link destination parentheses).
 *
 * Output format: `[escaped text](escaped url)`
 */
object LinkPreserver {

    /**
     * Builds a Markdown inline link from [text] and [url].
     *
     * @param text The visible link label.
     * @param url  The link destination URL.
     * @return A Markdown-formatted inline link string.
     */
    fun toMarkdownLink(text: String, url: String): String {
        val escapedText = text
            .replace("\\", "\\\\") // backslash first
            .replace("]", "\\]")
        val escapedUrl = url.replace(")", "%29")
        return "[$escapedText]($escapedUrl)"
    }
}
