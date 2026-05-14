/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3: Wasm/JS actual stub for `readBuiltinTheme`.
 *
 * The Wasm browser path needs an async `fetch()` against a static asset URL
 * served from `/themes/builtin/<filename>.json`. That plumbing lands as part
 * of Phase 6 (vscode-textmate Wasm engine + Wasm asset bundling).
 * Until then this stub throws — Wasm callers MUST use the in-memory
 * `ThemeRegistry.setActive(theme: Theme)` overload to seed a theme.
 * The bootstrap default in [ThemeRegistry.activeTheme] guarantees the
 * Wasm app never crashes for absence of a theme.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * Wasm/JS stub: throws [IllegalStateException] until Phase 6 wires
 * fetch-based asset loading. Documented in spec §3.8 and plan Phase 6.
 *
 * @suppress until Phase 6.
 */
actual fun readBuiltinTheme(filename: String): String {
    error(
        "readBuiltinTheme is not yet wired on Wasm/JS (filename=`$filename`). " +
            "Phase 6 (vscode-textmate Wasm engine + asset bundling) implements " +
            "fetch-based loading. Until then, seed ThemeRegistry via " +
            "setActive(theme: Theme)."
    )
}
