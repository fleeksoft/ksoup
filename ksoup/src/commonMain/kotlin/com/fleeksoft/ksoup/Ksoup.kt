package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.helper.DataUtil
import com.fleeksoft.ksoup.helper.NetworkHelper
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.safety.Cleaner
import com.fleeksoft.ksoup.safety.Safelist
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import okio.IOException
import okio.Path.Companion.toPath

/**
 * The core public access point to the com.fleeksoft.ksoup functionality.
 *
 * @author Sabeeh
 */
object Ksoup {
    /**
     * Parse HTML into a Document. The parser will make a sensible, balanced document tree out of any HTML.
     *
     * @param html    HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @return sane HTML
     */
    fun parse(html: String, baseUri: String): Document {
        return Parser.parse(html, baseUri)
    }

    /**
     * Parse HTML into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param html    HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     */
    fun parse(html: String, baseUri: String, parser: Parser): Document {
        return parser.parseInput(html, baseUri)
    }

    /**
     * Parse HTML into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.  As no base URI is specified, absolute URL resolution, if required, relies on the HTML including
     * a `<base href>` tag.
     *
     * @param html    HTML to parse
     * before the HTML declares a `<base href>` tag.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     */
    fun parse(html: String, parser: Parser): Document {
        return parser.parseInput(html, "")
    }

    /**
     * Parse HTML into a Document. As no base URI is specified, absolute URL resolution, if required, relies on the HTML
     * including a `<base href>` tag.
     *
     * @param html HTML to parse
     * @return sane HTML
     * @see .parse
     */
    fun parse(html: String): Document {
        return Parser.parse(html, "")
    }

    /**
     * Creates a new connection, with the defined request URL. Use to fetch and parse a HTML page.
     *
     *
     * Use examples:
     *
     *  * `Document doc = Jsoup.connect("http://example.com").userAgent("Mozilla").data("name", "com.fleeksoft.ksoup").get();`
     *  * `Document doc = Jsoup.connect("http://example.com").cookie("auth", "token").post();`
     *
     * @param url URL to connect to. The protocol must be `http` or `https`.
     * @return the connection. You can add data, cookies, and headers; set the user-agent, referrer, method; and then execute.
     *
     */
    fun connect(
        url: String,
        headers: Map<String, String> = mapOf(),
        parser: Parser = Parser.htmlParser(),
    ): Document {
        val result: String =
            runBlocking { NetworkHelper.instance.get(url, headers = headers).bodyAsText() }
        return parse(html = result, parser = parser, baseUri = url)
    }

    /**
     * Parse the contents of a file as HTML.
     *
     * @param file    file to load HTML from. Supports gzipped files (ending in .z or .gz).
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    @Throws(IOException::class)
    fun parseFile(file: String, baseUri: String, charsetName: String? = null): Document {
        val filePath = file.toPath()
        return DataUtil.load(filePath, charsetName, baseUri)
    }

    /**
     * Parse the contents of a file as HTML. The location of the file is used as the base URI to qualify relative URLs.
     *
     * @param file        file to load HTML from. Supports gzipped files (ending in .z or .gz).
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     * @see .parse
     */
    @Throws(IOException::class)
    fun parseFile(file: String, charsetName: String? = null): Document {
        val filePath = file.toPath()
        return DataUtil.load(filePath, charsetName, filePath.toString())
    }

    /**
     * Parse the contents of a file as HTML. The location of the file is used as the base URI to qualify relative URLs.
     * The charset used to read the file will be determined by the byte-order-mark (BOM), or a `<meta charset>` tag,
     * or if neither is present, will be `UTF-8`.
     *
     *
     * This is the equivalent of calling [parse(file, null)][.parse]
     *
     * @param file the file to load HTML from. Supports gzipped files (ending in .z or .gz).
     * @return sane HTML
     * @throws IOException if the file could not be found or read.
     * @see .parse
     * @since 1.15.1
     */
    @Throws(IOException::class)
    fun parseFile(file: String): Document {
        val filePath = file.toPath()
        return DataUtil.load(filePath, null, filePath.toString())
    }

    /**
     * Parse the contents of a file as HTML.
     *
     * @param file          file to load HTML from. Supports gzipped files (ending in .z or .gz).
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     * @since 1.14.2
     */
    @Throws(IOException::class)
    fun parseFile(
        file: String,
        charsetName: String?,
        baseUri: String,
        parser: Parser,
    ): Document {
        return DataUtil.load(file.toPath(), charsetName, baseUri, parser)
    }

    /**
     * Read an input stream, and parse it to a Document.
     *
     * @param `in`          input stream to read. The stream will be closed after reading.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    @Throws(IOException::class)
    fun parse(
        bufferReader: BufferReader,
        charsetName: String?,
        baseUri: String,
    ): Document {
        return DataUtil.load(bufferReader, charsetName, baseUri)
    }

    /**
     * Read an input stream, and parse it to a Document. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param `in`          input stream to read. Make sure to close it after parsing.
     * @param charsetName (optional) character set of file contents. Set to `null` to determine from `http-equiv` meta tag, if
     * present, or fall back to `UTF-8` (which is often safe to do).
     * @param baseUri     The URL where the HTML was retrieved from, to resolve relative links against.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     * @throws IOException if the file could not be found, or read, or if the charsetName is invalid.
     */
    @Throws(IOException::class)
    fun parse(
        bufferReader: BufferReader,
        charsetName: String?,
        baseUri: String,
        parser: Parser,
    ): Document {
        return DataUtil.load(bufferReader, charsetName, baseUri, parser)
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @param baseUri  URL to resolve relative URLs against.
     * @return sane HTML document
     * @see Document.body
     */
    fun parseBodyFragment(bodyHtml: String, baseUri: String?): Document {
        return Parser.parseBodyFragment(bodyHtml, baseUri ?: "")
    }

    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @return sane HTML document
     * @see Document.body
     */
    fun parseBodyFragment(bodyHtml: String): Document {
        return Parser.parseBodyFragment(bodyHtml, "")
    }

    /**
     * Fetch a URL, and parse it as HTML. Provided for compatibility; in most cases use [.connect] instead.
     *
     *
     * The encoding character set is determined by the content-type header or http-equiv meta tag, or falls back to `UTF-8`.
     *
     * @param url           URL to fetch (with a GET). The protocol must be `http` or `https`.
     * @param timeoutMillis Connection and read timeout, in milliseconds. If exceeded, IOException is thrown.
     * @return The parsed HTML.
     * @throws java.net.MalformedURLException if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
     * @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     * @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     * @throws java.net.SocketTimeoutException if the connection times out
     * @throws IOException if a connection or read error occurs
     * @see .connect
     */
    /*@Throws(IOException::class)
    fun parse(url: java.net.URL?, timeoutMillis: Int): Document {
        val con: Connection = HttpConnection.connect(url)
        con.timeout(timeoutMillis)
        return con.get()
    }*/

    /**
     * Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through an allow-list of safe
     * tags and attributes.
     *
     * @param bodyHtml  input untrusted HTML (body fragment)
     * @param baseUri   URL to resolve relative URLs against
     * @param safelist  list of permitted HTML elements
     * @return safe HTML (body fragment)
     * @see Cleaner.clean
     */
    fun clean(bodyHtml: String, baseUri: String?, safelist: Safelist): String {
        val dirty: Document = parseBodyFragment(bodyHtml, baseUri)
        val cleaner = Cleaner(safelist)
        val clean: Document = cleaner.clean(dirty)
        return clean.body().html()
    }

    /**
     * Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a safe-list of permitted
     * tags and attributes.
     *
     *
     * Note that as this method does not take a base href URL to resolve attributes with relative URLs against, those
     * URLs will be removed, unless the input HTML contains a `<base href> tag`. If you wish to preserve those, use
     * the [Ksoup.clean] method instead, and enable
     * [Safelist.preserveRelativeLinks].
     *
     *
     * Note that the output of this method is still **HTML** even when using the TextNode only
     * [Safelist.none], and so any HTML entities in the output will be appropriately escaped.
     * If you want plain text, not HTML, you should use a text method such as [Element.text] instead, after
     * cleaning the document.
     *
     * Example:
     * <pre>`String sourceBodyHtml = "<p>5 is &lt; 6.</p>";
     * String html = Jsoup.clean(sourceBodyHtml, Safelist.none());
     *
     * Cleaner cleaner = new Cleaner(Safelist.none());
     * String text = cleaner.clean(Jsoup.parse(sourceBodyHtml)).text();
     *
     * // html is: 5 is &lt; 6.
     * // text is: 5 is < 6.
    `</pre> *
     *
     * @param bodyHtml input untrusted HTML (body fragment)
     * @param safelist list of permitted HTML elements
     * @return safe HTML (body fragment)
     * @see Cleaner.clean
     */
    fun clean(bodyHtml: String, safelist: Safelist): String {
        return clean(bodyHtml, "", safelist)
    }

    /**
     * Get safe HTML from untrusted input HTML, by parsing input HTML and filtering it through a safe-list of
     * permitted tags and attributes.
     *
     * The HTML is treated as a body fragment; it's expected the cleaned HTML will be used within the body of an
     * existing document. If you want to clean full documents, use [Cleaner.clean] instead, and add
     * structural tags (`html, head, body` etc) to the safelist.
     *
     * @param bodyHtml input untrusted HTML (body fragment)
     * @param baseUri URL to resolve relative URLs against
     * @param safelist list of permitted HTML elements
     * @param outputSettings document output settings; use to control pretty-printing and entity escape modes
     * @return safe HTML (body fragment)
     * @see Cleaner.clean
     */
    fun clean(
        bodyHtml: String,
        baseUri: String?,
        safelist: Safelist,
        outputSettings: Document.OutputSettings,
    ): String {
        val dirty: Document = parseBodyFragment(bodyHtml, baseUri)
        val cleaner = Cleaner(safelist)
        val clean: Document = cleaner.clean(dirty)
        clean.outputSettings(outputSettings)
        return clean.body().html()
    }

    /**
     * Test if the input body HTML has only tags and attributes allowed by the Safelist. Useful for form validation.
     *
     *
     * This method is intended to be used in a user interface as a validator for user input. Note that regardless of the
     * output of this method, the input document **must always** be normalized using a method such as
     * [.clean], and the result of that method used to store or serialize the document
     * before later reuse such as presentation to end users. This ensures that enforced attributes are set correctly, and
     * that any differences between how a given browser and how com.fleeksoft.ksoup parses the input HTML are normalized.
     *
     *
     * Example:
     * <pre>`Safelist safelist = Safelist.relaxed();
     * boolean isValid = Jsoup.isValid(sourceBodyHtml, safelist);
     * String normalizedHtml = Jsoup.clean(sourceBodyHtml, "https://example.com/", safelist);
    `</pre> *
     *
     * Assumes the HTML is a body fragment (i.e. will be used in an existing HTML document body.)
     * @param bodyHtml HTML to test
     * @param safelist safelist to test against
     * @return true if no tags or attributes were removed; false otherwise
     * @see .clean
     */
    fun isValid(bodyHtml: String, safelist: Safelist): Boolean {
        return Cleaner(safelist).isValidBodyHtml(bodyHtml)
    }
}
