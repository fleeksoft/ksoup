package com.fleeksoft.ksoup

import com.fleeksoft.io.Reader
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.model.MetaData
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.safety.Cleaner
import com.fleeksoft.ksoup.safety.Safelist
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsStatic


/**
 * The core public access point to the com.fleeksoft.ksoup functionality.
 *
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
public object Ksoup {

    /**
     * Parse HTML into a Document. The parser will make a sensible, balanced document tree out of any HTML.
     *
     * @param html HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @return sane HTML
     */
    public fun parse(html: String, baseUri: String = ""): Document {
        return Parser.parse(html, baseUri)
    }

    /**
     * Parse HTML into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param html HTML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @return sane HTML
     */
    @JsName("parseWithParser")
    public fun parse(html: String, parser: Parser = Parser.htmlParser(), baseUri: String = ""): Document {
        return parser.parseInput(html, baseUri)
    }

    /**
     * Parse HTML/XML from Reader into a Document, using the provided Parser. You can provide an alternate parser, such as a simple XML
     * (non-HTML) parser.
     *
     * @param reader Reader to parse
     * @param parser alternate [parser][Parser.xmlParser] to use.
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs, that occur
     * before the HTML declares a `<base href>` tag.
     * @return sane HTML
     */
    @JsExport.Ignore
    public fun parse(reader: Reader, parser: Parser = Parser.htmlParser(), baseUri: String = ""): Document {
        return parser.parseInput(reader, baseUri)
    }

    /**
     * Parse XML into a Document
     *
     * @param xml XML to parse
     * @param baseUri The URL where the HTML was retrieved from. Used to resolve relative URLs to absolute URLs
     * @return Document
     */
    public fun parseXml(xml: String, baseUri: String = ""): Document {
        return Parser.xmlParser().parseInput(xml, baseUri)
    }


    /**
     * Parse a fragment of HTML, with the assumption that it forms the `body` of the HTML.
     *
     * @param bodyHtml body HTML fragment
     * @param baseUri  URL to resolve relative URLs against.
     * @return sane HTML document
     * @see Document.body
     */
    public fun parseBodyFragment(bodyHtml: String, baseUri: String = ""): Document {
        return Parser.parseBodyFragment(bodyHtml, baseUri)
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
     * @param safelist list of permitted HTML elements
     * @param baseUri URL to resolve relative URLs against
     * @param outputSettings document output settings; use to control pretty-printing and entity escape modes
     * @return safe HTML (body fragment)
     * @see Cleaner.clean
     */
    public fun clean(
        bodyHtml: String,
        safelist: Safelist = Safelist.relaxed(),
        baseUri: String = "",
        outputSettings: Document.OutputSettings? = null
    ): String {
        var uri = baseUri
        if (uri.isEmpty() && safelist.preserveRelativeLinks()) {
            // set a placeholder URI to allow relative links to pass abs resolution for protocol tests; won't leak to output
            uri = SharedConstants.DummyUri
        }
        val dirty: Document = parseBodyFragment(bodyHtml, uri)
        val cleaner = Cleaner(safelist)
        val clean: Document = cleaner.clean(dirty)
        if (outputSettings != null) {
            clean.outputSettings(outputSettings)
        }
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
     * <pre>`val safelist = Safelist.relaxed()
     * val isValid = Ksoup.isValid(sourceBodyHtml, safelist)
     * val normalizedHtml = Ksoup.clean(sourceBodyHtml, "https://example.com/", safelist)
    `</pre> *
     *
     * Assumes the HTML is a body fragment (i.e. will be used in an existing HTML document body.)
     * @param bodyHtml HTML to test
     * @param safelist safelist to test against
     * @return true if no tags or attributes were removed; false otherwise
     * @see .clean
     */
    public fun isValid(bodyHtml: String, safelist: Safelist = Safelist.relaxed()): Boolean {
        return Cleaner(safelist).isValidBodyHtml(bodyHtml)
    }

    fun parseMetaData(element: Element): MetaData {
        val el = if (element is Document) {
            element.headOrNull() ?: element
        } else element
        val title = el.selectFirst("title")?.text()
        return parseMetaDataInternal(baseUri = element.baseUri(), title = title) { query ->
            el.selectFirst(query)
        }
    }

    /**
     * Parses metadata from an HTML string.
     *
     * @param html HTML content as a String.
     * @param baseUri Base URI to resolve relative URLs against. Defaults to an empty string.
     * @param interceptor Optional function to intercept and manipulate the head element and generated MetaData.
     * @return MetaData object containing parsed metadata information.
     */
    @JsName("parseMetaDataOfHtml")
    fun parseMetaData(
        html: String,
        baseUri: String = "",
        interceptor: ((head: Element, metaData: MetaData) -> Unit)? = null
    ): MetaData {
        val head = parse(html = html, baseUri = baseUri).let { doc -> doc.headOrNull() ?: doc }

        val title = head.selectFirst("title")?.text()
        return parseMetaDataInternal(baseUri = baseUri, title = title) { query ->
            head.selectFirst(query)
        }.also {
            interceptor?.invoke(head, it)
        }
    }

    fun parseMetaDataInternal(
        baseUri: String,
        title: String?,
        selectFirst: (query: String) -> Element?
    ): MetaData {
        // Extract Open Graph metadata
        val ogTitle = selectFirst("meta[property=og:title]")?.attr("content")
        val ogSiteName = selectFirst("meta[property=og:site_name]")?.attr("content")
        val ogType = selectFirst("meta[property=og:type]")?.attr("content")
        val ogLocale = selectFirst("meta[property=og:locale]")?.attr("content")
        val ogDescription = selectFirst("meta[property=og:description]")?.attr("content")
        val ogImage = selectFirst("meta[property=og:image]")?.attr("content")
        val ogUrl = selectFirst("meta[property=og:url]")?.attr("content")

        // Extract Twitter metadata
        val twitterTitle = selectFirst("meta[name=twitter:title]")?.attr("content")
        val twitterCard = selectFirst("meta[name=twitter:card]")?.attr("content")
        val twitterDescription = selectFirst("meta[name=twitter:description]")?.attr("content")
        val twitterImage = selectFirst("meta[name=twitter:image]")?.attr("content")

        // Extract standard metadata
        val titleTag = selectFirst("meta[name=title]")?.attr("content")
        val descriptionTag = selectFirst("meta[name=description]")?.attr("content")
        val author = selectFirst("meta[name=author]")?.attr("content")

        // Extract canonical URL
        val canonicalTag = selectFirst("link[rel=canonical]")?.attr("href")

        // Fetch favicon
        var faviconTag = selectFirst("link[rel~=icon]")?.attr("href")
        if (faviconTag != null && !faviconTag.startsWith("http", ignoreCase = true) && baseUri.isNotEmpty()) {
            faviconTag = baseUri + faviconTag
        }

        var shortcutIcon = selectFirst("link[rel~=shortcut icon]")?.attr("href")
        if (shortcutIcon != null && !shortcutIcon.startsWith("http", ignoreCase = true) && baseUri.isNotEmpty()) {
            shortcutIcon = baseUri + shortcutIcon
        }

        // Create a MetaData object
        return MetaData(
            ogTitle = ogTitle,
            ogSiteName = ogSiteName,
            ogType = ogType,
            ogLocale = ogLocale,
            ogDescription = ogDescription,
            ogImage = ogImage,
            ogUrl = ogUrl,
            twitterCard = twitterCard,
            twitterTitle = twitterTitle,
            twitterDescription = twitterDescription,
            twitterImage = twitterImage,
            title = titleTag,
            description = descriptionTag,
            canonical = canonicalTag,
            htmlTitle = title,
            author = author,
            favicon = faviconTag,
            shortcutIcon = shortcutIcon,
        )
    }
}