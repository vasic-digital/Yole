/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 1: DocumentImporter interface + ImportError sealed class.
 *#######################################################*/
package digital.vasic.yole.import_

/**
 * Sealed hierarchy of errors that a [DocumentImporter] may raise.
 *
 * All subclasses extend [RuntimeException] so callers can
 * catch them with a single `catch (e: ImportError)`.
 *
 * CancellationException is never swallowed here — importers are
 * responsible for rethrowing it from their suspend bodies.
 */
sealed class ImportError(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {

    /** The requested format is not supported on the current platform. */
    class NotSupported(format: String, platform: String) :
        ImportError("Format $format not supported on $platform")

    /** The source bytes could not be parsed as the declared format. */
    class Malformed(format: String, cause: Throwable) :
        ImportError("Malformed $format: ${cause.message}", cause)

    /** The import operation exceeded its time budget. */
    class Timeout(format: String) :
        ImportError("Import of $format timed out")
}

/**
 * Contract for a single-format document importer.
 *
 * Implementations exist per platform (JVM for Desktop + Android; stub on iOS/Wasm).
 * The suspend modifier allows long-running I/O without blocking the calling coroutine;
 * implementations MUST rethrow [kotlinx.coroutines.CancellationException] when caught.
 */
interface DocumentImporter {
    /** File extensions this importer handles, lower-case, without leading dot. */
    val supportedExtensions: Set<String>

    /**
     * Convert [bytes] (the raw file content) to an [ImportedDocument].
     *
     * @param bytes Raw file bytes.
     * @param fileName Original file name, used for format-hint fallback.
     * @return [Result.success] on conversion, [Result.failure] wrapping an [ImportError] otherwise.
     */
    suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument>
}
