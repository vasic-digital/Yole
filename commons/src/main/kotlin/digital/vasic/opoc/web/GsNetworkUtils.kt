/*#######################################################
 *
 * SPDX-FileCopyrightText: 2021-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2021-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.web

import digital.vasic.opoc.util.GsFileUtils
import digital.vasic.opoc.wrapper.GsCallback
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset

/**
 * Network utilities for HTTP operations and file downloads.
 *
 * Provides functionality for:
 * - File downloads with progress tracking
 * - HTTP GET/POST/PATCH requests
 * - URL encoding/decoding
 * - Query parameter parsing
 * - Asynchronous HTTP requests
 */
@Suppress("unused", "MemberVisibilityCanBePrivate", "SpellCheckingInspection", "DEPRECATION")
object GsNetworkUtils {

    private const val UTF8 = "UTF-8"
    private const val BUFFER_SIZE = 4096

    const val GET = "GET"
    const val POST = "POST"
    const val PATCH = "PATCH"

    /**
     * Download file from URL to output file.
     * Creates parent directory if it doesn't exist.
     */
    @JvmStatic
    fun downloadFile(url: String, out: File): Boolean {
        return downloadFile(url, out, null)
    }

    /**
     * Download file from URL with progress callback.
     */
    @JvmStatic
    fun downloadFile(url: String, out: File, progressCallback: GsCallback.a1<Float>?): Boolean {
        return try {
            downloadFile(URL(url), out, progressCallback)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Download file from URL with progress callback.
     */
    @JvmStatic
    fun downloadFile(url: URL, outFile: File, progressCallback: GsCallback.a1<Float>?): Boolean {
        return downloadFile(url, outFile, null, progressCallback)
    }

    /**
     * Download file using existing connection with progress callback.
     */
    @JvmStatic
    fun downloadFile(
        url: URL,
        outFile: File,
        connection: HttpURLConnection?,
        progressCallback: GsCallback.a1<Float>?
    ): Boolean {
        var conn = connection
        var input: InputStream? = null
        var output: OutputStream? = null

        return try {
            if (conn == null) {
                conn = url.openConnection() as HttpURLConnection
            }
            conn.connect()

            input = if (conn.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                conn.inputStream
            } else {
                conn.errorStream
            }

            val parentFile = outFile.parentFile
            if (parentFile?.isDirectory == false) {
                if (!parentFile.mkdirs()) {
                    return false
                }
            }

            output = FileOutputStream(outFile)

            val buffer = ByteArray(BUFFER_SIZE)
            var count: Int
            var written = 0
            val invLength = 1f / conn.contentLength

            while (input.read(buffer).also { count = it } != -1) {
                output.write(buffer, 0, count)
                if (invLength != -1f && progressCallback != null) {
                    written += count
                    progressCallback.callback(written * invLength)
                }
            }

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }
            conn?.disconnect()
        }
    }

    /**
     * Perform HTTP call with method (GET, POST, etc) and no data.
     */
    @JvmStatic
    fun performCall(url: String, method: String): String {
        return try {
            performCall(URL(url), method, "")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Perform HTTP call with method and string data.
     */
    @JvmStatic
    fun performCall(url: String, method: String, data: String): String {
        return try {
            performCall(URL(url), method, data)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Perform HTTP call with URL-encoded parameters.
     */
    @JvmStatic
    fun performCall(url: String, method: String, params: HashMap<String, String>): String {
        return try {
            performCall(URL(url), method, encodeQuery(params))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Perform HTTP POST with JSON data.
     */
    @JvmStatic
    fun performCall(url: String, json: JSONObject): String {
        return performCall(url, POST, json)
    }

    /**
     * Perform HTTP call with JSON data.
     */
    @JvmStatic
    fun performCall(url: String, method: String, json: JSONObject): String {
        return try {
            performCall(URL(url), method, json.toString())
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Perform HTTP call with URL, method, and data.
     */
    private fun performCall(url: URL, method: String, data: String): String {
        return performCall(url, method, data, null)
    }

    /**
     * Perform HTTP call with existing connection.
     */
    @Suppress("CharsetObjectCanBeUsed")
    private fun performCall(
        url: URL,
        method: String,
        data: String,
        existingConnection: HttpURLConnection?
    ): String {
        return try {
            val connection = existingConnection ?: (url.openConnection() as HttpURLConnection)
            connection.requestMethod = method
            connection.doInput = true

            if (data.isNotEmpty()) {
                connection.doOutput = true
                val output = connection.outputStream
                output.write(data.toByteArray(Charset.forName("UTF-8")))
                output.flush()
                output.close()
            }

            val input = if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            GsFileUtils.readCloseTextStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Encode query parameters to URL-encoded string.
     */
    private fun encodeQuery(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true

        for ((key, value) in params) {
            if (first) {
                first = false
            } else {
                result.append("&")
            }

            result.append(URLEncoder.encode(key, UTF8))
            result.append("=")
            result.append(URLEncoder.encode(value, UTF8))
        }

        return result.toString()
    }

    /**
     * Parse URL-encoded query string to map.
     */
    @JvmStatic
    fun getDataMap(query: String): HashMap<String, String> {
        val result = HashMap<String, String>()
        val sb = StringBuilder()
        var name = ""

        try {
            for (i in query.indices) {
                when (val c = query[i]) {
                    '=' -> {
                        name = URLDecoder.decode(sb.toString(), UTF8)
                        sb.setLength(0)
                    }
                    '&' -> {
                        result[name] = URLDecoder.decode(sb.toString(), UTF8)
                        sb.setLength(0)
                    }
                    else -> {
                        sb.append(c)
                    }
                }
            }

            if (name.isNotEmpty()) {
                result[name] = URLDecoder.decode(sb.toString(), UTF8)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    /**
     * Perform async HTTP GET request.
     */
    @JvmStatic
    fun httpGetAsync(url: String, callback: GsCallback.a1<String>) {
        Thread {
            try {
                val result = performCall(url, GET)
                callback.callback(result)
            } catch (ignored: Exception) {
            }
        }.start()
    }
}
