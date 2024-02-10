package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import korlibs.io.net.http.HttpClient

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
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    parser: Parser = Parser.htmlParser(),
): Document {
    val httpResponse = NetworkHelper.instance.get(url, httpRequestBuilder = httpRequestBuilder)
//        url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val response = httpResponse.bodyAsText()
    return parse(html = response, parser = parser, baseUri = finalUrl)
}

public suspend fun Ksoup.parseGetRequestKorio(
    url: String,
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    parser: Parser = Parser.htmlParser(),
): Document {
    val httpResponse = NetworkHelperKorio.instance.get(url, requestConfig = HttpClient.RequestConfig.DEFAULT.copy())
//        url can be changed after redirection
    val finalUrl = httpResponse.headers["location"] ?: url
    val response = httpResponse.readAllString()
    return parse(html = response, parser = parser, baseUri = finalUrl)
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
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    parser: Parser = Parser.htmlParser(),
): Document {
    val httpResponse =
        NetworkHelper.instance.submitForm(
            url = url,
            params = params,
            httpRequestBuilder = httpRequestBuilder,
        )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val result: String = httpResponse.bodyAsText()
    return parse(html = result, parser = parser, baseUri = finalUrl)
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
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    parser: Parser = Parser.htmlParser(),
): Document {
    val httpResponse =
        NetworkHelper.instance.post(
            url = url,
            httpRequestBuilder = httpRequestBuilder,
        )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val result: String = httpResponse.bodyAsText()
    return parse(html = result, parser = parser, baseUri = finalUrl)
}
