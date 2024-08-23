package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.ported.exception.IOException
import com.fleeksoft.ksoup.ported.exception.SerializationException

/**
 * An XML Declaration.
 */
public class XmlDeclaration(
    name: String,
    private val isProcessingInstruction: Boolean // <! if true, <? if false, declaration (and last data char should be ?)
) : LeafNode(name) {

    override fun nodeName(): String {
        return "#declaration"
    }

    /**
     * Get the name of this declaration.
     * @return name of this declaration.
     */
    public fun name(): String {
        return coreValue()
    }

    public fun getWholeDeclaration(): String {
        val sb: StringBuilder = StringUtil.borrowBuilder()
        try {
            getWholeDeclaration(sb, Document.OutputSettings())
        } catch (e: IOException) {
            throw SerializationException(e)
        }
        return StringUtil.releaseBuilder(sb).trim()
    }


    private fun getWholeDeclaration(
        accum: Appendable,
        out: Document.OutputSettings,
    ) {
        for (attribute in attributes()) {
            val key: String = attribute.key
            val value: String = attribute.value
            if (key != nodeName()) { // skips coreValue (name)
                accum.append(' ')
                // basically like Attribute, but skip empty vals in XML
                accum.append(key)
                if (value.isNotEmpty()) {
                    accum.append("=\"")
                    Entities.escape(accum, value, out, Entities.ForAttribute)
                    accum.append('"')
                }
            }
        }
    }

    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        accum
            .append("<")
            .append(if (isProcessingInstruction) "!" else "?")
            .append(coreValue())
        getWholeDeclaration(accum, out)
        accum
            .append(if (isProcessingInstruction) "!" else "?")
            .append(">")
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
        return XmlDeclaration(this.value as String, this.isProcessingInstruction)
    }

    override fun clone(): XmlDeclaration {
        return this.clone()
    }
}
