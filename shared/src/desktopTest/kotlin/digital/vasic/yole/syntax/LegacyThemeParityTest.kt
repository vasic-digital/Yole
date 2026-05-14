/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 2 anti-bluff parity test (CONST-035).
 *
 * Yole-Light.json + Yole-Dark.json MUST reproduce the legacy
 * IdeTheme/YoleColors values byte-for-byte per every VS Code key
 * that LegacyThemeBridge defines.
 *
 * Anti-bluff guarantee: flipping any color byte in either
 * Yole-Light.json or Yole-Dark.json MUST cause this test to FAIL.
 * This property was mutation-verified before commit (iter-57 Phase 2).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.theme.LegacyThemeBridge
import digital.vasic.yole.syntax.theme.VsCodeThemeParser
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Byte-exact RGB parity test between the bundled VS Code JSON theme files
 * and the historical legacy palette recorded in [LegacyThemeBridge].
 *
 * Resources are loaded via the JVM classloader; the JSON files live in
 * `shared/src/desktopTest/resources/themes/builtin/` (test classpath) and
 * `shared/src/commonMain/resources/themes/builtin/` (production classpath).
 */
class LegacyThemeParityTest {

    /**
     * Load a classpath resource as a UTF-8 string.
     * Fails fast with [IllegalStateException] if the resource is not found —
     * this is itself a test assertion: missing JSON = failing build.
     */
    private fun loadJsonResource(path: String): String {
        val stream = LegacyThemeParityTest::class.java.classLoader
            ?.getResourceAsStream(path)
            ?: error(
                "Resource '$path' not found on the test classpath. " +
                    "Ensure the JSON file exists in " +
                    "shared/src/desktopTest/resources/$path"
            )
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    @Test
    fun yoleLightJsonMatchesLegacyPalette() = runBlocking<Unit> {
        val json = loadJsonResource("themes/builtin/Yole-Light.json")
        val theme = VsCodeThemeParser.parse(json)

        for ((key, expected) in LegacyThemeBridge.legacyLight) {
            val actual = theme.uiColor(key)
            assertNotNull(
                actual,
                "Yole-Light.json is missing required colors.* key `$key` " +
                    "(expected ARGB 0x${expected.toUInt().toString(16).uppercase()})"
            )
            assertEquals(
                expected,
                actual,
                "Yole-Light.json colors[\"$key\"] MUST equal legacy palette value. " +
                    "Expected ARGB 0x${expected.toUInt().toString(16).uppercase()} " +
                    "but got 0x${actual.toUInt().toString(16).uppercase()}"
            )
        }
    }

    @Test
    fun yoleDarkJsonMatchesLegacyPalette() = runBlocking<Unit> {
        val json = loadJsonResource("themes/builtin/Yole-Dark.json")
        val theme = VsCodeThemeParser.parse(json)

        for ((key, expected) in LegacyThemeBridge.legacyDark) {
            val actual = theme.uiColor(key)
            assertNotNull(
                actual,
                "Yole-Dark.json is missing required colors.* key `$key` " +
                    "(expected ARGB 0x${expected.toUInt().toString(16).uppercase()})"
            )
            assertEquals(
                expected,
                actual,
                "Yole-Dark.json colors[\"$key\"] MUST equal legacy palette value. " +
                    "Expected ARGB 0x${expected.toUInt().toString(16).uppercase()} " +
                    "but got 0x${actual.toUInt().toString(16).uppercase()}"
            )
        }
    }
}
