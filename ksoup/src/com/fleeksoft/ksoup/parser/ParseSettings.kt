/*
 * Kotlin port of jsoup's ParseSettings.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.internal.Normalizer.lowerCase
import com.fleeksoft.ksoup.internal.Normalizer.normalize
import com.fleeksoft.ksoup.nodes.Attributes
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Controls parser case settings, to optionally preserve tag and/or attribute name case.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
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
        var trimmedName = name.trim { it <= ' ' }
        if (!preserveTagCase) trimmedName = lowerCase(trimmedName)
        return trimmedName
    }

    /**
     * Normalizes an attribute according to the case preservation setting.
     */
    public fun normalizeAttribute(name: String): String {
        var trimmedName = name.trim { it <= ' ' }
        if (!preserveAttributeCase) trimmedName = lowerCase(trimmedName)
        return trimmedName
    }

    
    public fun normalizeAttributes(attributes: Attributes?) {
        if (!preserveAttributeCase) {
            attributes?.normalize()
        }
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
            return normalize(name!!.trim { it <= ' ' })
        }
    }
}
