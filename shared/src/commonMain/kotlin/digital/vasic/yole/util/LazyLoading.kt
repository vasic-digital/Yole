/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Lazy Loading Facades
 * Typealiases bridging to extracted Concurrency-KMP module
 *
 *########################################################*/
package digital.vasic.yole.util

typealias LazyDocumentLoader<T> = digital.vasic.concurrency.LazyDocumentLoader<T>
typealias LazyStringLoader = digital.vasic.concurrency.LazyStringLoader
typealias FlowLazyLoader<T> = digital.vasic.concurrency.FlowLazyLoader<T>
