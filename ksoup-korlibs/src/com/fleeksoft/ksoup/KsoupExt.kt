package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.io.asInputStream
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.file.VfsFile
import korlibs.io.file.std.uniVfs
import korlibs.io.stream.SyncStream


/**
 * Parse the contents of a file as HTML.
 *
 * @param filePath file to load HTML from. Supports gzipped files (ending in .z or .gz).
 * @param baseUri The URL where the HTML was retrieved from, to resolve relative links against.
 * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
 * present, or fall back to `UTF-8` (which is often safe to do).
 * @param parser alternate [parser][Parser.xmlParser] to use.
 * @return sane HTML
 */
@Deprecated("Korlibs support is deprecated; use the ksoup-kotlinx variant instead.")
public suspend fun Ksoup.parseFile(
    filePath: String,
    baseUri: String = filePath,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parseFile(
        file = filePath.uniVfs,
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}

@Deprecated("Korlibs support is deprecated; use the ksoup-kotlinx variant instead.")
public suspend fun Ksoup.parseFile(
    file: VfsFile,
    baseUri: String = file.absolutePath,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parseInput(
        input = file.inputStream(),
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}

@Deprecated("Korlibs support is deprecated; use the ksoup-kotlinx variant instead.")
public fun Ksoup.parseStream(
    stream: SyncStream,
    baseUri: String = "",
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return DataUtil.load(
        input = stream.asInputStream(),
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}


/*
public fun Ksoup.parseStream(
    stream: AsyncStream,
    baseUri: String = "",
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return DataUtil.load(
        input = stream.asInputStream(),
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}*/
