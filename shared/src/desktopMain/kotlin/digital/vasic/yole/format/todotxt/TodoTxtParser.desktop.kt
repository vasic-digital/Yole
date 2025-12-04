/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * TodoTxt Parser - Desktop Implementation
 *
 *########################################################*/
package digital.vasic.yole.format.todotxt

import java.text.SimpleDateFormat
import java.util.*

/**
 * Get current date in YYYY-MM-DD format (Desktop)
 */
actual fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    return dateFormat.format(Date())
}
