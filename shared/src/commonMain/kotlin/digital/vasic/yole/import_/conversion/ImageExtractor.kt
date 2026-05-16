/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: ImageExtractor — raw bytes → ExtractedImage wrapper.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

/**
 * Represents an image extracted from a source document.
 *
 * @property data              Raw image bytes.
 * @property format            Normalised format string (e.g. "jpeg", "png", "gif").
 * @property suggestedFileName A file-system-safe name for persisting the image.
 */
data class ExtractedImage(
    val data: ByteArray,
    val format: String,
    val suggestedFileName: String,
) {
    // ByteArray equality by content, not identity
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExtractedImage) return false
        return format == other.format &&
            suggestedFileName == other.suggestedFileName &&
            data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + suggestedFileName.hashCode()
        return result
    }
}

/**
 * Wraps raw image bytes and associated metadata into an [ExtractedImage].
 *
 * Format normalisation: "jpg" is canonicalised to "jpeg".
 * All other format strings are lower-cased as-is.
 */
object ImageExtractor {

    /**
     * Constructs an [ExtractedImage] from raw [bytes].
     *
     * @param bytes  Raw image data.
     * @param format Source format hint (e.g. "jpg", "png"). "jpg" is normalised to "jpeg".
     * @param name   Suggested file name for the extracted image.
     */
    fun fromBytes(bytes: ByteArray, format: String, name: String): ExtractedImage {
        val normalised = normaliseFormat(format)
        return ExtractedImage(data = bytes, format = normalised, suggestedFileName = name)
    }

    internal fun normaliseFormat(format: String): String {
        val lower = format.lowercase()
        return if (lower == "jpg") "jpeg" else lower
    }
}
