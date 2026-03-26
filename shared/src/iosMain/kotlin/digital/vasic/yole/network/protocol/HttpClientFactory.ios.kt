/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS implementation of HttpClientFactory
 * Uses Darwin engine for native iOS networking
 *
 *########################################################*/
package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        // Common HTTP client configuration
        followRedirects = true

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000L
            requestTimeoutMillis = 30_000L
            socketTimeoutMillis = 30_000L
        }
    }
}
