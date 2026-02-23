/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Multiplatform synchronization primitive
 *
 *########################################################*/
package digital.vasic.yole.util

/**
 * Multiplatform synchronized block.
 * On JVM/Native: delegates to platform synchronized.
 * On JS/Wasm (single-threaded): executes block directly.
 */
expect inline fun <R> platformSynchronized(lock: Any, block: () -> R): R
