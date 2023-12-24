package com.fleeksoft.ksoup.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class NetworkHelper(private val client: HttpClient) {
    companion object {
        val instance: NetworkHelper =
            NetworkHelper(
                HttpClient(provideHttpClientEngine()) {
                    this.followRedirects = true
                },
            )
    }

    suspend fun get(
        url: String,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.get(url) {
            httpRequestBuilder()
        }
    }

    suspend fun submitForm(
        url: String,
        params: Map<String, String>,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.submitForm(
            url = url,
            formParameters =
                parameters {
                    params.forEach { (key, value) ->
                        append(key, value)
                    }
                },
        ) {
            httpRequestBuilder()
        }
    }

    suspend fun post(
        url: String,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.post(url) {
            httpRequestBuilder()
        }
    }
}
