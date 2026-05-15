/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-58 Phase 1: Compose CompositionLocal for active language.
 *#######################################################*/
package digital.vasic.yole.language

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal carrying the active LanguageFormat for the current
 * editor surface. Mirrors iter-57's LocalTheme pattern. The editor
 * provides this via LanguageProvider; child Composables (CommentToggleAction,
 * BracketAutoCompleter, IndentEngine, OutlineDrawer, FoldGutter) read it.
 */
val LocalLanguage = compositionLocalOf<LanguageFormat?> { null }
