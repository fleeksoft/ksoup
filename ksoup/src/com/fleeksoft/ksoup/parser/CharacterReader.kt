/*
 * Kotlin port of jsoup's CharacterReader.java
 * Copyright © 2009–2025 Jonathan Hedley
 * Copyright © 2023–2025 FLEEK SOFT
 * Licensed under the MIT License
 * https://jsoup.org
 */

package com.fleeksoft.ksoup.parser

import com.fleeksoft.io.Reader
import com.fleeksoft.io.StringReader
import com.fleeksoft.io.exception.IOException
import com.fleeksoft.ksoup.exception.UncheckedIOException
import com.fleeksoft.ksoup.internal.SoftPool
import com.fleeksoft.ksoup.internal.StringUtil
import com.fleeksoft.ksoup.internal.StringUtil.isAsciiLetter
import com.fleeksoft.ksoup.internal.StringUtil.isHexDigit
import com.fleeksoft.ksoup.ported.buildString
import kotlin.math.abs
import kotlin.math.min

/**
 * CharacterReader consumes tokens off a string. Used internally by com.fleeksoft.ksoup. API subject to changes.
 */
public class CharacterReader : AutoCloseable {
    private var stringCache: Array<String?>? = null // holds reused strings in this doc, to lessen garbage

    private var reader: Reader? = null      // underlying Reader, will be backed by a buffered+controlled input source, or StringReader
    private var charBuf: CharArray?         // character buffer we consume from; filled from Reader
    private var bufPos = 0                  // position in charBuf that's been consumed to
    private var bufLength = 0               // the num of characters actually buffered in charBuf, <= charBuf.length
    private var fillPoint = 0               // how far into the charBuf we read before re-filling. 0.5 of charBuf.length after bufferUp
    private var consumed: Int = 0           // how many characters total have been consumed from this CharacterReader (less the current bufPos)
    private var bufMark = -1                // if not -1, the marked rewind position
    private var readFully = false           // if the underlying stream has been completely read, no value in further buffering

    private var newlinePositions: ArrayList<Int>? = null // optionally track the pos() position of newlines - scans during bufferUp()
    private var lineNumberOffset = 1 // line numbers start at 1; += newlinePosition[indexof(pos)]

    public constructor(reader: Reader) {
        this.reader = reader
        charBuf = BufferPool.borrow()
        stringCache = StringPool.borrow()
        bufferUp()
    }

    public constructor(html: String) : this(StringReader(html))

    public fun isClosed(): Boolean = reader == null

    public override fun close() {
        try {
            reader?.close()
        } catch (ignored: IOException) {
        } finally {
            reader = null
            charBuf?.fill(0.toChar()) // before release, clear the buffer. Not required, but acts as a safety net, and makes debug view clearer
            charBuf?.let { BufferPool.release(it) }
            charBuf = null
            stringCache?.let { StringPool.release(it) } // conversely, we don't clear the string cache, so we can reuse the contents
            stringCache = null
        }
    }

    private fun bufferUp() {
        if (readFully || bufPos < fillPoint || bufMark != -1) return

        doBufferUp() // structured so bufferUp may become an intrinsic candidate
    }

    private fun doBufferUp() {
        /*
        The flow:
        - if read fully, or if bufPos < fillPoint, or if marked - do not fill.
        - update readerPos (total amount consumed from this CharacterReader) += bufPos
        - shift charBuf contents such that bufPos = 0; set next read offset (bufLength) -= shift amount
        - loop read the Reader until we fill charBuf. bufLength += read.
        - readFully = true when read = -1
         */
        consumed += bufPos
        bufLength -= bufPos
        if (bufLength > 0) charBuf?.copyInto(destination = charBuf!!, destinationOffset = 0, startIndex = bufPos, endIndex = bufPos + bufLength)
        bufPos = 0
        while (bufLength < BufferSize) {
            try {
                val read = reader!!.read(charBuf!!, bufLength, charBuf!!.size - bufLength)
                if (read == -1) {
                    readFully = true
                    break
                }
                if (read == 0) {
                    break // if we have a surrogate on the buffer boundary and trying to read 1; will have enough in our buffer to proceed
                }
                bufLength += read
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
        fillPoint = min(bufLength, RefillPoint)

        scanBufferForNewlines() // if enabled, we index newline positions for line number tracking
        lastIcSeq = null // cache for last containsIgnoreCase(seq)
    }

    fun mark() {
        // make sure there is enough look ahead capacity
        if (bufLength - bufPos < RewindLimit) fillPoint = 0

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
     * Gets the position currently read to in the content. Starts at 0.
     * @return current position
     */
    public fun pos(): Int {
        return consumed + bufPos
    }

    /** Tests if the buffer has been fully read.  */
    public fun readFully(): Boolean {
        return readFully
    }

    /**
     * Enables or disables line number tracking. By default, will be **off**.Tracking line numbers improves the
     * legibility of parser error messages, for example. Tracking should be enabled before any content is read to be of
     * use.
     *
     * @param track set tracking on|off
     */
    public fun trackNewlines(track: Boolean) {
        if (track && newlinePositions == null) {
            newlinePositions = ArrayList<Int>(BufferSize / 80) // rough guess of likely count
            scanBufferForNewlines() // first pass when enabled; subsequently called during bufferUp
        } else if (!track) {
            newlinePositions = null
        }
    }

    public fun isTrackNewlines(): Boolean = newlinePositions != null

    /**
     * Get the current line number (that the reader has consumed to). Starts at line #1.
     * @return the current line number, or 1 if line tracking is not enabled.
     * @see .trackNewlines
     */
    public fun lineNumber(): Int {
        return lineNumber(pos())
    }

    public fun lineNumber(pos: Int): Int {
        // note that this impl needs to be called before the next buffer up or line numberoffset will be wrong. if that
        // causes issues, can remove the reset of newlinepositions during buffer, at the cost of a larger tracking array
        if (!isTrackNewlines()) return 1
        val i = lineNumIndex(pos)
        return if (i == -1) lineNumberOffset else i + lineNumberOffset + 1 // first line
    }

    /**
     * Get the current column number (that the reader has consumed to). Starts at column #1.
     * @return the current column number
     * @see .trackNewlines
     */
    public fun columnNumber(): Int {
        return columnNumber(pos())
    }

    public fun columnNumber(pos: Int): Int {
        if (!isTrackNewlines()) return pos + 1
        val i = lineNumIndex(pos)
        return if (i == -1) pos + 1 else pos - newlinePositions!![i] + 1
    }

    /**
     * Get a formatted string representing the current line and column positions. E.g. <code>5:10</code> indicating line
     * number 5 and column number 10.
     * @return line:col position
     * @see .trackNewlines
     */
    public fun posLineCol(): String {
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
        if (newlinePositions!!.isNotEmpty()) {
            // work out the line number that we have read up to (as we have likely scanned past this point)
            var index = lineNumIndex(consumed)
            if (index == -1) index = 0 // first line
            val linePos: Int = newlinePositions!![index]
            lineNumberOffset += index // the num lines we've read up to
            newlinePositions!!.clear()
            newlinePositions!!.add(linePos) // roll the last read pos to first, for cursor num after buffer
        }
        for (i in bufPos until bufLength) {
            if (charBuf!![i] == '\n') newlinePositions!!.add(1 + consumed + i)
        }
    }

    public fun isEmpty(): Boolean {
        bufferUp()
        return bufPos >= bufLength
    }

    private fun isEmptyNoBufferUp(): Boolean = bufPos >= bufLength

    /**
     * Get the char at the current position.
     * @return char
     */
    public fun current(): Char {
        bufferUp()
        return if (isEmptyNoBufferUp()) EOF else charBuf!![bufPos]
    }

    /**
    Consume one character off the queue.
    @return first character on queue, or EOF if the queue is empty.
     */
    public fun consume(): Char {
        bufferUp()
        val value = if (isEmptyNoBufferUp()) EOF else charBuf!![bufPos]
        bufPos++
        return value
    }

    /**
     * Unconsume one character (bufPos--). MUST only be called directly after a consume(), and no chance of a bufferUp.
     */
    public fun unconsume() {
        if (bufPos < 1) {
            throw UncheckedIOException(
                IOException("WTF: No buffer left to unconsume."),
            ) // a bug if this fires, need to trace it.
        }
        bufPos--
    }

    /**
     * Moves the current position by one.
     */
    public fun advance() {
        bufPos++
    }

    /**
     * Returns the number of characters between the current position and the next instance of the input char
     * @param c scan target
     * @return offset between current position and next instance of target. -1 if not found.
     */
    public fun nextIndexOf(c: Char): Int {
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
    public fun nextIndexOf(seq: CharSequence): Int {
        bufferUp()
        // doesn't handle scanning for surrogates
        val startChar = seq[0]
        var offset = bufPos
        while (offset < bufLength) {
            // scan to first instance of startChar:
            if (startChar != charBuf!![offset]) {
                while (++offset < bufLength && startChar != charBuf!![offset]) { // empty
                }
            }

            var i = offset + 1
            val last = i + seq.length - 1
            if (offset < bufLength && last <= bufLength) {
                var j = 1
                while (i < last && seq[j] == charBuf!![i]) {
                    i++
                    j++
                }
                if (i == last) {
                    // found full sequence
                    return offset - bufPos
                }
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
    public fun consumeTo(c: Char): String {
        val offset = nextIndexOf(c)
        return if (offset != -1) {
            val consumed =
                cacheString(
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

    /**
    Reads the characters up to (but not including) the specified case-sensitive string.
    <p>If the sequence is not found in the buffer, will return the remainder of the current buffered amount, less the
    length of the sequence, such that this call may be repeated.
    @param seq the delimiter
    @return the chars read
     */
    public fun consumeTo(seq: String): String {
        val offset = nextIndexOf(seq)
        return if (offset != -1) {
            val consumed =
                cacheString(
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
            val consumed =
                cacheString(
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
     * Read characters while the input predicate returns true.
     * @return characters read
     */
    fun consumeMatching(func: (Char) -> Boolean): String {
        return consumeMatching(func, -1)
    }

    /**
     * Read characters while the input predicate returns true, up to a maximum length.
     * @param func predicate to test
     * @param maxLength maximum length to read. -1 indicates no maximum
     * @return characters read
     */
    fun consumeMatching(func: (Char) -> Boolean, maxLength: Int): String {
        bufferUp()
        var pos = bufPos
        val start = pos
        val remaining = bufLength
        val valBuf = charBuf

        while (pos < remaining && (maxLength == -1 || pos - start < maxLength) && func(valBuf!![pos])) {
            pos++
        }

        bufPos = pos
        return if (pos > start) cacheString(charBuf, stringCache, start, pos - start) else ""
    }

    /**
     * Read characters until the first of any delimiters is found.
     * @param chars delimiters to scan for
     * @return characters read up to the matched delimiter.
     */
    fun consumeToAny(vararg chars: Char): String {
        return consumeMatching { c ->  // seeks until we see one of the terminating chars
            chars.none { seek -> c == seek }
        }
    }


    fun CharArray.binarySearch(c: Char): Int {
        // FIXME: don't have binary search for CharArray
        return this.indexOf(c)
    }

    public fun consumeToAnySorted(vararg chars: Char): String {
        return consumeMatching { c: Char -> chars.binarySearch(c) < 0 } // matches until a hit
    }

    public fun consumeData(): String {
        // consumes until &, <, null
        return consumeMatching { c -> c != '&' && c != '<' && c != TokeniserState.nullChar }
    }

    public fun consumeAttributeQuoted(single: Boolean): String {
        // null, " or ', &
        return consumeMatching { c: Char -> c != TokeniserState.nullChar && c != '&' && (if (single) c != '\'' else c != '"') }
    }

    public fun consumeRawData(): String {
        // <, null
        return consumeMatching { c: Char -> c != '<' && c != TokeniserState.nullChar }
    }

    public fun consumeTagName(): String {
        // '\t', '\n', '\r', '\f', ' ', '/', '>'
        // NOTE: out of spec; does not stop and append on nullChar but eats
        return consumeMatching { c: Char ->
            when (c) {
                '\t', '\n', '\r', '\u000c', ' ', '/', '>' -> return@consumeMatching false
            }
            true
        }
    }

    public fun consumeToEnd(): String {
        bufferUp()
        val data = cacheString(charBuf, stringCache, bufPos, bufLength - bufPos)
        bufPos = bufLength
        return data
    }

    public fun consumeLetterSequence(): String {
        return consumeMatching { ch: Char -> ch.isLetter() }
    }

    public fun consumeLetterThenDigitSequence(): String {
        bufferUp()
        val start = bufPos
        while (bufPos < bufLength) {
            if (isAsciiLetter(charBuf!![bufPos])) bufPos++
            else break
        }
        while (!isEmptyNoBufferUp()) {
            if (StringUtil.isDigit(charBuf!![bufPos])) bufPos++
            else break
        }

        return cacheString(charBuf, stringCache, start, bufPos - start)
    }

    public fun consumeHexSequence(): String {
        return consumeMatching({ c: Char -> isHexDigit(c) })
    }

    public fun consumeDigitSequence(): String {
        return consumeMatching { c: Char -> c >= '0' && c <= '9' }
    }

    public fun matches(c: Char): Boolean {
        return !isEmpty() && charBuf!![bufPos] == c
    }

    public fun matches(seq: String): Boolean {
        bufferUp()
        val scanLength = seq.length
        if (scanLength > bufLength - bufPos) return false
        for (offset in 0 until scanLength) if (seq[offset] != charBuf!![bufPos + offset]) return false
        return true
    }

    public fun matchesIgnoreCase(seq: String): Boolean {
        bufferUp()
        val scanLength = seq.length
        if (scanLength > bufLength - bufPos) return false

        for (offset in 0..<scanLength) {
            var scan = seq[offset]
            var target = charBuf!![bufPos + offset]
            if (scan == target) continue

            scan = scan.uppercaseChar()
            target = target.uppercaseChar()
            if (scan != target) return false
        }
        return true
    }

    /**
    Tests if the next character in the queue matches any of the characters in the sequence, case sensitively.
    @param seq list of characters to check for
    @return true if any matched, false if none did
     */
    public fun matchesAny(vararg seq: Char): Boolean {
        if (isEmpty()) return false
        bufferUp()
        val c = charBuf!![bufPos]
        for (seek in seq) {
            if (seek == c) return true
        }
        return false
    }

    public fun matchesAnySorted(seq: CharArray): Boolean {
        bufferUp()
        return !isEmpty() && seq.contains(charBuf!![bufPos])
    }

    /**
     * Checks if the current pos matches an ascii alpha (A-Z a-z) per https://infra.spec.whatwg.org/#ascii-alpha
     * @return if it matches or not
     */
    public fun matchesAsciiAlpha(): Boolean {
        if (isEmpty()) return false;
        return isAsciiLetter(charBuf!![bufPos])
    }

    public fun matchesDigit(): Boolean {
        if (isEmpty()) return false
        return StringUtil.isDigit(charBuf!![bufPos])
    }

    public fun matchConsume(seq: String): Boolean {
        bufferUp()
        return if (matches(seq)) {
            bufPos += seq.length
            true
        } else {
            false
        }
    }

    public fun matchConsumeIgnoreCase(seq: String): Boolean {
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

    private var lastIcSeq: String? = null // scan cache
    private var lastIcIndex = 0 // nearest found indexOf

    /** Used to check presence of ,  when we're in RCData and see a <xxx. Only finds consistent case.></xxx.>  */
    public fun containsIgnoreCase(seq: String): Boolean {
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
        if (bufLength - bufPos < 0) return ""
        return charBuf!!.concatToString(bufPos, bufPos + (bufLength - bufPos))
    }

    // just used for testing
    public fun rangeEquals(start: Int, count: Int, cached: String): Boolean {
        return rangeEquals(charBuf, start, count, cached)
    }

    public companion object {
        public const val EOF: Char = (-1).toChar()
        private const val MaxStringCacheLen = 12
        private const val StringCacheSize = 512
        private val StringPool: SoftPool<Array<String?>> = SoftPool { arrayOfNulls(StringCacheSize) }
        private val BufferPool: SoftPool<CharArray> = SoftPool { CharArray(BufferSize) } // recycled char buffer

        public const val BufferSize: Int = 1024 * 2 // visible for testing
        public const val RefillPoint: Int = BufferSize / 2 // when bufPos characters read, refill; visible for testing;
        private const val RewindLimit = 1024 // the maximum we can rewind. No HTML entities can be larger than this.

        /**
         * Caches short strings, as a flyweight pattern, to reduce GC load. Just for this doc, to prevent leaks.
         *
         *
         * Simplistic, and on hash collisions just falls back to creating a new string, vs a full HashMap with Entry list.
         * That saves both having to create objects as hash keys, and running through the entry list, at the expense of
         * some more duplicates.
         */
        private fun cacheString(charBuf: CharArray?, stringCache: Array<String?>?, start: Int, count: Int): String {
            // don't cache strings that are too big
            if (count > MaxStringCacheLen) return String.buildString(charBuf!!, start, count)
            if (count < 1) return ""

            // calculate hash:
            var hash = 0
            val end = count + start
            for (i in start until end) {
                hash = 31 * hash + charBuf!![i].code
            }

            // get from cache
            val index = hash and StringCacheSize - 1
            var cached = stringCache!![index]
            if (cached != null && rangeEquals(charBuf, start, count, cached)) {
                // positive hit
                return cached
            } else {
                cached = String.buildString(charBuf!!, start, count)
                stringCache[index] = cached // add or replace, assuming most recently used are most likely to recur next
            }
            return cached
        }

        /**
         * Check if the value of the provided range equals the string.
         */
        public fun rangeEquals(charBuf: CharArray?, start: Int, count: Int, cached: String): Boolean {
            var loopCount = count
            if (loopCount == cached.length) {
                var i = start
                var j = 0
                while (loopCount-- != 0) {
                    if (charBuf!![i++] != cached[j++]) return false
                }
                return true
            }
            return false
        }
    }
}
