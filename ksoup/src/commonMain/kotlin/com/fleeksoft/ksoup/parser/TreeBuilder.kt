package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.nodes.Attributes
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.Range
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceHtml
import com.fleeksoft.ksoup.ported.BufferReader

/**
 * @author Sabeeh
 */
abstract class TreeBuilder {
    var parser: Parser? = null
    lateinit var reader: CharacterReader
    var tokeniser: Tokeniser? = null
    protected lateinit var doc: Document // current doc we are building into
    lateinit var stack: ArrayList<Element?> // the stack of open elements
    open var baseUri: String? = null // current base uri, for creating new elements
    protected var currentToken: Token? = null // currentToken is used only for error tracking.
    var settings: ParseSettings? = null
    private var seenTags: MutableMap<String, Tag>? =
        null // tags we've used in this parse; saves tag GC for custom tags.
    private val start: Token.StartTag = Token.StartTag() // start tag to process
    private val end: Token.EndTag = Token.EndTag()
    abstract fun defaultSettings(): ParseSettings?
    private var trackSourceRange = false // optionally tracks the source range of nodes

    protected open fun initialiseParse(input: BufferReader, baseUri: String?, parser: Parser) {
        doc = Document(parser.defaultNamespace(), baseUri)
        doc.parser(parser)
        this.parser = parser
        settings = parser.settings()
        reader = CharacterReader(input)
        trackSourceRange = parser.isTrackPosition
        reader.trackNewlines(parser.isTrackErrors() || trackSourceRange) // when tracking errors or source ranges, enable newline tracking for better legibility
        currentToken = null
        tokeniser = Tokeniser(reader, parser.getErrors())
        stack = ArrayList<Element?>(32)
        seenTags = HashMap<String, Tag>()
        this.baseUri = baseUri
    }

    fun parse(input: BufferReader, baseUri: String?, parser: Parser): Document {
        initialiseParse(input, baseUri, parser)
        runParser()

        // tidy up - as the Parser and Treebuilder are retained in document for settings / fragments
        reader.close()
        tokeniser = null
        stack.clear()
        seenTags = null
        return doc
    }

    /**
     * Create a new copy of this TreeBuilder
     * @return copy, ready for a new parse
     */
    abstract fun newInstance(): TreeBuilder
    abstract fun parseFragment(
        inputFragment: String,
        context: Element?,
        baseUri: String?,
        parser: Parser,
    ): List<Node>

    protected fun runParser() {
        val tokeniser: Tokeniser? = tokeniser
        val eof: Token.TokenType = Token.TokenType.EOF
        while (true) {
            val token: Token = tokeniser!!.read()
            process(token)
            token.reset()
            if (token.type === eof) break
        }
    }

    abstract fun process(token: Token): Boolean
    fun processStartTag(name: String): Boolean {
        // these are "virtual" start tags (auto-created by the treebuilder), so not tracking the start position
        val start: Token.StartTag = start
        return if (currentToken === start) { // don't recycle an in-use token
            process(Token.StartTag().name(name))
        } else {
            process(start.reset().name(name))
        }
    }

    fun processStartTag(name: String?, attrs: Attributes?): Boolean {
        val start: Token.StartTag = start
        if (currentToken === start) { // don't recycle an in-use token
            return process(Token.StartTag().nameAttr(name, attrs))
        }
        start.reset()
        start.nameAttr(name, attrs)
        return process(start)
    }

    fun processEndTag(name: String): Boolean {
        return if (currentToken === end) { // don't recycle an in-use token
            process(Token.EndTag().name(name))
        } else {
            process(end.reset().name(name))
        }
    }

    /**
     * Get the current element (last on the stack). If all items have been removed, returns the document instead
     * (which might not actually be on the stack; use stack.size() == 0 to test if required.
     * @return the last element on the stack, if any; or the root document
     */
    fun currentElement(): Element {
        val size: Int = stack.size
        return if (size > 0) stack[size - 1]!! else doc
    }

    /**
     * Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     * @param normalName name to check
     * @return true if there is a current element on the stack, and its name equals the supplied
     */
    fun currentElementIs(normalName: String?): Boolean {
        if (stack.size == 0) return false
        val current: Element = currentElement()
        return (
                current.normalName() == normalName && current.tag().namespace() == NamespaceHtml
                )
    }

    /**
     * Checks if the Current Element's normal name equals the supplied name, in the specified namespace.
     * @param normalName name to check
     * @param namespace the namespace
     * @return true if there is a current element on the stack, and its name equals the supplied
     */
    fun currentElementIs(normalName: String?, namespace: String?): Boolean {
        if (stack.size == 0) return false
        val current: Element = currentElement()
        return (
                current.normalName() == normalName && current.tag().namespace() == namespace
                )
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message template
     */
    protected fun error(msg: String) {
        val errors: ParseErrorList = parser!!.getErrors()
        if (errors.canAddError()) errors.add(ParseError(reader, msg))
    }

    /**
     * (An internal method, visible for Element. For HTML parse, signals that script and style text should be treated as
     * Data Nodes).
     */
    open fun isContentForTagData(normalName: String): Boolean {
        return false
    }

    protected fun tagFor(tagName: String, namespace: String, settings: ParseSettings?): Tag {
        val cached: Tag? =
            seenTags!![tagName] // note that we don't normalize the cache key. But tag via valueOf may be normalized.
        if (cached == null || cached.namespace() != namespace) {
            // only return from cache if the namespace is the same. not running nested cache to save double hit on the common flow
            val tag: Tag = Tag.valueOf(tagName, namespace, settings)
            seenTags!![tagName] = tag
            return tag
        }
        return cached
    }

    fun tagFor(tagName: String, settings: ParseSettings?): Tag {
        return tagFor(tagName, defaultNamespace(), settings)
    }

    /**
     * Gets the default namespace for this TreeBuilder
     * @return the default namespace
     */
    open fun defaultNamespace(): String {
        return NamespaceHtml
    }

    /**
     * Called by implementing TreeBuilders when a node has been inserted. This implementation includes optionally tracking
     * the source range of the node.
     * @param node the node that was just inserted
     * @param token the (optional) token that created this node
     */
    fun onNodeInserted(node: Node, /*@Nullable*/ token: Token?) {
        trackNodePosition(node, token, true)
    }

    /**
     * Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     * tracking the closing source range of the node.
     * @param node the node being closed
     * @param token the end-tag token that closed this node
     */
    protected fun onNodeClosed(node: Node, token: Token?) {
        trackNodePosition(node, token, false)
    }

    private fun trackNodePosition(node: Node, /*@Nullable*/ token: Token?, start: Boolean) {
        if (trackSourceRange && token != null) {
            val startPos: Int = token.startPos()
            if (startPos == Token.Unset) return // untracked, virtual token
            val startRange: Range.Position =
                Range.Position(
                    startPos,
                    reader.lineNumber(startPos),
                    reader.columnNumber(startPos),
                )
            val endPos: Int = token.endPos()
            val endRange: Range.Position =
                Range.Position(endPos, reader.lineNumber(endPos), reader.columnNumber(endPos))
            val range = Range(startRange, endRange)
            range.track(node, start)
        }
    }
}
