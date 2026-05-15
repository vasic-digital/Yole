/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 3: iOS actual stub for `readScmResource`.
 *
 * iOS bundle-resource access requires NSBundle wiring that lands as
 * part of iter-58 Phase 7 (per the plan) — same upstream blocker as
 * the iter-57 Phase 7 iOS TokenizerEngine actual
 * (#phase-7-blocked-on-ios-baseline / CONST-038 sibling-submodule
 * decoupling). Until then this stub throws `IllegalStateException`
 * per CONST-035 anti-bluff covenant — callers on iOS get an honest
 * "not bundled yet" error rather than a faked-empty result.
 *
 * See KNOWN_DEFECTS.md entry `#f2-phase-3-bonede-query-api-gap`.
 *#######################################################*/
package digital.vasic.yole.language

/**
 * iOS stub: throws [IllegalStateException] until iter-58 Phase 7 wires
 * NSBundle resource access for vendored `.scm` query files. Documented
 * in the iter-58 plan Phase 7 + KNOWN_DEFECTS.md.
 *
 * @suppress until iter-58 Phase 7.
 */
actual fun readScmResource(path: String): String {
    error(
        "readScmResource is not yet wired on iOS (path=`$path`). " +
            "iter-58 Phase 7 (NSBundle asset bundling for vendored .scm queries) " +
            "implements this actual. See KNOWN_DEFECTS.md " +
            "#f2-phase-3-bonede-query-api-gap.",
    )
}
