/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4: thrown by EnabledFormatGate.requireEnabled when a
 * caller attempts to load a grammar or invoke a parser for a format that
 * the user has not enabled in Settings → Formats. See spec §3.7.
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * Thrown when a caller attempts to operate on a format that is not currently
 * enabled in [EnabledFormatGate]. The [formatId] property identifies which
 * format was rejected so the caller can surface a meaningful error to the
 * user or route to a fallback (e.g., plain-text rendering).
 */
class FormatDisabledException(val formatId: String) :
    RuntimeException("format `$formatId` is not enabled in Settings → Formats")
