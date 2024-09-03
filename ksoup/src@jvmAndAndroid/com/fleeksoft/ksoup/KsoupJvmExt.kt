package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString


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
        sourceReader = inputStream.toSourceReader(),
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
public suspend fun Ksoup.parseFile(
    file: File,
    baseUri: String = file.absolutePath,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parseFile(
        file = file.toFileSource(),
        charsetName = charsetName,
        baseUri = baseUri,
        parser = parser,
    )
}


/**
 * Parse the contents of a file as HTML.
 *
 * @param path file to load HTML from. Supports gzipped files (ending in .z or .gz).
 * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
 * present, or fall back to `UTF-8` (which is often safe to do).
 * @param baseUri The URL where the HTML was retrieved from, to resolve relative links against.
 * @param parser alternate [parser][Parser.xmlParser] to use.
 * @return sane HTML
 * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
 */

suspend fun Ksoup.parsePath(
    path: Path,
    baseUri: String = path.absolutePathString(),
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser()
): Document {
    return parseFile(
        file = path.toFileSource(),
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}
