/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: language registry — lookup and detection.
 *#######################################################*/
package digital.vasic.yole.language

object LanguageRegistry {
    fun get(id: String): LanguageFormat? = LanguageMetadata.all.firstOrNull { it.id == id }

    fun detectByFilename(name: String): LanguageFormat? {
        val lowered = name.lowercase()
        for (lf in LanguageMetadata.all) {
            if (lf.extensions.any { lowered.endsWith(it.lowercase()) }) return lf
        }
        return null
    }

    fun all(): List<LanguageFormat> = LanguageMetadata.all
}
