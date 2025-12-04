/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * Migrated to Kotlin 2025 by Milos Vasic
 *
 * To the extent possible under law, the author(s) have dedicated all copyright
 * and related and neighboring rights to this software to the public domain worldwide.
 * This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software.
 * If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 #########################################################*/
package digital.vasic.opoc.wrapper

import android.view.View
import android.widget.AdapterView

/**
 * Simple adapter for handling Spinner item selection events.
 * Calls the provided callback with the selected position, or -1 if nothing is selected.
 *
 * Example usage:
 * ```kotlin
 * spinner.onItemSelectedListener = GsAndroidSpinnerOnItemSelectedAdapter { position ->
 *     // Handle selection
 * }
 * ```
 */
class GsAndroidSpinnerOnItemSelectedAdapter(
    private val callback: GsCallback.a1<Int>
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        callback.callback(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        callback.callback(-1)
    }
}
