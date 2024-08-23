package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Document.OutputSettings.Syntax

/**
 * A `<!DOCTYPE>` node.
 * Create a new doctype element.
 * @param name the doctype's name
 * @param publicId the doctype's public ID
 * @param systemId the doctype's system ID
 */
public class DocumentType(private val name: String, private val publicId: String, private val systemId: String) : LeafNode(name) {
    init {
        attr(Name, name)
        attr(PublicId, publicId)
        attr(SystemId, systemId)
        updatePubSyskey()
    }

    public fun setPubSysKey(value: String?) {
        if (value != null) attr(PubSysKey, value)
    }

    private fun updatePubSyskey() {
        if (has(PublicId)) {
            attr(PubSysKey, PUBLIC_KEY)
        } else if (has(SystemId)) {
            attr(PubSysKey, SYSTEM_KEY)
        }
    }

    /**
     * Get this doctype's name (when set, or empty string)
     * @return doctype name
     */
    public fun name(): String {
        return attr(Name)
    }

    /**
     * Get this doctype's Public ID (when set, or empty string)
     * @return doctype Public ID
     */
    public fun publicId(): String {
        return attr(PublicId)
    }

    /**
     * Get this doctype's System ID (when set, or empty string)
     * @return doctype System ID
     */
    public fun systemId(): String {
        return attr(SystemId)
    }

    override fun nodeName(): String {
        return Name
    }

    override fun outerHtmlHead(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
        // add a newline if the doctype has a preceding node (which must be a comment)
        if (_siblingIndex > 0 && out.prettyPrint()) accum.append('\n')

        if (out.syntax() == Syntax.html && !has(PublicId) && !has(SystemId)) {
            // looks like a html5 doctype, go lowercase for aesthetics
            accum.append("<!doctype")
        } else {
            accum.append("<!DOCTYPE")
        }
        if (has(Name)) accum.append(" ").append(attr(Name))
        if (has(PubSysKey)) accum.append(" ").append(attr(PubSysKey))
        if (has(PublicId)) accum.append(" \"").append(attr(PublicId)).append('"')
        if (has(SystemId)) accum.append(" \"").append(attr(SystemId)).append('"')
        accum.append('>')
    }

    override fun outerHtmlTail(
        accum: Appendable,
        depth: Int,
        out: Document.OutputSettings,
    ) {
    }

    override fun createClone(): Node {
        return DocumentType(this.name, this.publicId, this.systemId)
    }

    private fun has(attribute: String): Boolean {
        return !StringUtil.isBlank(attr(attribute))
    }

    public companion object {
        // todo needs a bit of a chunky cleanup. this level of detail isn't needed
        public const val PUBLIC_KEY: String = "PUBLIC"
        public const val SYSTEM_KEY: String = "SYSTEM"
        private const val Name: String = "#doctype"
        private const val PubSysKey: String = "pubSysKey" // PUBLIC or SYSTEM
        private const val PublicId: String = "publicId"
        private const val SystemId: String = "systemId"
        // todo: quirk mode from publicId and systemId
    }
}
