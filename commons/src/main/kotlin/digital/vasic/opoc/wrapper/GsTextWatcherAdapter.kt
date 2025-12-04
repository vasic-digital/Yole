/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.wrapper

import android.text.Editable
import android.text.TextWatcher

/**
 * Adapter class for Android's TextWatcher interface with no-op implementations.
 * Provides convenient factory methods for creating TextWatchers with lambdas.
 *
 * Example usage:
 * ```kotlin
 * editText.addTextChangedListener(GsTextWatcherAdapter.after { editable ->
 *     // Handle text changed
 * })
 * ```
 */
@Suppress("unused")
open class GsTextWatcherAdapter : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // No-op by default, override if needed
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // No-op by default, override if needed
    }

    override fun afterTextChanged(s: Editable?) {
        // No-op by default, override if needed
    }

    companion object {
        /**
         * Create a TextWatcher that only responds to beforeTextChanged events.
         */
        @JvmStatic
        fun before(impl: GsCallback.a4<CharSequence, Int, Int, Int>): TextWatcher {
            return object : GsTextWatcherAdapter() {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    s?.let { impl.callback(it, start, count, after) }
                }
            }
        }

        /**
         * Create a TextWatcher that only responds to onTextChanged events.
         */
        @JvmStatic
        fun on(impl: GsCallback.a4<CharSequence, Int, Int, Int>): TextWatcher {
            return object : GsTextWatcherAdapter() {
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let { impl.callback(it, start, before, count) }
                }
            }
        }

        /**
         * Create a TextWatcher that only responds to afterTextChanged events.
         */
        @JvmStatic
        fun after(impl: GsCallback.a1<Editable>): TextWatcher {
            return object : GsTextWatcherAdapter() {
                override fun afterTextChanged(s: Editable?) {
                    s?.let { impl.callback(it) }
                }
            }
        }
    }
}
