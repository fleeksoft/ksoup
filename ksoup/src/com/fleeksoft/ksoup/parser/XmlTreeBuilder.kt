package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceXml
import com.fleeksoft.ksoup.ported.io.Reader
import com.fleeksoft.ksoup.ported.io.StringReader

/**
 * Use the `XmlTreeBuilder` when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 *
 * Usage example: `Document xmlDoc = Ksoup.parse(html, baseUrl, Parser.xmlParser());`
 *
 * @author Sabeeh
 */
public open class XmlTreeBuilder : TreeBuilder() {
    override fun defaultSettings(): ParseSettings {
        return ParseSettings.preserveCase
    }

    override fun initialiseParse(
        input: Reader,
        baseUri: String,
        parser: Parser,
    ) {
        super.initialiseParse(input, baseUri, parser)
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false) // as XML, we don't understand what whitespace is significant or not
    }

    override fun completeParseFragment(): List<Node> {
        return doc.childNodes()
    }

    public fun parse(
        input: Reader,
        baseUri: String? = null,
    ): Document {
        return parse(input, baseUri ?: "", Parser(this))
    }

    public fun parse(
        input: String,
        baseUri: String? = null,
    ): Document {
        return parse(StringReader(input), baseUri ?: "", Parser(this))
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
        }
        return true
    }

    public fun insertElementFor(startTag: Token.StartTag) {
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

    public fun insertLeafNode(node: LeafNode?) {
        currentElement().appendChild(node!!)
        onNodeInserted(node)
    }

    public fun insertCommentFor(commentToken: Token.Comment) {
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

    public fun insertCharacterFor(token: Token.Character) {
        val data: String = token.data!!
        insertLeafNode(if (token.isCData()) CDataNode(data) else TextNode(data))
    }

    public fun insertDoctypeFor(token: Token.Doctype) {
        val doctypeNode =
            DocumentType(
                settings!!.normalizeTag(token.getName()),
                token.getPublicIdentifier(),
                token.getSystemIdentifier(),
            )
        doctypeNode.setPubSysKey(token.pubSysKey)
        insertLeafNode(doctypeNode)
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    private fun popStackToClose(endTag: Token.EndTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        val elName = settings!!.normalizeTag(endTag.tagName!!)
        var firstFound: Element? = null

        val bottom: Int = getStack().size - 1
        val upper =
            if (bottom >= XmlTreeBuilder.maxQueueDepth) bottom - XmlTreeBuilder.maxQueueDepth else 0

        for (pos in getStack().size - 1 downTo upper) {
            val next = _stack!![pos]!!
            if (next.nodeName() == elName) {
                firstFound = next
                break
            }
        }
        if (firstFound == null) return // not found, skip

        for (pos in getStack().size - 1 downTo 0) {
            val next = pop()
            if (next === firstFound) {
                break
            }
        }
    }

    public companion object {
        private const val maxQueueDepth = 256 // an arbitrary tension point between real XML and crafted pain
    }
}
