package digital.vasic.yole.network.protocol

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*

actual fun createHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        // Common HTTP client configuration
        followRedirects = true
    }
}