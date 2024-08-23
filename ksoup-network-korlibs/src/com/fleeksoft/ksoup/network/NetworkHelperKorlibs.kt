package com.fleeksoft.ksoup.network

import korlibs.io.net.http.*

public class NetworkHelperKorlibs {
    private val client: HttpClient = createHttpClient()

    public companion object {
        public val instance: NetworkHelperKorlibs = NetworkHelperKorlibs()
    }

    public suspend fun get(
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

    public suspend fun submitForm(
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

    public suspend fun post(
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
