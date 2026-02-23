/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS (Native) implementation of platform synchronization
 *
 *########################################################*/
package digital.vasic.yole.util

actual inline fun <R> platformSynchronized(lock: Any, block: () -> R): R =
    block()
