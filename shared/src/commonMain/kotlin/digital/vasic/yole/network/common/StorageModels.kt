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
    val id: String,
    val name: String,
    val storageType: StorageType,
    val baseUrl: String? = null,
    val isConnected: Boolean = false,
    val lastSync: String? = null
)

/**
 * Quota information for storage locations
 */
data class QuotaInfo(
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val usedPercentage: Double = 0.0
)

/**
 * File information for network storage
 */
data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,
    val isDirectory: Boolean = false,
    val lastModified: String? = null
)
