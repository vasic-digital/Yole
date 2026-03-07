/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm/JS implementation of HttpClientFactory
 * Uses Ktor JS engine (Fetch API) for browser-based networking
 *
 *########################################################*/
package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.js.*

/**
 * Create an HTTP client for the Wasm/JS platform using the Ktor JS engine.
 *
 * The JS engine uses the browser's native Fetch API under the hood.
 * This supports CORS-compliant requests to cloud storage APIs (Dropbox,
 * Google Drive, OneDrive) which use standard HTTPS REST endpoints.
 *
 * TODO: Configure request timeouts appropriate for browser environment.
 * TODO: Add content negotiation plugins (JSON serialization) for API calls.
 * TODO: Consider adding retry logic for transient network failures.
 */
actual fun createHttpClient(): HttpClient {
    return HttpClient(Js) {
        followRedirects = true
    }
}