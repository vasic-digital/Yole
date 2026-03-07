/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS SMB client stub -- SMB is not supported on iOS.
 *
 *########################################################*/
package digital.vasic.yole.network.protocols.smb

/**
 * iOS implementation of [SmbProtocolClient].
 *
 * SMB/CIFS is not currently supported on iOS because the smbj library
 * is JVM-only. All methods return [UnsupportedOperationException] failures.
 *
 * TODO: Implement using libsmb2 via Kotlin/Native cinterop for SMB2/3 support.
 *       Steps:
 *       1. Create a .def file for libsmb2 C headers in the cinterop/ directory
 *       2. Implement SMB2 session setup (connect, authenticate via NTLM/Kerberos)
 *       3. Implement share tree connect and file operations (list, read, write, delete)
 *       4. Support SMB3 encryption for secure communication
 *       5. Handle reconnection on network changes (iOS can switch networks frequently)
 *
 * TODO: Alternatively, use iOS's built-in NetFS framework (if available) or
 *       SMBClient framework for macOS Catalyst builds.
 */
actual class SmbProtocolClient actual constructor() {

    actual val isConnected: Boolean
        get() = false

    actual suspend fun connect(host: String, port: Int, shareName: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun authenticate(domain: String, username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun disconnect(): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun list(path: String): Result<List<SmbEntry>> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun read(path: String): Result<ByteArray> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun write(path: String, data: ByteArray): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun delete(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun mkdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun rmdir(path: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun rename(fromPath: String, toPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun stat(path: String): Result<SmbFileInfo> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))

    actual suspend fun getShareInfo(): Result<SmbShareInfo> =
        Result.failure(UnsupportedOperationException("SMB not supported on iOS"))
}
