/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Deep fuzz-style path traversal security tests.
 * Validates that PathUtils.normalizePath() correctly
 * handles null bytes, Unicode escapes, double encoding,
 * mixed separators, long paths, control characters,
 * and other adversarial path inputs.
 *
 *########################################################*/
package digital.vasic.yole.security

import digital.vasic.yole.network.common.PathUtils
import kotlin.test.*

/**
 * Deep path traversal security tests exercising
 * PathUtils.normalizePath() with adversarial inputs.
 *
 * Categories:
 * - Null byte injection
 * - URL-encoded traversal (%2e%2e)
 * - Double URL-encoding (%252e%252e)
 * - Mixed forward/backslash separators
 * - Extremely long paths (10 000+ characters)
 * - Repeated deep traversal
 * - Hidden directory segments
 * - Empty path segments
 * - Trailing dots
 * - Space injection
 * - Paths composed only of dots
 * - Control character injection
 *
 * Total: 30+ tests
 */
class PathTraversalDeepTests {

    // ==================== Null Byte Injection ====================

    @Test
    fun nullByteInMiddleOfPath() {
        val path = "subdir/file\u0000.txt"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun nullByteBeforeTraversalSegment() {
        val path = "subdir/\u0000../secret"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun nullByteAfterTraversalSegment() {
        val path = "subdir/..\u0000/secret"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun multipleNullBytesInPath() {
        val path = "\u0000sub\u0000dir\u0000/\u0000file\u0000.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun pathIsOnlyNullByte() {
        val path = "\u0000"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    // ==================== URL-Encoded Traversal ====================

    @Test
    fun urlEncodedDotDotLiteralString() {
        // These are literal characters "%2e%2e%2f" (not decoded).
        // normalizePath works on raw strings, so these should be treated as
        // a directory name, not as traversal.
        val path = "%2e%2e%2f%2e%2e%2fetc%2fpasswd"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"),
            "URL-encoded dot-dot treated as literal segment must remain under root")
    }

    @Test
    fun urlEncodedDotDotSlashCombinations() {
        val paths = listOf(
            "%2e%2e/secret",
            "%2e%2e/%2e%2e/secret",
            "dir/%2e%2e/%2e%2e/secret"
        )
        paths.forEach { path ->
            val result = PathUtils.normalizePath(path, "/home/user")
            assertNotNull(result, "Should not crash on URL-encoded path: $path")
            assertTrue(result.startsWith("/home/user"),
                "URL-encoded traversal literal must stay under root: $path")
        }
    }

    // ==================== Double URL-Encoding ====================

    @Test
    fun doubleEncodedDotDot() {
        // %252e decodes to %2e (single decode), which is literal text
        val path = "%252e%252e/%252e%252e/etc/passwd"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"),
            "Double-encoded traversal should not escape root")
    }

    @Test
    fun tripleEncodedDotDot() {
        val path = "%25252e%25252e/%25252e%25252e/secret"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    // ==================== Mixed Separators ====================

    @Test
    fun backslashSeparators() {
        // Backslashes are not treated as path separators by normalizePath,
        // so "..\\.." is a single literal segment
        val path = "..\\..\\etc\\passwd"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"),
            "Backslash traversal should not escape root")
    }

    @Test
    fun mixedForwardAndBackslash() {
        val paths = listOf(
            "sub\\dir/../secret",
            "sub/dir\\..\\secret",
            "..\\../etc/passwd",
            "../..\\etc\\passwd"
        )
        paths.forEach { path ->
            try {
                val result = PathUtils.normalizePath(path, "/home/user")
                // If it succeeds, the result must stay under root
                assertNotNull(result, "Should not crash on mixed separators: $path")
                assertTrue(result.startsWith("/home/user"),
                    "Mixed-separator path must stay under root: $path -> $result")
            } catch (e: IllegalArgumentException) {
                // Path traversal correctly detected and rejected
                assertTrue(true, "Path traversal correctly blocked for: $path")
            }
        }
    }

    @Test
    fun backslashOnlyPath() {
        val path = "dir1\\dir2\\file.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    // ==================== Long Paths ====================

    @Test
    fun veryLongPathSegment() {
        val longSegment = "a".repeat(10_000)
        val path = "$longSegment/file.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
        assertTrue(result.contains(longSegment))
    }

    @Test
    fun veryLongPathWithManySegments() {
        val path = ("dir/" .repeat(5_000)) + "file.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
        assertTrue(result.startsWith("/"))
    }

    @Test
    fun tenThousandCharacterPath() {
        val path = "x".repeat(10_001) + ".txt"
        val result = PathUtils.normalizePath(path, "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun longPathWithTraversalAttempts() {
        val path = ("sub/" .repeat(100)) + ("../" .repeat(200)) + "etc/passwd"
        // After resolving: 100 sub-dirs pushed, then 200 ".." pops (100 pops back + further pops but list is empty)
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
        assertEquals("/etc/passwd", result)
    }

    // ==================== Repeated Deep Traversal ====================

    @Test
    fun deepTraversalEightLevels() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("../../../../../../../../etc/passwd", "/home/user")
        }
    }

    @Test
    fun deepTraversalTwentyLevels() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("../".repeat(20) + "etc/passwd", "/home/user")
        }
    }

    @Test
    fun deepTraversalHundredLevels() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("../".repeat(100) + "etc/passwd", "/home/user")
        }
    }

    // ==================== Hidden Directory Segments ====================

    @Test
    fun hiddenDirectoryAtStart() {
        val result = PathUtils.normalizePath(".hidden/file.txt", "/home/user")
        assertNotNull(result)
        // ".hidden" is a real directory name (not "."), should be preserved
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun hiddenDirectoryInMiddle() {
        val result = PathUtils.normalizePath("dir/.hidden/file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.contains(".hidden"))
    }

    @Test
    fun doubleHiddenDirectory() {
        val result = PathUtils.normalizePath(".hidden/.secret/file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    // ==================== Empty Segments ====================

    @Test
    fun multipleConsecutiveSlashes() {
        val result = PathUtils.normalizePath("path//with///empty////slashes", "/")
        assertNotNull(result)
        assertEquals("/path/with/empty/slashes", result)
    }

    @Test
    fun leadingMultipleSlashes() {
        val result = PathUtils.normalizePath("///leading/slashes", "/")
        assertNotNull(result)
        assertEquals("/leading/slashes", result)
    }

    @Test
    fun trailingMultipleSlashes() {
        val result = PathUtils.normalizePath("trailing/slashes///", "/")
        assertNotNull(result)
        assertFalse(result.endsWith("///"))
    }

    @Test
    fun onlySlashes() {
        val result = PathUtils.normalizePath("////", "/")
        assertNotNull(result)
    }

    // ==================== Trailing Dots ====================

    @Test
    fun fileNameWithTrailingDots() {
        val result = PathUtils.normalizePath("path/file...", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun directoryWithTrailingDots() {
        val result = PathUtils.normalizePath("dir.../file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun tripleDotSegment() {
        // "..." is a valid directory name (not "." or "..")
        val result = PathUtils.normalizePath(".../file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
        assertTrue(result.contains("..."))
    }

    // ==================== Space Injection ====================

    @Test
    fun spaceInPathSegment() {
        val result = PathUtils.normalizePath("path/ /traversal", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun dotDotWithSpaces() {
        // ".. " and " .." are not ".." and should be treated as regular segments
        val result = PathUtils.normalizePath("sub/.. /secret", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun tabCharacterInPath() {
        val result = PathUtils.normalizePath("path/\tdir/file.txt", "/")
        assertNotNull(result)
    }

    // ==================== Paths with Only Dots ====================

    @Test
    fun pathOfSingleDot() {
        val result = PathUtils.normalizePath(".", "/home/user")
        assertNotNull(result)
        assertEquals("/home/user", result)
    }

    @Test
    fun pathOfDoubleDot() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("..", "/home/user")
        }
    }

    @Test
    fun pathOfDotSlashDot() {
        val result = PathUtils.normalizePath("./.", "/home/user")
        assertNotNull(result)
        assertEquals("/home/user", result)
    }

    @Test
    fun pathOfManyDotsAndDotDots() {
        // ./../../.. should reduce and escape
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("./../../..", "/home/user")
        }
    }

    // ==================== Control Characters ====================

    @Test
    fun bellCharacterInPath() {
        val path = "path/\u0007file.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun escapeCharacterInPath() {
        val path = "path/\u001Bfile.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun deleteCharacterInPath() {
        val path = "path/\u007Ffile.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun carriageReturnInPath() {
        val path = "path/\rfile.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun newlineInPath() {
        val path = "path/\nfile.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    @Test
    fun mixedControlCharacters() {
        val path = "path/\u0000\u0007\u001B\u007F/file.txt"
        val result = PathUtils.normalizePath(path, "/")
        assertNotNull(result)
    }

    // ==================== Boundary Conditions ====================

    @Test
    fun emptyStringPath() {
        val result = PathUtils.normalizePath("", "/home/user")
        assertNotNull(result)
    }

    @Test
    fun blankStringPath() {
        val result = PathUtils.normalizePath("   ", "/home/user")
        assertNotNull(result)
    }

    @Test
    fun rootSlashOnly() {
        val result = PathUtils.normalizePath("/", "/home/user")
        assertNotNull(result)
    }

    @Test
    fun rootSlashWithEmptyRoot() {
        val result = PathUtils.normalizePath("file.txt", "")
        assertNotNull(result)
    }

    @Test
    fun traversalExactlyToRootBoundary() {
        // /home/user + sub/.. should resolve to /home/user
        val result = PathUtils.normalizePath("sub/..", "/home/user")
        assertEquals("/home/user", result)
    }

    @Test
    fun traversalOneStepBeyondRootBoundary() {
        assertFailsWith<IllegalArgumentException> {
            PathUtils.normalizePath("..", "/home/user")
        }
    }

    @Test
    fun unicodeDirectoryNames() {
        val result = PathUtils.normalizePath("dir/\u00E9\u00E0\u00FC/file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun cjkCharactersInPath() {
        val result = PathUtils.normalizePath("dir/\u4E2D\u6587/file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }

    @Test
    fun emojiInPath() {
        val result = PathUtils.normalizePath("dir/\uD83D\uDE00/file.txt", "/home/user")
        assertNotNull(result)
        assertTrue(result.startsWith("/home/user"))
    }
}
