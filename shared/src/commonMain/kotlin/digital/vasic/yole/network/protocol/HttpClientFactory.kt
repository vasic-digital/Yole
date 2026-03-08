/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 *########################################################*/
package digital.vasic.yole.network.protocol

import io.ktor.client.*

/**
 * Platform-specific HTTP client factory for network protocols.
 */
expect fun createHttpClient(): HttpClient