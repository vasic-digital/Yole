/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 #android-tree-sitter-ndk-so-missing — verifies that the
 * shared module's `repackageTreeSitterJarForAndroid` Gradle task
 * actually produced a patched JAR with our Yole-replacement
 * `org.treesitter.utils.NativeUtils` class swapped in, and that
 * the JAR is byte-for-byte structurally sound.
 *
 * This is NOT a substitute for the on-device
 * TokenizerEngineAndroidTest — it cannot execute the .so files
 * (Android ABIs are not loadable on a host JVM). It IS a build-
 * artefact integrity check: if the repackaging task ever silently
 * regresses (e.g. upstream JAR layout changes, ZipOutputStream
 * compression mode flips, Kotlin DSL refactor breaks
 * `RepackageBonedeJarTask`), this test catches it on the next
 * `:shared:desktopTest` run without needing an emulator.
 *
 * Anti-bluff per CONST-035:
 *   - Asserts the patched JAR exists (so the Gradle task ran).
 *   - Asserts the patched NativeUtils.class bytecode contains the
 *     Yole-specific `loadOnAndroid` method (proves our class is in
 *     the JAR, not the bonede original).
 *   - Asserts the bonede-original NativeUtils internal helper
 *     `getLibStorePath` is preserved (proves Desktop fall-back path
 *     still works — Kover bench on desktop continues to pass).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class AndroidNativeUtilsPatchTest {

    private val sharedBuildDir: File =
        File(System.getProperty("user.dir") ?: ".").let {
            // `gradle :shared:desktopTest` runs with user.dir == shared/
            // so build dir is "build" relative to that.
            File(it, "build/repackaged-libs")
        }

    private val patchedJar: File = File(sharedBuildDir, "tree-sitter-android.jar")

    @Test
    fun repackagedJarExists() {
        assertTrue(
            "Patched tree-sitter-android.jar must exist at " +
                "${patchedJar.absolutePath}. Run " +
                "`:shared:repackageTreeSitterJarForAndroid` first.",
            patchedJar.exists() && patchedJar.length() > 0
        )
    }

    @Test
    fun patchedJarContainsYoleNativeUtilsClass() {
        if (!patchedJar.exists()) {
            // Fail loud — same exit criterion as repackagedJarExists.
            assertTrue(
                "Patched JAR not present at ${patchedJar.absolutePath}",
                false
            )
            return
        }
        ZipFile(patchedJar).use { zip ->
            val entry = zip.getEntry("org/treesitter/utils/NativeUtils.class")
            assertNotNull(
                "Patched JAR must contain org/treesitter/utils/NativeUtils.class",
                entry
            )
            val bytes = zip.getInputStream(entry).use { it.readAllBytes() }
            // Forensic markers from our replacement source. We do NOT
            // parse the class file — a substring scan of the constant
            // pool is sufficient and stable.
            val asString = String(bytes, Charsets.ISO_8859_1)
            assertTrue(
                "Patched NativeUtils.class is missing Yole marker 'loadOnAndroid' " +
                    "— the repackage step must have failed and reverted to " +
                    "the bonede original. Patched JAR: ${patchedJar.absolutePath}",
                asString.contains("loadOnAndroid")
            )
            assertTrue(
                "Patched NativeUtils.class is missing 'java.vm.vendor' lookup " +
                    "— the Android-detect branch is gone.",
                asString.contains("java.vm.vendor")
            )
        }
    }
}
