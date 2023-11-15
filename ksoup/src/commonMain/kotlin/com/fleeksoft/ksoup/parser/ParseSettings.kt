package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.nodes.Attributes

/**
 * Controls parser case settings, to optionally preserve tag and/or attribute name case.
 */
public class ParseSettings
/**
 * Define parse settings.
 * @param tag preserve tag case?
 * @param attribute preserve attribute name case?
 */(private val preserveTagCase: Boolean, private val preserveAttributeCase: Boolean) {
    /**
     * Returns true if preserving tag name case.
     */
    public fun preserveTagCase(): Boolean {
        return preserveTagCase
    }

    /**
     * Returns true if preserving attribute case.
     */
    public fun preserveAttributeCase(): Boolean {
        return preserveAttributeCase
    }

    internal constructor(copy: ParseSettings?) : this(
        copy!!.preserveTagCase,
        copy.preserveAttributeCase,
    )

    /**
     * Normalizes a tag name according to the case preservation setting.
     */
    public fun normalizeTag(name: String): String {
        var name = name
        name = name.trim { it <= ' ' }
        if (!preserveTagCase) name = lowerCase(name)
        return name
    }

    /**
     * Normalizes an attribute according to the case preservation setting.
     */
    public fun normalizeAttribute(name: String): String {
        var name = name
        name = name.trim { it <= ' ' }
        if (!preserveAttributeCase) name = lowerCase(name)
        return name
    }

    /*@Nullable*/
    public fun normalizeAttributes(/*@Nullable*/ attributes: Attributes?): Attributes? {
        if (attributes != null && !preserveAttributeCase) {
            attributes.normalize()
        }
        return attributes
    }

    public companion object {
        /**
         * HTML default settings: both tag and attribute names are lower-cased during parsing.
         */
        public val htmlDefault: ParseSettings = ParseSettings(false, false)

        /**
         * Preserve both tag and attribute case.
         */
        public val preserveCase: ParseSettings = ParseSettings(true, true)

        /** Returns the normal name that a Tag will have (trimmed and lower-cased)  */
        public fun normalName(name: String?): String {
            return lowerCase(name!!.trim { it <= ' ' })
        }
    }
}
