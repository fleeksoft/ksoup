package com.fleeksoft.ksoup.parser

import com.fleeksoft.ksoup.helper.Validate
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.nodes.Entities
import com.fleeksoft.ksoup.ported.codePointsToString
import de.cketti.codepoints.appendCodePoint

/**
 * Readers the input stream into tokens.
 */
internal class Tokeniser(private val treeBuilder: TreeBuilder) {
    private val reader: CharacterReader = treeBuilder.reader
    private val errors: ParseErrorList = treeBuilder.parser.getErrors()
    private var state = TokeniserState.Data
    private var emitPending: Token? = null
    private var isEmitPending = false
    private var charsString: String? = null
    private val charsBuilder = StringBuilder(1024)
    val dataBuffer = StringBuilder(1024)

    private val startPending = Token.StartTag(treeBuilder)
    private val endPending = Token.EndTag(treeBuilder)
    var tagPending: Token.Tag = startPending
    private val charPending = Token.Character()
    val doctypePending = Token.Doctype()
    val commentPending = Token.Comment()
    private var lastStartTag: String? = null
    private var lastStartCloseSeq: String? = null
    private var markupStartPos = Unset
    private var charStartPos = Unset

    private val codepointHolder = IntArray(1) // holder to not have to keep creating arrays
    private val multipointHolder = IntArray(2)

    fun read(): Token {
        while (!isEmitPending) {
            state.read(this, reader)
        }

        return when {
            charsBuilder.isNotEmpty() -> {
                val str = charsBuilder.toString()
                charsBuilder.clear()
                charPending.data(str).also { charsString = null }
            }

            charsString != null -> {
                charPending.data(charsString!!).also { charsString = null }
            }

            else -> {
                isEmitPending = false
                emitPending!!
            }
        }
    }

    fun emit(token: Token) {
        Validate.isFalse(isEmitPending)

        emitPending = token
        isEmitPending = true
        token.startPos(markupStartPos)
        token.endPos(reader.pos())
        charStartPos = Unset

        when (token.type) {
            Token.TokenType.StartTag -> {
                val startTag = token as Token.StartTag
                lastStartTag = startTag.tagName
                lastStartCloseSeq = null // only lazy inits
            }

            Token.TokenType.EndTag -> {
                val endTag = token as Token.EndTag
                if (endTag.hasAttributes()) {
                    error("Attributes incorrectly present on end tag [/${endTag.retrieveNormalName()}]")
                }
            }

            else -> {}
        }
    }

    fun emit(str: String) {
        if (charsString == null) {
            charsString = str
        } else {
            if (charsBuilder.isEmpty()) {
                charsBuilder.append(charsString)
            }
            charsBuilder.append(str)
        }
        charPending.startPos(charStartPos)
        charPending.endPos(reader.pos())
    }

    fun emit(strBuilder: StringBuilder) {
        if (charsString == null) {
            charsString = strBuilder.toString()
        } else {
            if (charsBuilder.isEmpty()) {
                charsBuilder.append(charsString)
            }
            charsBuilder.append(strBuilder)
        }
        charPending.startPos(charStartPos)
        charPending.endPos(reader.pos())
    }

    fun emit(c: Char) {
        if (charsString == null) {
            charsString = c.toString()
        } else {
            if (charsBuilder.isEmpty()) {
                charsBuilder.append(charsString)
            }
            charsBuilder.append(c)
        }
        charPending.startPos(charStartPos)
        charPending.endPos(reader.pos())
    }

    fun emit(chars: CharArray) {
        emit(chars.concatToString())
    }

    fun emit(codepoints: IntArray) {
        emit(codepoints.codePointsToString())
//        emit(String(codepoints, 0, codepoints.size))
    }

    fun getState(): TokeniserState {
        return state
    }

    fun transition(newState: TokeniserState) {
        when (newState) {
            TokeniserState.TagOpen -> markupStartPos = reader.pos()
            TokeniserState.Data -> if (charStartPos == Unset) charStartPos = reader.pos()
            else -> {}
        }
        this.state = newState
    }

    fun advanceTransition(newState: TokeniserState) {
        transition(newState)
        reader.advance()
    }

    fun consumeCharacterReference(
        additionalAllowedCharacter: Char?,
        inAttribute: Boolean,
    ): IntArray? {
        if (reader.isEmpty()) return null
        if (additionalAllowedCharacter != null && additionalAllowedCharacter == reader.current()) return null
        if (reader.matchesAnySorted(notCharRefCharsSorted)) return null

        val codeRef = codepointHolder
        reader.mark()
        if (reader.matchConsume("#")) {
            val isHexMode = reader.matchConsumeIgnoreCase("X")
            val numRef =
                if (isHexMode) reader.consumeHexSequence() else reader.consumeDigitSequence()
            if (numRef.isEmpty()) {
                characterReferenceError("numeric reference with no numerals")
                reader.rewindToMark()
                return null
            }

            reader.unmark()
            if (!reader.matchConsume(";")) {
                characterReferenceError("missing semicolon on [&#$numRef]")
            }

            var charval =
                try {
                    numRef.toInt(if (isHexMode) 16 else 10)
                } catch (e: NumberFormatException) {
                    -1
                }

            if (charval == -1 || charval > 0x10FFFF) {
                characterReferenceError("character [$charval] outside of valid range")
                codeRef[0] = Tokeniser.replacementChar.code
            } else {
                if (charval >= win1252ExtensionsStart && charval < win1252ExtensionsStart + win1252Extensions.size) {
                    characterReferenceError("character [$charval] is not a valid unicode code point")
                    charval = win1252Extensions[charval - win1252ExtensionsStart]
                }
                codeRef[0] = charval
            }
            return codeRef
        } else {
            val nameRef = reader.consumeLetterThenDigitSequence()
            val looksLegit = reader.matches(';')
            val found =
                (Entities.isBaseNamedEntity(nameRef) || (Entities.isNamedEntity(nameRef) && looksLegit))

            if (!found) {
                reader.rewindToMark()
                if (looksLegit) characterReferenceError("invalid named reference [$nameRef]")
                return null
            }

            if (inAttribute && reader.matchesAny('=', '-', '_')) {
                reader.rewindToMark() // don't want that to match
                return null
            }

            reader.unmark()
            if (!reader.matchConsume(";")) characterReferenceError("missing semicolon on [&$nameRef]")

            val numChars = Entities.codepointsForName(nameRef, multipointHolder)
            return when (numChars) {
                1 -> {
                    codeRef[0] = multipointHolder[0]
                    codeRef
                }

                2 -> multipointHolder
                else -> {
                    Validate.fail("Unexpected characters returned for $nameRef")
                    multipointHolder
                }
            }
        }
    }

    fun createTagPending(start: Boolean): Token.Tag {
        tagPending = if (start) startPending.reset() else endPending.reset()
        return tagPending
    }

    fun emitTagPending() {
        tagPending.finaliseTag()
        emit(tagPending)
    }

    fun createCommentPending() {
        commentPending.reset()
    }

    fun emitCommentPending() {
        emit(commentPending)
    }

    fun createBogusCommentPending() {
        commentPending.reset()
        commentPending.bogus = true
    }

    fun createDoctypePending() {
        doctypePending.reset()
    }

    fun emitDoctypePending() {
        emit(doctypePending)
    }

    fun createTempBuffer() {
        Token.reset(dataBuffer)
    }

    fun isAppropriateEndTagToken(): Boolean = lastStartTag != null && tagPending.name().equals(lastStartTag, ignoreCase = true)

    fun appropriateEndTagName(): String? {
        return lastStartTag // could be null
    }

    /** Returns the closer sequence `</lastStart`  */
    fun appropriateEndTagSeq(): String {
        if (lastStartCloseSeq == null) {
            // reset on start tag emit
            lastStartCloseSeq = "</$lastStartTag"
        }
        return lastStartCloseSeq!!
    }

    fun error(state: TokeniserState?) {
        if (errors.canAddError()) {
            errors.add(
                ParseError(
                    reader,
                    "Unexpected character '${reader.current()}' in input state [$state]",
                ),
            )
        }
    }

    fun eofError(state: TokeniserState?) {
        if (errors.canAddError()) {
            errors.add(
                ParseError(
                    reader,
                    "Unexpectedly reached end of file (EOF) in input state [$state]",
                ),
            )
        }
    }

    private fun characterReferenceError(message: String) {
        if (errors.canAddError()) {
            errors.add(
                ParseError(
                    reader,
                    "Invalid character reference: $message",
                ),
            )
        }
    }

    fun error(errorMsg: String) {
        if (errors.canAddError()) errors.add(ParseError(reader, errorMsg))
    }

    /**
     * Utility method to consume reader and unescape entities found within.
     * @param inAttribute if the text to be unescaped is in an attribute
     * @return unescaped string from reader
     */
    fun unescapeEntities(inAttribute: Boolean): String {
        val builder: StringBuilder = StringUtil.borrowBuilder()
        while (!reader.isEmpty()) {
            builder.append(reader.consumeTo('&'))
            if (reader.matches('&')) {
                reader.consume()
                val c = consumeCharacterReference(null, inAttribute)
                if (c == null || c.isEmpty()) {
                    builder.append('&')
                } else {
                    builder.appendCodePoint(c[0])
                    if (c.size == 2) builder.appendCodePoint(c[1])
                }
            }
        }
        return StringUtil.releaseBuilder(builder)
    }

    companion object {
        const val replacementChar: Char = '\uFFFD' // replaces null character
        private val notCharRefCharsSorted: CharArray =
            charArrayOf('\t', '\n', '\r', '\u000c', ' ', '<', '&').sortedArray()

        // Some illegal character escapes are parsed by browsers as windows-1252 instead. See issue #1034
        // https://html.spec.whatwg.org/multipage/parsing.html#numeric-character-reference-end-state
        const val win1252ExtensionsStart = 0x80
        val win1252Extensions =
            intArrayOf(
                // we could build this manually, but Windows-1252 is not a standard java charset so that could break on
                // some platforms - this table is verified with a test
                0x20AC, 0x0081, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021,
                0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0x008D, 0x017D, 0x008F,
                0x0090, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014,
                0x02DC, 0x2122, 0x0161, 0x203A, 0x0153, 0x009D, 0x017E, 0x0178,
            )

        private const val Unset = -1

        fun currentNodeInHtmlNS(): Boolean {
            // todo: implement namespaces correctly
            return true
            // Element currentNode = currentNode();
            // return currentNode != null && currentNode.namespace().equals("HTML");
        }
    }
}
