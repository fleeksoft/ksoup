package com.fleeksoft.ksoup.kotlinx.helper

import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.kotlinx.openStream
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import kotlinx.io.files.Path

/**
 * Loads and parses a file to a Document, with the HtmlParser. Files that are compressed with gzip (and end in `.gz` or `.z`)
 * are supported in addition to uncompressed files.
 *
 * @param filePath file to load
 * @param baseUri base URI of document, to resolve relative links against
 * @param charsetName (optional) character set of input; specify `null` to attempt to autodetect. A BOM in
 * the file will always override this setting.
 * @return Document
 */
public suspend fun DataUtil.load(
    filePath: String,
    baseUri: String,
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return DataUtil.load(file = Path(filePath), charsetName = charsetName, baseUri = baseUri, parser = parser)
}

/**
 * Loads and parses a file to a Document. Files that are compressed with gzip (and end in `.gz` or `.z`)
 * are supported in addition to uncompressed files.
 *
 * @param file file to load
 * @param baseUri base URI of document, to resolve relative links against
 * @param charsetName (optional) character set of input; specify `null` to attempt to autodetect. A BOM in
 * the file will always override this setting.
 * @param parser alternate [parser][Parser.xmlParser] to use.
 *
 * @return Document
 */
public suspend fun DataUtil.load(
    file: Path,
    baseUri: String = file.toString(),
    charsetName: String? = null,
    parser: Parser = Parser.htmlParser(),
): Document {
    return parseInputSource(
        sourceReader = file.openStream(),
        baseUri = baseUri,
        charsetName = charsetName,
        parser = parser
    )
}
