/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for DesktopImportDragDrop pure-logic helpers.
 * Tests use no Compose runtime — only JVM file system calls — so they
 * run cleanly under :desktopApp:test without a display.
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui.import_

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Exercises [resolveFirstDroppedFile] — the Compose-free extraction of the
 * file-resolution logic from [acceptImportFileDrops].
 *
 * Each test writes a real temporary file and verifies that the helper resolves
 * (or refuses) it correctly, ensuring behaviour is grounded in actual file I/O
 * per the CONST-035 anti-bluff covenant.
 */
class DesktopImportDragDropTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("yole_dragdrop_test").toFile()
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    // ── resolve: happy path ──────────────────────────────────────────────────

    @Test
    fun `resolveFirstDroppedFile returns file for valid local URI`() {
        val file = File(tempDir, "sample.md").also { it.writeText("# Hello") }
        val uris = listOf(file.toURI().toString())

        val result = resolveFirstDroppedFile(uris)

        assertNotNull(result, "Expected non-null File for a valid local URI")
        assertEquals(file.canonicalPath, result.canonicalPath)
    }

    @Test
    fun `resolveFirstDroppedFile returns first file when multiple URIs supplied`() {
        val file1 = File(tempDir, "first.txt").also { it.writeText("one") }
        val file2 = File(tempDir, "second.txt").also { it.writeText("two") }
        val uris = listOf(file1.toURI().toString(), file2.toURI().toString())

        val result = resolveFirstDroppedFile(uris)

        assertNotNull(result)
        assertEquals(file1.canonicalPath, result.canonicalPath, "Should resolve the first URI")
    }

    @Test
    fun `resolveFirstDroppedFile reads correct bytes from resolved file`() {
        val content = "# Drag-drop test\n\nSome content."
        val file = File(tempDir, "imported.md").also { it.writeText(content) }
        val uris = listOf(file.toURI().toString())

        val resolved = resolveFirstDroppedFile(uris)
        assertNotNull(resolved)
        val bytes = resolved.readBytes()

        assertEquals(content, bytes.toString(Charsets.UTF_8))
    }

    // ── resolve: rejection cases ─────────────────────────────────────────────

    @Test
    fun `resolveFirstDroppedFile returns null for empty list`() {
        assertNull(resolveFirstDroppedFile(emptyList()))
    }

    @Test
    fun `resolveFirstDroppedFile returns null for non-existent file URI`() {
        val missing = File(tempDir, "does_not_exist.docx")
        val uris = listOf(missing.toURI().toString())

        assertNull(resolveFirstDroppedFile(uris))
    }

    @Test
    fun `resolveFirstDroppedFile returns null for directory URI`() {
        val dirUri = tempDir.toURI().toString()

        assertNull(resolveFirstDroppedFile(listOf(dirUri)),
            "Directories must not be returned as droppable files")
    }

    @Test
    fun `resolveFirstDroppedFile returns null for malformed URI string`() {
        assertNull(resolveFirstDroppedFile(listOf("not a valid uri :// bad")))
    }

    // ── fileName extraction (simulates what acceptImportFileDrops does) ──────

    @Test
    fun `file name derived from resolved file matches original`() {
        val file = File(tempDir, "invoice.pdf").also { it.writeBytes(ByteArray(4) { it.toByte() }) }
        val uris = listOf(file.toURI().toString())

        val resolved = resolveFirstDroppedFile(uris)
        assertNotNull(resolved)
        assertEquals("invoice.pdf", resolved.name)
    }
}
