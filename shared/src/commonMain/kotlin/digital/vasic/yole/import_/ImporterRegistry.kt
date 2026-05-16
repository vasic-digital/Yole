/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 1: ImporterRegistry — extension → importer lookup.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Maps normalised file extensions to their [DocumentImporter] implementation.
 *
 * Construction goes through the [Companion.default] factory which flattens
 * each importer's [DocumentImporter.supportedExtensions] set into the lookup map.
 * Last importer wins on collisions (preserves list ordering semantics).
 *
 * The constructor is private; only [Companion.default] may instantiate.
 */
class ImporterRegistry private constructor(
    private val byExt: Map<String, DocumentImporter>,
) {

    /**
     * Look up the importer for [ext].
     *
     * [ext] is normalised: lowercased and any leading dot removed before lookup.
     *
     * @return the matching [DocumentImporter], or `null` if no importer is registered.
     */
    fun forExtension(ext: String): DocumentImporter? =
        byExt[ext.lowercase().removePrefix(".")]

    /** All extensions currently registered (lower-case, no leading dot). */
    fun supported(): Set<String> = byExt.keys

    companion object {
        /**
         * Build an [ImporterRegistry] from a list of [importers].
         *
         * Each importer's [DocumentImporter.supportedExtensions] are registered.
         * If two importers claim the same extension, the later one in [importers] wins.
         */
        fun default(importers: List<DocumentImporter>): ImporterRegistry {
            val byExt = mutableMapOf<String, DocumentImporter>()
            for (importer in importers) {
                for (ext in importer.supportedExtensions) {
                    byExt[ext.lowercase()] = importer
                }
            }
            return ImporterRegistry(byExt)
        }
    }
}
