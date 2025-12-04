/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/

/*
 * Parses most common markdown tags. Only inline tags are supported, multiline/block syntax
 * is not supported (citation, multiline code, ..). This is intended to stay as easy as possible.
 *
 * You can e.g. apply a accent color by replacing #000001 with your accentColor string.
 *
 * FILTER_ANDROID_TEXTVIEW output is intended to be used at simple Android TextViews,
 * where a limited set of HTML tags is supported. This allows displaying e.g. a simple
 * CHANGELOG.md file without including a WebView for showing HTML, or other additional UI-libraries.
 *
 * FILTER_WEB is intended to be used at engines understanding most common HTML tags.
 */

package digital.vasic.opoc.format

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Simple Markdown Parser with fluent API.
 *
 * Provides filters for converting Markdown to HTML for different rendering contexts:
 * - Android TextView (limited HTML support)
 * - Web browsers (full HTML support)
 * - Changelog formatting
 *
 * Example usage:
 * ```kotlin
 * val html = GsSimpleMarkdownParser()
 *     .parse(markdownText, filters = arrayOf(FILTER_WEB))
 *     .removeMultiNewlines()
 *     .html
 * ```
 */
@Suppress("unused")
class GsSimpleMarkdownParser {

    /**
     * Filter interface for transforming text.
     */
    fun interface SmpFilter {
        fun filter(text: String): String
    }

    private var defaultSmpFilter: SmpFilter = FILTER_WEB
    private var html: String = ""

    /**
     * Set the default filter to be used when no filters are specified.
     */
    fun setDefaultSmpFilter(defaultSmpFilter: SmpFilter): GsSimpleMarkdownParser {
        this.defaultSmpFilter = defaultSmpFilter
        return this
    }

    /**
     * Parse markdown from a file.
     */
    @Throws(IOException::class)
    fun parse(filepath: String, vararg smpFilters: SmpFilter): GsSimpleMarkdownParser {
        return parse(File(filepath).inputStream(), "", *smpFilters)
    }

    /**
     * Parse markdown from an InputStream.
     */
    @Throws(IOException::class)
    fun parse(
        inputStream: InputStream,
        lineMdPrefix: String = "",
        vararg smpFilters: SmpFilter
    ): GsSimpleMarkdownParser {
        val sb = StringBuilder()

        try {
            BufferedReader(InputStreamReader(inputStream)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(lineMdPrefix)
                    sb.append(line)
                    sb.append("\n")
                }
            }
        } catch (e: IOException) {
            html = ""
            throw e
        }

        html = parse(sb.toString(), "", *smpFilters).html
        return this
    }

    /**
     * Parse markdown from a String.
     */
    @Throws(IOException::class)
    fun parse(
        markdown: String,
        lineMdPrefix: String = "",
        vararg smpFilters: SmpFilter
    ): GsSimpleMarkdownParser {
        html = markdown
        val filters = if (smpFilters.isEmpty()) arrayOf(defaultSmpFilter) else smpFilters
        for (smpFilter in filters) {
            html = smpFilter.filter(html).trim()
        }
        return this
    }

    /**
     * Get the parsed HTML.
     */
    fun getHtml(): String = html

    /**
     * Set the HTML directly.
     */
    fun setHtml(html: String): GsSimpleMarkdownParser {
        this.html = html
        return this
    }

    /**
     * Remove multiple consecutive newlines.
     */
    fun removeMultiNewlines(): GsSimpleMarkdownParser {
        html = html.replace("\n", "").replace(Regex("(<br/>){3,}"), "<br/><br/>")
        return this
    }

    /**
     * Replace bullet character with a custom string.
     */
    fun replaceBulletCharacter(replacement: String): GsSimpleMarkdownParser {
        html = html.replace("&#8226;", replacement)
        return this
    }

    /**
     * Replace a hex color with a new color.
     */
    fun replaceColor(hexColor: String, newIntColor: Int): GsSimpleMarkdownParser {
        html = html.replace(hexColor, String.format("#%06X", 0xFFFFFF and newIntColor))
        return this
    }

    override fun toString(): String = html

    companion object {
        /**
         * Singleton instance accessor (for Java compatibility).
         */
        @JvmStatic
        private var instance: GsSimpleMarkdownParser? = null

        @JvmStatic
        fun get(): GsSimpleMarkdownParser {
            if (instance == null) {
                instance = GsSimpleMarkdownParser()
            }
            return instance!!
        }

        /**
         * Filter for Android TextView (supports limited HTML tags).
         */
        @JvmField
        val FILTER_ANDROID_TEXTVIEW = SmpFilter { text ->
            var result = text

            // Don't start new line if 2 empty lines and heading
            while (result.contains("\n\n#")) {
                result = result.replace("\n\n#", "\n#")
            }

            result
                .replace(Regex("(?s)<!--.*?-->"), "")  // HTML comments
                .replace("\n\n", "\n<br/>\n")  // Start new line if 2 empty lines
                .replace("~°", "&nbsp;&nbsp;")  // double space/half tab
                .replace(Regex("(?m)^### (.*)$"), "<br/><big><b><font color='#000000'>$1</font></b></big><br/>")  // h3
                .replace(Regex("(?m)^## (.*)$"), "<br/><big><big><b><font color='#000000'>$1</font></b></big></big><br/><br/>")  // h2
                .replace(Regex("(?m)^# (.*)$"), "<br/><big><big><big><b><font color='#000000'>$1</font></b></big></big></big><br/><br/>")  // h1
                .replace(Regex("!\\[(.*?)\\]\\((.*?)\\)"), "<a href='$2'>$1</a>")  // img
                .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href='$2'>$1</a>")  // a href
                .replace(Regex("<http(s?)://(.*)>"), "<a href='http$1://$2'>$1://$2</a>")  // a href
                .replace(Regex("(?m)^([-*] )(.*)$"), "<font color='#000001'>&#8226;</font> $2<br/>")  // unordered list
                .replace(Regex("(?m)^  (-|\\*) ([^<]*)$"), "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $2<br/>")  // unordered list2
                .replace(Regex("`([^<]*)`"), "<font face='monospace'>$1</font>")  // code
                .replace("\\*", "●")  // temporary replace escaped star symbol
                .replace(Regex("(?m)\\*\\*(.*)\\*\\*"), "<b>$1</b>")  // bold
                .replace(Regex("(?m)\\*(.*)\\*"), "<i>$1</i>")  // italic
                .replace("●", "*")  // restore escaped star symbol
                .replace(Regex("(?m)  $"), "<br/>")  // new line
        }

        /**
         * Filter for web browsers (full HTML support).
         */
        @JvmField
        val FILTER_WEB = SmpFilter { text ->
            var result = text

            // Don't start new line if 2 empty lines and heading
            while (result.contains("\n\n#")) {
                result = result.replace("\n\n#", "\n#")
            }

            result
                .replace(Regex("(?s)<!--.*?-->"), "")  // HTML comments
                .replace("\n\n", "\n<br/>\n")  // Start new line if 2 empty lines
                .replace("~°", "&nbsp;&nbsp;")  // double space/half tab
                .replace(Regex("(?m)^### (.*)$"), "<h3>$1</h3>")  // h3
                .replace(Regex("(?m)^## (.*)$"), "<h2>$1</h2>")  // h2
                .replace(Regex("(?m)^# (.*)$"), "<h1>$1</h1>")  // h1
                .replace(Regex("!\\[(.*?)\\]\\((.*?)\\)"), "<img src='$2' alt='$1' />")  // img
                .replace(Regex("<http(s?)://(.*)>"), "<a href='http$1://$2'>$1://$2</a>")  // a href
                .replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href='$2'>$1</a>")  // a href
                .replace(Regex("(?m)^[-*] (.*)$"), "<font color='#000001'>&#8226;</font> $1  ")  // unordered list
                .replace(Regex("(?m)^  [-*] (.*)$"), "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $1  ")  // unordered list2
                .replace(Regex("`([^<]*)`"), "<code>$1</code>")  // code
                .replace("\\*", "●")  // temporary replace escaped star symbol
                .replace(Regex("(?m)\\*\\*(.*)\\*\\*"), "<b>$1</b>")  // bold
                .replace(Regex("(?m)\\*(.*)\\*"), "<i>$1</i>")  // italic
                .replace("●", "*")  // restore escaped star symbol
                .replace(Regex("(?m)  $"), "<br/>")  // new line
        }

        /**
         * Filter for CHANGELOG files with colored labels.
         */
        @JvmField
        val FILTER_CHANGELOG = SmpFilter { text ->
            text
                .replace("New:", "<font color='#276230'>New:</font>")
                .replace("New features:", "<font color='#276230'>New:</font>")
                .replace("Added:", "<font color='#276230'>Added:</font>")
                .replace("Add:", "<font color='#276230'>Add:</font>")
                .replace("Fixed:", "<font color='#005688'>Fixed:</font>")
                .replace("Fix:", "<font color='#005688'>Fix:</font>")
                .replace("Removed:", "<font color='#C13524'>Removed:</font>")
                .replace("Updated:", "<font color='#555555'>Updated:</font>")
                .replace("Improved:", "<font color='#555555'>Improved:</font>")
                .replace("Modified:", "<font color='#555555'>Modified:</font>")
                .replace("Mod:", "<font color='#555555'>Mod:</font>")
        }

        /**
         * Filter to convert heading tags to superscript.
         */
        @JvmField
        val FILTER_H_TO_SUP = SmpFilter { text ->
            text
                .replace("<h1>", "<sup><sup><sup>")
                .replace("</h1>", "</sup></sup></sup>")
                .replace("<h2>", "<sup><sup>")
                .replace("</h2>", "</sup></sup>")
                .replace("<h3>", "<sup>")
                .replace("</h3>", "</sup>")
        }

        /**
         * No-op filter (passes text through unchanged).
         */
        @JvmField
        val FILTER_NONE = SmpFilter { text -> text }
    }
}
