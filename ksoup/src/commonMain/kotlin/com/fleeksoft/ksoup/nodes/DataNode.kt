package com.fleeksoft.ksoup.nodes

import okio.IOException

/**
 * A data node, for contents of style, script tags etc, where contents should not show in text().
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
public class DataNode(data: String) : LeafNode() {
    /**
     * Create a new DataNode.
     * @param data data contents
     */
    init {
        value = data
    }

    override fun nodeName(): String {
        return "#data"
    }

    public fun getWholeData(): String = coreValue()

    /**
     * Set the data contents of this node.
     * @param data unencoded data
     * @return this node, for chaining
     */
    public fun setWholeData(data: String?): DataNode {
        coreValue(data)
        return this
    }

    @Throws(IOException::class)
    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        if (out.syntax() == Document.OutputSettings.Syntax.xml) {
            // In XML mode, output data nodes as CDATA, so can parse as XML
            accum
                .append("<![CDATA[")
                .append(getWholeData())
                .append("]]>")
        } else {
            // In HTML, data is not escaped in return from data nodes, so " in script, style is plain
            accum.append(getWholeData())
        }
    }

    override fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
    }

    override fun toString(): String {
        return outerHtml()
    }

    override fun createClone(): Node {
        return DataNode(value as String)
    }

    override fun clone(): DataNode {
        return super.clone() as DataNode
    }
}
