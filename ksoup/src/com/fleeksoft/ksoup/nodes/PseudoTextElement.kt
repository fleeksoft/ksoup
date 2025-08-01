/*
 * Kotlin port of jsoup's PseudoTextElement.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.QuietAppendable
import com.fleeksoft.ksoup.parser.Tag

/**
 * Represents a [TextNode] as an [Element], to enable text nodes to be selected with
 * the [com.fleeksoft.ksoup.select.Selector] `:matchText` syntax.
 */

@Deprecated("use {@link Element#selectNodes(String, Class)} instead, with selector of <code>::textnode</code> and class <code>TextNode</code>.")
internal class PseudoTextElement(tag: Tag, baseUri: String?, attributes: Attributes?) :
    Element(tag, baseUri, attributes) {
    override fun outerHtmlHead(accum: QuietAppendable, out: Document.OutputSettings) {
    }

    override fun outerHtmlTail(accum: QuietAppendable, out: Document.OutputSettings) {
    }
}
