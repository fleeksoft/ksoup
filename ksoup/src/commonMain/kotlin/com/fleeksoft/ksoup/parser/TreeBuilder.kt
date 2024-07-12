package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceHtml
import com.fleeksoft.ksoup.ported.StreamCharReader

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

    protected lateinit var doc: Document // current doc we are building into
        private set

    public lateinit var stack: ArrayList<Element?> // the stack of open elements
    public open var baseUri: String? = null // current base uri, for creating new elements
    public var currentToken: Token? = null // currentToken is used only for error tracking.
    public var settings: ParseSettings? = null

    // tags we've used in this parse; saves tag GC for custom tags.
    private var seenTags: MutableMap<String, Tag>? = null
    private lateinit var start: Token.StartTag // start tag to process
    private lateinit var end: Token.EndTag

    public abstract fun defaultSettings(): ParseSettings?

    public var trackSourceRange: Boolean = false // optionally tracks the source range of nodes

    public open fun initialiseParse(
        input: StreamCharReader,
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
        stack = ArrayList(32)
        seenTags = HashMap()
        start = Token.StartTag(this)
        currentToken = start // init current token to the virtual start token.
        this.baseUri = baseUri
    }

    public fun parse(
        input: StreamCharReader,
        baseUri: String,
        parser: Parser,
    ): Document {
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
    public abstract fun newInstance(): TreeBuilder

    public abstract fun parseFragment(
        inputFragment: String,
        context: Element?,
        baseUri: String?,
        parser: Parser,
    ): List<Node>

    protected fun runParser() {
        val tokeniser = this.tokeniser!!
        val eof = Token.TokenType.EOF

        while (true) {
            val token = tokeniser.read()
            currentToken = token
            process(token)
            if (token.type === eof) break
            token.reset()
        }

        // once we hit the end, pop remaining items off the stack
        while (!stack.isEmpty()) pop()
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
        val size = stack.size
        val removed = stack.removeAt(size - 1)!!
        onNodeClosed(removed)
        return removed
    }

    /**
     * Adds the specified Element to the end of the stack, and hits onNodeInserted.
     * @param element
     */
    public fun push(element: Element) {
        stack.add(element)
        onNodeInserted(element)
    }

    /**
     * Get the current element (last on the stack). If all items have been removed, returns the document instead
     * (which might not actually be on the stack; use stack.size() == 0 to test if required.
     * @return the last element on the stack, if any; or the root document
     */
    public fun currentElement(): Element {
        val size: Int = stack.size
        return if (size > 0) stack[size - 1]!! else doc
    }

    /**
     * Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     * @param normalName name to check
     * @return true if there is a current element on the stack, and its name equals the supplied
     */
    public fun currentElementIs(normalName: String?): Boolean {
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
    public fun currentElementIs(
        normalName: String?,
        namespace: String?,
    ): Boolean {
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
    }

    /**
     * Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     * tracking the closing source range of the node.  @param node the node being closed
     */
    public fun onNodeClosed(node: Node) {
        trackNodePosition(node, false)
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
