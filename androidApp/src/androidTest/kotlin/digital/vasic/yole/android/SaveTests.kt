/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Instrumented tests for SAF-based file saving.
 * Runs on real devices and emulators.
 *
 *########################################################*/
package digital.vasic.yole.android

import android.content.Intent
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import digital.vasic.yole.util.AppContextHolder
import digital.vasic.yole.util.FileHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class SaveTests {

    @get:Rule
    val activityRule = ActivityTestRule(
        MainActivity::class.java, false, false
    )

    private val context get() =
        InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        AppContextHolder.context = context
    }

    @Test
    fun saveToCacheAndReadBack() {
        val fileName = "save_test_${System.currentTimeMillis()}.txt"
        val content = "Hello from Yole save test!\n" +
            "API level: ${android.os.Build.VERSION.SDK_INT}"
        val cacheDir = File(context.filesDir, "autosave")
        cacheDir.mkdirs()
        val cacheFile = File(cacheDir, fileName)

        cacheFile.writeText(content)
        assertTrue("File should exist after write", cacheFile.exists())

        val handle = FileHandle(Uri.fromFile(cacheFile).toString())
        val read = handle.readBytes()
        assertNotNull("Read should return content", read)
        assertEquals(content, String(read!!))

        println("SAVE_VERIFIED: ${read.size} bytes")

        cacheFile.delete()
    }

    @Test
    fun writeAndExists() {
        val fileName = "exists_test_${System.currentTimeMillis()}.txt"
        val cacheFile = File(context.filesDir, fileName)
        val uri = Uri.fromFile(cacheFile).toString()

        val handle = FileHandle(uri)
        val wrote = handle.writeBytes("test content".toByteArray())
        assertTrue("Write should succeed", wrote)
        assertTrue("exists() should return true", handle.exists())

        println("SAVE_VERIFIED: ${cacheFile.length()} bytes")

        cacheFile.delete()
    }

    @Test
    fun readNonExistentReturnsNull() {
        val handle = FileHandle(
            Uri.fromFile(
                File(
                    context.filesDir,
                    "nonexistent_${System.currentTimeMillis()}.txt"
                )
            ).toString()
        )
        assertNull(
            "Non-existent file should return null",
            handle.readBytes()
        )
    }

    @Test
    fun writeEmptyContent() {
        val f = File(
            context.filesDir,
            "empty_${System.currentTimeMillis()}.txt"
        )
        val handle = FileHandle(Uri.fromFile(f).toString())
        val ok = handle.writeBytes(ByteArray(0))
        assertTrue("Empty write should succeed", ok)
        assertTrue("File should exist", f.exists())
        assertEquals(0, f.length().toInt())
        println("SAVE_VERIFIED: 0 bytes")
        f.delete()
    }

    @Test
    fun writeAndReadRoundtrip() {
        val f = File(
            context.filesDir,
            "roundtrip_${System.currentTimeMillis()}.txt"
        )
        val uri = Uri.fromFile(f).toString()
        val original =
            "Roundtrip test content with special chars: \u00E4\u00F6\u00FC \u00F1 \u4F60\u597D"
        val handle1 = FileHandle(uri)
        assertTrue(handle1.writeBytes(original.toByteArray()))

        val handle2 = FileHandle(uri)
        val read = handle2.readBytes()
        assertNotNull(read)
        assertEquals(original, String(read!!))
        println("SAVE_VERIFIED: ${read.size} bytes")
        f.delete()
    }
}
