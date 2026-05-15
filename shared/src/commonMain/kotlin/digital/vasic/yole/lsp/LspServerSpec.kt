/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 1: LspServerSpec — per-language LSP server
 * metadata, deserialized from
 * shared/src/commonMain/resources/lsp-servers/<langId>/server.json.
 *
 * The spec is platform-agnostic. The binary path is resolved per
 * platform at install/extract time by LspServerInstaller (Phase 3).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Per-language LSP server metadata.
 *
 * @property langIds Yole language IDs this server handles (one server
 *   may cover multiple langs — clangd handles both `c` and `cpp`).
 * @property executable bare executable name (e.g., `rust-analyzer`).
 *   Resolved to an absolute path by LspServerInstaller (Phase 3).
 * @property args command-line args appended after the executable.
 * @property projectMarkers filenames LspWorkspaceResolver (Phase 2)
 *   walks up looking for. First match becomes the workspace rootUri.
 * @property initOptions sent in the LSP `initialize` request as the
 *   server-specific `initializationOptions` JSON.
 */
@Serializable
data class LspServerSpec(
    val langIds: List<String>,
    val executable: String,
    val args: List<String> = emptyList(),
    val projectMarkers: List<String> = emptyList(),
    val initOptions: JsonObject = JsonObject(emptyMap()),
)
