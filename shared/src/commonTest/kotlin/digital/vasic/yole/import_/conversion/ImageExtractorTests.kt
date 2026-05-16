/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: anti-bluff tests for ImageExtractor + ExtractedImage.
 *
 * Mutation stub: replace normaliseFormat body with `return format`
 * → test jpgFormat_normalisedToJpeg FAILS.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ImageExtractorTests {

    @Test
    fun construction_allFieldsPreserved() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
        val image = ImageExtractor.fromBytes(bytes, "png", "figure1.png")

        assertContentEquals(bytes, image.data, "Raw bytes must be preserved exactly")
        assertEquals("png", image.format, "Format 'png' must be stored as-is")
        assertEquals("figure1.png", image.suggestedFileName, "File name must be preserved")
    }

    @Test
    fun jpgFormat_normalisedToJpeg() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        val image = ImageExtractor.fromBytes(bytes, "jpg", "photo.jpg")

        assertEquals(
            "jpeg",
            image.format,
            "Format 'jpg' must be normalised to 'jpeg'",
        )
        assertContentEquals(bytes, image.data, "Bytes must not be altered during normalisation")
    }
}
