/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TodoTxt Parser - WebAssembly Implementation
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import kotlinx.datetime.*

/**
 * Get current date in YYYY-MM-DD format (WebAssembly)
 */
actual fun getCurrentDate(): String {
    val now = Clock.System.now()
    val date = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return date.toString()
}