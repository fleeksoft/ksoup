/*
 * Kotlin port of jsoup's Normalizer.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.internal

import com.fleeksoft.ksoup.nodes.Attribute
import com.fleeksoft.ksoup.nodes.Document

/**
 * Util methods for normalizing strings. Ksoup internal use only, please don't depend on this API.
 */
object Normalizer {
    /** Drops the input string to lower case.  */
    fun lowerCase(input: String?): String {
        return input?.lowercase() ?: ""
    }

    /** Lower-cases and trims the input string.  */
    fun normalize(input: String?): String {
        return lowerCase(input).trim { it <= ' ' }
    }

    /** If a string literal, just lower case the string; otherwise lower-case and trim.  */
    fun normalize(input: String?, isStringLiteral: Boolean): String {
        return if (isStringLiteral) lowerCase(input) else normalize(input)
    }

    /** Minimal helper to get an otherwise OK HTML name like "foo<bar" to "foo_bar". */
    fun xmlSafeTagName(tagname: String): String? {
        return Attribute.getValidKey(tagname, Document.OutputSettings.Syntax.xml) // Reuses the Attribute key normal, which is same for xml tag names
    }

}
