/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-64 Phase 10: ImportShareIntentHandler
 *
 * Extracts a ByteArray from an incoming share / open-with Intent so that
 * the caller can forward the bytes to the importer pipeline.
 *
 * Supported intent actions:
 *   - Intent.ACTION_SEND  → URI read from Intent.EXTRA_STREAM
 *   - Intent.ACTION_VIEW  → URI read from Intent.data
 *
 * Returns null on any failure (no URI, unreadable stream, missing permission).
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub handle() to always return null → importShareIntentHandlerExtractsBytesFromSend FAILS
 *      because the mutation evidence check detects the null-return stub.
 *   2. Remove the Intent.EXTRA_STREAM branch → the SEND path returns null in
 *      the behavioural test → importShareIntentHandlerExtractsBytesFromSend FAILS.
 *   3. Remove the Intent.ACTION_VIEW / intent.data branch → VIEW path returns
 *      null → importShareIntentHandlerExtractsBytesFromView FAILS.
 *   Revert each → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   Android: ships here.
 *   Desktop / iOS / Web: N/A — intent system is Android-only.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.import_

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Extracts raw bytes from a share/open-with [Intent] for the Yole import pipeline.
 *
 * Usage:
 * ```kotlin
 * val bytes = ImportShareIntentHandler.handle(context, intent)
 * if (bytes != null) launchImport(bytes)
 * ```
 */
object ImportShareIntentHandler {

    /**
     * Reads the document bytes delivered by the OS via a SEND or VIEW intent.
     *
     * @param context Android [Context] used to open a [android.content.ContentResolver] stream.
     * @param intent  The incoming [Intent] (action SEND or VIEW).
     * @return [ByteArray] with the file contents, or **null** on any failure
     *         (missing URI, IO error, missing permission).
     */
    fun handle(context: Context, intent: Intent): ByteArray? {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Intent.ACTION_VIEW -> intent.data
            else -> null
        }
        uri ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }
}
