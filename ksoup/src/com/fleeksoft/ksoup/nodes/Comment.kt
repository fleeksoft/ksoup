package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.ParseSettings
import com.fleeksoft.ksoup.parser.Parser

/**
 * A comment node.
 *
 * @author Sabeeh, fleeksoft@gmail.com
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

    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        if (out.prettyPrint() && (
                    isEffectivelyFirst() && _parentNode is Element &&
                            (_parentNode as Element).tag()
                                .formatAsBlock() || out.outline()
                    )
        ) {
            indent(accum, depth, out)
        }
        accum
            .append("<!--")
            .append(getData())
            .append("-->")
    }

    override fun outerHtmlTail(accum: Appendable, depth: Int, out: Document.OutputSettings) {
    }

    override fun createClone(): Node {
        return Comment(value as String)
    }

    override fun clone(): Comment {
        return super.clone() as Comment
    }

    public fun isXmlDeclaration(): Boolean {
        val data = getData()
        return isXmlDeclarationData(data)
    }

    /**
     * Attempt to cast this comment to an XML Declaration node.
     * @return an XML declaration if it could be parsed as one, null otherwise.
     */

    public fun asXmlDeclaration(): XmlDeclaration? {
        val data = getData()
        var decl: XmlDeclaration? = null
        val declContent = data.substring(1, data.length - 1)
        // make sure this bogus comment is not immediately followed by another, treat as comment if so
        if (isXmlDeclarationData(declContent)) return null
        val fragment = "<$declContent>"
        // use the HTML parser not XML, so we don't get into a recursive XML Declaration on contrived data
        val doc: Document = Parser.htmlParser().settings(ParseSettings.preserveCase).parseInput(fragment, baseUri())
        if (doc.body().childrenSize() > 0) {
            val el: Element = doc.body().child(0)
            decl =
                XmlDeclaration(
                    NodeUtils.parser(doc).settings()!!.normalizeTag(el.tagName()),
                    data.startsWith("!"),
                )
            decl.attributes().addAll(el.attributes())
        }
        return decl
    }

    public companion object {
        private fun isXmlDeclarationData(data: String): Boolean {
            return data.length > 1 && (data.startsWith("!") || data.startsWith("?"))
        }
    }
}
