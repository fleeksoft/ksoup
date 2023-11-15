package com.fleeksoft.ksoup.nodes

import okio.IOException
import com.fleeksoft.ksoup.SerializationException
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil

/**
 * An XML Declaration.
 */
class XmlDeclaration(name: String, isProcessingInstruction: Boolean) : LeafNode() {
    // todo this impl isn't really right, the data shouldn't be attributes, just a run of text after the name
    private val isProcessingInstruction: Boolean

    /**
     * Create a new XML declaration
     * @param name of declaration
     * @param isProcessingInstruction is processing instruction
     */
    init {
        value = name
        this.isProcessingInstruction = isProcessingInstruction
    }

    override fun nodeName(): String {
        return "#declaration"
    }

    /**
     * Get the name of this declaration.
     * @return name of this declaration.
     */
    fun name(): String {
        return coreValue()!!
    }

    val wholeDeclaration: String
        /**
         * Get the unencoded XML declaration.
         * @return XML declaration
         */
        get() {
            val sb: StringBuilder = StringUtil.borrowBuilder()
            try {
                getWholeDeclaration(sb, Document.OutputSettings())
            } catch (e: IOException) {
                throw SerializationException(e)
            }
            return StringUtil.releaseBuilder(sb).trim()
        }

    @Throws(IOException::class)
    private fun getWholeDeclaration(accum: Appendable, out: Document.OutputSettings) {
        for (attribute in attributes()) {
            val key: String = attribute.key
            val value: String = attribute.value
            if (key != nodeName()) { // skips coreValue (name)
                accum.append(' ')
                // basically like Attribute, but skip empty vals in XML
                accum.append(key)
                if (!value.isEmpty()) {
                    accum.append("=\"")
                    Entities.escape(accum, value, out, true, false, false, false)
                    accum.append('"')
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun outerHtmlHead(accum: Appendable, depth: Int, out: Document.OutputSettings) {
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
        return this.clone() as XmlDeclaration
    }
}
