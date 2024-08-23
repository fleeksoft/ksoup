package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

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
public fun Ksoup.parseGetRequestBlocking(
    url: String,
    parser: Parser = Parser.htmlParser(),
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document = runBlocking {
    val httpResponse = NetworkHelperKtor.instance.get(url, httpRequestBuilder = httpRequestBuilder)
//        url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val response = httpResponse.bodyAsText()
    return@runBlocking parse(html = response, parser = parser, baseUri = finalUrl)
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
public fun Ksoup.parseSubmitRequestBlocking(
    url: String,
    params: Map<String, String> = emptyMap(),
    parser: Parser = Parser.htmlParser(),
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document = runBlocking {
    val httpResponse =
        NetworkHelperKtor.instance.submitForm(
            url = url,
            params = params,
            httpRequestBuilder = httpRequestBuilder,
        )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val result: String = httpResponse.bodyAsText()
    return@runBlocking parse(html = result, parser = parser, baseUri = finalUrl)
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
public fun Ksoup.parsePostRequestBlocking(
    url: String,
    parser: Parser = Parser.htmlParser(),
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
): Document = runBlocking {
    val httpResponse =
        NetworkHelperKtor.instance.post(
            url = url,
            httpRequestBuilder = httpRequestBuilder,
        )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val result: String = httpResponse.bodyAsText()
    return@runBlocking parse(html = result, parser = parser, baseUri = finalUrl)
}
