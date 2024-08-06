package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.stream.openSync
import java.io.File
import java.io.InputStream

/**
 * Parse the contents of a file as HTML.
 *
 * @param inputStream  input stream to read.
 * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
 * present, or fall back to `UTF-8` (which is often safe to do).
 * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
 * @return sane HTML
 */

public fun Ksoup.parseInputStream(
    inputStream: InputStream,
    baseUri: String,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parse(
        // TODO: use syncstream without reading bytes
        syncStream = inputStream.readAllBytes().openSync(),
        charsetName = charsetName,
        baseUri = baseUri,
        parser = parser,
    )
}


/**
 * Parse the contents of a file as HTML.
 *
 * @param file file to read.
 * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
 * present, or fall back to `UTF-8` (which is often safe to do).
 * @param baseUri  The URL where the HTML was retrieved from, to resolve relative links against.
 * @return sane HTML
 */
public fun Ksoup.parseFile(
    file: File,
    charsetName: String? = null,
    baseUri: String = file.absolutePath,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parse(
        syncStream = file.openSync(),
        charsetName = charsetName,
        baseUri = baseUri,
        parser = parser,
    )
}
