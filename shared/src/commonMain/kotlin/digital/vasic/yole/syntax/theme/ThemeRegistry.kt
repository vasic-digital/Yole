/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: ThemeRegistry — central authority for the active
 * VS Code theme. Wraps a MutableStateFlow<Theme> so all Composables
 * subscribed via ThemeProvider re-emit on theme change.
 *
 * Built-in themes ship under `shared/src/commonMain/resources/themes/builtin/`.
 * Per-platform classloader/asset access is delegated to the expect fun
 * `readBuiltinTheme(filename)` (JVM via ClassLoader, iOS/Wasm with platform
 * stubs that will be filled in Phases 6/7).
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read a built-in theme JSON file from the platform's bundled resources.
 *
 * @param filename leaf filename (e.g., "Yole-Dark.json"); the implementation
 *                 prepends the canonical `themes/builtin/` prefix.
 * @return the JSON contents as a UTF-8 string.
 * @throws IllegalStateException if the resource is not bundled on this platform
 *         (e.g., iOS/Wasm where Phase 6/7 still pending).
 */
expect fun readBuiltinTheme(filename: String): String

/**
 * Active-theme broker. The single source of truth that Composables read
 * via [ThemeProvider] / [LocalTheme].
 *
 * Theme switches MUST go through [setActive]; the StateFlow re-emits the
 * new theme and every subscribed Composable recomposes.
 *
 * Built-in themes are lazy-loaded the first time a user (or app startup)
 * calls [setActive] or [loadBuiltin] for them — we never eagerly parse
 * every bundled JSON.
 */
object ThemeRegistry {

    /**
     * The default theme used until [setActive] is called. Defined as a
     * minimal hardcoded Theme rather than parsing JSON at class-init so
     * commonMain stays platform-agnostic and the registry never crashes
     * before [setActive] runs.
     *
     * Once the app shell wires up `LaunchedEffect { ThemeRegistry.setActive("Yole Dark") }`
     * at startup, this default is supplanted with the bundled JSON theme.
     */
    private val bootstrapTheme = Theme(
        name = "Yole Dark (bootstrap)",
        type = "dark",
        uiColors = LegacyThemeBridge.legacyDark,
        tokenColors = emptyMap(),
    )

    private val _activeTheme: MutableStateFlow<Theme> = MutableStateFlow(bootstrapTheme)

    /** The active theme, observed by every Composable beneath [ThemeProvider]. */
    val activeTheme: StateFlow<Theme> = _activeTheme.asStateFlow()

    /**
     * Hardcoded list of bundled theme names. iter-57 ships two built-ins;
     * later iterations append Dracula, Solarized, One Dark, etc.
     */
    private val builtinFilenames: Map<String, String> = mapOf(
        "Yole Light" to "Yole-Light.json",
        "Yole Dark" to "Yole-Dark.json",
    )

    /** Names of all bundled themes available via [setActive] / [loadBuiltin]. */
    fun available(): List<String> = builtinFilenames.keys.toList()

    /**
     * Load and parse a bundled theme by its user-facing name.
     * Does NOT mutate the active theme — use [setActive] for that.
     *
     * @param name one of [available]; case-sensitive.
     * @throws IllegalArgumentException if [name] is not a known built-in.
     * @throws ThemeParseException if the JSON is malformed.
     * @throws IllegalStateException if the JSON resource is unavailable on
     *         this platform (Phase 6/7 stubs).
     */
    fun loadBuiltin(name: String): Theme {
        val filename = builtinFilenames[name]
            ?: throw IllegalArgumentException(
                "Unknown built-in theme `$name`. Available: ${available()}"
            )
        val json = readBuiltinTheme(filename)
        return VsCodeThemeParser.parse(json)
    }

    /**
     * Load the named built-in theme and publish it as the active theme.
     * Every Composable beneath [ThemeProvider] recomposes with the new colors.
     *
     * @param name one of [available].
     * @throws IllegalArgumentException if [name] is not a known built-in.
     */
    fun setActive(name: String) {
        _activeTheme.value = loadBuiltin(name)
    }

    /**
     * Directly publish a pre-parsed [Theme] as active. Used by tests and
     * advanced callers (e.g., user-imported themes from disk).
     */
    fun setActive(theme: Theme) {
        _activeTheme.value = theme
    }
}
