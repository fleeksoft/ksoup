package com.fleeksoft.ksoup.nodes

import okio.IOException

/**
 * A Character Data node, to support CDATA sections.
 */
class CDataNode(text: String?) : TextNode(text!!) {
    override fun nodeName(): String {
        return "#cdata"
    }

    /**
     * Get the unencoded, **non-normalized** text content of this CDataNode.
     * @return unencoded, non-normalized text
     */
    override fun text(): String {
        return getWholeText()
    }

    @Throws(IOException::class)
    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum
            .append("<![CDATA[")
            .append(getWholeText())
    }

    @Throws(IOException::class)
    override fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum.append("]]>")
    }

    override fun clone(): CDataNode {
        return this.clone() as CDataNode
    }
}
