package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.ported.Character
import com.fleeksoft.ksoup.ported.appendCodePoint

/**
 * A value holder for Tokens, as the stream is Tokenized. Can hold a String or a StringBuilder.
 *
 * The goal is to minimize String copies -- the tokenizer tries to read the entirety of the token's data in one it, and
 * set that as the simple String value. But if it turns out we need to append, fall back to a StringBuilder, which we get
 * out of the pool (to reduce the GC load).
 */
class TokenData {
    private var value: String? = null
    private var builder: StringBuilder? = null

    fun set(str: String) {
        reset()
        value = str
    }

    fun append(str: String) {
        when {
            builder != null -> builder!!.append(str)
            value != null -> {
                flipToBuilder()
                builder!!.append(str)
            }

            else -> value = str
        }
    }

    fun append(c: Char) {
        when {
            builder != null -> builder!!.append(c)
            value != null -> {
                flipToBuilder()
                builder!!.append(c)
            }

            else -> value = c.toString()
        }
    }

    fun appendCodePoint(codepoint: Int) {
        when {
            builder != null -> builder!!.appendCodePoint(codepoint)
            value != null -> {
                flipToBuilder()
                builder!!.appendCodePoint(codepoint)
            }

            else -> value = Character.toChars(codepoint).concatToString()
        }
    }

    private fun flipToBuilder() {
        builder = StringUtil.borrowBuilder()
        builder!!.append(value)
        value = null
    }

    fun hasData(): Boolean = builder != null || value != null

    fun reset() {
        builder?.let {
            StringUtil.releaseBuilderVoid(it)
            builder = null
        }
        value = null
    }

    fun value(): String {
        builder?.let {
            // in rare case we get hit twice, don't toString the builder twice
            value = it.toString()
            StringUtil.releaseBuilder(it)
            builder = null
            return value!!
        }
        return value ?: ""
    }

    override fun toString(): String {
        // for debug views; no side effects
        return builder?.toString() ?: (value ?: "")
    }
}