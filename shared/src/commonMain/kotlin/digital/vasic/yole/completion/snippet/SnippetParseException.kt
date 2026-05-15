/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: SnippetParseException — signals malformed VS Code
 * snippets.json input to VsCodeSnippetParser.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * Thrown by [VsCodeSnippetParser.parse] when the input JSON is malformed
 * or the root element is not a JSON object.
 */
class SnippetParseException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
