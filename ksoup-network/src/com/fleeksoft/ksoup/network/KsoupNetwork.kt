package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.io.from
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
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
    httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    parser: Parser = Parser.htmlParser(),
): Document {
    val httpResponse = NetworkHelperKtor.instance.get(url, httpRequestBuilder = httpRequestBuilder)
//        url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val sourceReader = SourceReader.from(httpResponse.bodyAsChannel())
    return parse(sourceReader = sourceReader, parser = parser, baseUri = finalUrl)
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
        NetworkHelperKtor.instance.submitForm(
            url = url,
            params = params,
            httpRequestBuilder = httpRequestBuilder,
        )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val sourceReader = SourceReader.from(httpResponse.bodyAsChannel())
    return parse(sourceReader = sourceReader, parser = parser, baseUri = finalUrl)
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
    val httpResponse = NetworkHelperKtor.instance.post(
        url = url,
        httpRequestBuilder = httpRequestBuilder,
    )
//            url can be changed after redirection
    val finalUrl = httpResponse.request.url.toString()
    val sourceReader = SourceReader.from(httpResponse.bodyAsChannel())
    return parse(sourceReader = sourceReader, parser = parser, baseUri = finalUrl)
}
