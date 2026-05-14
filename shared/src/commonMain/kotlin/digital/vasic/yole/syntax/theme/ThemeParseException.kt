/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57: thrown by VsCodeThemeParser when input JSON is
 * structurally invalid or missing required fields.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

class ThemeParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
