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
 *
 * ## Protocol Status Overview
 *
 * | Protocol | Service Class | Tier | HTTP Client | Auth |
 * |----------|---------------|------|-------------|------|
 * | FTP | [FtpService][digital.vasic.yole.network.protocols.ftp.FtpService] | STUBBED | ktor (unused) | config-based (no real auth) |
 * | SFTP | [SftpService][digital.vasic.yole.network.protocols.sftp.SftpService] | STUBBED | ktor (unused) | password or private key (validated, not sent) |
 * | SMB/CIFS | [SmbService][digital.vasic.yole.network.protocols.smb.SmbService] | STUBBED | none | none |
 * | WebDAV | [WebDavService][digital.vasic.yole.network.protocols.webdav.WebDavService] | PARTIALLY_IMPLEMENTED | ktor (active) | Basic, Digest, OAuth, None |
 * | Git | [GitService][digital.vasic.yole.network.protocols.git.GitService] | PARTIALLY_IMPLEMENTED | ktor (active) | PAT or Basic |
 * | Dropbox | [DropboxService][digital.vasic.yole.network.protocols.dropbox.DropboxService] | SUBSTANTIALLY_IMPLEMENTED | ktor (active) | OAuth2 (DropboxOAuth2Flow) |
 * | Google Drive | [GoogleDriveService][digital.vasic.yole.network.protocols.googledrive.GoogleDriveService] | SUBSTANTIALLY_IMPLEMENTED | ktor (active) | OAuth2 (GoogleDriveOAuth2Flow) |
 * | OneDrive | [OneDriveService][digital.vasic.yole.network.protocols.onedrive.OneDriveService] | SUBSTANTIALLY_IMPLEMENTED | ktor (active) | OAuth2 (OneDriveOAuth2Flow) |
 *
 * ## Common Limitations Across All Services
 *
 * - **No local filesystem I/O**: Upload operations send empty byte arrays; download
 *   operations receive bytes but do not write them to disk. Wiring up
 *   platform-specific file I/O is a prerequisite for end-to-end file transfer.
 * - **Cancel/Pause/Resume**: Operation lifecycle methods are no-ops in all services.
 * - **syncAll**: Returns either an empty flow or a single "completed" operation.
 *   No incremental or delta-sync logic is implemented.
 * - **getRecentChanges**: Returns an empty list in most services.
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
        SUBSTANTIALLY_IMPLEMENTED
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
            tier = ImplementationTier.STUBBED,
            serviceClass = "digital.vasic.yole.network.protocols.ftp.FtpService",
            usesHttpClient = false,
            authMechanism = "Config-based (username/password validated, not sent)",
            notes = "In-memory VFS. No TCP/FTP socket connection. copyFile() always fails (no FTP COPY command)."
        ),
        ProtocolInfo(
            protocolName = "SFTP",
            tier = ImplementationTier.STUBBED,
            serviceClass = "digital.vasic.yole.network.protocols.sftp.SftpService",
            usesHttpClient = false,
            authMechanism = "Password or private key (validated locally, not sent over SSH)",
            notes = "In-memory VFS with delay()-based transfer simulation. Strict host key checking flag present but not enforced."
        ),
        ProtocolInfo(
            protocolName = "SMB/CIFS",
            tier = ImplementationTier.STUBBED,
            serviceClass = "digital.vasic.yole.network.protocols.smb.SmbService",
            usesHttpClient = false,
            authMechanism = "None (no SMB negotiation)",
            notes = "In-memory file tree. listFiles() returns failure. Requires native SMB protocol library."
        ),
        ProtocolInfo(
            protocolName = "WebDAV",
            tier = ImplementationTier.PARTIALLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.webdav.WebDavService",
            usesHttpClient = true,
            authMechanism = "Basic, Digest (fallback to Basic), OAuth (Bearer), None",
            notes = "Most complete implementation. Real PROPFIND/GET/PUT/DELETE/MKCOL/MOVE/COPY. KMP-compatible XML parsing. Upload sends empty body."
        ),
        ProtocolInfo(
            protocolName = "Git",
            tier = ImplementationTier.PARTIALLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.git.GitService",
            usesHttpClient = true,
            authMechanism = "Personal access token (preferred) or Basic auth",
            notes = "Read-only HTTP via Smart HTTP protocol and GitHub/GitLab/Bitbucket APIs. Write operations tracked locally as pending changes (not pushed)."
        ),
        ProtocolInfo(
            protocolName = "Dropbox",
            tier = ImplementationTier.SUBSTANTIALLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.dropbox.DropboxService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via DropboxOAuth2Flow + AuthTokenManager",
            notes = "Real Dropbox API v2 calls. Upload sends empty bytes. Download does not write to disk. Quota returns hardcoded values."
        ),
        ProtocolInfo(
            protocolName = "Google Drive",
            tier = ImplementationTier.SUBSTANTIALLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.googledrive.GoogleDriveService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via GoogleDriveOAuth2Flow + AuthTokenManager",
            notes = "Real Google Drive API v3 calls. getFileIdFromPath() is a stub (always returns rootFolderId). Upload sends empty bytes."
        ),
        ProtocolInfo(
            protocolName = "OneDrive",
            tier = ImplementationTier.SUBSTANTIALLY_IMPLEMENTED,
            serviceClass = "digital.vasic.yole.network.protocols.onedrive.OneDriveService",
            usesHttpClient = true,
            authMechanism = "OAuth2 via OneDriveOAuth2Flow + AuthTokenManager",
            notes = "Real Microsoft Graph API v1.0 calls. Supports ME/BUSINESS/SHAREPOINT/GROUP drive types. getItemIdFromPath() is a stub. Quota returns hardcoded values."
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
     */
    fun stubbedProtocols(): List<ProtocolInfo> =
        protocolsByTier(ImplementationTier.STUBBED)
}
