package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parseInput
import com.fleeksoft.ksoup.parser.Parser
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

/**
 * Use to fetch and parse a HTML page.
 *
 * Use examples:
 *
 *  * `Document doc = Ksoup.parseGetRequest("http://example.com")`
 *
 * @param url URL to connect to. The protocol must be `http` or `https`.
 * @return sane HTML
 *
 */
public suspend fun Ksoup.parseGetRequest(
    url: String,
    parser: Parser = Parser.htmlParser(),
    httpClient: HttpClient? = null,
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document {
    val client = httpClient ?: HttpClient(provideHttpClientEngine())
    val httpResponse = NetworkHelperKtor2.get(url, httpRequestBuilder = httpRequestBuilder, client = client)
//        url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    return Ksoup.parseInput(input = httpResponse.asInputStream(), parser = parser, baseUri = finalUrl)
}

/**
 * Use to fetch and parse a HTML page.
 *
 * Use examples:
 *
 *  * `Document doc = Ksoup.parseSubmitRequest("http://example.com", params = mapOf("param1Key" to "param1Value"))`
 *
 * @param url URL to connect to. The protocol must be `http` or `https`.
 * @return sane HTML
 *
 */
public suspend fun Ksoup.parseSubmitRequest(
    url: String,
    params: Map<String, String> = emptyMap(),
    parser: Parser = Parser.htmlParser(),
    httpClient: HttpClient? = null,
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document {
    val client = httpClient ?: HttpClient(provideHttpClientEngine())
    val httpResponse = NetworkHelperKtor2.submitForm(
        url = url,
        params = params,
        httpRequestBuilder = httpRequestBuilder,
        client = client
    )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    return Ksoup.parseInput(input = httpResponse.asInputStream(), parser = parser, baseUri = finalUrl)
}

/**
 * Use to fetch and parse a HTML page.
 *
 * Use examples:
 *
 *  * `Document doc = Ksoup.parsePostRequest("http://example.com")`
 *
 * @param url URL to connect to. The protocol must be `http` or `https`.
 * @return sane HTML
 *
 */
public suspend fun Ksoup.parsePostRequest(
    url: String,
    parser: Parser = Parser.htmlParser(),
    httpClient: HttpClient? = null,
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document {
    val client = httpClient ?: HttpClient(provideHttpClientEngine())
    val httpResponse = NetworkHelperKtor2.post(
        url = url,
        httpRequestBuilder = httpRequestBuilder,
        client = client
    )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    return Ksoup.parseInput(input = httpResponse.asInputStream(), parser = parser, baseUri = finalUrl)
}
