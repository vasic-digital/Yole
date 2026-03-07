/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Platform Sync Facade
 * Delegates to extracted Concurrency-KMP module
 *
 *########################################################*/
package digital.vasic.yole.util

inline fun <R> platformSynchronized(lock: Any, block: () -> R): R =
    digital.vasic.concurrency.platformSynchronized(lock, block)
