package com.fleeksoft.ksoup.network

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.net.http.HttpBodyContent
import korlibs.io.net.http.HttpClient
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
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document =
    runBlocking {
        val httpResponse = NetworkHelperKorIo.instance.get(url = url, headers = headers, requestConfig = requestConfig)
//        url can be changed after redirection
        val finalUrl = httpResponse.headers["location"] ?: url
        val response = httpResponse.readAllString()
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
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document =
    runBlocking {
        val httpResponse =
            NetworkHelperKorIo.instance.submitForm(
                url = url,
                params = params,
                headers = headers,
                requestConfig = requestConfig
            )
//            url can be changed after redirection
        val finalUrl = httpResponse.headers["location"] ?: url
        val result: String = httpResponse.readAllString()
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
    body: HttpBodyContent = HttpBodyContent("", ""),
    headers: Map<String, String> = emptyMap(),
    requestConfig: HttpClient.RequestConfig = HttpClient.RequestConfig.DEFAULT,
    parser: Parser = Parser.htmlParser(),
): Document =
    runBlocking {
        val httpResponse =
            NetworkHelperKorIo.instance.post(
                url = url,
                body = body,
                headers = headers,
                requestConfig = requestConfig
            )
//            url can be changed after redirection
        val finalUrl = httpResponse.headers["location"] ?: url
        val result: String = httpResponse.readAllString()
        return@runBlocking parse(html = result, parser = parser, baseUri = finalUrl)
    }
