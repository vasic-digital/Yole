/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57: kotlinx.serialization-based VS Code theme JSON parser.
 *   Reads `name`, `type`, `colors.*`, `tokenColors[]` from a VS Code
 *   theme JSON string and produces a Theme value object.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object VsCodeThemeParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = false }

    /**
     * Parse a VS Code theme JSON string. Throws ThemeParseException on any
     * malformed input or missing required field (name, type).
     */
    fun parse(input: String): Theme {
        val root: JsonObject = try {
            json.parseToJsonElement(input).jsonObject
        } catch (e: SerializationException) {
            throw ThemeParseException("malformed JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ThemeParseException("root is not a JSON object: ${e.message}", e)
        }

        val name = root["name"]?.jsonPrimitive?.contentOrNull
            ?: throw ThemeParseException("required field `name` missing")
        val type = root["type"]?.jsonPrimitive?.contentOrNull
            ?: throw ThemeParseException("required field `type` missing")

        val uiColors = parseUiColors(root["colors"])
        val tokenColors = parseTokenColors(root["tokenColors"])

        return Theme(name = name, type = type, uiColors = uiColors, tokenColors = tokenColors)
    }

    private fun parseUiColors(element: JsonElement?): Map<String, Int> {
        val obj = element?.jsonObject ?: return emptyMap()
        return obj.mapNotNull { (key, value) ->
            val hex = (value as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            parseHexColor(hex)?.let { key to it }
        }.toMap()
    }

    private fun parseTokenColors(element: JsonElement?): Map<String, Int> {
        val arr = element?.jsonArray ?: return emptyMap()
        val out = mutableMapOf<String, Int>()
        for (entry in arr) {
            val obj = entry.jsonObject
            val settings = obj["settings"]?.jsonObject ?: continue
            val foreground = (settings["foreground"] as? JsonPrimitive)?.contentOrNull ?: continue
            val color = parseHexColor(foreground) ?: continue
            when (val scope = obj["scope"]) {
                is JsonPrimitive -> {
                    scope.contentOrNull?.let { out[it.trim()] = color }
                }
                is JsonArray -> {
                    scope.forEach { s ->
                        (s as? JsonPrimitive)?.contentOrNull?.let { out[it.trim()] = color }
                    }
                }
                else -> { /* skip */ }
            }
        }
        return out
    }

    /**
     * Parse "#RRGGBB" or "#RRGGBBAA" to ARGB int (0xFFRRGGBB or 0xAARRGGBB).
     * Returns null on malformed.
     */
    private fun parseHexColor(hex: String): Int? {
        val trimmed = hex.trim().removePrefix("#")
        return when (trimmed.length) {
            6 -> runCatching {
                val rgb = trimmed.toLong(16).toInt()
                (0xFF shl 24) or rgb
            }.getOrNull()
            8 -> runCatching {
                // VS Code: #RRGGBBAA — convert to ARGB
                val rgba = trimmed.toLong(16)
                val r = (rgba shr 24) and 0xFF
                val g = (rgba shr 16) and 0xFF
                val b = (rgba shr 8) and 0xFF
                val a = rgba and 0xFF
                ((a shl 24) or (r shl 16) or (g shl 8) or b).toInt()
            }.getOrNull()
            else -> null
        }
    }
}
