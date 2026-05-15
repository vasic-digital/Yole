/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-59 anti-bluff structural test (CONST-035).
 *
 * Mutation contract: reverting `applicationIdSuffix = ".dev"` or
 * `versionNameSuffix = " DEV"` in androidApp/build.gradle.kts, or
 * removing the green tint from src/debug/res/values/colors.xml,
 * MUST cause one of these tests to FAIL. The user-visible behavior
 * under test is "DEV builds install side-by-side with production
 * under a visually-distinct (green) launcher icon".
 *
 * Pure JVM — reads the gradle source + resource XML from disk.
 *
 *########################################################*/
package digital.vasic.yole.android

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class IterB59VariantConfigTest {

    private fun readFirstExisting(vararg paths: String): String {
        for (p in paths) {
            val f = File(p)
            if (f.isFile) return f.readText()
        }
        error("None of these paths exist (cwd=${File(".").absolutePath}): " + paths.joinToString())
    }

    @Test
    fun debugVariantHasApplicationIdSuffixDotDev() {
        val src = readFirstExisting(
            "androidApp/build.gradle.kts",
            "../androidApp/build.gradle.kts",
            "build.gradle.kts",
        )
        assertTrue(
            "debug build type MUST declare applicationIdSuffix = \".dev\" (iter-59)",
            src.contains("applicationIdSuffix = \".dev\""),
        )
    }

    @Test
    fun debugVariantHasVersionNameSuffixDEV() {
        val src = readFirstExisting(
            "androidApp/build.gradle.kts",
            "../androidApp/build.gradle.kts",
            "build.gradle.kts",
        )
        assertTrue(
            "debug build type MUST declare versionNameSuffix = \" DEV\" (iter-59)",
            src.contains("versionNameSuffix = \" DEV\""),
        )
    }

    @Test
    fun debugLauncherIconHasGreenBackground() {
        val colorsXml = readFirstExisting(
            "androidApp/src/debug/res/values/colors.xml",
            "../androidApp/src/debug/res/values/colors.xml",
            "src/debug/res/values/colors.xml",
        )
        assertTrue(
            "debug launcher background MUST be #00FF00 (iter-59 green-tint mandate)",
            colorsXml.contains("#FF00FF00") || colorsXml.contains("#00FF00"),
        )
    }

    @Test
    fun debugAdaptiveIconReferencesGreenBackground() {
        val iconXml = readFirstExisting(
            "androidApp/src/debug/res/mipmap-anydpi-v26/ic_launcher.xml",
            "../androidApp/src/debug/res/mipmap-anydpi-v26/ic_launcher.xml",
            "src/debug/res/mipmap-anydpi-v26/ic_launcher.xml",
        )
        assertTrue(
            "debug adaptive-icon MUST reference @color/ic_launcher_background_dev",
            iconXml.contains("@color/ic_launcher_background_dev"),
        )
    }

    @Test
    fun debugManifestPlaceholderIsYoleDev() {
        val src = readFirstExisting(
            "androidApp/build.gradle.kts",
            "../androidApp/build.gradle.kts",
            "build.gradle.kts",
        )
        // Both quote styles accepted to keep the assertion robust.
        val hasDevLabel = src.contains("manifestPlaceholders[\"appLabel\"] = \"Yole DEV\"") ||
            src.contains("manifestPlaceholders[\"appLabel\"]=\"Yole DEV\"")
        assertTrue(
            "debug build type MUST set manifestPlaceholders[\"appLabel\"] = \"Yole DEV\" (iter-59)",
            hasDevLabel,
        )
    }

    @Test
    fun googleServicesJsonRegistersDevPackage() {
        val gs = readFirstExisting(
            "androidApp/google-services.json",
            "../androidApp/google-services.json",
            "google-services.json",
        )
        assertTrue(
            "google-services.json MUST register digital.vasic.yole.android.dev (iter-59 Firebase registration)",
            gs.contains("\"package_name\": \"digital.vasic.yole.android.dev\""),
        )
        assertTrue(
            "google-services.json MUST still register the production package digital.vasic.yole.android",
            gs.contains("\"package_name\": \"digital.vasic.yole.android\""),
        )
    }
}
