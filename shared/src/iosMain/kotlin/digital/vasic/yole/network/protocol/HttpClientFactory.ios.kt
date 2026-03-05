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

actual fun createHttpClient(): HttpClient {
    return HttpClient(Darwin) {
        // Common HTTP client configuration
        followRedirects = true
    }
}
