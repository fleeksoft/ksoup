package com.fleeksoft.ksoup.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.parameters
import io.ktor.utils.io.core.*

internal class NetworkHelper(private val client: HttpClient) {

    companion object {
        val instance: NetworkHelper = NetworkHelper(HttpClient(provideHttpClientEngine()) {
            this.followRedirects = true
            /*this.ResponseObserver {
                println("headers => ${it.headers}")
            }*/
        })
    }

    suspend fun get(
        url: String,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return client.get(url) {
            httpRequestBuilder()
        }
    }

    suspend fun submitForm(
        url: String,
        params: Map<String, String>,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return client.submitForm(url = url, formParameters = parameters {
            params.forEach { (key, value) ->
                append(key, value)
            }
        }) {
            httpRequestBuilder()
        }
    }


    suspend fun post(
        url: String, httpRequestBuilder: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return client.post(url) {
            httpRequestBuilder()
        }
    }
}