/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3 anti-bluff registry test (CONST-035).
 *
 * Verifies that ThemeRegistry.setActive() actually propagates a new Theme
 * through the activeTheme StateFlow — the very property ThemeProvider relies
 * on to publish theme changes app-wide.
 *
 * Anti-bluff guarantee: if setActive() were stubbed to a no-op (i.e., the
 * StateFlow value never changed), `setActiveSwitchesTheTheme` MUST FAIL.
 * Verified before commit by mutation: temporarily removing the
 * `_activeTheme.value = ...` write in setActive() turns this test RED.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import digital.vasic.yole.syntax.theme.LegacyThemeBridge
import digital.vasic.yole.syntax.theme.Theme
import digital.vasic.yole.syntax.theme.ThemeRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ThemeRegistryTest {

    /**
     * Each test must leave the registry in a defined state for the next.
     * We reset to the bootstrap theme by injecting a known Theme directly.
     */
    private val bootstrap = Theme(
        name = "test-bootstrap",
        type = "dark",
        uiColors = mapOf("editor.background" to 0xFF000000.toInt()),
        tokenColors = emptyMap(),
    )

    @BeforeTest
    fun seedBootstrap() {
        ThemeRegistry.setActive(bootstrap)
    }

    @AfterTest
    fun restoreBootstrap() {
        ThemeRegistry.setActive(bootstrap)
    }

    @Test
    fun availableExposesBuiltinNames() {
        val names = ThemeRegistry.available()
        assertTrue(names.contains("Yole Light"), "expected 'Yole Light' in $names")
        assertTrue(names.contains("Yole Dark"), "expected 'Yole Dark' in $names")
    }

    @Test
    fun activeThemeIsNonNullBeforeSetActive() {
        // The seedBootstrap() @BeforeTest seeded a known theme; activeTheme
        // must emit *something* — proves StateFlow is initialized.
        val current = ThemeRegistry.activeTheme.value
        assertNotNull(current)
        assertEquals("test-bootstrap", current.name)
    }

    @Test
    fun loadBuiltinYoleLightMatchesLegacy() = runBlocking<Unit> {
        val theme = ThemeRegistry.loadBuiltin("Yole Light")
        assertEquals("Yole Light", theme.name)
        // Spot-check a known legacy parity value (asserted byte-for-byte by
        // LegacyThemeParityTest in this same source set).
        val expected = LegacyThemeBridge.legacyLight["editor.background"]
            ?: fail("legacyLight is missing editor.background — bridge desync")
        val actual = theme.uiColor("editor.background")
            ?: fail("Yole-Light.json is missing editor.background")
        assertEquals(expected, actual)
    }

    @Test
    fun setActiveSwitchesTheTheme() = runBlocking<Unit> {
        // Mutation-anchor test: if setActive() ever no-ops, this MUST FAIL.
        // Before — bootstrap is active.
        assertEquals("test-bootstrap", ThemeRegistry.activeTheme.value.name)

        // Switch to Yole Light.
        ThemeRegistry.setActive("Yole Light")
        val afterLight = ThemeRegistry.activeTheme.value
        assertEquals("Yole Light", afterLight.name)
        assertEquals("light", afterLight.type)

        // Switch to Yole Dark.
        ThemeRegistry.setActive("Yole Dark")
        val afterDark = ThemeRegistry.activeTheme.value
        assertEquals("Yole Dark", afterDark.name)
        assertEquals("dark", afterDark.type)

        // The dark theme MUST have a different editor.background than light —
        // otherwise the switch didn't actually swap colors.
        val lightBg = LegacyThemeBridge.legacyLight["editor.background"]
        val darkBg = LegacyThemeBridge.legacyDark["editor.background"]
        assertNotNull(lightBg)
        assertNotNull(darkBg)
        assertTrue(
            lightBg != darkBg,
            "legacy palette desync: light and dark editor.background are equal " +
                "(both ${lightBg.toUInt().toString(16)})",
        )
        assertEquals(darkBg, afterDark.uiColor("editor.background"))
    }

    @Test
    fun setActiveUnknownNameThrows() {
        try {
            ThemeRegistry.setActive("NotAThemeName")
            fail("expected IllegalArgumentException for unknown built-in name")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                e.message?.contains("Unknown built-in theme") == true,
                "unexpected message: ${e.message}",
            )
        }
    }

    @Test
    fun setActiveByThemeBypassesBuiltinLookup() {
        val handcrafted = Theme(
            name = "Custom",
            type = "dark",
            uiColors = mapOf("editor.background" to 0xFF112233.toInt()),
            tokenColors = emptyMap(),
        )
        ThemeRegistry.setActive(handcrafted)
        assertEquals("Custom", ThemeRegistry.activeTheme.value.name)
        assertEquals(
            0xFF112233.toInt(),
            ThemeRegistry.activeTheme.value.uiColor("editor.background"),
        )
    }
}
