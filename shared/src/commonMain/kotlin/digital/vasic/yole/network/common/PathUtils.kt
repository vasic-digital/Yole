/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Shared path normalization utility for protocol services
 *
 *########################################################*/
package digital.vasic.yole.network.common

/**
 * Shared path normalization and traversal protection for all protocol services.
 *
 * Provides a single, consistent implementation of path normalization that resolves
 * `.` and `..` segments and enforces root boundary to prevent path traversal attacks.
 *
 * All protocol services (FTP, SFTP, SMB, Dropbox, etc.) should delegate their
 * internal `normalizePath()` to [PathUtils.normalizePath] for consistency.
 */
object PathUtils {

    /**
     * Normalize a file path relative to a root path.
     *
     * Resolves `.` (current directory) and `..` (parent directory) segments,
     * ensures a leading slash, and enforces that the resulting path stays
     * within the [rootPath] boundary.
     *
     * @param path The path to normalize (may be relative or absolute)
     * @param rootPath The root path that constrains the result
     * @return The normalized absolute path
     * @throws SecurityException if the resolved path escapes [rootPath]
     */
    fun normalizePath(path: String, rootPath: String): String {
        if (path.isBlank() || path == "/") return rootPath.ifBlank { "/" }

        val basePath = if (rootPath.isBlank() || rootPath == "/") path else "$rootPath/$path"
        val segments = basePath.split("/").filter { it.isNotEmpty() }
        val resolved = mutableListOf<String>()
        for (segment in segments) {
            when (segment) {
                "." -> { /* skip */ }
                ".." -> { if (resolved.isNotEmpty()) resolved.removeLast() }
                else -> resolved.add(segment)
            }
        }
        val result = "/" + resolved.joinToString("/")

        val rootPrefix = rootPath.ifBlank { "/" }
        if (rootPrefix != "/" && !result.startsWith(rootPrefix)) {
            throw SecurityException("Path traversal detected: '$path' escapes root '$rootPath'")
        }
        return result
    }
}
