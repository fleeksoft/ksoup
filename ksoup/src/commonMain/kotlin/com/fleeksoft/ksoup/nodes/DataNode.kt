package com.fleeksoft.ksoup.nodes

import korlibs.io.lang.IOException

/**
 * Create a new DataNode.
 * A data node, for contents of style, script tags etc, where contents should not show in text().
 *
 * @param data data contents
 */
public class DataNode(data: String) : LeafNode() {
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
        /* For XML output, escape the DataNode in a CData section. The data may contain pseudo-CData content if it was
        parsed as HTML, so don't double up Cdata. Output in polygot HTML / XHTML / XML format. */
        val data = getWholeData()
        if (out.syntax() === Document.OutputSettings.Syntax.xml && !data.contains("<![CDATA[")) {
            if (parentNameIs("script")) {
                accum.append("//<![CDATA[\n").append(data).append("\n//]]>")
            } else if (parentNameIs("style")) {
                accum.append("/*<![CDATA[*/\n").append(data).append("\n/*]]>*/")
            } else {
                accum.append("<![CDATA[").append(data).append("]]>")
            }
        } else {
            // In HTML, data is not escaped in the output of data nodes, so < and & in script, style is OK
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
