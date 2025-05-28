/*
 * Kotlin port of jsoup's XmlDeclaration.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.QuietAppendable
import com.fleeksoft.ksoup.internal.StringUtil

/**
 * An XML Declaration. Includes support for treating the declaration contents as pseudo‑attributes.
 *
 * @param name           name of the declaration
 * @param isDeclaration  true if a declaration (first char `!`), false for a processing instruction (first char `?`)
 */
class XmlDeclaration(name: String, private val isDeclaration: Boolean) : LeafNode(name) {

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
        getWholeDeclaration(QuietAppendable.wrap(sb), Document.OutputSettings())
        return StringUtil.releaseBuilder(sb).trim()
    }


    private fun getWholeDeclaration(accum: QuietAppendable, out: Document.OutputSettings) {
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

    override fun outerHtmlHead(accum: QuietAppendable, out: Document.OutputSettings) {
        accum
            .append("<")
            .append(if (isDeclaration) "!" else "?")
            .append(coreValue())
        getWholeDeclaration(accum, out)
        accum
            .append(if (isDeclaration) "" else "?")
            .append(">")
    }

    override fun outerHtmlTail(accum: QuietAppendable, out: Document.OutputSettings) {
    }

    override fun toString(): String {
        return outerHtml()
    }

    override fun createClone(): Node {
        return XmlDeclaration(this.value as String, this.isDeclaration)
    }

    override fun clone(): XmlDeclaration {
        return this.clone()
    }
}
