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
package digital.vasic.opoc.format

import android.util.Base64
import digital.vasic.opoc.wrapper.GsCallback
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.Random
import java.util.regex.Pattern

/**
 * Text manipulation utilities.
 *
 * Provides a comprehensive set of text processing functions including:
 * - URL and resource extraction
 * - Line manipulation
 * - Base64 encoding/decoding
 * - JSON serialization
 * - Character counting and searching
 * - String validation and formatting
 */
@Suppress("unused", "SpellCheckingInspection")
object GsTextUtils {

    const val UTF8 = "UTF-8"

    // Regex patterns used for finding resources in tags
    @JvmField
    val SELF_CLOSING_TAG: Pattern = Pattern.compile("<(\\w+)([^>]*?)src='([^']+)'([^>]*?)/>")

    @JvmField
    val REGULAR_TAG: Pattern = Pattern.compile("<(\\w+)([^>]*?)src='([^']+)'([^>]*?)>(.*?)</\\1>")

    /**
     * This is a simple method that tries to extract a URL around a given index.
     * It doesn't do any validation. Separation by whitespace or end. Detects http and https.
     *
     * @param text Text to extract from
     * @param pos  Position to start searching from (backwards)
     * @return Extracted URL or null if none found
     */
    @JvmStatic
    fun tryExtractUrlAroundPos(text: String, pos: Int): String? {
        val safePos = pos.coerceIn(0, text.length - 1)
        val begin = maxOf(
            text.lastIndexOf("https://", safePos),
            text.lastIndexOf("http://", safePos)
        )

        if (begin < 0) return null

        var end = text.length
        for (check in arrayOf("\n", " ", "\t", "\r", ")", "|")) {
            val idx = text.indexOf(check, begin)
            if (idx in (begin + 1) until end) {
                end = idx
            }
        }

        return if (end - begin > 5) {
            text.substring(begin, end).replace(Regex("[\\]=%>}]+$"), "")
        } else {
            null
        }
    }

    /**
     * This is a simple method that tries to extract the value of the 'src' attribute
     * for any tag in the text which surrounds pos.
     * It doesn't do any validation. Separation by whitespace or end.
     *
     * @param text Text to extract from
     * @param pos  Position to start searching from (backwards)
     * @return Extracted resource path or null if none found
     */
    @JvmStatic
    fun tryExtractResourceAroundPos(text: String, pos: Int): String? {
        for (pattern in listOf(SELF_CLOSING_TAG, REGULAR_TAG)) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (pos in start..end) {
                    return matcher.group(3)
                }
            }
        }
        return null
    }

    /**
     * Find '\n' to the right and left of text[pos] .. text[posEnd].
     * If left does not exist 0 (begin of text) is used.
     * If right does not exist text.length() (end of text) is used.
     *
     * @return result[0] is left, result[1] is right, or null if invalid range
     */
    @JvmStatic
    fun getNeighbourLineEndings(text: String, pos: Int, posEnd: Int): IntArray? {
        val len = text.length
        var p = pos
        var pEnd = posEnd

        if (p < len && p >= 0 && text[p] == '\n') {
            p--
        }

        p = p.coerceIn(0, len - 1)
        pEnd = pEnd.coerceIn(0, len - 1)

        if (p == len) p--
        if (p < 0 || p > len) return null

        p = maxOf(0, text.lastIndexOf("\n", p))
        pEnd = text.indexOf("\n", pEnd)

        if (pEnd < 0 || pEnd >= len - 1) {
            pEnd = len
        }

        if (p == 0 && p == pEnd && pEnd + 1 <= len) {
            pEnd++
        }

        return if (p <= len && pEnd <= len && p <= pEnd) {
            intArrayOf(p, pEnd)
        } else {
            null
        }
    }

    /**
     * Returns begin of line starting from startPosition down to 0.
     */
    @JvmStatic
    fun beginOfLine(text: String, startPosition: Int): Int {
        return getNeighbourLineEndings(text, startPosition, startPosition)?.get(0) ?: 0
    }

    /**
     * Returns end of line starting from startPosition.
     */
    @JvmStatic
    fun endOfLine(text: String, startPosition: Int): Int {
        return getNeighbourLineEndings(text, startPosition, startPosition)?.get(1) ?: text.length
    }

    /**
     * Remove lines of text around the specified range.
     */
    @JvmStatic
    fun removeLinesOfTextAround(text: String, pos: Int, posEnd: Int): String {
        val endings = getNeighbourLineEndings(text, pos, posEnd)
        return if (endings != null) {
            text.substring(0, pos) + text.substring(posEnd)
        } else {
            text
        }
    }

    /**
     * Generate a UUID that starts with human readable datetime, using 8-4-4-4-12 UUID grouping.
     * While there are zero guarantees, you will get most relevant datetime information out of a huuid.
     * Plays nice with 'sort by name' in file managers and other tools.
     *
     * Example based on "Mon Jan 2 15:04:05 MST 2006", deviceID dddd, 430 milliseconds, random string ffffffff, timezone MST=UTC+07:00:
     * 20060102-1504-0543-070a-ddddffffffff
     *
     * Format detail: Milliseconds -> first two digits(=Centiseconds); UTC+/- -> a/f instead of last minute digit
     *
     * @param hostid4c 4-character host identifier (hex)
     * @return Human-readable UUID
     */
    @JvmStatic
    fun newHuuid(hostid4c: String?): String {
        val sdf = SimpleDateFormat("yyyyMMdd-hhmm-ssSSS'%STRIP1BEFORE%'-'%HOSTID%%RAND%'", Locale.ENGLISH)
        val rnd8c = String.format("%08x", Random().nextInt())
        val hostId = ((hostid4c ?: "") + "0000").substring(0, 4).replace(Regex("[^A-Fa-f0-9]"), "0")

        return sdf.format(Date())
            .replace("%HOSTID%", hostId)
            .replace("%RAND%", rnd8c)
            .replace(Regex(".%STRIP1BEFORE%"), "")
            .lowercase()
    }

    /**
     * Convert string to Title Case.
     */
    @JvmStatic
    fun toTitleCase(str: String): String {
        val delimiters = " '-/#."
        val sb = StringBuilder()
        var nextUppercase = true

        for (c in str) {
            sb.append(if (nextUppercase) c.uppercaseChar() else c.lowercaseChar())
            nextUppercase = c in delimiters
        }

        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Encode string to Base64.
     */
    @JvmStatic
    fun toBase64(s: String): String {
        return try {
            toBase64(s.toByteArray(charset(UTF8)))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Encode bytes to Base64.
     */
    @JvmStatic
    fun toBase64(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (ignored: Exception) {
            ""
        }
    }

    /**
     * Decode Base64 bytes.
     */
    @JvmStatic
    fun fromBase64(bytes: ByteArray): ByteArray {
        return Base64.decode(bytes, Base64.DEFAULT)
    }

    /**
     * Decode Base64 string to string.
     */
    @JvmStatic
    fun fromBase64ToString(s: String): String {
        return try {
            String(fromBase64(s.toByteArray(charset(UTF8))), charset(UTF8))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Try to parse an integer, return default value if parsing fails.
     */
    @JvmStatic
    fun tryParseInt(value: String?, defaultValue: Int): Int {
        return try {
            value?.toInt() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    /**
     * Convert varargs array to ArrayList.
     */
    @JvmStatic
    fun <T> toArrayList(vararg array: T): ArrayList<T> {
        return ArrayList(array.toList())
    }

    /**
     * Check if string is null, empty, or contains only whitespace.
     */
    @JvmStatic
    fun isNullOrEmpty(str: CharSequence?): Boolean {
        return str == null || str.isEmpty() || str.toString().trim().isEmpty()
    }

    /**
     * Convert an int color to a hex string. Optionally including alpha value.
     *
     * @param intColor  The color coded in int
     * @param withAlpha Optional; Set first bool parameter to true to also include alpha value
     */
    @JvmStatic
    fun colorToHexString(intColor: Int, vararg withAlpha: Boolean): String {
        val includeAlpha = withAlpha.isNotEmpty() && withAlpha[0]
        return if (includeAlpha) {
            String.format("#%08X", 0xFFFFFFFF.toInt() and intColor)
        } else {
            String.format("#%06X", 0xFFFFFF and intColor)
        }
    }

    /**
     * Convert escape sequences in string to escaped special characters.
     * For example, convert A\tB -> A    B
     * Or A\nB -> A
     *              B
     *
     * @param input Input string
     * @return String with escaped sequences converted
     */
    @JvmStatic
    fun unescapeString(input: String): String {
        val builder = StringBuilder()
        var isEscaped = false

        for (current in input) {
            if (isEscaped) {
                when (current) {
                    't' -> builder.append('\t')
                    'b' -> builder.append('\b')
                    'r' -> builder.append('\r')
                    'n' -> builder.append('\n')
                    'f' -> builder.append('\u000C') // form feed
                    else -> {
                        // Replace anything else with the literal pattern
                        builder.append('\\')
                        builder.append(current)
                    }
                }
                isEscaped = false
            } else if (current == '\\') {
                isEscaped = true
            } else {
                builder.append(current)
            }
        }

        // Handle trailing slash
        if (isEscaped) {
            builder.append('\\')
        }

        return builder.toString()
    }

    /**
     * Pretty print JSON string with indentation.
     */
    @JvmStatic
    fun jsonPrettyPrint(input: String): String? {
        return try {
            when (JSONTokener(input).nextValue()) {
                is JSONObject -> JSONObject(input).toString(2)
                else -> JSONArray(input).toString(2)
            }
        } catch (ignored: Exception) {
            null
        }
    }

    /**
     * Count number of instances of 'find' in 'text'.
     *
     * @param text Text to search
     * @param find Substring to match
     * @return count
     */
    @JvmStatic
    fun countSubstrings(text: String, find: String): Int {
        var index = 0
        var count = 0
        while (text.indexOf(find, index).also { index = it } != -1) {
            index += find.length
            count++
        }
        return count
    }

    /**
     * Pad string on left up to size.
     *
     * @param obj  Converted to string
     * @param size Total length after padding
     * @param c    Character to pad with
     * @return Padded string
     */
    @JvmStatic
    fun padLeft(obj: Any, size: Int, c: Char): String {
        val text = obj.toString()
        return repeatChars(c, size - text.length) + text
    }

    /**
     * Repeat a char count times.
     *
     * @param character Char to repeat
     * @param count     Times to repeat
     * @return String with repeated chars
     */
    @JvmStatic
    fun repeatChars(character: Char, count: Int): String {
        return if (count <= 0) "" else character.toString().repeat(count)
    }

    /**
     * Find all positions of a character in text.
     */
    @JvmStatic
    fun findChar(text: CharSequence, c: Char): List<Int> {
        return findChar(text, c, 0, text.length)
    }

    /**
     * Find all positions of a character in text within range.
     */
    @JvmStatic
    fun findChar(text: CharSequence, c: Char, start: Int, end: Int): List<Int> {
        val posns = mutableListOf<Int>()
        for (i in start until end) {
            if (text[i] == c) {
                posns.add(i)
            }
        }
        return posns
    }

    /**
     * Iterate over lines in a text.
     *
     * @param text     Text to iterate over
     * @param callback Callback to call for each line. Return false to stop iteration.
     */
    @JvmStatic
    fun forEachline(text: CharSequence, callback: GsCallback.b3<Int, Int, Int>) {
        val ends = findChar(text, '\n')
        var start = 0
        var i = 0

        while (i < ends.size) {
            val end = ends[i]
            if (!callback.callback(i, start, end)) {
                break
            }
            start = end + 1
            i++
        }

        callback.callback(i, start, text.length)
    }

    /**
     * Count instances of chars in entire sequence.
     */
    @JvmStatic
    fun countChars(s: CharSequence, vararg chars: Char): IntArray {
        return countChars(s, 0, s.length, *chars)
    }

    /**
     * Count instances of chars between start and end.
     *
     * @param s     Sequence to count in
     * @param start Start of section to count within
     * @param end   End of section to count within
     * @param chars Array of chars to count
     * @return Number of instances of each char in [start, end)
     */
    @JvmStatic
    fun countChars(s: CharSequence, start: Int, end: Int, vararg chars: Char): IntArray {
        // Faster specialization for the common single case
        if (chars.size == 1) {
            return intArrayOf(countChar(s, start, end, chars[0]))
        }

        val counts = IntArray(chars.size)
        val safeStart = maxOf(0, start)
        val safeEnd = minOf(end, s.length)

        for (i in safeStart until safeEnd) {
            val c = s[i]
            for (j in chars.indices) {
                if (c == chars[j]) {
                    counts[j]++
                }
            }
        }

        return counts
    }

    /**
     * Count instances of a single char in entire sequence.
     */
    @JvmStatic
    fun countChar(s: CharSequence, c: Char): Int {
        return countChar(s, 0, s.length, c)
    }

    /**
     * Count instances of a single char in a charsequence.
     */
    @JvmStatic
    fun countChar(s: CharSequence, start: Int, end: Int, c: Char): Int {
        val safeStart = maxOf(0, start)
        val safeEnd = minOf(end, s.length)
        var count = 0

        for (i in safeStart until safeEnd) {
            if (s[i] == c) {
                count++
            }
        }

        return count
    }

    /**
     * Check if range contains a newline character.
     */
    @JvmStatic
    fun isNewLine(source: CharSequence, start: Int, end: Int): Boolean {
        return isValidIndex(source, start, end - 1) &&
                (source[start] == '\n' || source[end - 1] == '\n')
    }

    /**
     * Check if all indices are valid for the given sequence.
     */
    @JvmStatic
    fun isValidIndex(s: CharSequence?, vararg indices: Int): Boolean {
        return s != null && inRange(0, s.length - 1, *indices)
    }

    /**
     * Check if all indices are valid selection positions.
     */
    @JvmStatic
    fun isValidSelection(s: CharSequence?, vararg indices: Int): Boolean {
        return s != null && inRange(0, s.length, *indices)
    }

    /**
     * Check if all values are in [min, max] inclusive.
     */
    @JvmStatic
    fun inRange(min: Int, max: Int, vararg values: Int): Boolean {
        return values.all { it in min..max }
    }

    /**
     * Convert map to JSON string.
     */
    @JvmStatic
    fun mapToJsonString(map: Map<String, String>): String {
        return JSONObject(map).toString()
    }

    /**
     * Convert JSON string to map.
     */
    @JvmStatic
    fun jsonStringToMap(jsonString: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()

        if (isNullOrEmpty(jsonString)) {
            return map
        }

        try {
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getString(key)
                map[key] = value
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return map
    }

    /**
     * Convert list to JSON string.
     */
    @JvmStatic
    fun listToJsonString(list: Collection<String>): String {
        return JSONArray(list).toString()
    }

    /**
     * Convert JSON string to list.
     */
    @JvmStatic
    fun jsonStringToList(jsonString: String): List<String> {
        val list = mutableListOf<String>()

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    /**
     * Check if text ends with suffix.
     */
    @JvmStatic
    fun endsWith(text: CharSequence?, suffix: CharSequence?): Boolean {
        if (text == null || suffix == null) return false
        if (text.length < suffix.length) return false
        if (suffix.isEmpty()) return true
        if (text === suffix) return true

        val offset = text.length - suffix.length
        for (i in suffix.indices) {
            if (text[offset + i] != suffix[i]) {
                return false
            }
        }

        return true
    }
}
