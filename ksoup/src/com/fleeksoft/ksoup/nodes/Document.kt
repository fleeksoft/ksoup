package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.parser.Tag
import com.fleeksoft.ksoup.ported.KCloneable
import com.fleeksoft.ksoup.ported.io.Charsets
import com.fleeksoft.ksoup.select.Elements
import com.fleeksoft.ksoup.select.Evaluator
import com.fleeksoft.ksoup.select.Selector

/**
 * A HTML Document.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */

/**
 * Create a new, empty Document, in the specified namespace.
 * @param namespace the namespace of this Document's root node.
 * @param location base URI of document
 * @see .createShell
 */
public class Document(private val namespace: String, private val location: String?) :
    Element(Tag.valueOf("#root", namespace, ParseSettings.htmlDefault), location) {
    private var outputSettings = OutputSettings()
    private var parser: Parser?
    private var quirksMode = QuirksMode.noQuirks
    private var updateMetaCharset = false

    init {
        parser = Parser.htmlParser() // default, but overridable
    }

    /**
     * Create a new, empty Document, in the HTML namespace.
     * @param baseUri base URI of document
     * @see com.fleeksoft.ksoup.Ksoup.parseFile
     * @see .Document
     */
    public constructor(baseUri: String?) : this(Parser.NamespaceHtml, baseUri)

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     *
     * Will return an empty string if the location is unknown (e.g. if parsed from a String).
     * @return location
     */
    public fun location(): String? {
        return location
    }

    /**
     * Returns this Document's doctype.
     * @return document type, or null if not set
     */
    public fun documentType(): DocumentType? {
        for (node in _childNodes) {
            if (node is DocumentType) {
                return node
            } else if (node !is LeafNode) {
                // scans forward across comments, text, processing instructions etc
                break
            }
        }
        return null
        // todo - add a set document type?
    }

    /**
     * Find the root HTML element, or create it if it doesn't exist.
     * @return the root HTML element.
     */
    private fun htmlEl(): Element {
        var el: Element? = firstElementChild()
        while (el != null) {
            if (el.nameIs("html")) return el
            el = el.nextElementSibling()
        }
        return appendElement("html")
    }

    /**
    Get this document's {@code head} element.
    <p>
    As a side effect, if this Document does not already have an HTML structure, it will be created. If you do not want
    that, use {@code #selectFirst("head")} instead.

    @return {@code head} element.
     */
    public fun head(): Element {
        val html: Element = htmlEl()
        var el: Element? = html.firstElementChild()
        while (el != null) {
            if (el.nameIs("head")) return el
            el = el.nextElementSibling()
        }
        return html.prependElement("head")
    }

    /**
    Get this document's {@code <body>} or {@code <frameset>} element.
    <p>
    As a <b>side effect</b>, if this Document does not already have an HTML structure, it will be created with a {@code
    <body>} element. If you do not want that, use {@code #selectFirst("body")} instead.

    @return {@code body} element for documents with a {@code <body>}, a new {@code <body>} element if the document
    had no contents, or the outermost {@code <frameset> element} for frameset documents.
     */
    public fun body(): Element {
        val html: Element = htmlEl()
        var el: Element? = html.firstElementChild()
        while (el != null) {
            if (el.nameIs("body") || el.nameIs("frameset")) return el
            el = el.nextElementSibling()
        }
        return html.appendElement("body")
    }

    /**
     * Get each of the `<form>` elements contained in this document.
     * @return a List of FormElement objects, which will be empty if there are none.
     * @see Elements.forms
     * @see FormElement.elements
     */
    public fun forms(): List<FormElement> {
        return select("form").forms()
    }

    /**
     * Selects the first [FormElement] in this document that matches the query. If none match, throws an
     * [IllegalArgumentException].
     * @param cssQuery a [Selector] CSS query
     * @return the first matching `<form>` element
     * @throws IllegalArgumentException if no match is found
     */
    public fun expectForm(cssQuery: String): FormElement? {
        val els: Elements = select(cssQuery)
        for (el in els) {
            if (el is FormElement) return el
        }
        Validate.fail("No form elements matched the query '$cssQuery' in the document.")
        return null // (not really)
    }

    /**
     * Get the string contents of the document's `title` element.
     * @return Trimmed title, or empty string if none set.
     */
    public fun title(): String {
        // title is a preserve whitespace tag (for document output), but normalised here
        val titleEl: Element? = head().selectFirst(titleEval)
        return if (titleEl != null) StringUtil.normaliseWhitespace(titleEl.text()).trim() else ""
    }

    /**
     * Set the document's `title` element. Updates the existing element, or adds `title` to `head` if
     * not present
     * @param title string to set as title
     */
    public fun title(title: String) {
        var titleEl: Element? = head().selectFirst(titleEval)
        if (titleEl == null) {
            // add to head
            titleEl = head().appendElement("title")
        }
        titleEl.text(title)
    }

    /**
     * Create a new Element, with this document's base uri. Does not make the new element a child of this document.
     * @param tagName element tag name (e.g. `a`)
     * @return new element
     */
    public fun createElement(tagName: String): Element {
        return Element(
            Tag.valueOf(
                tagName,
                parser!!.defaultNamespace(),
                ParseSettings.preserveCase,
            ),
            this.baseUri(),
        )
    }

    override fun outerHtml(): String {
        return super.html() // no outer wrapper tag
    }

    /**
     * Set the text of the `body` of this document. Any existing nodes within the body will be cleared.
     * @param text un-encoded text
     * @return this document
     */
    override fun text(text: String): Element {
        body().text(text) // overridden to not nuke doc structure
        return this
    }

    override fun nodeName(): String {
        return "#document"
    }

    /**
     * Sets the charset used in this document. This method is equivalent
     * to [ OutputSettings.charset(Charset)][OutputSettings.charset] but in addition it updates the
     * charset / encoding element within the document.
     *
     *
     * This enables
     * [meta charset update][.updateMetaCharsetElement].
     *
     *
     * If there's no element with charset / encoding information yet it will
     * be created. Obsolete charset / encoding definitions are removed!
     *
     *
     * **Elements used:**
     *
     *
     *  * **Html:** *&lt;meta charset="CHARSET"&gt;*
     *  * **Xml:** *&lt;?xml version="1.0" encoding="CHARSET"&gt;*
     *
     *
     * @param charset Charset
     *
     * @see .updateMetaCharsetElement
     * @see OutputSettings.charset
     */
    public fun charset(charset: Charset) {
        updateMetaCharsetElement(true)
        outputSettings.charset(charset)
        ensureMetaCharsetElement()
    }

    /**
     * Returns the charset used in this document. This method is equivalent
     * to [OutputSettings.charset].
     *
     * @return Current Charset
     *
     * @see OutputSettings.charset
     */
    public fun charset(): Charset {
        return outputSettings.charset()
    }

    /**
     * Sets whether the element with charset information in this document is
     * updated on changes through [ Document.charset(Charset)][.charset] or not.
     *
     *
     * If set to <tt>false</tt> *(default)* there are no elements
     * modified.
     *
     * @param update If <tt>true</tt> the element updated on charset
     * changes, <tt>false</tt> if not
     *
     * @see .charset
     */
    public fun updateMetaCharsetElement(update: Boolean) {
        updateMetaCharset = update
    }

    /**
     * Returns whether the element with charset information in this document is
     * updated on changes through [ Document.charset(Charset)][.charset] or not.
     *
     * @return Returns <tt>true</tt> if the element is updated on charset
     * changes, <tt>false</tt> if not
     */
    public fun updateMetaCharsetElement(): Boolean {
        return updateMetaCharset
    }

    override fun clone(): Document {
        return super.clone() as Document
    }

    override fun createClone(): Node {
        val document = Document(namespace, location)
        document.outputSettings = this.outputSettings.clone()
        return document
    }

    public override fun shallowClone(): Document {
        val clone = Document(this.tag().namespace(), baseUri())
        if (attributes != null) clone.attributes = attributes!!.clone()
        clone.outputSettings = outputSettings.clone()
        return clone
    }

    /**
     * Ensures a meta charset (html) or xml declaration (xml) with the current
     * encoding used. This only applies with
     * [updateMetaCharset][.updateMetaCharsetElement] set to
     * <tt>true</tt>, otherwise this method does nothing.
     *
     *
     *  * An existing element gets updated with the current charset
     *  * If there's no element yet it will be inserted
     *  * Obsolete elements are removed
     *
     *
     *
     * **Elements used:**
     *
     *
     *  * **Html:** *&lt;meta charset="CHARSET"&gt;*
     *  * **Xml:** *&lt;?xml version="1.0" encoding="CHARSET"&gt;*
     *
     */
    private fun ensureMetaCharsetElement() {
        if (updateMetaCharset) {
            val syntax = outputSettings().syntax()
            if (syntax == OutputSettings.Syntax.html) {
                val metaCharset: Element? = selectFirst("meta[charset]")
                if (metaCharset != null) {
                    metaCharset.attr("charset", charset().name)
                } else {
                    head().appendElement("meta").attr("charset", charset().name)
                }
                select("meta[name=charset]").remove() // Remove obsolete elements
            } else if (syntax == OutputSettings.Syntax.xml) {
                val node: Node = ensureChildNodes()[0]
                if (node is XmlDeclaration) {
                    var decl: XmlDeclaration = node
                    if (decl.name() == "xml") {
                        decl.attr("encoding", charset().name)
                        if (decl.hasAttr("version")) decl.attr("version", "1.0")
                    } else {
                        decl = XmlDeclaration("xml", false)
                        decl.attr("version", "1.0")
                        decl.attr("encoding", charset().name)
                        prependChild(decl)
                    }
                } else {
                    val decl = XmlDeclaration("xml", false)
                    decl.attr("version", "1.0")
                    decl.attr("encoding", charset().name)
                    prependChild(decl)
                }
            }
        }
    }

    /**
     * A Document's output settings control the form of the text() and html() methods.
     */
    public data class OutputSettings(
        private var escapeMode: Entities.EscapeMode = Entities.EscapeMode.base,
        private var charset: Charset = Charsets.UTF8,
        private var prettyPrint: Boolean = true,
        private var outline: Boolean = false,
        private var indentAmount: Int = 1,
        private var maxPaddingWidth: Int = 30,
        private var syntax: Syntax = Syntax.html,
    ) : KCloneable<OutputSettings> {
        /**
         * The output serialization syntax.
         */
        public enum class Syntax { html, xml }

        /**
        Get the document's current entity escape mode:
        <ul>
        <li><code>xhtml</code>, the minimal named entities in XHTML / XML</li>
        <li><code>base</code>, which provides a limited set of named HTML
        entities and escapes other characters as numbered entities for maximum compatibility</li>
        <li><code>extended</code>,
        which uses the complete set of HTML named entities.</li>
        </ul>
        <p>The default escape mode is <code>base</code>.
        @return the document's current escape mode
         */
        public fun escapeMode(): Entities.EscapeMode {
            return escapeMode
        }

        /**
         * Set the document's escape mode, which determines how characters are escaped when the output character set
         * does not support a given character:- using either a named or a numbered escape.
         * @param escapeMode the new escape mode to use
         * @return the document's output settings, for chaining
         */
        public fun escapeMode(escapeMode: Entities.EscapeMode): OutputSettings {
            this.escapeMode = escapeMode
            return this
        }

        /**
         * Get the document's current output charset, which is used to control which characters are escaped when
         * generating HTML (via the `html()` methods), and which are kept intact.
         *
         *
         * Where possible (when parsing from a URL or File), the document's output charset is automatically set to the
         * input charset. Otherwise, it defaults to UTF-8.
         * @return the document's current charset.
         */
        public fun charset(): Charset {
            return charset
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset to use.
         * @return the document's output settings, for chaining
         */
        public fun charset(charset: Charset): OutputSettings {
            this.charset = charset
            return this
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset (by name) to use.
         * @return the document's output settings, for chaining
         */
        public fun charset(charset: String): OutputSettings {
            // FIXME: ascii not supported on some targest fallback to ISO-8859-1
            if (charset.lowercase() == "ascii" || charset.lowercase() == "us-ascii") {
                runCatching {
                    charset(Charsets.forName(charset))
                }.onFailure {
                    charset(Charsets.forName("ISO-8859-1"))
                }
            } else {
                charset(Charsets.forName(charset))
            }
            return this
        }

        public fun encoder(): Charset {
            return charset
        }

        /**
         * Get the document's current output syntax.
         * @return current syntax
         */
        public fun syntax(): Syntax {
            return syntax
        }

        /**
         * Set the document's output syntax. Either `html`, with empty tags and boolean attributes (etc), or
         * `xml`, with self-closing tags.
         *
         * When set to [xml][Document.OutputSettings.Syntax.xml], the [escapeMode][.escapeMode] is
         * automatically set to [Entities.EscapeMode.xhtml], but may be subsequently changed if desired.
         * @param syntax serialization syntax
         * @return the document's output settings, for chaining
         */
        public fun syntax(syntax: Syntax): OutputSettings {
            this.syntax = syntax
            if (syntax == Syntax.xml) this.escapeMode(Entities.EscapeMode.xhtml)
            return this
        }

        /**
         * Get if pretty printing is enabled. Default is true. If disabled, the HTML output methods will not re-format
         * the output, and the output will generally look like the input.
         * @return if pretty printing is enabled.
         */
        public fun prettyPrint(): Boolean {
            return prettyPrint
        }

        /**
         * Enable or disable pretty printing.
         * @param pretty new pretty print setting
         * @return this, for chaining
         */
        public fun prettyPrint(pretty: Boolean): OutputSettings {
            prettyPrint = pretty
            return this
        }

        /**
         * Get if outline mode is enabled. Default is false. If enabled, the HTML output methods will consider
         * all tags as block.
         * @return if outline mode is enabled.
         */
        public fun outline(): Boolean {
            return outline
        }

        /**
         * Enable or disable HTML outline mode.
         * @param outlineMode new outline setting
         * @return this, for chaining
         */
        public fun outline(outlineMode: Boolean): OutputSettings {
            outline = outlineMode
            return this
        }

        /**
         * Get the current tag indent amount, used when pretty printing.
         * @return the current indent amount
         */
        public fun indentAmount(): Int {
            return indentAmount
        }

        /**
         * Set the indent amount for pretty printing
         * @param indentAmount number of spaces to use for indenting each level. Must be &gt;= 0.
         * @return this, for chaining
         */
        public fun indentAmount(indentAmount: Int): OutputSettings {
            Validate.isTrue(indentAmount >= 0)
            this.indentAmount = indentAmount
            return this
        }

        /**
         * Get the current max padding amount, used when pretty printing
         * so very deeply nested nodes don't get insane padding amounts.
         * @return the current indent amount
         */
        public fun maxPaddingWidth(): Int {
            return maxPaddingWidth
        }

        /**
         * Set the max padding amount for pretty printing so very deeply nested nodes don't get insane padding amounts.
         * @param maxPaddingWidth number of spaces to use for indenting each level of nested nodes. Must be &gt;= -1.
         * Default is 30 and -1 means unlimited.
         * @return this, for chaining
         */
        public fun maxPaddingWidth(maxPaddingWidth: Int): OutputSettings {
            Validate.isTrue(maxPaddingWidth >= -1)
            this.maxPaddingWidth = maxPaddingWidth
            return this
        }

        override fun clone(): OutputSettings {
            return this.copy()
        }
    }

    /**
     * Get the document's current output settings.
     * @return the document's current output settings.
     */
    public fun outputSettings(): OutputSettings {
        return outputSettings
    }

    /**
     * Set the document's output settings.
     * @param outputSettings new output settings.
     * @return this document, for chaining.
     */
    public fun outputSettings(outputSettings: OutputSettings): Document {
        this.outputSettings = outputSettings
        return this
    }

    public enum class QuirksMode {
        noQuirks,
        quirks,
        limitedQuirks,
    }

    public fun quirksMode(): QuirksMode {
        return quirksMode
    }

    public fun quirksMode(quirksMode: QuirksMode): Document {
        this.quirksMode = quirksMode
        return this
    }

    /**
     * Get the parser that was used to parse this document.
     * @return the parser
     */
    public fun parser(): Parser? {
        return parser
    }

    /**
     * Set the parser used to create this document. This parser is then used when further parsing within this document
     * is required.
     * @param parser the configured parser to use when further parsing is required for this document.
     * @return this document, for chaining.
     */
    public fun parser(parser: Parser?): Document {
        this.parser = parser
        return this
    }

    public companion object {
        /**
         * Create a valid, empty shell of a document, suitable for adding more elements to.
         * @param baseUri baseUri of document
         * @return document with html, head, and body elements.
         */
        public fun createShell(baseUri: String): Document {
            val doc = Document(baseUri)
            val html: Element = doc.appendElement("html")
            html.appendElement("head")
            html.appendElement("body")
            return doc
        }

        private val titleEval: Evaluator = Evaluator.Tag("title")
    }
}
