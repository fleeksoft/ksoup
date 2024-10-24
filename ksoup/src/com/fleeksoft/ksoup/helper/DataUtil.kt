package com.fleeksoft.ksoup.helper

import com.fleeksoft.charset.Charset
import com.fleeksoft.io.Reader
import com.fleeksoft.io.buffered
import com.fleeksoft.io.reader
import com.fleeksoft.ksoup.exception.IllegalCharsetNameException
import com.fleeksoft.ksoup.exception.UncheckedIOException
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.io.SourceReader
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.XmlDeclaration
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.StreamParser
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.ported.io.SourceInputStream
import com.fleeksoft.ksoup.ported.io.sourceInputStreamReader
import com.fleeksoft.ksoup.ported.isCharsetSupported
import com.fleeksoft.ksoup.select.Elements
import kotlin.random.Random

/**
 * Internal static utilities for handling data.
 */
public object DataUtil {
    private val charsetPattern: Regex = Regex("charset=\\s*['\"]?([^\\s,;'\"]*)", RegexOption.IGNORE_CASE)
    private val defaultCharsetName: String = Charsets.UTF8.name() // used if not found in header or meta charset
    private const val firstReadBufferSize: Long = (1024 * 5).toLong()
    private val mimeBoundaryChars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    public const val boundaryLength: Int = 32

    /**
     * Parses a Document from an input steam, using the provided Parser.
     * @param sourceReader buffer reader to parse. The stream will be closed after reading.
     * @param baseUri base URI of document, to resolve relative links against
     * @param charsetName character set of input (optional)
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return Document
     */
    public fun load(
        sourceReader: SourceReader,
        baseUri: String,
        charsetName: String? = null,
        parser: Parser = Parser.htmlParser(),
    ): Document {
        return parseInputSource(
            sourceReader = sourceReader,
            baseUri = baseUri,
            charsetName = charsetName,
            parser = parser
        )
    }

    /**
     * Returns a [StreamParser] that will parse the supplied file progressively.
     * Files that are compressed with gzip (and end in `.gz` or `.z`)
     * are supported in addition to uncompressed files.
     *
     * @param sourceReader buffer reader to parse. The stream will be closed after reading.
     * @param charset (optional) character set of input; specify `null` to attempt to autodetect from metadata.
     * A BOM in the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate [parser][Parser.xmlParser] to use.
     *
     * @return Document
     * @throws IOException on IO error
     * @see Connection.Response.streamParser
     */
    fun streamParser(sourceReader: SourceReader, baseUri: String, charset: Charset?, parser: Parser): StreamParser {
        val streamer = StreamParser(parser)
        val charsetName: String? = charset?.name()
        val charsetDoc: CharsetDoc = detectCharset(sourceReader, baseUri, charsetName, parser)
        val reader = SourceInputStream(source = charsetDoc.input).reader(charsetDoc.charset).buffered(SharedConstants.DEFAULT_BYTE_BUFFER_SIZE)
        streamer.parse(reader, baseUri) // initializes the parse and the document, but does not step() it

        return streamer
    }

    fun parseInputSource(sourceReader: SourceReader, baseUri: String, charsetName: String?, parser: Parser): Document {
        val doc: Document
        var charsetDoc: CharsetDoc? = null
        try {
            charsetDoc =
                detectCharset(inputSource = sourceReader, baseUri = baseUri, charsetName = charsetName, parser = parser)
            doc = parseInputSource(charsetDoc = charsetDoc, baseUri = baseUri, parser = parser)
        } finally {
            sourceReader.close()
        }
        return doc
    }

    /** A struct to return a detected charset, and a document (if fully read).  */
    data class CharsetDoc internal constructor(
        val charset: Charset,
        var doc: Document?,
        val input: SourceReader,
    )


    private fun detectCharset(inputSource: SourceReader, baseUri: String, charsetName: String?, parser: Parser): CharsetDoc {
        var effectiveCharsetName: String? = charsetName

        var doc: Document? = null

        // read the start of the stream and look for a BOM or meta charset:
        // look for BOM - overrides any other header or input
        val bomCharset = detectCharsetFromBom(inputSource) // resets / consumes appropriately
        if (bomCharset != null) effectiveCharsetName = bomCharset

        if (effectiveCharsetName == null) { // read ahead and determine from meta. safe first parse as UTF-8
            // @TODO:// implement it limit the max reading size
//            input.max(firstReadBufferSize)
            inputSource.mark(firstReadBufferSize)
            try {
                val reader: Reader = sourceInputStreamReader(inputSource, Charsets.UTF8)
                doc = parser.parseInput(reader, baseUri)
                inputSource.reset()
//                input.max(origMax); // reset for a full read if required // @TODO implement it
            } catch (e: UncheckedIOException) {
                throw e
            }
            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            val metaElements: Elements = doc.select("meta[http-equiv=content-type], meta[charset]")
            var foundCharset: String? = null // if not found, will keep utf-8 as best attempt
            for (meta in metaElements) {
                if (meta.hasAttr("http-equiv")) {
                    foundCharset = getCharsetFromContentType(meta.attr("content"))
                }
                if (foundCharset == null && meta.hasAttr("charset")) {
                    foundCharset = meta.attr("charset")
                }
                if (foundCharset != null) break
            }
            // look for <?xml encoding='ISO-8859-1'?>
            if (foundCharset == null && doc.childNodeSize() > 0) {
                val first: Node = doc.childNode(0)
                var decl: XmlDeclaration? = null
                if (first is XmlDeclaration) {
                    decl = first
                } else if (first is Comment) {
                    val comment: Comment = first
                    if (comment.isXmlDeclaration()) decl = comment.asXmlDeclaration()
                }
                if (decl?.name()?.equals("xml", ignoreCase = true) == true) {
                    foundCharset = decl.attr("encoding")
                }
            }
            foundCharset = validateCharset(foundCharset)
            if (foundCharset != null && !foundCharset.equals(defaultCharsetName, ignoreCase = true)) {
                // need to re-decode. (case insensitive check here to match how validate works)
                foundCharset = foundCharset.trim { it <= ' ' }.replace("[\"']".toRegex(), "")
                effectiveCharsetName = foundCharset
//                if can't change charset don't try other
                if (Charsets.isOnlyUtf8 && inputSource.exhausted()) {
                    inputSource.close()
                } else {
                    doc = null
                }
            } else if (inputSource.exhausted()) { // if we have read fully, and the charset was correct, keep that current parse
                inputSource.close()
            } else {
                doc = null
            }
        } else { // specified by content type header (or by user on file load)
            Validate.notEmpty(
                effectiveCharsetName,
                "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML"
            )
        }

        // finally: prepare the return struct
        if (effectiveCharsetName == null) effectiveCharsetName = defaultCharsetName
        val charset: Charset =
            if (effectiveCharsetName == defaultCharsetName) Charsets.UTF8 else com.fleeksoft.charset.Charsets.forName(effectiveCharsetName)
        return CharsetDoc(charset = charset, doc = doc, input = inputSource)
    }

    public fun parseInputSource(charsetDoc: CharsetDoc, baseUri: String, parser: Parser): Document {
        // if doc != null it was fully parsed during charset detection; so just return that
        if (charsetDoc.doc != null) return charsetDoc.doc!!

        val input = charsetDoc.input
        val doc: Document
        val charset: Charset = charsetDoc.charset

        val reader = sourceInputStreamReader(input, charset)
        try {
            doc = parser.parseInput(reader, baseUri)
        } catch (e: UncheckedIOException) {
            // io exception when parsing (not seen before because reading the stream as we go)
            throw e
        }
        doc.outputSettings().charset(charset)
        if (!charset.canEncode()) {
            // some charsets can read but not encode; switch to an encodable charset and update the meta el
            doc.charset(Charsets.UTF8)
        }
        return doc
    }

    /**
     * Parse out a charset from a content type header. If the charset is not supported, returns null (so the default
     * will kick in.)
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    public fun getCharsetFromContentType(contentType: String?): String? {
        if (contentType == null) return null
        val matchResult: MatchResult? = charsetPattern.find(contentType)
        matchResult?.let {
            var charset: String = it.groupValues[1].trim { it <= ' ' }
            charset = charset.replace("charset=", "")
            return validateCharset(charset)
        }
        return null
    }

    private fun validateCharset(cs: String?): String? {
        if (cs.isNullOrEmpty()) return null
        val cleanedStr = cs.trim { it <= ' ' }.replace("[\"']".toRegex(), "")
        return try {
            when {
                cleanedStr.isCharsetSupported() -> cleanedStr
                else -> null
            }
        } catch (e: IllegalCharsetNameException) {
            // if our this charset matching fails.... we just take the default
            null
        }
    }

    /**
     * Creates a random string, suitable for use as a mime boundary
     */
    public fun mimeBoundary(): String {
        val mime: StringBuilder = StringUtil.borrowBuilder()
        for (i in 0 until boundaryLength) {
            mime.append(mimeBoundaryChars[Random.nextInt(mimeBoundaryChars.size)])
        }
        return StringUtil.releaseBuilder(mime)
    }

    private fun detectCharsetFromBom(sourceReader: SourceReader): String? {
        val bom = ByteArray(4)
        sourceReader.mark(bom.size.toLong())
        sourceReader.read(bom, 0, bom.size)
        sourceReader.reset()

        // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        if (bom[0].toInt() == 0x00 && bom[1].toInt() == 0x00 && bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte()) { // BE
            sourceReader.read(bom, 0, 4) // consume BOM
            return "UTF-32BE" // and I hope it's on your system
        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() && bom[2].toInt() == 0x00 && bom[3].toInt() == 0x00) { // LE
            sourceReader.read(bom, 0, 4) // consume BOM
            return "UTF-32LE" // and I hope it's on your system
        } else if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte()) { // BE
            sourceReader.read(bom, 0, 2) // consume BOM
            return "UTF-16BE" // in all Javas
        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()) { // LE
            sourceReader.read(bom, 0, 2) // consume BOM
            return "UTF-16LE" // in all Javas
        } else if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            sourceReader.read(bom, 0, 3) // consume the UTF-8 BOM
            return "UTF-8"
        }
        return null
    }
}
