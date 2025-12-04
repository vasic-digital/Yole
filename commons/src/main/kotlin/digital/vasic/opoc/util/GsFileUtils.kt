/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import androidx.annotation.RequiresApi
import digital.vasic.opoc.format.GsTextUtils
import java.io.*
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import java.util.zip.CRC32

/**
 * Comprehensive file utilities for Android.
 *
 * Provides functionality for:
 * - File reading/writing (text and binary)
 * - File operations (copy, rename, delete, touch)
 * - MIME type detection
 * - File sorting and filtering
 * - Hash computation (SHA-256, CRC32)
 * - Path manipulation
 * - File metadata extraction
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object GsFileUtils {

    @SuppressLint("ConstantLocale")
    @JvmField
    val INITIAL_LOCALE: Locale = Locale.getDefault()

    @JvmField
    val DATEFORMAT_IMG = SimpleDateFormat("yyyyMMdd-HHmmss", INITIAL_LOCALE)

    private const val BUFFER_SIZE = 4096
    private val MIME_TYPE_CACHE = ConcurrentHashMap<String, String>()

    // Sort constants
    const val SORT_BY_NAME = "NAME"
    const val SORT_BY_FILESIZE = "FILESIZE"
    const val SORT_BY_MTIME = "MTIME"
    const val SORT_BY_MIMETYPE = "MIMETYPE"

    /**
     * Info of various types about a file.
     */
    data class FileInfo(
        @JvmField var hasBom: Boolean = false,
        @JvmField var ioError: Boolean = false
    ) : Serializable

    /**
     * Read input stream fast with BOM detection.
     */
    @JvmStatic
    fun readInputStreamFast(inputStream: InputStream, info: FileInfo? = null): Pair<String, FileInfo> {
        val fileInfo = info ?: FileInfo()

        try {
            val result = ByteArrayOutputStream()
            val bomBuffer = ByteArray(3)
            val bomReadLength = inputStream.read(bomBuffer)

            fileInfo.hasBom = bomReadLength == 3 &&
                    bomBuffer[0] == 0xEF.toByte() &&
                    bomBuffer[1] == 0xBB.toByte() &&
                    bomBuffer[2] == 0xBF.toByte()

            if (!fileInfo.hasBom && bomReadLength > 0) {
                result.write(bomBuffer, 0, bomReadLength)
            }

            if (bomReadLength < 3) {
                return Pair(result.toString("UTF-8"), fileInfo)
            }

            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } != -1) {
                result.write(buffer, 0, length)
            }

            return Pair(result.toString("UTF-8"), fileInfo)
        } catch (e: IOException) {
            e.printStackTrace()
            fileInfo.ioError = true
        }

        return Pair("", fileInfo)
    }

    /**
     * Read text file fast with BOM detection.
     */
    @JvmStatic
    fun readTextFileFast(file: File): Pair<String, FileInfo> {
        val info = FileInfo()

        try {
            FileInputStream(file).use { inputStream ->
                return readInputStreamFast(inputStream, info)
            }
        } catch (e: FileNotFoundException) {
            System.err.println("readTextFileFast: File $file not found.")
        } catch (e: IOException) {
            System.err.println("readTextFileFast: File $file could not be read.")
            info.ioError = true
        }

        return Pair("", info)
    }

    /**
     * Read stream with known size.
     */
    @JvmStatic
    fun readCloseStreamWithSize(stream: InputStream, size: Int): ByteArray {
        val data = ByteArray(size)
        try {
            DataInputStream(stream).use { dis ->
                dis.readFully(data)
            }
        } catch (ignored: IOException) {
        }
        return data
    }

    /**
     * Read text file and return contents as string.
     */
    @JvmStatic
    fun readTextFile(file: File): String {
        return try {
            readCloseTextStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            System.err.println("readTextFile: File $file not found.")
            ""
        }
    }

    /**
     * Read text stream and return as single string.
     */
    @JvmStatic
    fun readCloseTextStream(stream: InputStream): String {
        return readCloseTextStream(stream, true)[0]
    }

    /**
     * Read text stream and return as list of lines or single concatenated string.
     */
    @JvmStatic
    fun readCloseTextStream(stream: InputStream, concatToOneString: Boolean): List<String> {
        val lines = ArrayList<String>()
        var line = ""

        try {
            val sb = StringBuilder()
            BufferedReader(InputStreamReader(stream)).use { reader ->
                while (reader.readLine().also { line = it } != null) {
                    if (concatToOneString) {
                        sb.append(line).append('\n')
                    } else {
                        lines.add(line)
                    }
                }
                line = sb.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (concatToOneString) {
            lines.clear()
            lines.add(line)
        }

        return lines
    }

    /**
     * Read binary file and return byte array.
     */
    @JvmStatic
    fun readBinaryFile(file: File): ByteArray {
        return try {
            readCloseBinaryStream(FileInputStream(file), file.length().toInt())
        } catch (e: FileNotFoundException) {
            System.err.println("readBinaryFile: File $file not found.")
            ByteArray(0)
        }
    }

    /**
     * Read binary stream with known byte count.
     */
    @JvmStatic
    fun readCloseBinaryStream(stream: InputStream, byteCount: Int): ByteArray {
        val buf = ByteArray(byteCount)
        var totalBytesRead = 0

        try {
            BufferedInputStream(stream).use { reader ->
                while (totalBytesRead < byteCount) {
                    val bytesRead = reader.read(buf, totalBytesRead, byteCount - totalBytesRead)
                    if (bytesRead > 0) {
                        totalBytesRead += bytesRead
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return buf
    }

    /**
     * Read binary stream of unknown size.
     */
    @JvmStatic
    fun readCloseBinaryStream(stream: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()

        try {
            stream.use {
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (it.read(buffer).also { read = it } != -1) {
                    baos.write(buffer, 0, read)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return baos.toByteArray()
    }

    /**
     * Write file with optional BOM.
     */
    @JvmStatic
    fun writeFile(file: File, data: ByteArray, options: FileInfo?): Boolean {
        return try {
            FileOutputStream(file, false).use { output ->
                if (options != null && options.hasBom) {
                    output.write(0xEF)
                    output.write(0xBB)
                    output.write(0xBF)
                }
                output.write(data)
                output.flush()
            }
            true
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    /**
     * Write file with string data.
     */
    @JvmStatic
    fun writeFile(file: File, data: String, options: FileInfo?): Boolean {
        return writeFile(file, data.toByteArray(), options)
    }

    /**
     * Copy file from source to destination.
     */
    @JvmStatic
    fun copyFile(src: File, dst: File): Boolean {
        // Just touch file if src is empty
        if (src.length() == 0L) {
            return touch(dst)
        }

        return try {
            FileInputStream(src).use { inputStream ->
                FileOutputStream(dst).use { outputStream ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var len: Int
                    while (inputStream.read(buf).also { len = it } > 0) {
                        outputStream.write(buf, 0, len)
                    }
                    outputStream.flush()
                }
            }
            true
        } catch (ex: IOException) {
            false
        }
    }

    /**
     * Copy file to output stream.
     */
    @JvmStatic
    fun copyFile(src: File, os: OutputStream): Boolean {
        return try {
            FileInputStream(src).use { inputStream ->
                os.use { outputStream ->
                    val buf = ByteArray(BUFFER_SIZE)
                    var len: Int
                    while (inputStream.read(buf).also { len = it } > 0) {
                        outputStream.write(buf, 0, len)
                    }
                }
            }
            true
        } catch (ex: IOException) {
            false
        }
    }

    /**
     * Check if file contains any of the given needles (case-insensitive).
     * Returns index of found needle or -1 if none found.
     * Needles MUST be in lower-case.
     */
    @JvmStatic
    fun fileContains(file: File, vararg needles: String): Int {
        try {
            FileInputStream(file).use { input ->
                BufferedReader(InputStreamReader(input)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        for (i in needles.indices) {
                            if (line!!.lowercase(Locale.ROOT).contains(needles[i])) {
                                return i
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return -1
    }

    /**
     * Delete file or directory recursively.
     */
    @JvmStatic
    fun deleteRecursive(file: File): Boolean {
        var ok = true
        if (file.exists()) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    ok = ok and deleteRecursive(child)
                }
            }
            ok = ok and file.delete()
        }
        return ok
    }

    /**
     * Check if string has any of the given extensions.
     */
    @JvmStatic
    fun hasExtension(str: String, vararg extensions: String): Boolean {
        val lc = str.lowercase(Locale.ROOT)
        return extensions.any { ext ->
            lc.endsWith(".${ext.lowercase(Locale.ROOT)}")
        }
    }

    /**
     * Rename file with fallback to copy+delete.
     */
    @JvmStatic
    fun renameFile(srcFile: File, destFile: File): Boolean {
        var src = srcFile

        if (src.absolutePath == destFile.absolutePath) {
            return false
        }

        // renameTo will fail in case of case-changed filename in same dir. Even on case-sensitive FS!!!
        if (src.parent == destFile.parent &&
            src.name.lowercase(Locale.getDefault()) == destFile.name.lowercase(Locale.getDefault())
        ) {
            val tmpFile = File(destFile.parent, "${UUID.randomUUID().leastSignificantBits}.tmp")
            if (!tmpFile.exists()) {
                renameFile(src, tmpFile)
                src = tmpFile
            }
        }

        if (!src.renameTo(destFile)) {
            if (copyFile(src, destFile) && !src.delete()) {
                if (!destFile.delete()) {
                    return false
                }
                return false
            }
        }

        return true
    }

    /**
     * Rename file in same folder.
     */
    @JvmStatic
    fun renameFileInSameFolder(srcFile: File, destFilename: String): Boolean {
        return renameFile(srcFile, File(srcFile.parent, destFilename))
    }

    /**
     * Touch file (create if doesn't exist, update timestamp if exists).
     */
    @JvmStatic
    fun touch(file: File): Boolean {
        return try {
            if (!file.exists()) {
                FileOutputStream(file).close()
            }
            file.setLastModified(System.currentTimeMillis())
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Get relative path from src to dest.
     */
    @JvmStatic
    fun relativePath(src: File, dest: File): String {
        val srcDir = if (src.isFile) src.parentFile else src
        val srcStr = getPath(srcDir)
        val destStr = getPath(dest)

        val srcSplit = srcStr.split(Pattern.quote(File.separator).toRegex()).toTypedArray()
        val destSplit = destStr.split(Pattern.quote(File.separator).toRegex()).toTypedArray()

        var commonLength = 0
        while (commonLength < destSplit.size && commonLength < srcSplit.size) {
            if (destSplit[commonLength] != srcSplit[commonLength]) break
            commonLength++
        }

        val sb = StringBuilder()

        if (commonLength != srcSplit.size) {
            for (iUpperDir in commonLength until srcSplit.size) {
                sb.append("..")
                sb.append(File.separator)
            }
        }

        for (i in commonLength until destSplit.size) {
            sb.append(destSplit[i])
            sb.append(File.separator)
        }

        if (!dest.isFile && GsTextUtils.endsWith(sb, File.separator)) {
            sb.delete(sb.length - File.separator.length, sb.length)
        }

        if (sb.isEmpty()) {
            sb.append(".")
        }

        return sb.toString()
    }

    /**
     * Gather MIME type using various detection methods.
     */
    private fun gatherMimeType(file: File?): String {
        if (file == null) {
            return "*/*"
        }

        val ext = getFilenameExtension(file).replace(".", "")

        if (file.isDirectory) {
            return "inode/directory"
        } else if (ext.matches(Regex("ya?ml"))) {
            return "text/yaml"
        } else if (ext.matches(Regex("json.*"))) {
            return "text/json"
        } else if (ext.matches(Regex("((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(mdwn)|(mdx)|(rmd))"))) {
            return "text/markdown"
        } else if (ext.matches(Regex("(te?xt)|(taskpaper)"))) {
            return "text/plain"
        } else if (ext.matches(Regex("org"))) {
            return "text/org"
        } else if (ext.matches(Regex("webp"))) {
            return "image/webp"
        } else if (ext.matches(Regex("jpe?g"))) {
            return "image/jpeg"
        } else if (ext.matches(Regex("png"))) {
            return "image/png"
        } else if (ext.matches(Regex("a(sciidoc)?doc"))) {
            return "text/asciidoc"
        }

        var t: String?

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                t = Files.probeContentType(file.toPath())
                if (!GsTextUtils.isNullOrEmpty(t)) {
                    return t!!
                }
            }
        } catch (ignored: Exception) {
        }

        try {
            t = URLConnection.guessContentTypeFromName(file.name.replace(".jenc", ""))
            if (!GsTextUtils.isNullOrEmpty(t)) {
                return t!!
            }
        } catch (ignored: Exception) {
        }

        // Try extracting by running shell file -b on the file
        try {
            val cmd = "file -b ${file.absolutePath.replace(" ", "\\ ").replace(";", "").replace("&", "").replace("$", "")}"
            val p = Runtime.getRuntime().exec(cmd)
            BufferedReader(InputStreamReader(p.inputStream)).use { reader ->
                t = reader.readLine()?.trim()?.lowercase(Locale.ROOT)
                try {
                    p.destroy()
                } catch (ignored: Exception) {
                }

                if (!GsTextUtils.isNullOrEmpty(t) && !t!!.contains("not found")) {
                    when {
                        t!!.contains("ascii text") || t!!.contains("empty") -> return "text/plain"
                        t!!.contains("text") || t!!.contains("script") -> return "text/x-${t!!.replace(Regex("(\\s|-)*script(\\s|-)*"), "").replace(" ", "-")}"
                    }
                }
            }
        } catch (ignored: Exception) {
        }

        return "*/*"
    }

    /**
     * Try to detect MIME type by backwards compatible methods.
     * Uses caching for performance.
     */
    @JvmStatic
    fun getMimeType(file: File): String {
        val fp = file.absolutePath
        return MIME_TYPE_CACHE.getOrPut(fp) { gatherMimeType(file) }
    }

    /**
     * Check if file is a text file based on MIME type.
     */
    @JvmStatic
    fun isTextFile(file: File): Boolean {
        val mime = getMimeType(file)
        return mime.startsWith("text/") || mime.contains("xml") && !mime.contains("openxml")
    }

    /**
     * Check if file contents are plain text by reading first 1KB.
     */
    @JvmStatic
    fun isContentsPlainText(file: File): Boolean {
        // Empty files are considered text files
        if (file.length() == 0L) {
            return true
        }

        return try {
            FileInputStream(file).use { fis ->
                val bytes = readCloseStreamWithSize(fis, 1024)
                Charset.forName("UTF-8").newDecoder().decode(java.nio.ByteBuffer.wrap(bytes))
                true
            }
        } catch (ignored: Exception) {
            false
        }
    }

    /**
     * Retrieve text file summary (characters, lines, words).
     */
    @JvmStatic
    fun retrieveTextFileSummary(
        file: File,
        numCharacters: AtomicInteger,
        numLines: AtomicInteger,
        numWords: AtomicInteger
    ) {
        try {
            BufferedReader(FileReader(file)).use { br ->
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    numLines.incrementAndGet()
                    numCharacters.set(numCharacters.get() + line!!.length)
                    if (line!!.isNotEmpty()) {
                        numWords.set(numWords.get() + line!!.split("\\s+".toRegex()).size)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            numCharacters.set(-1)
            numLines.set(-1)
            numWords.set(-1)
        }
    }

    /**
     * Format filesize to human readable format.
     */
    @JvmStatic
    fun getReadableFileSize(size: Long, abbreviation: Boolean): String {
        if (size <= 0) {
            return "0B"
        }

        val units = if (abbreviation) {
            arrayOf("B", "kB", "MB", "GB", "TB")
        } else {
            arrayOf("Bytes", "Kilobytes", "Megabytes", "Gigabytes", "Terabytes")
        }

        val unit = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val value = size / Math.pow(1024.0, unit.toDouble())

        return "${DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value)} ${units[unit]}"
    }

    /**
     * Get time difference in hours, minutes, seconds.
     */
    @JvmStatic
    fun getTimeDiffHMS(now: Long, past: Long): IntArray {
        val diff = Math.abs(now - past)
        return intArrayOf(
            (diff / (1000 * 60 * 60)).toInt(), // hours
            (diff / (1000 * 60) % 60).toInt(),  // minutes
            (diff / 1000 % 60).toInt()          // seconds
        )
    }

    /**
     * Get human readable byte count in SI units.
     */
    @JvmStatic
    fun getHumanReadableByteCountSI(bytes: Long): String {
        val out = when {
            bytes < 1000 -> String.format(Locale.getDefault(), "%d %s", bytes, "B")
            bytes < 1000000 -> String.format(Locale.getDefault(), "%.2f %s", bytes / 1000f, "KB")
            bytes < 1000000000 -> String.format(Locale.getDefault(), "%.2f %s", bytes / 1000000f, "MB")
            bytes < 1000000000000L -> String.format(Locale.getDefault(), "%.2f %s", bytes / 1000000000f, "GB")
            else -> String.format(Locale.getDefault(), "%.2f %s", bytes / 1000000000000f, "TB")
        }
        return out.replace(Regex(",0+( [A-Z])"), "$1")
    }

    /**
     * Join file with child segments.
     */
    @JvmStatic
    fun join(file: File, vararg childSegments: String): File {
        var result = file
        childSegments.forEach { segment ->
            result = File(result, segment)
        }
        return result
    }

    /**
     * Join multiple files.
     */
    @JvmStatic
    fun join(vararg files: File): File? {
        if (files.isEmpty()) {
            return null
        }

        var result = files[0]
        for (i in 1 until files.size) {
            result = File(result, files[i].absolutePath)
        }
        return result
    }

    /**
     * Compute SHA-256 hash of file.
     */
    @JvmStatic
    fun sha256(file: File?): String? {
        if (file == null || !file.exists() || !file.isFile) {
            return null
        }

        return try {
            FileInputStream(file).use { fis ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                val hashBytes = digest.digest()
                hashBytes.joinToString("") { "%02x".format(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Compute CRC32 checksum of CharSequence.
     */
    @JvmStatic
    fun crc32(data: CharSequence): Long {
        val alg = CRC32()
        for (i in data.indices) {
            val c = data[i]
            // Upper and lower bytes
            alg.update((c.code and 0xff).toByte().toInt())
            alg.update((c.code shr 8).toByte().toInt())
        }
        return alg.value
    }

    /**
     * Compute CRC32 checksum of byte array.
     */
    @JvmStatic
    fun crc32(data: ByteArray): Long {
        val alg = CRC32()
        alg.update(data)
        return alg.value
    }

    /**
     * Check if file exists (case-sensitive or case-insensitive).
     */
    @JvmStatic
    fun fileExists(checkFile: File?, vararg caseInsensitive: Boolean): Boolean {
        val isAndroid = System.getProperty("java.specification.vendor")?.contains("Android") == true
        val sensitive = !isAndroid && (caseInsensitive.isEmpty() || !caseInsensitive[0])

        if (checkFile?.parentFile == null) {
            return false
        }

        val files = checkFile.parentFile?.listFiles() ?: return false
        val checkFilename = checkFile.name

        return files.any { existingFile ->
            val existingName = existingFile.name
            if (sensitive) {
                existingName == checkFilename
            } else {
                existingName.equals(checkFilename, ignoreCase = true)
            }
        }
    }

    /**
     * Get filename without extension.
     */
    @JvmStatic
    fun getFilenameWithoutExtension(file: File): String {
        return getNameWithoutExtension(file.name)
    }

    /**
     * Get name without extension.
     */
    @JvmStatic
    fun getNameWithoutExtension(fileName: String): String {
        val doti = fileName.lastIndexOf(".")
        return if (doti < 0) fileName else fileName.substring(0, doti)
    }

    /**
     * Get filename extension (without dot).
     */
    @JvmStatic
    fun getFilenameExtension(file: File): String {
        return getFilenameExtension(file.name)
    }

    /**
     * Get filename extension from string (without dot).
     * Examples: "" -> "", "index" -> "", "index.html" -> "html", "my.website.html" -> "html"
     */
    @JvmStatic
    fun getFilenameExtension(name: String?): String {
        return if (name != null && name.contains(".")) {
            name.replace(Regex(".*?\\.([^.]+)$"), "$1")
        } else {
            ""
        }
    }

    /**
     * Get filtered filename without disallowed characters.
     */
    @JvmStatic
    fun getFilteredFilenameWithoutDisallowedChars(str: String, vararg a1NoRCEguard: Boolean): String {
        var result = str
        val noRCEguard = a1NoRCEguard.isNotEmpty() && a1NoRCEguard[0]
        val disallowedChars = "<|[\\?*:\"\n\r\t\u0000]/>" + if (noRCEguard) "" else "`$%;=&#@"

        disallowedChars.forEach { disallowed ->
            result = result.replace(disallowed.toString(), "")
        }

        return result.trim()
    }

    /**
     * Generate filename with timestamp.
     * Examples: Screenshot_20210208-184301_Trebuchet.png, IMG_20190511-230845.jpg
     */
    @JvmStatic
    fun getFilenameWithTimestamp(vararg A0prefixA1postfixA2ext: String): String {
        val prefix = (if (A0prefixA1postfixA2ext.isNotEmpty() && !TextUtils.isEmpty(A0prefixA1postfixA2ext[0])) {
            A0prefixA1postfixA2ext[0]
        } else {
            "Screenshot"
        } + "_").trim().replace(Regex("^_$"), "")

        val postfix = ("_" + if (A0prefixA1postfixA2ext.size > 1 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[1])) {
            A0prefixA1postfixA2ext[1]
        } else {
            ""
        }).trim().replace(Regex("^_$"), "")

        val ext = if (A0prefixA1postfixA2ext.size > 2 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[2])) {
            A0prefixA1postfixA2ext[2]
        } else {
            "jpg"
        }

        var filename = String.format(
            "%s%s%s.%s",
            prefix.trim(),
            DATEFORMAT_IMG.format(Date()),
            postfix.trim(),
            ext.lowercase().replace(".", "").replace("jpeg", "jpg")
        )

        filename = getFilteredFilenameWithoutDisallowedChars(filename)
        return filename
    }

    /**
     * Get prefix from filename (before first underscore).
     */
    @JvmStatic
    fun getPrefix(name: String): String {
        return name.split("_")[0]
    }

    /**
     * Make sort key for file based on sort type.
     */
    private fun makeSortKey(sortBy: String, file: File): String {
        val name = file.name.lowercase()
        return when (sortBy) {
            SORT_BY_MTIME -> file.lastModified().toString() + name
            SORT_BY_FILESIZE -> String.format("%015d", file.length()) + name
            SORT_BY_MIMETYPE -> getMimeType(file).lowercase() + name
            else -> name // SORT_BY_NAME and any other value
        }
    }

    /**
     * File sort order configuration.
     */
    class SortOrder {
        @JvmField var sortByType = SORT_BY_NAME
        @JvmField var reverse = false
        @JvmField var showDotFiles = false
        @JvmField var folderFirst = true
        @JvmField var isFolderLocal = false

        override fun toString(): String {
            val map = mapOf(
                SORT_BY_KEY to sortByType,
                REVERSE_KEY to reverse.toString(),
                SHOW_DOT_FILES_KEY to showDotFiles.toString(),
                FOLDER_FIRST_KEY to folderFirst.toString()
            )
            return GsTextUtils.mapToJsonString(map)
        }

        companion object {
            private const val SORT_BY_KEY = "SORT_BY"
            private const val REVERSE_KEY = "REVERSE"
            private const val SHOW_DOT_FILES_KEY = "SHOW_DOT_FILES"
            private const val FOLDER_FIRST_KEY = "FOLDER_FIRST"

            @JvmStatic
            fun fromString(json: String): SortOrder {
                val fso = SortOrder()
                val map = GsTextUtils.jsonStringToMap(json)
                fso.sortByType = GsCollectionUtils.getOrDefault(map, SORT_BY_KEY, SORT_BY_NAME)
                fso.reverse = GsCollectionUtils.getOrDefault(map, REVERSE_KEY, "false").toBoolean()
                fso.showDotFiles = GsCollectionUtils.getOrDefault(map, SHOW_DOT_FILES_KEY, "false").toBoolean()
                fso.folderFirst = GsCollectionUtils.getOrDefault(map, FOLDER_FIRST_KEY, "true").toBoolean()
                return fso
            }
        }
    }

    /**
     * Sort files according to sort order.
     */
    @JvmStatic
    fun sortFiles(filesToSort: MutableCollection<File>?, order: SortOrder) {
        if (filesToSort.isNullOrEmpty()) {
            return
        }

        try {
            val copy = filesToSort !is MutableList<*>
            val sortable = if (copy) ArrayList(filesToSort) else filesToSort as MutableList<File>

            GsCollectionUtils.keySort(sortable) { f -> makeSortKey(order.sortByType, f) }

            if (order.reverse) {
                sortable.reverse()
            }

            if (order.folderFirst) {
                GsCollectionUtils.keySort(sortable) { f -> !f.isDirectory }
            }

            if (copy) {
                filesToSort.clear()
                filesToSort.addAll(sortable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if file is writable.
     */
    @JvmStatic
    fun isWritable(file: File?): Boolean {
        if (file == null) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.isWritable(file.toPath())
        } else {
            file.canWrite()
        }
    }

    /**
     * Check if file can be created (first existing ancestor is writable).
     */
    @JvmStatic
    fun canCreate(file: File?): Boolean {
        var f = file
        while (f != null && !f.exists()) {
            f = f.parentFile
        }
        return isWritable(f)
    }

    /**
     * Check if test is a child of parent.
     */
    @JvmStatic
    fun isChild(parent: File?, test: File?): Boolean {
        if (test == null || parent == null || parent == test) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return test.toPath().toAbsolutePath().startsWith(parent.toPath().toAbsolutePath())
        }

        var current: File? = test
        do {
            current = current?.parentFile
            if (parent == current) {
                return true
            }
        } while (current != null)

        return false
    }

    /**
     * Find non-conflicting destination filename.
     */
    @JvmStatic
    fun findNonConflictingDest(destDir: File, name: String): File {
        var dest = File(destDir, name)
        val splits = name.split(".")
        val baseName = splits[0]
        val extension = splits.drop(1).joinToString(".")
        var i = 1

        while (dest.exists()) {
            dest = File(destDir, String.format("%s_%d.%s", baseName, i, extension))
            i++
        }

        return dest
    }

    /**
     * Check if string is URI or file path.
     */
    @JvmStatic
    fun isUriOrFilePath(path: String): Boolean {
        return try {
            "file" == Uri.parse(path).scheme
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Copy content from URI to file.
     */
    @JvmStatic
    fun copyUriToFile(context: Context, source: Uri, dest: File) {
        try {
            FileOutputStream(dest, false).use { outputStream ->
                context.contentResolver.openInputStream(source)?.use { inputStream ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        } catch (ignored: IOException) {
        }
    }

    /**
     * Make file absolute relative to base directory.
     */
    @JvmStatic
    fun makeAbsolute(path: String?, base: File?): File? {
        return if (path != null) makeAbsolute(File(path.trim()), base) else null
    }

    /**
     * Make file absolute relative to base directory.
     */
    @JvmStatic
    fun makeAbsolute(file: File?, base: File?): File? {
        if (file == null || base == null) {
            return null
        }

        if (file.isAbsolute) {
            return file
        }

        val full = base.absolutePath + File.separator + file.path
        val parts = full.split(Pattern.quote(File.separator).toRegex()).toTypedArray()

        val stack = Stack<String>()
        for (part in parts) {
            when {
                part == ".." && stack.isNotEmpty() -> stack.pop()
                part != "." -> stack.add(part)
            }
        }

        return File(stack.joinToString(File.separator))
    }

    /**
     * Search files matching glob pattern (requires Android O+).
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @JvmStatic
    fun searchFiles(root: File, glob: String): List<File>? {
        return try {
            val pattern = if (glob.trim().startsWith("glob:")) glob.trim() else "glob:${glob.trim()}"
            val matcher = FileSystems.getDefault().getPathMatcher(pattern)
            val found = ArrayList<File>()

            Files.walkFileTree(root.toPath(), object : SimpleFileVisitor<Path>() {
                override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (matcher.matches(path)) {
                        found.add(path.toFile())
                    }
                    return FileVisitResult.CONTINUE
                }
            })

            found
        } catch (e: IOException) {
            Log.d(GsFileUtils::class.java.name, e.toString())
            null
        }
    }

    /**
     * Get canonical path or absolute path as fallback.
     */
    @JvmStatic
    fun getPath(file: File?): String {
        return try {
            file?.canonicalPath ?: ""
        } catch (e: IOException) {
            file?.absolutePath ?: ""
        } catch (e: NullPointerException) {
            ""
        }
    }

    /**
     * Check if file is a directory.
     */
    @JvmStatic
    fun isDirectory(file: File?): Boolean {
        return file?.isDirectory == true
    }

    /**
     * Check if file exists.
     */
    @JvmStatic
    fun exists(file: File?): Boolean {
        return file?.exists() == true
    }

    /**
     * Check if file is a symbolic link.
     */
    @JvmStatic
    fun isSymbolicLink(file: File): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.isSymbolicLink(file.toPath())
        } else {
            try {
                val actualParent = file.canonicalFile.parentFile
                val parent = file.parentFile
                actualParent == null || actualParent != parent
            } catch (ignored: IOException) {
                false
            } catch (ignored: NullPointerException) {
                false
            }
        }
    }
}
