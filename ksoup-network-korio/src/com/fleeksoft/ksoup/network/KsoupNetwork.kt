package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.async.CIO
import korlibs.io.net.http.HttpBodyContent
import korlibs.io.net.http.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document = withContext(Dispatchers.CIO) {
    val httpResponse = NetworkHelperKorIo.instance.get(
        url,
        headers = headers,
        requestConfig = requestConfig
    )
//        url can be changed after redirection
    val finalUrl = httpResponse.headers["location"] ?: url
    val response = httpResponse.readAllString()
    return@withContext parse(html = response, parser = parser, baseUri = finalUrl)
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
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document = withContext(Dispatchers.CIO) {
    val httpResponse = NetworkHelperKorIo.instance.submitForm(
        url = url,
        params = params,
        headers = headers,
        requestConfig = requestConfig,
    )
//            url can be changed after redirection
    val finalUrl = httpResponse.headers["location"] ?: url
    val response = httpResponse.readAllString()
    return@withContext parse(html = response, parser = parser, baseUri = finalUrl)
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
    body: HttpBodyContent = HttpBodyContent("", ""),
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document = withContext(Dispatchers.CIO) {
    val httpResponse = NetworkHelperKorIo.instance.post(
        url = url,
        body = body,
        headers = headers,
        requestConfig = requestConfig
    )
//            url can be changed after redirection
    val finalUrl = httpResponse.headers["location"] ?: url
    val response = httpResponse.readAllString()
    return@withContext parse(html = response, parser = parser, baseUri = finalUrl)
}
