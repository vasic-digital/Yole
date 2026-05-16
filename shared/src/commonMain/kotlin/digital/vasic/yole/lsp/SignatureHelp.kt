/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 2: SignatureHelp + supporting data classes — forward-declared
 * placeholders.
 *
 * Phase 3 finalizes LSP4J SignatureHelp mapping via mapLspSignatureHelp().
 * Phase 2 defines these types so LspServerHost.signatureHelp() compiles on
 * all targets.
 *
 * Cross-platform (CONST-037):
 *   - Desktop/Android: populated by LspServerHost.signatureHelp() JVM actual.
 *   - iOS/Wasm:        signatureHelp() returns null; never instantiated.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Metadata for a single parameter within a [SignatureInformation].
 *
 * @param label         Parameter label shown in the signature pill/popup.
 * @param documentation Optional markdown documentation for this parameter.
 */
data class ParameterInformation(
    val label: String,
    val documentation: String?,
)

/**
 * Full signature of a callable (function, method, constructor).
 *
 * @param label         Full signature label string (e.g. "fun foo(a: Int, b: String): Unit").
 * @param documentation Optional markdown documentation for the signature.
 * @param parameters    Ordered list of parameter metadata.
 */
data class SignatureInformation(
    val label: String,
    val documentation: String?,
    val parameters: List<ParameterInformation>,
)

/**
 * Signature help result returned by [LspServerHost.signatureHelp].
 *
 * @param signatures      All overload signatures available at the call site.
 * @param activeSignature Zero-based index of the currently active signature.
 * @param activeParameter Zero-based index of the currently active parameter.
 */
data class SignatureHelp(
    val signatures: List<SignatureInformation>,
    val activeSignature: Int,
    val activeParameter: Int,
)
