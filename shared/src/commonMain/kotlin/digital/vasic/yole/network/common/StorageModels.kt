/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Storage Information
 * Represents a network storage location
 *
 *########################################################*/
package digital.vasic.yole.network.common

/**
 * Represents storage information for network storage locations.
 */
data class StorageInfo(
    /** Unique identifier for this storage location. */
    val id: String,
    /** Display name of the storage location. */
    val name: String,
    /** Type of the storage backend (e.g., cloud provider or protocol). */
    val storageType: StorageType,
    /** Base URL of the storage endpoint, if applicable. */
    val baseUrl: String? = null,
    /** Whether the storage location is currently connected. */
    val isConnected: Boolean = false,
    /** ISO timestamp of the last successful synchronization, if any. */
    val lastSync: String? = null
)

/**
 * Quota information for storage locations
 */
data class QuotaInfo(
    /** Number of bytes currently consumed by stored content. */
    val usedBytes: Long = 0L,
    /** Total capacity of the storage location in bytes. */
    val totalBytes: Long = 0L,
    /** Number of bytes still available for new content. */
    val availableBytes: Long = 0L,
    /** Percentage of total storage capacity currently in use. */
    val usedPercentage: Double = 0.0
)

/**
 * File information for network storage
 */
data class FileInfo(
    /** File or directory name without the path prefix. */
    val name: String,
    /** Full path of the file within the storage location. */
    val path: String,
    /** Size of the file in bytes. */
    val size: Long,
    /** Whether this entry represents a directory rather than a file. */
    val isDirectory: Boolean = false,
    /** ISO timestamp of the last modification, if available. */
    val lastModified: String? = null
)
