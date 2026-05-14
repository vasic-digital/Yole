/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 #android-tree-sitter-ndk-so-missing — committed Android
 * NDK .so binaries integrity check.
 *
 * Asserts that each vendored shared library under
 * shared/native/android-tree-sitter/<abi>/ is:
 *   - present,
 *   - a valid ELF 32/64-bit shared object (magic 0x7F 'E' 'L' 'F'),
 *   - of the expected ABI architecture per the directory name,
 *   - non-empty (> 100 KB — a stripped stub would be ~10 KB).
 *
 * Anti-bluff per CONST-035: if any of these files are accidentally
 * truncated, replaced with a placeholder, or built for the wrong
 * triple, this test fails BEFORE the APK is shipped. The on-device
 * TokenizerEngineAndroidTest catches behavioural regressions; this
 * one catches binary-substitution regressions at desktop-test time
 * (no emulator required, sub-second runtime).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidNativeSoIntegrityTest {

    // shared/native/android-tree-sitter/ relative to the shared module
    // root (which is the JVM working dir of :shared:desktopTest).
    private val nativeDir: File =
        File(System.getProperty("user.dir") ?: ".", "native/android-tree-sitter")

    private fun verifyElf(
        abi: String,
        libBase: String,
        expectedClass: Int, // 1=ELF32, 2=ELF64
        expectedMachine: Int, // e_machine; ARM=40, AArch64=183, x86_64=62
    ) {
        val file = File(File(nativeDir, abi), "lib$libBase.so")
        assertTrue(
            "Missing Android NDK $abi $libBase build at ${file.absolutePath}. " +
                "Rebuild via the Android NDK toolchain — see commit notes for " +
                "#android-tree-sitter-ndk-so-missing.",
            file.exists() && file.isFile
        )
        assertTrue(
            "$file is suspiciously small (${file.length()} bytes) — likely " +
                "a placeholder or truncated build, not a real Tree-Sitter binary.",
            file.length() > 100_000
        )
        val header = file.inputStream().use { it.readNBytes(20) }
        // ELF magic
        assertEquals(0x7F.toByte(), header[0])
        assertEquals('E'.code.toByte(), header[1])
        assertEquals('L'.code.toByte(), header[2])
        assertEquals('F'.code.toByte(), header[3])
        // EI_CLASS — 1=32-bit, 2=64-bit
        assertEquals(
            "$file ELF class mismatch (expected ${expectedClass}, got ${header[4]})",
            expectedClass.toByte(),
            header[4]
        )
        // EI_DATA — 1=little-endian
        assertEquals(1.toByte(), header[5])
        // e_machine at offset 18 (little-endian u16)
        val machine = (header[18].toInt() and 0xFF) or
            ((header[19].toInt() and 0xFF) shl 8)
        assertEquals(
            "$file e_machine mismatch (expected $expectedMachine, got $machine)",
            expectedMachine,
            machine
        )
    }

    @Test fun arm64v8aTreeSitterIsAndroidElf() =
        verifyElf("arm64-v8a", "tree-sitter", 2, 183 /* AArch64 */)

    @Test fun arm64v8aTreeSitterMarkdownIsAndroidElf() =
        verifyElf("arm64-v8a", "tree-sitter-markdown", 2, 183 /* AArch64 */)

    @Test fun armeabiV7aTreeSitterIsAndroidElf() =
        verifyElf("armeabi-v7a", "tree-sitter", 1, 40 /* ARM */)

    @Test fun armeabiV7aTreeSitterMarkdownIsAndroidElf() =
        verifyElf("armeabi-v7a", "tree-sitter-markdown", 1, 40 /* ARM */)

    @Test fun x8664TreeSitterIsAndroidElf() =
        verifyElf("x86_64", "tree-sitter", 2, 62 /* AMD64 / x86-64 */)

    @Test fun x8664TreeSitterMarkdownIsAndroidElf() =
        verifyElf("x86_64", "tree-sitter-markdown", 2, 62 /* AMD64 / x86-64 */)
}
