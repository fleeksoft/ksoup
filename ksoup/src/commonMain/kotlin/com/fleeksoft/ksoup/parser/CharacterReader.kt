package com.fleeksoft.ksoup.parser

import okio.IOException
import com.fleeksoft.ksoup.UncheckedIOException
import com.fleeksoft.ksoup.ported.BufferReader
import com.fleeksoft.ksoup.ported.buildString
import kotlin.math.abs
import kotlin.math.min

/**
 * CharacterReader consumes tokens off a string. Used internally by com.fleeksoft.ksoup. API subject to changes.
 */
class CharacterReader {
    private var charBuf: CharArray?
    private var source: CharArray?
    private var bufLength = 0
    private var bufSplitPoint = 0
    private var bufPos = 0
    private var readerPos: Int = 0
    private var bufMark = -1
    private var close: Boolean = false
    private var stringCache: Array<String?>? =
        arrayOfNulls(stringCacheSize) // holds reused strings in this doc, to lessen garbage

    /*@Nullable*/ // optionally track the pos() position of newlines - scans during bufferUp()
    private var newlinePositions: ArrayList<Int>? = null
    private var lineNumberOffset = 1 // line numbers start at 1; += newlinePosition[indexof(pos)]

    constructor(input: BufferReader) : this(input.readCharArray(), maxBufferLen)

    constructor(input: String) : this(input.toCharArray(), input.length)

    constructor(input: CharArray, sz: Int = maxBufferLen) {
        source = input
        charBuf = CharArray(min(sz, maxBufferLen))
        bufferUp()
    }

    fun isClosed() = close

    fun close() {
        close = true
        try {
            source = null
        } catch (ignored: IOException) {
        } finally {
            charBuf = null
            stringCache = null
        }
    }

    private var readFully =
        false // if the underlying stream has been completely read, no value in further buffering

    private var skipPos: Int = 0
    private fun bufferUp() {
//        println("pre => bufSize: ${charBuf?.size} bufLength: $bufLength, readerPos: $readerPos, bufPos: $bufPos, bufSplitPoint: $bufSplitPoint")
        if (readFully || bufPos < bufSplitPoint) return

        val (pos, offset) = if (bufMark != -1) {
            Pair(bufMark.toLong(), bufPos - bufMark)
        } else {
            Pair(bufPos.toLong(), 0)
        }
//        val markSource = SourceMarker(source, minReadAheadLen.toLong())

        try {
            skipPos += pos.toInt()
            var read: Int = 0
            while (read <= minReadAheadLen && !readFully) {
                var thisRead = 0
//                val readData: ByteArray = ByteArray(charBuf!!.size)
                val toIndex = min(source!!.size, skipPos + charBuf!!.size) - read
                val toReadDataSize = toIndex - skipPos
                if (toReadDataSize > 0 && toIndex > skipPos) {
                    source!!.copyInto(charBuf!!, startIndex = skipPos, endIndex = toIndex)
//                    val readData: CharArray = source!!.copyOfRange(skipPos, toIndex)
                    thisRead = toReadDataSize
                    if (toIndex >= source!!.size) readFully = true
                }

//                thisRead = peekSource.read(readData, read, charBuf!!.size - pos.toInt()) // always reading 8126 bytes only
                /*charBuf = peekSource.readByteArray(charBuf!!.size - read).also { thisRead = it.size }.decodeToString()
                    .toCharArray()*/
//                if (thisRead == -1) readFully = true
                if (thisRead <= 0) break
                read += thisRead
            }

            if (read > 0) {
                bufLength = read
                readerPos += pos.toInt()
                bufPos = offset
                if (bufMark != -1) bufMark = 0
                bufSplitPoint = minOf(bufLength, readAheadLimit)
            }

//            println("post => bufSize: ${charBuf?.size} bufLength: $bufLength, readerPos: $readerPos, bufPos: $bufPos, bufSplitPoint: $bufSplitPoint")

            /*if (source.buffer.size > 0) {
                markSource.source().skip(pos)
                val userOffset = markSource.mark(maxBufferLen.toLong())
                var read = 0
                val byteArray = ByteArray(charBuf!!.size)
                while (read <= minReadAheadLen) {
                    val length = charBuf!!.size - read
                    val endIndex = read + length
                    if (endIndex == -1) readFully = true
                    if (endIndex <= 0) break
                    val thisRead = source.buffer.readAtMostTo(byteArray, read, endIndex)
                    if (thisRead == -1) readFully = true
                    if (thisRead <= 0) break
                    read += thisRead
                }
                charBuf = byteArray.decodeToString().toCharArray()
                markSource.reset(userOffset)
                if (read > 0) {
                    bufLength = read
                    readerPos += pos.toInt()
                    bufPos = offset
                    if (bufMark != -1) bufMark = 0
                    bufSplitPoint = minOf(bufLength, readAheadLimit)
                }
            } else {
                readFully = true
            }*/

        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
        scanBufferForNewlines() // if enabled, we index newline positions for line number tracking
        lastIcSeq = null // cache for last containsIgnoreCase(seq)
    }

    /*private fun bufferUp() {
        if (readFully || bufPos < bufSplitPoint) return
        val pos: Int
        val offset: Int
        if (bufMark != -1) {
            pos = bufMark
            offset = bufPos - bufMark
        } else {
            pos = bufPos
            offset = 0
        }
        try {
            val skipped: Long = reader.skip(pos.toLong())
            reader.mark(maxBufferLen)
            var read = 0
            while (read <= minReadAheadLen) {
                val thisRead: Int = reader.read(charBuf, read, charBuf!!.size - read)
                if (thisRead == -1) readFully = true
                if (thisRead <= 0) break
                read += thisRead
            }
            reader.reset()
            if (read > 0) {
                Validate.isTrue(skipped == pos.toLong()) // Previously asserted that there is room in buf to skip, so this will be a WTF
                bufLength = read
                readerPos += pos
                bufPos = offset
                if (bufMark != -1) bufMark = 0
                bufSplitPoint = min(bufLength, readAheadLimit)
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
        scanBufferForNewlines() // if enabled, we index newline positions for line number tracking
        lastIcSeq = null // cache for last containsIgnoreCase(seq)
    }*/

    /**
     * Gets the position currently read to in the content. Starts at 0.
     * @return current position
     */
    fun pos(): Int {
        return readerPos + bufPos
    }

    /** Tests if the buffer has been fully read.  */
    fun readFully(): Boolean {
        return readFully
    }

    /**
     * Enables or disables line number tracking. By default, will be **off**.Tracking line numbers improves the
     * legibility of parser error messages, for example. Tracking should be enabled before any content is read to be of
     * use.
     *
     * @param track set tracking on|off
     * @since 1.14.3
     */
    fun trackNewlines(track: Boolean) {
        if (track && newlinePositions == null) {
            newlinePositions =
                ArrayList<Int>(maxBufferLen / 80) // rough guess of likely count
            scanBufferForNewlines() // first pass when enabled; subsequently called during bufferUp
        } else if (!track) newlinePositions = null
    }

    fun isTrackNewlines(): Boolean = newlinePositions != null

    /**
     * Get the current line number (that the reader has consumed to). Starts at line #1.
     * @return the current line number, or 1 if line tracking is not enabled.
     * @since 1.14.3
     * @see .trackNewlines
     */
    fun lineNumber(): Int {
        return lineNumber(pos())
    }

    fun lineNumber(pos: Int): Int {
        // note that this impl needs to be called before the next buffer up or line numberoffset will be wrong. if that
        // causes issues, can remove the reset of newlinepositions during buffer, at the cost of a larger tracking array
        if (!isTrackNewlines()) return 1
        val i = lineNumIndex(pos)
        return if (i == -1) lineNumberOffset else i + lineNumberOffset + 1 // first line
    }

    /**
     * Get the current column number (that the reader has consumed to). Starts at column #1.
     * @return the current column number
     * @since 1.14.3
     * @see .trackNewlines
     */
    fun columnNumber(): Int {
        return columnNumber(pos())
    }

    fun columnNumber(pos: Int): Int {
        if (!isTrackNewlines()) return pos + 1
        val i = lineNumIndex(pos)
        return if (i == -1) pos + 1 else pos - newlinePositions!![i] + 1
    }

    /**
     * Get a formatted string representing the current line and cursor positions. E.g. `5:10` indicating line
     * number 5 and column number 10.
     * @return line:col position
     * @since 1.14.3
     * @see .trackNewlines
     */
    fun cursorPos(): String {
        return lineNumber().toString() + ":" + columnNumber()
    }

    private fun lineNumIndex(pos: Int): Int {
        if (!isTrackNewlines()) return 0
        var i: Int = newlinePositions!!.binarySearch(pos)
        if (i < -1) i = abs(i) - 2
        return i
    }

    /**
     * Scans the buffer for newline position, and tracks their location in newlinePositions.
     */
    private fun scanBufferForNewlines() {
        if (!isTrackNewlines()) return
        if (newlinePositions!!.size > 0) {
            // work out the line number that we have read up to (as we have likely scanned past this point)
            var index = lineNumIndex(readerPos)
            if (index == -1) index = 0 // first line
            val linePos: Int = newlinePositions!![index]
            lineNumberOffset += index // the num lines we've read up to
            newlinePositions!!.clear()
            newlinePositions!!.add(linePos) // roll the last read pos to first, for cursor num after buffer
        }
        for (i in bufPos until bufLength) {
            if (charBuf!![i] == '\n') newlinePositions!!.add(1 + readerPos + i)
        }
    }

    fun isEmpty(): Boolean {
        bufferUp()
        return bufPos >= bufLength
    }

    private fun isEmptyNoBufferUp(): Boolean = bufPos >= bufLength

    /**
     * Get the char at the current position.
     * @return char
     */
    fun current(): Char {
        bufferUp()
        return if (isEmptyNoBufferUp()) EOF else charBuf!![bufPos]
    }

    fun consume(): Char {
        bufferUp()
        val value = if (isEmptyNoBufferUp()) EOF else charBuf!![bufPos]
        bufPos++
        return value
    }

    /**
     * Unconsume one character (bufPos--). MUST only be called directly after a consume(), and no chance of a bufferUp.
     */
    fun unconsume() {
        if (bufPos < 1) throw UncheckedIOException(IOException("WTF: No buffer left to unconsume.")) // a bug if this fires, need to trace it.
        bufPos--
    }

    /**
     * Moves the current position by one.
     */
    fun advance() {
        bufPos++
    }

    fun mark() {
        // make sure there is enough look ahead capacity
        if (bufLength - bufPos < minReadAheadLen) bufSplitPoint = 0
        bufferUp()
        bufMark = bufPos
    }

    fun unmark() {
        bufMark = -1
    }

    fun rewindToMark() {
        if (bufMark == -1) throw UncheckedIOException(IOException("Mark invalid"))
        bufPos = bufMark
        unmark()
    }

    /**
     * Returns the number of characters between the current position and the next instance of the input char
     * @param c scan target
     * @return offset between current position and next instance of target. -1 if not found.
     */
    fun nextIndexOf(c: Char): Int {
        // doesn't handle scanning for surrogates
        bufferUp()
        for (i in bufPos until bufLength) {
            if (c == charBuf!![i]) return i - bufPos
        }
        return -1
    }

    /**
     * Returns the number of characters between the current position and the next instance of the input sequence
     *
     * @param seq scan target
     * @return offset between current position and next instance of target. -1 if not found.
     */
    fun nextIndexOf(seq: CharSequence): Int {
        bufferUp()
        // doesn't handle scanning for surrogates
        val startChar = seq[0]
        var offset = bufPos
        while (offset < bufLength) {
            // scan to first instance of startChar:
            if (startChar != charBuf!![offset])
                while (++offset < bufLength && startChar != charBuf!![offset]) { /* empty */
                }

            var i = offset + 1
            val last = i + seq.length - 1
            if (offset < bufLength && last <= bufLength) {
                var j = 1
                while (i < last && seq[j] == charBuf!![i]) {
                    i++
                    j++
                }
                if (i == last) // found full sequence
                    return offset - bufPos
            }
            offset++
        }
        return -1
    }

    /**
     * Reads characters up to the specific char.
     * @param c the delimiter
     * @return the chars read
     */
    fun consumeTo(c: Char): String {
        val offset = nextIndexOf(c)
        return if (offset != -1) {
            val consumed = cacheString(
                charBuf,
                stringCache,
                bufPos,
                offset,
            )
            bufPos += offset
            consumed
        } else {
            consumeToEnd()
        }
    }

    fun consumeTo(seq: String): String {
        val offset = nextIndexOf(seq)
        return if (offset != -1) {
            val consumed = cacheString(
                charBuf,
                stringCache,
                bufPos,
                offset,
            )
            bufPos += offset
            consumed
        } else if (bufLength - bufPos < seq.length) {
            // nextIndexOf() did a bufferUp(), so if the buffer is shorter than the search string, we must be at EOF
            consumeToEnd()
        } else {
            // the string we're looking for may be straddling a buffer boundary, so keep (length - 1) characters
            // unread in case they contain the beginning of the search string
            val endPos = bufLength - seq.length + 1
            val consumed = cacheString(
                charBuf,
                stringCache,
                bufPos,
                endPos - bufPos,
            )
            bufPos = endPos
            consumed
        }
    }

    /**
     * Read characters until the first of any delimiters is found.
     * @param chars delimiters to scan for
     * @return characters read up to the matched delimiter.
     */
    fun consumeToAny(vararg chars: Char): String {
        bufferUp()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf
        val charLen = chars.size
        var i: Int
        OUTER@ while (pos < remaining) {
            i = 0
            while (i < charLen) {
                if (value!![pos] == chars[i]) break@OUTER
                i++
            }
            pos++
        }
        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeToAnySorted(vararg chars: Char): String {
        bufferUp()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf ?: return ""
        while (pos < remaining) {
            if (chars.contains(value[pos])) break
            pos++
        }
        bufPos = pos
        return if (bufPos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeData(): String {
        // &, <, null
        // bufferUp(); // no need to bufferUp, just called consume()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf
        OUTER@ while (pos < remaining) {
            when (value!![pos]) {
                '&', '<', TokeniserState.nullChar -> break@OUTER
                else -> pos++
            }
        }
        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeAttributeQuoted(single: Boolean): String {
        // null, " or ', &
        // bufferUp(); // no need to bufferUp, just called consume()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf
        OUTER@ while (pos < remaining) {
            when (value!![pos]) {
                '&', TokeniserState.nullChar -> break@OUTER
                '\'' -> {
                    if (single) break@OUTER
                    if (!single) break@OUTER
                    pos++
                }

                '"' -> {
                    if (!single) break@OUTER
                    pos++
                }

                else -> pos++
            }
        }
        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeRawData(): String {
        // <, null
        // bufferUp(); // no need to bufferUp, just called consume()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf
        OUTER@ while (pos < remaining) {
            when (value!![pos]) {
                '<', TokeniserState.nullChar -> break@OUTER
                else -> pos++
            }
        }
        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeTagName(): String {
        // '\t', '\n', '\r', '\u000c', ' ', '/', '>'
        // NOTE: out of spec, added '<' to fix common author bugs; does not stop and append on nullChar but eats
        bufferUp()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val value = charBuf ?: return ""
        OUTER@ while (pos < remaining) {
            when (value[pos]) {
                '\t', '\n', '\r', '\u000c', ' ', '/', '>', '<' -> break@OUTER // for form feed '\u000c' to '\u000c'
            }
            pos++
        }
        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    fun consumeToEnd(): String {
        bufferUp()
        val data = cacheString(charBuf, stringCache, bufPos, bufLength - bufPos)
        bufPos = bufLength
        return data
    }

    fun consumeLetterSequence(): String {
        bufferUp()
        val start = bufPos
        while (bufPos < bufLength) {
            val c = charBuf!![bufPos]
            if (c in 'A'..'Z' || c in 'a'..'z' || c.isLetter()) bufPos++ else break
        }
        return cacheString(charBuf, stringCache, start, bufPos - start)
    }

    fun consumeLetterThenDigitSequence(): String {
        bufferUp()
        val start = bufPos
        while (bufPos < bufLength) {
            val c = charBuf!![bufPos]
            if (c in 'A'..'Z' || c in 'a'..'z' || c.isLetter()) bufPos++ else break
        }
        while (!isEmptyNoBufferUp()) {
            val c = charBuf!![bufPos]
            if (c in '0'..'9') bufPos++ else break
        }
        return cacheString(charBuf, stringCache, start, bufPos - start)
    }

    fun consumeHexSequence(): String {
        bufferUp()
        val start = bufPos
        while (bufPos < bufLength) {
            val c = charBuf!![bufPos]
            if (c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f') bufPos++ else break
        }
        return cacheString(charBuf, stringCache, start, bufPos - start)
    }

    fun consumeDigitSequence(): String {
        bufferUp()
        val start = bufPos
        while (bufPos < bufLength) {
            val c = charBuf!![bufPos]
            if (c in '0'..'9') bufPos++ else break
        }
        return cacheString(charBuf, stringCache, start, bufPos - start)
    }

    fun matches(c: Char): Boolean {
        return !isEmpty() && charBuf!![bufPos] == c
    }

    fun matches(seq: String): Boolean {
        bufferUp()
        val scanLength = seq.length
        if (scanLength > bufLength - bufPos) return false
        for (offset in 0 until scanLength) if (seq[offset] != charBuf!![bufPos + offset]) return false
        return true
    }

    fun matchesIgnoreCase(seq: String): Boolean {
        bufferUp()
        val scanLength = seq.length
        if (scanLength > bufLength - bufPos) return false
        for (offset in 0 until scanLength) {
            val upScan = seq[offset].uppercaseChar()
            val upTarget = charBuf!![bufPos + offset].uppercaseChar()
            if (upScan != upTarget) return false
        }
        return true
    }

    fun matchesAny(vararg seq: Char): Boolean {
        if (isEmpty()) return false
        bufferUp()
        val c = charBuf!![bufPos]
        for (seek in seq) {
            if (seek == c) return true
        }
        return false
    }

    fun matchesAnySorted(seq: CharArray): Boolean {
        bufferUp()
        return !isEmpty() && seq.contains(charBuf!![bufPos])
    }

    fun matchesLetter(): Boolean {
        if (isEmpty()) return false
        val c = charBuf!![bufPos]
        return c in 'A'..'Z' || c in 'a'..'z' || c.isLetter()
    }

    /**
     * Checks if the current pos matches an ascii alpha (A-Z a-z) per https://infra.spec.whatwg.org/#ascii-alpha
     * @return if it matches or not
     */
    fun matchesAsciiAlpha(): Boolean {
        if (isEmpty()) return false
        val c = charBuf!![bufPos]
        return c in 'A'..'Z' || c in 'a'..'z'
    }

    fun matchesDigit(): Boolean {
        if (isEmpty()) return false
        val c = charBuf!![bufPos]
        return c in '0'..'9'
    }

    fun matchConsume(seq: String): Boolean {
        bufferUp()
        return if (matches(seq)) {
            bufPos += seq.length
            true
        } else {
            false
        }
    }

    fun matchConsumeIgnoreCase(seq: String): Boolean {
        return if (matchesIgnoreCase(seq)) {
            bufPos += seq.length
            true
        } else {
            false
        }
    }

    // we maintain a cache of the previously scanned sequence, and return that if applicable on repeated scans.
    // that improves the situation where there is a sequence of <p<p<p<p<p<p<p...</title> and we're bashing on the <p
    // looking for the </title>. Resets in bufferUp()
    /*@Nullable*/
    private var lastIcSeq: String? = null // scan cache
    private var lastIcIndex = 0 // nearest found indexOf

    /** Used to check presence of ,  when we're in RCData and see a <xxx. Only finds consistent case.></xxx.>  */
    fun containsIgnoreCase(seq: String): Boolean {
        if (seq == lastIcSeq) {
            if (lastIcIndex == -1) return false
            if (lastIcIndex >= bufPos) return true
        }
        lastIcSeq = seq
        val loScan = seq.lowercase()
        val lo = nextIndexOf(loScan)
        if (lo > -1) {
            lastIcIndex = bufPos + lo
            return true
        }
        val hiScan = seq.uppercase()
        val hi = nextIndexOf(hiScan)
        val found = hi > -1
        lastIcIndex =
            if (found) bufPos + hi else -1 // we don't care about finding the nearest, just that buf contains
        return found
    }

    override fun toString(): String {
        return if (bufLength - bufPos < 0) {
            ""
        } else {
            String.buildString(
                charBuf!!,
                bufPos,
                bufLength - bufPos,
            )
        }
    }

    // just used for testing
    fun rangeEquals(start: Int, count: Int, cached: String): Boolean {
        return rangeEquals(charBuf, start, count, cached)
    }

    companion object {
        const val EOF: Char = (-1).toChar()
        private const val maxStringCacheLen = 12
        const val maxBufferLen = 1024 * 32 // visible for testing
        const val readAheadLimit = (maxBufferLen * 0.75).toInt() // visible for testing
        private const val minReadAheadLen =
            1024 // the minimum mark length supported. No HTML entities can be larger than this.
        private const val stringCacheSize = 512

        /**
         * Caches short strings, as a flyweight pattern, to reduce GC load. Just for this doc, to prevent leaks.
         *
         *
         * Simplistic, and on hash collisions just falls back to creating a new string, vs a full HashMap with Entry list.
         * That saves both having to create objects as hash keys, and running through the entry list, at the expense of
         * some more duplicates.
         */
        private fun cacheString(
            charBuf: CharArray?,
            stringCache: Array<String?>?,
            start: Int,
            count: Int,
        ): String {
            // limit (no cache):
            if (count > maxStringCacheLen) return String.buildString(charBuf!!, start, count)
            if (count < 1) return ""

            // calculate hash:
            var hash = 0
            for (i in 0 until count) {
                hash = 31 * hash + charBuf!![start + i].code
            }

            // get from cache
            val index = hash and stringCacheSize - 1
            var cached = stringCache!![index]
            if (cached != null && rangeEquals(charBuf, start, count, cached)) {
                // positive hit
                return cached
            } else {
                cached = String.buildString(charBuf!!, start, count)
                stringCache[index] =
                    cached // add or replace, assuming most recently used are most likely to recur next
            }
            return cached
        }

        /**
         * Check if the value of the provided range equals the string.
         */
        fun rangeEquals(charBuf: CharArray?, start: Int, count: Int, cached: String): Boolean {
            var count = count
            if (count == cached.length) {
                var i = start
                var j = 0
                while (count-- != 0) {
                    if (charBuf!![i++] != cached[j++]) return false
                }
                return true
            }
            return false
        }
    }
}
