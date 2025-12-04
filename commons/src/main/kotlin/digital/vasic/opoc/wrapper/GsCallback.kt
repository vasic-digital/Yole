/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.wrapper

/**
 * Collection of functional interface types for callbacks.
 * These provide Java-compatible functional interfaces while being usable as
 * Kotlin function types.
 */
@Suppress("unused")
object GsCallback {

    // Action callbacks (void return)
    fun interface a0 {
        fun callback()
    }

    fun interface a1<A> {
        fun callback(arg1: A)
    }

    fun interface a2<A, B> {
        fun callback(arg1: A, arg2: B)
    }

    fun interface a3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C)
    }

    fun interface a4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D)
    }

    fun interface a5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E)
    }

    // Boolean return callbacks
    fun interface b0 {
        fun callback(): Boolean
    }

    fun interface b1<A> {
        fun callback(arg1: A): Boolean
    }

    fun interface b2<A, B> {
        fun callback(arg1: A, arg2: B): Boolean
    }

    fun interface b3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C): Boolean
    }

    fun interface b4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D): Boolean
    }

    fun interface b5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E): Boolean
    }

    // String return callbacks
    fun interface s0 {
        fun callback(): String
    }

    fun interface s1<A> {
        fun callback(arg1: A): String
    }

    fun interface s2<A, B> {
        fun callback(arg1: A, arg2: B): String
    }

    fun interface s3<A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C): String
    }

    fun interface s4<A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D): String
    }

    fun interface s5<A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E): String
    }

    // Generic return callbacks
    fun interface r0<R> {
        fun callback(): R
    }

    fun interface r1<R, A> {
        fun callback(arg1: A): R
    }

    fun interface r2<R, A, B> {
        fun callback(arg1: A, arg2: B): R
    }

    fun interface r3<R, A, B, C> {
        fun callback(arg1: A, arg2: B, arg3: C): R
    }

    fun interface r4<R, A, B, C, D> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D): R
    }

    fun interface r5<R, A, B, C, D, E> {
        fun callback(arg1: A, arg2: B, arg3: C, arg4: D, arg5: E): R
    }
}
