/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for PathUtils shared path normalization utility
 *
 *########################################################*/
package digital.vasic.yole.network.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PathUtilsTest {

    // ==================== Basic Path Resolution ====================

    @Test
    fun normalizeSimplePath() {
        val result = PathUtils.normalizePath("/docs/file.txt", "/")
        assertEquals("/docs/file.txt", result)
    }

    @Test
    fun normalizeRootPath() {
        val result = PathUtils.normalizePath("/", "/")
        assertEquals("/", result)
    }

    @Test
    fun normalizeBlankPathReturnsRoot() {
        val result = PathUtils.normalizePath("", "/")
        assertEquals("/", result)
    }

    @Test
    fun normalizeBlankPathReturnsCustomRoot() {
        val result = PathUtils.normalizePath("", "/home/user")
        assertEquals("/home/user", result)
    }

    @Test
    fun normalizeSlashPathReturnsCustomRoot() {
        val result = PathUtils.normalizePath("/", "/data")
        assertEquals("/data", result)
    }

    @Test
    fun normalizeRelativePathUnderRoot() {
        val result = PathUtils.normalizePath("subdir/file.txt", "/root")
        assertEquals("/root/subdir/file.txt", result)
    }

    @Test
    fun normalizeAbsolutePathUnderDefaultRoot() {
        val result = PathUtils.normalizePath("/a/b/c", "/")
        assertEquals("/a/b/c", result)
    }

    // ==================== Dot Segment Resolution ====================

    @Test
    fun normalizeSkipsCurrentDirSegment() {
        val result = PathUtils.normalizePath("/a/./b/./c", "/")
        assertEquals("/a/b/c", result)
    }

    @Test
    fun normalizeResolvesParentDirSegment() {
        val result = PathUtils.normalizePath("/a/b/../c", "/")
        assertEquals("/a/c", result)
    }

    @Test
    fun normalizeResolvesMultipleParentDirSegments() {
        val result = PathUtils.normalizePath("/a/b/c/../../d", "/")
        assertEquals("/a/d", result)
    }

    @Test
    fun normalizeResolvesAllParentSegmentsToRoot() {
        val result = PathUtils.normalizePath("/a/b/../../..", "/")
        assertEquals("/", result)
    }

    @Test
    fun normalizeMixedDotAndDoubleDotSegments() {
        val result = PathUtils.normalizePath("/a/./b/../c/./d/../e", "/")
        assertEquals("/a/c/e", result)
    }

    @Test
    fun normalizeDoubleDotAtStartResolvesToRoot() {
        val result = PathUtils.normalizePath("../../file.txt", "/")
        assertEquals("/file.txt", result)
    }

    // ==================== Root Boundary Enforcement ====================

    @Test
    fun pathWithinRootIsAllowed() {
        val result = PathUtils.normalizePath("docs/readme.md", "/home/user")
        assertEquals("/home/user/docs/readme.md", result)
    }

    @Test
    fun pathTraversalBeyondRootThrowsSecurityException() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("../../etc/passwd", "/home/user")
        }
    }

    @Test
    fun pathTraversalWithDoubleDotThrowsSecurityException() {
        assertFailsWith<SecurityException> {
            PathUtils.normalizePath("../../../root/secret", "/home/user")
        }
    }

    @Test
    fun pathTraversalExactlyAtBoundaryIsAllowed() {
        // Going up to root and then back into root path
        val result = PathUtils.normalizePath("subdir/../other", "/home/user")
        assertEquals("/home/user/other", result)
    }

    @Test
    fun pathStaysWithinRootAfterResolving() {
        val result = PathUtils.normalizePath("a/b/../c", "/data/share")
        assertEquals("/data/share/a/c", result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun normalizeWhitespaceOnlyPathReturnsRoot() {
        val result = PathUtils.normalizePath("   ", "/data")
        assertEquals("/data", result)
    }

    @Test
    fun normalizeEmptyStringPathReturnsRoot() {
        val result = PathUtils.normalizePath("", "/")
        assertEquals("/", result)
    }

    @Test
    fun normalizeEmptyRootPath() {
        val result = PathUtils.normalizePath("/files/doc.txt", "")
        assertEquals("/files/doc.txt", result)
    }

    @Test
    fun normalizeBlankRootPathTreatedAsSlash() {
        val result = PathUtils.normalizePath("file.txt", "")
        assertEquals("/file.txt", result)
    }

    @Test
    fun normalizePathWithMultipleSlashes() {
        val result = PathUtils.normalizePath("/a///b//c", "/")
        assertEquals("/a/b/c", result)
    }

    @Test
    fun normalizePathWithTrailingSlash() {
        val result = PathUtils.normalizePath("/a/b/c/", "/")
        assertEquals("/a/b/c", result)
    }

    @Test
    fun normalizePathOnlyDots() {
        val result = PathUtils.normalizePath("./././.", "/")
        assertEquals("/", result)
    }

    @Test
    fun normalizeDeepPathUnderRoot() {
        val result = PathUtils.normalizePath("a/b/c/d/e/f/g", "/root")
        assertEquals("/root/a/b/c/d/e/f/g", result)
    }

    @Test
    fun normalizePathWithSpecialCharacters() {
        val result = PathUtils.normalizePath("dir with spaces/file (1).txt", "/")
        assertEquals("/dir with spaces/file (1).txt", result)
    }

    @Test
    fun normalizePathWithUnicodeCharacters() {
        val result = PathUtils.normalizePath("docs/notes.txt", "/")
        assertEquals("/docs/notes.txt", result)
    }

    // ==================== Boundary Behavior with Specific Roots ====================

    @Test
    fun rootPrefixSlashAllowsAnyAbsolutePath() {
        // When rootPath is "/", any resolved path is valid (no traversal exception)
        val result = PathUtils.normalizePath("/anything/goes/here", "/")
        assertEquals("/anything/goes/here", result)
    }

    @Test
    fun rootPrefixWithSubdirectoryEnforcesPrefix() {
        val result = PathUtils.normalizePath("file.txt", "/var/data")
        assertEquals("/var/data/file.txt", result)
    }

    @Test
    fun traversalThatLandsOnRootPrefixExactlyIsAllowed() {
        val result = PathUtils.normalizePath("subdir/..", "/home/user")
        assertEquals("/home/user", result)
    }
}
