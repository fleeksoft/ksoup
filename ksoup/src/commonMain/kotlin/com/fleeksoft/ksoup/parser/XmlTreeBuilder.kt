package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceXml
import com.fleeksoft.ksoup.ported.StreamCharReader
import com.fleeksoft.ksoup.ported.toStreamCharReader
import korlibs.io.stream.openSync

/**
 * Use the `XmlTreeBuilder` when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 *
 * Usage example: `Document xmlDoc = Ksoup.parse(html, baseUrl, Parser.xmlParser());`
 *
 * @author Sabeeh
 */
internal open class XmlTreeBuilder : TreeBuilder() {
    override fun defaultSettings(): ParseSettings {
        return ParseSettings.preserveCase
    }

    override fun initialiseParse(
        input: StreamCharReader,
        baseUri: String,
        parser: Parser,
    ) {
        super.initialiseParse(input, baseUri, parser)

        // place the document onto the stack. differs from HtmlTreeBuilder (not on stack). Note not push()ed, so not onNodeInserted.
        stack.add(doc)

        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false) // as XML, we don't understand what whitespace is significant or not
    }

    fun parse(
        input: StreamCharReader,
        baseUri: String? = null,
    ): Document {
        return parse(input, baseUri ?: "", Parser(this))
    }

    fun parse(
        input: String,
        baseUri: String? = null,
    ): Document {
        return parse(input.openSync().toStreamCharReader(), baseUri ?: "", Parser(this))
    }

    override fun newInstance(): XmlTreeBuilder {
        return XmlTreeBuilder()
    }

    override fun defaultNamespace(): String {
        return NamespaceXml
    }

    override fun process(token: Token): Boolean {
        currentToken = token

        when (token.type) {
            Token.TokenType.StartTag -> insertElementFor(token.asStartTag())
            Token.TokenType.EndTag -> popStackToClose(token.asEndTag())
            Token.TokenType.Comment -> insertCommentFor(token.asComment())
            Token.TokenType.Character -> insertCharacterFor(token.asCharacter())
            Token.TokenType.Doctype -> insertDoctypeFor(token.asDoctype())
            Token.TokenType.EOF -> {}
            else -> Validate.fail("Unexpected token type: " + token.type)
        }
        return true
    }

    fun insertElementFor(startTag: Token.StartTag) {
        val tag = tagFor(startTag.name(), settings)
        if (startTag.attributes != null) startTag.attributes!!.deduplicate(settings!!)

        val el = Element(tag, null, settings!!.normalizeAttributes(startTag.attributes))
        currentElement().appendChild(el)
        push(el)

        if (startTag.isSelfClosing) {
            tag.setSelfClosing()
            pop() // push & pop ensures onNodeInserted & onNodeClosed
        }
    }

    fun insertLeafNode(node: LeafNode?) {
        currentElement().appendChild(node!!)
        onNodeInserted(node)
    }

    fun insertCommentFor(commentToken: Token.Comment) {
        val comment = Comment(commentToken.getData())
        var insert: LeafNode? = comment
        if (commentToken.bogus && comment.isXmlDeclaration()) {
            // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            // so we do a bit of a hack and parse the data as an element to pull the attributes out
            // todo - refactor this to parse more appropriately
            val decl = comment.asXmlDeclaration() // else, we couldn't parse it as a decl, so leave as a comment
            if (decl != null) insert = decl
        }
        insertLeafNode(insert)
    }

    fun insertCharacterFor(token: Token.Character) {
        val data: String = token.data!!
        insertLeafNode(if (token.isCData()) CDataNode(data) else TextNode(data))
    }

    fun insertDoctypeFor(token: Token.Doctype) {
        val doctypeNode =
            DocumentType(
                settings!!.normalizeTag(token.getName()),
                token.getPublicIdentifier(),
                token.getSystemIdentifier(),
            )
        doctypeNode.setPubSysKey(token.pubSysKey)
        insertLeafNode(doctypeNode)
    }

    @Deprecated("unused and will be removed. ")
    protected fun insertNode(node: Node?) {
        currentElement().appendChild(node!!)
        onNodeInserted(node)
    }

    @Deprecated("unused and will be removed. ")
    protected fun insertNode(
        node: Node?,
        token: Token?,
    ) {
        currentElement().appendChild(node!!)
        onNodeInserted(node)
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    protected fun popStackToClose(endTag: Token.EndTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        val elName = settings!!.normalizeTag(endTag.tagName!!)
        var firstFound: Element? = null

        val bottom: Int = stack.size - 1
        val upper =
            if (bottom >= XmlTreeBuilder.maxQueueDepth) bottom - XmlTreeBuilder.maxQueueDepth else 0

        for (pos in stack.size - 1 downTo upper) {
            val next = stack[pos]!!
            if (next.nodeName() == elName) {
                firstFound = next
                break
            }
        }
        if (firstFound == null) return // not found, skip

        for (pos in stack.size - 1 downTo 0) {
            val next = pop()
            if (next === firstFound) {
                break
            }
        }
    }

    override fun parseFragment(
        inputFragment: String,
        context: Element?,
        baseUri: String?,
        parser: Parser,
    ): List<Node> {
        return parseFragment(inputFragment, baseUri, parser)
    }

    fun parseFragment(
        inputFragment: String,
        baseUri: String?,
        parser: Parser,
    ): List<Node> {
        initialiseParse(inputFragment.openSync().toStreamCharReader(), baseUri ?: "", parser)
        runParser()
        return doc.childNodes()
    }

    companion object {
        private const val maxQueueDepth = 256 // an arbitrary tension point between real XML and crafted pain
    }
}
