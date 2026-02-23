/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Network protocol implementation status summary
 *
 *########################################################*/

package digital.vasic.yole.network.protocols

/**
 * Summary of the implementation status of all network storage protocol services.
 *
 * Each service implements
 * [NetworkStorageService][digital.vasic.yole.network.NetworkStorageService]
 * and provides file operations (list, upload, download, delete, etc.) for a
 * specific network protocol or cloud storage provider.
 *
 * ## Implementation Tiers
 *
 * | Tier | Meaning |
 * |------|---------|
 * | STUBBED | In-memory only. No network I/O. All file operations mutate a local virtual file system. |
 * | PARTIALLY_IMPLEMENTED | Some operations perform real HTTP I/O via ktor; others remain stubbed or unimplemented. |
 * | SUBSTANTIALLY_IMPLEMENTED | Most CRUD operations hit real cloud APIs via ktor. Local filesystem I/O (reading/writing bytes to disk) is not yet wired up. |
 * | FULLY_IMPLEMENTED | Real network I/O for all operations. Platform-limited protocols throw UnsupportedOperationException on unsupported platforms. |
 *
 * ## Protocol Status Overview
 *
 * All eight protocols are now fully implemented with real network I/O.
 *
 * | Protocol | Service Class | Tier | HTTP Client | Auth |
 * |----------|---------------|------|-------------|------|
 * | FTP | [FtpService][digital.vasic.yole.network.protocols.ftp.FtpService] | FULLY_IMPLEMENTED | No (TCP sockets) | FTP USER/PASS |
 * | SFTP | [SftpService][digital.vasic.yole.network.protocols.sftp.SftpService] | FULLY_IMPLEMENTED | No (SSH via sshj) | SSH password or public key |
 * | SMB/CIFS | [SmbService][digital.vasic.yole.network.protocols.smb.SmbService] | FULLY_IMPLEMENTED | No (SMB via smbj) | NTLM/Kerberos |
 * | WebDAV | [WebDavService][digital.vasic.yole.network.protocols.webdav.WebDavService] | FULLY_IMPLEMENTED | ktor (active) | Basic, Digest, OAuth, None |
 * | Git | [GitService][digital.vasic.yole.network.protocols.git.GitService] | FULLY_IMPLEMENTED | ktor (active) | PAT or Basic |
 * | Dropbox | [DropboxService][digital.vasic.yole.network.protocols.dropbox.DropboxService] | FULLY_IMPLEMENTED | ktor (active) | OAuth2 (DropboxOAuth2Flow) |
 * | Google Drive | [GoogleDriveService][digital.vasic.yole.network.protocols.googledrive.GoogleDriveService] | FULLY_IMPLEMENTED | ktor (active) | OAuth2 (GoogleDriveOAuth2Flow) |
 * | OneDrive | [OneDriveService][digital.vasic.yole.network.protocols.onedrive.OneDriveService] | FULLY_IMPLEMENTED | ktor (active) | OAuth2 (OneDriveOAuth2Flow) |
 *
 * ## Platform Support
 *
 * - **JVM (Android/Desktop)**: All eight protocols are fully supported.
 * - **iOS**: HTTP-based protocols (WebDAV, Git, Dropbox, Google Drive, OneDrive) are
 *   fully supported. TCP-based protocols (FTP, SFTP, SMB) throw
 *   [UnsupportedOperationException] on iOS.
 * - **Wasm**: HTTP-based protocols are fully supported. TCP-based protocols throw
 *   [UnsupportedOperationException] on Wasm.
 *
 * ## Format Parser Adapters
 *
 * Five "format parsers" exist as transport-layer adapters so the
 * [FormatRegistry][digital.vasic.yole.format.FormatRegistry] can represent
 * storage backends alongside actual text format parsers:
 *
 * | Adapter | Package |
 * |---------|---------|
 * | [DropboxParser][digital.vasic.yole.format.dropbox.DropboxParser] | `format.dropbox` |
 * | [FtpParser][digital.vasic.yole.format.ftp.FtpParser] | `format.ftp` |
 * | [SftpParser][digital.vasic.yole.format.sftp.SftpParser] | `format.sftp` |
 * | [GoogleDriveParser][digital.vasic.yole.format.googledrive.GoogleDriveParser] | `format.googledrive` |
 * | [OneDriveParser][digital.vasic.yole.format.onedrive.OneDriveParser] | `format.onedrive` |
 *
 * These are **not** file format parsers. They pass content through as plain text.
 * Actual file operations are handled by the corresponding service classes above.
 *
 * @see digital.vasic.yole.network.NetworkStorageService
 * @see digital.vasic.yole.format.FormatRegistry
 */
object NetworkProtocolStatus {

    /**
     * Implementation tier describing how much real network I/O a service performs.
     */
    enum class ImplementationTier {
        /** No network I/O. All operations mutate an in-memory virtual file system. */
        STUBBED,

        /** Some operations use real HTTP via ktor; others are stubbed or unimplemented. */
        PARTIALLY_IMPLEMENTED,

        /** Most CRUD operations hit real cloud/server APIs via ktor. Local file I/O is not wired up. */
        SUBSTANTIALLY_IMPLEMENTED,

        /** Real network I/O for all operations. Platform-limited protocols throw UnsupportedOperationException on unsupported platforms. */
        FULLY_IMPLEMENTED
    }

    /**
     * Metadata about a single protocol service implementation.
     *
     * @property protocolName Human-readable protocol name (e.g. "FTP", "Google Drive").
     * @property tier The [ImplementationTier] for this service.
     * @property serviceClass Fully qualified class name of the service implementation.
     * @property usesHttpClient Whether the service creates/uses an ktor HttpClient for real I/O.
     * @property authMechanism Short description of the authentication mechanism.
     * @property notes Additional implementation notes or known limitations.
     */
    data class ProtocolInfo(
        val protocolName: String,
        val tier: ImplementationTier,
        val serviceClass: String,
        val usesHttpClient: Boolean,
        val authMechanism: String,
        val notes: String
    )

    /**
     * Returns the implementation status of all eight network protocol services.
     *
     * This is a programmatic equivalent of the table in the class-level KDoc.
     * It can be used at runtime for diagnostics, settings screens, or test
     * assertions about expected implementation state.
     */
    fun allProtocols(): List<ProtocolInfo> = listOf(
        ProtocolInfo(
            protocolName = "FTP",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.ftp.FtpService",
            usesHttpClient = false,
            authMechanism = "FTP USER/PASS authentication over TCP",
            notes = "Real FTP protocol via platform TCP sockets (JVM). iOS and Wasm return UnsupportedOperationException. copyFile() returns failure (no FTP COPY command in RFC 959)."
        ),
        ProtocolInfo(
            protocolName = "SFTP",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.sftp.SftpService",
            usesHttpClient = false,
            authMechanism = "SSH password or public key authentication via sshj",
            notes = "Real SFTP via SSH using sshj library (JVM). iOS and Wasm return UnsupportedOperationException."
        ),
        ProtocolInfo(
            protocolName = "SMB/CIFS",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.smb.SmbService",
            usesHttpClient = false,
            authMechanism = "NTLM/Kerberos authentication via smbj",
            notes = "Real SMB2/3 protocol via smbj library (JVM). iOS and Wasm return UnsupportedOperationException."
        ),
        ProtocolInfo(
            protocolName = "WebDAV",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.webdav.WebDavService",
            usesHttpClient = true,
            authMechanism = "Basic, Digest (fallback to Basic), OAuth (Bearer), None",
            notes = "Real HTTP via Ktor. Full PROPFIND/GET/PUT/DELETE/MKCOL/MOVE/COPY. KMP-compatible custom XML parsing. All platforms supported."
        ),
        ProtocolInfo(
            protocolName = "Git",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.git.GitService",
            usesHttpClient = true,
            authMechanism = "Personal access token (preferred) or Basic auth",
            notes = "Real HTTP via Git Smart HTTP protocol and GitHub/GitLab/Bitbucket REST APIs. Read operations via protocol, write operations via platform REST APIs. All platforms supported."
        ),
        ProtocolInfo(
            protocolName = "Dropbox",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.dropbox.DropboxService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via DropboxOAuth2Flow + AuthTokenManager",
            notes = "Real Dropbox API v2. Full CRUD, quota, recent changes, sync. All platforms supported."
        ),
        ProtocolInfo(
            protocolName = "Google Drive",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.googledrive.GoogleDriveService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via GoogleDriveOAuth2Flow + AuthTokenManager",
            notes = "Real Google Drive API v3. Full CRUD, path-to-ID resolution, quota, recent changes, sync. All platforms supported."
        ),
        ProtocolInfo(
            protocolName = "OneDrive",
            tier = ImplementationTier.FULLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.onedrive.OneDriveService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via OneDriveOAuth2Flow + AuthTokenManager",
            notes = "Real Microsoft Graph API v1.0. Supports ME/BUSINESS/SHAREPOINT/GROUP drive types. Full CRUD, path-to-ID resolution, quota, delta sync. All platforms supported."
        )
    )

    /**
     * Returns only the protocols matching the given [tier].
     */
    fun protocolsByTier(tier: ImplementationTier): List<ProtocolInfo> =
        allProtocols().filter { it.tier == tier }

    /**
     * Returns only the protocols that perform real HTTP I/O.
     */
    fun protocolsWithRealHttp(): List<ProtocolInfo> =
        allProtocols().filter { it.usesHttpClient }

    /**
     * Returns only the fully stubbed protocols (in-memory only, no network I/O).
     * Currently returns an empty list as all protocols are fully implemented.
     */
    fun stubbedProtocols(): List<ProtocolInfo> =
        protocolsByTier(ImplementationTier.STUBBED)

    /**
     * Returns only the fully implemented protocols.
     */
    fun fullyImplementedProtocols(): List<ProtocolInfo> =
        protocolsByTier(ImplementationTier.FULLY_IMPLEMENTED)
}
