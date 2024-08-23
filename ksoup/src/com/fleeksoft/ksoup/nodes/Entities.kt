@file:OptIn(ExperimentalStdlibApi::class)

package com.fleeksoft.ksoup.nodes

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.nodes.Document.OutputSettings
import com.fleeksoft.ksoup.nodes.Document.OutputSettings.Syntax
import com.fleeksoft.ksoup.nodes.Entities.EscapeMode.base
import com.fleeksoft.ksoup.nodes.Entities.EscapeMode.extended
import com.fleeksoft.ksoup.parser.CharacterReader
import com.fleeksoft.ksoup.parser.Parser
import com.fleeksoft.ksoup.ported.Character
import com.fleeksoft.ksoup.ported.ThreadLocal
import com.fleeksoft.ksoup.ported.exception.IOException
import com.fleeksoft.ksoup.ported.exception.SerializationException
import com.fleeksoft.ksoup.ported.io.Charsets
import de.cketti.codepoints.deluxe.CodePoint
import de.cketti.codepoints.deluxe.codePointAt


/**
 * HTML entities, and escape routines. Source: [W3C
 * HTML named character references](http://www.w3.org/TR/html5/named-character-references.html#named-character-references).
 */
public object Entities {
    // constants for escape options:
    const val ForText: Int = 0x1
    const val ForAttribute: Int = 0x2
    const val Normalise: Int = 0x4
    const val TrimLeading: Int = 0x8
    const val TrimTrailing: Int = 0x10

    private const val empty = -1
    private const val emptyName = ""
    private const val codepointRadix = 36
    private val codeDelims = charArrayOf(',', ';')
    private val multipoints: HashMap<String, String> =
        HashMap<String, String>() // name -> multiple character references

    /**
     * Check if the input is a known named entity
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity
     */
    public fun isNamedEntity(name: String): Boolean {
        return extended.codepointForName(name) != empty
    }

    /**
     * Check if the input is a known named entity in the base entity set.
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity in the base set
     * @see .isNamedEntity
     */
    public fun isBaseNamedEntity(name: String): Boolean {
        return base.codepointForName(name) != empty
    }

    /**
     * Get the character(s) represented by the named entity
     *
     * @param name entity (e.g. "lt" or "amp")
     * @return the string value of the character(s) represented by this entity, or "" if not defined
     */
    public fun getByName(name: String): String {
        val value: String? = multipoints[name]
        if (value != null) return value
        val codepoint = extended.codepointForName(name)
        return if (codepoint != empty) {
            charArrayOf(codepoint.toChar()).concatToString()
        } else {
            emptyName
        }
    }

    public fun codepointsForName(
        name: String,
        codepoints: IntArray,
    ): Int {
        val value: String? = multipoints[name]
        if (value != null) {
            codepoints[0] = value.codePointAt(0).value
            codepoints[1] = value.codePointAt(1).value
            return 2
        }
        val codepoint = extended.codepointForName(name)
        if (codepoint != empty) {
            codepoints[0] = codepoint
            return 1
        }
        return 0
    }

    /**
    HTML escape an input string. That is, {@code <} is returned as {@code &lt;}. The escaped string is suitable for use
    both in attributes and in text data.
    @param data the un-escaped string to escape
    @param out the output settings to use. This configures the character set escaped against (that is, if a
    character is supported in the output character set, it doesn't have to be escaped), and also HTML or XML
    settings.
    @return the escaped string
     */
    public fun escape(data: String?, out: OutputSettings): String {
        return escapeString(data, out.escapeMode(), out.syntax(), out.charset())
    }

    /**
    HTML escape an input string, using the default settings (UTF-8, base entities, HTML syntax). That is, {@code <} is
    returned as {@code &lt;}. The escaped string is suitable for use both in attributes and in text data.
    @param data the un-escaped string to escape
    @return the escaped string
    @see #escape(String, OutputSettings)
     */
    public fun escape(data: String?): String {
        return escapeString(data, base, Syntax.html, Charsets.UTF8)
    }

    public fun escapeString(data: String?, escapeMode: EscapeMode, syntax: Syntax, charset: Charset): String {
        if (data == null) return ""
        val accum = StringUtil.borrowBuilder()
        try {
            doEscape(data, accum, escapeMode, syntax, charset, ForText or ForAttribute)
        } catch (e: IOException) {
            throw SerializationException(e) // doesn't happen
        }

        return StringUtil.releaseBuilder(accum)
    }

    fun escape(accum: Appendable, data: String, out: OutputSettings, options: Int) {
        doEscape(data, accum, out.escapeMode(), out.syntax(), out.charset(), options)
    }

    // this method does a lot, but other breakups cause rescanning and stringbuilder generations
    private fun doEscape(data: String, accum: Appendable, mode: EscapeMode, syntax: Syntax, charset: Charset, options: Int) {
        val fallback: Charset = charset
        val coreCharset: CoreCharset = CoreCharset.byName(charset.name)
        val length = data.length
        var codePoint: CodePoint
        var lastWasWhite = false
        var reachedNonWhite = false
        var skipped = false
        var offset = 0
        while (offset < length) {
            codePoint = data.codePointAt(offset)

            if (options and Normalise != 0) {
                if (StringUtil.isWhitespace(codePoint.value)) {
                    if ((options and TrimLeading) != 0 && !reachedNonWhite) {
                        offset += codePoint.charCount
                        continue
                    }
                    if (lastWasWhite) {
                        offset += codePoint.charCount
                        continue
                    }
                    if (options and TrimTrailing != 0) {
                        skipped = true
                        offset += codePoint.charCount
                        continue
                    }
                    accum.append(' ')
                    lastWasWhite = true
                    offset += codePoint.charCount
                    continue
                } else {
                    lastWasWhite = false
                    reachedNonWhite = true
                    if (skipped) {
                        accum.append(' ') // wasn't the end, so need to place a normalized space
                        skipped = false
                    }
                }
            }

            appendEscaped(codePoint, accum, options, mode, syntax, coreCharset, fallback)
            // surrogate pairs, split implementation for efficiency on single char common case (saves creating strings, char[]):
            offset += codePoint.charCount
        }
    }

    private fun appendEscaped(
        codePoint: CodePoint,
        accum: Appendable,
        options: Int,
        escapeMode: EscapeMode,
        syntax: Syntax,
        coreCharset: CoreCharset,
        fallback: Charset
    ) {
        // surrogate pairs, split implementation for efficiency on single char common case (saves creating strings, char[]):
        val c = codePoint.value.toChar()
        if (codePoint.value < Character.MIN_SUPPLEMENTARY_CODE_POINT || fallback.name.uppercase() == "ASCII" || fallback.name.uppercase() == "US-ASCII" || fallback.name.uppercase() == "ISO-8859-1") {
            when {
                c == '&' -> {
                    accum.append("&amp;")
                }

                c.code == 0xA0 -> {
                    appendNbsp(accum, escapeMode)
                    /*if (escapeMode != EscapeMode.xhtml) {
                        accum.append("&nbsp;")
                    } else {
                        accum.append("&#xa0;")
                    }*/
                }

                c == '<' -> {

                    // escape when in character data or when in a xml attribute val or XML syntax; not needed in html attr val
                    appendLt(accum, options, escapeMode, syntax)
                    /*if (!inAttribute || escapeMode == EscapeMode.xhtml || out.syntax() === OutputSettings.Syntax.xml) {
                        accum.append("&lt;")
                    } else {
                        accum.append(c)
                    }*/
                }

                c == '>' -> {
                    if ((options and ForText) != 0) accum.append("&gt;")
                    else accum.append(c)
                }

                c == '"' -> {
                    if ((options and ForAttribute) != 0) accum.append("&quot;")
                    else accum.append(c)
                }

                c == '\'' -> {
                    // special case for the Entities.escape(string) method when we are maximally escaping. Otherwise, because we output attributes in "", there's no need to escape.
                    appendApos(accum, options, escapeMode)
                }

                // we escape ascii control <x20 (other than tab, line-feed, carriage return) for XML compliance (required) and HTML ease of reading (not required) - https://www.w3.org/TR/xml/#charsets
                c.code == 0x9 || c.code == 0xA || c.code == 0xD -> {
                    accum.append(c)
                }

                else -> {
                    if (c.code < 0x20 || !canEncode(coreCharset, c, fallback)) {
                        appendEncoded(accum, escapeMode, codePoint.value)
                    } else {
                        accum.append(c)
                    }
                }
            }
        } else {
            if (fallback.canEncode(codePoint.toChars().concatToString())) {
                val chars = charBuf.get()
                val len = codePoint.toChars(chars, 0)
                if (accum is StringBuilder) {
                    accum.append(chars)
                } else {
                    accum.append(chars.concatToString(0, len))
                }
            } else {
                appendEncoded(accum, escapeMode, codePoint.value)
            }
        }
    }

    private val charBuf: ThreadLocal<CharArray> = ThreadLocal { CharArray(2) }

    private fun appendNbsp(accum: Appendable, escapeMode: EscapeMode) {
        if (escapeMode != EscapeMode.xhtml) accum.append("&nbsp;")
        else accum.append("&#xa0;")
    }

    private fun appendLt(accum: Appendable, options: Int, escapeMode: EscapeMode, syntax: Syntax) {
        if ((options and ForText) != 0 || (escapeMode == EscapeMode.xhtml) || (syntax === Syntax.xml)) {
            accum.append("&lt;")
        } else {
            accum.append('<') // no need to escape < when in an HTML attribute
        }
    }

    private fun appendApos(accum: Appendable, options: Int, escapeMode: EscapeMode) {
        if ((options and ForAttribute) != 0 && (options and ForText) != 0) {
            if (escapeMode == EscapeMode.xhtml) accum.append("&#x27;")
            else accum.append("&apos;")
        } else {
            accum.append('\'')
        }
    }

    private fun appendEncoded(
        accum: Appendable,
        escapeMode: EscapeMode,
        codePoint: Int,
    ) {
        val name = escapeMode.nameForCodepoint(codePoint)
        if (emptyName != name) {
            // ok for identity check
            accum.append('&').append(name).append(';')
        } else {
            accum.append("&#x")
                .append(codePoint.toHexString(HexFormat { number { removeLeadingZeros = true } }))
                .append(';')
        }
    }

    /**
     * Un-escape an HTML escaped string. That is, `&lt;` is returned as `<`.
     *
     * @param string the HTML string to un-escape
     * @return the unescaped string
     */
    public fun unescape(string: String): String {
        return unescape(string, false)
    }

    /**
     * Unescape the input string.
     *
     * @param string to un-HTML-escape
     * @param strict if "strict" (that is, requires trailing ';' char, otherwise that's optional)
     * @return unescaped string
     */
    public fun unescape(
        string: String,
        strict: Boolean,
    ): String {
        return Parser.unescapeEntities(string, strict)
    }

    /*
     * Provides a fast-path for Encoder.canEncode, which drastically improves performance on Android post JellyBean.
     * After KitKat, the implementation of canEncode degrades to the point of being useless. For non ASCII or UTF,
     * performance may be bad. We can add more encoders for common character sets that are impacted by performance
     * issues on Android if required.
     *
     * Benchmarks:     *
     * OLD toHtml() impl v New (fastpath) in millis
     * Wiki: 1895, 16
     * CNN: 6378, 55
     * Alterslash: 3013, 28
     */
    private fun canEncode(
        charset: CoreCharset,
        c: Char,
        fallback: Charset,
    ): Boolean {
        // todo add more charset tests if impacted by Android's bad perf in canEncode
        return when (charset) {
            CoreCharset.asciiExt -> c.code <= 0xFF // ISO-8859-1 range from 0x00 to 0xFF
            CoreCharset.utf -> !(c >= Character.MIN_SURROGATE && c.code < (Character.MAX_SURROGATE.code + 1)) // !Character.isSurrogate(c); but not in Android 10 desugar;
            else -> fallback.canEncode(c)
        }
    }

    private fun load(e: EscapeMode, pointsData: String, size: Int) {
        e.nameKeys = arrayOfNulls(size)
        e.codeVals = IntArray(size)
        e.codeKeys = IntArray(size)
        e.nameVals = arrayOfNulls(size)
        var i = 0
        val reader = CharacterReader(pointsData)
        try {
            while (!reader.isEmpty()) {
                // NotNestedLessLess=10913,824;1887&
                val name: String = reader.consumeTo('=')
                reader.advance()
                val cp1: Int = reader.consumeToAny(*codeDelims).toInt(codepointRadix)
                val codeDelim: Char = reader.current()
                reader.advance()
                val cp2: Int
                if (codeDelim == ',') {
                    cp2 = reader.consumeTo(';').toInt(codepointRadix)
                    reader.advance()
                } else {
                    cp2 = empty
                }
                val indexS: String = reader.consumeTo('&')
                val index = indexS.toInt(codepointRadix)
                reader.advance()
                e.nameKeys[i] = name
                e.codeVals[i] = cp1
                e.codeKeys[index] = cp1
                e.nameVals[index] = name
                if (cp2 != empty) {
                    multipoints[name] = charArrayOf(cp1.toChar(), cp2.toChar()).concatToString()
                }
                i++
            }
            Validate.isTrue(i == size, "Unexpected count of entities loaded")
        } finally {
            reader.close()
        }
    }

    public enum class EscapeMode(file: String, size: Int) {
        /**
         * Restricted entities suitable for XHTML output: lt, gt, amp, and quot only.
         */
        xhtml(EntitiesData.xmlPoints, 4),

        /**
         * Default HTML output entities.
         */
        base(EntitiesData.basePoints, 106),

        /**
         * Complete HTML entities.
         */
        extended(EntitiesData.fullPoints, 2125),
        ;

        // table of named references to their codepoints. sorted so we can binary search. built by BuildEntities.
        internal lateinit var nameKeys: Array<String?>
        internal lateinit var codeVals: IntArray

        // table of codepoints to named entities.
        internal lateinit var codeKeys: IntArray
        internal lateinit var nameVals: Array<String?>

        init {
            load(this, file, size)
        }

        public fun codepointForName(name: String): Int {
            val index: Int = nameKeys.toList().binarySearch(name)
            return if (index >= 0) codeVals[index] else empty
        }

        public fun nameForCodepoint(codepoint: Int): String {
            val index: Int = codeKeys.toList().binarySearch(codepoint)
            return if (index >= 0) {
                // the results are ordered so lower case versions of same codepoint come after uppercase, and we prefer to emit lower
                // (and binary search for same item with multi results is undefined
                if (index < nameVals.size - 1 && codeKeys[index + 1] == codepoint) nameVals[index + 1]!! else nameVals[index]!!
            } else {
                emptyName
            }
        }
    }

    public enum class CoreCharset {
        asciiExt, // ISO-8859-1
        utf,
        fallback,
        ;

        public companion object {

            public fun byName(name: String): CoreCharset {
                if (name.uppercase() == "US-ASCII" || name.uppercase() == "ASCII" || name.uppercase() == "ISO-8859-1") return asciiExt
                return if (name.startsWith("UTF-")) utf else fallback
            }
        }
    }
}
