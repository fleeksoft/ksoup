/*
 * Kotlin port of jsoup's TextNode.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil


public open class TextNode(text: String) : LeafNode(text) {

    override fun nodeName(): String {
        return "#text"
    }

    /**
     * Get the text content of this text node.
     * @return Unencoded, normalised text.
     * @see TextNode.getWholeText
     */
    public open fun text(): String {
        return StringUtil.normaliseWhitespace(getWholeText())
    }

    /**
     * Set the text content of this text node.
     * @param text unencoded text
     * @return this, for chaining
     */
    public fun text(text: String?): TextNode {
        coreValue(text)
        return this
    }

    public fun getWholeText(): String = coreValue()

    public fun isBlank(): Boolean = StringUtil.isBlank(coreValue())

    /**
     * Split this text node into two nodes at the specified string offset. After splitting, this node will contain the
     * original text up to the offset, and will have a new text node sibling containing the text after the offset.
     * @param offset string offset point to split node at.
     * @return the newly created text node containing the text after the offset.
     */
    public fun splitText(offset: Int): TextNode {
        val text: String = coreValue()
        Validate.isTrue(offset >= 0, "Split offset must be not be negative")
        Validate.isTrue(
            offset < text.length,
            "Split offset must not be greater than current text length",
        )
        val head = text.substring(0, offset)
        val tail = text.substring(offset)
        text(head)
        val tailNode = TextNode(tail)
        if (_parentNode != null) _parentNode!!.addChildren(siblingIndex() + 1, tailNode)
        return tailNode
    }

    override fun outerHtmlHead(accum: Appendable, out: Document.OutputSettings) {
        Entities.escape(accum, coreValue(), out, Entities.ForText)
    }

    override fun toString(): String {
        return outerHtml()
    }

    override fun createClone(): Node {
        val clone = TextNode("")
        clone.value = this.value
        return clone
    }

    override fun clone(): TextNode {
        return super.clone() as TextNode
    }

    public companion object {
        /**
         * Create a new TextNode from HTML encoded (aka escaped) data.
         * @param encodedText Text containing encoded HTML (e.g. `&lt;`)
         * @return TextNode containing unencoded data (e.g. `<`)
         */
        public fun createFromEncoded(encodedText: String): TextNode {
            val text: String = Entities.unescape(encodedText)
            return TextNode(text)
        }

        public fun normaliseWhitespace(text: String): String {
            return StringUtil.normaliseWhitespace(text)
        }

        public fun stripLeadingWhitespace(text: String): String {
            return text.replaceFirst("^\\s+".toRegex(), "")
        }

        internal fun lastCharIsWhitespace(sb: StringBuilder): Boolean {
            return sb.isNotEmpty() && sb[sb.length - 1] == ' '
        }
    }
}
