/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform-specific exception for unsupported protocol operations.
 *
 *########################################################*/
package digital.vasic.yole.network.common

/**
 * Exception thrown when a network protocol operation is not supported
 * on the current platform.
 *
 * This is a structured replacement for generic [UnsupportedOperationException]
 * in platform stubs, providing richer context about why an operation is
 * unavailable and what alternatives may exist.
 *
 * @property protocol The protocol name (e.g., "FTP", "SFTP", "SMB")
 * @property platform The platform name (e.g., "iOS", "Wasm/Web")
 * @property reason Human-readable explanation of why the protocol is unsupported
 * @property alternativeApproach Optional suggestion for an alternative approach
 */
class PlatformNotSupportedException(
    val protocol: String,
    val platform: String,
    val reason: String,
    val alternativeApproach: String? = null,
    cause: Throwable? = null
) : UnsupportedOperationException(
    buildMessage(protocol, platform, reason, alternativeApproach),
    cause
) {
    companion object {
        private fun buildMessage(
            protocol: String,
            platform: String,
            reason: String,
            alternativeApproach: String?
        ): String {
            val base = "$protocol is not supported on $platform: $reason"
            return if (alternativeApproach != null) {
                "$base. Alternative: $alternativeApproach"
            } else {
                base
            }
        }

        /**
         * Create an exception for iOS protocol stubs.
         */
        fun ios(protocol: String, reason: String, alternativeApproach: String? = null) =
            PlatformNotSupportedException(
                protocol = protocol,
                platform = "iOS",
                reason = reason,
                alternativeApproach = alternativeApproach
            )

        /**
         * Create an exception for Wasm/Web protocol stubs.
         */
        fun wasm(protocol: String, reason: String, alternativeApproach: String? = null) =
            PlatformNotSupportedException(
                protocol = protocol,
                platform = "Wasm/Web",
                reason = reason,
                alternativeApproach = alternativeApproach
            )
    }
}
