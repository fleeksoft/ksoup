package com.fleeksoft.ksoup.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.core.use

/**
 * Helper class for making HTTP requests using Ktor client.
 */
object NetworkHelperKtor2 {

    /**
     * Performs an HTTP GET request
     *
     * @param url The URL to request
     * @param client Optional custom HTTP client (if null, uses the default client or creates a new one)
     * @param httpRequestBuilder Optional request configuration
     * @return HTTP response
     */
    public suspend fun get(
        url: String,
        client: HttpClient? = null,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return executeRequest(client) { httpClient ->
            httpClient.get(url) {
                httpRequestBuilder()
            }
        }
    }

    /**
     * Submits a form via HTTP POST
     *
     * @param url The URL to submit to
     * @param params Form parameters
     * @param client Optional custom HTTP client (if null, uses the default client or creates a new one)
     * @param httpRequestBuilder Optional request configuration
     * @return HTTP response
     */
    public suspend fun submitForm(
        url: String,
        params: Map<String, String>,
        client: HttpClient? = null,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return executeRequest(client) { httpClient ->
            httpClient.submitForm(
                url = url,
                formParameters = parameters {
                    params.forEach { (key, value) ->
                        append(key, value)
                    }
                },
            ) {
                httpRequestBuilder()
            }
        }
    }

    /**
     * Performs an HTTP POST request
     *
     * @param url The URL to request
     * @param client Optional custom HTTP client (if null, uses the default client or creates a new one)
     * @param httpRequestBuilder Optional request configuration
     * @return HTTP response
     */
    public suspend fun post(
        url: String,
        client: HttpClient? = null,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return executeRequest(client) { httpClient ->
            httpClient.post(url) {
                httpRequestBuilder()
            }
        }
    }

    /**
     * Helper method to execute a request with proper client handling
     *
     * @param providedClient Optional user-provided client
     * @param requestBlock The actual request execution
     * @return HTTP response
     */
    private suspend fun <T> executeRequest(
        providedClient: HttpClient?,
        requestBlock: suspend (HttpClient) -> T
    ): T {
        // Use provided client, or create a new one
        val clientToUse = providedClient ?: HttpClient(provideHttpClientEngine())

        // If we're using a client that was created just for this request (not provided by user,
        // then close it after use
        return if (providedClient == null) {
            clientToUse.use { requestBlock(it) }
        } else {
            // Otherwise, just use the client without closing it
            requestBlock(clientToUse)
        }
    }
}
