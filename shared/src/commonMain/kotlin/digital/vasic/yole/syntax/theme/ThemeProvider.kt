/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: ThemeProvider Composable + LocalTheme CompositionLocal.
 *
 * Wraps any subtree in the active VS Code [Theme] taken from
 * [ThemeRegistry.activeTheme]. Composables beneath this provider read the
 * current theme via `LocalTheme.current` or the convenience helpers
 * [themeUiColor] (UI `colors.*`) and [themeTokenColor] (`tokenColors[]`).
 *
 * Per spec §3.8: this is the single source of truth for every color in the
 * app — editor surface, status bar, drawer, dialogs, accents, gutter, syntax.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * CompositionLocal exposing the currently active [Theme].
 *
 * Reading this outside a [ThemeProvider] throws — every Composable subtree
 * that consumes theme colors MUST be wrapped at the root of the app shell.
 */
val LocalTheme = compositionLocalOf<Theme> {
    error(
        "LocalTheme accessed outside a ThemeProvider. " +
            "Wrap your root Composable in ThemeProvider { ... } " +
            "(see digital.vasic.yole.syntax.theme.ThemeProvider)."
    )
}

/**
 * Subscribes to [ThemeRegistry.activeTheme] and exposes the current [Theme]
 * to its [content] via [LocalTheme]. Recomposes on every theme change.
 *
 * Place at the root of every app shell (Android MainActivity, Desktop
 * application{}, iOS ComposeUIViewController, Wasm CanvasBasedWindow).
 */
@Composable
fun ThemeProvider(content: @Composable () -> Unit) {
    val theme by ThemeRegistry.activeTheme.collectAsState()
    CompositionLocalProvider(LocalTheme provides theme, content = content)
}

/**
 * Resolve a VS Code UI color key (`colors[...]`) from the active theme.
 *
 * @param key VS Code colors-section key (e.g. `"editor.background"`,
 *            `"statusBar.foreground"`).
 * @param fallback Color to return when the active theme has no entry for [key].
 *                 Defaults to [Color.Unspecified] — Compose treats that as
 *                 "use the parent / default" in most APIs.
 * @return ARGB Color, or [fallback] if missing.
 */
@Composable
fun themeUiColor(key: String, fallback: Color = Color.Unspecified): Color {
    val argb = LocalTheme.current.uiColor(key) ?: return fallback
    return Color(argb)
}

/**
 * Resolve a VS Code tokenColors scope from the active theme, with the
 * standard dot-separated hierarchical fallback (`foo.bar.baz` → `foo.bar` → `foo`).
 *
 * @param scope textmate scope (e.g. `"keyword.control.kotlin"`).
 * @param fallback Color to return when no entry matches.
 * @return ARGB Color, or [fallback] if missing.
 */
@Composable
fun themeTokenColor(scope: String, fallback: Color = Color.Unspecified): Color {
    val argb = LocalTheme.current.tokenColor(scope) ?: return fallback
    return Color(argb)
}
