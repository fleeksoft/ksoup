package com.fleeksoft.ksoup.nodes

/**
A node that does not hold any children. E.g.: {@link TextNode}, {@link DataNode}, {@link Comment}.
 */
public abstract class LeafNode : Node {
    // either a string value, or an attribute map (in the rare case multiple attributes are set)
    internal var value: Any? = null

    constructor() {
        value = ""
    }

    constructor(coreValue: String) {
        value = coreValue
    }

    override fun hasAttributes(): Boolean {
        return value is Attributes
    }

    override fun attributes(): Attributes {
        ensureAttributes()
        return value as Attributes
    }

    private fun ensureAttributes() {
        if (!hasAttributes()) {
            val coreValue = value as? String
            val attributes = Attributes()
            value = attributes
            attributes.put(nodeName(), coreValue)
        }
    }

    public fun coreValue(): String {
        return attr(nodeName())
    }

    public fun coreValue(value: String?) {
        attr(nodeName(), value)
    }

    override fun attr(attributeKey: String): String {
        return if (!hasAttributes()) {
            if (nodeName() == attributeKey) value as String else EmptyString
        } else {
            super.attr(attributeKey)
        }
    }

    override fun attr(
        attributeKey: String,
        attributeValue: String?,
    ): Node {
        if (!hasAttributes() && attributeKey == nodeName()) {
            this.value = attributeValue
        } else {
            ensureAttributes()
            super.attr(attributeKey, attributeValue)
        }
        return this
    }

    override fun hasAttr(attributeKey: String): Boolean {
        ensureAttributes()
        return super.hasAttr(attributeKey)
    }

    override fun removeAttr(attributeKey: String): Node {
        ensureAttributes()
        return super.removeAttr(attributeKey)
    }

    override fun absUrl(attributeKey: String): String {
        ensureAttributes()
        return super.absUrl(attributeKey)
    }

    override fun baseUri(): String {
        return if (_parentNode != null) _parentNode!!.baseUri() else ""
    }

    protected override fun doSetBaseUri(baseUri: String?) {
        // noop
    }

    override fun childNodeSize(): Int {
        return 0
    }

    override fun empty(): Node {
        return this
    }

    public override fun ensureChildNodes(): MutableList<Node> {
        return EmptyNodes
    }

    protected override fun doClone(parent: Node?): LeafNode {
        val clone = super.doClone(parent) as LeafNode

        // Object value could be plain string or attributes - need to clone
        if (hasAttributes()) clone.value = (value as Attributes?)!!.clone()
        return clone
    }
}
