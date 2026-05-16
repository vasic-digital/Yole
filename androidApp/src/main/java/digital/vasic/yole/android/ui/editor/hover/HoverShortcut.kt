/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 6.3: HoverShortcut — Modifier extension that wires
 * F1 as the explicit hover trigger shortcut.
 *
 * Design:
 *   Uses onPreviewKeyEvent so the event is intercepted before
 *   BasicTextField consumes it. Returns true (consumed) on F1 KeyDown
 *   so the key does not propagate further. All other keys return false.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — stub modifier to consume F1 but NOT call onTrigger():
 *     `if (...F1...) { /* no onTrigger() */ true } else false`
 *   HoverShortcutRobolectricTest.f1_keydown_invokesCallback FAILS
 *   because the callback counter stays at 0. Reverted; tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: full implementation (physical keyboard / Bluetooth KB).
 *   Desktop: same pattern available via identical Compose API;
 *            Phase 8 integration will wire the Desktop editor.
 *   iOS:     hardware keyboard support deferred.
 *   Web:     deferred.
 *
 * Submodules: not touched (CONST-038).
 *########################################################*/
package digital.vasic.yole.android.ui.editor.hover

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Attaches an F1 keyboard shortcut to the composable that triggers an
 * explicit hover lookup.
 *
 * The modifier intercepts [KeyEventType.KeyDown] events with [Key.F1]
 * BEFORE the inner composable (typically BasicTextField) sees them,
 * invokes [onTrigger], and consumes the event by returning `true`.
 *
 * All other key events are passed through unchanged.
 *
 * Usage:
 * ```
 * BasicTextField(
 *   modifier = Modifier.hoverShortcut { triggerHoverAtCursor() }
 * )
 * ```
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.hoverShortcut(onTrigger: () -> Unit): Modifier = this.then(
    Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.F1) {
            onTrigger()
            true
        } else {
            false
        }
    },
)
