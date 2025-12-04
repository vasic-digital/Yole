/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
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
package digital.vasic.opoc.web

import android.webkit.WebView
import android.webkit.WebViewClient
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebViewClient with scroll position restoration support.
 *
 * Extends the standard WebViewClient to add functionality for restoring
 * vertical scroll position after page loads. This is useful when reloading
 * content and wanting to maintain the user's scroll position.
 *
 * Example usage:
 * ```kotlin
 * val webView = WebView(context)
 * val client = GsWebViewClient(webView)
 * client.setRestoreScrollY(webView.scrollY)
 * webView.webViewClient = client
 * webView.reload()
 * ```
 */
@Suppress("unused")
open class GsWebViewClient(webView: WebView) : WebViewClient() {

    protected val webViewRef: WeakReference<WebView> = WeakReference(webView)

    private val restoreScrollYEnabled = AtomicBoolean(false)
    private var restoreScrollY = 0

    override fun onPageFinished(webView: WebView, url: String) {
        onPageFinishedRestoreScrollY(webView, url)
        super.onPageFinished(webView, url)
    }

    /**
     * Internal method to handle scroll restoration after page finishes loading.
     * Activated by calling [setRestoreScrollY].
     *
     * @param webView The WebView that finished loading
     * @param url     The URL that was loaded
     */
    protected fun onPageFinishedRestoreScrollY(webView: WebView, url: String) {
        if (restoreScrollYEnabled.getAndSet(false)) {
            // Apply scroll position with progressive delays to handle dynamic content
            for (dt in intArrayOf(50, 100, 150, 200, 250, 300)) {
                webView.postDelayed({ webView.scrollY = restoreScrollY }, dt.toLong())
            }
        }
    }

    /**
     * Apply vertical scroll position on next page load.
     * Call this before reloading the WebView to restore scroll position.
     *
     * @param scrollY Scroll position from [WebView.getScrollY]
     */
    fun setRestoreScrollY(scrollY: Int) {
        restoreScrollY = scrollY
        restoreScrollYEnabled.set(scrollY >= 0)
    }
}
