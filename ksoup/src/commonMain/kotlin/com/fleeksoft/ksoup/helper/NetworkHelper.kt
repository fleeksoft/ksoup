package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.provideHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.parameters

internal class NetworkHelper(private val client: HttpClient) {

    companion object {
        val instance: NetworkHelper = NetworkHelper(HttpClient(provideHttpClientEngine()))
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        return client.get(url) {
            headers {
                headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
    }

    suspend fun <T : Any> post(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        return client.submitForm(url = url, formParameters = parameters {
            params.forEach { (key, value) ->
                append(key, value)
            }
        }) {
            headers {
                headers.forEach { (key, value) ->
                    append(key, value)
                }
            }
        }
    }
}