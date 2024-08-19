package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.openStream
import com.fleeksoft.ksoup.parser.Parser
import korlibs.io.file.VfsFile
import korlibs.io.file.std.uniVfs

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
    return DataUtil.load(file = filePath.uniVfs, charsetName = charsetName, baseUri = baseUri, parser = parser)
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
    file: VfsFile,
    baseUri: String = file.absolutePath,
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
