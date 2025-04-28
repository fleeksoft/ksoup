/*
 * Kotlin port of jsoup's XmlTreeBuilder.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.Reader
import com.fleeksoft.io.StringReader
import com.fleeksoft.ksoup.internal.SharedConstants
import com.fleeksoft.ksoup.nodes.*
import com.fleeksoft.ksoup.parser.Parser.Companion.NamespaceXml


/**
 * Use the `XmlTreeBuilder` when you want to parse XML without any of the HTML DOM rules being applied to the
 * document.
 *
 * Usage example: `Document xmlDoc = Ksoup.parse(html, baseUrl, Parser.xmlParser());`
 *
 */
public open class XmlTreeBuilder : TreeBuilder() {
    private val namespacesStack: ArrayDeque<HashMap<String, String>> = ArrayDeque<HashMap<String, String>>() // stack of namespaces, prefix => urn

    override fun defaultSettings(): ParseSettings {
        return ParseSettings.preserveCase
    }

    override fun initialiseParse(input: Reader, baseUri: String, parser: Parser) {
        super.initialiseParse(input, baseUri, parser)
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false) // as XML, we don't understand what whitespace is significant or not

        namespacesStack.clear()
        val ns = hashMapOf<String, String>()
        ns["xml"] = NamespaceXml
        ns[""] = NamespaceXml
        namespacesStack.add(ns)
    }

    override fun initialiseParseFragment(context: Element?) {
        super.initialiseParseFragment(context)
        if (context == null) return

        // transition to the tag's text state if available
        val textState: TokeniserState? = context.tag().textState()
        if (textState != null) tokeniser?.transition(textState)

        // reconstitute the namespace stack by traversing the element and its parents (top down)
        val chain = context.parents() // Assuming 'parents()' returns a MutableList<Element>
        chain.add(0, context)
        for (i in chain.size - 1 downTo 0) {
            val el = chain[i]
            val namespaces = HashMap(namespacesStack.first())
            namespacesStack.addFirst(namespaces)
            if (el.attributesSize() > 0) {
                processNamespaces(el.attributes(), namespaces)
            }
        }
    }


    override fun completeParseFragment(): List<Node> {
        return doc.childNodes()
    }

    public fun parse(input: Reader, baseUri: String? = null): Document {
        return parse(input, baseUri ?: "", Parser(this))
    }

    public fun parse(input: String, baseUri: String? = null): Document {
        return parse(StringReader(input), baseUri ?: "", Parser(this))
    }

    override fun newInstance(): XmlTreeBuilder {
        return XmlTreeBuilder()
    }

    override fun defaultNamespace(): String {
        return NamespaceXml
    }

    public override fun defaultTagSet(): TagSet {
        return TagSet() // an empty tagset
    }

    override fun process(token: Token): Boolean {
        currentToken = token

        // start tag, end tag, doctype, xmldecl, comment, character, eof
        when (token.type) {
            Token.TokenType.StartTag -> insertElementFor(token.asStartTag())
            Token.TokenType.EndTag -> popStackToClose(token.asEndTag())
            Token.TokenType.Comment -> insertCommentFor(token.asComment())
            Token.TokenType.Character -> insertCharacterFor(token.asCharacter())
            Token.TokenType.Doctype -> insertDoctypeFor(token.asDoctype())
            Token.TokenType.XmlDecl -> insertXmlDeclarationFor(token.asXmlDecl())
            Token.TokenType.EOF -> {}
        }
        return true
    }

    fun insertElementFor(startTag: Token.StartTag) {
        // handle namespace for tag
        val namespaces = HashMap(namespacesStack.firstOrNull() ?: hashMapOf())
        namespacesStack.addFirst(namespaces)

        val attributes = startTag.attributes
        if (attributes != null) {
            attributes.deduplicate(settings)
            processNamespaces(attributes, namespaces)
            applyNamespacesToAttributes(attributes, namespaces)
        }

        val tagName = startTag.tagName.value()
        val ns = resolveNamespace(tagName, namespaces)
        val tag = tagFor(tagName, startTag.normalName!!, ns!!, settings)
        val el = Element(tag, null, settings.normalizeAttributes(attributes))
        currentElement().appendChild(el)
        push(el)

        when {
            startTag.selfClosing -> {
                tag.setSeenSelfClose()
                pop() // push & pop ensures onNodeInserted & onNodeClosed
            }

            tag.isEmpty() -> {
                pop() // custom defined void tag
            }

            else -> {
                val textState = tag.textState()
                if (textState != null) tokeniser?.transition(textState)
            }
        }
    }

    public fun insertLeafNode(node: LeafNode?) {
        currentElement().appendChild(node!!)
        onNodeInserted(node)
    }

    public fun insertCommentFor(commentToken: Token.Comment) {
        val comment = Comment(commentToken.getData())
        insertLeafNode(comment)
    }

    public fun insertCharacterFor(token: Token.Character) {
        val data: String = token.getData()
        val node = if (token.isCData()) CDataNode(data)
        else if (currentElement().tag().`is`(Tag.Data)) DataNode(data)
        else TextNode(data)
        insertLeafNode(node)
    }

    public fun insertDoctypeFor(token: Token.Doctype) {
        val doctypeNode =
            DocumentType(
                settings.normalizeTag(token.getName()),
                token.getPublicIdentifier(),
                token.getSystemIdentifier(),
            )
        doctypeNode.setPubSysKey(token.pubSysKey)
        insertLeafNode(doctypeNode)
    }

    fun insertXmlDeclarationFor(token: Token.XmlDecl) {
        val decl = XmlDeclaration(token.name(), token.isDeclaration)
        token.attributes?.let { decl.attributes().addAll(it) }
        insertLeafNode(decl)
    }

    override fun pop(): Element {
        namespacesStack.removeAt(0)
        return super.pop()
    }

    /**
     * If the stack contains an element with this tag's name, pop up the stack to remove the first occurrence. If not
     * found, skips.
     *
     * @param endTag tag to close
     */
    private fun popStackToClose(endTag: Token.EndTag) {
        // like in HtmlTreeBuilder - don't scan up forever for very (artificially) deeply nested stacks
        val elName = settings.normalizeTag(endTag.name())
        var firstFound: Element? = null

        val bottom: Int = getStack().size - 1
        val upper =
            if (bottom >= maxQueueDepth) bottom - maxQueueDepth else 0

        for (pos in getStack().size - 1 downTo upper) {
            val next = getStack()[pos]
            if (next!!.nodeName() == elName) {
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
        const val XmlnsKey: String = "xmlns"
        const val XmlnsPrefix: String = "xmlns:"

        fun processNamespaces(attributes: Attributes, namespaces: HashMap<String, String>) {
            // process attributes for namespaces (xmlns, xmlns:)
            for (attr in attributes) {
                val key: String = attr.key
                val value: String = attr.value
                if (key == XmlnsKey) {
                    namespaces.put("", value) // new default for this level
                } else if (key.startsWith(XmlnsPrefix)) {
                    val nsPrefix = key.substring(XmlnsPrefix.length)
                    namespaces.put(nsPrefix, value)
                }
            }
        }

        private fun applyNamespacesToAttributes(attributes: Attributes, namespaces: HashMap<String, String>) {
            // second pass, apply namespace to attributes. Collects them first then adds (as userData is an attribute)
            val attrPrefix: MutableMap<String, String> = HashMap()
            for (attr in attributes) {
                val prefix = attr.prefix()
                if (!prefix.isEmpty()) {
                    if (prefix == XmlnsKey) continue
                    val ns = namespaces[prefix]
                    if (ns != null) attrPrefix.put(SharedConstants.XmlnsAttr + prefix, ns)
                }
            }
            for (entry in attrPrefix.entries) attributes.userData(entry.key, entry.value)
        }

        private fun resolveNamespace(tagName: String, namespaces: HashMap<String, String>): String? {
            var ns = namespaces[""]
            val pos = tagName.indexOf(':')
            if (pos > 0) {
                val prefix = tagName.substring(0, pos)
                if (namespaces.containsKey(prefix)) ns = namespaces[prefix]
            }
            return ns
        }
    }
}
