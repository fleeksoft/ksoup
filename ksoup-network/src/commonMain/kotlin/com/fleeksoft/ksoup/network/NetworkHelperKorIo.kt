package com.fleeksoft.ksoup.network

import korlibs.io.net.http.*

internal class NetworkHelperKorIo {
    private val client: HttpClient = createHttpClient()

    companion object {
        val instance: NetworkHelperKorIo = NetworkHelperKorIo()
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = mapOf(),
        requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    ): HttpClient.Response {
        val headersBuild =
            Http.Headers.build {
                headers.forEach {
                    this.put(it.key, it.value)
                }
            }
        return client.request(url = url, method = Http.Methods.GET, headers = headersBuild, config = requestConfig)
    }

    suspend fun submitForm(
        url: String,
        params: Map<String, String>,
        headers: Map<String, String> = mapOf(),
        requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    ): HttpClient.Response {
        val headersBuild =
            Http.Headers.build {
                headers.forEach {
                    this.put(it.key, it.value)
                }
            }
        return client.post(
            url = url,
            HttpBodyContentFormUrlEncoded(params.map { it.toPair() }),
            headers = headersBuild,
            config = requestConfig,
        )
    }

    suspend fun post(
        url: String,
        body: HttpBodyContent = HttpBodyContent("", ""),
        headers: Map<String, String> = mapOf(),
        requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    ): HttpClient.Response {
        val headersBuild =
            Http.Headers.build {
                headers.forEach {
                    this.put(it.key, it.value)
                }
            }
        return client.post(url = url, data = body, headers = headersBuild, config = requestConfig)
    }
}
