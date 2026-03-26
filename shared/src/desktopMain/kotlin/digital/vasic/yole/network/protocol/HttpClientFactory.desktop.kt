package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        // Common HTTP client configuration
        followRedirects = true

        install(HttpTimeout) {
            connectTimeoutMillis = 10_000L
            requestTimeoutMillis = 30_000L
            socketTimeoutMillis = 30_000L
        }
    }
}