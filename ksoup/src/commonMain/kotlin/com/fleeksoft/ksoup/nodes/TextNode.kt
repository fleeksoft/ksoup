package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import okio.IOException

/**
 * A text node.
 *
 * @author Sabeeh, fleeksoft@gmail.com
 */
public open class TextNode(text: String) : LeafNode() {
    /**
     * Create a new TextNode representing the supplied (unencoded) text).
     *
     * @param text raw text
     * @see .createFromEncoded
     */
    init {
        value = text
    }

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
        if (parentNode != null) parentNode!!.addChildren(siblingIndex() + 1, tailNode)
        return tailNode
    }

    @Throws(IOException::class)
    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        val prettyPrint: Boolean = out.prettyPrint()
        val parent: Element? = if (parentNode is Element) parentNode as Element? else null
        val normaliseWhite = prettyPrint && !Element.preserveWhitespace(parentNode)
        val trimLikeBlock = parent != null && (parent.tag().isBlock || parent.tag().formatAsBlock())
        var trimLeading = false
        var trimTrailing = false
        if (normaliseWhite) {
            trimLeading = trimLikeBlock && siblingIndex == 0 || parentNode is Document
            trimTrailing = trimLikeBlock && nextSibling() == null

            // if this text is just whitespace, and the next node will cause an indent, skip this text:
            val next: Node? = nextSibling()
            val prev: Node? = previousSibling()
            val isBlank = isBlank()
            val couldSkip =
                next is Element && next.shouldIndent(out) || next is TextNode && next.isBlank() || prev is Element && (
                    prev.isBlock() || prev.nameIs("br")
                ) // br is a bit special - make sure we don't get a dangling blank line, but not a block otherwise wraps in head
            if (couldSkip && isBlank) return
            if (
                (prev == null && parent != null && parent.tag().formatAsBlock() && !isBlank) ||
                (out.outline() && siblingNodes().isNotEmpty() && !isBlank) ||
                (prev != null && prev.nameIs("br")) // special case wrap on inline <br> - doesn't make sense as a block tag
            ) {
                indent(accum, depth, out)
            }
        }
        Entities.escape(accum, coreValue(), out, false, normaliseWhite, trimLeading, trimTrailing)
    }

    @Throws(IOException::class)
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

        internal fun normaliseWhitespace(text: String): String {
            return StringUtil.normaliseWhitespace(text)
        }

        internal fun stripLeadingWhitespace(text: String): String {
            return text.replaceFirst("^\\s+".toRegex(), "")
        }

        internal fun lastCharIsWhitespace(sb: StringBuilder): Boolean {
            return sb.isNotEmpty() && sb[sb.length - 1] == ' '
        }
    }
}
