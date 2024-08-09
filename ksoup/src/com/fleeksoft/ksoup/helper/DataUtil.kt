package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.Platform
import com.fleeksoft.ksoup.UncheckedIOException
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.isApple
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.XmlDeclaration
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.StreamParser
import com.fleeksoft.ksoup.ported.*
import com.fleeksoft.ksoup.readGzipFile
import com.fleeksoft.ksoup.select.Elements
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.readAsSyncStream
import korlibs.io.file.std.uniVfs
import korlibs.io.lang.Charset
import korlibs.io.lang.Charsets
import korlibs.io.stream.*
import kotlin.random.Random

/**
 * Internal static utilities for handling data.
 */
public object DataUtil {
    private val charsetPattern: Regex = Regex("charset=\\s*['\"]?([^\\s,;'\"]*)", RegexOption.IGNORE_CASE)
    private val defaultCharsetName: String = Charsets.UTF8.name // used if not found in header or meta charset
    private const val firstReadBufferSize: Long = (1024 * 5).toLong()
    private val mimeBoundaryChars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    public const val boundaryLength: Int = 32

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
    public suspend fun load(
        filePath: String,
        baseUri: String,
        charsetName: String? = null,
        parser: Parser = Parser.htmlParser(),
    ): Document {
        return load(file = filePath.uniVfs, charsetName = charsetName, baseUri = baseUri, parser = parser)
    }

    /**
     * Parses a Document from an input steam, using the provided Parser.
     * @param syncStream buffer reader to parse. The stream will be closed after reading.
     * @param baseUri base URI of document, to resolve relative links against
     * @param charsetName character set of input (optional)
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return Document
     */
    public fun load(
        syncStream: SyncStream,
        baseUri: String,
        charsetName: String? = null,
        parser: Parser = Parser.htmlParser(),
    ): Document {
        return parseInputSource(syncStream = syncStream, baseUri = baseUri, charsetName = charsetName, parser = parser)
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
    public suspend fun load(
        file: VfsFile,
        baseUri: String = file.absolutePath,
        charsetName: String? = null,
        parser: Parser = Parser.htmlParser(),
    ): Document {
        val stream = openStream(file)
        return parseInputSource(syncStream = stream, baseUri = baseUri, charsetName = charsetName, parser = parser)
    }

    /**
     * Returns a [StreamParser] that will parse the supplied file progressively.
     * Files that are compressed with gzip (and end in `.gz` or `.z`)
     * are supported in addition to uncompressed files.
     *
     * @param path file to load
     * @param charset (optional) character set of input; specify `null` to attempt to autodetect from metadata.
     * A BOM in the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate [parser][Parser.xmlParser] to use.
     *
     * @return Document
     * @throws IOException on IO error
     * @see Connection.Response.streamParser
     */
    suspend fun streamParser(file: VfsFile, baseUri: String, charset: Charset?, parser: Parser): StreamParser {
        val streamer = StreamParser(parser)
        val charsetName: String? = charset?.name
        val charsetDoc: CharsetDoc = detectCharset(openStream(file = file), baseUri, charsetName, parser)
        val reader = charsetDoc.input.toStreamCharReader(charset = charsetDoc.charset)
        maybeSkipBom(reader, charsetDoc)
        streamer.parse(reader, baseUri) // initializes the parse and the document, but does not step() it

        return streamer
    }

    fun parseInputSource(syncStream: SyncStream, baseUri: String, charsetName: String?, parser: Parser): Document {
        val doc: Document
        var charsetDoc: CharsetDoc? = null
        try {
            charsetDoc = detectCharset(syncStream = syncStream, baseUri = baseUri, charsetName = charsetName, parser = parser)
            doc = parseInputSource(charsetDoc = charsetDoc, baseUri = baseUri, parser = parser)
        } finally {
            charsetDoc?.input?.close()
        }
        return doc
    }

    /** Open an input stream from a file; if it's a gzip file, returns a GZIPInputStream to unzip it.  */
    private suspend fun openStream(file: VfsFile): SyncStream {
        val name = Normalizer.lowerCase(file.fullName)
        if (name.endsWith(".gz") || name.endsWith(".z")) {
            val byteArray = file.readChunk(0, 2)
            val zipped = (byteArray.size == 2 && byteArray[0].toInt() == 31 && byteArray[1].toInt() == -117) // gzip magic bytes 31(0x1f), -117(0x1f)
            if (zipped) {
                return readGzipFile(file)
            }
        }
//        fixme:// replace with streaming instead of reading all bytes
        return file.readAsSyncStream()
    }

    /** A struct to return a detected charset, and a document (if fully read).  */
    data class CharsetDoc internal constructor(
        val charset: Charset,
        var doc: Document?,
        val input: SyncStream,
        val skip: Boolean
    )


    private fun detectCharset(syncStream: SyncStream, baseUri: String, charsetName: String?, parser: Parser): CharsetDoc {
        var effectiveCharsetName: String? = charsetName

        var doc: Document? = null

        // read the start of the stream and look for a BOM or meta charset

        syncStream.mark(SharedConstants.DefaultBufferSize)
        // -1 because we read one more to see if completed. First read is < buffer size, so can't be invalid.
        val firstBytes: ByteArray = readToByteBuffer(syncStream, firstReadBufferSize - 1)
        val fullyRead = syncStream.availableRead <= 0
        syncStream.reset()


        // look for BOM - overrides any other header or input
        val bomCharset: BomCharset? = detectCharsetFromBom(firstBytes)
        if (bomCharset != null) effectiveCharsetName = bomCharset.charset

        if (effectiveCharsetName == null) { // determine from meta. safe first parse as UTF-8
            doc = try {
                parser.parseInput(firstBytes, baseUri)
            } catch (e: UncheckedIOException) {
                throw e
            }
            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            val metaElements: Elements = doc!!.select("meta[http-equiv=content-type], meta[charset]")
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
                doc = null
            } else if (!fullyRead) {
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
        val charset: Charset = if (effectiveCharsetName == defaultCharsetName) Charsets.UTF8 else Charset.forName(effectiveCharsetName)
        val skip = bomCharset != null && bomCharset.offset // skip 1 if the BOM is there and needs offset
        // if consumer needs to parse the input; prep it if there's a BOM. Can't skip in inputstream as wrapping buffer will ignore the pos
        return CharsetDoc(charset, doc, syncStream, skip)
    }

    public fun parseInputSource(charsetDoc: CharsetDoc, baseUri: String, parser: Parser): Document {


        // if doc != null it was fully parsed during charset detection; so just return that
        if (charsetDoc.doc != null) return charsetDoc.doc!!

        val input = charsetDoc.input
        Validate.notNull(input)
        val doc: Document
        val charset: Charset = charsetDoc.charset

        val reader = charsetDoc.input.toStreamCharReader(charset = charset)
        maybeSkipBom(reader, charsetDoc)
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
     * Read the input stream into a byte buffer. To deal with slow input streams, you may interrupt the thread this
     * method is executing on. The data read until being interrupted will be available.
     * @param syncStream the input stream to read from
     * @param maxSize the maximum size in bytes to read from the stream. Set to 0 to be unlimited.
     * @return the filled byte buffer
     */
    public fun readToByteBuffer(
        syncStream: SyncStream,
        maxSize: Long,
    ): ByteArray {
        return if (maxSize == 0L) {
            syncStream.readAll()
        } else {
            val size = if (syncStream.availableRead > 0) minOf(maxSize, syncStream.available) else maxSize
            syncStream.readBytes(size.toInt())
        }
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

    //    @Nullable
    private fun validateCharset(cs: String?): String? {
        if (cs.isNullOrEmpty()) return null
        val cleanedStr = cs.trim { it <= ' ' }.replace("[\"']".toRegex(), "")
        return try {
            when {
                cleanedStr.isCharsetSupported() -> cleanedStr
                cleanedStr.uppercase().isCharsetSupported() -> cleanedStr.uppercase()
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

    private fun detectCharsetFromBom(firstByteArray: ByteArray): BomCharset? {
        // .mark and rewind used to return Buffer, now ByteBuffer, so cast for backward compat
        val bom = if (firstByteArray.size >= 4) {
            firstByteArray.copyOf(4)
        } else {
            ByteArray(4)
        }

        if (bom[0].toInt() == 0x00 && bom[1].toInt() == 0x00 && bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte()) { // BE
            return BomCharset("UTF-32BE", Platform.isApple()) // and I hope it's on your system
        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() && bom[2].toInt() == 0x00 && bom[3].toInt() == 0x00) { // LE
            return BomCharset("UTF-32LE", Platform.isApple()) // and I hope it's on your system
        } else if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte()) { // BE
            return BomCharset("UTF-16BE", true) // in all Javas
        } else if (bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()) { // LE
            return BomCharset("UTF-16LE", true) // in all Javas
        } else if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            return BomCharset("UTF-8", true) // in all Javas
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null
    }

    private class BomCharset(val charset: String, val offset: Boolean)

    private fun maybeSkipBom(reader: StreamCharReader, charsetDoc: CharsetDoc) {
        if (charsetDoc.skip) {
            reader.skip(1)
        }
    }
}
