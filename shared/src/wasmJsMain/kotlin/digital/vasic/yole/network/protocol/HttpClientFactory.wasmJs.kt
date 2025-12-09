package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.js.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(Js) {
        // Common HTTP client configuration
        followRedirects = true
    }
}