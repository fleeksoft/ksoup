/*
 * Kotlin port of jsoup's TokenQueue.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.ported.Character
import com.fleeksoft.ksoup.ported.appendCodePoint
import kotlin.jvm.JvmStatic

/**
 * A character reader with helpers focusing on parsing CSS selectors. Used internally by ksoup.
 * API subject to changes.
 */
class TokenQueue(data: String) : AutoCloseable {
    private val reader = CharacterReader(data)

    fun isEmpty(): Boolean = reader.isEmpty()

    fun consume(): Char = reader.consume()

    fun advance() {
        if (!isEmpty()) reader.advance()
    }

    fun current(): Char = reader.current()

    fun matches(seq: String): Boolean = reader.matchesIgnoreCase(seq)

    /** Tests if the next character on the queue matches the character, case-sensitively.  */
    fun matches(c: Char): Boolean {
        return reader.matches(c)
    }

    fun matchesAny(vararg seq: Char): Boolean = reader.matchesAny(*seq)

    fun matchChomp(seq: String): Boolean = reader.matchConsumeIgnoreCase(seq)

    /** If the queue matches the supplied (case-sensitive) character, consume it off the queue.  */
    fun matchChomp(c: Char): Boolean {
        if (reader.matches(c)) {
            consume()
            return true
        }
        return false
    }

    fun matchesWhitespace(): Boolean = reader.current().isWhitespace()

    fun matchesWord(): Boolean = reader.current().isLetterOrDigit()

    fun consume(seq: String) {
        if (!reader.matchConsumeIgnoreCase(seq)) {
            throw IllegalStateException("Queue did not match expected sequence")
        }
    }

    fun consumeTo(seq: String): String = reader.consumeTo(seq)

    fun consumeToAny(vararg seq: String): String {
        val sb = StringUtil.borrowBuilder()
        loop@ while (!isEmpty()) {
            for (s in seq) if (reader.matchesIgnoreCase(s)) break@loop
            sb.append(consume())
        }
        return StringUtil.releaseBuilder(sb)
    }

    fun chompBalanced(open: Char, close: Char): String {
        val accum = StringUtil.borrowBuilder()
        var depth = 0
        var prev: Char = 0.toChar()
        var inSingle = false
        var inDouble = false
        var inRegexQE = false
        reader.mark()

        do {
            if (isEmpty()) break
            val c = consume()
            if (prev == ESC) {
                if (c == 'Q') inRegexQE = true
                else if (c == 'E') inRegexQE = false
                accum.append(c)
            } else {
                if (c == '\'' && c != open && !inDouble) inSingle = !inSingle
                else if (c == '"' && c != open && !inSingle) inDouble = !inDouble

                if (inSingle || inDouble || inRegexQE) {
                    accum.append(c)
                } else if (c == open) {
                    depth++
                    if (depth > 1) accum.append(c) // don't include the outer match pair in the return
                } else if (c == close) {
                    depth--
                    if (depth > 0) accum.append(c)
                } else {
                    accum.append(c)
                }
            }
            prev = c
        } while (depth > 0)

        val out = StringUtil.releaseBuilder(accum)
        if (depth > 0) {
            reader.rewindToMark()
            Validate.fail("Did not find balanced marker at '$out'")
        }
        return out
    }

    fun consumeWhitespace(): Boolean {
        var seen = false
        while (matchesWhitespace()) {
            advance()
            seen = true
        }
        return seen
    }

    fun consumeElementSelector(): String = consumeEscapedCssIdentifier(*ELEMENT_SELECTOR_CHARS)

    fun consumeCssIdentifier(): String {
        if (isEmpty()) throw IllegalArgumentException("CSS identifier expected, but end of input found")
        val identifier = reader.consumeMatching(::isIdent)
        var c = current()
        if (c != ESC && c != UNICODE_NULL) {
            return identifier
        }
        val out = StringUtil.borrowBuilder().apply { if (identifier.isNotEmpty()) append(identifier) }
        while (!isEmpty()) {
            c = current()
            when {
                isIdent(c) -> out.append(consume())
                c == UNICODE_NULL -> {
                    advance()
                    out.append(REPLACEMENT)
                }

                c == ESC -> {
                    advance()
                    if (!isEmpty() && isNewline(current())) {
                        reader.unconsume()
                        break
                    } else {
                        consumeCssEscapeSequenceInto(out)
                    }
                }

                else -> break
            }
        }
        return StringUtil.releaseBuilder(out)
    }

    private fun consumeCssEscapeSequenceInto(out: StringBuilder) {
        if (isEmpty()) {
            out.append(REPLACEMENT)
            return
        }
        val firstEsc = consume()
        if (!StringUtil.isHexDigit(firstEsc)) {
            out.append(firstEsc)
        } else {
            reader.unconsume()
            val hex = reader.consumeMatching({ StringUtil.isHexDigit(it) }, 6)
            val cp = hex.toIntOrNull(16) ?: throw IllegalArgumentException("Invalid escape sequence: $hex")
            if (isValidCodePoint(cp)) out.appendCodePoint(cp) else out.append(REPLACEMENT)
            if (!isEmpty()) {
                val c2 = current()
                if (c2 == '\r') {
                    advance()
                    if (!isEmpty() && current() == '\n') advance()
                } else if (c2 == ' ' || c2 == '\t' || isNewline(c2)) {
                    advance()
                }
            }
        }
    }

    private fun consumeEscapedCssIdentifier(vararg matches: Char): String {
        val sb = StringUtil.borrowBuilder()
        while (!isEmpty()) {
            val c = current()
            if (c == ESC) {
                advance()
                if (!isEmpty()) sb.append(consume()) else break
            } else if (matchesCssIdentifier(*matches)) {
                sb.append(c)
                advance()
            } else break
        }
        return StringUtil.releaseBuilder(sb)
    }

    private fun matchesCssIdentifier(vararg matches: Char): Boolean = matchesWord() || reader.matchesAny(*matches)

    fun remainder(): String = reader.consumeToEnd()

    override fun toString(): String = reader.toString()

    override fun close() {
        reader.close()
    }

    companion object {
        private const val ESC: Char = '\\'
        private const val HYPHEN_MINUS: Char = '-'
        private const val UNICODE_NULL: Char = '\u0000'
        private const val REPLACEMENT: Char = '\uFFFD'
        private val ELEMENT_SELECTOR_CHARS = charArrayOf('*', '|', '_', '-')

        @JvmStatic
        fun unescape(input: String): String {
            if (!input.contains(ESC)) return input
            val out = StringUtil.borrowBuilder()
            var last: Char = 0.toChar()
            for (c in input) {
                if (c == ESC) {
                    if (last == ESC) {
                        out.append(c)
                        last = 0.toChar()
                        continue
                    }
                } else {
                    out.append(c)
                }
                last = c
            }
            return StringUtil.releaseBuilder(out)
        }

        @JvmStatic
        fun escapeCssIdentifier(input: String): String {
            if (input.isEmpty()) return input
            val out = StringUtil.borrowBuilder()
            val q = TokenQueue(input)
            val firstChar = q.current()
            when {
                firstChar == HYPHEN_MINUS -> {
                    q.advance()
                    if (q.isEmpty()) appendEscaped(out, HYPHEN_MINUS) else {
                        out.append(HYPHEN_MINUS)
                        if (StringUtil.isDigit(q.current())) appendEscapedCodepoint(out, q.consume())
                    }
                }

                StringUtil.isDigit(firstChar) -> appendEscapedCodepoint(out, q.consume())
            }
            while (!q.isEmpty()) {
                val c = q.consume()
                when {
                    c == UNICODE_NULL -> out.append(REPLACEMENT)
                    c <= '\u001F' || c == '\u007F' -> appendEscapedCodepoint(out, c)
                    isIdent(c) -> out.append(c)
                    else -> appendEscaped(out, c)
                }
            }
            q.close()
            return StringUtil.releaseBuilder(out)
        }

        private fun appendEscaped(out: StringBuilder, c: Char) {
            out.append(ESC).append(c)
        }

        private fun appendEscapedCodepoint(out: StringBuilder, c: Char) {
            out.append(ESC).append(c.code.toString(16)).append(' ')
        }

        private fun isNonAscii(c: Char): Boolean = c >= '\u0080'
        private fun isIdentStart(c: Char): Boolean = c == '_' || StringUtil.isAsciiLetter(c) || isNonAscii(c)
        private fun isIdent(c: Char): Boolean = c == HYPHEN_MINUS || StringUtil.isDigit(c) || isIdentStart(c)
        private fun isNewline(c: Char): Boolean = c == '\n' || c == '\r' || c == '\u000C'
        private fun isValidCodePoint(codePoint: Int): Boolean =
            codePoint != 0 && Character.isValidCodePoint(codePoint) && !codePoint.toChar().isSurrogate()
    }
}

