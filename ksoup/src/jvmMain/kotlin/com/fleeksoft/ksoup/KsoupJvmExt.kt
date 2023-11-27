package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import okio.buffer
import okio.source
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

// TODO: create common module for android and jvm
public fun Ksoup.parse(
    inputStream: InputStream,
    baseUri: String,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser()
): Document {
    return this.parse(
        bufferReader = BufferReader(source = inputStream.source()),
        charsetName = charsetName,
        baseUri = baseUri,
        parser = parser
    )
}