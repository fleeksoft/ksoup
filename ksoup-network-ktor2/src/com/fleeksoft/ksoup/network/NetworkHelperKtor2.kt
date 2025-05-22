package com.fleeksoft.ksoup.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

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
        client: HttpClient,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.get(url) {
            httpRequestBuilder()
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
        client: HttpClient,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.submitForm(
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
        client: HttpClient,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.post(url) {
            httpRequestBuilder()
        }
    }
}
