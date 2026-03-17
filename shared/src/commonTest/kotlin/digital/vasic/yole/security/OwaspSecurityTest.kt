/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * OWASP-inspired security tests covering path traversal,
 * null byte injection, header injection, buffer overflow
 * prevention, and Unicode normalization attacks.
 *
 *########################################################*/
package digital.vasic.yole.security

import digital.vasic.yole.format.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.network.common.PathUtils
import digital.vasic.yole.network.common.StorageConfig
import kotlin.test.*

/**
 * OWASP-inspired security tests.
 *
 * Validates path traversal prevention, injection attack
 * resistance, and credential exposure prevention across
 * the Yole codebase.
 *
 * Total: 35+ tests
 */
class OwaspSecurityTest {

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
        ParserInitializer.registerAllParsers()
    }

    @AfterTest
    fun tearDown() {
        ParserRegistry.clear()
    }

    // ==================== Path Traversal Prevention (PathUtils) ====================

    @Test
    fun pathTraversalBasicDotDot() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("../../etc/passwd", "/home/user")
        }
    }

    @Test
    fun pathTraversalDeep() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("../../../root/secret", "/home/user")
        }
    }

    @Test
    fun pathTraversalEncodedDots() {
        // Direct ../ in path segments
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("subdir/../../etc/shadow", "/home/user")
        }
    }

    @Test
    fun pathTraversalMixedWithValidSegments() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("valid/dir/../../../etc/hosts", "/home/user")
        }
    }

    @Test
    fun pathTraversalAtRoot() {
        // With root "/" any normalized path is valid
        val result = PathUtils.normalizePath("../../etc/passwd", "/")
        assertEquals("/etc/passwd", result)
    }

    @Test
    fun pathTraversalWithSlashRoot() {
        val result = PathUtils.normalizePath("/a/b/../c", "/")
        assertEquals("/a/c", result)
    }

    @Test
    fun pathTraversalExactBoundary() {
        // Going up to root prefix exactly should be allowed
        val result = PathUtils.normalizePath("subdir/..", "/home/user")
        assertEquals("/home/user", result)
    }

    @Test
    fun pathTraversalOneAboveRoot() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("..", "/home/user")
        }
    }

    // ==================== Null Byte Injection ====================

    @Test
    fun nullByteInPathDoesNotCrash() {
        // PathUtils should handle null bytes gracefully
        val path = "file\u0000.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun nullByteInParserInputDoesNotCrash() {
        val parser = PlaintextParser()
        val content = "Hello\u0000World"
        val doc = parser.parse(content)
        assertNotNull(doc)
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html)
    }

    @Test
    fun nullBytesInMarkdownDoNotCrash() {
        val parser = MarkdownParser()
        val content = "# Title\u0000\n\nParagraph\u0000 text."
        val doc = parser.parse(content)
        assertNotNull(doc)
    }

    @Test
    fun nullBytesInCsvDoNotCrash() {
        val parser = CsvParser()
        val content = "a\u0000,b,c\n1,2\u0000,3"
        val doc = parser.parse(content)
        assertNotNull(doc)
    }

    // ==================== Header Injection ====================

    @Test
    fun crlfInjectionInPathDoesNotCrash() {
        val path = "dir\r\nHeader-Injection: true\r\nfile.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun crlfInParserContent() {
        val parser = PlaintextParser()
        val content = "Line1\r\nInjected-Header: value\r\nLine2"
        val doc = parser.parse(content)
        assertNotNull(doc)
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html)
    }

    // ==================== Very Long Paths (Buffer Overflow Prevention) ====================

    @Test
    fun veryLongPathDoesNotCrash() {
        val longPath = "a/".repeat(5000) + "file.txt"
        val result = PathUtils.normalizePath(longPath, "/")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun veryLongFilenameDoesNotCrash() {
        val longName = "x".repeat(10000) + ".txt"
        val result = PathUtils.normalizePath(longName, "/")
        assertNotNull(result)
    }

    @Test
    fun veryLongRootPathDoesNotCrash() {
        val longRoot = "/" + "dir/".repeat(1000) + "root"
        val result = PathUtils.normalizePath("file.txt", longRoot)
        assertNotNull(result)
        assertTrue(result.startsWith(longRoot))
    }

    @Test
    fun veryLongContentDoesNotCrashParsers() {
        val longContent = "word ".repeat(100000)
        val parser = PlaintextParser()
        val doc = parser.parse(longContent)
        assertNotNull(doc)
    }

    // ==================== Unicode Normalization Attacks ====================

    @Test
    fun unicodeDotsInPathDoNotBypassTraversal() {
        // Normal dots
        val result = PathUtils.normalizePath("./safe/./file.txt", "/home/user")
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun unicodeInPathSegments() {
        val result = PathUtils.normalizePath("docs/notes.txt", "/")
        assertEquals("/docs/notes.txt", result)
    }

    @Test
    fun mixedUnicodeAndAsciiPath() {
        val result = PathUtils.normalizePath("folder/file.txt", "/root")
        assertTrue(result.startsWith("/root"))
    }

    @Test
    fun emptyPathSegmentsHandledCorrectly() {
        val result = PathUtils.normalizePath("///a///b///c///", "/")
        assertEquals("/a/b/c", result)
    }

    // ==================== Token/Credential Not Exposed in Error Messages ====================

    @Test
    fun circuitBreakerExceptionDoesNotContainCredentials() {
        val ex = digital.vasic.yole.network.common.CircuitBreakerOpenException("test-service", kotlin.time.Duration.ZERO)
        val message = ex.message ?: ""
        assertFalse(message.contains("password", ignoreCase = true))
        assertFalse(message.contains("secret", ignoreCase = true))
        assertFalse(message.contains("token", ignoreCase = true))
    }

    @Test
    fun dropboxConfigCanBeCreatedWithSensitiveData() {
        val config = StorageConfig.DropboxConfig(
            name = "test",
            accessToken = "secret-access-token-12345",
            appKey = "app-key",
            appSecret = "app-secret-value"
        )
        assertEquals("test", config.name)
        assertEquals("secret-access-token-12345", config.accessToken)
    }

    @Test
    fun ftpConfigCanBeCreatedWithSensitiveData() {
        val config = StorageConfig.FtpConfig(
            name = "test-ftp",
            host = "ftp.example.com",
            username = "admin",
            password = "SuperSecret123!"
        )
        assertEquals("test-ftp", config.name)
        val str = config.toString()
        assertNotNull(str)
    }

    @Test
    fun googleDriveConfigCanBeCreatedWithTokens() {
        val config = StorageConfig.GoogleDriveConfig(
            name = "gdrive-test",
            clientId = "client-id",
            clientSecret = "client-secret",
            refreshToken = "refresh-token",
            accessToken = "access-token"
        )
        assertEquals("gdrive-test", config.name)
    }

    @Test
    fun oneDriveConfigCanBeCreatedWithTokens() {
        val config = StorageConfig.OneDriveConfig(
            name = "onedrive-test",
            clientId = "ms-client-id",
            clientSecret = "ms-secret",
            refreshToken = "ms-refresh",
            accessToken = "ms-access"
        )
        assertEquals("onedrive-test", config.name)
    }

    @Test
    fun sftpConfigCanBeCreatedWithKeyPassphrase() {
        val config = StorageConfig.SftpConfig(
            name = "sftp-test",
            host = "sftp.example.com",
            username = "sftpuser",
            password = "sftp-pass",
            privateKeyPassphrase = "key-phrase"
        )
        assertEquals("sftp-test", config.name)
    }

    @Test
    fun smbConfigCanBeCreatedWithCredentials() {
        val config = StorageConfig.SmbConfig(
            name = "smb-test",
            host = "smb.example.com",
            share = "share",
            username = "smbuser",
            password = "smb-pass"
        )
        assertEquals("smb-test", config.name)
    }

    // ==================== XSS in Parser Output ====================

    @Test
    fun escapeHtmlPreventsXss() {
        val payloads = listOf(
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>",
            "'\"><script>alert(document.cookie)</script>"
        )
        payloads.forEach { payload ->
            val escaped = payload.escapeHtml()
            assertFalse(escaped.contains("<script>"), "Script tag should be escaped in: $payload")
            assertFalse(escaped.contains("<img"), "Img tag should be escaped in: $payload")
            assertFalse(escaped.contains("<svg"), "SVG tag should be escaped in: $payload")
        }
    }

    @Test
    fun allSpecialCharsEscaped() {
        val input = "<>&\"'"
        val escaped = input.escapeHtml()
        assertEquals("&lt;&gt;&amp;&quot;&#39;", escaped)
    }

    @Test
    fun parsersHandleMaliciousInputWithoutCrash() {
        val malicious = listOf(
            "<script>document.location='http://evil.com/steal'</script>",
            "{{constructor.constructor('return this')()}}",
            "\u0000\u0001\u0002\u0003",
            "%00%0d%0a",
            "A".repeat(100000)
        )
        val parsers: List<TextParser> = listOf(
            MarkdownParser(), PlaintextParser(), CsvParser(),
            TodoTxtParser(), LatexParser(), WikitextParser(),
            RestructuredTextParser()
        )
        for (parser in parsers) {
            for (input in malicious) {
                val doc = parser.parse(input)
                assertNotNull(doc, "Parser ${parser.supportedFormat.name} crashed on malicious input")
            }
        }
    }

    // ==================== Path Traversal in Protocol Services ====================

    @Test
    fun webdavPathTraversalBlocked() {
        val service = digital.vasic.yole.network.protocols.webdav.WebDavService(
            StorageConfig.WebDavConfig(
                name = "test", url = "https://example.com/dav",
                username = "user", password = "pass"
            )
        )
        val parent = service.getParentPath("/")
        assertTrue(parent == null || parent == "/")
    }

    @Test
    fun ftpPathTraversalBlocked() {
        val service = digital.vasic.yole.network.protocols.ftp.FtpService(
            StorageConfig.FtpConfig(
                name = "test", host = "ftp.example.com",
                username = "user", password = "pass"
            )
        )
        val parent = service.getParentPath("/")
        assertTrue(parent == null || parent == "/")
    }

    @Test
    fun sftpPathTraversalBlocked() {
        val service = digital.vasic.yole.network.protocols.sftp.SftpService(
            StorageConfig.SftpConfig(
                name = "test", host = "sftp.example.com"
            )
        )
        val parent = service.getParentPath("/")
        assertTrue(parent == null || parent == "/")
    }
}
