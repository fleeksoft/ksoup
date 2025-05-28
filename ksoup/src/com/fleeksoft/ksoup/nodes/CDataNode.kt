/*
 * Kotlin port of jsoup's CDataNode.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.QuietAppendable

/**
 * A Character Data node, to support CDATA sections.
 */
public class CDataNode(text: String?) : TextNode(text!!) {
    override fun nodeName(): String {
        return "#cdata"
    }

    /**
     * Get the un-encoded, **non-normalized** text content of this CDataNode.
     * @return un-encoded, non-normalized text
     */
    override fun text(): String {
        return getWholeText()
    }

    override fun outerHtmlHead(accum: QuietAppendable, out: Document.OutputSettings) {
        accum
            .append("<![CDATA[")
            .append(getWholeText())
            .append("]]>")
    }

    override fun clone(): CDataNode {
        return this.clone()
    }
}
