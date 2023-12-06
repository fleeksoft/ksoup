package com.fleeksoft.ksoup.helper

import com.fleeksoft.ksoup.UncheckedIOException
import com.fleeksoft.ksoup.internal.ConstrainableSource
import com.fleeksoft.ksoup.internal.Normalizer
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.XmlDeclaration
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.*
import com.fleeksoft.ksoup.ported.canEncode
import com.fleeksoft.ksoup.ported.isCharsetSupported
import com.fleeksoft.ksoup.readFile
import com.fleeksoft.ksoup.readGzipFile
import com.fleeksoft.ksoup.select.Elements
import io.ktor.utils.io.charsets.*
import okio.*
import okio.Buffer
import kotlin.random.Random

/**
 * Internal static utilities for handling data.
 *
 */
public object DataUtil {
    private val charsetPattern: Regex =
        Regex("charset=\\s*['\"]?([^\\s,;'\"]*)", RegexOption.IGNORE_CASE)
    private val defaultCharsetName: String = Charsets.UTF_8.name // used if not found in header or meta charset
    private const val firstReadBufferSize: Long = (1024 * 5).toLong()
    private const val bufferSize: Long = (1024 * 32).toLong()
    private val mimeBoundaryChars =
        "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
    public const val boundaryLength: Int = 32

    /**
     * Loads and parses a file to a Document, with the HtmlParser. Files that are compressed with gzip (and end in `.gz` or `.z`)
     * are supported in addition to uncompressed files.
     *
     * @param path file to load
     * @param charsetName (optional) character set of input; specify `null` to attempt to autodetect. A BOM in
     * the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    @Throws(IOException::class)
    public fun load(path: Path, charsetName: String?, baseUri: String): Document {
        return load(path, charsetName, baseUri, Parser.htmlParser())
    }

    /**
     * Loads and parses a file to a Document. Files that are compressed with gzip (and end in `.gz` or `.z`)
     * are supported in addition to uncompressed files.
     *
     * @param filePath file to load
     * @param charsetName (optional) character set of input; specify `null` to attempt to autodetect. A BOM in
     * the file will always override this setting.
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate [parser][Parser.xmlParser] to use.
     *
     * @return Document
     * @throws IOException on IO error
     * @since 1.14.2
     */
    @Throws(IOException::class)
    public fun load(
        filePath: Path,
        charsetName: String?,
        baseUri: String,
        parser: Parser,
    ): Document {
        val name: String = Normalizer.lowerCase(filePath.name)

        val source = readFile(filePath)
        return source.use { bufferedSource ->
            val bufferReader: BufferReader = if (name.endsWith(".gz") || name.endsWith(".z")) {
                val zipped: Boolean = runCatching {
                    bufferedSource.peek().use { peekSource ->
//                    In Kotlin, a Byte is signed and ranges from -128 to 127. In contrast, in Java, a byte is an unsigned type and ranges from 0 to 255.
//                    in kotlin use readUByte to get unsigned byte
                        peekSource.readByte().toUByte().toInt() == 0x1f && peekSource.readByte()
                            .toUByte().toInt() == 0x8b // gzip magic bytes  0x1f == 31 & 0x8b = 139
                    }
                }.getOrNull() ?: false

                if (zipped) {
                    BufferReader(readGzipFile(filePath))
                } else {
                    BufferReader(bufferedSource)
                }
            } else {
                BufferReader(bufferedSource)
            }

//            val charset = charsetName?.let { Charset.forName(it) } ?: Charsets.UTF_8
//            val inputData = bufferedSource.readString()
            parseInputSource(
                bufferReader,
                charsetName,
                baseUri,
                parser,
            ) // Assuming there's a method called parseInputString
        }
    }

    /**
     * Parses a Document from an input steam.
     * @param bufferReader buffer reader to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri base URI of document, to resolve relative links against
     * @return Document
     * @throws IOException on IO error
     */
    @Throws(IOException::class)
    public fun load(
        bufferReader: BufferReader,
        charsetName: String?,
        baseUri: String,
    ): Document {
        return parseInputSource(bufferReader, charsetName, baseUri, Parser.htmlParser())
    }

    /**
     * Parses a Document from an input steam, using the provided Parser.
     * @param bufferReader buffer reader to parse. The stream will be closed after reading.
     * @param charsetName character set of input (optional)
     * @param baseUri base URI of document, to resolve relative links against
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return Document
     * @throws IOException on IO error
     */
    @Throws(IOException::class)
    public fun load(
        bufferReader: BufferReader,
        charsetName: String?,
        baseUri: String,
        parser: Parser,
    ): Document {
        return parseInputSource(bufferReader, charsetName, baseUri, parser)
    }

    /**
     * Writes the input stream to the output stream. Doesn't close them.
     * @param bufferReader buffer reader to read from
     * @param outSource output stream to write to
     * @throws IOException on IO error
     */
    @Throws(IOException::class)
    public fun crossStreams(source: ByteArray, outSource: Buffer) {
        outSource.write(source)
    }

    @Throws(IOException::class)
    public fun parseInputSource(
        bufferReader: BufferReader?,
        charsetName: String?,
        baseUri: String,
        parser: Parser,
    ): Document {
        if (bufferReader == null) {
            // empty body
            return Document(baseUri)
        }
        var effectiveCharsetName: String? = charsetName

        /*@Nullable */
        var doc: Document? = null

        // read the start of the stream and look for a BOM or meta charset

        val peekedBuffer = bufferReader.peek()
        // -1 because we read one more to see if completed. First read is < buffer size, so can't be invalid.
        val firstBytes: ByteArray = readToByteBuffer(peekedBuffer, firstReadBufferSize - 1)
        val fullyRead = peekedBuffer.exhausted()
        peekedBuffer.close()

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
            val metaElements: Elements =
                doc!!.select("meta[http-equiv=content-type], meta[charset]")
            var foundCharset: String? = null // if not found, will keep utf-8 as best attempt
            for (meta in metaElements) {
                if (meta.hasAttr("http-equiv")) {
                    foundCharset =
                        getCharsetFromContentType(meta.attr("content"))
                }
                if (foundCharset == null && meta.hasAttr("charset")) {
                    foundCharset =
                        meta.attr("charset")
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
                if (decl != null) {
                    if (decl.name().equals("xml", ignoreCase = true)) {
                        foundCharset =
                            decl.attr("encoding")
                    }
                }
            }
            foundCharset = validateCharset(foundCharset)
            if (foundCharset != null && !foundCharset.equals(
                    defaultCharsetName,
                    ignoreCase = true,
                )
            ) { // need to re-decode. (case insensitive check here to match how validate works)
                foundCharset = foundCharset.trim { it <= ' ' }.replace("[\"']".toRegex(), "")
                effectiveCharsetName = foundCharset
                doc = null
            } else if (!fullyRead) {
                doc = null
            }
        } else { // specified by content type header (or by user on file load)
            Validate.notEmpty(
                effectiveCharsetName,
                "Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML",
            )
        }
        if (doc == null) {
            if (effectiveCharsetName == null) effectiveCharsetName = defaultCharsetName
            // TODO: bufferSize not used here because not supported yet
            bufferReader.setCharSet(effectiveCharsetName)

            if (bomCharset != null && bomCharset.offset) { // creating the buffered inputReader ignores the input pos, so must skip here
//                skip first char which can be 2-4
                bufferReader.skipFirstUnicodeChar(1)
            }
            doc = try {
                parser.parseInput(bufferReader, baseUri)
            } catch (e: UncheckedIOException) {
                // io exception when parsing (not seen before because reading the stream as we go)
                throw e
            }
            val charset: Charset =
                if (effectiveCharsetName == defaultCharsetName) {
                    Charsets.UTF_8
                } else {
                    Charset.forName(
                        effectiveCharsetName,
                    )
                }
            doc!!.outputSettings().charset(charset)
            if (!charset.canEncode()) {
                // some charsets can read but not encode; switch to an encodable charset and update the meta el
                doc.charset(Charsets.UTF_8)
            }
        }

        return doc
    }

    /**
     * Read the input stream into a byte buffer. To deal with slow input streams, you may interrupt the thread this
     * method is executing on. The data read until being interrupted will be available.
     * @param bufferReader the input stream to read from
     * @param maxSize the maximum size in bytes to read from the stream. Set to 0 to be unlimited.
     * @return the filled byte buffer
     * @throws IOException if an exception occurs whilst reading from the input stream.
     */
    @Throws(IOException::class)
    public fun readToByteBuffer(bufferReader: BufferReader, maxSize: Long): ByteArray {
        require(maxSize >= 0) {
            "maxSize must be 0 (unlimited) or larger"
        }
        return if (maxSize == 0L) {
            bufferReader.readByteArray()
        } else {
            ConstrainableSource.wrap(BufferReader(bufferReader), maxSize.toInt()).readToByteBuffer(maxSize.toInt())
                .readByteArray()
        }
    }

    /**
     * Parse out a charset from a content type header. If the charset is not supported, returns null (so the default
     * will kick in.)
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
//    @Nullable
    internal fun getCharsetFromContentType(contentType: String?): String? {
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
    internal fun mimeBoundary(): String {
        val mime: StringBuilder = StringUtil.borrowBuilder()
        for (i in 0 until boundaryLength) {
            mime.append(mimeBoundaryChars[Random.nextInt(mimeBoundaryChars.size)])
        }
        return StringUtil.releaseBuilder(mime)
    }

    //    @Nullable
    /*private fun detectCharsetFromBom(reader: Reader): BomCharset? {
        val snapshot = okio.Buffer()
        reader.copyTo(snapshot, 0, min(4, reader.size))
        val bom = snapshot.readByteArray()
        if (bom[0].toInt() == 0x00 && bom[1].toInt() == 0x00 && bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte() || // BE
            bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() && bom[2].toInt() == 0x00 && bom[3].toInt() == 0x00
        ) { // LE
            return BomCharset("UTF-32", false) // and I hope it's on your system
        } else if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte() || // BE
            bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()
        ) {
            return BomCharset("UTF-16", false) // in all Javas
        } else if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            return BomCharset("UTF-8", true) // in all Javas
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null
    }*/

    private fun detectCharsetFromBom(firstByteArray: ByteArray): BomCharset? {
        // .mark and rewind used to return Buffer, now ByteBuffer, so cast for backward compat
        val bom = if (firstByteArray.size >= 4) {
            firstByteArray.copyOf(4)
        } else {
            ByteArray(4)
        }
        if (bom[0].toInt() == 0x00 && bom[1].toInt() == 0x00 && bom[2] == 0xFE.toByte() && bom[3] == 0xFF.toByte() || // BE
            bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte() && bom[2].toInt() == 0x00 && bom[3].toInt() == 0x00
        ) { // LE
            return BomCharset("UTF-32", false) // and I hope it's on your system
        } else if (bom[0] == 0xFE.toByte() && bom[1] == 0xFF.toByte() || // BE
            bom[0] == 0xFF.toByte() && bom[1] == 0xFE.toByte()
        ) {
            return BomCharset("UTF-16", false) // in all Javas
        } else if (bom[0] == 0xEF.toByte() && bom[1] == 0xBB.toByte() && bom[2] == 0xBF.toByte()) {
            return BomCharset("UTF-8", true) // in all Javas
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null
    }

    private class BomCharset(val charset: String, val offset: Boolean)
}
