package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceHtml
import com.fleeksoft.ksoup.ported.io.Reader
import com.fleeksoft.ksoup.ported.io.StringReader
import com.fleeksoft.ksoup.select.NodeVisitor

/**
 * @author Sabeeh
 */
public abstract class TreeBuilder {
    public lateinit var parser: Parser
        internal set
    public lateinit var reader: CharacterReader
        private set
    public var tokeniser: Tokeniser? = null
        private set

    lateinit var doc: Document // current doc we are building into
        private set

    public var _stack: ArrayList<Element?>? = null // the stack of open elements
    public open var baseUri: String? = null // current base uri, for creating new elements
    public var currentToken: Token? = null // currentToken is used only for error tracking.
    public var settings: ParseSettings? = null

    // tags we've used in this parse; saves tag GC for custom tags.
    private var seenTags: MutableMap<String, Tag>? = null
    var nodeListener: NodeVisitor? = null // optional listener for node add / removes

    private lateinit var start: Token.StartTag // start tag to process
    private lateinit var end: Token.EndTag

    public abstract fun defaultSettings(): ParseSettings?

    public var trackSourceRange: Boolean = false // optionally tracks the source range of nodes

    fun getStack() = _stack!!

    public open fun initialiseParse(
        input: Reader,
        baseUri: String,
        parser: Parser,
    ) {
        end = Token.EndTag(this)
        doc = Document(parser.defaultNamespace(), baseUri)
        doc.parser(parser)
        this.parser = parser
        settings = parser.settings()
        reader = CharacterReader(input)
        trackSourceRange = parser.isTrackPosition
        reader.trackNewlines(
            parser.isTrackErrors() || trackSourceRange,
        ) // when tracking errors or source ranges, enable newline tracking for better legibility
        tokeniser = Tokeniser(this)
        _stack = ArrayList(32)
        seenTags = HashMap()
        start = Token.StartTag(this)
        currentToken = start // init current token to the virtual start token.
        this.baseUri = baseUri
        onNodeInserted(doc)
    }

    public fun completeParse() {
        // tidy up - as the Parser and Treebuilder are retained in document for settings / fragments
        if (::reader.isInitialized.not()) return
        reader.close()
        tokeniser = null
        _stack = null
        seenTags = null
    }

    public fun parse(input: Reader, baseUri: String, parser: Parser): Document {
        initialiseParse(input, baseUri, parser)
        runParser()
        return doc
    }

    public fun parseFragment(
        inputFragment: String,
        context: Element?,
        baseUri: String,
        parser: Parser,
    ): List<Node> {
        initialiseParse(StringReader(inputFragment), baseUri, parser)
        initialiseParseFragment(context)
        runParser()
        return completeParseFragment()
    }

    open fun initialiseParseFragment(context: Element?) {
        // in Html, sets up context; no-op in XML
    }

    abstract fun completeParseFragment(): List<Node>

    /** Set the node listener, which will then get callbacks for node insert and removals.  */
    fun nodeListener(nodeListener: NodeVisitor?) {
        this.nodeListener = nodeListener
    }

    /**
     * Create a new copy of this TreeBuilder
     * @return copy, ready for a new parse
     */
    abstract fun newInstance(): TreeBuilder

    fun runParser() {
        do {
        } while (stepParser())
        completeParse()
    }

    fun stepParser(): Boolean {
        // if we have reached the end already, step by popping off the stack, to hit nodeRemoved callbacks:
        if (currentToken?.type === Token.TokenType.EOF) {
            if (_stack == null) {
                return false
            } else if (_stack?.isEmpty() == true) {
                onNodeClosed(doc) // the root doc is not on the stack, so let this final step close it
                _stack = null
                return true
            }
            pop()
            return true
        }
        val token = tokeniser!!.read()
        currentToken = token
        process(token)
        token.reset()
        return true
    }

    public abstract fun process(token: Token): Boolean

    public fun processStartTag(name: String): Boolean {
        // these are "virtual" start tags (auto-created by the treebuilder), so not tracking the start position
        val start = this.start
        if (currentToken === start) { // don't recycle an in-use token
            return process(Token.StartTag(this).name(name))
        }
        return process(start.reset().name(name))
    }

    public fun processStartTag(
        name: String,
        attrs: Attributes?,
    ): Boolean {
        val start = this.start
        if (currentToken === start) { // don't recycle an in-use token
            return process(Token.StartTag(this).nameAttr(name, attrs))
        }
        start.reset()
        start.nameAttr(name, attrs)
        return process(start)
    }

    public fun processEndTag(name: String): Boolean {
        if (currentToken === end) { // don't recycle an in-use token
            return process(Token.EndTag(this).name(name))
        }
        return process(end.reset().name(name))
    }

    /**
     * Removes the last Element from the stack, hits onNodeClosed, and then returns it.
     * @return
     */
    public fun pop(): Element {
        val size = _stack?.size
        val removed = if (size != null) _stack?.removeAt(size - 1) else null
        removed?.let { onNodeClosed(it) }
        return removed!!
    }

    /**
     * Adds the specified Element to the end of the stack, and hits onNodeInserted.
     * @param element
     */
    public fun push(element: Element) {
        getStack().add(element)
        onNodeInserted(element)
    }

    /**
     * Get the current element (last on the stack). If all items have been removed, returns the document instead
     * (which might not actually be on the stack; use stack.size() == 0 to test if required.
     * @return the last element on the stack, if any; or the root document
     */
    public fun currentElement(): Element {
        val size: Int = _stack?.size ?: 0
        return if (size > 0) _stack!![size - 1]!! else doc
    }

    /**
     * Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     * @param normalName name to check
     * @return true if there is a current element on the stack, and its name equals the supplied
     */
    public fun currentElementIs(normalName: String?): Boolean {
        if ((_stack?.size ?: 0) == 0) return false
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
    public fun currentElementIs(
        normalName: String?,
        namespace: String?,
    ): Boolean {
        if ((_stack?.size ?: 0) == 0) return false
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
        val errors: ParseErrorList = parser.getErrors()
        if (errors.canAddError()) errors.add(ParseError(reader, msg))
    }

    /**
     * (An internal method, visible for Element. For HTML parse, signals that script and style text should be treated as
     * Data Nodes).
     */
    public open fun isContentForTagData(normalName: String): Boolean {
        return false
    }

    protected fun tagFor(
        tagName: String,
        namespace: String,
        settings: ParseSettings?,
    ): Tag {
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

    public fun tagFor(
        tagName: String,
        settings: ParseSettings?,
    ): Tag {
        return tagFor(tagName, defaultNamespace(), settings)
    }

    /**
     * Gets the default namespace for this TreeBuilder
     * @return the default namespace
     */
    public open fun defaultNamespace(): String {
        return NamespaceHtml
    }

    /**
     * Called by implementing TreeBuilders when a node has been inserted. This implementation includes optionally tracking
     * the source range of the node.  @param node the node that was just inserted
     */
    public fun onNodeInserted(node: Node) {
        trackNodePosition(node, true)

        nodeListener?.head(node, getStack().size)
    }

    /**
     * Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     * tracking the closing source range of the node.  @param node the node being closed
     */
    public fun onNodeClosed(node: Node) {
        trackNodePosition(node, false)

        nodeListener?.tail(node, getStack().size)
    }

    private fun trackNodePosition(
        node: Node,
        isStart: Boolean,
    ) {
        if (!trackSourceRange) return

        val token = currentToken!!
        var startPos = token.startPos()
        var endPos = token.endPos()

        // handle implicit element open / closes.
        if (node is Element) {
            if (token.isEOF()) {
                if (node.endSourceRange()
                        .isTracked()
                ) {
                    return // /body and /html are left on stack until EOF, don't reset them
                }

                endPos = reader.pos()
                startPos = endPos
            } else if (isStart) { // opening tag
                if (!token.isStartTag() || node.normalName() != token.asStartTag().normalName) {
                    endPos = startPos
                }
            } else { // closing tag
                if (!node.tag().isEmpty && !node.tag().isSelfClosing()) {
                    if (!token.isEndTag() || node.normalName() != token.asEndTag().normalName) {
                        endPos = startPos
                    }
                }
            }
        }

        val startPosition: Range.Position =
            Range.Position(startPos, reader.lineNumber(startPos), reader.columnNumber(startPos))
        val endPosition: Range.Position = Range.Position(endPos, reader.lineNumber(endPos), reader.columnNumber(endPos))
        val range = Range(startPosition, endPosition)
        node.attributes().userData(if (isStart) SharedConstants.RangeKey else SharedConstants.EndRangeKey, range)
    }
}
