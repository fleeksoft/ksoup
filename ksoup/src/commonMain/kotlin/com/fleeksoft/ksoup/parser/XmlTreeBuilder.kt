package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceXml
import com.fleeksoft.ksoup.ported.BufferReader

/**
 * Use the `XmlTreeBuilder` when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 *
 * Usage example: `Document xmlDoc = Jsoup.parse(html, baseUrl, Parser.xmlParser());`
 *
 * @author Sabeeh
 */
open class XmlTreeBuilder : TreeBuilder() {
    override fun defaultSettings(): ParseSettings {
        return ParseSettings.preserveCase
    }

    override fun initialiseParse(
        input: BufferReader,
        baseUri: String?,
        parser: Parser,
    ) {
        super.initialiseParse(input, baseUri, parser)
        stack.add(doc) // place the document onto the stack. differs from HtmlTreeBuilder (not on stack)
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false) // as XML, we don't understand what whitespace is significant or not
    }

    fun parse(input: BufferReader, baseUri: String?): Document {
        return parse(input, baseUri, Parser(this))
    }

    fun parse(input: String, baseUri: String?): Document {
        return parse(BufferReader(input), baseUri, Parser(this))
    }

    override fun newInstance(): XmlTreeBuilder {
        return XmlTreeBuilder()
    }

    override fun defaultNamespace(): String {
        return NamespaceXml
    }

    override fun process(token: Token): Boolean {
        // start tag, end tag, doctype, comment, character, eof
        when (token.type) {
            Token.TokenType.StartTag -> insert(token.asStartTag())
            Token.TokenType.EndTag -> popStackToClose(token.asEndTag())
            Token.TokenType.Comment -> insert(token.asComment())
            Token.TokenType.Character -> insert(token.asCharacter())
            Token.TokenType.Doctype -> insert(token.asDoctype())
            Token.TokenType.EOF -> {}
            else -> Validate.fail("Unexpected token type: " + token.type)
        }
        return true
    }

    protected fun insertNode(node: Node) {
        currentElement().appendChild(node)
        onNodeInserted(node, null)
    }

    private fun insertNode(node: Node, token: Token?) {
        currentElement().appendChild(node)
        onNodeInserted(node, token)
    }

    fun insert(startTag: Token.StartTag): Element {
        val tag: Tag = tagFor(startTag.name(), settings)
        if (startTag.hasAttributes()) startTag.attributes!!.deduplicate(settings!!)
        val el = Element(tag, null, settings!!.normalizeAttributes(startTag.attributes))
        insertNode(el, startTag)
        if (startTag.isSelfClosing) {
            tag.setSelfClosing()
        } else {
            stack.add(el)
        }
        return el
    }

    fun insert(commentToken: Token.Comment) {
        val comment = Comment(commentToken.getData())
        var insert: Node = comment
        if (commentToken.bogus && comment.isXmlDeclaration()) {
            // xml declarations are emitted as bogus comments (which is right for html, but not xml)
            // so we do a bit of a hack and parse the data as an element to pull the attributes out
            // else, we couldn't parse it as a decl, so leave as a comment
            val decl: XmlDeclaration? = comment.asXmlDeclaration()
            if (decl != null) insert = decl
        }
        insertNode(insert, commentToken)
    }

    fun insert(token: Token.Character) {
        val data: String = token.data ?: ""
        insertNode(if (token.isCData()) CDataNode(data) else TextNode(data), token)
    }

    fun insert(d: Token.Doctype) {
        val doctypeNode = DocumentType(
            settings!!.normalizeTag(d.getName()),
            d.getPublicIdentifier(),
            d.getSystemIdentifier(),
        )
        doctypeNode.setPubSysKey(d.pubSysKey)
        insertNode(doctypeNode, d)
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    protected fun popStackToClose(endTag: Token.EndTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        val elName: String = settings!!.normalizeTag(endTag.tagName!!)
        var firstFound: Element? = null
        val bottom: Int = stack.size - 1
        val upper = if (bottom >= maxQueueDepth) bottom - maxQueueDepth else 0
        for (pos in stack.size - 1 downTo upper) {
            val next: Element? = stack[pos]
            if (next?.nodeName() == elName) {
                firstFound = next
                break
            }
        }
        if (firstFound == null) return // not found, skip
        for (pos in stack.size - 1 downTo 0) {
            val next: Element? = stack[pos]
            stack.removeAt(pos)
            if (next === firstFound) {
                onNodeClosed(next, endTag)
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

    fun parseFragment(inputFragment: String, baseUri: String?, parser: Parser): List<Node> {
        initialiseParse(BufferReader(inputFragment), baseUri, parser)
        runParser()
        return doc.childNodes()
    }

    companion object {
        private const val maxQueueDepth =
            256 // an arbitrary tension point between real XML and crafted pain
    }
}
