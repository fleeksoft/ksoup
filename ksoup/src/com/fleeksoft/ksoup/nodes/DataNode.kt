package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.Unbaser

/**
 * Create a new DataNode.
 * A data node, for contents of style, script tags etc, where contents should not show in text().
 *
 * @param data data contents
 */
public class DataNode(data: String) : LeafNode(data) {

    public val isPacked: Boolean by lazy {
        parentNameIs("script") && getWholeData().contains(packedRegex)
    }

    override fun nodeName(): String {
        return "#data"
    }

    public fun getWholeData(): String = coreValue()

    /**
     * Set the data contents of this node.
     * @param data un-encoded data
     * @return this node, for chaining
     */
    public fun setWholeData(data: String?): DataNode {
        coreValue(data)
        return this
    }

    public fun getUnpackedData(): String {
        return if (isPacked) {
            getWholeData().replace(packedRegex) { packed ->
                packedExtractRegex.findAll(packed.value).mapNotNull { matchResult ->
                    val payload = matchResult.groups[1]?.value
                    val symtab = matchResult.groups[4]?.value?.split('|')
                    val radix = matchResult.groups[2]?.value?.toIntOrNull() ?: 10
                    val count = matchResult.groups[3]?.value?.toIntOrNull()
                    val unbaser = Unbaser(radix)

                    if (symtab == null || count == null || symtab.size != count) {
                        null
                    } else {
                        payload?.replace(unpackReplaceRegex) { match ->
                            val word = match.value
                            val unbased = symtab[unbaser.unbase(word)]
                            unbased.ifEmpty { word }
                        }
                    }
                }.joinToString(separator = "")
            }
        } else {
            getWholeData()
        }
    }

    public override fun outerHtmlHead(accum: Appendable, depth: Int, out: Document.OutputSettings) {
        /* For XML output, escape the DataNode in a CData section. The data may contain pseudo-CData content if it was
        parsed as HTML, so don't double up Cdata. Output in polyglot HTML / XHTML / XML format. */
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

    override fun outerHtmlTail(accum: Appendable, depth: Int, out: Document.OutputSettings) {
    }

    override fun createClone(): Node {
        return DataNode(value as String)
    }

    override fun clone(): DataNode {
        return super.clone() as DataNode
    }

    companion object {
        /**
         * Regex to detect packed functions.
         */
        private val packedRegex = Regex("eval[(]function[(]p,a,c,k,e,[rd][)][{].*?[}][)]{2}", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        /**
         * Regex to get and group the packed javascript.
         * Needed to get information and unpack the code.
         */
        private val packedExtractRegex = Regex("[}][(]'(.*)', *(\\d+), *(\\d+), *'(.*?)'[.]split[(]'[|]'[)]", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

        /**
         * Matches function names and variables to de-obfuscate the code.
         */
        private val unpackReplaceRegex = Regex("\\b\\w+\\b", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    }
}
