/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: iOS actual stub for `readBuiltinTheme`.
 *
 * iOS bundle-resource access requires NSBundle wiring that lands as part
 * of Phase 7 (Tree-Sitter Kotlin/Native engine + iOS asset bundling).
 * Until then this stub throws — callers on iOS MUST seed the registry
 * via the in-memory `ThemeRegistry.setActive(theme)` overload, or supply
 * a hardcoded Theme. The bootstrap default in [ThemeRegistry.activeTheme]
 * guarantees the iOS app never crashes for absence of a theme.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * iOS stub: throws [IllegalStateException] until Phase 7 wires NSBundle
 * resource access. Documented in spec §3.8 and plan Phase 7.
 *
 * @suppress until Phase 7.
 */
actual fun readBuiltinTheme(filename: String): String {
    error(
        "readBuiltinTheme is not yet wired on iOS (filename=`$filename`). " +
            "Phase 7 (Tree-Sitter K/N + iOS asset bundling) implements NSBundle " +
            "loading. Until then, seed ThemeRegistry via setActive(theme: Theme)."
    )
}
