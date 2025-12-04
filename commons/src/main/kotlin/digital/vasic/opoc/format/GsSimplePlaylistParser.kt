/*#######################################################
 *
 * SPDX-FileCopyrightText: 2019-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2019-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/

/*
 * Simple Parser for M3U playlists with some extensions
 * Mainly for playlists with video streams
 */

package digital.vasic.opoc.format

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.roundToInt

/**
 * Simple Parser for M3U playlists.
 *
 * Supports M3U extended format with TVG tags commonly used for IPTV streams.
 *
 * Example usage:
 * ```kotlin
 * val parser = GsSimplePlaylistParser()
 * val items = parser.parse(File("playlist.m3u"))
 * items.forEach { println("${it.name}: ${it.url}") }
 * ```
 */
@Suppress("unused")
class GsSimplePlaylistParser {

    /**
     * Parse M3U file by reading content from file.
     */
    fun parse(filepath: File): ArrayList<Item> {
        return try {
            parse(filepath.inputStream())
        } catch (e: Exception) {
            ArrayList()
        }
    }

    /**
     * Parse M3U file by reading from InputStream.
     */
    fun parse(inputStream: InputStream): ArrayList<Item> {
        val text = StringBuilder()
        try {
            BufferedReader(InputStreamReader(inputStream)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    text.append(line)
                    text.append("\n")
                }
            }
        } catch (ignored: Exception) {
        }
        return parse(text.toString())
    }

    /**
     * Parse M3U content from a String.
     */
    fun parse(text: String): ArrayList<Item> {
        var lastEntry: Item? = null
        val entries = ArrayList<Item>()

        val normalizedText = text.trim().replace("\r", "")

        for (line in normalizedText.split("\n")) {
            try {
                lastEntry = parseLine(line.trim(), entries, lastEntry)
            } catch (e: Exception) {
                lastEntry = null
            }
        }
        return entries
    }

    /**
     * Parse one line of M3U content.
     */
    private fun parseLine(line: String, entries: MutableList<Item>, lastEntry: Item?): Item? {
        return when {
            line.startsWith(EXTINF_TAG) -> {
                // #EXTINF line
                try {
                    parseExtInf(line)
                } catch (ignored: Exception) {
                    null
                }
            }
            line.isNotEmpty() && !line.startsWith("#") -> {
                // URL line (no comment, no empty line)
                val entry = lastEntry ?: Item()
                entry.url = line
                entries.add(entry)
                null
            }
            else -> {
                // No useable data -> reset last EXTINF for next entry
                null
            }
        }
    }

    /**
     * Parse EXTINF line and extract metadata.
     */
    private fun parseExtInf(line: String): Item {
        val curEntry = Item()
        if (line.length < EXTINF_TAG.length + 1) {
            return curEntry
        }

        var remaining = line.substring(EXTINF_TAG.length)

        // Read seconds (may end with comma or whitespace)
        val secondsStr = remaining.takeWhile { it.isDigit() || it == '-' || it == '+' }
        if (secondsStr.isEmpty()) {
            return curEntry
        }
        curEntry.seconds = secondsStr.toIntOrNull() ?: -1
        remaining = remaining.substring(secondsStr.length)

        // Parse TVG tags
        var oldRemaining: String
        while (remaining.isNotEmpty() && !remaining.startsWith(",")) {
            oldRemaining = remaining.trim()
            remaining = oldRemaining

            remaining = when {
                remaining.startsWith(EXTINF_TVG_NAME) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_TVG_NAME)
                    curEntry.tvgName = value.replace("'", "")
                    rest
                }
                remaining.startsWith(EXTINF_TVG_LOGO) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_TVG_LOGO)
                    curEntry.tvgLogo = value
                    rest
                }
                remaining.startsWith(EXTINF_TVG_EPGURL) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_TVG_EPGURL)
                    curEntry.tvgEpgUrl = value
                    rest
                }
                remaining.startsWith(EXTINF_RADIO) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_RADIO)
                    curEntry.isRadio = value.toBooleanStrictOrNull() ?: false
                    rest
                }
                remaining.startsWith(EXTINF_GROUP_TITLE) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_GROUP_TITLE)
                    curEntry.groupTitle = value
                    rest
                }
                remaining.startsWith(EXTINF_TVG_ID) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_TVG_ID)
                    curEntry.tvgId = value
                    rest
                }
                remaining.startsWith(EXTINF_TAGS) -> {
                    val (value, rest) = extractQuotedValue(remaining, EXTINF_TAGS)
                    curEntry.tags = value.split(",").toTypedArray()
                    rest
                }
                else -> {
                    // Skip unknown tag
                    val firstQuote = remaining.indexOf("\"")
                    if (firstQuote == -1) break
                    val secondQuote = remaining.indexOf("\"", firstQuote + 1)
                    if (secondQuote == -1) break
                    remaining.substring(secondQuote + 1)
                }
            }

            // Break if no progress
            if (remaining == oldRemaining) break
        }

        // Parse name (after comma)
        remaining = remaining.trim()
        if (remaining.startsWith(",")) {
            remaining = remaining.substring(1).trim()
            if (remaining.isNotEmpty()) {
                curEntry.name = remaining.replace("'", "")
            }
        }

        return curEntry
    }

    /**
     * Extract a quoted value from a string.
     * Returns pair of (value, remaining string)
     */
    private fun extractQuotedValue(text: String, prefix: String): Pair<String, String> {
        val afterPrefix = text.substring(prefix.length)
        val endQuoteIndex = afterPrefix.indexOf("\"")
        return if (endQuoteIndex != -1) {
            val value = afterPrefix.substring(0, endQuoteIndex)
            val rest = afterPrefix.substring(endQuoteIndex + 1)
            Pair(value, rest)
        } else {
            Pair("", afterPrefix)
        }
    }

    /**
     * Data class for playlist entries.
     */
    data class Item(
        var tvgName: String? = null,
        var name: String? = null,
        var tvgLogo: String? = null,
        var tvgEpgUrl: String? = null,
        var tvgId: String? = null,
        var groupTitle: String? = null,
        var url: String? = null,
        var tags: Array<String> = emptyArray(),
        var seconds: Int = -1,
        var isRadio: Boolean = false
    ) {
        /**
         * Get the display name for this item, optionally truncated to maxLen.
         */
        fun getName(vararg maxLen: Int): String {
            val limitTotal = if (maxLen.isNotEmpty()) maxLen[0] else 999
            val limitBegin = (limitTotal * 0.565f).roundToInt()
            val limitEnd = (limitTotal * 0.435f).roundToInt()

            var t = when {
                tvgName != null -> tvgName!!
                name != null -> name!!
                url != null -> url!!
                    .replace(Regex("(?i)https?://"), "")
                    .replace(Regex("\\.\\w+$"), "")
                else -> ""
            }

            if (t.length > limitTotal) {
                t = t.replaceFirst(Regex("(.{$limitBegin}).+(.{$limitEnd})"), "$1…$2")
            }

            return t
        }

        /**
         * Get the URL for this item.
         */
        @JvmName("getUrlNonNull")
        fun getUrl(): String = url ?: ""

        override fun toString(): String = "${getName()} ${getUrl()}"

        // Custom equals/hashCode because of Array field
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Item

            if (tvgName != other.tvgName) return false
            if (name != other.name) return false
            if (tvgLogo != other.tvgLogo) return false
            if (tvgEpgUrl != other.tvgEpgUrl) return false
            if (tvgId != other.tvgId) return false
            if (groupTitle != other.groupTitle) return false
            if (url != other.url) return false
            if (!tags.contentEquals(other.tags)) return false
            if (seconds != other.seconds) return false
            if (isRadio != other.isRadio) return false

            return true
        }

        override fun hashCode(): Int {
            var result = tvgName?.hashCode() ?: 0
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + (tvgLogo?.hashCode() ?: 0)
            result = 31 * result + (tvgEpgUrl?.hashCode() ?: 0)
            result = 31 * result + (tvgId?.hashCode() ?: 0)
            result = 31 * result + (groupTitle?.hashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + tags.contentHashCode()
            result = 31 * result + seconds
            result = 31 * result + isRadio.hashCode()
            return result
        }
    }

    companion object {
        private const val EXTINF_TAG = "#EXTINF:"
        private const val EXTINF_TVG_NAME = "tvg-name=\""
        private const val EXTINF_TVG_ID = "tvg-id=\""
        private const val EXTINF_TVG_LOGO = "tvg-logo=\""
        private const val EXTINF_TVG_EPGURL = "tvg-epgurl=\""
        private const val EXTINF_GROUP_TITLE = "group-title=\""
        private const val EXTINF_RADIO = "radio=\""
        private const val EXTINF_TAGS = "tags=\""
    }
}
