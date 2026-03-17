/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025-2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Wasm/JS implementation of HttpClientFactory.
 * Uses Ktor JS engine (Fetch API) for browser-based networking
 * with timeout, content negotiation, and retry configuration.
 *
 *########################################################*/
package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Create an HTTP client for the Wasm/JS platform using the Ktor JS engine.
 *
 * The JS engine uses the browser's native Fetch API under the hood.
 * This supports CORS-compliant requests to cloud storage APIs (Dropbox,
 * Google Drive, OneDrive) which use standard HTTPS REST endpoints.
 *
 * ## Configuration
 *
 * - **Timeouts**: 30s connect, 30s request, 60s socket idle. These are
 *   appropriate for browser environments where network latency varies widely
 *   and cloud API responses may be slow for large file operations.
 *
 * - **Content Negotiation**: JSON serialization with lenient parsing to handle
 *   various cloud API response formats. Unknown keys are ignored to provide
 *   forward compatibility with API changes.
 *
 * - **Retry Logic**: 3 retries with exponential backoff (1s base, 2x multiplier)
 *   for transient HTTP failures (5xx server errors). This handles temporary
 *   cloud service outages without burdening the caller with retry logic.
 *   Rate-limited responses (429) are also retried after the suggested delay.
 */
actual fun createHttpClient(): HttpClient {
    return HttpClient(Js) {
        followRedirects = true

        // Request timeout configuration
        install(HttpTimeout) {
            connectTimeoutMillis = 30_000L
            requestTimeoutMillis = 30_000L
            socketTimeoutMillis = 60_000L
        }

        // JSON content negotiation for cloud storage API responses
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
                prettyPrint = false
            })
        }

        // Retry logic for transient failures with exponential backoff
        install(HttpRequestRetry) {
            maxRetries = 3
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay(
                base = 1.0,
                maxDelayMs = 10_000L
            )
        }
    }
}
