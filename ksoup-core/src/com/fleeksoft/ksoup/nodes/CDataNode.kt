package com.fleeksoft.ksoup.nodes

/**
 * A Character Data node, to support CDATA sections.
 */
public class CDataNode(text: String?) : TextNode(text!!) {
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

    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum
            .append("<![CDATA[")
            .append(getWholeText())
    }

    override fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum.append("]]>")
    }

    override fun clone(): CDataNode {
        return this.clone()
    }
}
