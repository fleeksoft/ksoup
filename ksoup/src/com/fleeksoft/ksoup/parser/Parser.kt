/*
 * Kotlin port of jsoup's Parser.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import co.touchlab.stately.concurrency.Synchronizable
import co.touchlab.stately.concurrency.synchronize
import com.fleeksoft.io.Reader
import com.fleeksoft.io.StringReader
import com.fleeksoft.ksoup.KmpJsExport
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TagSet
import com.fleeksoft.ksoup.ported.KCloneable
import kotlin.js.JsName

/**
 * Parses HTML or XML into a {@link org.jsoup.nodes.Document}. Generally, it is simpler to use one of the parse methods in
 * {@link org.jsoup.Jsoup}.
 * <p>Note that a given Parser instance object is threadsafe, but not concurrent. (Concurrent parse calls will
 * synchronize.) To reuse a Parser configuration in a multithreaded environment, use {@link #newInstance()} to make
 * copies.</p>
 */
@KmpJsExport
public class Parser : KCloneable<Parser> {
    private var treeBuilder: TreeBuilder
    private var errors: ParseErrorList
    private var settings: ParseSettings

    @JsName("_tagSet")
    private var tagSet: TagSet? = null
    private val lock = Synchronizable()

    /**
     * Test if position tracking is enabled. If it is, Nodes will have a Position to track where in the original input
     * source they were created from. By default, tracking is not enabled.
     * @return current track position setting
     */
    public var isTrackPosition: Boolean = false
        private set

    /**
     * Create a new Parser, using the specified TreeBuilder
     * @param treeBuilder TreeBuilder to use to parse input into Documents.
     */
    @JsName("withTreeBuilder")
    public constructor(treeBuilder: TreeBuilder) {
        this.treeBuilder = treeBuilder
        settings = treeBuilder.defaultSettings()
        errors = ParseErrorList.noTracking()
    }

    /**
     * Creates a new Parser as a deep copy of this; including initializing a new TreeBuilder. Allows independent (multi-threaded) use.
     * @return a copied parser
     */
    public fun newInstance(): Parser {
        return Parser(this)
    }

    public override fun clone(): Parser {
        return Parser(this)
    }

    private constructor(copy: Parser) {
        treeBuilder = copy.treeBuilder.newInstance() // because extended
        errors = ParseErrorList(copy.errors) // only copies size, not contents
        settings = ParseSettings(copy.settings)
        isTrackPosition = copy.isTrackPosition
    }

    public fun parseInput(input: String, baseUri: String): Document {
        return parseInput(StringReader(input), baseUri)
    }

    @JsName("parseReaderInput")
    public fun parseInput(reader: Reader, baseUri: String): Document {
        return lock.synchronize { treeBuilder.parse(reader, baseUri, this) }
    }

    public fun parseFragmentInput(fragment: String, context: Element?, baseUri: String): List<Node> {
        return parseFragmentInput(StringReader(fragment), context, baseUri)
    }


    @JsName("parseReaderFragmentInput")
    fun parseFragmentInput(fragment: Reader, context: Element?, baseUri: String): List<Node> {
        return lock.synchronize { treeBuilder.parseFragment(fragment, context, baseUri, this) }
    }

    /**
     * Get the TreeBuilder currently in use.
     * @return current TreeBuilder.
     */
    public fun getTreeBuilder(): TreeBuilder {
        return treeBuilder
    }

    public fun isTrackErrors(): Boolean = errors.maxSize > 0

    /**
     * Enable or disable parse error tracking for the next parse.
     * @param maxErrors the maximum number of errors to track. Set to 0 to disable.
     * @return this, for chaining
     */
    public fun setTrackErrors(maxErrors: Int): Parser {
        errors =
            if (maxErrors > 0) ParseErrorList.tracking(maxErrors) else ParseErrorList.noTracking()
        return this
    }

    /**
     * Retrieve the parse errors, if any, from the last parse.
     * @return list of parse errors, up to the size of the maximum errors tracked.
     * @see .setTrackErrors
     */
    public fun getErrors(): ParseErrorList {
        return errors
    }

    /**
     * Enable or disable source position tracking. If enabled, Nodes will have a Position to track where in the original
     * input source they were created from.
     * @param trackPosition position tracking setting; `true` to enable
     * @return this Parser, for chaining
     */
    public fun setTrackPosition(trackPosition: Boolean): Parser {
        isTrackPosition = trackPosition
        return this
    }

    /**
     * Update the ParseSettings of this Parser, to control the case sensitivity of tags and attributes.
     * @param settings the new settings
     * @return this Parser
     */
    @JsName("setSettings")
    public fun settings(settings: ParseSettings): Parser {
        this.settings = settings
        return this
    }

    /**
     * Gets the current ParseSettings for this Parser
     * @return current ParseSettings
     */
    public fun settings(): ParseSettings {
        return settings
    }

    /**
     * Set a custom TagSet to use for this Parser. This allows you to define your own tags, and control how they are
     * parsed. For example, you can set a tag to preserve whitespace, or to be treated as a block tag.
     *
     * You can start with the [TagSet.Html] defaults and customize, or a new empty TagSet.
     *
     * @param tagSet the TagSet to use. This gets copied, so that changes that the parse makes (tags found in the document will be added) do not clobber the original TagSet.
     * @return this Parser
     */
    @JsName("setTagSet")
    fun tagSet(tagSet: TagSet): Parser {
        this.tagSet = TagSet(tagSet) // copy it as we are going to mutate it
        return this
    }

    /**
     * Get the current TagSet for this Parser, which will be either this parser's default, or one that you have set.
     * @return the current TagSet. After the parse, this will contain any new tags that were found in the document.
     */
    fun tagSet(): TagSet {
        if (tagSet == null)
            tagSet = treeBuilder.defaultTagSet()
        return tagSet!!
    }

    public fun defaultNamespace(): String {
        return getTreeBuilder().defaultNamespace()
    }

    public companion object {
        public const val NamespaceHtml: String = "http://www.w3.org/1999/xhtml"
        public const val NamespaceXml: String = "http://www.w3.org/XML/1998/namespace"
        public const val NamespaceMathml: String = "http://www.w3.org/1998/Math/MathML"
        public const val NamespaceSvg: String = "http://www.w3.org/2000/svg"
        // static parse functions below

        /**
         * Parse HTML into a Document.
         *
         * @param html HTML to parse
         * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
         *
         * @return parsed Document
         */
        public fun parse(html: String, baseUri: String): Document {
            val treeBuilder: TreeBuilder = HtmlTreeBuilder()
            return treeBuilder.parse(StringReader(html), baseUri, Parser(treeBuilder))
        }

        /**
         * Parse a fragment of HTML into a list of nodes. The context element, if supplied, supplies parsing context.
         *
         * @param fragmentHtml the fragment of HTML to parse
         * @param context (optional) the element that this HTML fragment is being parsed for (i.e. for inner HTML). This
         * provides stack context (for implicit element creation).
         * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
         * @param errorList list to add errors to
         *
         * @return list of nodes parsed from the input HTML. Note that the context element, if supplied, is not modified.
         */
        public fun parseFragment(
            fragmentHtml: String,
            context: Element?,
            baseUri: String,
            errorList: ParseErrorList? = null,
        ): List<Node> {
            val treeBuilder = HtmlTreeBuilder()
            val parser = Parser(treeBuilder)
            if (errorList != null) {
                parser.errors = errorList
            }
            return treeBuilder.parseFragment(StringReader(fragmentHtml), context, baseUri, parser)
        }

        /**
         * Parse a fragment of XML into a list of nodes.
         *
         * @param fragmentXml the fragment of XML to parse
         * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
         * @return list of nodes parsed from the input XML.
         */
        public fun parseXmlFragment(fragmentXml: String, baseUri: String): List<Node> {
            val treeBuilder = XmlTreeBuilder()
            return treeBuilder.parseFragment(StringReader(fragmentXml), null, baseUri, Parser(treeBuilder))
        }

        /**
         * Parse a fragment of HTML into the `body` of a Document.
         *
         * @param bodyHtml fragment of HTML
         * @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
         *
         * @return Document, with empty head, and HTML parsed into body
         */
        public fun parseBodyFragment(bodyHtml: String, baseUri: String): Document {
            val doc: Document = Document.createShell(baseUri)
            val body: Element = doc.body()
            val nodeList: List<Node> = parseFragment(bodyHtml, body, baseUri)
            body.appendChildren(nodeList)
            return doc
        }

        /**
         * Utility method to unescape HTML entities from a string
         * @param html HTML escaped string
         * @param inAttribute if the string is to be escaped in strict mode (as attributes are)
         * @return an unescaped string
         */
        public fun unescapeEntities(html: String, inAttribute: Boolean): String {
            if (html.indexOf('&') < 0) return html // nothing to unescape
            val parser: Parser = htmlParser()
            parser.treeBuilder.initialiseParse(StringReader(html), "", parser)
            val tokeniser = Tokeniser(parser.treeBuilder)
            return tokeniser.unescapeEntities(inAttribute)
        }
        // builders

        /**
         * Create a new HTML parser. This parser treats input as HTML5, and enforces the creation of a normalised document,
         * based on a knowledge of the semantics of the incoming tags.
         * @return a new HTML parser.
         */
        public fun htmlParser(): Parser {
            return Parser(HtmlTreeBuilder())
        }

        /**
         * Create a new XML parser. This parser assumes no knowledge of the incoming tags and does not treat it as HTML,
         * rather creates a simple tree directly from the input.
         * @return a new simple XML parser.
         */
        public fun xmlParser(): Parser {
            return Parser(XmlTreeBuilder())
        }
    }
}
