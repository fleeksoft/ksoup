package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.SerializationException
import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Document.OutputSettings.Syntax
import com.fleeksoft.ksoup.ported.KCloneable
import kotlinx.io.IOException

/**
 * A single key + value attribute. (Only used for presentation.)
 */
public open class Attribute : Map.Entry<String, String?>, KCloneable<Attribute> {
    private var attributeKey: String

    private var attributeValue: String?

    internal var parent: Attributes?

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value (may be null)
     * @see .createFromEncoded
     */
    public constructor(key: String, value: String?) : this(key, value, null)

    /**
     * Get the attribute key.
     * @return the attribute key
     */
    final override val key: String
        get() = attributeKey

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value (may be null)
     * @param parent the containing Attributes (this Attribute is not automatically added to said Attributes)
     * @see .createFromEncoded
     */
    public constructor(
        key: String,
        value: String?,
        parent: Attributes?,
    ) {
        var sKey = key
        sKey = sKey.trim { it <= ' ' }
        Validate.notEmpty(sKey) // trimming could potentially make empty, so validate here
        this.attributeKey = sKey
        this.attributeValue = value
        this.parent = parent
    }

    /**
     * Set the attribute key; case is preserved.
     * @param key the new key; must not be null
     */
    internal fun setKey(key: String) {
        val trimmedKey = key.trim { it <= ' ' }
        Validate.notEmpty(trimmedKey) // trimming could potentially make empty, so validate here
        if (parent != null) {
            val i = parent!!.indexOfKey(this.key)
            if (i != Attributes.NotFound) {
                val oldKey = parent!!.keys[i]!!
                parent!!.keys[i] = trimmedKey

                // if tracking source positions, update the key in the range map
                val ranges: MutableMap<String, Range.AttributeRange>? = parent!!.getRanges()
                if (ranges != null) {
                    val range: Range.AttributeRange? = ranges.remove(oldKey)
                    if (range != null) {
                        ranges[trimmedKey] = range
                    }
                }
            }
        }
        this.attributeKey = trimmedKey
    }

    /**
     * Get the attribute value. Will return an empty string if the value is not set.
     * @return the attribute value
     */
    override val value: String
        get() = Attributes.checkNotNull(attributeValue)

    /**
     * Check if this Attribute has a value. Set boolean attributes have no value.
     * @return if this is a boolean attribute / attribute without a value
     */
    public fun hasDeclaredValue(): Boolean {
        return attributeValue != null
    }

    /**
     * Set the attribute value.
     * @param newValue the new attribute value; may be null (to set an enabled boolean attribute)
     * @return the previous value (if was null; an empty string)
     */
    public fun setValue(newValue: String?): String {
        var oldVal = this.attributeValue
        if (parent != null) {
            val i: Int = parent!!.indexOfKey(attributeKey)
            if (i != Attributes.NotFound) {
                oldVal = parent!![attributeKey] // trust the container more
                parent!!.vals[i] = newValue
            }
        }
        this.attributeValue = newValue
        return Attributes.checkNotNull(oldVal)
    }

    /**
     * Get the HTML representation of this attribute; e.g. `href="index.html"`.
     * @return HTML
     */
    public fun html(): String {
        val sb: StringBuilder = StringUtil.borrowBuilder()
        try {
            html(sb, Document("").outputSettings())
        } catch (exception: IOException) {
            throw SerializationException(exception)
        }
        return StringUtil.releaseBuilder(sb)
    }

    /**
     * Get the source ranges (start to end positions) in the original input source from which this attribute's **name**
     * and **value** were parsed.
     *
     * Position tracking must be enabled prior to parsing the content.
     * @return the ranges for the attribute's name and value, or `untracked` if the attribute does not exist or its range
     * was not tracked.
     * @see com.fleeksoft.ksoup.parser.Parser.setTrackPosition
     * @see Attributes.sourceRange
     * @see Node.sourceRange
     * @see Element.endSourceRange
     */
    public fun sourceRange(): Range.AttributeRange {
        if (parent == null) return Range.AttributeRange.UntrackedAttr
        return parent!!.sourceRange(key)!!
    }

    protected fun html(
        accum: Appendable,
        out: Document.OutputSettings,
    ) {
        html(attributeKey, attributeValue, accum, out)
    }

    /**
     * Get the string representation of this attribute, implemented as [.html].
     * @return string
     */
    override fun toString(): String {
        return html()
    }

    /**
     * Collapsible if it's a boolean attribute and value is empty or same as name
     *
     * @param out output settings
     * @return Returns whether collapsible or not
     */
    protected fun shouldCollapseAttribute(out: Document.OutputSettings): Boolean {
        return shouldCollapseAttribute(attributeKey, attributeValue, out)
    }

    override fun equals(other: Any?): Boolean { // note parent not considered
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        val attribute: Attribute = other as Attribute
        if (attributeKey != attribute.attributeKey) return false
        return if (attributeValue != null) attributeValue == attribute.attributeValue else attribute.attributeValue == null
    }

    override fun hashCode(): Int { // note parent not considered
        var result = attributeKey.hashCode()
        result = 31 * result + if (attributeValue != null) attributeValue.hashCode() else 0
        return result
    }

    override fun clone(): Attribute {
        val attribute = Attribute(attributeKey, attributeValue)
        attribute.parent = this.parent
        return attribute
    }

    public fun isDataAttribute(): Boolean {
        return isDataAttribute(key)
    }

    /**
     * Is this an internal attribute? Internal attributes can be fetched by key, but are not serialized.
     * @return if an internal attribute.
     */
    public fun isInternal(): Boolean {
        return Attributes.isInternalKey(key)
    }

    internal companion object {
        private val booleanAttributes =
            arrayOf(
                "allowfullscreen",
                "async",
                "autofocus",
                "checked",
                "compact",
                "declare",
                "default",
                "defer",
                "disabled",
                "formnovalidate",
                "hidden",
                "inert",
                "ismap",
                "itemscope",
                "multiple",
                "muted",
                "nohref",
                "noresize",
                "noshade",
                "novalidate",
                "nowrap",
                "open",
                "readonly",
                "required",
                "reversed",
                "seamless",
                "selected",
                "sortable",
                "truespeed",
                "typemustmatch",
            )

        protected fun html(
            key: String,
            value: String?,
            accum: Appendable,
            out: Document.OutputSettings,
        ) {
            val resultKey: String = getValidKey(key, out.syntax()) ?: return // can't write it :(
            htmlNoValidate(resultKey, value, accum, out)
        }

        fun htmlNoValidate(
            key: String,
            value: String?,
            accum: Appendable,
            out: Document.OutputSettings,
        ) {
            // structured like this so that Attributes can check we can write first, so it can add whitespace correctly
            accum.append(key)
            if (!shouldCollapseAttribute(key, value, out)) {
                accum.append("=\"")
                Entities.escape(
                    accum,
                    Attributes.checkNotNull(value),
                    out,
                    inAttribute = true,
                    normaliseWhite = false,
                    stripLeadingWhite = false,
                    trimTrailing = false,
                )
                accum.append('"')
            }
        }

        private val xmlKeyValid: Regex =
            Regex("[a-zA-Z_:][-a-zA-Z0-9_:.]*")
        private val xmlKeyReplace: Regex =
            Regex("[^-a-zA-Z0-9_:.]")
        private val htmlKeyValid: Regex =
            Regex("[^\\x00-\\x1f\\x7f-\\x9f \"'/=]+")
        private val htmlKeyReplace: Regex =
            Regex("[\\x00-\\x1f\\x7f-\\x9f \"'/=]")

        fun getValidKey(
            key: String,
            syntax: Syntax,
        ): String? {
            return when (syntax) {
                Syntax.xml -> {
                    if (!xmlKeyValid.matches(key)) {
                        val newKey = xmlKeyReplace.replace(key, "")
                        if (xmlKeyValid.matches(newKey)) newKey else null
                    } else {
                        key
                    }
                }

                Syntax.html -> {
                    if (!htmlKeyValid.matches(key)) {
                        val newKey = htmlKeyReplace.replace(key, "")
                        if (htmlKeyValid.matches(newKey)) newKey else null
                    } else {
                        key
                    }
                }
            }
        }

        /**
         * Create a new Attribute from an unencoded key and a HTML attribute encoded value.
         * @param unencodedKey assumes the key is not encoded, as can be only run of simple \w chars.
         * @param encodedValue HTML attribute encoded value
         * @return attribute
         */
        fun createFromEncoded(
            unencodedKey: String,
            encodedValue: String,
        ): Attribute {
            val value: String = Entities.unescape(encodedValue, true)
            return Attribute(unencodedKey, value, null) // parent will get set when Put
        }

        protected fun isDataAttribute(key: String): Boolean {
            return key.startsWith(Attributes.dataPrefix) && key.length > Attributes.dataPrefix.length
        }

        // collapse unknown foo=null, known checked=null, checked="", checked=checked; write out others
        protected fun shouldCollapseAttribute(
            key: String,
            value: String?,
            out: Document.OutputSettings,
        ): Boolean {
            return out.syntax() === Syntax.html &&
                (
                    value == null || (
                        value.isEmpty() ||
                            value.equals(
                                key,
                                ignoreCase = true,
                            )
                    ) && isBooleanAttribute(key)
                )
        }

        /**
         * Checks if this attribute name is defined as a boolean attribute in HTML5
         */
        fun isBooleanAttribute(key: String): Boolean {
            return booleanAttributes.toList().binarySearch { it.compareTo(key.lowercase()) } >= 0
        }
    }
}
