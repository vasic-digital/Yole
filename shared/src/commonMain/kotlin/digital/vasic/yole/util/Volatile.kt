/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Cross-platform volatile annotation for Kotlin Multiplatform
 *########################################################*/
package digital.vasic.yole.util

/**
 * Annotation for volatile field access.
 *
 * On JVM targets, use [kotlin.jvm.Volatile] directly in platform code.
 * This annotation provides a cross-platform placeholder that compiles
 * on all targets but has no effect on non-JVM platforms.
 *
 * @see kotlin.jvm.Volatile
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Volatile
