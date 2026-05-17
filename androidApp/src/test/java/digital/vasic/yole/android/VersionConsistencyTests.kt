/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Version Consistency Tests for Yole
 * Validates version strings are consistent across all platforms
 *
 *########################################################*/
package digital.vasic.yole.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.io.File

/**
 * Tests to ensure version consistency across all platforms
 */
@RunWith(AndroidJUnit4::class)
class VersionConsistencyTests {

    companion object {
        const val EXPECTED_VERSION = "2.0.0"
        const val EXPECTED_VERSION_CODE = 200

        /**
         * Resolves a path relative to the project root regardless of the
         * Gradle test task's working directory. Walks up from the JVM's
         * working dir until it finds a parent containing
         * `gradle/libs.versions.toml` (the canonical project-root marker).
         * Falls back to the unmodified relative path if no marker is found.
         */
        private fun projectFile(relative: String): File {
            var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
            while (dir != null) {
                if (File(dir, "gradle/libs.versions.toml").exists() &&
                    File(dir, "settings.gradle.kts").exists()) {
                    return File(dir, relative)
                }
                dir = dir.parentFile
            }
            return File(relative)
        }
    }

    @Test
    fun testAndroidBuildGradleVersion() {
        val buildFile = projectFile("androidApp/build.gradle.kts")
        assertTrue("Build file should exist", buildFile.exists())
        
        val content = buildFile.readText()
        assertTrue("Should contain correct versionName", 
            content.contains("versionName = \"$EXPECTED_VERSION\""))
        assertTrue("Should contain correct versionCode",
            content.contains("versionCode = $EXPECTED_VERSION_CODE"))
    }

    @Test
    fun testDesktopBuildGradleVersion() {
        val buildFile = projectFile("desktopApp/build.gradle.kts")
        assertTrue("Desktop build file should exist", buildFile.exists())
        
        val content = buildFile.readText()
        assertTrue("Should contain correct packageVersion",
            content.contains("packageVersion = \"$EXPECTED_VERSION\""))
    }

    @Test
    fun testAndroidAppVersionStrings() {
        val appFile = projectFile("androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt")
        assertTrue("Android app file should exist", appFile.exists())
        
        val content = appFile.readText()
        
        // Check no old version strings remain
        assertFalse("Should not contain old version 2.19.3",
            content.contains("2.19.3"))
        assertFalse("Should not contain old version 2.19",
            content.contains("2.19"))
        
        // Check new version is present
        assertTrue("Should contain new version in drawer",
            content.contains("Yole v$EXPECTED_VERSION"))
        assertTrue("Should contain new version in about",
            content.contains("Version $EXPECTED_VERSION"))
    }

    @Test
    fun testDesktopAppVersionStrings() {
        val appFile = projectFile("desktopApp/src/main/kotlin/digital/vasic/yole/desktop/ui/YoleApp.kt")
        assertTrue("Desktop app file should exist", appFile.exists())
        
        val content = appFile.readText()
        
        // Check no old version strings remain
        assertFalse("Should not contain old version 2.19.3.5",
            content.contains("2.19.3.5"))
        
        // Check new version is present
        assertTrue("Should contain new version",
            content.contains("Version: $EXPECTED_VERSION"))
    }

    @Test
    fun testDesktopDialogsVersionStrings() {
        val dialogFile = projectFile("desktopApp/src/main/kotlin/digital/vasic/yole/desktop/ui/Dialogs.kt")
        assertTrue("Desktop dialogs file should exist", dialogFile.exists())
        
        val content = dialogFile.readText()
        
        // Check no old version strings remain
        assertFalse("Should not contain old version 2.19.3.5",
            content.contains("2.19.3.5"))
        
        // Check new version is present
        assertTrue("Should contain new version",
            content.contains("Version $EXPECTED_VERSION"))
    }

    @Test
    fun testWebAppVersionStrings() {
        val mainFile = projectFile("webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/Main.kt")
        val enhancedFile = projectFile("webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/EnhancedWebApp.kt")
        
        assertTrue("Main web file should exist", mainFile.exists())
        assertTrue("Enhanced web file should exist", enhancedFile.exists())
        
        val mainContent = mainFile.readText()
        val enhancedContent = enhancedFile.readText()
        
        // Check no old version strings remain
        assertFalse("Main should not contain old version",
            mainContent.contains("2.19.3"))
        assertFalse("Enhanced should not contain old version",
            enhancedContent.contains("2.19.3"))
        
        // Check new version is present
        assertTrue("Main should contain new version",
            mainContent.contains("app.version=$EXPECTED_VERSION"))
        assertTrue("Enhanced should contain new version",
            enhancedContent.contains("app.version=$EXPECTED_VERSION"))
    }

    @Test
    fun testDesktopAutomationTestVersionStrings() {
        val testFile = projectFile("desktopApp/src/test/kotlin/digital/vasic/yole/desktop/FullUIAutomationTest.kt")
        assertTrue("Desktop test file should exist", testFile.exists())
        
        val content = testFile.readText()
        
        // Check no old version strings remain
        assertFalse("Should not contain old version 2.19.3.5",
            content.contains("2.19.3.5"))
        
        // Check new version is present
        assertTrue("Should contain new version",
            content.contains("Version: $EXPECTED_VERSION"))
    }

    @Test
    fun testNoOldVersionReferencesInCode() {
        val codeDirs = listOf(
            "androidApp/src",
            "desktopApp/src",
            "webApp/src"
        )
        
        var foundOldVersion = false
        for (dir in codeDirs) {
            val dirFile = projectFile(dir)
            if (dirFile.exists()) {
                dirFile.walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                    // Skip test files: they may legitimately contain version
                    // strings inside assertFalse messages or similar
                    // string-literal contexts. The audit target is production
                    // source, not the audit code itself.
                    .filter { !it.path.contains("/test/") && !it.path.contains("/androidTest/") }
                    .forEach { file ->
                        val content = file.readText()
                        if (content.contains("2.19.3") || content.contains("2.19.35")) {
                            foundOldVersion = true
                            println("Found old version in: ${file.path}")
                        }
                    }
            }
        }

        assertFalse("Should not find any old version references in code", foundOldVersion)
    }
}
