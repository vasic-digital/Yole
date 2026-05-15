/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: VsCodeSnippetParser — parses VS Code snippets.json
 * bundles into [Snippet] instances.
 *
 * VS Code snippet JSON schema:
 *   { "<name>": { "prefix": "...", "body": "..." | [...], "description": "..." } }
 * Both string and array forms of `body` are supported; arrays are joined
 * with `\n`. Missing `prefix` entries are silently skipped (VS Code
 * behaviour). Malformed JSON throws [SnippetParseException].
 *#######################################################*/
package digital.vasic.yole.completion.snippet

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Stateless parser for VS Code snippets.json bundles.
 *
 * Thread-safe: all state is local to [parse].
 */
object VsCodeSnippetParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    /**
     * Parse a VS Code snippets.json string into a list of [Snippet]s.
     *
     * @param input raw JSON content of a snippets.json file.
     * @return parsed snippets; entries without a `prefix` are skipped.
     * @throws SnippetParseException if [input] is not valid JSON or its
     *   root element is not a JSON object.
     */
    fun parse(input: String): List<Snippet> {
        val root: JsonObject = try {
            json.parseToJsonElement(input).jsonObject
        } catch (e: SerializationException) {
            throw SnippetParseException("malformed JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw SnippetParseException("root is not a JSON object: ${e.message}", e)
        }
        return root.values.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val prefix = obj["prefix"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val body = parseBody(obj["body"]) ?: return@mapNotNull null
            val description = obj["description"]?.jsonPrimitive?.contentOrNull
            val scope = obj["scope"]?.jsonPrimitive?.contentOrNull ?: ""
            Snippet(prefix = prefix, body = body, description = description, scope = scope)
        }
    }

    /**
     * Convert a snippet body JSON element to a string.
     *
     * - [JsonPrimitive]: returned as-is (single-line body).
     * - [JsonArray]: joined with `\n` (multi-line body — the VS Code norm).
     * - anything else (including null): returns null → entry skipped.
     */
    private fun parseBody(element: kotlinx.serialization.json.JsonElement?): String? =
        when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonArray -> element.joinToString("\n") { (it as? JsonPrimitive)?.contentOrNull ?: "" }
            else -> null
        }
}
