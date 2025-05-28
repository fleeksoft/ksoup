/*
 * Kotlin port of jsoup's Comment.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.QuietAppendable
import com.fleeksoft.ksoup.parser.Parser

/**
 * A comment node.
 *
 */
public class Comment(data: String) : LeafNode(data) {
    override fun nodeName(): String {
        return "#comment"
    }

    public fun getData(): String = coreValue()

    public fun setData(data: String?): Comment {
        coreValue(data)
        return this
    }

    override fun outerHtmlHead(accum: QuietAppendable, out: Document.OutputSettings) {
        accum
            .append("<!--")
            .append(getData())
            .append("-->")
    }

    override fun createClone(): Node {
        return Comment(value as String)
    }

    override fun clone(): Comment {
        return super.clone() as Comment
    }

    /**
     * Check if this comment looks like an XML Declaration. This is the case when the HTML parser sees an XML
     * declaration or processing instruction. Other than doctypes, those aren't part of HTML, and will be parsed as a
     * bogus comment.
     * @return true if it looks like, maybe, it's an XML Declaration.
     * @see #asXmlDeclaration()
     */
    public fun isXmlDeclaration(): Boolean {
        val data = getData()
        return isXmlDeclarationData(data)
    }

    /**
     * Attempt to cast this comment to an XML Declaration node.
     * @return an XML declaration if it could be parsed as one, null otherwise.
     */

    public fun asXmlDeclaration(): XmlDeclaration? {
        val fragment = "<" + getData() + ">"
        val parser = Parser.xmlParser()
        val nodes: List<Node> = parser.parseFragmentInput(fragment, null, "")
        if (!nodes.isEmpty() && nodes[0] is XmlDeclaration)
            return nodes[0] as XmlDeclaration
        return null
    }

    public companion object {
        private fun isXmlDeclarationData(data: String): Boolean {
            return data.length > 1 && (data.startsWith("!") || data.startsWith("?"))
        }
    }
}
