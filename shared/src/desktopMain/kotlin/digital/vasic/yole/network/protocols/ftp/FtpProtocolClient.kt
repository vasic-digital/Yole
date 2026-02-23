/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/

package digital.vasic.yole.network.protocols.ftp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

/**
 * JVM actual implementation of [FtpProtocolClient] using [java.net.Socket]
 * for both control and data channels.
 *
 * This implementation speaks raw FTP protocol (RFC 959) over TCP sockets:
 * - Control channel: persistent connection to the FTP server command port
 * - Data channel: ephemeral connections negotiated via PASV for each transfer
 * - Supports both Unix-style and DOS-style LIST output parsing
 */
actual class FtpProtocolClient actual constructor() {

    private var controlSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    actual val isConnected: Boolean
        get() = controlSocket?.isConnected == true && controlSocket?.isClosed == false

    /**
     * Connect to an FTP server's control channel.
     * Reads the 220 greeting banner after TCP connect.
     *
     * @param host FTP server hostname or IP
     * @param port FTP server control port (typically 21)
     * @return [Result] containing the server greeting on success
     */
    actual suspend fun connect(host: String, port: Int): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val socket = Socket(host, port)
            controlSocket = socket
            reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

            // Read the 220 greeting
            val greeting = readResponse()
            if (!greeting.startsWith("220")) {
                socket.close()
                controlSocket = null
                reader = null
                writer = null
                throw Exception("FTP server rejected connection: $greeting")
            }
            greeting
        }
    }

    /**
     * Authenticate with the FTP server using USER and PASS commands.
     *
     * @param username FTP username
     * @param password FTP password
     */
    actual suspend fun login(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Send USER command
            sendCommand("USER $username")
            val userResponse = readResponse()

            // 230 = logged in without password, 331 = password required, 332 = need account
            when {
                userResponse.startsWith("230") -> return@runCatching // Already logged in
                userResponse.startsWith("331") -> {
                    // Send PASS command
                    sendCommand("PASS $password")
                    val passResponse = readResponse()
                    if (!passResponse.startsWith("230")) {
                        throw Exception("FTP login failed: $passResponse")
                    }
                }
                else -> throw Exception("FTP USER command failed: $userResponse")
            }
        }
    }

    /**
     * Disconnect from the FTP server by sending QUIT and closing the socket.
     */
    actual suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                if (isConnected) {
                    sendCommand("QUIT")
                    try {
                        readResponse() // Read 221 Goodbye
                    } catch (_: Exception) {
                        // Ignore read errors during disconnect
                    }
                }
            } finally {
                try { reader?.close() } catch (_: Exception) {}
                try { writer?.close() } catch (_: Exception) {}
                try { controlSocket?.close() } catch (_: Exception) {}
                reader = null
                writer = null
                controlSocket = null
            }
        }
    }

    /**
     * List directory contents using the LIST command over a PASV data connection.
     * Parses both Unix-style and DOS-style directory listings.
     *
     * @param path directory path to list
     * @return list of [FtpEntry] representing the directory contents
     */
    actual suspend fun list(path: String): Result<List<FtpEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()

            // Enter passive mode and open data connection
            val (dataHost, dataPort) = enterPassiveMode()
            val dataSocket = Socket(dataHost, dataPort)

            try {
                // Send LIST command
                sendCommand("LIST $path")
                val listResponse = readResponse()
                if (!listResponse.startsWith("150") && !listResponse.startsWith("125")) {
                    dataSocket.close()
                    throw Exception("FTP LIST failed: $listResponse")
                }

                // Read listing data from data connection
                val dataReader = BufferedReader(InputStreamReader(dataSocket.getInputStream(), Charsets.UTF_8))
                val lines = mutableListOf<String>()
                var line = dataReader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        lines.add(line)
                    }
                    line = dataReader.readLine()
                }
                dataReader.close()
                dataSocket.close()

                // Read the 226 Transfer complete response
                val transferResponse = readResponse()
                if (!transferResponse.startsWith("226") && !transferResponse.startsWith("250")) {
                    throw Exception("FTP LIST transfer incomplete: $transferResponse")
                }

                // Parse the listing
                lines.mapNotNull { parseFtpListLine(it) }
            } catch (e: Exception) {
                try { dataSocket.close() } catch (_: Exception) {}
                throw e
            }
        }
    }

    /**
     * Retrieve (download) a file from the FTP server using RETR over PASV data connection.
     *
     * @param remotePath path to the remote file
     * @return file contents as [ByteArray]
     */
    actual suspend fun retrieve(remotePath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()

            // Set binary transfer mode
            setTransferTypeDirect(true)

            // Enter passive mode and open data connection
            val (dataHost, dataPort) = enterPassiveMode()
            val dataSocket = Socket(dataHost, dataPort)

            try {
                // Send RETR command
                sendCommand("RETR $remotePath")
                val retrResponse = readResponse()
                if (!retrResponse.startsWith("150") && !retrResponse.startsWith("125")) {
                    dataSocket.close()
                    throw Exception("FTP RETR failed: $retrResponse")
                }

                // Read file data from data connection
                val data = dataSocket.getInputStream().readBytes()
                dataSocket.close()

                // Read the 226 Transfer complete response
                val transferResponse = readResponse()
                if (!transferResponse.startsWith("226") && !transferResponse.startsWith("250")) {
                    throw Exception("FTP RETR transfer incomplete: $transferResponse")
                }

                data
            } catch (e: Exception) {
                try { dataSocket.close() } catch (_: Exception) {}
                throw e
            }
        }
    }

    /**
     * Store (upload) a file to the FTP server using STOR over PASV data connection.
     *
     * @param remotePath path to the remote file
     * @param data file contents to upload
     */
    actual suspend fun store(remotePath: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()

            // Set binary transfer mode
            setTransferTypeDirect(true)

            // Enter passive mode and open data connection
            val (dataHost, dataPort) = enterPassiveMode()
            val dataSocket = Socket(dataHost, dataPort)

            try {
                // Send STOR command
                sendCommand("STOR $remotePath")
                val storResponse = readResponse()
                if (!storResponse.startsWith("150") && !storResponse.startsWith("125")) {
                    dataSocket.close()
                    throw Exception("FTP STOR failed: $storResponse")
                }

                // Write file data to data connection
                dataSocket.getOutputStream().write(data)
                dataSocket.getOutputStream().flush()
                dataSocket.close()

                // Read the 226 Transfer complete response
                val transferResponse = readResponse()
                if (!transferResponse.startsWith("226") && !transferResponse.startsWith("250")) {
                    throw Exception("FTP STOR transfer incomplete: $transferResponse")
                }
            } catch (e: Exception) {
                try { dataSocket.close() } catch (_: Exception) {}
                throw e
            }
        }
    }

    /**
     * Delete a file on the FTP server using the DELE command.
     *
     * @param path path to the file to delete
     */
    actual suspend fun delete(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("DELE $path")
            val response = readResponse()
            if (!response.startsWith("250")) {
                throw Exception("FTP DELE failed: $response")
            }
        }
    }

    /**
     * Create a directory on the FTP server using the MKD command.
     *
     * @param path path to the directory to create
     */
    actual suspend fun mkdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("MKD $path")
            val response = readResponse()
            if (!response.startsWith("257")) {
                throw Exception("FTP MKD failed: $response")
            }
        }
    }

    /**
     * Remove a directory on the FTP server using the RMD command.
     *
     * @param path path to the directory to remove
     */
    actual suspend fun rmdir(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("RMD $path")
            val response = readResponse()
            if (!response.startsWith("250")) {
                throw Exception("FTP RMD failed: $response")
            }
        }
    }

    /**
     * Rename a file or directory using RNFR (Rename From) followed by RNTO (Rename To).
     *
     * @param fromPath current path
     * @param toPath new path
     */
    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()

            // Step 1: RNFR - specify the file to rename
            sendCommand("RNFR $fromPath")
            val rnfrResponse = readResponse()
            if (!rnfrResponse.startsWith("350")) {
                throw Exception("FTP RNFR failed: $rnfrResponse")
            }

            // Step 2: RNTO - specify the new name
            sendCommand("RNTO $toPath")
            val rntoResponse = readResponse()
            if (!rntoResponse.startsWith("250")) {
                throw Exception("FTP RNTO failed: $rntoResponse")
            }
        }
    }

    /**
     * Print working directory using the PWD command.
     *
     * @return the current working directory path (extracted from the 257 response)
     */
    actual suspend fun pwd(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("PWD")
            val response = readResponse()
            if (!response.startsWith("257")) {
                throw Exception("FTP PWD failed: $response")
            }
            // Parse path from response like: 257 "/current/path" is the current directory
            extractQuotedPath(response)
        }
    }

    /**
     * Change working directory using the CWD command.
     *
     * @param path directory to change to
     */
    actual suspend fun cwd(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("CWD $path")
            val response = readResponse()
            if (!response.startsWith("250")) {
                throw Exception("FTP CWD failed: $response")
            }
        }
    }

    /**
     * Get file size using the SIZE command.
     *
     * @param path path to the file
     * @return file size in bytes
     */
    actual suspend fun size(path: String): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("SIZE $path")
            val response = readResponse()
            if (!response.startsWith("213")) {
                throw Exception("FTP SIZE failed: $response")
            }
            // Parse size from response like: 213 12345
            response.substringAfter("213").trim().toLong()
        }
    }

    /**
     * Get file modification time using the MDTM command.
     *
     * @param path path to the file
     * @return modification time string in YYYYMMDDHHmmss format
     */
    actual suspend fun mdtm(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            sendCommand("MDTM $path")
            val response = readResponse()
            if (!response.startsWith("213")) {
                throw Exception("FTP MDTM failed: $response")
            }
            // Parse timestamp from response like: 213 20230615120000
            response.substringAfter("213").trim()
        }
    }

    /**
     * Set the transfer type using the TYPE command.
     *
     * @param binary true for binary (TYPE I), false for ASCII (TYPE A)
     */
    actual suspend fun setTransferType(binary: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            setTransferTypeDirect(binary)
        }
    }

    /**
     * Enter passive mode using the PASV command.
     *
     * @return pair of (host, port) for the data connection
     */
    actual suspend fun pasv(): Result<Pair<String, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            requireConnected()
            enterPassiveMode()
        }
    }

    // ---- Internal helpers ----

    /**
     * Send a raw FTP command followed by CRLF.
     */
    private fun sendCommand(command: String) {
        val w = writer ?: throw Exception("FTP control channel not connected")
        w.write("$command\r\n")
        w.flush()
    }

    /**
     * Read a complete FTP response, handling multi-line responses.
     *
     * Multi-line responses have the format:
     * ```
     * 123-First line
     * Additional lines
     * 123 Last line
     * ```
     * The response is complete when a line starts with the same 3-digit code followed by a space.
     */
    private fun readResponse(): String {
        val r = reader ?: throw Exception("FTP control channel not connected")
        val sb = StringBuilder()

        val firstLine = r.readLine() ?: throw Exception("FTP connection closed by server")
        sb.appendLine(firstLine)

        // Check if this is a multi-line response
        if (firstLine.length >= 4 && firstLine[3] == '-') {
            val code = firstLine.substring(0, 3)
            // Read until we find the terminating line "NNN "
            while (true) {
                val line = r.readLine() ?: throw Exception("FTP connection closed during multi-line response")
                sb.appendLine(line)
                if (line.length >= 4 && line.startsWith(code) && line[3] == ' ') {
                    break
                }
            }
        }

        return sb.toString().trim()
    }

    /**
     * Ensure the control connection is established.
     */
    private fun requireConnected() {
        if (!isConnected) {
            throw Exception("FTP not connected")
        }
    }

    /**
     * Internal TYPE command that doesn't wrap in Result.
     */
    private fun setTransferTypeDirect(binary: Boolean) {
        val typeChar = if (binary) "I" else "A"
        sendCommand("TYPE $typeChar")
        val response = readResponse()
        if (!response.startsWith("200")) {
            throw Exception("FTP TYPE command failed: $response")
        }
    }

    /**
     * Enter passive mode by sending PASV and parsing the response.
     *
     * Response format: 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)
     * where the data connection host is h1.h2.h3.h4 and port is p1*256+p2
     */
    private fun enterPassiveMode(): Pair<String, Int> {
        sendCommand("PASV")
        val response = readResponse()
        if (!response.startsWith("227")) {
            throw Exception("FTP PASV failed: $response")
        }
        return parsePasvResponse(response)
    }

    /**
     * Parse a PASV response to extract host and port.
     *
     * Example: "227 Entering Passive Mode (192,168,1,1,200,100)"
     * -> host = "192.168.1.1", port = 200*256 + 100 = 51300
     */
    private fun parsePasvResponse(response: String): Pair<String, Int> {
        // Find the numbers inside parentheses
        val start = response.indexOf('(')
        val end = response.indexOf(')')
        if (start == -1 || end == -1 || end <= start) {
            throw Exception("Invalid PASV response format: $response")
        }

        val parts = response.substring(start + 1, end).split(',')
        if (parts.size != 6) {
            throw Exception("Invalid PASV response format: expected 6 numbers, got ${parts.size}")
        }

        val host = "${parts[0].trim()}.${parts[1].trim()}.${parts[2].trim()}.${parts[3].trim()}"
        val port = parts[4].trim().toInt() * 256 + parts[5].trim().toInt()
        return Pair(host, port)
    }

    /**
     * Extract a quoted path from an FTP response.
     *
     * Example: `257 "/home/user" is the current directory` -> `/home/user`
     */
    private fun extractQuotedPath(response: String): String {
        val firstQuote = response.indexOf('"')
        if (firstQuote == -1) {
            // Fallback: return everything after the response code
            return response.substringAfter(' ').trim()
        }
        val secondQuote = response.indexOf('"', firstQuote + 1)
        if (secondQuote == -1) {
            return response.substring(firstQuote + 1)
        }
        // Handle escaped quotes ("" -> ") inside path
        return response.substring(firstQuote + 1, secondQuote).replace("\"\"", "\"")
    }

    /**
     * Parse a single line from an FTP LIST response.
     *
     * Supports two formats:
     *
     * **Unix-style:**
     * ```
     * drwxr-xr-x   2 user  group  4096 Jan 15 12:30 dirname
     * -rw-r--r--   1 user  group  1234 Feb 20 09:15 filename.txt
     * ```
     *
     * **DOS-style:**
     * ```
     * 01-15-23  12:30PM       <DIR>          dirname
     * 02-20-23  09:15AM              1234 filename.txt
     * ```
     */
    private fun parseFtpListLine(line: String): FtpEntry? {
        if (line.isBlank()) return null

        // Try Unix-style parsing first
        val unixEntry = parseUnixListLine(line)
        if (unixEntry != null) return unixEntry

        // Try DOS-style parsing
        val dosEntry = parseDosListLine(line)
        if (dosEntry != null) return dosEntry

        return null
    }

    /**
     * Parse a Unix-style FTP LIST line.
     *
     * Format: `permissions links owner group size month day time/year name`
     */
    private fun parseUnixListLine(line: String): FtpEntry? {
        // Unix listing starts with permission string like drwxr-xr-x or -rw-r--r--
        if (line.length < 10) return null
        val firstChar = line[0]
        if (firstChar != 'd' && firstChar != '-' && firstChar != 'l') return null

        // Validate that first 10 chars look like permissions
        val permStr = line.substring(0, 10)
        if (!permStr.matches(Regex("[dl\\-][rwxsStT\\-]{9}"))) return null

        val isDirectory = firstChar == 'd'

        // Split by whitespace, handling multiple spaces between fields
        val parts = line.trim().split(Regex("\\s+"), limit = 9)
        if (parts.size < 9) return null

        val sizeStr = parts[4]
        val size = sizeStr.toLongOrNull() ?: 0L

        // Name is the last field (everything after the 8th space-separated field)
        val name = parts[8]

        // Skip . and .. entries
        if (name == "." || name == "..") return null

        // For symlinks, strip the " -> target" portion
        val displayName = if (firstChar == 'l' && name.contains(" -> ")) {
            name.substringBefore(" -> ")
        } else {
            name
        }

        // Parse modification time from month/day/time-or-year fields
        val lastModified = parseUnixDateTime(parts[5], parts[6], parts[7])

        return FtpEntry(
            name = displayName,
            size = size,
            isDirectory = isDirectory,
            lastModified = lastModified,
            permissions = permStr
        )
    }

    /**
     * Parse a DOS-style FTP LIST line.
     *
     * Format: `MM-DD-YY  HH:MMAM/PM  <DIR>|size  name`
     */
    private fun parseDosListLine(line: String): FtpEntry? {
        // DOS listing typically starts with a date like 01-15-23
        val dosPattern = Regex("""^(\d{2}-\d{2}-\d{2,4})\s+(\d{1,2}:\d{2}[AP]M)\s+(<DIR>|\d+)\s+(.+)$""")
        val match = dosPattern.matchEntire(line.trim()) ?: return null

        val dateStr = match.groupValues[1]
        val timeStr = match.groupValues[2]
        val sizeOrDir = match.groupValues[3]
        val name = match.groupValues[4]

        if (name == "." || name == "..") return null

        val isDirectory = sizeOrDir == "<DIR>"
        val size = if (isDirectory) 0L else sizeOrDir.toLongOrNull() ?: 0L

        val lastModified = parseDosDateTime(dateStr, timeStr)

        return FtpEntry(
            name = name,
            size = size,
            isDirectory = isDirectory,
            lastModified = lastModified,
            permissions = null
        )
    }

    /**
     * Parse a Unix-style date/time from LIST output.
     *
     * Month is abbreviated (Jan, Feb, ...), day is numeric, and the third field
     * is either a time (HH:MM) for recent files or a year (YYYY) for older files.
     */
    private fun parseUnixDateTime(monthStr: String, dayStr: String, timeOrYear: String): Instant? {
        return try {
            val monthMap = mapOf(
                "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
                "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
                "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12
            )
            val month = monthMap[monthStr] ?: return null
            val day = dayStr.toIntOrNull() ?: return null

            if (timeOrYear.contains(':')) {
                // Recent file: HH:MM format, assume current year
                val timeParts = timeOrYear.split(':')
                val hour = timeParts[0].toIntOrNull() ?: 0
                val minute = timeParts[1].toIntOrNull() ?: 0
                // Use a reasonable default year since we don't have one
                val currentYear = kotlinx.datetime.Clock.System.now()
                    .let { kotlinx.datetime.TimeZone.UTC.let { tz -> it.toLocalDateTime(tz).year } }
                LocalDateTime(currentYear, month, day, hour, minute)
                    .toInstant(TimeZone.UTC)
            } else {
                // Older file: year format
                val year = timeOrYear.toIntOrNull() ?: return null
                LocalDateTime(year, month, day, 0, 0)
                    .toInstant(TimeZone.UTC)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse a DOS-style date/time from LIST output.
     *
     * Date format: MM-DD-YY or MM-DD-YYYY
     * Time format: HH:MMAM or HH:MMPM
     */
    private fun parseDosDateTime(dateStr: String, timeStr: String): Instant? {
        return try {
            val dateParts = dateStr.split('-')
            if (dateParts.size != 3) return null

            val month = dateParts[0].toIntOrNull() ?: return null
            val day = dateParts[1].toIntOrNull() ?: return null
            var year = dateParts[2].toIntOrNull() ?: return null
            if (year < 100) year += 2000

            // Parse time: "12:30PM" or "09:15AM"
            val isPM = timeStr.endsWith("PM", ignoreCase = true)
            val timePart = timeStr.dropLast(2)
            val timeParts = timePart.split(':')
            if (timeParts.size != 2) return null

            var hour = timeParts[0].toIntOrNull() ?: return null
            val minute = timeParts[1].toIntOrNull() ?: return null

            if (isPM && hour != 12) hour += 12
            if (!isPM && hour == 12) hour = 0

            LocalDateTime(year, month, day, hour, minute)
                .toInstant(TimeZone.UTC)
        } catch (_: Exception) {
            null
        }
    }
}
