package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.parser.Tag

/**
 * Represents a [TextNode] as an [Element], to enable text nodes to be selected with
 * the [com.fleeksoft.ksoup.select.Selector] `:matchText` syntax.
 */
internal class PseudoTextElement(tag: Tag, baseUri: String?, attributes: Attributes?) :
    Element(tag, baseUri, attributes) {
    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
    }

    override fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
    }
}
